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

import androidx.animation.AnimationBuilder
import androidx.animation.BaseAnimatedValue
import androidx.animation.physics
import androidx.compose.Model
import androidx.animation.AnimatedFloatImpl
import androidx.animation.DynamicTargetAnimation
import androidx.animation.FloatDynamicTargetAnimation

@Model
class AnimatedValue<T> private constructor(private val animValue: BaseAnimatedValue<T>)
    : DynamicTargetAnimation<T> by animValue {

    init {
        animValue.onUpdate = { value = animValue.value }
    }

    constructor(initValue: T, valueInterpolator: (T, T, Float) -> T)
            : this(BaseAnimatedValue(initValue, valueInterpolator)) {}

    override var value : T = animValue.value
        private set

}

@Model
class AnimatedFloat private constructor(private val animFloat: AnimatedFloatImpl)
    : FloatDynamicTargetAnimation by animFloat {
    init {
        animFloat.onUpdate = { value = animFloat.value }
    }

    constructor(initValue: Float) : this(AnimatedFloatImpl(initValue)) {}

    override var value : Float = animFloat.value
        private set
}
