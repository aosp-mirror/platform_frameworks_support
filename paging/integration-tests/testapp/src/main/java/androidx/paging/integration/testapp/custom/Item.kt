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

package androidx.paging.integration.testapp.custom

import androidx.recyclerview.widget.DiffUtil

/**
 * Sample item.
 */
internal class Item(val id: Int, val text: String, val bgColor: Int) {
    companion object {
        val DIFF_CALLBACK: DiffUtil.ItemCallback<Item> = object : DiffUtil.ItemCallback<Item>() {
            override fun areContentsTheSame(oldItem: Item, newItem: Item) = oldItem == newItem
            override fun areItemsTheSame(oldItem: Item, newItem: Item) = oldItem.id == newItem.id
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val item = other as Item?
        return (this.id == item!!.id &&
                this.bgColor == item.bgColor &&
                this.text == item.text)
    }

    override fun hashCode(): Int {
        var result = 0
        result = result * 17 + id
        result = result * 17 + text.hashCode()
        result = result * 17 + bgColor
        return result
    }
}
