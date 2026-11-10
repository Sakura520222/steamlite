package org.steamlite
import org.junit.Assert.*
import org.junit.Test
class ObservedPriceTest {
    @Test fun firstObservationDoesNotClaimNewLow() { assertEquals(ObservedPriceChange.FIRST, compareObservedPrice(100, null)) }
    @Test fun distinguishesAllObservedComparisons() {
        assertEquals(ObservedPriceChange.LOWER, compareObservedPrice(50, 100))
        assertEquals(ObservedPriceChange.EQUAL, compareObservedPrice(100, 100))
        assertEquals(ObservedPriceChange.HIGHER, compareObservedPrice(150, 100))
        assertEquals(ObservedPriceChange.LOWER, compareObservedPrice(0, 100))
    }
    @Test fun rejectsInvalidPrice() { assertThrows(IllegalArgumentException::class.java) { compareObservedPrice(-1, 100) } }
}
