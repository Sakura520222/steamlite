package org.steamlite

import android.app.Activity
import android.app.Instrumentation
import android.graphics.BitmapFactory
import android.os.Bundle
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import androidx.room.Room
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.Protocol
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.ByteArrayOutputStream

/** Read-only check using public Steam artwork, without any account credentials. */
class ImageNetworkCheck : Instrumentation() {
    private var artworkOnly = false
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); artworkOnly = arguments?.getString("artworkOnly") == "true"; start() }
    override fun onStart() {
        val result = Bundle()
        if (artworkOnly) {
            try { result.putString("artwork", AnimatedArtworkCheck.run(this)) }
            catch(e: Exception) { result.putString("artwork", "FAIL: ${e.javaClass.simpleName}: ${e.message}") }
            finish(Activity.RESULT_OK, result); return
        }
        try { result.putString("voiceEngine", VoiceEngineCheck.run(this)) }
        catch(e: Exception) { result.putString("voiceEngine", "FAIL: ${e.javaClass.simpleName}: ${e.message}") }
        runBlocking {
        val db = Room.inMemoryDatabaseBuilder(targetContext, LiteDatabase::class.java).build()
        try {
            val dao = db.dao()
            dao.put(Message("old", "test-a", 1, "old", 1000, false, "test"))
            dao.put(Message("new", "test-a", 1, "new", 2000, false, "test"))
            dao.put(Message("tie", "test-a", 1, "tie", 2000, true, "test"))
            dao.put(Message("peer-two", "test-a", 2, "second", 3000, false, "test"))
            dao.put(Message("other-account", "test-b", 1, "private", 9000, false, "test"))
            val rows = dao.conversations("test-a").first()
            check(rows.size == 2 && rows.map { it.peer } == listOf(2L, 1L))
            check(rows.last().time == 2000L && rows.none { it.owner != "test-a" })
            dao.put(Message("new", "test-a", 1, "replacement", 2000, false, "test"))
            check(dao.messages("test-a", 1).first().size == 3)
            check(dao.messages("test-a", 1, 2).first().all { it.time == 2000L })
            result.putString("storage", "PASS: latest conversations, ties, account isolation, duplicate upsert, last-N history")
        } catch(e: Exception) { result.putString("storage", "FAIL: ${e.javaClass.simpleName}: ${e.message}") }
        finally { db.close() }
        try {
            val png = ByteArrayOutputStream().apply {
                android.graphics.Bitmap.createBitmap(4, 4, android.graphics.Bitmap.Config.ARGB_8888).also { bitmap ->
                    bitmap.eraseColor(android.graphics.Color.BLUE); bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, this); bitmap.recycle()
                }
            }.toByteArray()
            val url = "https://images.steamusercontent.com/ugc/test/fixture"
            val mock = OkHttpClient.Builder().addInterceptor { chain ->
                check(chain.request().header("Cookie") == null && chain.request().header("Authorization") == null)
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("test").body(png.toResponseBody()).build()
            }.build()
            val image = readImage(url, mock)
            check(image.second == "png" && image.first.contentEquals(png))
            val saved = java.io.File(targetContext.cacheDir, "image-save-check.png")
            targetContext.contentResolver.openOutputStream(android.net.Uri.fromFile(saved))!!.use { it.write(image.first) }
            check(saved.readBytes().contentEquals(png)); saved.delete()
            var requests = 0
            val redirect = OkHttpClient.Builder().addInterceptor { chain ->
                requests++
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(302).message("test").header("Location", "https://untrusted.example/image.png").body(byteArrayOf().toResponseBody()).build()
            }.build()
            check(runCatching { readImage(url, redirect) }.isFailure && requests == 1)
            val invalid = OkHttpClient.Builder().addInterceptor { chain ->
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("test").body("<html>not an image</html>".toResponseBody()).build()
            }.build()
            check(runCatching { readImage(url, invalid) }.isFailure)
            result.putString("imagePipeline", "PASS: PNG decode, unchanged bytes, ContentResolver save, no credentials, external redirect rejected, HTML rejected")
        } catch(e: Exception) { result.putString("imagePipeline", "FAIL: ${e.javaClass.simpleName}: ${e.message}") }
        for(host in listOf("shared.fastly.steamstatic.com", "shared.akamai.steamstatic.com", "cdn.akamai.steamstatic.com", "community.akamai.steamstatic.com", "store.steampowered.com")) {
            try {
                val path = if(host == "store.steampowered.com") "/api/appdetails?appids=34900" else if(host.startsWith("community")) "/economy/emoticon/steamthumbsup" else if(host.startsWith("cdn")) "/steam/apps/34900/header.jpg" else "/store_item_assets/steam/apps/34900/header.jpg"
                val client = PublicImages.http.newBuilder().callTimeout(8, java.util.concurrent.TimeUnit.SECONDS).build()
                result.putString("dns:$host", okhttp3.Dns.SYSTEM.lookup(host).joinToString { it.hostAddress.orEmpty() })
                client.newCall(Request.Builder().url("https://$host$path").header("User-Agent", "Mozilla/5.0 SteamLite/0.5").build()).execute().use { response ->
                    val bytes = response.body!!.bytes()
                    result.putString(host, "HTTP ${response.code}; ${bytes.size} bytes")
                }
            } catch(e: Exception) { result.putString(host, e.javaClass.simpleName + ": " + e.message) }
        }
        try {
            val url = "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/34900/header.jpg"
            PublicImages.http.newCall(Request.Builder().url(url).build()).execute().use { response ->
                result.putInt("http", response.code)
                check(response.isSuccessful)
                val bytes = response.body!!.bytes()
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                check(bounds.outWidth > 0 && bounds.outHeight > 0)
                result.putString("image", "${bounds.outWidth}x${bounds.outHeight}")
            }
            result.putString("result", "PASS")
        } catch(e: Exception) { result.putString("result", "FAIL: " + generateSequence<Throwable>(e) { it.cause }.joinToString(" / ") { it.javaClass.simpleName + ": " + it.message }) } }
        finish(Activity.RESULT_OK, result)
    }
}
