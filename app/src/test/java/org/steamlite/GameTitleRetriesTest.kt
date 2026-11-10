package org.steamlite

import org.junit.Assert.*
import org.junit.Test

class GameTitleRetriesTest {
    @Test fun failuresBackOffAndStopRatherThanLoop() {
        val retry = GameTitleRetries()
        assertTrue(retry.ready(1, 0)); retry.started(1, 0)
        assertFalse(retry.ready(1, 29999)); assertTrue(retry.ready(1, 30000))
        retry.started(1, 30000)
        assertFalse(retry.ready(1, 149999)); assertTrue(retry.ready(1, 150000))
        retry.started(1, 150000)
        assertFalse(retry.ready(1, Long.MAX_VALUE)); assertNull(retry.next(listOf(1)))
        retry.reset(listOf(1)); assertTrue(retry.ready(1, 150001))
    }
    @Test fun budgetsAreIndependentForDifferentGames() {
        val retry = GameTitleRetries(); retry.started(1, 0)
        assertTrue(retry.ready(2, 0)); assertEquals(30000L, retry.next(listOf(1, 2)))
    }
}
