/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.ui.painting

import androidx.ui.graphics.ColorSpace

// Opaque handle to raw decoded image data (pixels).
/**
 *
 * To obtain an [Image] object, use [instantiateImageCodec].
 *
 * To draw an [Image], use one of the methods on the [Canvas] class, such as
 * [Canvas.drawImage].
 */

/**
 * This class is created by the engine, and should not be instantiated
 * or extended directly.
 *
 * To obtain an [Image] object, use [instantiateImageCodec].
 */

// TODO njawad/aelias uncomment implementation when platform support is enabled + uncomment `expect`
// expect fun Image(
//    width: Int,
//    height: Int,
//    config: ImageConfig = ImageConfig.Argb8888,
//    hasAlpha: Boolean = true,
//    colorSpace: ColorSpace = ColorSpace.get(ColorSpace.Named.Srgb)
// ): Image

/* expect */ typealias NativeImage = android.graphics.Bitmap

// TODO njawad/aelias uncomment when host side testing support is added
interface Image {

    /** The number of image pixels along the image's horizontal axis. */
    val width: Int

    /** The number of image pixels along the image's vertical axis. */
    val height: Int

    /** Return the size of this Image in bytes **/
    val byteCount: Int

    /** ColorSpace the Image renders in **/
    val colorSpace: ColorSpace

    /** Determines whether or not the Image contains an alpha channel **/
    val hasAlpha: Boolean

    /**
     * Returns the current configuration of this Image, either:
     * @see ImageConfig.Argb8888
     * @see ImageConfig.Rgb565
     * @see ImageConfig.Alpha8
     * @see ImageConfig.Gpu
     */
    val config: ImageConfig

    /**
     * Return backing object that implements the Image interface
     */
    val nativeImage: NativeImage

    /**
     * Builds caches associated with the bitmap that are used for drawing it. This method can
     * be used as a signal to upload images to the GPU to eventually be rendered
     */
    fun prepareToDraw()
}
