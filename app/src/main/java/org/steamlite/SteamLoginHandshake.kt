package org.steamlite

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Callers hold this monitor while changing the login state and committing its side effects. */
internal class SteamLoginHandshake {
    class Credentials(val account: String, val token: String)
    class Transport
    class Logon(val credentials: Credentials, val persist: Boolean)

    var transport: Transport? = null
        private set
    private val closingState = MutableStateFlow(false)
    val closing = closingState.asStateFlow()
    var approved: Credentials? = null
        private set
    private var submitted: Logon? = null

    fun openTransport(): Transport {
        check(transport == null)
        return Transport().also { transport = it }
    }

    fun closeTransport(): Transport? {
        if (closing.value) return null
        val current = transport ?: return null
        submitted = null
        closingState.value = true
        return current
    }

    fun disconnected(expected: Transport? = transport): Boolean {
        if (transport !== expected) return false
        transport = null
        closingState.value = false
        submitted = null
        return true
    }

    fun approve(credentials: Credentials) {
        approved = credentials
    }

    fun submit(saved: Credentials, send: (Credentials) -> Unit) {
        check(transport != null && !closing.value && submitted == null)
        val fresh = approved
        val credentials = fresh ?: saved
        val logon = Logon(credentials, persist = fresh != null)
        submitted = logon
        try {
            send(credentials)
        } catch (error: Throwable) {
            if (submitted === logon) submitted = null
            throw error
        }
    }

    fun <T> complete(accept: (Logon) -> T): T? {
        val logon = submitted ?: return null
        submitted = null
        approved = null
        return accept(logon)
    }

    fun discardApproval() {
        approved = null
        submitted = null
    }
}
