package org.steamlite

import org.junit.Assert.*
import org.junit.Test

class SteamLoginHandshakeTest {
    private val existing = SteamLoginHandshake.Credentials("saved-account", "saved-token")
    private val approved = SteamLoginHandshake.Credentials("new-account", "fresh-token")

    @Test fun cancellationDiscardsApprovalBeforeAnyLogonWasSent() {
        val login = SteamLoginHandshake()
        login.openTransport()
        login.approve(approved)
        login.discardApproval()
        login.closeTransport()

        assertNull(login.approved)
        login.complete { fail("No credentials should be committed") }
    }

    @Test fun lateCallbackAfterCancellationCannotReplaceSavedSession() {
        val login = SteamLoginHandshake()
        val transport = login.openTransport()
        login.approve(approved)
        login.submit(existing) { assertSame(approved, it) }

        login.discardApproval()
        assertSame(transport, login.closeTransport())
        var persisted = existing
        login.complete { persisted = it.credentials }
        assertSame(existing, persisted)
        assertThrows(IllegalStateException::class.java) { login.openTransport() }
        login.disconnected()
        login.openTransport()
        login.complete { fail("No request is pending on the new transport") }
    }

    @Test fun retryKeepsApprovalUntilTheNewTransportCompletes() {
        val login = SteamLoginHandshake()
        login.openTransport()
        login.approve(approved)
        login.submit(existing) { assertSame(approved, it) }
        login.disconnected()

        login.openTransport()
        login.submit(existing) { assertSame(approved, it) }
        var commits = 0
        login.complete {
            assertTrue(it.persist)
            assertSame(approved, it.credentials)
            commits++
        }
        login.complete { fail("A logon response is consumed only once") }
        assertEquals(1, commits)
        assertNull(login.approved)
    }

    @Test fun restoredSessionLogsOnWithoutWritingAReplacement() {
        val login = SteamLoginHandshake()
        login.openTransport()
        login.submit(existing) { assertSame(existing, it) }
        login.complete {
            assertSame(existing, it.credentials)
            assertFalse(it.persist)
        }
    }

    @Test fun retiredTransportCannotClearNewTransportOrItsSubmittedLogon() {
        val login = SteamLoginHandshake()
        val old = login.openTransport()
        login.closeTransport()
        assertTrue(login.disconnected(old))
        val current = login.openTransport()
        login.approve(approved)
        login.submit(existing) {}
        assertFalse(login.disconnected(old))
        assertSame(current, login.transport)
        assertNotNull(login.complete { it.credentials })
    }

    @Test fun synchronousSendFailureAllowsRetryWithTheSameApproval() {
        val login = SteamLoginHandshake()
        login.openTransport()
        login.approve(approved)
        assertThrows(IllegalStateException::class.java) {
            login.submit(existing) { error("socket closed before send") }
        }
        login.complete { fail("A failed send must not consume a later callback") }
        assertSame(approved, login.approved)
        login.submit(existing) { assertSame(approved, it) }
        assertSame(approved, login.complete { it.credentials })
    }
}
