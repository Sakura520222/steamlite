package org.steamlite

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.*

@Composable fun ImageAttachmentButton(model: LiteModel, key: String, enabled: Boolean, content: (@Composable (onClick: () -> Unit) -> Unit)? = null) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    val people by model.steam.people.collectAsState(); val groups by model.community.groups.collectAsState()
    var uri by remember(key) { mutableStateOf<Uri?>(null) }
    var image by remember(key) { mutableStateOf<UploadImage?>(null) }
    var owner by remember(key) { mutableStateOf("") }
    var busy by remember(key) { mutableStateOf(false) }
    var status by remember(key) { mutableStateOf("") }
    var uncertain by remember(key) { mutableStateOf(false) }
    val destination = key.split(':')
    val recipient = if(destination.firstOrNull() == "friend") people.find { it.id.toString() == destination.getOrNull(1) }?.name ?: "当前好友"
        else groups.find { it.id.toString() == destination.getOrNull(1) }?.let { g -> g.name + " / " + g.channels.find { it.id.toString() == destination.getOrNull(2) }?.name.orEmpty() } ?: "当前群组频道"
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { chosen -> if(chosen != null) {
        uri = chosen; image = null; status = "正在读取图片"; uncertain = false
        scope.launch { try { image = ChatUploadPolicy.read(context, chosen); status = "确认后将上传至 Steam 并发送给此会话" }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { status = e.message ?: "读取图片失败" }
        }
    } }
    val open = { if(enabled && !busy) { owner = model.steam.owner; pick.launch("image/*") } }
    if(content != null) content(open)
    else Text("图片", color = if(enabled) Accent else Text3, fontSize = 12.sp,
        modifier = Modifier.clickable(enabled = enabled && !busy, onClick = open).padding(8.dp))
    if(uri != null) AlertDialog(
        onDismissRequest = { if(!busy) { uri = null; image = null } },
        containerColor = Surface1,
        title = { Text("发送给 $recipient", color = Text1) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AsyncImage(uri, "待发送图片", Modifier.fillMaxWidth().height(200.dp))
            image?.let { Text("${it.width} × ${it.height} · %.1f MB".format(it.bytes.size / 1048576.0), color = Text3, fontSize = 11.sp) }
            Text(status, color = if(uncertain) WarnText else Text2, fontSize = 12.sp)
            if(busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Accent)
        } },
        confirmButton = { TextButton(enabled = enabled && image != null && !busy && !uncertain && owner == model.steam.owner, onClick = {
            val selected = image ?: return@TextButton
            busy = true; status = "正在发送图片，请勿重复操作"
            scope.launch {
                try {
                    model.imageUploader.upload(key, selected, owner)
                    uri = null; image = null
                    if(destination[0] == "friend") { delay(700); model.steam.syncRecentMessages(destination[1].toLong()) }
                    else { delay(700); model.community.selectChannel(destination[1].toLong(), destination[2].toLong()) }
                } catch(e: CancellationException) { throw e }
                catch(e: Exception) {
                    android.util.Log.w("SteamLiteUpload", "upload failure type=${e.javaClass.simpleName}")
                    status = when(e) {
                        is IllegalStateException -> e.message.orEmpty()
                        is java.io.IOException -> "图片上传连接失败（${e.javaClass.simpleName}），请检查模拟器代理"
                        is org.json.JSONException -> "Steam 图片响应字段不兼容，未确认发送成功"
                        else -> "图片发送失败（${e.javaClass.simpleName}）"
                    }
                    uncertain = status.contains("结果未确认")
                }
                finally { busy = false }
            }
        }) { Text(if(busy) "发送中" else "发送图片", color = Accent) } },
        dismissButton = { TextButton(enabled = !busy, onClick = { uri = null; image = null }) { Text(if(uncertain) "关闭并核对会话" else "取消", color = Text3) } }
    )
}
