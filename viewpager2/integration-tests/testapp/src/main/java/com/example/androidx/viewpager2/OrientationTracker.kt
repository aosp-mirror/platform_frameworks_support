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

package com.example.androidx.viewpager2

import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.viewpager2.widget.ViewPager2

class OrientationTracker(private val viewPager: ViewPager2, spinner: Spinner) {
    internal var orientation: Int = viewPager.orientation

    init {
        val adapter = ArrayAdapter(spinner.context, android.R.layout.simple_spinner_item,
            arrayOf(HORIZONTAL, VERTICAL))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val initialPosition = adapter.getPosition(when (orientation) {
            ViewPager2.ORIENTATION_HORIZONTAL -> HORIZONTAL
            ViewPager2.ORIENTATION_VERTICAL -> VERTICAL
            else -> null
        })
        spinner.adapter = adapter
        if (initialPosition >= 0) {
            spinner.setSelection(initialPosition)
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (parent.selectedItem.toString()) {
                    HORIZONTAL -> orientation = ViewPager2.ORIENTATION_HORIZONTAL
                    VERTICAL -> orientation = ViewPager2.ORIENTATION_VERTICAL
                }
                viewPager.orientation = orientation
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {}
        }
    }

    companion object {
        private const val HORIZONTAL = "horizontal"
        private const val VERTICAL = "vertical"
    }
}