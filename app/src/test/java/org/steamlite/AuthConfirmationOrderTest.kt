package org.steamlite

import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesAuthSteamclient.*
import org.junit.Assert.*
import org.junit.Test

class AuthConfirmationOrderTest {
    private fun choice(type: EAuthSessionGuardType) = CAuthentication_AllowedConfirmation.newBuilder().setConfirmationType(type).build()
    @Test fun phoneAndTokenAreEnabledTogetherOnlyWhenSteamOffersBoth() {
        val phone = choice(EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceConfirmation)
        val token = choice(EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceCode)
        assertTrue(supportsConcurrentGuard(listOf(phone, token)))
        assertTrue(supportsConcurrentGuard(listOf(token, phone)))
        assertFalse(supportsConcurrentGuard(listOf(phone)))
        assertFalse(supportsConcurrentGuard(listOf(token)))
    }
    @Test fun alreadyAuthorizedSessionsDoNotOpenAdditionalGuardInputs() {
        val none = choice(EAuthSessionGuardType.k_EAuthSessionGuardType_None)
        val phone = choice(EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceConfirmation)
        val token = choice(EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceCode)
        assertFalse(supportsConcurrentGuard(listOf(none, phone, token)))
    }
    @Test fun availablePhoneApprovalIsChosenInsteadOfRequestingATokenCode() {
        val phone = choice(EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceConfirmation)
        val token = choice(EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceCode)
        val email = choice(EAuthSessionGuardType.k_EAuthSessionGuardType_EmailCode)
        assertEquals(phone, preferPhoneApproval(listOf(token, email, phone)).first())
    }
    @Test fun mandatoryCodeIsStillSupportedWhenPhoneApprovalIsUnavailable() {
        val token = choice(EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceCode)
        assertEquals(listOf(token), preferPhoneApproval(listOf(token)))
        val none = choice(EAuthSessionGuardType.k_EAuthSessionGuardType_None)
        assertEquals(none, preferPhoneApproval(listOf(token, none)).first())
    }
}
