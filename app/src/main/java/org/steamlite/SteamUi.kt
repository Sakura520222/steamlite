package org.steamlite

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/* ============================================================================
 * Design tokens — single source of truth.
 *
 * Rules
 *  - one brand colour (Accent) = "this is interactive"
 *  - Money  = savings only ("-90%", historic low)
 *  - Warn   = something needs attention
 *  - Danger = destructive only
 *  - everything else is a neutral surface or neutral text
 * ========================================================================== */

// ---- surfaces -------------------------------------------------------------
val Bg = Color(0xFF0A0C11)
val Surface1 = Color(0xFF11151C)
val Surface2 = Color(0xFF171C25)
val Surface3 = Color(0xFF1E242F)

// ---- lines ----------------------------------------------------------------
val Line = Color(0x14FFFFFF)          // 8% white hairline
val LineStrong = Color(0x1FFFFFFF)    // 12% white

// ---- text -----------------------------------------------------------------
val Text1 = Color(0xFFE9EEF6)
val Text2 = Color(0xFF9AA5B7)
val Text3 = Color(0xFF6E7A91)

// ---- semantic -------------------------------------------------------------
val Accent = Color(0xFF4C9BF5)
val AccentText = Color(0xFF9DCEFF)
val AccentSoft = Color(0x244C9BF5)
val AccentLine = Color(0x4D4C9BF5)

val Money = Color(0xFF6FD98A)
val MoneyText = Color(0xFF7FE09A)
val MoneySoft = Color(0x226FD98A)
val MoneyLine = Color(0x386FD98A)

val Warn = Color(0xFFE0AE4B)
val WarnText = Color(0xFFE5BE72)
val WarnSoft = Color(0x1FE0AE4B)
val WarnLine = Color(0x2EE0AE4B)

val Danger = Color(0xFFFF7070)
val DangerText = Color(0xFFFFA9A9)
val DangerSoft = Color(0x1FFF7070)

/**
 * Legacy aliases kept so modules outside the redesign keep compiling.
 * Prefer [Accent] / [Money] in new code.
 */
val SteamGreen = Money
val SteamBlue = Accent

// ---- scheme ---------------------------------------------------------------
val SteamColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF04121F),
    primaryContainer = Color(0xFF17324D),
    onPrimaryContainer = Color(0xFFD6E9FF),
    secondary = Money,
    onSecondary = Color(0xFF04121F),
    secondaryContainer = Color(0xFF17324D),
    onSecondaryContainer = Color(0xFFCBEAFF),
    tertiary = Warn,
    onTertiary = Color(0xFF1A1300),
    background = Bg,
    onBackground = Text1,
    surface = Bg,
    onSurface = Text1,
    surfaceVariant = Surface2,
    onSurfaceVariant = Text2,
    surfaceTint = Accent,
    inverseSurface = Text1,
    inverseOnSurface = Bg,
    error = Danger,
    onError = Color(0xFF1A0000),
    errorContainer = DangerSoft,
    onErrorContainer = DangerText,
    outline = Color(0xFF2A3340),
    outlineVariant = Color(0xFF1B2130),
    scrim = Color(0xCC04060A),
    surfaceBright = Surface3,
    surfaceDim = Bg,
    surfaceContainerLowest = Color(0xFF07090D),
    surfaceContainerLow = Surface1,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
    surfaceContainerHighest = Surface3
)

// ---- type -----------------------------------------------------------------
val SteamTypography = Typography(
    displaySmall = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp),
    headlineMedium = TextStyle(fontSize = 25.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.15).sp),
    titleMedium = TextStyle(fontSize = 14.5.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontSize = 13.5.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 11.5.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 10.5.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun SteamTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SteamColors, typography = SteamTypography, content = content)
}

// ---- navigation -----------------------------------------------------------
fun openSteamLink(context: Context, url: String) {
    if (!ChatContent.webUrl(url)) {
        Toast.makeText(context, "无法打开此链接", Toast.LENGTH_SHORT).show(); return
    }
    val target = Uri.parse(url)
    // Try the official Steam client first. Android package visibility does not affect startActivity.
    for (pkg in listOf("com.valvesoftware.android.steam.community", "com.valvesoftware.android.steam.friendsui")) {
        if (runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, target).setPackage(pkg)); true }.getOrDefault(false)) return
    }
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("steam://openurl/$url"))) }.onSuccess { return }
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, target)) }
        .onFailure { Toast.makeText(context, "未找到可以打开链接的应用", Toast.LENGTH_SHORT).show() }
}
