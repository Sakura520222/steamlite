package org.steamlite

import okio.Buffer
import org.junit.Assert.assertThrows
import org.junit.Test

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

    @Test fun rejectsDataAfterEndChunk() {
        assertThrows(IllegalStateException::class.java) {
            checkApngDimensions(metadata(width = 64, height = 64) + byteArrayOf(0))
        }
    }

    // Only chunk metadata is needed to exercise the pre-decode check.
    private fun metadata(width: Int, height: Int, frameWidth: Int = 1, x: Int = 0): ByteArray =
        Buffer()
            .write(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a))
            .writeInt(13).writeUtf8("IHDR").writeInt(width).writeInt(height)
            .write(byteArrayOf(8, 6, 0, 0, 0)).writeInt(0)
            .writeInt(26).writeUtf8("fcTL").writeInt(0)
            .writeInt(frameWidth).writeInt(1).writeInt(x).writeInt(0)
            .write(ByteArray(6)).writeInt(0)
            .writeInt(0).writeUtf8("IEND").writeInt(0)
            .readByteArray()
}
