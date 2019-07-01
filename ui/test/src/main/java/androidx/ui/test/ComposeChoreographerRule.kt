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

import androidx.compose.AndroidChoreographer
import androidx.compose.ComposeChoreographer
import androidx.compose.defaultChoreographer
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class ComposeChoreographerRule : TestRule {

    private val fakeChoreographer = FakeChoreographer()

    override fun apply(base: Statement, description: Description?): Statement {
        return ComposeChoreographerRuleStatement(base)
    }

    inner class ComposeChoreographerRuleStatement(
        private val base: Statement
    ) : Statement() {
        override fun evaluate() {
            defaultChoreographer = fakeChoreographer
            try {
                base.evaluate()
            } finally {
                defaultChoreographer = AndroidChoreographer
            }
        }
    }

    fun newFrame() {
        fakeChoreographer.newFrame()
    }

    class FakeChoreographer : ComposeChoreographer {

        private val callbacks = mutableListOf<() -> Unit>()

        override fun postFrameCallback(callback: () -> Unit) {
            callbacks.add(callback)
        }

        fun newFrame() {
            val callbacksCopy = callbacks.toTypedArray()
            callbacks.clear()
            callbacksCopy.forEach { it.invoke() }
        }
    }

}