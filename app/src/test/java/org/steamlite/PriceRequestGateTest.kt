package org.steamlite

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PriceRequestGateTest {
    @Test fun consumersShareSpacingBetweenRequests() = runBlocking {
        var now = 0L
        val gate = PriceRequestGate({ now }, { now += it })
        val starts = mutableListOf<Long>()
        repeat(3) { gate.request { starts += now } }
        assertEquals(listOf(0L, 2000L, 4000L), starts)
    }
    @Test fun rateLimitStopsAllConsumersAndBacksOffRepeatedFailures() = runBlocking {
        var now = 0L
        var calls = 0
        val gate = PriceRequestGate({ now }, { now += it })
        suspend fun fail(): Long = try {
            gate.request { calls++; throw PriceRateLimited() }
        } catch (e: PriceRateLimited) { e.retryAfterMillis }
        assertEquals(60_000L, fail())
        now = 1000
        assertEquals(59_000L, fail())
        assertEquals(1, calls)
        now = 60_000
        assertEquals(120_000L, fail())
        assertEquals(2, calls)
        now = 180_000
        assertEquals("recovered", gate.request { "recovered" })
        now += 2000
        assertEquals(60_000L, fail())
    }
    @Test fun cancellationReleasesTheGateWithoutStartingACooldown() = runBlocking {
        var now = 0L
        val gate = PriceRequestGate({ now }, { now += it })
        try { gate.request { throw CancellationException("left page") }; fail("expected cancellation") }
        catch (_: CancellationException) { }
        assertEquals(7, gate.request { 7 })
        assertEquals(2000L, now)
    }
}
