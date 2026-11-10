package org.steamlite

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

data class UploadImage(val bytes: ByteArray, val mime: String, val extension: String, val width: Int, val height: Int)
object ChatUploadPolicy {
    const val MAX_BYTES = 20 * 1024 * 1024
    fun destination(url: String): Boolean {
        val u = url.toHttpUrlOrNull() ?: return false
        if(u.scheme != "https" || u.port != 443 || u.username.isNotEmpty() || u.password.isNotEmpty()) return false
        // Exact storage origins observed in Steam's authenticated beginfileupload response.
        val steamStorage = setOf("steamcloud-ugc.storage.googleapis.com", "steamugcquincy.blob.core.windows.net")
        return u.host in steamStorage || listOf("steamusercontent.com", "steamstatic.com", "steamuserimages-a.akamaihd.net", "steamusercontent-a.akamaihd.net").any { u.host == it || u.host.endsWith(".$it") }
    }
    fun target(key: String): Map<String, String> {
        val parts = key.split(':')
        require(parts.drop(1).all { it.toLongOrNull()?.let { n -> n > 0 } == true })
        return when {
            parts.size == 2 && parts[0] == "friend" -> mapOf("friend_steamid" to parts[1])
            parts.size == 3 && parts[0] == "group" -> mapOf("chat_group_id" to parts[1], "chat_id" to parts[2])
            else -> error("无效会话")
        }
    }
    suspend fun read(context: Context, uri: Uri): UploadImage = withContext(Dispatchers.IO) {
        val bytes = ByteArrayOutputStream()
        context.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(8192)
            while(true) { ensureActive(); val count = input.read(buffer); if(count < 0) break
                require(bytes.size() + count <= MAX_BYTES) { "图片超过 20 MB，请选择更小的图片" }; bytes.write(buffer, 0, count)
            }
        } ?: error("无法读取所选图片")
        val data = bytes.toByteArray(); require(data.isNotEmpty()) { "图片为空" }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0 && bounds.outWidth.toLong() * bounds.outHeight <= 100_000_000) { "图片格式或尺寸不支持" }
        val ext = when(bounds.outMimeType) { "image/jpeg" -> "jpg"; "image/png" -> "png"; "image/gif" -> "gif"; "image/webp" -> "webp"; else -> error("请选择 JPEG、PNG、GIF 或 WebP 图片") }
        UploadImage(data, bounds.outMimeType, ext, bounds.outWidth, bounds.outHeight)
    }
}

/** One begin / PUT / commit sequence. Commit sends the picture; never repeat it automatically. */
class ChatImageUploader(private val steam: SteamConnection) {
    private val http = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).callTimeout(90, TimeUnit.SECONDS).build()
    private fun nonce() = ByteArray(20).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
    suspend fun upload(key: String, image: UploadImage, expectedOwner: String): String = withContext(Dispatchers.IO) {
        val target = ChatUploadPolicy.target(key)
        fun current() { ensureActive(); check(steam.online.value && steam.owner == expectedOwner) { "登录会话已改变，未继续发送" } }
        current()
        val token = steam.appAccessToken(); current()
        val session = nonce(); val hash = nonce(); val file = "SteamLite-${System.currentTimeMillis()}.${image.extension}"
        val cookie = "sessionid=$session; steamLoginSecure=" + URLEncoder.encode("$expectedOwner||$token", "UTF-8")
        fun form(): MultipartBody.Builder = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("sessionid", session).addFormDataPart("l", "schinese")
            .addFormDataPart("file_name", file).addFormDataPart("file_sha", hash)
            .addFormDataPart("file_type", image.mime).addFormDataPart("file_image_width", image.width.toString()).addFormDataPart("file_image_height", image.height.toString())
        fun post(path: String, body: RequestBody): JSONObject {
            val request = Request.Builder().url("https://steamcommunity.com/chat/$path/").header("Cookie", cookie)
                .header("Referer", "https://steamcommunity.com/chat/").header("User-Agent", "Mozilla/5.0 SteamLite/0.5").post(body).build()
            return http.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Steam 图片请求 HTTP ${response.code}" }
                val raw = response.body?.string().orEmpty()
                val json = try { JSONObject(raw) } catch(_: Exception) { error("Steam 返回了非 JSON 响应（$path），请重新登录后重试") }
                check(json.optInt("success") == 1 || json.optBoolean("success")) { "Steam 未接受图片请求" }
                json
            }
        }
        val begun = post("beginfileupload", form().addFormDataPart("file_size", image.bytes.size.toString()).build())
        current()
        val result = begun.optJSONObject("result") ?: error("Steam 未返回上传地址，未发送图片")
        val url = "https://${result.getString("url_host")}${result.getString("url_path") }"
        android.util.Log.i("SteamLiteUpload", "begin accepted; storageHost=${url.toHttpUrlOrNull()?.host.orEmpty()}; headerCount=${result.optJSONArray("request_headers")?.length() ?: 0}")
        check(ChatUploadPolicy.destination(url)) { "Steam 返回的上传来源不在支持范围，未上传图片" }
        val put = Request.Builder().url(url).put(image.bytes.toRequestBody(image.mime.toMediaType()))
        val headers = result.optJSONArray("request_headers")
        if(headers != null) for(i in 0 until headers.length()) {
            val h = headers.getJSONObject(i); val name = h.getString("name")
            if(name.lowercase() !in setOf("cookie", "host", "content-length", "proxy-authorization", "connection")) put.header(name, h.getString("value"))
        }
        http.newCall(put.build()).execute().use { check(it.isSuccessful) { "Steam 图片存储上传失败：HTTP ${it.code}" } }
        current()
        val commit = form().addFormDataPart("success", "1").addFormDataPart("ugcid", result.get("ugcid").toString())
            .addFormDataPart("timestamp", begun.opt("timestamp")?.toString().orEmpty()).addFormDataPart("hmac", begun.optString("hmac")).addFormDataPart("spoiler", "0")
        target.forEach { (k, v) -> commit.addFormDataPart(k, v) }
        // After this point a timeout may still mean delivered. No implicit retry is safe.
        try {
            val done = post("commitfileupload", commit.build()).optJSONObject("result") ?: error("未返回图片提交结果")
            check(done.optInt("success", 1) == 1) { "Steam 未确认图片发送成功" }
            done.optJSONObject("details")?.optString("url").orEmpty()
        } catch(e: CancellationException) { throw e }
        catch(_: Exception) { error("图片发送结果未确认，请先同步会话核对，避免重复发送") }
    }
}
