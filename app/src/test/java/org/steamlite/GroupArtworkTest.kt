package org.steamlite

import org.junit.Assert.*
import org.junit.Test

class GroupArtworkTest {
    @Test fun groupHashUsesChatIconDirectoriesAndSizeSuffix() {
        val hash = ByteArray(20) { it.toByte() }
        assertEquals("https://community.akamai.steamstatic.com/public/images/chaticons/00/01/02/000102030405060708090a0b0c0d0e0f10111213_256.jpg", groupAvatarUrls(hash, "").first())
    }
    @Test fun uploadedAvatarIsPreferredAndUntrustedAddressesAreExcluded() {
        val url = "https://images.steamusercontent.com/ugc/test/avatar.png"
        assertEquals(listOf(url), groupAvatarUrls(ByteArray(20), url))
        assertTrue(groupAvatarUrls(ByteArray(40), "https://evil.example/avatar.jpg").isEmpty())
    }
}
