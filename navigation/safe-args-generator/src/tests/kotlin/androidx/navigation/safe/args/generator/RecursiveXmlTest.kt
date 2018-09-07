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

package androidx.navigation.safe.args.generator

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RecursiveXmlTest {

    @Test
    fun detectRecursiveInclude() {
        val context = Context()
        val navFile = testData("recursive_xmls/nav.xml")
        val subNavFile = testData("recursive_xmls/sub_nav.xml")
        val expectedError = ErrorMessage(subNavFile.file.path, 27, 5,
                "Recursive referencing through <include> detected: " +
                        "nav.xml -> sub_nav.xml -> nav.xml")
        NavParser.parseNavigationFile(navFile, "a.b", "foo.app", context,
                listOf(navFile, subNavFile), LinkedHashSet())
        val messages = context.logger.allMessages()
        assertThat(messages.size, `is`(1))
        assertThat(messages.first(), `is`(expectedError))
    }
}