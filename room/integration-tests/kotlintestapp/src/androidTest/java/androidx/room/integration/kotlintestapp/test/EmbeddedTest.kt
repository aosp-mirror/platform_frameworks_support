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

package androidx.room.integration.kotlintestapp.test

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmbeddedTest {

    data class Contact(
        val code: String,
        val phone: String
    )

    open class CustomerBase(
        @PrimaryKey
        val id: Long,
        val name: String,
        @Embedded
        val contact: Contact?
    )

    @Entity
    class Customer(id: Long, name: String, contact: Contact?) : CustomerBase(id, name, contact)

    @Dao
    interface CustomerDao {
        @Insert
        fun insert(customer: Customer)

        @Query("SELECT * FROM Customer WHERE id = :id")
        fun loadById(id: Long): Customer
    }

    @Database(version = 1, exportSchema = false, entities = [Customer::class])
    abstract class CustomerDatabase : RoomDatabase() {
        abstract fun customer(): CustomerDao
    }

    @Test
    fun embeddedFieldInParent() {
        val db = openDatabase()
        val dao = db.customer()
        dao.insert(Customer(1, "a", Contact("01", "1234")))
        val customer = dao.loadById(1)
        assertThat(customer.name, `is`(equalTo("a")))
        customer.contact!!.let { contact ->
            assertThat(contact.code, `is`(equalTo("01")))
            assertThat(contact.phone, `is`(equalTo("1234")))
        }
    }

    private fun openDatabase(): CustomerDatabase {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return Room.inMemoryDatabaseBuilder(context, CustomerDatabase::class.java).build()
    }
}
