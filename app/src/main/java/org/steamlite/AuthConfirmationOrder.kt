package org.steamlite

import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesAuthSteamclient.CAuthentication_AllowedConfirmation
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesAuthSteamclient.EAuthSessionGuardType

/** Phone approval and token codes are alternatives, not consecutive login steps. */
internal fun preferPhoneApproval(choices: List<CAuthentication_AllowedConfirmation>): List<CAuthentication_AllowedConfirmation> =
    choices.sortedBy { when (it.confirmationType) {
        EAuthSessionGuardType.k_EAuthSessionGuardType_None -> 0
        EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceConfirmation -> 1
        EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceCode -> 2
        EAuthSessionGuardType.k_EAuthSessionGuardType_EmailCode -> 3
        else -> 4
    } }

internal fun supportsConcurrentGuard(choices: List<CAuthentication_AllowedConfirmation>): Boolean {
    val types = choices.map { it.confirmationType }.toSet()
    return EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceConfirmation in types &&
        EAuthSessionGuardType.k_EAuthSessionGuardType_DeviceCode in types &&
        EAuthSessionGuardType.k_EAuthSessionGuardType_None !in types
}
