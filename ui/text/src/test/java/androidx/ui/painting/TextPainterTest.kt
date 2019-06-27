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

import androidx.ui.core.Constraints
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextDirection
import androidx.ui.engine.window.Locale
import androidx.ui.rendering.paragraph.TextOverflow
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextPainterTest() {
    @Test
    fun `constructor with default values`() {
        val textPainter = TextPainter()

        assertThat(textPainter.text).isNull()
        assertThat(textPainter.textAlign).isEqualTo(TextAlign.Start)
        assertThat(textPainter.textDirection).isEqualTo(TextDirection.Ltr)
        assertThat(textPainter.textScaleFactor).isEqualTo(1.0f)
        assertThat(textPainter.maxLines).isNull()
        assertThat(textPainter.overflow).isEqualTo(TextOverflow.Clip)
        assertThat(textPainter.locale).isNull()
    }

    @Test
    fun `constructor with customized text(TextSpan)`() {
        val text = AnnotatedString("Hello")
        val textPainter = TextPainter(text = text)

        assertThat(textPainter.text).isEqualTo(text)
    }

    @Test
    fun `constructor with customized textAlign`() {
        val textPainter = TextPainter(paragraphStyle = ParagraphStyle(textAlign = TextAlign.Left))

        assertThat(textPainter.textAlign).isEqualTo(TextAlign.Left)
    }

    @Test
    fun `constructor with customized textDirection`() {
        val textPainter = TextPainter(
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Rtl)
        )

        assertThat(textPainter.textDirection).isEqualTo(TextDirection.Rtl)
    }

    @Test
    fun `constructor with customized textScaleFactor`() {
        val scaleFactor = 2.0f

        val textPainter = TextPainter(textScaleFactor = scaleFactor)

        assertThat(textPainter.textScaleFactor).isEqualTo(scaleFactor)
    }

    @Test
    fun `constructor with customized maxLines`() {
        val maxLines = 8

        val textPainter = TextPainter(maxLines = maxLines)

        assertThat(textPainter.maxLines).isEqualTo(maxLines)
    }

    @Test
    fun `constructor with customized overflow`() {
        val overflow = TextOverflow.Ellipsis

        val textPainter = TextPainter(overflow = overflow)

        assertThat(textPainter.overflow).isEqualTo(overflow)
    }

    @Test
    fun `constructor with customized locale`() {
        val locale = Locale("en", "US")

        val textPainter = TextPainter(locale = locale)

        assertThat(textPainter.locale).isEqualTo(locale)
    }

    @Test
    fun `text setter`() {
        val textPainter = TextPainter()
        val text = AnnotatedString(text = "Hello")

        textPainter.text = text

        assertThat(textPainter.text).isEqualTo(text)
        assertThat(textPainter.paragraph).isNull()
        assertThat(textPainter.needsLayout).isTrue()
    }

    @Test
    fun `textAlign setter`() {
        val textPainter = TextPainter()

        textPainter.textAlign = TextAlign.Left

        assertThat(textPainter.textAlign).isEqualTo(TextAlign.Left)
        assertThat(textPainter.paragraph).isNull()
        assertThat(textPainter.needsLayout).isTrue()
    }

    @Test
    fun `textDirection setter`() {
        val textPainter = TextPainter()

        textPainter.textDirection = TextDirection.Rtl

        assertThat(textPainter.textDirection).isEqualTo(TextDirection.Rtl)
        assertThat(textPainter.paragraph).isNull()
        assertThat(textPainter.layoutTemplate).isNull()
        assertThat(textPainter.needsLayout).isTrue()
    }

    @Test
    fun `textScaleFactor setter`() {
        val textPainter = TextPainter()
        val scaleFactor = 3.0f

        textPainter.textScaleFactor = scaleFactor

        assertThat(textPainter.textScaleFactor).isEqualTo(scaleFactor)
        assertThat(textPainter.paragraph).isNull()
        assertThat(textPainter.layoutTemplate).isNull()
        assertThat(textPainter.needsLayout).isTrue()
    }

    @Test
    fun `maxLines setter`() {
        val textPainter = TextPainter()
        val maxLines = 5

        textPainter.maxLines = maxLines

        assertThat(textPainter.maxLines).isEqualTo(maxLines)
        assertThat(textPainter.paragraph).isNull()
        assertThat(textPainter.needsLayout).isTrue()
    }

    @Test
    fun `overflow setter`() {
        val textPainter = TextPainter()
        val overflow = TextOverflow.Ellipsis

        textPainter.overflow = overflow

        assertThat(textPainter.overflow).isEqualTo(overflow)
        assertThat(textPainter.paragraph).isNull()
        assertThat(textPainter.needsLayout).isTrue()
    }

    @Test
    fun `locale setter`() {
        val textPainter = TextPainter()
        val locale = Locale("en", "US")

        textPainter.locale = locale

        assertThat(textPainter.locale).isEqualTo(locale)
        assertThat(textPainter.paragraph).isNull()
        assertThat(textPainter.needsLayout).isTrue()
    }

    @Test
    fun `createParagraphStyle without TextStyle in AnnotatedText`() {
        val scaleFactor = 3.0f
        val maxLines = 5
        val overflow = TextOverflow.Ellipsis
        val locale = Locale("en", "US")
        val text = AnnotatedString(text = "Hello")
        val textPainter = TextPainter(
            text = text,
            paragraphStyle = ParagraphStyle(
                textAlign = TextAlign.Center,
                textDirection = TextDirection.Rtl
            ),
            textScaleFactor = scaleFactor,
            maxLines = maxLines,
            overflow = overflow,
            locale = locale
        )

        val paragraphStyle = textPainter.createParagraphStyle()

        assertThat(paragraphStyle.textAlign).isEqualTo(TextAlign.Center)
        assertThat(paragraphStyle.textDirection).isEqualTo(TextDirection.Rtl)
        assertThat(paragraphStyle.maxLines).isEqualTo(maxLines)
        assertThat(paragraphStyle.ellipsis).isEqualTo(true)
    }

    @Test
    fun `createParagraphStyle with defaultTextDirection`() {
        val fontSize = 15.0f
        val scaleFactor = 3.0f
        val maxLines = 5
        val overflow = TextOverflow.Ellipsis
        val locale = Locale("en", "US")
        val textStyle = TextStyle(fontSize = fontSize)
        val text = AnnotatedString(text = "Hello")
        val textPainter = TextPainter(
            text = text,
            style = textStyle,
            paragraphStyle = ParagraphStyle(
                textAlign = TextAlign.Center,
                textDirection = TextDirection.Rtl
            ),
            textScaleFactor = scaleFactor,
            maxLines = maxLines,
            overflow = overflow,
            locale = locale
        )

        val paragraphStyle = textPainter.createParagraphStyle()

        assertThat(paragraphStyle.textAlign).isEqualTo(TextAlign.Center)
        assertThat(paragraphStyle.textDirection).isEqualTo(TextDirection.Rtl)
        assertThat(paragraphStyle.maxLines).isEqualTo(maxLines)
        assertThat(paragraphStyle.ellipsis).isEqualTo(true)
    }

    @Test
    fun `applyFloatingPointHack with value is integer toDouble`() {
        assertThat(applyFloatingPointHack(2f)).isEqualTo(2.0f)
    }

    @Test
    fun `applyFloatingPointHack with value smaller than half`() {
        assertThat(applyFloatingPointHack(2.2f)).isEqualTo(3.0f)
    }

    @Test
    fun `applyFloatingPointHack with value larger than half`() {
        assertThat(applyFloatingPointHack(2.8f)).isEqualTo(3.0f)
    }

    @Test(expected = AssertionError::class)
    fun `minIntrinsicWidth without layout assertion should fail`() {
        val textPainter = TextPainter()

        textPainter.minIntrinsicWidth
    }

    @Test(expected = AssertionError::class)
    fun `maxIntrinsicWidth without layout assertion should fail`() {
        val textPainter = TextPainter()

        textPainter.maxIntrinsicWidth
    }

    @Test(expected = AssertionError::class)
    fun `width without layout assertion should fail`() {
        val textPainter = TextPainter()

        textPainter.width
    }

    @Test(expected = AssertionError::class)
    fun `height without layout assertion should fail`() {
        val textPainter = TextPainter()

        textPainter.height
    }

    @Test(expected = AssertionError::class)
    fun `size without layout assertion should fail`() {
        val textPainter = TextPainter()

        textPainter.size
    }

    @Test(expected = AssertionError::class)
    fun `layout without text assertion should fail`() {
        val textPainter = TextPainter(
            paragraphStyle = ParagraphStyle(textDirection = TextDirection.Ltr)
        )

        textPainter.layout(Constraints())
    }

    @Test(expected = AssertionError::class)
    fun `paint without layout assertion should fail`() {
        val textPainter = TextPainter()
        val canvas = mock<Canvas>()

        textPainter.paint(canvas, Offset(0.0f, 0.0f))
    }

    @Test
    fun `equals return true when compare default TextPainters`() {
        val textPainter1 = TextPainter()
        val textPainter2 = TextPainter()

        assertThat(textPainter1).isEqualTo(textPainter2)
    }

    @Test
    fun `equals return true when compare same TextPainters`() {
        val text = AnnotatedString(text = "hello world")
        val textStyle = TextStyle(fontSize = 10f)
        val paragraphStyle = ParagraphStyle(lineHeight = 2f)
        val textScale = 2f
        val softWrap = false
        val overflow = TextOverflow.Ellipsis
        val locale = Locale("en", "US")

        val textPainter1 = TextPainter(
            text = text,
            style = textStyle,
            paragraphStyle = paragraphStyle,
            textScaleFactor = textScale,
            softWrap = softWrap,
            overflow = overflow,
            locale = locale
        )
        val textPainter2 = TextPainter(
            text = text,
            style = textStyle,
            paragraphStyle = paragraphStyle,
            textScaleFactor = textScale,
            softWrap = softWrap,
            overflow = overflow,
            locale = locale
        )

        assertThat(textPainter1).isEqualTo(textPainter2)
    }

    @Test
    fun `equals return false when text is different`() {
        val textPainter1 = TextPainter(text = AnnotatedString(text = "abc"))
        val textPainter2 = TextPainter(text = AnnotatedString(text = "cde"))

        assertThat(textPainter1).isNotEqualTo(textPainter2)
    }

    @Test
    fun `equals return false when textStyle is different`() {
        val textPainter1 = TextPainter(style = TextStyle())
        val textPainter2 = TextPainter(style = TextStyle(fontSize = 1f))

        assertThat(textPainter1).isNotEqualTo(textPainter2)
    }

    @Test
    fun `equals return false when paragraphStyle is different`() {
        val textPainter1 = TextPainter(paragraphStyle = ParagraphStyle())
        val textPainter2 = TextPainter(paragraphStyle = ParagraphStyle(textAlign = TextAlign.Right))

        assertThat(textPainter1).isNotEqualTo(textPainter2)
    }

    @Test
    fun `equals return false when textScaleFactor is different`() {
        val textPainter1 = TextPainter(textScaleFactor = 2f)
        val textPainter2 = TextPainter(textScaleFactor = 3f)

        assertThat(textPainter1).isNotEqualTo(textPainter2)
    }

    @Test
    fun `equals return false when maxLines is different`() {
        val textPainter1 = TextPainter(maxLines = 1)
        val textPainter2 = TextPainter(maxLines = 2)

        assertThat(textPainter1).isNotEqualTo(textPainter2)
    }

    @Test
    fun `equals return false when softWrap is different`() {
        val textPainter1 = TextPainter(softWrap = true)
        val textPainter2 = TextPainter(softWrap = false)

        assertThat(textPainter1).isNotEqualTo(textPainter2)
    }

    @Test
    fun `equals return false when overflow is different`() {
        val textPainter1 = TextPainter(overflow = TextOverflow.Clip)
        val textPainter2 = TextPainter(overflow = TextOverflow.Fade)

        assertThat(textPainter1).isNotEqualTo(textPainter2)
    }

    @Test
    fun `equals return false when locale is different`() {
        val textPainter1 = TextPainter(locale = Locale("ja", "JP"))
        val textPainter2 = TextPainter(locale = Locale("en", "US"))

        assertThat(textPainter1).isNotEqualTo(textPainter2)
    }
}
