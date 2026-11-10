package org.steamlite

import org.junit.Assert.*
import org.junit.Test

class FriendGameGroupingTest {
    @Test fun loadingTitleDoesNotChangeSectionIdentity() {
        val pending = FriendRow(1, "Test", "在线", null, true, true, "游戏名称暂未获取", gameAppId = 123)
        assertEquals(friendGameKey(pending), friendGameKey(pending.copy(game = "Game title")))
    }
    @Test fun twoUnknownGamesDoNotMerge() {
        val first = FriendRow(1, "Test", "在线", null, true, true, "游戏名称暂未获取", gameAppId = 123)
        assertNotEquals(friendGameKey(first), friendGameKey(first.copy(gameAppId = 456)))
    }
    @Test fun nonSteamCustomGamesKeepTheirOwnIdentity() {
        val first = FriendRow(1, "Test", "在线", null, true, true, "Custom A")
        assertNotEquals(friendGameKey(first), friendGameKey(first.copy(game = "Custom B")))
    }
}
