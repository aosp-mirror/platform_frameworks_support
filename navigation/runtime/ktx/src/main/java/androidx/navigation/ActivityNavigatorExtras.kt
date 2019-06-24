<<<<<<< HEAD   (a5e8e6 Merge "Merge empty history for sparse-5675002-L2860000033185)
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

import android.support.v4.app.ActivityOptionsCompat

/**
 * Create a new [ActivityNavigator.Extras] instance with a specific [ActivityOptionsCompat]
 * instance and/or any `Intent.FLAG_ACTIVITY_` flags.
 *
 * @param activityOptions Optional [ActivityOptionsCompat] to pass through to
 * [android.support.v4.app.ActivityCompat.startActivity].
 * @param flags `Intent.FLAG_ACTIVITY_` flags to add to the Intent.
 */
@Suppress("FunctionName")
fun ActivityNavigatorExtras(activityOptions: ActivityOptionsCompat? = null, flags: Int = 0) =
        ActivityNavigator.Extras.Builder().apply {
            if (activityOptions != null) {
                setActivityOptions(activityOptions)
            }
            addFlags(flags)
        }.build()
=======
>>>>>>> BRANCH (5b4a18 Merge "Merge cherrypicks of [987799] into sparse-5647264-L96)
