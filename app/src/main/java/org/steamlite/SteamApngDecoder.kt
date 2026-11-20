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

private const val MAX_APNG_EDGE = 4096
private const val MAX_APNG_PIXELS = 4L * 1024 * 1024
private const val PNG_SIGNATURE = -0x76afb1b8f2f5e5f6L
private const val IHDR = 0x49484452
private const val FCTL = 0x6663544c
private const val IEND = 0x49454e44

internal fun checkApngDimensions(bytes: ByteArray) {
    val input = ByteBuffer.wrap(bytes)
    check(input.remaining() >= 33 && input.long == PNG_SIGNATURE &&
        input.int == 13 && input.int == IHDR) { "Invalid APNG header" }
    val width = input.int
    val height = input.int
    check(width in 1..MAX_APNG_EDGE && height in 1..MAX_APNG_EDGE &&
        width.toLong() * height <= MAX_APNG_PIXELS) { "Animated image exceeds dimension limit" }
    input.position(33) // Signature, IHDR chunk and CRC.

    while (input.remaining() >= 12) {
        val length = input.int
        val type = input.int
        check(length in 0..(input.remaining() - 4) && type != IHDR) { "Invalid APNG chunk" }
        val dataStart = input.position()
        if (type == FCTL) {
            check(length == 26) { "Invalid APNG frame header" }
            val frameWidth = input.getInt(dataStart + 4)
            val frameHeight = input.getInt(dataStart + 8)
            val x = input.getInt(dataStart + 12)
            val y = input.getInt(dataStart + 16)
            check(frameWidth in 1..width && frameHeight in 1..height &&
                x in 0..(width - frameWidth) && y in 0..(height - frameHeight)) { "APNG frame exceeds canvas bounds" }
        }
        if (type == IEND) {
            check(length == 0 && input.remaining() == 4) { "Unexpected data after APNG image" }
            return
        }
        input.position(dataStart + length + 4)
    }
    error("Incomplete APNG")
}

/** APNG's animation control chunk precedes the first image-data chunk. */
internal class SteamApngDecoder(private val result: SourceResult) : Decoder {
    override suspend fun decode(): DecodeResult = withContext(Dispatchers.IO) {
        val source = result.source.source()
        val buffer = okio.Buffer()
        while (source.read(buffer, 8192) != -1L) check(buffer.size <= 20L * 1024 * 1024) { "Animated image exceeds size limit" }
        val bytes = buffer.readByteArray()
        checkApngDimensions(bytes)
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
