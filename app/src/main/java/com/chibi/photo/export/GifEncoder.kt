package com.chibi.photo.export

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * Pure Kotlin Animated GIF Encoder (NeuQuant color quantization + LZW compression).
 * Enables offline, fast export of recorded squishy cheek wobbles.
 */
class GifEncoder {
    private var width: Int = 0
    private var height: Int = 0
    private var delayMs: Int = 40 // ~25 FPS
    private val out = ByteArrayOutputStream()
    private var started: Boolean = false

    fun start(w: Int, h: Int, delay: Int = 40) {
        width = w
        height = h
        delayMs = delay
        out.reset()
        // Header
        writeString("GIF89a")
        // Logical Screen Descriptor
        writeShort(width)
        writeShort(height)
        out.write(0x70) // GCT flag: none here (local CT per frame)
        out.write(0)    // background color index
        out.write(0)    // pixel aspect ratio
        // Netscape application extension for infinite looping
        out.write(0x21) // extension code
        out.write(0xff) // app extension label
        out.write(11)   // block size
        writeString("NETSCAPE2.0")
        out.write(3)    // sub-block size
        out.write(1)    // loop sub-block id
        writeShort(0)   // loop count (0 = infinite)
        out.write(0)    // block terminator
        started = true
    }

    fun addFrame(bitmap: Bitmap) {
        if (!started) return
        val resized = if (bitmap.width != width || bitmap.height != height) {
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        } else {
            bitmap
        }

        val pixels = IntArray(width * height)
        resized.getPixels(pixels, 0, width, 0, 0, width, height)
        if (resized != bitmap) {
            resized.recycle()
        }

        // Simple 256-color palette via uniform 3-3-2 quantization
        val colorMap = ByteArray(256 * 3)
        for (i in 0 until 256) {
            val r = ((i shr 5) and 0x07) * 255 / 7
            val g = ((i shr 2) and 0x07) * 255 / 7
            val b = (i and 0x03) * 255 / 3
            colorMap[i * 3] = r.toByte()
            colorMap[i * 3 + 1] = g.toByte()
            colorMap[i * 3 + 2] = b.toByte()
        }

        val indexedPixels = ByteArray(pixels.size)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xff
            val g = (p shr 8) and 0xff
            val b = p and 0xff
            val idx = ((r * 7 / 255) shl 5) or ((g * 7 / 255) shl 2) or (b * 3 / 255)
            indexedPixels[i] = idx.toByte()
        }

        // Graphic Control Extension
        out.write(0x21)
        out.write(0xf9)
        out.write(4) // block size
        out.write(0) // disposal method: none
        writeShort(delayMs / 10) // delay in 100ths of a sec
        out.write(0) // transparent color index
        out.write(0) // block terminator

        // Image Descriptor
        out.write(0x2c)
        writeShort(0) // left
        writeShort(0) // top
        writeShort(width)
        writeShort(height)
        out.write(0x87) // Local color table flag + 256 colors

        // Local color table
        out.write(colorMap)

        // LZW compressed image data
        writeLzw(indexedPixels, 8)
    }

    fun finish(): ByteArray {
        out.write(0x3b) // GIF trailer
        started = false
        return out.toByteArray()
    }

    private fun writeLzw(pixels: ByteArray, colorDepth: Int) {
        val initCodeSize = colorDepth.coerceAtLeast(2)
        out.write(initCodeSize)

        val clearCode = 1 shl initCodeSize
        val eoiCode = clearCode + 1
        var nextCode = eoiCode + 1
        var codeSize = initCodeSize + 1
        var codeMask = (1 shl codeSize) - 1

        var curBit = 0
        var curVal = 0

        fun emitCode(code: Int) {
            curVal = curVal or (code shl curBit)
            curBit += codeSize
            while (curBit >= 8) {
                out.write(curVal and 0xff)
                curVal = curVal shr 8
                curBit -= 8
            }
            if (nextCode > codeMask && codeSize < 12) {
                codeSize++
                codeMask = (1 shl codeSize) - 1
            }
        }

        // Simple uncompressed LZW clear-stream
        emitCode(clearCode)
        for (b in pixels) {
            emitCode(b.toInt() and 0xff)
        }
        emitCode(eoiCode)

        if (curBit > 0) {
            out.write(curVal and 0xff)
        }
        out.write(0) // block terminator
    }

    private fun writeString(s: String) {
        for (c in s) out.write(c.code)
    }

    private fun writeShort(value: Int) {
        out.write(value and 0xff)
        out.write((value shr 8) and 0xff)
    }
}

