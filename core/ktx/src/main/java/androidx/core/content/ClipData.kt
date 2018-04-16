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
inline fun <reified T> ClipData.map(transform: (ClipData.Item) -> T) = MutableList(itemCount) {
    transform(getItemAt(it))
}

/**
 * Returns a new [ClipData] with given list of items and
 * the optional clip label and [ContentResolver] for [Uri] items.
 * NOTE: HtmlText ClipData not supported.
 *
 * @throws IllegalArgumentException When the list doesn't contain items of type supported
 *         by [ClipData].
 */
fun clipDataOf(
    items: List<Any>,
    label: String = "",
    resolver: ContentResolver? = null
): ClipData = if (items.isEmpty()) {
    throw IllegalArgumentException("Illegal argument, list cannot be empty.")
} else {
    when (items[0]) {
        is Uri ->
            if (resolver == null) {
                ClipData.newRawUri(label, items[0] as Uri)
            } else {
                ClipData.newUri(resolver, label, items[0] as Uri)
            }
        is CharSequence ->
            ClipData.newPlainText(label, items[0] as CharSequence)
        is Intent ->
            ClipData.newIntent(label, items[0] as Intent)
        else -> throw IllegalArgumentException(
                "Illegal type: ${items[0]::class.java.canonicalName}")
    }.apply {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                when (item) {
                    is Uri -> addItem(ClipData.Item(item))
                    is CharSequence -> addItem(ClipData.Item(item))
                    is Intent -> addItem(ClipData.Item(item))
                    else -> throw IllegalArgumentException(
                            "Illegal type: ${item::class.java.canonicalName}")
                }
            }
        }
    }
}
