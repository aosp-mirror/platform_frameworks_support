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

fun SingleNodeQuery.doClick(): SingleNodeQuery {
    mBaseNodeQuery.doClickInternal(::sanityCheck)

    return this
}

fun MultipleNodesQuery.doClickAll(): MultipleNodesQuery {
    mBaseNodeQuery.doClickInternal(::sanityCheck)

    return this
}

internal fun BaseNodeQuery.doClickInternal(sanityCheck: (List<SemanticsTreeNode>) -> Unit) {
    val foundNodes = findAllMatching()
    sanityCheck(foundNodes)

    foundNodes.forEach {
        // TODO(catalintudor): get real coordonates after Semantics API is ready (b/125702443)
        val globalCoordinates = it.globalPosition
            ?: throw AssertionError("Semantic Node has no child layout to perform click on!")
        val x = globalCoordinates.x.value + 1f
        val y = globalCoordinates.y.value + 1f

        sendClick(x, y)
    }
}