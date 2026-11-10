package org.steamlite

import org.junit.Assert.*
import org.junit.Test

class PersonaSnapshotTest {
    @Test fun avatarOnlyUpdateDoesNotTurnPlayerOffline() {
        val before = PersonaSnapshot(1, "Game", 123)
        assertEquals(before, before.merge(null, null, null))
    }
    @Test fun exitGameClearsStaleNameEvenWhenNameIsAbsent() {
        assertEquals(PersonaSnapshot(1, "", 0), PersonaSnapshot(1, "Game", 123).merge(null, null, 0))
    }
    @Test fun awayAndGameAreIndependent() {
        assertEquals(PersonaSnapshot(3, "Game", 123), PersonaSnapshot(1, "Game", 123).merge(3, null, null))
    }
    @Test fun offlineClearsGameAndOnlineCanStartNewGame() {
        val offline = PersonaSnapshot(1, "Game", 123).merge(0, null, null)
        assertEquals(PersonaSnapshot(0, "", 0), offline)
        assertEquals(PersonaSnapshot(1, "Other", 456), offline.merge(1, "Other", 456))
    }
}
