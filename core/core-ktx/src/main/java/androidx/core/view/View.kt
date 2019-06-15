/*
 * Copyright (C) 2017 The Android Open Source Project
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

@file:Suppress("NOTHING_TO_INLINE") // Aliases to other public API.

package androidx.core.view

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.core.graphics.applyCanvas

/**
 * Performs the given action when this view is next laid out.
 *
 * The action will only be invoked once on the next layout and then removed.
 *
 * @see doOnLayout
 */
inline fun View.doOnNextLayout(crossinline action: (view: View) -> Unit) {
    addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
        override fun onLayoutChange(
            view: View,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            view.removeOnLayoutChangeListener(this)
            action(view)
        }
    })
}

/**
 * Performs the given action when this view is laid out. If the view has been laid out and it
 * has not requested a layout, the action will be performed straight away, otherwise the
 * action will be performed after the view is next laid out.
 *
 * The action will only be invoked once on the next layout and then removed.
 *
 * @see doOnNextLayout
 */
inline fun View.doOnLayout(crossinline action: (view: View) -> Unit) {
    if (ViewCompat.isLaidOut(this) && !isLayoutRequested) {
        action(this)
    } else {
        doOnNextLayout {
            action(it)
        }
    }
}

/**
 * Performs the given action when the view tree is about to be drawn.
 *
 * The action will only be invoked once prior to the next draw and then removed.
 */
inline fun View.doOnPreDraw(crossinline action: (view: View) -> Unit): OneShotPreDrawListener {
    return OneShotPreDrawListener.add(this) { action(this) }
}

/**
 * Performs the given action when this view is attached to a window. If the view is already
 * attached to a window the action will be performed immediately, otherwise the
 * action will be performed after the view is next attached.
 *
 * The action will only be invoked once, and any listeners will then be removed.
 *
 * @see doOnDetach
 */
inline fun View.doOnAttach(crossinline action: (view: View) -> Unit) {
    if (ViewCompat.isAttachedToWindow(this)) {
        action(this)
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                removeOnAttachStateChangeListener(this)
                action(view)
            }

            override fun onViewDetachedFromWindow(view: View) {}
        })
    }
}

/**
 * Performs the given action when this view is detached from a window. If the view is not
 * attached to a window the action will be performed immediately, otherwise the
 * action will be performed after the view is detached from its current window.
 *
 * The action will only be invoked once, and any listeners will then be removed.
 *
 * @see doOnAttach
 */
inline fun View.doOnDetach(crossinline action: (view: View) -> Unit) {
    if (!ViewCompat.isAttachedToWindow(this)) {
        action(this)
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {}

            override fun onViewDetachedFromWindow(view: View) {
                removeOnAttachStateChangeListener(this)
                action(view)
            }
        })
    }
}

/**
 * Updates this view's relative padding. This version of the method allows using named parameters
 * to just set one or more axes.
 *
 * @see View.setPaddingRelative
 */
@RequiresApi(17)
inline fun View.updatePaddingRelative(
    @Px start: Int = paddingStart,
    @Px top: Int = paddingTop,
    @Px end: Int = paddingEnd,
    @Px bottom: Int = paddingBottom
) {
    setPaddingRelative(start, top, end, bottom)
}

/**
 * Updates this view's padding. This version of the method allows using named parameters
 * to just set one or more axes.
 *
 * @see View.setPadding
 */
inline fun View.updatePadding(
    @Px left: Int = paddingLeft,
    @Px top: Int = paddingTop,
    @Px right: Int = paddingRight,
    @Px bottom: Int = paddingBottom
) {
    setPadding(left, top, right, bottom)
}

/**
 * Sets the view's padding. This version of the method sets all axes to the provided size.
 *
 * @see View.setPadding
 */
inline fun View.setPadding(@Px size: Int) {
    setPadding(size, size, size, size)
}

/**
 * Version of [View.postDelayed] which re-orders the parameters, allowing the action to be placed
 * outside of parentheses.
 *
 * ```
 * view.postDelayed(200) {
 *     doSomething()
 * }
 * ```
 *
 * @return the created Runnable
 */
inline fun View.postDelayed(delayInMillis: Long, crossinline action: () -> Unit): Runnable {
    val runnable = Runnable { action() }
    postDelayed(runnable, delayInMillis)
    return runnable
}

/**
 * Version of [View.postOnAnimationDelayed] which re-orders the parameters, allowing the action
 * to be placed outside of parentheses.
 *
 * ```
 * view.postOnAnimationDelayed(16) {
 *     doSomething()
 * }
 * ```
 *
 * @return the created Runnable
 */
@RequiresApi(16)
inline fun View.postOnAnimationDelayed(
    delayInMillis: Long,
    crossinline action: () -> Unit
): Runnable {
    val runnable = Runnable { action() }
    postOnAnimationDelayed(runnable, delayInMillis)
    return runnable
}

/**
 * Return a [Bitmap] representation of this [View].
 *
 * The resulting bitmap will be the same width and height as this view's current layout
 * dimensions. This does not take into account any transformations such as scale or translation.
 *
 * Note, this will use the software rendering pipeline to draw the view to the bitmap. This may
 * result with different drawing to what is rendered on a hardware accelerated canvas (such as
 * the device screen).
 *
 * If this view has not been laid out this method will throw a [IllegalStateException].
 *
 * @param config Bitmap config of the desired bitmap. Defaults to [Bitmap.Config.ARGB_8888].
 */
