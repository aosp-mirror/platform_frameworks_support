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

package androidx.compose

import android.os.Trace
import java.lang.StringBuilder
import java.util.Stack

/**
 * Wrap the specified [block] in calls to [Trace.beginSection] (with the supplied [sectionName])
 * and [Trace.endSection].
 */
inline fun <T> trace(sectionName: String, block: () -> T): T {
    ComposeTrace.beginSection(sectionName)
    try {
        return block()
    } finally {
        ComposeTrace.endSection(sectionName)
    }
}


class ComposeTraceData {


    var sectionsData = mutableListOf<SectionData>()


    fun recordSection(section: SectionData) {
        sectionsData.add(section)
    }

//    fun dumpData(): List<String> {
//        val strings = mutableListOf<String>();
//
//        sectionsData.forEach { instances ->
//            instances.forEach {
//                strings.add("${it.sectionName}: ${it.durationInMs}ms")
//            }
//        }
//
//        return strings
//    }

    fun groupSections(): Set<GroupedSection> {
        val flatList = mutableListOf<SectionData>()
        sectionsData.forEach {
            toFlatList(it, flatList)

        }

        return flatList.groupBy { it.sectionName }.map {  instances ->
            var totalDuration: Long = 0
            var totalOverhead: Long = 0
            instances.value.forEach {
                totalDuration += it.duration
                totalOverhead += it.getOverhead()

            }
            GroupedSection(instances.key, totalDuration, totalOverhead, instances.value.size)
        }.toSet()
    }

    fun toFlatList(data: SectionData, output: MutableList<SectionData>) {
        output.add(data)
        data.children.forEach { toFlatList(it, output) }
    }

    fun dumpGroupedData(): String {
        val sb = StringBuilder()
        val groupedData = groupSections();
        groupedData.forEach {
            sb.appendln("${it.sectionName}: ${"%.2f".format(it.overheadInMs)}ms o (${it.count}x) | total duration: ${"%.2f".format(it.durationInMs)}ms")
        }

        return sb.toString()
    }

}

data class GroupedSection(val sectionName: String, val duration: Long, val overhead: Long, val count: Int) {
    val durationInMs get() = duration / 1000000f
    val overheadInMs get() = overhead / 1000000f
}

data class SectionData(val sectionName: String, val start: Long, val end: Long, val children: List<SectionData>) {
    val duration = end - start
    val durationInMs get() = (end - start) / 1000000f

    fun getOverhead(): Long {
        var childrenDuration: Long = 0
        children.forEach {
            childrenDuration += it.duration
        }
        return duration - childrenDuration;
    }
}

data class SectionStart(val sectionName: String, val start: Long) {
    val children: MutableList<SectionData> = mutableListOf()
}

object ComposeTrace {

    private val sectionsStack = Stack<SectionStart>()

    var data: ComposeTraceData = ComposeTraceData()


    fun resetData() {
        if (sectionsStack.isNotEmpty()) {
            throw RuntimeException("Resetting data in unstable state (stack still contains traces)!")
        }

        data = ComposeTraceData()
    }


    fun beginSection(sectionName: String) {
        //sectionsStack.push(SectionStart(sectionName, System.nanoTime()))
        Trace.beginSection(sectionName)
    }

    fun endSection(sectionName: String) {
        Trace.endSection()
//        if (sectionsStack.empty()) {
//            // TODO: Corrupted stack
//            throw RuntimeException("Your traces are broken!")
//            return
//        }
//        val start = sectionsStack.pop()
//        if (start.sectionName != sectionName) {
//            throw RuntimeException("Your traces are broken!")
//        }
//
//        val end = System.nanoTime()
//        val sectionData = SectionData(start.sectionName, start.start, end, start.children);
//
//        if (sectionsStack.isNotEmpty()) {
//            val parentSection = sectionsStack.peek()
//            parentSection.children.add(sectionData)
//        } else {
//            data.recordSection(sectionData)
//        }

    }


}