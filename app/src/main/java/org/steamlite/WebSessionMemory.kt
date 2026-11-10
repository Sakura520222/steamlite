package org.steamlite

/** In-memory state with an epoch fence so delayed jobs cannot repopulate a logged-out session. */
internal class WebSessionMemory {
    private var epoch = 0L
    private var owner = ""
    private var token = ""
    private var issuedAt = 0L
    private val cookies = linkedMapOf<String, String>()
    @Synchronized fun generation(): Long = epoch
    @Synchronized fun cached(account: String, now: Long, expected: Long): String? = token.takeIf { expected == epoch && account == owner && it.isNotEmpty() && now >= issuedAt && now - issuedAt < 240000 }
    @Synchronized fun cache(account: String, value: String, now: Long, expected: Long): Boolean {
        if(expected != epoch) return false
        owner = account; token = value; issuedAt = now; return true
    }
    @Synchronized fun seed(account: String, value: String, session: String, expected: Long) {
        check(expected == epoch) { "网页会话已失效，未发送请求" }
        cookies.clear(); cookies["steamLoginSecure"] = "$account||$value"; cookies["sessionid"] = session
    }
    @Synchronized fun absorb(headers: List<String>, expected: Long) {
        check(expected == epoch) { "网页会话已失效" }
        headers.forEach { header -> val pair = header.substringBefore(';'); val name = pair.substringBefore('=', "").trim(); if(name.isNotEmpty()) cookies[name] = pair.substringAfter('=', "").trim() }
    }
    @Synchronized fun snapshot(expected: Long): Map<String,String> {
        check(expected == epoch) { "网页会话已失效，未发送请求" }
        return cookies.toMap()
    }
    @Synchronized fun invalidate() { epoch++; owner = ""; token = ""; issuedAt = 0; cookies.clear() }
}
