/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.core.widget

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.TextView
import androidx.test.InstrumentationRegistry
import androidx.test.annotation.UiThreadTest
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
@SmallTest
class TextViewTest {

    private val context = InstrumentationRegistry.getContext()
    private val view = TextView(context)

    @UiThreadTest
    @Test fun doBeforeTextChanged() {
        val called = AtomicBoolean()
        view.doBeforeTextChanged { _, _, _, _ ->
            called.set(true)
        }

        view.text = "text"

        assertTrue(called.get())
    }

    @UiThreadTest
    @Test fun doOnTextChanged() {
        val called = AtomicBoolean()
        view.doOnTextChanged { _, _, _, _ ->
            called.set(true)
        }

        view.text = "text"

        assertTrue(called.get())
    }

    @UiThreadTest
    @Test fun doAfterTextChanged() {
        val called = AtomicBoolean()
        view.doAfterTextChanged { _ ->
            called.set(true)
        }

        view.text = "text"

        assertTrue(called.get())
    }

    @Test
    fun updateCompoundDrawables() {
        val drawable = ColorDrawable(Color.RED)
        view.updateCompoundDrawables(left = drawable, right = drawable)

        assertEquals(drawable, view.compoundDrawables[0])
        assertEquals(drawable, view.compoundDrawables[2])
        assertNull(view.compoundDrawables[1])
        assertNull(view.compoundDrawables[3])
    }

    @Test
    fun updateCompoundDrawables_keepsDrawables() {
        val drawableRed = ColorDrawable(Color.RED)
        val drawableBlack = ColorDrawable(Color.BLACK)
        view.updateCompoundDrawables(left = drawableRed, top = drawableBlack)
        view.updateCompoundDrawables(right = drawableBlack, top = null)
        assertEquals(drawableRed, view.compoundDrawables[0])
        assertEquals(drawableBlack, view.compoundDrawables[2])
        assertNull(view.compoundDrawables[1])
        assertNull(view.compoundDrawables[3])
    }

    @Test
    fun updateCompoundDrawablesWithIntrinsicBounds() {
        val drawable = ColorDrawable(Color.RED)
        view.updateCompoundDrawablesWithIntrinsicBounds(top = drawable, bottom = drawable)

        assertEquals(drawable, view.compoundDrawables[1])
        assertEquals(drawable, view.compoundDrawables[3])
        assertNull(view.compoundDrawables[0])
        assertNull(view.compoundDrawables[2])
    }

    @Test
    fun updateCompoundDrawablesWithIntrinsicBounds_keepsDrawables() {
        val drawableRed = ColorDrawable(Color.RED)
        val drawableBlack = ColorDrawable(Color.BLACK)
        view.updateCompoundDrawablesWithIntrinsicBounds(
                top = drawableRed, bottom = drawableBlack
        )
        view.updateCompoundDrawablesWithIntrinsicBounds(right = drawableBlack, bottom = null)

        assertEquals(drawableRed, view.compoundDrawables[1])
        assertEquals(drawableBlack, view.compoundDrawables[2])
        assertNull(view.compoundDrawables[0])
        assertNull(view.compoundDrawables[3])
    }

    @Test
    fun updateCompoundDrawablesWithIntrinsicBounds_acceptsUpdateTypeConstants() {
        val drawable = ColorDrawable(Color.RED)
        view.updateCompoundDrawablesWithIntrinsicBounds(drawable, drawable, drawable, drawable)
        view.updateCompoundDrawablesWithIntrinsicBounds(
                0,
                -1,
                0,
                -1
        )
        assertNull(view.compoundDrawables[0])
        assertEquals(drawable, view.compoundDrawables[1])
        assertNull(view.compoundDrawables[2])
        assertEquals(drawable, view.compoundDrawables[3])
    }

    @Test
    fun updateCompoundDrawablesWithIntrinsicBoundsWithResourceIds() {
        view.updateCompoundDrawablesWithIntrinsicBounds(
                left = android.R.drawable.btn_default, bottom = android.R.drawable.btn_default
        )

        assertNotNull(view.compoundDrawables[0])
        assertNotNull(view.compoundDrawables[3])
        assertNull(view.compoundDrawables[1])
        assertNull(view.compoundDrawables[2])
    }

    @Test
    fun updateCompoundDrawablesWithIntrinsicBoundsWithResourceIds_keepsDrawables() {
        view.updateCompoundDrawablesWithIntrinsicBounds(
                left = android.R.drawable.btn_default, bottom = android.R.drawable.btn_default
        )
        view.updateCompoundDrawablesWithIntrinsicBounds(
                left = 0, right = android.R.drawable.btn_default
        )

        assertNotNull(view.compoundDrawables[3])
        assertNotNull(view.compoundDrawables[2])
        assertNull(view.compoundDrawables[0])
        assertNull(view.compoundDrawables[1])
    }

