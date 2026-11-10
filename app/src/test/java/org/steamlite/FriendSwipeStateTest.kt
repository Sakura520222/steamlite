package org.steamlite

import org.junit.Assert.*
import org.junit.Test

class FriendSwipeStateTest {
    @Test fun closingAnOlderRowCannotCloseTheNewlyOpenedMenu() {
        val state = FriendSwipeState()
        state.open(1); state.open(2); state.close(1)
        assertEquals(2L, state.peer)
        state.clear(); assertNull(state.peer)
    }
}
