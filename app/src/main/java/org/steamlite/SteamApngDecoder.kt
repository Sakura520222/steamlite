package org.steamlite

import coil.ImageLoader
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.fetch.SourceResult
import coil.request.Options
import com.github.penfeizhou.animation.apng.APNGDrawable
import com.github.penfeizhou.animation.loader.ByteBufferLoader
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** APNG's animation control chunk precedes the first image-data chunk. */
internal class SteamApngDecoder(private val result: SourceResult) : Decoder {
    override suspend fun decode(): DecodeResult = withContext(Dispatchers.IO) {
        val source = result.source.source()
        val buffer = okio.Buffer()
        while (source.read(buffer, 8192) != -1L) check(buffer.size <= 20L * 1024 * 1024) { "Animated image exceeds size limit" }
        val bytes = buffer.readByteArray()
        val drawable = APNGDrawable(object : ByteBufferLoader() {
            override fun getByteBuffer(): ByteBuffer = ByteBuffer.wrap(bytes)
        })
        drawable.setAutoPlay(false)
        DecodeResult(drawable, false)
    }
    class Factory : Decoder.Factory {
        override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder? {
            val peek = result.source.source().peek()
            if (!peek.request(8) || peek.readByteString(8).hex() != "89504e470d0a1a0a") return null
            var scanned = 8L
            while (scanned < 1024 * 1024 && peek.request(8)) {
                val length = peek.readInt().toLong() and 0xffffffffL
                val chunk = peek.readUtf8(4)
                if (chunk == "acTL" && length == 8L) return SteamApngDecoder(result)
                if (chunk == "IDAT" || chunk == "IEND" || length > 1024 * 1024 || !peek.request(length + 4)) return null
                peek.skip(length + 4); scanned += length + 12
            }
            return null
        }
    }
}
