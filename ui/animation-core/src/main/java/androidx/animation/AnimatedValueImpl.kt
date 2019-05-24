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

typealias OnFinishedCallback = ((Boolean) -> Unit)

/**
 * Dynamic target animation allows and anticipates the animation target to change frequently. When
 * the target changes as the animation is in-flight, the animation is expected to maintain make
 * a continuous transition to the new target.
 */
interface DynamicTargetAnimation<T> {
    /**
     * Current value of the animation.
     */
    val value: T
    /**
     * Indicates whether the animation is running.
     */
    val isRunning: Boolean
    /**
     * The target of the current animation. This target will not be the same as the value of the
     * animation, until the animation finishes un-interrupted.
     */
    val target: T

    // TODO: use lambda with default values to combine the following 4 methods when b/134103877
    //  is fixed
    fun toValue(targetValue: T)
    fun toValue(targetValue: T, onFinished: OnFinishedCallback)
    fun toValue(targetValue: T, anim: AnimationBuilder<T> = PhysicsBuilder())
    /**
     * Sets the target value, which effectively starts an animation to change the value from [value]
     * to the target value. If there is already an animation in flight, this method will interrupt
     * the ongoing animation, invoke [onFinished] that is associated with that animation, and start
     * a new animation from the current value to the new target value.
     *
     * @param targetValue The new value to animate to
     * @param anim The animation that will be used to animate from the current value to the new
     *             target value
     * @param onFinished A callback that will be invoked when the animation reaches the target or
     *                   gets canceled.
     */
    fun toValue(
        targetValue: T,
        anim: AnimationBuilder<T> = PhysicsBuilder(),
        onFinished: OnFinishedCallback
    )

    /**
     * Sets the current value to the target value immediately, without any animation.
     *
     * @param targetValue The new target value to set [value] to.
     */
    fun snapToValue(targetValue: T)

    /**
     * Stops any on-going animation. No op if no animation is running. Note that this method does
     * not skip the animation value to its target value. Rather the animation will be stopped in its
     * track.
     */
    fun stop()
}

/**
 * Float dynamic target animation is specifically designed to animate floats. Aside from the
 * functionality it inherits from [DynamicTargetAnimation], it supports the definition of bounds,
 * and understands the concept of fling. Once bounds are defined using [setBounds], the animation
 * will consider itself finished when it reaches the upper or lower bound, even when the velocity is
 * non-zero. [fling] starts an animation with a pre-defined velocity (usually from user's fling
 * gesture), and gradually slows down before it eventually comes to rest (i.e. finishes).
 */
interface FloatDynamicTargetAnimation : DynamicTargetAnimation<Float> {
    /**
     * Velocity of the current animation.
     */
    val velocity: Float

    /**
     * Indicate whether there is an on-going fling animation.
     */
    val isFlinging: Boolean

    /**
     * Sets up the bounds that the animation should be constrained to. Note that when the animation
     * reaches the bounds it will stop right away, even when there is remaining velocity.
     */
    fun setBounds(min: Float = Float.NEGATIVE_INFINITY, max: Float = Float.POSITIVE_INFINITY)

    /**
     * Starts a fling animation with the specified starting velocity.
     *
     * @param startVelocity Starting velocity of the fling animation
     * @param decay The decay animation used for slowing down the animation from the starting
     *              velocity
     * @param onFinished An optional callback that will be invoked when this fling animation is
     *                   finished.
     */
    fun fling(
        startVelocity: Float,
        decay: DecayAnimation = ExponentialDecay(),
        onFinished: OnFinishedCallback? = null
    )
    // TODO: Devs may want to change the target animation based on how close the target is to the
    //       snapping position.
    /**
     * Starts a fling animation with the specified starting velocity.
     *
     * @param startVelocity Starting velocity of the fling animation
     * @param adjustTarget A lambda that takes in the projected destination based on the decay
     *                     animation, and returns a new destination.
     * @param decay The decay animation used for slowing down the animation from the starting
     *              velocity
     * @param targetAnimation This animation will be used if [adjustTarget] returns a different
     *              target position than the projected destination.
     * @param onFinished An optional callback that will be invoked when this fling animation is
     *                   finished.
     */
    fun fling(
        startVelocity: Float,
        adjustTarget: ((Float) -> Float),
        decay: DecayAnimation = ExponentialDecay(),
        targetAnimation: AnimationBuilder<Float> = PhysicsBuilder(),
        onFinished: OnFinishedCallback? = null
    )

    // TODO: Decay animation should support value/velocity threshold
}

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

    override var target: T = initVal
        internal set

    // TODO: remove this
    var onValueChanged: ((T) -> Unit)? = null

    internal var internalVelocity: Float = 0f
    internal var onFinished: OnFinishedCallback? = null
    private lateinit var anim: AnimationWrapper<T>
    private var startTime: Long = -1
    private val defaultAnimation = PhysicsBuilder<T>()

    private var frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            // TODO: Refactor out all the dependencies on Choreographer
            doAnimationFrame(frameTimeNanos / 1000000L)
        }
    }

    override fun toValue(targetValue: T) {
        toValueInternal(targetValue, null, PhysicsBuilder())
    }
    override fun toValue(targetValue: T, onFinished: OnFinishedCallback) {
        toValueInternal(targetValue, onFinished, PhysicsBuilder())
    }
    override fun toValue(
        targetValue: T,
        anim: AnimationBuilder<T>,
        onFinished: OnFinishedCallback
    ) {
        toValueInternal(targetValue, onFinished, anim)
    }
    override fun toValue(targetValue: T, anim: AnimationBuilder<T>) {
        toValueInternal(targetValue, null, anim)
    }

    private fun toValueInternal(
        targetValue: T,
        onFinished: OnFinishedCallback?,
        anim: AnimationBuilder<T>
    ) {
        // Start animation here
        if (isRunning) {
            notifyFinished(false)
        }

        target = targetValue
        val animationWrapper = TargetBasedAnimationWrapper(value, internalVelocity, targetValue,
            valueInterpolator, anim.build())

        if (DEBUG) {
            Log.w(
                "AnimValue", "To value called: start value: $value," +
                        "end value: $target, velocity: $internalVelocity"
            )
        }
        this.onFinished = onFinished
        startAnimation(animationWrapper)
    }

    override fun snapToValue(targetValue: T) {
        stop()
        value = targetValue
        target = targetValue
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
class AnimatedFloatImpl(initVal: Float)
    : AnimatedValueImpl<Float>(initVal, ::lerp), FloatDynamicTargetAnimation {

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

    override fun snapToValue(targetValue: Float) {
        super.snapToValue(targetValue.coerceIn(min, max))
    }
    // TODO: Figure out an API for customizing the type of decay & the friction
    override fun fling(
        startVelocity: Float,
        decay: DecayAnimation,
        onFinished: OnFinishedCallback?
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
        target = decay.getTarget(value, startVelocity)
        val animWrapper = DecayAnimationWrapper(value, startVelocity, decay)
        startAnimation(animWrapper)
    }

    override fun fling(
        startVelocity: Float,
        adjustTarget: ((Float) -> Float),
        decay: DecayAnimation,
        targetAnimation: AnimationBuilder<Float>,
        onFinished: OnFinishedCallback?
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
        target = decay.getTarget(value, startVelocity)
        val newTarget = adjustTarget(target)
        if (DEBUG) {
            Log.w("AnimFloat", "original target: $target, new target: $newTarget")
        }
        if (newTarget == target) {
            val animWrapper = DecayAnimationWrapper(value, startVelocity, decay)
            startAnimation(animWrapper)
        } else {
            target = newTarget
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
