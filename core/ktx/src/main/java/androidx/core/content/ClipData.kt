/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("NOTHING_TO_INLINE")

package androidx.core.content

import android.content.ClipData
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri

/**
 * Returns the ClipData.Item at [index].
 *
 * @throws IndexOutOfBoundsException if index is less than 0 or greater than or equal to the count.
 */
inline operator fun ClipData.get(index: Int): ClipData.Item = getItemAt(index)

/** Returns `true` if [item] is found in this ClipData. */
operator fun ClipData.contains(item: ClipData.Item): Boolean {
    for (index in 0 until itemCount) {
        if (getItemAt(index) == item) {
            return true
        }
    }
    return false
}

/** Performs the given action on each item in this ClipData. */
inline fun ClipData.forEach(action: (item: ClipData.Item) -> Unit) {
    for (index in 0 until itemCount) {
        action(getItemAt(index))
    }
}

/** Performs the given action on each item in this ClipData, providing its sequential index. */
inline fun ClipData.forEachIndexed(action: (index: Int, item: ClipData.Item) -> Unit) {
    for (index in 0 until itemCount) {
        action(index, getItemAt(index))
    }
}

/**
 * Returns an [Iterator] over the items in this ClipData.
 **/
operator fun ClipData.iterator() = object : Iterator<ClipData.Item> {
    private var index = 0
    override fun hasNext() = index < itemCount
    override fun next() = getItemAt(index++) ?: throw IndexOutOfBoundsException()
}

/** Returns a [Sequence] over the items in this ClipData. */
val ClipData.items: Sequence<ClipData.Item>
    get() = object : Sequence<ClipData.Item> {
        override fun iterator() = this@items.iterator()
    }

/**
 * Returns a [List] containing the results of applying the given transform function to each
 * item in this ClipData.
 */
inline fun <T> ClipData.map(transform: (ClipData.Item) -> T) = List(itemCount) {
    transform(getItemAt(it))
}
