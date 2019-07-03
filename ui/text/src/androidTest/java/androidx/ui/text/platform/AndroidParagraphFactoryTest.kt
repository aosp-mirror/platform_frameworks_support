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

package androidx.ui.text.platform

import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.ui.core.Density
import androidx.ui.text.TextStyle
import androidx.ui.text.style.ParagraphStyle
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class AndroidParagraphFactoryTest {
    private val defaultDensity = Density(density = 1f)
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun create_returns_AndroidParagraph() {
        val paragraph = AndroidParagraph.Factory(context).create(
            text = "",
            style = TextStyle(),
            paragraphStyle = ParagraphStyle(),
            textStyles = listOf(),
            density = defaultDensity
        )

        assertThat(paragraph).isNotNull()
        assertThat(paragraph).isInstanceOf(AndroidParagraph::class.java)
    }
}