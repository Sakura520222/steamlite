package org.steamlite

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class PriceRateLimited(val retryAfterMillis: Long = 0) : Exception("Steam 价格查询暂时受限")

/** All StoreBrowse consumers share pacing and cooldown, including library valuation. */
internal class PriceRequestGate(
    private val now: () -> Long = { System.nanoTime() / 1_000_000 },
    private val pause: suspend (Long) -> Unit = { delay(it) },
    private val spacing: Long = 2000
) {
    private val mutex = Mutex()
    private var next = 0L
    private var blockedUntil = 0L
    private var failures = 0

    suspend fun <T> request(fetch: suspend () -> T): T = mutex.withLock {
        val remaining = blockedUntil - now()
        if (remaining > 0) throw PriceRateLimited(remaining)
        val wait = next - now()
        if (wait > 0) pause(wait)
        next = now() + spacing
        try {
            fetch().also { failures = 0 }
        } catch (e: PriceRateLimited) {
            val cooldown = maxOf(e.retryAfterMillis, minOf(900_000L, 60_000L shl minOf(failures++, 4)))
            blockedUntil = now() + cooldown
            throw PriceRateLimited(cooldown)
        }
    }
}
