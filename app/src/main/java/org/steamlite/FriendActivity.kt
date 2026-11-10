package org.steamlite

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class FriendActivity(val id: String, val day: String, val text: String, val images: List<String>, val links: List<Pair<String, String>>, val author: String = "", val avatar: String? = null)
object FriendActivityParser {
    /** Jsoup resolves an empty src to the document URL, which is HTML, not an image. */
    private fun imageSource(image: org.jsoup.nodes.Element): String? =
        listOf("data-src", "data-lazy-src", "data-original", "src").firstNotNullOfOrNull { attribute ->
            if (image.attr(attribute).isBlank()) null else image.absUrl(attribute).takeIf { url ->
                ChatContent.mediaUrl(url) && url.toHttpUrlOrNull()?.encodedPath?.let { it.isNotBlank() && it != "/" } == true
            }
        }
    fun allowedPage(url: String): Boolean {
        val u = url.toHttpUrlOrNull() ?: return false
        return u.scheme == "https" && u.host == "steamcommunity.com" && u.port == 443 && u.username.isEmpty() && u.password.isEmpty() && Regex("/(profiles/[0-9]+|id/[^/]+)/ajaxgetusernews/?").matches(u.encodedPath)
    }
    fun parse(html: String): List<FriendActivity> {
        val doc = Jsoup.parse(html, "https://steamcommunity.com")
        return doc.select(".blotter_block").mapNotNull { block ->
            block.select("script, style, textarea, input, button, .commentthread_area, .blotter_comment_thread, .blotter_vote_container, .blotter_comment_entry").remove()
            val text = block.text().trim()
            if(text.isBlank()) return@mapNotNull null
            val day = block.parents().firstOrNull { it.hasClass("blotter_day") }?.selectFirst(".blotter_day_header_date")?.text().orEmpty()
            val images = block.select("img").filter { it.parents().none { p -> p.hasClass("avatarMedium") || p.hasClass("playerAvatar") || p.hasClass("blotter_player_avatar") } }.mapNotNull(::imageSource)
                .filterNot { it.toHttpUrlOrNull()?.let { url -> url.host.startsWith("avatars.") || url.encodedPath.startsWith("/avatars/") } == true }.distinct().take(6)
            val links = block.select("a[href]").map { it.text().ifBlank { "查看 Steam 内容" } to it.absUrl("href") }.filter { ChatContent.webUrl(it.second) }.distinctBy { it.second }.take(8)
            val id = block.id().ifBlank { MessageDigest.getInstance("SHA-256").digest(block.outerHtml().toByteArray()).joinToString("") { "%02x".format(it) } }
            val author = block.selectFirst("a.blotter_author_block, .blotter_author_block a[href], a.blotter_author, .blotter_header .friendLink, .blotter_header .whiteLink, a[data-miniprofile]")?.text()?.takeIf { it.isNotBlank() }
                ?: block.select("a[href]").firstOrNull { it.text().isNotBlank() && Regex("https://steamcommunity.com/(id|profiles)/[^/?#]+/?").matches(it.absUrl("href")) }?.text().orEmpty()
            val avatar = block.selectFirst(".blotter_player_avatar img, .playerAvatar img, .avatarMedium img")?.let(::imageSource)
            FriendActivity(id, day, text.take(5000), images, links, author, avatar)
        }
    }
}

/** Account-only feed, held in memory. Credentials never leave the fixed Community origin. */
class FriendActivityRepository(private val steam: SteamConnection, private val scope: CoroutineScope) {
    val items = MutableStateFlow<List<FriendActivity>>(emptyList())
    val loading = MutableStateFlow(false)
    val error = MutableStateFlow("")
    val hasMore = MutableStateFlow(false)
    private var next: String? = null
    private var job: Job? = null
    private val http = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).callTimeout(30, TimeUnit.SECONDS).build()
    suspend fun syncNow() { refresh(); job?.join(); check(error.value.isBlank()) { error.value } }
    init { scope.launch { steam.online.collect { online -> if(!online) { job?.cancel(); items.value = emptyList(); next = null; hasMore.value = false; error.value = "" } } } }
    fun refresh(more: Boolean = false) {
        if(!steam.online.value || loading.value) return
        val owner = steam.owner
        val target = if(more) next ?: return else "https://steamcommunity.com/profiles/$owner/ajaxgetusernews/?l=schinese"
        check(FriendActivityParser.allowedPage(target))
        loading.value = true; error.value = ""
        job = scope.launch {
            try {
                val token = steam.appAccessToken()
                check(owner == steam.owner && steam.online.value)
                val parsed = withContext(Dispatchers.IO) {
                    val cookie = "steamLoginSecure=" + URLEncoder.encode("$owner||$token", "UTF-8")
                    val request = Request.Builder().url(target).header("Cookie", cookie)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                        .header("Accept", "application/json, text/javascript, */*; q=0.01")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .header("Referer", "https://steamcommunity.com/profiles/$owner/home/").build()
                    http.newCall(request).execute().use { response ->
                        check(response.isSuccessful) { "Steam 动态请求 HTTP ${response.code}" }
                        val json = JSONObject(response.body?.string().orEmpty())
                        check(json.optBoolean("success")) { "Steam 未返回动态内容，请重新登录后同步" }
                        val html = json.optString("blotter_html")
                        val rows = FriendActivityParser.parse(html)
                        check(rows.isNotEmpty() || html.isBlank()) { "Steam 动态格式发生变化，暂时无法解析" }
                        rows to json.optString("next_request").takeIf { it.isNotBlank() && it != target && FriendActivityParser.allowedPage(it) }
                    }
                }
                if(owner != steam.owner || !steam.online.value) return@launch
                items.value = ((if(more) items.value else emptyList()) + parsed.first).distinctBy { it.id }
                next = parsed.second; hasMore.value = next != null && parsed.first.isNotEmpty()
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { if(owner == steam.owner) error.value = if(e is IllegalStateException) e.message.orEmpty() else "动态同步失败，请检查网络后重试" }
            finally { loading.value = false }
        }
    }
}
