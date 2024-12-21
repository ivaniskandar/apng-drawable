//
// Copyright 2018 LINE Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package com.linecorp.apng.decoder

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Trace
import android.util.Log
import androidx.annotation.IntRange
import java.io.InputStream

/**
 * A class which holds APNG image information and bridge to the native(jni) layer.
 * When finished to use this class, you must explicitly call [recycle].
 */
internal class Apng(
    private val id: Int,
    /**
     * The width of the image
     */
    val width: Int,
    /**
     * The height of the image
     */
    val height: Int,
    /**
     * The number of frames included in this APNG image.
     */
    @IntRange(from = 1, to = Int.MAX_VALUE.toLong())
    val frameCount: Int,

    val frameDurations: IntArray,
    /**
     * The number of times to loop this APNG image. The value must be a signed value.
     * `0` indicates infinite looping.
     */
    @IntRange(from = 0, to = Int.MAX_VALUE.toLong())
    val loopCount: Int,
    /**
     * The size of memory required for this image in the native layer.
     */
    @IntRange(from = 0, to = Int.MAX_VALUE.toLong())
    val allFrameByteCount: Long
) {
    private val bitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    val byteCount: Int
        get() = bitmap.allocationByteCount

    init {
        Trace.beginSection("Apng#draw")
        ApngDecoderJni.draw(id, 0, bitmap)
        Trace.endSection()
    }

    /**
     * The duration to animate one loop of APNG animation.
     */
    @IntRange(from = 0, to = Int.MAX_VALUE.toLong())
    val duration: Int = frameDurations.sum()

    val isRecycled: Boolean
        get() = bitmap.isRecycled

    val config: Bitmap.Config?
        get() = bitmap.config

    fun recycle() {
        ApngDecoderJni.recycle(id)
    }

    fun copy(): Apng = copy(this)

    fun finalize() {
        if (DEBUG) {
            Log.d("apng-drawable", "finalized: $id")
        }
        recycle()
    }

    /**
     * Draws specified frame to the [canvas].
     */
    fun drawWithIndex(frameIndex: Int, canvas: Canvas, src: Rect?, dst: Rect, paint: Paint) {
        Trace.beginSection("Apng#draw")
        try {
            ApngDecoderJni.draw(id, frameIndex, bitmap)
        } catch (e: Throwable) {
            Log.e("apng-drawable", "Failed to draw $id frame index: $frameIndex", e)
        } finally {
            Trace.endSection()
        }
        canvas.drawBitmap(bitmap, src, dst, paint)
    }

    /**
     * A model class which contains the results of decoding in the native layer.
     *
     * Note:
     * If you edit this class, you should update `ApngDecoderJni.cpp` too.
     * Also, this class shouldn't be obfuscated, including class name and class member's name.
     * This class is accessed from the native layer and the fields are accessed by name like
     * reflection.
     */
    class DecodeResult {
        var width: Int = 0
        var height: Int = 0
        var frameCount: Int = 0
        var loopCount: Int = 0
        var frameDurations: IntArray = intArrayOf()
        var allFrameByteCount: Long = 0
    }

    companion object {

        @Throws(ApngException::class)
        fun decode(stream: InputStream): Apng {
            val result = DecodeResult()
            Trace.beginSection("Apng#decode")
            val id = try {
                ApngDecoderJni.decode(stream, result)
            } catch (e: Throwable) {
                throw ApngException(e)
            } finally {
                Trace.endSection()
            }
            throwIfError(id)
            try {
                return Apng(
                    id,
                    result.width,
                    result.height,
                    result.frameCount,
                    result.frameDurations,
                    result.loopCount,
                    result.allFrameByteCount
                )
            } catch (e: Throwable) {
                throw ApngException(e)
            }
        }

        @Throws(ApngException::class)
        fun isApng(stream: InputStream): Boolean {
            return try {
                ApngDecoderJni.isApng(stream)
            } catch (e: Throwable) {
                throw ApngException(e)
            }
        }

        @Throws(ApngException::class)
        fun copy(apng: Apng): Apng {
            val result = DecodeResult()
            Trace.beginSection("Apng#copy")
            val id = try {
                ApngDecoderJni.copy(apng.id, result)
            } catch (e: Throwable) {
                throw ApngException(e)
            } finally {
                Trace.endSection()
            }
            throwIfError(id)
            try {
                return Apng(
                    id,
                    result.width,
                    result.height,
                    result.frameCount,
                    result.frameDurations,
                    result.loopCount,
                    result.allFrameByteCount
                )
            } catch (e: Throwable) {
                throw ApngException(e)
            }
        }

        @Throws(ApngException::class)
        private fun throwIfError(resultCode: Int) {
            if (resultCode >= 0) {
                return
            }
            throw ApngException(ApngException.ErrorCode.fromErrorCode(resultCode))
        }
    }
}

private const val DEBUG = false
