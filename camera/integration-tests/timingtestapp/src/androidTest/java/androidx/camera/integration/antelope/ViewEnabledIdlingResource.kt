/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.integration.antelope

import android.view.View
import androidx.test.espresso.IdlingResource.ResourceCallback
import androidx.test.espresso.IdlingResource

class ViewEnabledIdlingResource(private val view: View) : IdlingResource {
    private var resourceCallback: ResourceCallback? = null

    override fun getName(): String {
        return ViewEnabledIdlingResource::class.java.name + ":" + view.id
    }

    override fun isIdleNow(): Boolean {
        val idle = view.isEnabled
        if (idle) {
            resourceCallback!!.onTransitionToIdle()
        }
        return idle
    }

    override fun registerIdleTransitionCallback(resourceCallback: ResourceCallback) {
        this.resourceCallback = resourceCallback
    }
}