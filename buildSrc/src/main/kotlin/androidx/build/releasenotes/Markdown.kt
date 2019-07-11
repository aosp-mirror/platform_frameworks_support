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

import java.text.SimpleDateFormat
import java.util.Date
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

/*
 * All Markdown classes should extend this so MarkdownList can contains all Markdown types
 */
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

class LibraryHeader(version: String, groupId:String) : MarkdownHeader() {
    init {
        markdownType = HeaderType.H2
        text = groupId.toUpperCase(Locale.US) + " Version " + version + " {:#" + version + "}"
    }
}

/* General Markdown-Formatted List
 */
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

/* General Link class
 */
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

/*

## Version 1.1.0-beta01 {:#1.1.0-beta01}
June 5, 2019

`androidx.coordinatorlayout:coordinatorlayout:1.1.0-beta01` are released.  The commits included in this version can be found [here](https://android.googlesource.com/platform/frameworks/support/+log/225ba21ffaa37f0f1fc0da373afd960907e8092f..c8501f258905089258e1d19e04fd610bb923c364/coordinatorlayout).

**Bug fixes**

- Migrate away from deprecated test classes ([aosp/853955](https://android-review.googlesource.com/c/platform/frameworks/support/+/853955))

 */

/*
    Release notes structured class
 */
class LibraryReleaseNotes {
    var header: LibraryHeader
    var date: Date
    var version: String = ""
    var groupId: String = ""
    var artifactIds: MutableList<String> = mutableListOf()
    var diffLogLink: MarkdownLink
    var commitList: MarkdownList
    var summary: String = ""

    fun makeHeader() {
        if (version == "" || groupId == "") {
            throw RuntimeException("Tried to create Library Release Notes Header without setting" +
                    "the groupId or version!")
        }
        header = LibraryHeader(version, groupId)
    }

    fun getFormattedDate(): String {
        val pattern = "dd MMMMM, yyyy"
        val simpleDateFormat = SimpleDateFormat(pattern)
        return simpleDateFormat.format(date)
    }

    fun formatBlub() {
        val numberArtifacts = artifactIds.size()
        for (i in 0..numberArtifacts-1) {
            var currentArtifactId = artifactIds[i]
            when (numberArtifacts) {
                1 -> {
                    summary = "`$groupId:$currentArtifactId:$version` is released.  "
                }
                2 -> {
                    if (i == 0) {
                        summary = "`$groupId:$currentArtifactId:$version` and "
                    }
                    if (i == 1) {
                        summary += "`$groupId:$currentArtifactId:$version` are released. "
                    }
                }
                else {
                    if (i < numberArtifacts-1) {
                        summary += "`$groupId:$currentArtifactId:$version`, "
                    }
                    else {
                        summary += "and `$groupId:$currentArtifactId:$version` are released. "
                    }
                }
            }
        }

        summary += "The commits included in this version can be found $diffLogLink."
    }

    override fun toString(): String {
        var finalString: String = ""
        finalString = header.toString() + '\n'
        finalString += getFormattedDate()
        finalString += "\n\n"
        formatBlub()
        finalString += summary
        finalString += '\n'
        finalString += "$commitList"


        println(finalString)

    }
}