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

package androidx.ui.core.gesture

import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange
import androidx.ui.core.anyPositionChangeConsumed
import androidx.ui.core.changedToDown
import androidx.ui.core.consumeDownChange
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.core.PointerInputWrapper
import androidx.ui.core.changedToUp
import androidx.ui.temputils.DelayUtil

@Composable
fun LongPressGestureDetector(
    onLongPress: (() -> Unit)? = null,
    @Children children: @Composable() () -> Unit
) {
    val recognizer = +memo { LongPressGestureDetector() }
    recognizer.onLongPress = onLongPress
    PointerInputWrapper(pointerInputHandler = recognizer.pointerInputHandler) {
        children()
    }
}

internal class LongPressGestureDetector {

    var onLongPress: (() -> Unit)? = null

    private var started = false
    private val delayUtil = DelayUtil()

    val pointerInputHandler =
        { changes: List<PointerInputChange>, pass: PointerEventPass ->

            if (pass == PointerEventPass.PostUp) {

                if (!started && changes.all { it.changedToDown() }) {
                    // If we have not yet started and all of the changes changed to down, we are
                    // starting.
                    started = true
                    delayUtil.delay(LongPressTimeout) {
                        onLongPress?.invoke()
                    }

                } else if (started && changes.all { it.changedToUp() }) {
                    // If we have started and all of the changes changed to up, we are stopping.
                    started = false
                    delayUtil.cancel()
                }
            }

            if (pass == PointerEventPass.PostDown &&
                started &&
                changes
                    .any { it.anyPositionChangeConsumed() }
            ) {
                // On the final pass, if we have started and any of the changes had consumed
                // position changes, we cancel.
                started = false
                delayUtil.cancel()
            }

            changes
        }
}