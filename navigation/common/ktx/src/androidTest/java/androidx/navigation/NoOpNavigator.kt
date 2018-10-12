/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.navigation

import android.os.Bundle

/**
 * A [Navigator] that does nothing.
 */
internal open class NoOpNavigator : Navigator<NavDestination>() {

    override fun createDestination(): NavDestination {
        return NavDestination(this)
    }

    override fun navigate(
        destination: NavDestination,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ) {
        throw IllegalStateException("navigate is not supported")
    }

    override fun popBackStack(): Boolean {
        throw IllegalStateException("popBackStack is not supported")
    }
}
