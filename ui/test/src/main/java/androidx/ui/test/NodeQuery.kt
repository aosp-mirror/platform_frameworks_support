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

package androidx.ui.test

import android.view.MotionEvent
import androidx.ui.core.SemanticsTreeNode
import androidx.ui.core.semantics.SemanticsConfiguration

/**
 * The flow pattern for using [NodeQuery] would be:
 * - use findX extension methods such us [findByTag]
 * - optionally perform an action e.g. [doClick]
 * - assert properties e.g. [assertChecked]
 */

class NodeQuery internal constructor(
    private val uiTestRunner: UiTestRunner,
    private val expectation: Expectation,
    private val selector: SemanticsConfiguration.() -> Boolean
) {
// TODO(pavlis): Caching needs to be done only between operations that do not mutate the  tree.
// Disabling it for now.
//    private var cachedNodes: List<SemanticsTreeNode>? = null

    internal fun findAllMatching(): List<SemanticsTreeNode> {
//        if (cachedNodes == null) {
//            cachedNodes = uiTestRunner.findSemantics(selector)
//        }
//        return cachedNodes!!
        return uiTestRunner.findSemantics(selector)
    }

    internal fun sendEvent(event: MotionEvent) {
        uiTestRunner.sendEvent(event)
    }

    internal fun sendClick(x: Float, y: Float) {
        uiTestRunner.performClick(x, y)
    }

    internal fun findAndCheckExpectation(): List<SemanticsTreeNode> {
        val foundNodes = findAllMatching()

        if (!expectation.condition(foundNodes.size)) {
            throw AssertionError(expectation.errorMessage(foundNodes.size))
        }

        return foundNodes
    }
}