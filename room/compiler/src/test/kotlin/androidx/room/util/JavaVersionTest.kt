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

package androidx.room.util

import org.junit.Test

class JavaVersionTest {

    @Test
    fun testTryParse() {
        assert(JavaVersion.tryParse("11.0.1+13-LTS")!! == JavaVersion(11, 0, null))
        assert(JavaVersion.tryParse("1.8.0_202-release-1483-b39-5396753") == JavaVersion(8, 0, 202))
        assert(
            JavaVersion.tryParse("1.8.0_181-google-v7-238857965-238857965")
                    == JavaVersion(8, 0, 181)
        )
        assert(JavaVersion.tryParse("a.b.c") == null)
    }

    @Test
    fun testComparison() {
        assert(JavaVersion(8, 0, 203) > JavaVersion(8, 0, 202))
        assert(JavaVersion(8, 0, 202) == JavaVersion(8, 0, 202))
        assert(JavaVersion(8, 0, 152) < JavaVersion(8, 0, 202))
    }
}