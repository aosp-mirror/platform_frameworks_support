/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.build.releasenotes

import java.util.Locale

enum class HeaderType {
    H1, H2, H3, H4, H5, H6;
    companion object {
        fun getHeaderTag(tag: HeaderType): String {
            when (tag) {
                H1 -> return("#")
                H2 -> return("##")
                H3 -> return("###")
                H4 -> return("####")
                H5 -> return("#####")
                H6 -> return("######")
            }
        }
    }
}

// All Markdown classes should extend this so MarkdownList can contains all Markdown types
abstract class Markdown

open class MarkdownHeader : Markdown() {
    var markdownType: HeaderType
    var text: String

    init {
        markdownType = HeaderType.H1
        text = ""
    }
    @Override
    override fun toString(): String {
        return HeaderType.getHeaderTag(markdownType) + ' ' + text
    }
    fun print() {
        println(toString())
    }
}

class LibraryHeader : MarkdownHeader() {
    var groupId: String = ""
    var version: String = ""
    init {
        markdownType = HeaderType.H2
        text = groupId.toUpperCase(Locale.US) + " Version " + version + " {:#" + version + "}"
    }
}

// General Markdown-Formatted List
class MarkdownList {
    var items: MutableList<Markdown> = mutableListOf()

    fun getListItemStr(): String { return "- " }

    @Override
    override fun toString(): String {
        var markdownString: String = ""
        items.forEach { markdownItem ->
            markdownString = markdownString + getListItemStr() + markdownItem
            if (markdownString.last() != '\n') markdownString += '\n'
        }
        return markdownString
    }
    fun print() {
        println(toString())
    }
}

// General Link class
open class MarkdownLink : Markdown() {
    var linkText: String = ""
    var linkUrl: String = ""

    @Override
    override fun toString(): String {
        return "([" + linkText + "](" + linkUrl + "))"
    }

    fun print() {
        println(toString())
    }
}

// Link to AOSP Gerrit
class AOSPLink(clNumber: String) : MarkdownLink() {
    init {
        linkText = "aosp/" + clNumber.take(6)
        linkUrl = getBaseAOSPUrl() + clNumber
    }
    fun getBaseAOSPUrl(): String {
        return "https://android-review.googlesource.com/#/q/"
    }
}

// Link to Public Buganizer
class BuganizerLink(bugId: Int) : MarkdownLink() {
    init {
        linkText = "b/$bugId"
        linkUrl = "${getBaseBuganizerUrl()}$bugId"
    }
    fun getBaseBuganizerUrl(): String {
        return "https://issuetracker.google.com/issues/"
    }
}