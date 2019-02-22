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

package com.android.tools.build.jetifier.processor.transform

import com.android.tools.build.jetifier.core.config.Config
import java.io.File

class SourceJetifier {

    companion object {
        fun jetifySource(map: Map<String, String>, inputSource: String): String {
            var sourceCode = inputSource
            for (pair in map) {
                val fromType = pair.key
                val toType = pair.value
                var startIndex = sourceCode.indexOf(string = fromType,
                    startIndex = 0)
                while (startIndex != -1) {
                    // Replace only if the match is not followed by an alphanumeric character.
                    // This serves to avoid matches where we match to a subset of the type instead
                    // of the actual intended type (e.g com.foo.Class should not
                    // match for the start of com.foo.Class2)
                    if (startIndex + fromType.length == sourceCode.length ||
                        !sourceCode[startIndex + fromType.length].isLetterOrDigit()) {
                        sourceCode = sourceCode.replaceRange(startIndex,
                            startIndex + fromType.length, toType)
                    }
                    startIndex += toType.length
                    startIndex = sourceCode.indexOf(string = fromType, startIndex = startIndex)
                }
            }
            return sourceCode
        }
        fun jetifySourceFile(config: Config, inputFile: File, outputFile: File, reverse: Boolean) {
            val typesMap = if (reverse) config.typesMap.reverseMapOrDie() else config.typesMap
            val mappings = HashMap<String, String>()
            for (mapping in typesMap.getClassMappings()) {
                mappings.put(mapping.key.toDotNotation(), mapping.value.toDotNotation())
            }
            outputFile.writeText(jetifySource(mappings, inputFile.readText()))
        }
    }
}
