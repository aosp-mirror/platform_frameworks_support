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

package androidx.testutils

import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.runner.AndroidJUnitRunner
import org.junit.rules.ExternalResource
import org.junit.runner.Description
import org.junit.runners.model.Statement

@Suppress("unused")
open class HackyAndroidJunitRunner : AndroidJUnitRunner() {
    override fun waitForActivitiesToComplete() {
    }
}

/**
 * Implement this interface on your Activity to allow HackyActivityScenarioRule to
 * launch once-per-test-class.
 */
interface Hacktivity {
    fun setFinishEnabled(finishEnabled: Boolean)
}

/**
 * Copy of ActivityScenarioRule, but which works around AndroidX test infra trying to finish
 * activities in between each test.
 */
class HackyActivityScenarioRule<A> : ExternalResource where A : Activity, A : Hacktivity {
    private val scenarioSupplier: () -> ActivityScenario<A>
    private lateinit var _scenario: ActivityScenario<A>
    private var disableFinish: Boolean = false

    val scenario: ActivityScenario<A>
        get() = checkNotNull(_scenario)

    override fun apply(base: Statement?, description: Description): Statement {
        // Running as a ClassRule? Disable activity finish
        disableFinish = (description.methodName == null)
        return super.apply(base, description)
    }

    constructor(activityClass: Class<A>) {
        scenarioSupplier = { ActivityScenario.launch(checkNotNull(activityClass)) }
    }

    constructor(startActivityIntent: Intent) {
        scenarioSupplier = { ActivityScenario.launch(checkNotNull(startActivityIntent)) }
    }

    @Throws(Throwable::class)
    override fun before() {
        _scenario = scenarioSupplier.invoke()
        if (disableFinish) {
            // TODO: Correct approach inside test lib would be removing activity from cleanup list
            scenario.onActivity {
                it.setFinishEnabled(false)
            }
        }
    }

    override fun after() {
        if (disableFinish) {
            scenario.onActivity {
                it.setFinishEnabled(true)
            }
        }
        scenario.close()
    }
}