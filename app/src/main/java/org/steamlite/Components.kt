package org.steamlite

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Snackbar host shared by every screen (used for undo + progress feedback). */
val LocalSnackbar = compositionLocalOf<SnackbarHostState> { SnackbarHostState() }

enum class Tone { Info, Warn, Error }

fun clock(value: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(value))

// ---------------------------------------------------------------- primitives

@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = Line) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
fun AccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glyph: Glyph? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        enabled = enabled,
        shape = RoundedCornerShape(11.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Accent,
            contentColor = Color(0xFF04121F),
            disabledContainerColor = Surface3,
            disabledContentColor = Text3
        ),
        contentPadding = PaddingValues(horizontal = 18.dp)
    ) {
        if (glyph != null) {
            SteamIcon(glyph, 15.dp, if (enabled) Color(0xFF04121F) else Text3, 2f)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glyph: Glyph? = null,
    tint: Color = Text1
) {
    val shape = RoundedCornerShape(11.dp)
    Row(
        modifier
            .height(40.dp)
            .clip(shape)
            .background(Surface2)
            .border(1.dp, LineStrong, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (glyph != null) {
            SteamIcon(glyph, 15.dp, tint, 2f)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

// ---------------------------------------------------------------- app chrome

/**
 * Compact page header. Deliberately small: the title is a signpost, not a hero.
 * No global brand bar — the app identity lives on the「我的」page.
 */
@Composable
fun PageHeader(title: String, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp, color = Text1)
        if (trailing != null) {
            Spacer(Modifier.weight(1f))
            trailing()
        }
    }
}

/** Only shown while the Steam session is down; otherwise the app has no chrome. */
@Composable
fun OfflineBar(status: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(11.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 2.dp)
            .clip(shape)
            .background(WarnSoft)
            .border(1.dp, WarnLine, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(Warn))
        Spacer(Modifier.width(8.dp))
        Text(
            status.ifBlank { "未连接 Steam" },
            Modifier.weight(1f),
            fontSize = 11.5.sp,
            color = WarnText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text("查看", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Accent)
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth().padding(top = 22.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = Text1)
        Spacer(Modifier.weight(1f))
        trailing?.invoke(this)
    }
}

@Composable
fun RowScope.TextLink(text: String, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Accent)
        SteamIcon(Glyph.ChevronRight, 13.dp, Accent, 2.1f)
    }
}

@Composable
fun MetaLine(text: String, glyph: Glyph? = Glyph.Info, color: Color = Text3) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (glyph != null) {
            SteamIcon(glyph, 13.dp, color, 1.8f)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontSize = 11.5.sp, color = color, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

// ---------------------------------------------------------------- controls

@Composable
fun SteamChip(text: String, active: Boolean, count: Int? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(if (active) AccentSoft else Surface1)
            .border(1.dp, if (active) AccentLine else Line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = if (active) AccentText else Text2)
        if (count != null) {
            Spacer(Modifier.width(5.dp))
            Text(count.toString(), fontSize = 11.sp, color = if (active) Color(0x99C9E4FF) else Text3)
        }
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(13.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = Text1, fontSize = 13.5.sp),
        cursorBrush = SolidColor(Accent),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = androidx.compose.ui.text.input.ImeAction.Search
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onSearch = { onSearch?.invoke() }
        ),
        modifier = modifier,
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(shape)
                    .background(Surface1)
                    .border(1.dp, Line, shape)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SteamIcon(Glyph.Search, 17.dp, Text3, 1.8f)
                Spacer(Modifier.width(9.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(placeholder, fontSize = 13.5.sp, color = Text3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    inner()
                }
                if (trailing != null) {
                    Spacer(Modifier.width(6.dp))
                    trailing()
                }
            }
        }
    )
}

@Composable
fun IconSquare(glyph: Glyph, onClick: () -> Unit, tint: Color = Text2, dot: Boolean = false) {
    val shape = RoundedCornerShape(13.dp)
    Box(
        Modifier
            .size(42.dp)
            .clip(shape)
            .background(Surface1)
            .border(1.dp, Line, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        SteamIcon(glyph, 19.dp, tint)
        if (dot) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = 9.dp, end = 9.dp).size(5.dp)
                    .clip(CircleShape).background(Accent)
            )
        }
    }
}

// ---------------------------------------------------------------- feedback

