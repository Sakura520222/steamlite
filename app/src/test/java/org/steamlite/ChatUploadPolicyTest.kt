package org.steamlite

import org.junit.Assert.*
import org.junit.Test

class ChatUploadPolicyTest {
    @Test fun recipientTypesStaySeparate() {
        assertEquals(mapOf("friend_steamid" to "76561198000000001"), ChatUploadPolicy.target("friend:76561198000000001"))
        assertEquals(mapOf("chat_group_id" to "11", "chat_id" to "22"), ChatUploadPolicy.target("group:11:22"))
        listOf("friend:0", "friend:1:2", "group:1", "group:1:-2", "other:123").forEach { assertTrue(runCatching { ChatUploadPolicy.target(it) }.isFailure) }
    }
    @Test fun uploadedPictureCannotBeDirectedToAnArbitraryServer() {
        assertTrue(ChatUploadPolicy.destination("https://images.steamusercontent.com/upload/path"))
        assertTrue(ChatUploadPolicy.destination("https://steamcloud-ugc.storage.googleapis.com/upload/path"))
        assertTrue(ChatUploadPolicy.destination("https://steamugcquincy.blob.core.windows.net/upload/path"))
        assertFalse(ChatUploadPolicy.destination("https://other-bucket.storage.googleapis.com/upload/path"))
        assertFalse(ChatUploadPolicy.destination("https://other.blob.core.windows.net/upload/path"))
        assertFalse(ChatUploadPolicy.destination("https://steamcloud-ugc.storage.googleapis.com.evil.example/upload/path"))
        listOf("http://images.steamusercontent.com/upload", "https://localhost/upload", "https://steamusercontent.com.evil.example/upload", "https://user@steamusercontent.com/upload", "https://steamusercontent.com:8443/upload").forEach { assertFalse(it, ChatUploadPolicy.destination(it)) }
    }
}
