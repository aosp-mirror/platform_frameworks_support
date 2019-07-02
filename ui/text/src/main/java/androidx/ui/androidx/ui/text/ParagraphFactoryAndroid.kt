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

package androidx.ui.androidx.ui.text

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.ui.core.Density
import androidx.ui.engine.text.ParagraphStyle
import androidx.ui.engine.text.platform.ParagraphAndroid
import androidx.ui.engine.text.platform.TypefaceAdapter
import androidx.ui.painting.AnnotatedString
import androidx.ui.painting.TextStyle

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class ParagraphFactoryAndroid(val context: Context) : ParagraphFactory {
    override fun createParagraph(
        text: String,
        style: TextStyle,
        paragraphStyle: ParagraphStyle,
        textStyles: List<AnnotatedString.Item<TextStyle>>,
        density: Density
    ): ParagraphInterface {
        val typefaceAdapter = TypefaceAdapter(context)
        return ParagraphAndroid(
            text = text,
            style = style,
            paragraphStyle = paragraphStyle,
            textStyles = textStyles,
            density = density,
            typefaceAdapter = typefaceAdapter
        )
    }
}