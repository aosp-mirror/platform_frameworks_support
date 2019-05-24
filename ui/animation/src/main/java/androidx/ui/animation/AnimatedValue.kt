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

package androidx.ui.animation

import androidx.compose.Model
import androidx.animation.AnimatedScrollImpl
import androidx.animation.AnimatedValueImpl
import androidx.animation.DynamicTargetAnimation
import androidx.animation.ScrollAnimation
import androidx.ui.graphics.Color
import androidx.ui.graphics.lerp
import androidx.ui.lerp

/**
 * AnimatedValue is an animatable value holder. It can hold any type of value, and automatically
 * animate the value change when the value is changed via [animateTo]. AnimatedValue supports value
 * change during an ongoing value change animation. When that happens, a new animation will
 * transition AnimatedValue from its current value (i.e. value at the point of interruption) to the
 * new target. This ensures that the value change is always continuous.
 */
@Model
class AnimatedValue<T> private constructor(private val animValue: AnimatedValueImpl<T>)
    : DynamicTargetAnimation<T> by animValue {
    init {
        animValue.onValueChanged = { value = it }
    }

    constructor(initValue: T, valueInterpolator: (T, T, Float) -> T)
            : this(AnimatedValueImpl(initValue, valueInterpolator)) {}

    override var value: T = animValue.value
        private set
}

/**
 * AnimatedFloat holds a value of [Float] type and runs an animation whenever the value changes.
 */
fun AnimatedFloat(initValue: Float): DynamicTargetAnimation<Float> =
    AnimatedValue(initValue, ::lerp)

/**
 * AnimatedColor holds a value of [Color] type and runs an animation whenever the color value
 * changes.
 */
fun AnimatedColor(initValue: Color): DynamicTargetAnimation<Color> =
    AnimatedValue(initValue, ::lerp)

/**
 * AnimatedScroll is a value holder for floats that automatically kicks off an animation whenever
 * the value changes via [animateTo]. AnimatedScroll tracks not only its value but also its velocity
 * as the animation goes on. When new value change interrupts an in-flight animation, the value and
 * velocity will be used as the starting condition of the new value change animation. The resulting
 * animation is smooth as both its value and velocity are continuous.
 *
 * AnimatedScroll also supports fling, which is a [DecayAnimation] that takes in a starting velocity
 * and slows down over time.
 */
@Model
class AnimatedScroll private constructor(private val animScroll: AnimatedScrollImpl)
    : ScrollAnimation by animScroll {

    init {
        animScroll.onValueChanged = { value = it }
    }

    constructor(initValue: Float) : this(AnimatedScrollImpl(initValue)) {}

    override var value: Float = animScroll.value
        private set
}
