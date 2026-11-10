package org.steamlite

import android.content.Context
import coil.Coil
import coil.ImageLoader
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object PublicImages {
    // Public artwork uses the system network/proxy settings, with normal TLS validation.
    // This client never shares the authenticated store session's Cookie jar.
    val http: OkHttpClient by lazy { OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS).build() }
    private var installed = false
    @Synchronized fun install(context: Context) { if(!installed) {
        Coil.setImageLoader(ImageLoader.Builder(context.applicationContext).okHttpClient(http).components {
            add(SteamApngDecoder.Factory())
            if(android.os.Build.VERSION.SDK_INT >= 28) add(coil.decode.ImageDecoderDecoder.Factory()) else add(coil.decode.GifDecoder.Factory())
        }.build()); installed = true
    } }
}
