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

package androidx.ui.foundation.gestures

import androidx.animation.AnimationBuilder
import androidx.animation.DecayAnimation
import androidx.animation.ExponentialDecay
import androidx.animation.PhysicsBuilder
import androidx.animation.TargetAnimation
import kotlin.math.abs

/**
 * Interface to provide value adjustment in [AnimatedDraggable].
 *
 * When drag has ended, fling occurs and from that point value adjustment is the only way to control
 * what's the result value will be after the fling.
 *
 * If you need natural fling support, use [DefaultAnimationAdjustment] or
 * [DecayAdjustment] to control how much friction is applied to the fling
 *
 * If you want to only be able to drag/animate between predefined set of values,
 * consider using [AnchorsAnimationAdjustment].
 *
 */
interface AnimationAdjustment {
    fun adjust(target: Float): TargetAnimation?
    val decayAnimation: DecayAnimation
}

/**
 * Anchors animations adjustment will make sure that after drag has ended,
 * the value will be animated to one of the points from the predefined set.
 *
 * This adjustment takes velocity into account, though value will be animated to the closest
 * point in set considering velocity.
 *
 * @see ExponentialDecay to understand when to pass your own decayAnimation.
 *
 * @param animationAnchors set of anchors to animate to
 * @param animationBuilder animation which will be used for animations
 * @param decayAnimation decay animation to be used to calculate closest point in the anchors set
 * considering velocity.
 */
data class AnchorsAnimationAdjustment(
    val animationAnchors: List<Float>,
    val animationBuilder: AnimationBuilder<Float> = PhysicsBuilder(),
    override val decayAnimation: DecayAnimation = ExponentialDecay()
) : AnimationAdjustment {
    override fun adjust(target: Float): TargetAnimation? {
        val point = animationAnchors.minBy { abs(it - target) }
        val adjusted = point ?: target
        return TargetAnimation(adjusted, animationBuilder)
    }
}

/**
 * Decay animation adjustment is the adjustment that control behaviour of fling,
 * e.g its friction or velocity threshold, but doesn't specify where this fling will end.
 *
 * The most common Decay animation is [ExponentialDecay].
 *
 * @param decayAnimation the animation to control fling behaviour
 */
data class DecayAdjustment(
    override val decayAnimation: DecayAnimation
) : AnimationAdjustment {
    override fun adjust(target: Float): TargetAnimation? {
        return null
    }
}

/**
 * Default animation adjustment is the [DecayAdjustment] with [ExponentialDecay],
 * which means it uses default decay logic to provide natural fling.
 */
object DefaultAnimationAdjustment : AnimationAdjustment {
    override fun adjust(target: Float): TargetAnimation? {
        return null
    }

    override val decayAnimation: DecayAnimation = ExponentialDecay()
}