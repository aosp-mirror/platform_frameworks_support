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

package androidx.ui.androidview.adapters

import android.widget.NumberPicker

class NumberPickerInputController(
    view: NumberPicker
) : InputController<NumberPicker, Int>(view), NumberPicker.OnValueChangeListener {
    var onValueChange: Function1<Int, Unit>? = null

    override fun getValue(): Int = view.value

    override fun setValue(value: Int) {
        view.value = value
    }

    override fun onValueChange(view: NumberPicker?, oldVal: Int, newVal: Int) {
        prepareForChange(newVal)
        onValueChange?.invoke(newVal)
    }
}