/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.viewpager2.widget.swipe

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.viewpager2.test.R

class PageManipulator {
    fun create(container: ViewGroup): View =
            LayoutInflater.from(container.context)
                    .inflate(R.layout.item_test_layout, container, false)

    fun findView(activity: Activity): TextView = activity.findViewById(R.id.text_view)

    fun setText(view: View, value: String) {
        (view as TextView).text = value
    }

    fun getText(view: View): String = (view as TextView).text.toString()
}
