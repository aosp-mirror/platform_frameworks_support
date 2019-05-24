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
import androidx.ui.graphics.Color
import androidx.ui.graphics.lerp
import androidx.ui.lerp

/**
 * TODO: kdoc
 */
sealed class AnimatedValue<T>(initVal : T, private val valueInterpolator: (T, T, Float) -> T) {

    var value: T = initVal
        internal set
    var isRunning : Boolean = false
        private set
    var onFinished : ((T, Float) -> Unit)? = null

    // TODO: remove this
    var onUpdate: (() -> Unit)? = null
    var target : T = initVal
        internal set

    internal var internalVelocity: Float = 0f
    private val defaultBuilder : () -> AnimationBuilder<T> = {PhysicsBuilder()}
    private lateinit var anim : AnimationWrapper<T>
    private var startTime : Long = -1

    private var frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            // TODO: Refactor out all the dependencies on Choreographer
            doAnimationFrame(frameTimeNanos / 1000000L)
        }
    }

    fun toValue(targetValue : T, anim: () -> AnimationBuilder<T> = defaultBuilder) {
        // Start animation here

        target = targetValue
        val animationWrapper = TargetBasedAnimationWrapper(value, internalVelocity, targetValue,
            valueInterpolator, anim.invoke().build())
        Log.w("LTD", "To value called: start value: $value, end value: $target")
        startAnimation(animationWrapper)
    }

    fun snapToValue(targetValue: T) {
        stop()
        // TODO: This is way too hacky
        value = targetValue
        target = targetValue
        onUpdate?.invoke()
    }

    fun stop() {
        if (isRunning) {
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
        if (!anim.isFinished(playtime)) {
            Choreographer.getInstance().postFrameCallback(frameCallback)
        } else {
            Log.w("LTD", "value = $value, playtime = $playtime, animation finished")
            endAnimation()
        }
        postAnimationFrame()
        onUpdate?.invoke()
        Log.w("LTD", "value = $value, playtime = $playtime, isRunning = $isRunning")
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
        onFinished?.invoke(value, internalVelocity)
        Log.w("LTD", "end animation")
    }


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
}

/**
 * TODO: kdoc
 */
class AnimatedColor(initVal: Color) : AnimatedValue<Color>(initVal, ::lerp)

/**
 * TODO: kdoc
 */
class AnimatedFloat(initVal: Float) : AnimatedValue<Float>(initVal, ::lerp) {

    val velocity: Float
        get() = internalVelocity

    private var min : Float = Float.NEGATIVE_INFINITY
    private var max : Float = Float.POSITIVE_INFINITY

    fun setBounds(min: Float = Float.NEGATIVE_INFINITY, max: Float = Float.POSITIVE_INFINITY) {
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
    fun decay(startVelocity : Float) {
        // start from current value with the given internalVelocity
        val animWrapper = DecayAnimationWrapper(value, startVelocity, ExponentialDecay())
        target = animWrapper.getValue(Int.MAX_VALUE.toLong())
        startAnimation(animWrapper)
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

private val exampleAnimatedValue = AnimatedFloat(100f).apply {
    toValue(0f) {
        physics { dampingRatio = 2.0f }
    }
    toValue(150f) { snap() }

    toValue(-100f) {
            keyframes {
            duration = 200
            150f at 100 }
    }

}