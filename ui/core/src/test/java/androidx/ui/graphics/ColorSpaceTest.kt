/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.ui.graphics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

@RunWith(JUnit4::class)
class ColorSpaceTest {

    @Test
    fun testNamedColorSpaces() {
        for (named in ColorSpace.Named.values()) {
            val colorSpace = ColorSpace.get(named)
            assertNotNull(colorSpace.name)
            assertNotNull(colorSpace)
            assertEquals(named.ordinal.toLong(), colorSpace.id.toLong())
            assertTrue(colorSpace.componentCount >= 1)
            assertTrue(colorSpace.componentCount <= 4)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testEmptyName() {
        ColorSpace.Rgb("", FloatArray(6), FloatArray(2), sIdentity, sIdentity, 0.0f, 1.0f)
    }

    @Test
    fun testName() {
        val cs = ColorSpace.Rgb(
            "Test", FloatArray(6), FloatArray(2),
            sIdentity, sIdentity, 0.0f, 1.0f
        )
        assertEquals("Test", cs.name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testPrimariesLength() {
        ColorSpace.Rgb("Test", FloatArray(7), FloatArray(2), sIdentity, sIdentity, 0.0f, 1.0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testWhitePointLength() {
        ColorSpace.Rgb("Test", FloatArray(6), FloatArray(1), sIdentity, sIdentity, 0.0f, 1.0f)
    }

    @Test
    fun testOETF() {
        val op: (Double) -> Double = { x -> sqrt(x) }
        val cs = ColorSpace.Rgb(
            "Test", FloatArray(6), FloatArray(2),
            op, sIdentity, 0.0f, 1.0f
        )
        assertEquals(0.5, cs.oetf(0.25), 1e-5)
    }

    @Test
    fun testEOTF() {
        val op: (Double) -> Double = { x -> x * x }
        val cs = ColorSpace.Rgb(
            "Test", FloatArray(6), FloatArray(2),
            sIdentity, op, 0.0f, 1.0f
        )
        assertEquals(0.0625, cs.eotf(0.25), 1e-5)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidRange() {
        ColorSpace.Rgb("Test", FloatArray(6), FloatArray(2), sIdentity, sIdentity, 2.0f, 1.0f)
    }

    @Test
    fun testRanges() {
        var cs = ColorSpace.get(ColorSpace.Named.Srgb)

        var m1 = cs.getMinValue(0)
        var m2 = cs.getMinValue(1)
        var m3 = cs.getMinValue(2)

        assertEquals(0.0f, m1, 1e-9f)
        assertEquals(0.0f, m2, 1e-9f)
        assertEquals(0.0f, m3, 1e-9f)

        m1 = cs.getMaxValue(0)
        m2 = cs.getMaxValue(1)
        m3 = cs.getMaxValue(2)

        assertEquals(1.0f, m1, 1e-9f)
        assertEquals(1.0f, m2, 1e-9f)
        assertEquals(1.0f, m3, 1e-9f)

        cs = ColorSpace.get(ColorSpace.Named.CieLab)

        m1 = cs.getMinValue(0)
        m2 = cs.getMinValue(1)
        m3 = cs.getMinValue(2)

        assertEquals(0.0f, m1, 1e-9f)
        assertEquals(-128.0f, m2, 1e-9f)
        assertEquals(-128.0f, m3, 1e-9f)

        m1 = cs.getMaxValue(0)
        m2 = cs.getMaxValue(1)
        m3 = cs.getMaxValue(2)

        assertEquals(100.0f, m1, 1e-9f)
        assertEquals(128.0f, m2, 1e-9f)
        assertEquals(128.0f, m3, 1e-9f)

        cs = ColorSpace.get(ColorSpace.Named.CieXyz)

        m1 = cs.getMinValue(0)
        m2 = cs.getMinValue(1)
        m3 = cs.getMinValue(2)

        assertEquals(-2.0f, m1, 1e-9f)
        assertEquals(-2.0f, m2, 1e-9f)
        assertEquals(-2.0f, m3, 1e-9f)

        m1 = cs.getMaxValue(0)
        m2 = cs.getMaxValue(1)
        m3 = cs.getMaxValue(2)

        assertEquals(2.0f, m1, 1e-9f)
        assertEquals(2.0f, m2, 1e-9f)
        assertEquals(2.0f, m3, 1e-9f)
    }

    @Test
    fun testMat3x3() {
        val cs = ColorSpace.Rgb("Test", SRGB_TO_XYZ, sIdentity, sIdentity)

        val rgbToXYZ = cs.getTransform()
        for (i in 0..8) {
            assertEquals(SRGB_TO_XYZ[i], rgbToXYZ[i], 1e-5f)
        }
    }

    @Test
    fun testMat3x3Inverse() {
        val cs = ColorSpace.Rgb("Test", SRGB_TO_XYZ, sIdentity, sIdentity)

        val xyzToRGB = cs.getInverseTransform()
        for (i in 0..8) {
            assertEquals(XYZ_TO_SRGB[i], xyzToRGB[i], 1e-5f)
        }
    }

    @Test
    fun testMat3x3Primaries() {
        val cs = ColorSpace.Rgb("Test", SRGB_TO_XYZ, sIdentity, sIdentity)

        val primaries = cs.getPrimaries()

        assertNotNull(primaries)
        assertEquals(6, primaries.size.toLong())

        assertEquals(SRGB_PRIMARIES_xyY[0], primaries[0], 1e-5f)
        assertEquals(SRGB_PRIMARIES_xyY[1], primaries[1], 1e-5f)
        assertEquals(SRGB_PRIMARIES_xyY[2], primaries[2], 1e-5f)
        assertEquals(SRGB_PRIMARIES_xyY[3], primaries[3], 1e-5f)
        assertEquals(SRGB_PRIMARIES_xyY[4], primaries[4], 1e-5f)
        assertEquals(SRGB_PRIMARIES_xyY[5], primaries[5], 1e-5f)
    }

    @Test
    fun testMat3x3WhitePoint() {
        val cs = ColorSpace.Rgb("Test", SRGB_TO_XYZ, sIdentity, sIdentity)

        val whitePoint = cs.getWhitePoint()

        assertNotNull(whitePoint)
        assertEquals(2, whitePoint.size.toLong())

        assertEquals(SRGB_WHITE_POINT_xyY[0], whitePoint[0], 1e-5f)
        assertEquals(SRGB_WHITE_POINT_xyY[1], whitePoint[1], 1e-5f)
    }

    @Test
    fun testXYZFromPrimaries_xyY() {
        val cs = ColorSpace.Rgb(
            "Test", SRGB_PRIMARIES_xyY, SRGB_WHITE_POINT_xyY,
            sIdentity, sIdentity, 0.0f, 1.0f
        )

        val rgbToXYZ = cs.getTransform()
        for (i in 0..8) {
            assertEquals(SRGB_TO_XYZ[i], rgbToXYZ[i], 1e-5f)
        }

        val xyzToRGB = cs.getInverseTransform()
        for (i in 0..8) {
            assertEquals(XYZ_TO_SRGB[i], xyzToRGB[i], 1e-5f)
        }
    }

    @Test
    fun testXYZFromPrimaries_XYZ() {
        val cs = ColorSpace.Rgb(
            "Test", SRGB_PRIMARIES_XYZ, SRGB_WHITE_POINT_XYZ,
            sIdentity, sIdentity, 0.0f, 1.0f
        )

        val primaries = cs.getPrimaries()

        assertNotNull(primaries)
        assertEquals(6, primaries.size.toLong())

        // SRGB_PRIMARIES_xyY only has 1e-3 of precision, match it
        assertEquals(SRGB_PRIMARIES_xyY[0], primaries[0], 1e-3f)
        assertEquals(SRGB_PRIMARIES_xyY[1], primaries[1], 1e-3f)
        assertEquals(SRGB_PRIMARIES_xyY[2], primaries[2], 1e-3f)
        assertEquals(SRGB_PRIMARIES_xyY[3], primaries[3], 1e-3f)
        assertEquals(SRGB_PRIMARIES_xyY[4], primaries[4], 1e-3f)
        assertEquals(SRGB_PRIMARIES_xyY[5], primaries[5], 1e-3f)

        val whitePoint = cs.getWhitePoint()

        assertNotNull(whitePoint)
        assertEquals(2, whitePoint.size.toLong())

        // SRGB_WHITE_POINT_xyY only has 1e-3 of precision, match it
        assertEquals(SRGB_WHITE_POINT_xyY[0], whitePoint[0], 1e-3f)
        assertEquals(SRGB_WHITE_POINT_xyY[1], whitePoint[1], 1e-3f)

        val rgbToXYZ = cs.getTransform()
        for (i in 0..8) {
            assertEquals(SRGB_TO_XYZ[i], rgbToXYZ[i], 1e-5f)
        }

        val xyzToRGB = cs.getInverseTransform()
        for (i in 0..8) {
            assertEquals(XYZ_TO_SRGB[i], xyzToRGB[i], 1e-5f)
        }
    }

    @Test
    fun testGetComponentCount() {
        assertEquals(3, ColorSpace.get(ColorSpace.Named.Srgb).componentCount.toLong())
        assertEquals(3, ColorSpace.get(ColorSpace.Named.LinearSrgb).componentCount.toLong())
        assertEquals(3, ColorSpace.get(ColorSpace.Named.ExtendedSrgb).componentCount.toLong())
        assertEquals(
            3,
            ColorSpace.get(ColorSpace.Named.LinearExtendedSrgb).componentCount.toLong()
        )
        assertEquals(3, ColorSpace.get(ColorSpace.Named.DisplayP3).componentCount.toLong())
        assertEquals(3, ColorSpace.get(ColorSpace.Named.CieLab).componentCount.toLong())
        assertEquals(3, ColorSpace.get(ColorSpace.Named.CieXyz).componentCount.toLong())
    }

    @Test
    fun testIsSRGB() {
        for (e in ColorSpace.Named.values()) {
            val colorSpace = ColorSpace.get(e)
            if (e == ColorSpace.Named.Srgb) {
                assertTrue(colorSpace.isSrgb)
            } else {
                assertFalse(
                    "Incorrectly treating $colorSpace as SRGB!",
                    colorSpace.isSrgb
                )
            }
        }

        val cs = ColorSpace.Rgb("Almost sRGB", SRGB_TO_XYZ,
            { x -> x.pow(1.0 / 2.2) }, { x -> x.pow(2.2) })
        assertFalse(cs.isSrgb)
    }

    @Test
    fun testIsWideGamut() {
        assertFalse(ColorSpace.get(ColorSpace.Named.Srgb).isWideGamut)
        assertFalse(ColorSpace.get(ColorSpace.Named.Bt709).isWideGamut)
        assertTrue(ColorSpace.get(ColorSpace.Named.ExtendedSrgb).isWideGamut)
        assertTrue(ColorSpace.get(ColorSpace.Named.DciP3).isWideGamut)
        assertTrue(ColorSpace.get(ColorSpace.Named.Bt2020).isWideGamut)
        assertTrue(ColorSpace.get(ColorSpace.Named.Aces).isWideGamut)
        assertTrue(ColorSpace.get(ColorSpace.Named.CieLab).isWideGamut)
        assertTrue(ColorSpace.get(ColorSpace.Named.CieXyz).isWideGamut)
    }

    @Test
    fun testWhitePoint() {
        val cs = ColorSpace.get(ColorSpace.Named.Srgb) as ColorSpace.Rgb

        val whitePoint = cs.getWhitePoint()

        assertNotNull(whitePoint)
        assertEquals(2, whitePoint.size.toLong())

        // Make sure a copy is returned
        whitePoint.fill(Float.NaN)
        assertArrayNotEquals(whitePoint, cs.getWhitePoint(), 1e-5f)
        assertSame(whitePoint, cs.getWhitePoint(whitePoint))
        assertArrayEquals(whitePoint, cs.getWhitePoint(), 1e-5f)
    }

    @Test
    fun testPrimaries() {
        val cs = ColorSpace.get(ColorSpace.Named.Srgb) as ColorSpace.Rgb

        val primaries = cs.getPrimaries()

        assertNotNull(primaries)
        assertEquals(6, primaries.size.toLong())

        // Make sure a copy is returned
        primaries.fill(Float.NaN)
        assertArrayNotEquals(primaries, cs.getPrimaries(), 1e-5f)
        assertSame(primaries, cs.getPrimaries(primaries))
        assertArrayEquals(primaries, cs.getPrimaries(), 1e-5f)
    }

    @Test
    fun testRGBtoXYZMatrix() {
        val cs = ColorSpace.get(ColorSpace.Named.Srgb) as ColorSpace.Rgb

        val rgbToXYZ = cs.getTransform()

        assertNotNull(rgbToXYZ)
        assertEquals(9, rgbToXYZ.size.toLong())

        // Make sure a copy is returned
        rgbToXYZ.fill(Float.NaN)
        assertArrayNotEquals(rgbToXYZ, cs.getTransform(), 1e-5f)
        assertSame(rgbToXYZ, cs.getTransform(rgbToXYZ))
        assertArrayEquals(rgbToXYZ, cs.getTransform(), 1e-5f)
    }

    @Test
    fun testXYZtoRGBMatrix() {
        val cs = ColorSpace.get(ColorSpace.Named.Srgb) as ColorSpace.Rgb

        val xyzToRGB = cs.getInverseTransform()

        assertNotNull(xyzToRGB)
        assertEquals(9, xyzToRGB.size.toLong())

        // Make sure a copy is returned
        xyzToRGB.fill(Float.NaN)
        assertArrayNotEquals(xyzToRGB, cs.getInverseTransform(), 1e-5f)
        assertSame(xyzToRGB, cs.getInverseTransform(xyzToRGB))
        assertArrayEquals(xyzToRGB, cs.getInverseTransform(), 1e-5f)
    }

    @Test
    fun testRGBtoXYZ() {
        val cs = ColorSpace.get(ColorSpace.Named.Srgb)

        val source = floatArrayOf(0.75f, 0.5f, 0.25f)
        val expected = floatArrayOf(0.3012f, 0.2679f, 0.0840f)

        val r1 = cs.toXyz(source[0], source[1], source[2])
        assertNotNull(r1)
        assertEquals(3, r1.size.toLong())
        assertArrayNotEquals(source, r1, 1e-5f)
        assertArrayEquals(expected, r1, 1e-3f)

        val r3 = floatArrayOf(source[0], source[1], source[2])
        assertSame(r3, cs.toXyz(r3))
        assertEquals(3, r3.size.toLong())
        assertArrayEquals(r1, r3, 1e-5f)
    }

    @Test
    fun testXYZtoRGB() {
        val cs = ColorSpace.get(ColorSpace.Named.Srgb)

        val source = floatArrayOf(0.3012f, 0.2679f, 0.0840f)
        val expected = floatArrayOf(0.75f, 0.5f, 0.25f)

        val r1 = cs.fromXyz(source[0], source[1], source[2])
        assertNotNull(r1)
        assertEquals(3, r1.size.toLong())
        assertArrayNotEquals(source, r1, 1e-5f)
        assertArrayEquals(expected, r1, 1e-3f)

        val r3 = floatArrayOf(source[0], source[1], source[2])
        assertSame(r3, cs.fromXyz(r3))
        assertEquals(3, r3.size.toLong())
        assertArrayEquals(r1, r3, 1e-5f)
    }

    @Test
    fun testConnect() {
        var connector: ColorSpace.Connector = ColorSpace.connect(
            ColorSpace.get(ColorSpace.Named.Srgb),
            ColorSpace.get(ColorSpace.Named.DciP3)
        )

        assertSame(ColorSpace.get(ColorSpace.Named.Srgb), connector.source)
        assertSame(ColorSpace.get(ColorSpace.Named.DciP3), connector.destination)
        assertSame(ColorSpace.RenderIntent.Perceptual, connector.renderIntent)

        connector = ColorSpace.connect(
            ColorSpace.get(ColorSpace.Named.Srgb),
            ColorSpace.get(ColorSpace.Named.Srgb)
        )

        assertSame(connector.destination, connector.source)
        assertSame(ColorSpace.RenderIntent.Relative, connector.renderIntent)

        connector = ColorSpace.connect(ColorSpace.get(ColorSpace.Named.DciP3))
        assertSame(ColorSpace.get(ColorSpace.Named.Srgb), connector.destination)

        connector = ColorSpace.connect(ColorSpace.get(ColorSpace.Named.Srgb))
        assertSame(connector.source, connector.destination)
    }

    @Test
    fun testConnector() {
        // Connect color spaces with same white points
        var connector: ColorSpace.Connector = ColorSpace.connect(
            ColorSpace.get(ColorSpace.Named.Srgb),
            ColorSpace.get(ColorSpace.Named.AdobeRgb)
        )

        var source = floatArrayOf(1.0f, 0.5f, 0.0f)
        var expected = floatArrayOf(0.8912f, 0.4962f, 0.1164f)

        var r1 = connector.transform(source[0], source[1], source[2])
        assertNotNull(r1)
        assertEquals(3, r1.size.toLong())
        assertArrayNotEquals(source, r1, 1e-5f)
        assertArrayEquals(expected, r1, 1e-3f)

        var r3 = floatArrayOf(source[0], source[1], source[2])
        assertSame(r3, connector.transform(r3))
        assertEquals(3, r3.size.toLong())
        assertArrayEquals(r1, r3, 1e-5f)

        connector = ColorSpace.connect(
            ColorSpace.get(ColorSpace.Named.AdobeRgb),
            ColorSpace.get(ColorSpace.Named.Srgb)
        )

        val tmp = source
        source = expected
        expected = tmp

        r1 = connector.transform(source[0], source[1], source[2])
        assertNotNull(r1)
        assertEquals(3, r1.size.toLong())
        assertArrayNotEquals(source, r1, 1e-5f)
        assertArrayEquals(expected, r1, 1e-3f)

        r3 = floatArrayOf(source[0], source[1], source[2])
        assertSame(r3, connector.transform(r3))
        assertEquals(3, r3.size.toLong())
        assertArrayEquals(r1, r3, 1e-5f)
    }

    @Test
    fun testAdaptedConnector() {
        // Connect color spaces with different white points
        val connector = ColorSpace.connect(
            ColorSpace.get(ColorSpace.Named.Srgb),
            ColorSpace.get(ColorSpace.Named.ProPhotoRgb)
        )

        val source = floatArrayOf(1.0f, 0.0f, 0.0f)
        val expected = floatArrayOf(0.70226f, 0.2757f, 0.1036f)

        val r = connector.transform(source[0], source[1], source[2])
        assertNotNull(r)
        assertEquals(3, r.size.toLong())
        assertArrayNotEquals(source, r, 1e-5f)
        assertArrayEquals(expected, r, 1e-4f)
    }

    @Test
    fun testAdaptedConnectorWithRenderIntent() {
        // Connect a wider color space to a narrow color space
        var connector: ColorSpace.Connector = ColorSpace.connect(
            ColorSpace.get(ColorSpace.Named.DciP3),
            ColorSpace.get(ColorSpace.Named.Srgb),
            ColorSpace.RenderIntent.Relative
        )

        val source = floatArrayOf(0.9f, 0.9f, 0.9f)

        val relative = connector.transform(source[0], source[1], source[2])
        assertNotNull(relative)
        assertEquals(3, relative.size.toLong())
        assertArrayNotEquals(source, relative, 1e-5f)
        assertArrayEquals(floatArrayOf(0.8862f, 0.8862f, 0.8862f), relative, 1e-4f)

        connector = ColorSpace.connect(
            ColorSpace.get(ColorSpace.Named.DciP3),
            ColorSpace.get(ColorSpace.Named.Srgb),
            ColorSpace.RenderIntent.Absolute
        )

        val absolute = connector.transform(source[0], source[1], source[2])
        assertNotNull(absolute)
        assertEquals(3, absolute.size.toLong())
        assertArrayNotEquals(source, absolute, 1e-5f)
        assertArrayNotEquals(relative, absolute, 1e-5f)
        assertArrayEquals(floatArrayOf(0.8475f, 0.9217f, 0.8203f), absolute, 1e-4f)
    }

    @Test
    fun testIdentityConnector() {
        val connector = ColorSpace.connect(
            ColorSpace.get(ColorSpace.Named.Srgb),
            ColorSpace.get(ColorSpace.Named.Srgb)
        )

        assertSame(connector.source, connector.destination)
        assertSame(ColorSpace.RenderIntent.Relative, connector.renderIntent)

        val source = floatArrayOf(0.11112f, 0.22227f, 0.444448f)

        val r = connector.transform(source[0], source[1], source[2])
        assertNotNull(r)
        assertEquals(3, r.size.toLong())
        assertArrayEquals(source, r, 1e-5f)
    }

    @Test
    fun testConnectorTransformIdentity() {
        val connector = ColorSpace.connect(
            ColorSpace.get(ColorSpace.Named.DciP3),
            ColorSpace.get(ColorSpace.Named.DciP3)
        )

        val source = floatArrayOf(1.0f, 0.0f, 0.0f)
        val expected = floatArrayOf(1.0f, 0.0f, 0.0f)

        val r1 = connector.transform(source[0], source[1], source[2])
        assertNotNull(r1)
        assertEquals(3, r1.size.toLong())
        assertArrayEquals(expected, r1, 1e-3f)

        val r3 = floatArrayOf(source[0], source[1], source[2])
        assertSame(r3, connector.transform(r3))
        assertEquals(3, r3.size.toLong())
        assertArrayEquals(r1, r3, 1e-5f)
    }

    @Test
    fun testAdaptation() {
        var adapted = ColorSpace.adapt(
            ColorSpace.get(ColorSpace.Named.Srgb),
            ColorSpace.IlluminantD50
        )

        val sRGBD50 = floatArrayOf(
            0.43602175f,
            0.22247513f,
            0.01392813f,
            0.38510883f,
            0.71690667f,
            0.09710153f,
            0.14308129f,
            0.06061824f,
            0.71415880f
        )

        assertArrayEquals(sRGBD50, (adapted as ColorSpace.Rgb).getTransform(), 1e-7f)

        adapted = ColorSpace.adapt(
            ColorSpace.get(ColorSpace.Named.Srgb),
            ColorSpace.IlluminantD50,
            ColorSpace.Adaptation.Bradford
        )
        assertArrayEquals(sRGBD50, (adapted as ColorSpace.Rgb).getTransform(), 1e-7f)
    }

    @Test
    fun testImplicitSRGBConnector() {
        val connector1 = ColorSpace.connect(
            ColorSpace.get(ColorSpace.Named.DciP3)
        )

        assertSame(ColorSpace.get(ColorSpace.Named.Srgb), connector1.destination)

        val connector2 = ColorSpace.connect(
            ColorSpace.get(ColorSpace.Named.DciP3),
            ColorSpace.get(ColorSpace.Named.Srgb)
        )

        val source = floatArrayOf(0.6f, 0.9f, 0.7f)
        assertArrayEquals(
            connector1.transform(source[0], source[1], source[2]),
            connector2.transform(source[0], source[1], source[2]), 1e-7f
        )
    }

    @Test
    fun testLab() {
        var connector: ColorSpace.Connector = ColorSpace.connect(
            ColorSpace.get(ColorSpace.Named.CieLab)
        )

        var source = floatArrayOf(100.0f, 0.0f, 0.0f)
        var expected = floatArrayOf(1.0f, 1.0f, 1.0f)

        var r1 = connector.transform(source[0], source[1], source[2])
        assertNotNull(r1)
        assertEquals(3, r1.size.toLong())
        assertArrayEquals(expected, r1, 1e-3f)

        source = floatArrayOf(100.0f, 0.0f, 54.0f)
        expected = floatArrayOf(1.0f, 0.9925f, 0.5762f)

        var r2 = connector.transform(source[0], source[1], source[2])
        assertNotNull(r2)
        assertEquals(3, r2.size.toLong())
        assertArrayEquals(expected, r2, 1e-3f)

        connector = ColorSpace.connect(
            ColorSpace.get(ColorSpace.Named.CieLab), ColorSpace.RenderIntent.Absolute
        )

        source = floatArrayOf(100.0f, 0.0f, 0.0f)
        expected = floatArrayOf(1.0f, 0.9910f, 0.8651f)

        r1 = connector.transform(source[0], source[1], source[2])
        assertNotNull(r1)
        assertEquals(3, r1.size.toLong())
        assertArrayEquals(expected, r1, 1e-3f)

        source = floatArrayOf(100.0f, 0.0f, 54.0f)
        expected = floatArrayOf(1.0f, 0.9853f, 0.4652f)

        r2 = connector.transform(source[0], source[1], source[2])
        assertNotNull(r2)
        assertEquals(3, r2.size.toLong())
        assertArrayEquals(expected, r2, 1e-3f)
    }

    @Test
    fun testXYZ() {
        val xyz = ColorSpace.get(ColorSpace.Named.CieXyz)

        val source = floatArrayOf(0.32f, 0.43f, 0.54f)

        val r1 = xyz.toXyz(source[0], source[1], source[2])
        assertNotNull(r1)
        assertEquals(3, r1.size.toLong())
        assertArrayEquals(source, r1, 1e-7f)

        val r2 = xyz.fromXyz(source[0], source[1], source[2])
        assertNotNull(r2)
        assertEquals(3, r2.size.toLong())
        assertArrayEquals(source, r2, 1e-7f)

        val connector = ColorSpace.connect(ColorSpace.get(ColorSpace.Named.CieXyz))

        val expected = floatArrayOf(0.2280f, 0.7541f, 0.8453f)

        val r3 = connector.transform(source[0], source[1], source[2])
        assertNotNull(r3)
        assertEquals(3, r3.size.toLong())
        assertArrayEquals(expected, r3, 1e-3f)
    }

    @Test
    fun testIDs() {
        // These cannot change
        assertEquals(0, ColorSpace.get(ColorSpace.Named.Srgb).id.toLong())
        assertEquals(-1, ColorSpace.MinId.toLong())
        assertEquals(63, ColorSpace.MaxId.toLong())
    }

    @Test
    fun testFromLinear() {
        val colorSpace = ColorSpace.get(ColorSpace.Named.Srgb) as ColorSpace.Rgb

        val source = floatArrayOf(0.0f, 0.5f, 1.0f)
        val expected = floatArrayOf(0.0f, 0.7354f, 1.0f)

        val r1 = colorSpace.fromLinear(source[0], source[1], source[2])
        assertNotNull(r1)
        assertEquals(3, r1.size.toLong())
        assertArrayEquals(expected, r1, 1e-3f)

        val r2 = floatArrayOf(source[0], source[1], source[2])
        assertSame(r2, colorSpace.fromLinear(r2))
        assertEquals(3, r2.size.toLong())
        assertArrayEquals(r1, r2, 1e-5f)
    }

    @Test
    fun testToLinear() {
        val colorSpace = ColorSpace.get(ColorSpace.Named.Srgb) as ColorSpace.Rgb

        val source = floatArrayOf(0.0f, 0.5f, 1.0f)
        val expected = floatArrayOf(0.0f, 0.2140f, 1.0f)

        val r1 = colorSpace.toLinear(source[0], source[1], source[2])
        assertNotNull(r1)
        assertEquals(3, r1.size.toLong())
        assertArrayEquals(expected, r1, 1e-3f)

        val r2 = floatArrayOf(source[0], source[1], source[2])
        assertSame(r2, colorSpace.toLinear(r2))
        assertEquals(3, r2.size.toLong())
        assertArrayEquals(r1, r2, 1e-5f)
    }

    @Test
    fun testTransferParameters() {
        var colorSpace = ColorSpace.get(ColorSpace.Named.Srgb) as ColorSpace.Rgb
        assertNotNull(colorSpace.transferParameters)

        colorSpace = ColorSpace.get(ColorSpace.Named.ExtendedSrgb) as ColorSpace.Rgb
        assertNull(colorSpace.transferParameters)
    }

    @Test
    fun testIdempotentTransferFunctions() {
        ColorSpace.Named.values().map { ColorSpace.get(it) }
            .filter { cs -> cs.model == ColorSpace.Model.Rgb }
            .map { cs -> cs as ColorSpace.Rgb }
            .forEach { cs ->
                val source = floatArrayOf(0.0f, 0.5f, 1.0f)
                val r = cs.fromLinear(cs.toLinear(source[0], source[1], source[2]))
                assertArrayEquals(source, r, 1e-3f)
            }
    }

    @Test
    fun testMatch() {
        for (named in ColorSpace.Named.values()) {
            val cs = ColorSpace.get(named)
            if (cs.model == ColorSpace.Model.Rgb) {
                var rgb = cs as ColorSpace.Rgb
                // match() cannot match extended sRGB
                if (rgb !== ColorSpace.get(ColorSpace.Named.ExtendedSrgb) && rgb !== ColorSpace.get(
                        ColorSpace.Named.LinearExtendedSrgb
                    )
                ) {

                    // match() uses CIE XYZ D50
                    rgb = ColorSpace.adapt(rgb, ColorSpace.IlluminantD50) as ColorSpace.Rgb
                    assertSame(
                        cs,
                        ColorSpace.match(rgb.getTransform(), rgb.transferParameters!!)
                    )
                }
            }
        }

        assertSame(
            ColorSpace.get(ColorSpace.Named.Srgb),
            ColorSpace.match(
                SRGB_TO_XYZ_D50, ColorSpace.Rgb.TransferParameters(
                    1 / 1.055, 0.055 / 1.055, 1 / 12.92, 0.04045, 2.4
                )
            )
        )
    }

    companion object {
        // Column-major RGB->XYZ transform matrix for the sRGB color space
        private val SRGB_TO_XYZ = floatArrayOf(
            0.412391f,
            0.212639f,
            0.019331f,
            0.357584f,
            0.715169f,
            0.119195f,
            0.180481f,
            0.072192f,
            0.950532f
        )
        // Column-major XYZ->RGB transform matrix for the sRGB color space
        private val XYZ_TO_SRGB = floatArrayOf(
            3.240970f,
            -0.969244f,
            0.055630f,
            -1.537383f,
            1.875968f,
            -0.203977f,
            -0.498611f,
            0.041555f,
            1.056971f
        )

        // Column-major RGB->XYZ transform matrix for the sRGB color space and a D50 white point
        private val SRGB_TO_XYZ_D50 = floatArrayOf(
            0.4360747f,
            0.2225045f,
            0.0139322f,
            0.3850649f,
            0.7168786f,
            0.0971045f,
            0.1430804f,
            0.0606169f,
            0.7141733f
        )

        private val SRGB_PRIMARIES_xyY =
            floatArrayOf(0.640f, 0.330f, 0.300f, 0.600f, 0.150f, 0.060f)
        private val SRGB_WHITE_POINT_xyY = floatArrayOf(0.3127f, 0.3290f)

        private val SRGB_PRIMARIES_XYZ = floatArrayOf(
            1.939394f,
            1.000000f,
            0.090909f,
            0.500000f,
            1.000000f,
            0.166667f,
            2.500000f,
            1.000000f,
            13.166667f
        )
        private val SRGB_WHITE_POINT_XYZ = floatArrayOf(0.950456f, 1.000f, 1.089058f)

        private val sIdentity: (Double) -> Double = { x -> x }

        private fun assertArrayNotEquals(a: FloatArray, b: FloatArray, eps: Float) {
            for (i in a.indices) {
                if (a[i].compareTo(b[i]) == 0 || abs(a[i] - b[i]) < eps) {
                    fail("Expected " + a[i] + ", received " + b[i])
                }
            }
        }

        private fun assertArrayEquals(a: FloatArray, b: FloatArray, eps: Float) {
            for (i in a.indices) {
                if (a[i].compareTo(b[i]) != 0 && abs(a[i] - b[i]) > eps) {
                    fail("Expected " + a[i] + ", received " + b[i])
                }
            }
        }
    }
}
