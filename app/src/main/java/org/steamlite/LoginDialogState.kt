package org.steamlite

internal enum class LoginDialogState { NONE, GUARD, SUCCESS }

internal fun loginDialogState(online: Boolean, busy: Boolean, guard: String, succeeded: Boolean): LoginDialogState = when {
    online && succeeded -> LoginDialogState.SUCCESS
    !online && busy && guard.isNotBlank() -> LoginDialogState.GUARD
    else -> LoginDialogState.NONE
}
