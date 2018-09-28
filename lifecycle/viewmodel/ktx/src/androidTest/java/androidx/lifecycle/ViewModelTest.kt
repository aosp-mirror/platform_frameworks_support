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

package androidx.lifecycle

import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class ViewModelTest {

    @Test fun testVmScope() {
        val vm = object : ViewModel() {}
        val job1 = vm.viewModelScope.launch { delay(1000) }
        val job2 = vm.viewModelScope.launch { delay(1000) }
        vm.clear()
        Truth.assertThat(job1.isCancelled).isTrue()
        Truth.assertThat(job2.isCancelled).isTrue()
    }
}