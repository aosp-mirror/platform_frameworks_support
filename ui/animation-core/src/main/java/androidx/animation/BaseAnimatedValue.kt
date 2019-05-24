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
import androidx.ui.graphics.Color
import androidx.ui.graphics.lerp
import androidx.ui.lerp

//TODO: Find a way to share these functions without requiring imports
/**
 * Creates a [Tween] animation, initialized with [init]
 *
 * @param init Initialization function for the [Tween] animation
 */
fun <T> tween(init: TweenBuilder<T>.() -> Unit): DurationBasedAnimationBuilder<T> =
    TweenBuilder<T>().apply(init)

/**
 * Creates a [Physics] animation, initialized with [init]
 *
 * @param init Initialization function for the [Physics] animation
 */
fun <T> physics(init: PhysicsBuilder<T>.() -> Unit): AnimationBuilder<T> =
    PhysicsBuilder<T>().apply(init)

/**
 * Creates a [Keyframes] animation, initialized with [init]
 *
 * @param init Initialization function for the [Keyframes] animation
 */
fun <T> keyframes(init: KeyframesBuilder<T>.() -> Unit): DurationBasedAnimationBuilder<T> =
    KeyframesBuilder<T>().apply(init)

/**
 * Creates a [Repeatable] animation, initialized with [init]
 *
 * @param init Initialization function for the [Repeatable] animation
 */
fun <T> repeatable(init: RepeatableBuilder<T>.() -> Unit): AnimationBuilder<T> =
    RepeatableBuilder<T>().apply(init)

/**
 * Creates a Snap animation for immediately switching the animating value to the end value.
 */
fun <T> snap(): AnimationBuilder<T> = SnapBuilder()

interface DynamicTargetAnimation<T> {
    val value : T
    val isRunning : Boolean
    val target: T
    // TODO: use lambda with default values to combine the following 4 methods when b/134103877
    //  is fixed
    fun toValue(targetValue: T)
    fun toValue(targetValue: T, onFinished: ((Boolean) -> Unit))
    fun toValue(targetValue: T, anim: AnimationBuilder<T> = physics {})
    fun toValue(targetValue: T, anim: AnimationBuilder<T> = physics {},
                onFinished: ((Boolean) -> Unit))
    fun snapToValue(targetValue: T)
    fun stop()
}

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
sealed class AnimatedValueImpl<T>(initVal : T,
                              private val valueInterpolator: (T, T, Float) -> T
) : DynamicTargetAnimation<T> {
    override var value: T = initVal
        internal set

    override var isRunning : Boolean = false
        internal set

    override var target : T = initVal
        internal set

    // TODO: remove this
    var onUpdate: (() -> Unit)? = null

    internal var internalVelocity: Float = 0f
    internal var onFinished : ((Boolean) -> Unit)? = null
    private lateinit var anim : AnimationWrapper<T>
    private var startTime : Long = -1
    private val defaultAnimation = { physics<T>{} }

    private var frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            // TODO: Refactor out all the dependencies on Choreographer
            doAnimationFrame(frameTimeNanos / 1000000L)
        }
    }

    override fun toValue(targetValue: T) {
        toValueInternal(targetValue, null, physics {})
    }
    override fun toValue(targetValue: T, onFinished: ((Boolean) -> Unit)) {
        toValueInternal(targetValue, onFinished, physics {})
    }
    override fun toValue(targetValue: T, anim: AnimationBuilder<T>,
                         onFinished: ((Boolean) -> Unit)) {
        toValueInternal(targetValue, onFinished, anim)
    }
    override fun toValue(targetValue: T, anim: AnimationBuilder<T>) {
        toValueInternal(targetValue, null, anim)
    }

    private fun toValueInternal(targetValue : T,
                onFinished: ((Boolean) -> Unit)?,
                         anim: AnimationBuilder<T>) {
        // Start animation here
        if (isRunning) {
            this.onFinished?.invoke(false)
        }

        target = targetValue
        val animationWrapper = TargetBasedAnimationWrapper(value, internalVelocity, targetValue,
            valueInterpolator, anim.build())
        Log.w("LTD", "To value called: start value: $value, end value: $target, velocity: $internalVelocity")
        this.onFinished = onFinished
        startAnimation(animationWrapper)
    }

    override fun snapToValue(targetValue: T) {
        stop()
        // TODO: This is way too hacky
        value = targetValue
        target = targetValue
        onUpdate?.invoke()
    }

    override fun stop() {
        if (isRunning) {
            onFinished?.invoke(false)
            onFinished = null
            endAnimation()
        }
    }

    private fun doAnimationFrame(time: Long) {
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
        } else {
            Log.w("LTD", "value = $value, playtime = $playtime, animation finished")
            endAnimation()
        }
        postAnimationFrame()
        onUpdate?.invoke()
        Log.w("LTD", "value = $value, playtime = $playtime, isRunning = $isRunning")
        if (animationFinished) {
            val onFinished = this.onFinished
            this.onFinished = null
            onFinished?.invoke(true)
        }
    }

    // For subclasses to define behaviors, check values, stop animations as needed
    internal open fun postAnimationFrame() {}

    internal fun startAnimation(anim: AnimationWrapper<T>) {
        this.anim = anim
        startTime = -1
        if (!isRunning) {
            isRunning = true
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
        Log.w("LTD", "start animation")
    }

    private fun endAnimation() {
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        isRunning = false
        startTime = -1
        Log.w("LTD", "end animation")
    }

}

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class BaseAnimatedValue<T>(initVal: T, valueInterpolator: (T, T, Float) -> T)
    : AnimatedValueImpl<T>(initVal, valueInterpolator)