@Composable
fun StatusBanner(
    text: String,
    tone: Tone = Tone.Warn,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    val bg = when (tone) {
        Tone.Warn -> WarnSoft
        Tone.Info -> Surface1
        Tone.Error -> DangerSoft
    }
    val border = when (tone) {
        Tone.Warn -> WarnLine
        Tone.Info -> Line
        Tone.Error -> Color(0x33FF7070)
    }
    val fg = when (tone) {
        Tone.Warn -> WarnText
        Tone.Info -> Text2
        Tone.Error -> DangerText
    }
    val iconTint = when (tone) {
        Tone.Warn -> Warn
        Tone.Info -> Text3
        Tone.Error -> Danger
    }
    val icon = if (tone == Tone.Info) Glyph.Info else Glyph.Alert
    val shape = RoundedCornerShape(11.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .padding(start = 11.dp, end = 11.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SteamIcon(icon, 15.dp, iconTint, 1.9f)
        Spacer(Modifier.width(9.dp))
        Text(text, Modifier.weight(1f), fontSize = 12.sp, color = fg, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (!action.isNullOrBlank() && onAction != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                action,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Accent,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onAction).padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun EmptyState(
    glyph: Glyph,
    title: String,
    body: String,
    tone: Tone = Tone.Info,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    extra: (@Composable () -> Unit)? = null
) {
    Column(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 64.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(
                    Brush.linearGradient(
                        if (tone == Tone.Warn) listOf(Color(0x33E0AE4B), Color(0x0DE0AE4B))
                        else listOf(Color(0x334C9BF5), Color(0x0D4C9BF5))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            SteamIcon(glyph, 26.dp, if (tone == Tone.Warn) WarnText else Color(0xFF86BEFA), 1.5f)
        }
        Spacer(Modifier.height(18.dp))
        Text(title, fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = Text1, textAlign = TextAlign.Center)
        Spacer(Modifier.height(7.dp))
        Text(
            body,
            fontSize = 12.5.sp,
            color = Text3,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.widthIn(max = 260.dp)
        )
        if (!action.isNullOrBlank() && onAction != null) {
            Spacer(Modifier.height(20.dp))
            GhostButton(action, onAction, glyph = Glyph.Refresh)
        }
        if (extra != null) {
            Spacer(Modifier.height(12.dp))
            extra()
        }
    }
}

@Composable
fun SteamSnackbar(data: SnackbarData) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(shape)
            .background(Color(0xFF1B212B))
            .border(1.dp, LineStrong, shape)
            .padding(start = 14.dp, end = 10.dp, top = 11.dp, bottom = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SteamIcon(Glyph.Check, 16.dp, Money, 2f)
        Spacer(Modifier.width(10.dp))
        Text(data.visuals.message, Modifier.weight(1f), fontSize = 12.5.sp, color = Text1, maxLines = 2)
        val label = data.visuals.actionLabel
        if (!label.isNullOrBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = Accent,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { data.performAction() }.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

// ---------------------------------------------------------------- rows

@Composable
fun PriceLine(price: String, discount: Int, original: String = "") {
    Row(Modifier.height(20.dp), verticalAlignment = Alignment.CenterVertically) {
        if (discount > 0) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(MoneySoft)
                    .border(1.dp, MoneyLine, RoundedCornerShape(5.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text("-$discount%", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MoneyText)
            }
            Spacer(Modifier.width(7.dp))
        }
        Text(
            price,
            fontSize = if (discount > 0) 15.sp else 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (price.isBlank() || price == "暂无报价" || price == "价格不可用") Text3 else Text1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (discount > 0 && original.isNotBlank()) {
            Spacer(Modifier.width(6.dp))
            Text(
                original,
                fontSize = 11.5.sp,
                color = Text3,
                maxLines = 1,
                textDecoration = TextDecoration.LineThrough
            )
        }
    }
}

@Composable
fun GameRow(
    gameId: Int,
    image: String,
    title: String,
    meta: String,
    price: String,
    discount: Int,
    original: String,
    onClick: () -> Unit,
    showDivider: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PosterCover(gameId, image, Modifier.size(54.dp, 72.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = Text1, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text(meta, fontSize = 11.5.sp, color = Text3, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(7.dp))
                PriceLine(price, discount, original)
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
        if (showDivider) Hairline()
    }
}

@Composable
fun HeartButton(on: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        SteamIcon(Glyph.Heart, 18.dp, if (on) Accent else Text3, 2f, filled = on)
    }
}

enum class Presence { Playing, Online, Away, Offline }

@Composable
fun PresenceColor(presence: Presence): Color = when (presence) {
    Presence.Playing -> Money
    Presence.Online -> Money
    Presence.Away -> Warn
    Presence.Offline -> Color(0xFF4C566A)
}

@Composable
fun PersonAvatar(initial: String, avatarUrl: String?, presence: Presence?, size: Dp = 44.dp) {
    val shape = RoundedCornerShape(size * 0.32f)
    Box(Modifier.size(size)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Brush.linearGradient(listOf(Color(0xFF232B38), Color(0xFF1A212C))))
                .border(1.dp, Line, shape),
            contentAlignment = Alignment.Center
        ) {
            Text(initial.take(1), fontSize = (size.value * 0.36f).sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFA9B6C9))
            if (!avatarUrl.isNullOrBlank()) {
                SteamImage(listOf(avatarUrl), "", Modifier.fillMaxSize())
            }
        }
        if (presence != null) {
            Box(
                Modifier.align(Alignment.BottomEnd).size(12.dp).clip(CircleShape).background(Bg),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(PresenceColor(presence)))
            }
        }
    }
}

@Composable
fun PersonRow(
    name: String,
    subtitle: String,
    initial: String,
    avatarUrl: String?,
    presence: Presence?,
    onClick: () -> Unit,
    showDivider: Boolean = true,
    end: (@Composable () -> Unit)? = null,
    subtitleContent: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    remark: String = ""
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if(leading != null) { leading(); Spacer(Modifier.width(8.dp)) }
            PersonAvatar(initial, avatarUrl, presence)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(androidx.compose.ui.text.buildAnnotatedString {
                    append(name)
                    if (remark.isNotBlank()) {
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = Text3, fontWeight = FontWeight.Normal))
                        append("（$remark）"); pop()
                    }
                }, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = Text1, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                if (subtitleContent != null) subtitleContent()
                else Text(subtitle, fontSize = 11.5.sp, color = Text3, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (end != null) end()
        }
        if (showDivider) Hairline()
    }
}

// ---------------------------------------------------------------- cards

@Composable
fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth().padding(top = 12.dp).clip(shape).background(Surface1).border(1.dp, Line, shape),
        content = content
    )
}