fun View.drawToBitmap(config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap {
    if (!ViewCompat.isLaidOut(this)) {
        throw IllegalStateException("View needs to be laid out before calling drawToBitmap()")
    }
    return Bitmap.createBitmap(width, height, config).applyCanvas {
        translate(-scrollX.toFloat(), -scrollY.toFloat())
        draw(this)
    }
}

/**
 * Returns true when this view's visibility is [View.VISIBLE], false otherwise.
 *
 * ```
 * if (view.isVisible) {
 *     // Behavior...
 * }
 * ```
 *
 * Setting this property to true sets the visibility to [View.VISIBLE], false to [View.GONE].
 *
 * ```
 * view.isVisible = true
 * ```
 */
inline var View.isVisible: Boolean
    get() = visibility == View.VISIBLE
    set(value) {
        visibility = if (value) View.VISIBLE else View.GONE
    }

/**
 * Returns true when this view's visibility is [View.INVISIBLE], false otherwise.
 *
 * ```
 * if (view.isInvisible) {
 *     // Behavior...
 * }
 * ```
 *
 * Setting this property to true sets the visibility to [View.INVISIBLE], false to [View.VISIBLE].
 *
 * ```
 * view.isInvisible = true
 * ```
 */
inline var View.isInvisible: Boolean
    get() = visibility == View.INVISIBLE
    set(value) {
        visibility = if (value) View.INVISIBLE else View.VISIBLE
    }

/**
 * Returns true when this view's visibility is [View.GONE], false otherwise.
 *
 * ```
 * if (view.isGone) {
 *     // Behavior...
 * }
 * ```
 *
 * Setting this property to true sets the visibility to [View.GONE], false to [View.VISIBLE].
 *
 * ```
 * view.isGone = true
 * ```
 */
inline var View.isGone: Boolean
    get() = visibility == View.GONE
    set(value) {
        visibility = if (value) View.GONE else View.VISIBLE
    }

/**
 * Executes [block] with the View's layoutParams and reassigns the layoutParams with the
 * updated version.
 *
 * @see View.getLayoutParams
 * @see View.setLayoutParams
 **/
inline fun View.updateLayoutParams(block: ViewGroup.LayoutParams.() -> Unit) {
    updateLayoutParams<ViewGroup.LayoutParams>(block)
}

/**
 * Executes [block] with a typed version of the View's layoutParams and reassigns the
 * layoutParams with the updated version.
 *
 * @see View.getLayoutParams
 * @see View.setLayoutParams
 **/
@JvmName("updateLayoutParamsTyped")
inline fun <reified T : ViewGroup.LayoutParams> View.updateLayoutParams(block: T.() -> Unit) {
    val params = layoutParams as T
    block(params)
    layoutParams = params
}

/**
 * Returns the left margin if this view's [LayoutParams] is a [ViewGroup.MarginLayoutParams],
 * otherwise 0.
 *
 * @see ViewGroup.MarginLayoutParams
 */
inline val View.marginLeft: Int
    get() = (layoutParams as? MarginLayoutParams)?.leftMargin ?: 0

/**
 * Returns the top margin if this view's [LayoutParams] is a [ViewGroup.MarginLayoutParams],
 * otherwise 0.
 *
 * @see ViewGroup.MarginLayoutParams
 */
inline val View.marginTop: Int
    get() = (layoutParams as? MarginLayoutParams)?.topMargin ?: 0

/**
 * Returns the right margin if this view's [LayoutParams] is a [ViewGroup.MarginLayoutParams],
 * otherwise 0.
 *
 * @see ViewGroup.MarginLayoutParams
 */
inline val View.marginRight: Int
    get() = (layoutParams as? MarginLayoutParams)?.rightMargin ?: 0

/**
 * Returns the bottom margin if this view's [LayoutParams] is a [ViewGroup.MarginLayoutParams],
 * otherwise 0.
 *
 * @see ViewGroup.MarginLayoutParams
 */
inline val View.marginBottom: Int
    get() = (layoutParams as? MarginLayoutParams)?.bottomMargin ?: 0

/**
 * Returns the start margin if this view's [LayoutParams] is a [ViewGroup.MarginLayoutParams],
 * otherwise 0.
 *
 * @see ViewGroup.MarginLayoutParams
 * @see MarginLayoutParamsCompat.getMarginStart
 */
inline val View.marginStart: Int
    get() {
        val lp = layoutParams
        return if (lp is MarginLayoutParams) MarginLayoutParamsCompat.getMarginStart(lp) else 0
    }

/**
 * Returns the end margin if this view's [LayoutParams] is a [ViewGroup.MarginLayoutParams],
 * otherwise 0.
 *
 * @see ViewGroup.MarginLayoutParams
 * @see MarginLayoutParamsCompat.getMarginEnd
 */
inline val View.marginEnd: Int
    get() {
        val lp = layoutParams
        return if (lp is MarginLayoutParams) MarginLayoutParamsCompat.getMarginEnd(lp) else 0
    }
