package org.steamlite

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

internal class FriendSwipeState {
    var peer by mutableStateOf<Long?>(null)
        private set
    fun open(id: Long) { peer = id }
    fun close(id: Long) { if (peer == id) peer = null }
    fun clear() { peer = null }
}

internal val LocalFriendSwipeState = compositionLocalOf<FriendSwipeState> { error("Missing friend swipe state") }

/** Horizontal gestures reveal four actions; vertical scrolling stays with the list. */
@Composable
internal fun FriendSwipeRow(model: LiteModel, friend: FriendRow, pinned: Boolean, enabled: Boolean, content: @Composable () -> Unit) {
    val width = with(LocalDensity.current) { 248.dp.toPx() }
    var target by remember(friend.id) { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val menus = LocalFriendSwipeState.current
    val animated by animateFloatAsState(target, spring(dampingRatio = .88f, stiffness = 500f), label = "friend actions")
    val offset = if (menus.peer != friend.id) 0f else if (dragging) target else animated
    var action by remember(friend.id) { mutableStateOf<String?>(null) }
    var value by remember(friend.id, friend.remark) { mutableStateOf(friend.remark) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current
    fun select(selected: String) { target = 0f; menus.close(friend.id); action = selected; value = friend.remark; error = "" }
    fun confirm() {
        val selected = action ?: return
        if (busy) return
        busy = true; error = ""
        scope.launch {
            try {
                model.steam.editFriend(friend.id, selected, value.trim())
                action = null
                snackbar.showSnackbar(when(selected) { "remark" -> "备注已更新"; "block" -> "已屏蔽好友"; else -> "已删除好友" })
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "操作未确认，请稍后检查" }
            finally { busy = false }
        }
    }
    Box(Modifier.fillMaxWidth().clipToBounds()) {
        if (abs(offset) > 1f) Row(
            Modifier.width(248.dp).height(66.dp).align(if (offset > 0) Alignment.CenterStart else Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            SwipeAction(if (pinned) "取消置顶" else "置顶", Glyph.Pin, Accent, enabled) { model.steam.toggleFavorite(friend.id); target = 0f; menus.close(friend.id) }
            SwipeAction("备注", Glyph.User, Accent, enabled) { select("remark") }
            SwipeAction("屏蔽", Glyph.Shield, Warn, enabled) { select("block") }
            SwipeAction("删除好友", Glyph.Close, Danger, enabled) { select("delete") }
        }
        Box(Modifier.fillMaxWidth().offset { IntOffset(offset.roundToInt(), 0) }.background(Bg)
            .semantics { customActions = listOf(CustomAccessibilityAction("显示好友操作") { menus.open(friend.id); target = -width; true }) }
            .draggable(rememberDraggableState { delta -> target = (target + delta).coerceIn(-width, width) }, Orientation.Horizontal,
                onDragStarted = { if (menus.peer != friend.id) target = 0f; menus.open(friend.id); dragging = true },
                onDragStopped = { dragging = false; target = if (abs(target) > width * .22f) { if (target > 0) width else -width } else 0f; if (target == 0f) menus.close(friend.id) })) {
            content()
        }
    }
    if (action != null) AlertDialog(
        onDismissRequest = { if (!busy) action = null },
        containerColor = Surface1,
        title = { Text(when(action) { "remark" -> "好友备注"; "block" -> "屏蔽好友"; else -> "删除好友" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(friend.name, color = Text2)
                if (action == "remark") {
                    OutlinedTextField(value, { value = it.filterNot(Char::isISOControl).take(32) }, Modifier.fillMaxWidth(),
                        enabled = !busy, singleLine = true, placeholder = { Text("输入备注，留空可清除") }, shape = RoundedCornerShape(12.dp))
                } else Text(if (action == "block") "确认在 Steam 中屏蔽此好友？" else "确认从 Steam 好友列表删除此人？重新添加需要再次发送好友邀请。", color = Text2)
                if (error.isNotBlank()) Text(error, color = Danger, fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(onClick = { confirm() }, enabled = !busy) {
            Text(if (busy) "处理中…" else if (action == "remark") "保存" else "确认", color = if (action == "delete") Danger else Accent)
        } },
        dismissButton = { TextButton(onClick = { action = null }, enabled = !busy) { Text("取消") } }
    )
}

@Composable
private fun RowScope.SwipeAction(label: String, glyph: Glyph, color: Color, enabled: Boolean, click: () -> Unit) {
    Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = .13f))
        .clickable(enabled = enabled, onClick = click).padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        SteamIcon(glyph, 20.dp, color)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 9.sp, color = color, maxLines = 1)
    }
}
