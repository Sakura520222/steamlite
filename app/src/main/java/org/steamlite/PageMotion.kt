package org.steamlite

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Animate a single page instance so navigation does not duplicate network effects. */
@Composable
internal fun PageMotion(key: Any, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val progress = remember { Animatable(1f) }
    val distance = with(LocalDensity.current) { 7.dp.toPx() }
    LaunchedEffect(key) { progress.snapTo(.55f); progress.animateTo(1f, tween(170)) }
    Box(modifier.graphicsLayer { alpha = progress.value; translationY = (1f - progress.value) * distance }) { content() }
}
