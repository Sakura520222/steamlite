package org.steamlite

import android.app.Instrumentation
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.github.penfeizhou.animation.apng.APNGDrawable
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Public images only. No session changes or outgoing messages. */
object AnimatedArtworkCheck {
    fun run(test: Instrumentation): String = runBlocking {
        PublicImages.install(test.targetContext)
        val loader = test.targetContext.imageLoader
        val emote = loader.execute(ImageRequest.Builder(test.targetContext).data(ChatContent.emoteUrl("waooo")).build())
        check(emote is SuccessResult && emote.drawable.intrinsicWidth > 0) { "unowned public emote failed" }
        val result = loader.execute(ImageRequest.Builder(test.targetContext).data(ChatContent.stickerUrl("Beat Mix")).build())
        check(result is SuccessResult && result.drawable is APNGDrawable) { "APNG decoder not selected" }
        val drawable = (result as SuccessResult).drawable as APNGDrawable
        val frames = CountDownLatch(3)
        val handler = Handler(Looper.getMainLooper())
        val callback = object : Drawable.Callback {
            override fun invalidateDrawable(who: Drawable) { frames.countDown() }
            override fun scheduleDrawable(who: Drawable, what: Runnable, whenMillis: Long) { handler.postAtTime(what, whenMillis) }
            override fun unscheduleDrawable(who: Drawable, what: Runnable) { handler.removeCallbacks(what) }
        }
        try {
            test.runOnMainSync { drawable.callback = callback; drawable.setVisible(true, true); drawable.start() }
            check(frames.await(5, TimeUnit.SECONDS)) { "APNG frames did not advance" }
            "PASS: public unowned emote decoded; Steam APNG sticker produced multiple animation frames"
        } finally { test.runOnMainSync { drawable.stop(); drawable.callback = null } }
    }
}
