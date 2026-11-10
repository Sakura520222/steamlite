package org.steamlite

import org.junit.Assert.*
import org.junit.Test

class VoiceJoinGateTest {
    @Test fun cannotRingBeforeBothMediaAndSteamSessionAreReady() {
        val gate = VoiceJoinGate(); assertFalse(gate.canRequest())
        gate.ice = true; assertFalse(gate.canRequest())
        gate.session = true; assertTrue(gate.canRequest())
        gate.requested = true; assertFalse(gate.canRequest())
    }
    @Test fun cannotBindAudioBeforePeerAcceptsOrBindTwice() {
        val gate = VoiceJoinGate(); gate.ice = true; gate.session = true
        assertFalse(gate.canBind()); gate.accepted = true; assertTrue(gate.canBind())
        gate.bound = true; assertFalse(gate.canBind())
    }
}
