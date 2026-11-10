package org.steamlite
import org.junit.Assert.*
import org.junit.Test
class WebSessionMemoryTest {
    @Test fun cachedTokensRespectOwnerExpiryAndClockRollback() {
        val state = WebSessionMemory(); val epoch = state.generation()
        assertTrue(state.cache("account-a", "dummy", 1000, epoch))
        assertEquals("dummy", state.cached("account-a", 2000, epoch))
        assertNull(state.cached("account-b", 2000, epoch))
        assertNull(state.cached("account-a", 999, epoch))
        assertNull(state.cached("account-a", 241000, epoch))
    }
    @Test fun logoutFencesDelayedTokenAndCookieResponses() {
        val state = WebSessionMemory(); val old = state.generation()
        state.cache("a", "dummy", 1, old); state.seed("a", "dummy", "old-session", old)
        state.invalidate()
        assertFalse(state.cache("a", "late-dummy", 2, old))
        assertNull(state.cached("a", 3, state.generation()))
        assertTrue(state.snapshot(state.generation()).isEmpty())
        assertThrows(IllegalStateException::class.java) { state.absorb(listOf("sessionid=late"), old) }
        assertThrows(IllegalStateException::class.java) { state.snapshot(old) }
    }
    @Test fun currentCookieSnapshotUsesUpdatedSession() {
        val state = WebSessionMemory(); val epoch = state.generation()
        state.seed("a", "dummy", "initial", epoch)
        state.absorb(listOf("sessionid=updated; Secure; Path=/"), epoch)
        assertEquals("updated", state.snapshot(epoch)["sessionid"])
    }
}