/**
 * TODO: kdoc
 */
/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class AnimatedColor(initVal: Color):  AnimatedValueImpl<Color>(initVal, ::lerp)

interface FloatDynamicTargetAnimation : DynamicTargetAnimation<Float> {
    val velocity : Float
    val isFlinging: Boolean
    fun setBounds(min: Float = Float.NEGATIVE_INFINITY, max: Float = Float.POSITIVE_INFINITY)
    fun fling(startVelocity: Float,
              decay: DecayAnimation<Float> = ExponentialDecay(),
              onFinished: ((Boolean) -> Unit)? = null)
    fun fling(
        startVelocity: Float,
        adjustTarget: ((Float) -> Float),
        decay: DecayAnimation<Float> = ExponentialDecay(),
        targetAnimation: AnimationBuilder<Float> = physics {},
        onFinished: ((Boolean) -> Unit)? = null )
}

/**
 * TODO: kdoc
 */
/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class AnimatedFloatImpl(initVal: Float)
    : AnimatedValueImpl<Float>(initVal, ::lerp), FloatDynamicTargetAnimation {

    override var velocity: Float = 0f
        get() = internalVelocity

    override var isFlinging: Boolean = false

    private val decay = ExponentialDecay()
    private var min: Float = Float.NEGATIVE_INFINITY
    private var max: Float = Float.POSITIVE_INFINITY

    override fun setBounds(min: Float, max: Float) {
        if (max < min) {
            // throw exception?
        }
        this.min = min
        this.max = max
    }

//    operator fun minusassign(newval: float) = tovalue(target - newval)
//    operator fun plusassign(newval: float) = tovalue(target + newval)
//    operator fun divassign(newval: float) = tovalue(target / newval)
//    operator fun timesassign(newval: float) = tovalue(target * newval)

    // TODO: Figure out an API for customizing the type of decay & the friction
    override fun fling(
        startVelocity: Float,
        decay : DecayAnimation<Float>,
        onFinished: ((Boolean) -> Unit)?
    ) {
        if (isRunning) {
            this.onFinished?.invoke(false)
        }
        isFlinging = true

        this.onFinished = {
            isFlinging = false
            onFinished?.invoke(it)
        }

        // start from current value with the given internalVelocity
        target = decay.getValue(Int.MAX_VALUE.toLong(), value, startVelocity, ::lerp)
        val animWrapper = DecayAnimationWrapper(value, startVelocity, decay)
        startAnimation(animWrapper)
    }

    override fun fling(
        startVelocity: Float,
        adjustTarget: ((Float) -> Float),
        decay : DecayAnimation<Float>,
        targetAnimation: AnimationBuilder<Float>,
        onFinished: ((Boolean) -> Unit)? ) {
        if (isRunning) {
            this.onFinished?.invoke(false)
        }
        isFlinging = true

        this.onFinished = {
            isFlinging = false
            onFinished?.invoke(it)
        }

        // start from current value with the given internalVelocity
        target = decay.getValue(Int.MAX_VALUE.toLong(), value, startVelocity, ::lerp)
        val newTarget = adjustTarget(target)
        Log.w("LTD", "original target: $target, new target: $newTarget")
        if (newTarget == target) {
            val animWrapper = DecayAnimationWrapper(value, startVelocity, decay)
            startAnimation(animWrapper)
        } else {
            target = newTarget
            val animWrapper = targetAnimation.createWrapper(value, startVelocity, newTarget, ::lerp)
            startAnimation(animWrapper)
        }
    }

    override fun postAnimationFrame() {
        if (value < min) {
            value = min
            stop()
        } else if (value > max) {
            value = max
            stop()
        }
    }
}

//private val exampleBaseAnimatedValue = AnimatedFloat(100f).apply {
//    toValue(0f) {
//        physics { dampingRatio = 2.0f }
//    }
//    toValue(150f) { snap() }
//    toValue(-100f) {
//            keyframes {
//            duration = 200
//            150f at 100 }
//    }

//}