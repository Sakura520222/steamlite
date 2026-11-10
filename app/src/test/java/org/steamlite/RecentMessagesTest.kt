package org.steamlite

import org.junit.Assert.*
import org.junit.Test

class RecentMessagesTest {
    @Test fun incomingUnreadCreatesAndUpdatesSessionWithoutOpeningMessagesPage() {
        val first = recordRecentMessage(emptyList(), 1, 1000, true)
        assertEquals(1, first.single().unread)
        val next = recordRecentMessage(first, 1, 2000, true)
        assertEquals(2, next.single().unread)
        assertEquals(2000L, next.single().time)
    }
    @Test fun viewingConversationAndOwnEchoDoNotCreateUnreadBadges() {
        assertEquals(0, recordRecentMessage(emptyList(), 1, 1000, false).single().unread)
    }
    @Test fun refreshDoesNotEraseNewerLiveMessagesOrReviveAlreadyReadMessages() {
        val local = listOf(RecentSession(1, 3000, 2), RecentSession(2, 2000, 0))
        val server = listOf(RecentSession(1, 1000, 0), RecentSession(2, 2000, 4), RecentSession(3, 2500, 1))
        val merged = mergeRecentMessages(local, server, mapOf(2L to 2000L))
        assertEquals(listOf(1L, 3L, 2L), merged.map { it.peer })
        assertEquals(listOf(2, 1, 0), merged.map { it.unread })
        assertTrue(merged.any { it.unread > 0 })
    }
}
