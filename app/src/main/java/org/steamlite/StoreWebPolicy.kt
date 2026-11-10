package org.steamlite

import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.Request

/** Fixed origin for every request carrying the store's authentication cookies. */
internal object StoreWebPolicy {
    fun requireStoreOrigin(url: HttpUrl): HttpUrl {
        require(url.scheme == "https" && url.host == "store.steampowered.com" && url.port == 443 && url.username.isEmpty() && url.password.isEmpty()) {
            "已阻止非 Steam 商店来源的认证请求"
        }
        return url
    }
    fun redirect(current: HttpUrl, location: String): HttpUrl = requireStoreOrigin(
        current.resolve(location) ?: error("Steam 跳转地址无效")
    )
    fun writeRequest(url: HttpUrl, appId: Int, cookies: Map<String, String>, userAgent: String): Request {
        require(appId > 0)
        val session = cookies["sessionid"]?.takeIf { it.isNotBlank() } ?: error("缺少商店会话标识，未发送修改")
        return Request.Builder().url(requireStoreOrigin(url))
            .header("User-Agent", userAgent)
            .header("Cookie", cookies.entries.joinToString("; ") { "${it.key}=${it.value}" })
            .header("Origin", "https://store.steampowered.com")
            .header("Referer", "https://store.steampowered.com/")
            .post(FormBody.Builder().add("appid", appId.toString()).add("sessionid", session).build()).build()
    }
}
