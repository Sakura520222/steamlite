package org.steamlite

internal fun groupAvatarUrls(hash: ByteArray, uploaded: String): List<String> {
    val sha = if (hash.size == 20) hash.joinToString("") { "%02x".format(it) }.takeIf { it.any { c -> c != '0' } } else null
    return buildList {
        if (ChatContent.mediaUrl(uploaded)) add(uploaded)
        if (sha != null) {
            add("https://community.akamai.steamstatic.com/public/images/chaticons/${sha.take(2)}/${sha.substring(2,4)}/${sha.substring(4,6)}/${sha}_256.jpg")
            add("https://avatars.steamstatic.com/${sha}_256.jpg")
        }
    }.distinct()
}
