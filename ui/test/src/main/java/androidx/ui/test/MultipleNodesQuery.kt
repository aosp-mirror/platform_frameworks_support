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

class MultipleNodesQuery internal constructor(
    uiTestRunner: UiTestRunner,
    selector: SemanticsConfiguration.() -> Boolean
) {

    internal var mBaseNodeQuery = BaseNodeQuery(uiTestRunner, selector)

    internal fun sanityCheck(nodes: List<SemanticsTreeNode>) {
        if (nodes.isEmpty()) {
            throw AssertionError("Found '${nodes.size}' nodes but at least 1 was expected!")
        }
    }

    fun asOne(): SingleNodeQuery {
        return SingleNodeQuery(
            mBaseNodeQuery.uiTestRunner,
            mBaseNodeQuery.selector
        )
    }
}
