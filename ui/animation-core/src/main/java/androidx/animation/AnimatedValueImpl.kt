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

package androidx.animation

import android.util.Log
import android.view.Choreographer
import androidx.annotation.RestrictTo
import androidx.ui.lerp

/**
 * The functionality of AnimatedValue as it is designed. This class is not meant to be used by
 * developers directly, therefore it is not public. Devs should use animation.ui.AnimatedValue,
 * which is a model class that delegates to AnimatedValueImpl.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
open class AnimatedValueImpl<T>(
    initVal: T,
    private val valueInterpolator: (T, T, Float) -> T
) : DynamicTargetAnimation<T> {
    override var value: T = initVal
        internal set(newVal: T) {
            field = newVal
            onValueChanged?.invoke(newVal)
        }

    override var isRunning: Boolean = false
        internal set

    override var targetValue: T = initVal
        internal set

    // TODO: remove this
    var onValueChanged: ((T) -> Unit)? = null

    internal var internalVelocity: Float = 0f
    internal var onFinished: ((Boolean) -> Unit)? = null
    private lateinit var anim: AnimationWrapper<T>
    private var startTime: Long = -1
    private val defaultAnimation = PhysicsBuilder<T>()

    private var frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            // TODO: Refactor out all the dependencies on Choreographer
            doAnimationFrame(frameTimeNanos / 1000000L)
        }
    }

    override fun animateTo(targetValue: T) {
        toValueInternal(targetValue, null, PhysicsBuilder())
    }
    override fun animateTo(targetValue: T, onFinished: (Boolean) -> Unit) {
        toValueInternal(targetValue, onFinished, PhysicsBuilder())
    }
    override fun animateTo(
        targetValue: T,
        anim: AnimationBuilder<T>,
        onFinished: (Boolean) -> Unit
    ) {
        toValueInternal(targetValue, onFinished, anim)
    }
    override fun animateTo(targetValue: T, anim: AnimationBuilder<T>) {
        toValueInternal(targetValue, null, anim)
    }

    private fun toValueInternal(
        targetValue: T,
        onFinished: ((Boolean) -> Unit)?,
        anim: AnimationBuilder<T>
    ) {
        if (isRunning) {
            notifyFinished(false)
        }

        this.targetValue = targetValue
        val animationWrapper = TargetBasedAnimationWrapper(value, internalVelocity, targetValue,
            valueInterpolator, anim.build())

        if (DEBUG) {
            Log.w(
                "AnimValue", "To value called: start value: $value," +
                        "end value: $targetValue, velocity: $internalVelocity"
            )
        }
        this.onFinished = onFinished
        startAnimation(animationWrapper)
    }

    override fun snapTo(targetValue: T) {
        stop()
        value = targetValue
        this.targetValue = targetValue
    }

    override fun stop() {
        if (isRunning) {
            endAnimation(false)
        }
    }

    internal fun notifyFinished(finished: Boolean) {
        val onFinished = this.onFinished
        this.onFinished = null
        onFinished?.invoke(finished)
    }

    internal open fun doAnimationFrame(time: Long) {
        var playtime: Long
        if (startTime == -1L) {
            startTime = time
            playtime = 0
        } else {
            playtime = time - startTime
        }
        value = anim.getValue(playtime)
        internalVelocity = anim.getVelocity(playtime)
        val animationFinished = anim.isFinished(playtime)
        if (!animationFinished) {
            Choreographer.getInstance().postFrameCallback(frameCallback)
            if (DEBUG) {
                Log.w(
                    "AnimValue",
                    "value = $value, playtime = $playtime, velocity: $internalVelocity"
                )
            }
        } else {
            if (DEBUG) {
                Log.w("AnimValue", "value = $value, playtime = $playtime, animation finished")
            }
            endAnimation()
        }
    }

    internal fun startAnimation(anim: AnimationWrapper<T>) {
        this.anim = anim
        startTime = -1
        if (!isRunning) {
            isRunning = true
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
        if (DEBUG) {
            Log.w("AnimValue", "start animation")
        }
    }

    internal fun endAnimation(finished: Boolean = true) {
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        isRunning = false
        startTime = -1
        if (DEBUG) {
            Log.w("AnimValue", "end animation")
        }
        notifyFinished(finished)
    }
}

/**
 * This class inherits most of the functionality from AnimatedValueImpl. In addition, it tracks
 * velocity and handles fling. This class serves as a delegate for AnimatedFloat class in
 * animation.ui package. The delegation is necessary as Model classes do not support subclassing.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class AnimatedScrollImpl(initVal: Float)
    : AnimatedValueImpl<Float>(initVal, ::lerp), ScrollAnimation {

    override var velocity: Float = 0f
        get() = internalVelocity

    override var isFlinging: Boolean = false

    private var min: Float = Float.NEGATIVE_INFINITY
    private var max: Float = Float.POSITIVE_INFINITY

    override fun setBounds(min: Float, max: Float) {
        if (max < min) {
            // throw exception?
        }
        this.min = min
        this.max = max
    }

    override fun snapTo(targetValue: Float) {
        super.snapTo(targetValue.coerceIn(min, max))
    }
    // TODO: Figure out an API for customizing the type of decay & the friction
    override fun fling(
        startVelocity: Float,
        decay: DecayAnimation,
        onFinished: ((Boolean) -> Unit)?
    ) {
        if (isRunning) {
            notifyFinished(false)
        }
        isFlinging = true

        this.onFinished = {
            isFlinging = false
            onFinished?.invoke(it)
        }

        // start from current value with the given internalVelocity
        targetValue = decay.getTarget(value, startVelocity)
        val animWrapper = DecayAnimationWrapper(value, startVelocity, decay)
        startAnimation(animWrapper)
    }

    override fun fling(
        startVelocity: Float,
        adjustTarget: ((Float) -> Float),
        decay: DecayAnimation,
        targetAnimation: AnimationBuilder<Float>,
        onFinished: ((Boolean) -> Unit)?
    ) {
        if (isRunning) {
            notifyFinished(false)
        }
        isFlinging = true

        this.onFinished = {
            isFlinging = false
            onFinished?.invoke(it)
        }

        // start from current value with the given internalVelocity
        targetValue = decay.getTarget(value, startVelocity)
        val newTarget = adjustTarget(targetValue)
        if (DEBUG) {
            Log.w("AnimFloat", "original targetValue: $targetValue, new target: $newTarget")
        }
        if (newTarget == targetValue) {
            val animWrapper = DecayAnimationWrapper(value, startVelocity, decay)
            startAnimation(animWrapper)
        } else {
            targetValue = newTarget
            val animWrapper = targetAnimation.createWrapper(value, startVelocity, newTarget, ::lerp)
            startAnimation(animWrapper)
        }
    }

    override fun doAnimationFrame(time: Long) {
        super.doAnimationFrame(time)
        if (value < min) {
            value = min
            endAnimation(true)
        } else if (value > max) {
            value = max
            stop()
            endAnimation(true)
        }
    }
}