    @Test
    fun updateCompoundDrawablesRelative() {
        val drawableRed = ColorDrawable(Color.RED)
        view.updateCompoundDrawablesRelative(
                start = drawableRed, bottom = drawableRed, end = drawableRed
        )

        assertEquals(drawableRed, view.compoundDrawablesRelative[0])
        assertEquals(drawableRed, view.compoundDrawablesRelative[2])
        assertEquals(drawableRed, view.compoundDrawablesRelative[3])
        assertNull(view.compoundDrawablesRelative[1])
    }

    @Test
    fun updateCompoundDrawablesRelative_keepsDrawables() {
        val drawableRed = ColorDrawable(Color.RED)
        val drawableBlack = ColorDrawable(Color.BLACK)
        view.updateCompoundDrawablesRelative(
                start = drawableRed, bottom = drawableRed, end = drawableRed
        )
        view.updateCompoundDrawablesRelative(
                start = drawableBlack, top = drawableRed, end = null
        )

        assertEquals(drawableBlack, view.compoundDrawablesRelative[0])
        assertEquals(drawableRed, view.compoundDrawablesRelative[1])
        assertEquals(drawableRed, view.compoundDrawablesRelative[3])
        assertNull(view.compoundDrawablesRelative[2])
    }

    @Test
    fun updateCompoundDrawablesRelativeWithIntrinsicBounds() {
        val drawable = ColorDrawable(Color.RED)
        view.updateCompoundDrawablesRelativeWithIntrinsicBounds(top = drawable)

        assertEquals(drawable, view.compoundDrawablesRelative[1])
        assertNull(view.compoundDrawablesRelative[0])
        assertNull(view.compoundDrawablesRelative[2])
        assertNull(view.compoundDrawablesRelative[3])
    }

    @Test
    fun updateCompoundDrawablesRelativeWithIntrinsicBounds_keepsDrawables() {
        val drawableRed = ColorDrawable(Color.RED)
        val drawableBlack = ColorDrawable(Color.BLACK)
        view.updateCompoundDrawablesRelativeWithIntrinsicBounds(
                top = drawableRed, bottom = drawableRed
        )
        view.updateCompoundDrawablesRelativeWithIntrinsicBounds(
                bottom = null, end = drawableBlack
        )

        assertEquals(drawableRed, view.compoundDrawablesRelative[1])
        assertEquals(drawableBlack, view.compoundDrawablesRelative[2])
        assertNull(view.compoundDrawablesRelative[0])
        assertNull(view.compoundDrawablesRelative[3])
    }

    @Test
    fun updateCompoundDrawablesRelativeWithIntrinsicBounds_acceptsUpdateTypeConstants() {
        val drawable = ColorDrawable(Color.RED)
        view.updateCompoundDrawablesRelativeWithIntrinsicBounds(
                drawable,
                drawable,
                drawable,
                drawable
        )
        view.updateCompoundDrawablesRelativeWithIntrinsicBounds(
                0,
                -1,
                0,
                -1
        )
        assertNull(view.compoundDrawables[0])
        assertEquals(drawable, view.compoundDrawables[1])
        assertNull(view.compoundDrawables[2])
        assertEquals(drawable, view.compoundDrawables[3])
    }

    @Test
    fun updateCompoundDrawablesRelativeWithIntrinsicBoundsWithResourceIds() {
        view.updateCompoundDrawablesRelativeWithIntrinsicBounds(
                start = android.R.drawable.btn_default,
                top = android.R.drawable.btn_default,
                bottom = android.R.drawable.btn_default,
                end = android.R.drawable.btn_default
        )

        assertNotNull(view.compoundDrawablesRelative[0])
        assertNotNull(view.compoundDrawablesRelative[1])
        assertNotNull(view.compoundDrawablesRelative[2])
        assertNotNull(view.compoundDrawablesRelative[3])
    }

    @Test
    fun updateCompoundDrawablesRelativeWithIntrinsicBoundsWithResourceIds_keepsDrawables() {
        view.updateCompoundDrawablesRelativeWithIntrinsicBounds(
                start = android.R.drawable.btn_default,
                top = android.R.drawable.btn_default,
                bottom = android.R.drawable.btn_default,
                end = android.R.drawable.btn_default
        )
        view.updateCompoundDrawablesRelativeWithIntrinsicBounds(start = 0, end = 0)

        assertNotNull(view.compoundDrawablesRelative[1])
        assertNotNull(view.compoundDrawablesRelative[3])
        assertNull(view.compoundDrawablesRelative[0])
        assertNull(view.compoundDrawablesRelative[2])
    }
}
