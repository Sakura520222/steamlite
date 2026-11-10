package org.steamlite

import org.junit.Assert.*
import org.junit.Test

class SteamMessageRequestTest {
    @Test fun identifiersMatchTheSteamWireFormat() {
        val requests = (1..100).map { privateMessageRequest(76561197960265729L, "hello") }
        assertTrue(requests.all { it.clientMessageId.matches(Regex("[0-9a-f]{16}")) })
        assertEquals(100, requests.map { it.clientMessageId }.toSet().size)
        assertTrue(requests.all { it.steamid == 76561197960265729L && it.chatEntryType == 1 && !it.echoToSender })
    }
    @Test fun stickerAndEmoteRequestsPreserveTheirWirePayload() {
        val sticker = privateMessageRequest(76561197960265729L, ChatContent.stickerCode("Nyan GLaDOS"))
        assertEquals("/sticker Nyan GLaDOS", sticker.message)
        assertTrue(sticker.containsBbcode)
        val emote = privateMessageRequest(76561197960265729L, "ːsteamhappyː")
        assertEquals("ːsteamhappyː", emote.message)
        assertTrue(emote.containsBbcode)
        assertEquals("hello", privateMessageRequest(76561197960265729L, "hello").message)
    }
}
