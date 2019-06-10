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

package androidx.ui.painting

import java.lang.StringBuilder

data class Span(
    val style: TextStyle,
    val start: Int,
    val end: Int
)

data class AnnotatedString(
    val text: String = "",
    val styles: List<Span> = listOf()
)

private data class SpanInternal(
    val start: Int,
    var end: Int,
    val style: TextStyle
)

private fun TextSpan.toAnnotatedStringVisitor(
    sb: StringBuilder,
    st: MutableList<SpanInternal>
) {
    val styleSpan = style?.let {
        val span = SpanInternal(sb.length, -1, it)
        st.add(span)
        span
    }
    text?.let { sb.append(text) }
    for (child in children) {
        child.toAnnotatedStringVisitor(sb, st)
    }
    if (styleSpan != null) {
        styleSpan.end = sb.length
    }
}

fun TextSpan.toAnnotatedString(): AnnotatedString {
    val stringBuilder = StringBuilder()
    val styleSpans = mutableListOf<SpanInternal>()
    toAnnotatedStringVisitor(stringBuilder, styleSpans)
    val lst = styleSpans.map { Span(it.style, it.start, it.end) }
    return AnnotatedString(stringBuilder.toString(), lst)
}