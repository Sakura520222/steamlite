package org.steamlite

import okio.Buffer
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream

class SteamApngDimensionsTest {
    @Test fun acceptsCanvasAtPixelLimitAndAFrameInsideIt() {
        checkApngDimensions(metadata(width = 4096, height = 1024, frameWidth = 64, x = 4032))
    }

    @Test fun rejectsCanvasesOverEitherDimensionBudget() {
        assertThrows(IllegalStateException::class.java) {
            checkApngDimensions(metadata(width = 4096, height = 1025))
        }
        assertThrows(IllegalStateException::class.java) {
            checkApngDimensions(metadata(width = 4097, height = 1))
        }
    }

    @Test fun rejectsFrameOutsideCanvasEvenWhenItsDimensionsAloneAreAllowed() {
        assertThrows(IllegalStateException::class.java) {
            checkApngDimensions(metadata(width = 64, height = 64, frameWidth = 64, x = 1))
        }
    }

    @Test fun rejectsTruncatedControlChunk() {
        val valid = metadata(width = 64, height = 64)
        assertThrows(IllegalStateException::class.java) {
            checkApngDimensions(valid.copyOf(50))
        }
    }

    @Test fun rejectsControlChunkWithWrongLength() {
        assertThrows(IllegalStateException::class.java) {
            // The length fits the stream, so only the strict 26-byte frame header rule rejects it.
            checkApngDimensions(metadata(width = 64, height = 64, controlLength = 20))
        }
    }

    @Test fun rejectsDataAfterEndChunk() {
        assertThrows(IllegalStateException::class.java) {
            checkApngDimensions(metadata(width = 64, height = 64) + byteArrayOf(0))
        }
    }

    @Test fun acceptsCompleteApngStream() {
        // A real APNG carries acTL and IDAT before IEND; the pre-decode check must accept it.
        checkApngDimensions(completeApng(width = 16, height = 16))
    }

    // Only chunk metadata is needed to exercise the pre-decode check.
    private fun metadata(width: Int, height: Int, frameWidth: Int = 1, x: Int = 0, controlLength: Int = 26): ByteArray =
        Buffer()
            .write(pngSignature())
            .write(chunk("IHDR", ihdrPayload(width, height)))
            .write(chunk("fcTL", fcTLPayload(frameWidth, 1, x, 0).copyOf(controlLength)))
            .write(chunk("IEND", ByteArray(0)))
            .readByteArray()

    /** Full single-frame APNG: signature, IHDR, acTL, fcTL, IDAT and a final IEND. */
    private fun completeApng(width: Int, height: Int): ByteArray {
        val raw = ByteArray(height * (1 + width * 4)) // Filter type 0 plus RGBA pixels.
        val idat = ByteArrayOutputStream().also { out -> DeflaterOutputStream(out).use { it.write(raw) } }.toByteArray()
        return Buffer()
            .write(pngSignature())
            .write(chunk("IHDR", ihdrPayload(width, height)))
            .write(chunk("acTL", Buffer().writeInt(1).writeInt(0).readByteArray()))
            .write(chunk("fcTL", fcTLPayload(width, height, 0, 0)))
            .write(chunk("IDAT", idat))
            .write(chunk("IEND", ByteArray(0)))
            .readByteArray()
    }

    private fun pngSignature() = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)

    private fun ihdrPayload(width: Int, height: Int) =
        Buffer().writeInt(width).writeInt(height).write(byteArrayOf(8, 6, 0, 0, 0)).readByteArray()

    private fun fcTLPayload(frameWidth: Int, frameHeight: Int, x: Int, y: Int) =
        Buffer().writeInt(0).writeInt(frameWidth).writeInt(frameHeight).writeInt(x).writeInt(y)
            .writeShort(1).writeShort(1).write(byteArrayOf(0, 0)).readByteArray()

    private fun chunk(type: String, payload: ByteArray): ByteArray {
        val crc = CRC32()
        crc.update(type.toByteArray(Charsets.US_ASCII))
        crc.update(payload)
        return Buffer()
            .writeInt(payload.size).writeUtf8(type).write(payload)
            .writeInt(crc.value.toInt())
            .readByteArray()
    }
}
