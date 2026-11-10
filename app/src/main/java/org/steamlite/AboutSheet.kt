package org.steamlite

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutSheet(close: () -> Unit) {
    val context = LocalContext.current
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
    ModalBottomSheet(onDismissRequest = close, containerColor = Surface1,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp), dragHandle = { SheetHandle() }) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            BrandMark(54.dp)
            Spacer(Modifier.height(16.dp))
            Text("关于 Steam Lite", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Text1)
            Text("版本 $version", fontSize = 12.sp, color = Text3, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
            SettingsGroup { SettingsRow(Glyph.Mark, "项目仓库", "用浏览器打开 GitHub", showDivider = false,
                onClick = {
                    val browser = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/zianyx6/steamlite"))
                    browser.selector = android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_APP_BROWSER)
                    runCatching { context.startActivity(browser) }.onFailure {
                        android.widget.Toast.makeText(context, "请先安装浏览器", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }) }
            Text("查看源码、提交反馈和下载安装包", fontSize = 11.sp, color = Text3, modifier = Modifier.padding(vertical = 16.dp))
            GhostButton("关闭", close, Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))
        }
    }
}
