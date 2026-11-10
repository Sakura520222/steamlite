package org.steamlite

import org.junit.Assert.assertEquals
import org.junit.Test

class LoginDialogStateTest {
    @Test fun codeSubmissionKeepsDialogUntilVerificationCompletes() {
        assertEquals(LoginDialogState.GUARD, loginDialogState(false, true, "输入 Steam Guard 验证码", false))
        assertEquals(LoginDialogState.GUARD, loginDialogState(false, true, "正在验证验证码", false))
        assertEquals(LoginDialogState.GUARD, loginDialogState(false, true, "验证码错误，请重新输入", false))
    }
    @Test fun successRequiresAuthenticatedConnection() {
        assertEquals(LoginDialogState.NONE, loginDialogState(false, true, "", true))
        assertEquals(LoginDialogState.SUCCESS, loginDialogState(true, false, "", true))
        assertEquals(LoginDialogState.NONE, loginDialogState(true, false, "", false))
    }
    @Test fun cancellationFailureAndInitialConnectionDoNotLeaveGuardDialogOpen() {
        assertEquals(LoginDialogState.NONE, loginDialogState(false, false, "旧验证码", false))
        assertEquals(LoginDialogState.NONE, loginDialogState(false, true, "", false))
    }
}
