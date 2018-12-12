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

package androidx.room.integration.kotlintestapp.test

import androidx.room.integration.kotlintestapp.vo.Publisher
import androidx.test.filters.SmallTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.junit.Test

@SmallTest
class JavaDaoTest : TestDatabaseTest() {

    @Test
    fun insertAndQuery() {
        val dao = database.javaDao()
        dao.insertPublisher(Publisher("a", "A"))
        val publisher = dao.loadPublisherById("a")
        assertThat(publisher.name, `is`(equalTo("A")))
    }
}
