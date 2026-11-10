package org.steamlite

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

/**
 * Wishlist writer that talks to Steam's own store / WebAPI endpoints.
 *
 * Rules this class keeps:
 * - the refresh token never leaves the process; only a short-lived access token is used,
 * - no cookie value, token or response body is logged or stored, only stage, status and sizes,
 * - a successful HTTP response is *not* treated as success; the caller must confirm the
 *   change with an independent CM read of Wishlist.GetWishlist#1,
 * - every blocking call runs on Dispatchers.IO; the UI thread never performs network work.
 *
 * The store redirects `/account/` back to itself until it has set its own cookies, so the
 * redirect chain is walked by hand with a small in-memory cookie jar instead of a one-shot
 * request per hop.
 */
class StoreWebClient(private val steam: SteamConnection) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .build()
    private val random = SecureRandom()
    private val tokenMutex = Mutex()
    private val operationMutex = Mutex()
    private val session = WebSessionMemory()
    fun invalidate() = session.invalidate()
    private val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

    private fun trace(stage: String, detail: String) = android.util.Log.i("SteamLiteWeb", "$stage | $detail")

    /** Short-lived access token for the logged-on account; cached for four minutes. */
    private suspend fun accessToken(epoch: Long): String = tokenMutex.withLock {
        val owner = steam.steamId
        val now = System.currentTimeMillis()
        session.cached(owner, now, epoch)?.let { return@withLock it }
        val fresh = steam.appAccessToken()
        check(steam.steamId == owner && session.cache(owner, fresh, now, epoch)) { "账号会话已变化，未缓存令牌" }
        fresh
    }
    private fun sessionId(): String = ByteArray(12).also(random::nextBytes).joinToString("") { "%02x".format(it) }
    private fun cookieHeader(epoch: Long) = session.snapshot(epoch).entries.joinToString("; ") { "${it.key}=${it.value}" }

    private fun get(url: HttpUrl): Request = Request.Builder().url(StoreWebPolicy.requireStoreOrigin(url))
        .header("User-Agent", userAgent)
        .header("Accept-Language", "en-US,en;q=0.9")
        .get()
        .build()

    /**
     * Sends one store mutation without transport fallback or automatic HTTP retries.
     * A timeout can have an unknown outcome; callers must read the wishlist back.
     */
    suspend fun write(appId: Int, add: Boolean) = withContext(Dispatchers.IO) { operationMutex.withLock {
        require(appId > 0) { "AppID 无效" }
        val owner = steam.steamId
        if (owner.isBlank()) throw IllegalStateException("账号尚未就绪，请稍后重试")
        val epoch = session.generation()
        val token = accessToken(epoch)
        check(steam.steamId == owner) { "账号会话已变化，未发送修改" }
        storeWrite(owner, token, appId, add, epoch)
        trace("write", "store accepted appid=$appId add=$add")
    } }

    /** POST https://store.steampowered.com/api/{add,remove}…wishlist with a real web session. */
    private suspend fun storeWrite(owner: String, token: String, appId: Int, add: Boolean, epoch: Long) {
        session.seed(owner, token, sessionId(), epoch)
        // One pass through the store's redirect chain collects the cookies it insists on setting.
        warmSession(epoch)
        val method = if (add) "addtowishlist" else "removefromwishlist"
        check(steam.steamId == owner) { "账号会话已变化，未发送修改" }
        val cookies = session.snapshot(epoch)
        val request = StoreWebPolicy.writeRequest("https://store.steampowered.com/api/$method".toHttpUrl(), appId, cookies, userAgent)
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val success = runCatching { JSONObject(body).optBoolean("success") }.getOrDefault(false)
            trace("store.$method", "HTTP ${response.code} success=$success bytes=${body.length}")
            if (response.code != 200) throw IllegalStateException("HTTP ${response.code}")
            if (!success) throw IllegalStateException("Steam 未接受该操作")
        }
    }

    /** Walks the store redirect chain so that Steam's own cookies are collected. */
    private suspend fun warmSession(epoch: Long): List<String> {
        var url = "https://store.steampowered.com/account/".toHttpUrl()
        val hops = mutableListOf<String>()
        repeat(6) { hop ->
            val request = get(url).newBuilder().header("Cookie", cookieHeader(epoch)).build()
            http.newCall(request).execute().use { response ->
                hops += "HTTP ${response.code} ${response.request.url.encodedPath}"
                trace("warm", "hop ${hop + 1}: HTTP ${response.code} ${response.request.url.encodedPath}")
                session.absorb(response.headers("Set-Cookie"), epoch)
                if (!response.isRedirect) {
                    check(response.code == 200 && response.request.url.encodedPath.startsWith("/account")) { "商店会话未就绪，未发送修改" }
                    return hops
                }
                val location = response.header("Location") ?: error("Steam 跳转缺少目标，未发送修改")
                url = StoreWebPolicy.redirect(response.request.url, location)
            }
        }
        error("Steam 商店跳转次数过多，未发送修改")
    }

    /**
     * Read-only check of the derived web session: walks the redirect chain of the store
     * account page and reports where it lands. No write endpoint is touched.
     */
    suspend fun probeStore(): String = withContext(Dispatchers.IO) { operationMutex.withLock {
        val owner = steam.steamId
        if (owner.isBlank()) return@withLock "账号尚未就绪"
        val epoch = session.generation()
        val token = accessToken(epoch)
        session.seed(owner, token, sessionId(), epoch)
        val hops = warmSession(epoch)
        val last = hops.lastOrNull() ?: "无响应"
        val ok = last.startsWith("HTTP 200") && !last.contains("/login")
        hops.joinToString(" → ") + if (ok) " · 网页会话有效" else " · 网页会话未被接受"
    } }
}
