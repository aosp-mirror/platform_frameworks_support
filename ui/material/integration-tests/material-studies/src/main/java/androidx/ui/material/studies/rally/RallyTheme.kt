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

package androidx.ui.material.studies.rally

import androidx.compose.composer
import androidx.compose.Children
import androidx.compose.Composable
import androidx.ui.core.CurrentTextStyleProvider
import androidx.ui.engine.text.FontStyle
import androidx.ui.engine.text.FontWeight
import androidx.ui.engine.text.font.Font
import androidx.ui.engine.text.font.FontFamily
import androidx.ui.material.MaterialColors
import androidx.ui.material.MaterialTheme
import androidx.ui.material.MaterialTypography
import androidx.ui.painting.Color
import androidx.ui.painting.TextStyle

val rallyGreen = Color(0xFF1EB980.toInt())
val rallyDarkGreen = Color(0xFF045D56.toInt())
val rallyOrange = Color(0xFFFF6859.toInt())
val rallyYellow = Color(0xFFFFCF44.toInt())
val rallyPurple = Color(0xFFB15DFF.toInt())
val rallyBlue = Color(0xFF72DEFF.toInt())

@Composable
fun RallyTheme(@Children children: @Composable() () -> Unit) {
    val colors = MaterialColors(
        primary = rallyGreen,
        surface = Color(0xFF33333D.toInt()),
        onSurface = Color(0xFFFFFFFF.toInt())
    )

    val roboto = FontFamily(
            Font(
                    name = "roboto_condensed_bold.ttf",
                    weight = FontWeight.w700
            ),
            Font(
                    name = "roboto_condensed_bolditalic.ttf",
                    weight = FontWeight.w700,
                    style = FontStyle.italic
            ),
            Font(
                    name = "roboto_condensed_italic.ttf",
                    weight = FontWeight.w400,
                    style = FontStyle.italic
            ),
            Font(
                    name = "roboto_condensed_regular.ttf",
                    weight = FontWeight.w400
            ),
            Font(
                    name = "roboto_condensed_light.ttf",
                    weight = FontWeight.w300
            ),
            Font(
                    name = "roboto_condensed_lightitalic.ttf",
                    weight = FontWeight.w300,
                    style = FontStyle.italic
            )
    )

    val eczar = FontFamily(
            Font(
                    name = "eczar_extrabold.ttf",
                    weight = FontWeight.w800
            ),
            Font(
                    name = "eczar_bold.ttf",
                    weight = FontWeight.w700
            ),
            Font(
                    name = "eczar_semibold.ttf",
                    weight = FontWeight.w600
            ),
            Font(
                    name = "eczar_medium.ttf",
                    weight = FontWeight.w500
            ),
            Font(
                    name = "eczar_regular.ttf",
                    weight = FontWeight.w400
            )
    )

    val typography = MaterialTypography(
        h1 = TextStyle(fontFamily = roboto,
            fontWeight = FontWeight.w100,
            fontSize = 96f),
        h2 = TextStyle(fontFamily = roboto,
            fontWeight = FontWeight.w100,
            fontSize = 60f),
        h3 = TextStyle(fontFamily = eczar,
            fontWeight = FontWeight.w500,
            fontSize = 48f),
        h4 = TextStyle(fontFamily = roboto,
            fontWeight = FontWeight.w700,
            fontSize = 34f),
        h5 = TextStyle(fontFamily = roboto,
            fontWeight = FontWeight.w700,
            fontSize = 24f),
        h6 = TextStyle(fontFamily = roboto,
            fontWeight = FontWeight.w500,
            fontSize = 20f),
        subtitle1 = TextStyle(fontFamily = roboto,
            fontWeight = FontWeight.w500,
            fontSize = 16f),
        subtitle2 = TextStyle(fontFamily = roboto,
            fontWeight = FontWeight.w500,
            fontSize = 14f),
        body1 = TextStyle(fontFamily = eczar,
            fontWeight = FontWeight.w400,
            fontSize = 16f),
        body2 = TextStyle(fontFamily = roboto,
            fontWeight = FontWeight.w200,
            fontSize = 14f),
        button = TextStyle(fontFamily = roboto,
            fontWeight = FontWeight.w800,
            fontSize = 14f,
            letterSpacing = 0.2f),
        caption = TextStyle(fontFamily = roboto,
            fontWeight = FontWeight.w500,
            fontSize = 12f),
        overline = TextStyle(fontFamily = roboto,
            fontWeight = FontWeight.w500,
            fontSize = 10f)

    )
    MaterialTheme(colors = colors, typography = typography) {
        // TODO: remove this when surface auto-sets the text color
        val value = TextStyle(color = Color(0xFFFFFFFF.toInt()))
        CurrentTextStyleProvider(value = value) {
            children()
        }
    }
}