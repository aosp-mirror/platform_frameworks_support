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

package androidx.work.impl.utils

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.work.TestLifecycleOwner
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveDataMappingTests {

    @get:Rule val rule = InstantTaskExecutorRule()

    private lateinit var lifecycleOwner: LifecycleOwner

    @Before
    fun setUp() {
        lifecycleOwner = TestLifecycleOwner()
    }

    @Test
    @SmallTest
    fun testFlattenToMap_notifiesCorrectly() {
        val id = "1"
        val liveData1 = MutableLiveData<Any>()
        liveData1.value = 1

        val map = LiveDataUtils.flattenToMap(listOf(id), listOf(liveData1))
        // Assert initial values
        assertThat(map.value?.size, `is`(1))
        var lastKnownValue = 0
        map.observe(lifecycleOwner, Observer {
            assertThat(it, notNullValue())
            if (it != null) {
                val value = it[id] as Int
                lastKnownValue = value
            }
        })
        liveData1.value = 2
        assertThat(lastKnownValue, `is`(2))
    }
}
