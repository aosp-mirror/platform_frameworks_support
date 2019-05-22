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

import androidx.ui.core.SemanticsTreeNode
import androidx.ui.core.semantics.SemanticsConfiguration

/**
 * The flow pattern for using [MultipleNodesQuery] would be:
 * - use findX extension methods such us [findAll]
 * - optionally perform an action e.g. [doClickAll]
 * - assert properties e.g. [assertAreChecked]
 */

class MultipleNodesQuery internal constructor(
    uiTestRunner: UiTestRunner,
    selector: SemanticsConfiguration.() -> Boolean
) {
    internal var baseNodeQuery = BaseNodeQuery(uiTestRunner, selector)

    internal fun findAtLeastOne(): List<SemanticsTreeNode> {
        val foundNodes = baseNodeQuery.findAllMatching()

        if (foundNodes.isEmpty()) {
            throw AssertionError("Found '${foundNodes.size}' nodes but at least 1 was expected!")
        }

        return foundNodes
    }
}