@Composable
fun SettingsRow(
    glyph: Glyph,
    title: String,
    value: String? = null,
    danger: Boolean = false,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val fg = if (danger) DangerText else Text1
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(28.dp).clip(RoundedCornerShape(9.dp))
                    .background(if (danger) DangerSoft else Surface3),
                contentAlignment = Alignment.Center
            ) {
                SteamIcon(glyph, 15.dp, if (danger) Danger else Text2, 1.8f)
            }
            Spacer(Modifier.width(12.dp))
            Text(title, Modifier.weight(1f), fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (value != null) {
                Text(value, fontSize = 12.sp, color = Text3, maxLines = 1)
                Spacer(Modifier.width(6.dp))
            }
            if (onClick != null) SteamIcon(Glyph.ChevronRight, 15.dp, Text3, 2f)
        }
        if (showDivider) Hairline(Modifier.padding(horizontal = 14.dp))
    }
}

@Composable
fun StatCell(label: String, value: String, unit: String?, sub: String?, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
        Text(label, fontSize = 11.sp, color = Text3, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Text1, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (unit != null) {
                Text(unit, fontSize = 11.sp, color = Text3, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
            }
        }
        if (sub != null) {
            Spacer(Modifier.height(3.dp))
            Text(sub, fontSize = 10.5.sp, color = Text3, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ---------------------------------------------------------------- navigation

@Composable
fun SteamBottomBar(selected: Int, hasUnreadMessages: Boolean = false, onSelect: (Int) -> Unit) {
    val entries = listOf(
        Triple(0, "商店", Glyph.Store),
        Triple(1, "愿望单", Glyph.Heart),
        Triple(4, "动态", Glyph.Bell),
        Triple(2, "消息", Glyph.Chat),
        Triple(3, "我的", Glyph.User)
    )
    Column(
        Modifier
            .fillMaxWidth()
            .background(Bg)
            .drawBehind {
                drawLine(Line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
            }
            .navigationBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 6.dp)) {
            entries.forEach { (index, label, glyph) ->
                val active = index == selected
                val activeColor by animateColorAsState(if (active) AccentSoft else Color.Transparent, tween(150), label = "navigation selection")
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        Modifier
                            .width(44.dp)
                            .height(26.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(activeColor),
                        contentAlignment = Alignment.Center
                    ) {
                        SteamIcon(glyph, 20.dp, if (active) Accent else Text3, if (active) 2f else 1.75f)
                        if (index == 2 && hasUnreadMessages) Box(Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 4.dp).size(6.dp).background(Accent, CircleShape))
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (active) Accent else Text3
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- sheet

@Composable
fun SheetHandle() {
    Box(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 14.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(34.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x29FFFFFF)))
    }
}

@Composable
fun SheetClose(onClose: () -> Unit) {
    Box(
        Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Surface3).clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        SteamIcon(Glyph.Close, 15.dp, Text2, 2.2f)
    }
}

@Composable
fun UnderlineTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, badge: Int = 0) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
            labels.forEachIndexed { index, label ->
                val active = index == selected
                Row(
                    Modifier
                        .clickable { onSelect(index) }
                        .padding(end = 26.dp, bottom = 10.dp)
                        .drawBehind {
                            if (active) {
                                val y = size.height - 1.dp.toPx()
                                drawLine(
                                    color = Accent,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 2.5.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        },
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        label,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) Text1 else Text3
                    )
                    if (index == 1 && badge > 0) {
                        Box(
                            Modifier.padding(start = 4.dp, bottom = 4.dp).size(5.dp)
                                .clip(CircleShape).background(Accent)
                        )
                    }
                }
            }
        }
        Hairline()
    }
}
