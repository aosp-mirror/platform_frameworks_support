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

@Model
class AnimatedValue<T>(initValue: T, valueInterpolator: (T, T, Float) -> T) {
    private val animValue : BaseAnimatedValue<T> = BaseAnimatedValue(initValue, valueInterpolator)

    var value : T = initValue
    val isRunning : Boolean
        get() = animValue.isRunning
    val target: T
        get() = animValue.target

    init {
        animValue.onUpdate = { value = animValue.value }
    }

    fun toValue(targetValue : T,
                onFinished: ((Boolean) -> Unit)? = null,
                anim: () -> AnimationBuilder<T> = {physics{}}) {
        animValue.toValue(targetValue, onFinished, anim)
    }

    fun snapToValue(targetValue: T) {
        animValue.snapToValue(targetValue)
    }

    fun stop() {
        animValue.stop()
    }
}

@Model
class AnimatedFloat(initValue: Float) {
    private val animFloat : AnimatedFloatImpl = AnimatedFloatImpl(initValue)

    var value : Float = initValue
        private set
    val velocity : Float
        get() = animFloat.velocity
    val isRunning : Boolean
        get() = animFloat.isRunning
    val target: Float
        get() = animFloat.target

    init {
        animFloat.onUpdate = { value = animFloat.value }
    }

    fun toValue(targetValue : Float,
                onFinished: ((Boolean) -> Unit)? = null,
                anim: () -> AnimationBuilder<Float> = {physics{}}) {
        animFloat.toValue(targetValue, onFinished, anim)
    }

    fun snapToValue(targetValue: Float) {
        animFloat.snapToValue(targetValue)
    }

    fun setBounds(min: Float = Float.NEGATIVE_INFINITY, max: Float = Float.POSITIVE_INFINITY) {
        animFloat.setBounds(min, max)
    }

    fun decay(velocity: Float, onFinished: (AnimatedFloat.(Boolean) -> Unit)? = null) {
        val onFinishedTransformed : (Boolean) -> Unit = { finished ->
            onFinished?.invoke(this, finished)
        }
        animFloat.decay(velocity, onFinishedTransformed)
    }

    fun stop() {
        animFloat.stop()
    }

}
