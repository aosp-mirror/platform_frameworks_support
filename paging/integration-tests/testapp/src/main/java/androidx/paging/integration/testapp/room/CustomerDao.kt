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

package androidx.paging.integration.testapp.room

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * Simple Customer DAO for Room Customer list sample.
 */
@Dao
internal interface CustomerDao {

    /**
     * Insert a customer
     * @param customer Customer.
     */
    @Insert
    fun insert(customer: Customer)

    /**
     * Insert multiple customers.
     * @param customers Customers.
     */
    @Insert
    fun insertAll(customers: Array<Customer>)

    /**
     * Delete all customers
     */
    @Query("DELETE FROM customer")
    fun removeAll()

    /**
     * @return DataSource.Factory of customers, ordered by last name. Use
     * [androidx.paging.LivePagedListBuilder] to get a LiveData of PagedLists.
     */
    @Query("SELECT * FROM customer ORDER BY lastName ASC")
    fun loadPagedAgeOrder(): DataSource.Factory<Int, Customer>

    /**
     * @return number of customers
     */
    @Query("SELECT COUNT(*) FROM customer")
    fun countCustomers(): Int

    /**
     * @return All customers
     */
    @Query("SELECT * FROM customer")
    fun all(): LiveData<List<Customer>>

    // Keyed

    @Query("SELECT * from customer ORDER BY lastName DESC LIMIT :limit")
    fun customerNameInitial(limit: Int): List<Customer>

    @Query("SELECT * from customer WHERE lastName < :key ORDER BY lastName DESC LIMIT :limit")
    fun customerNameLoadAfter(key: String, limit: Int): List<Customer>

    @Query("SELECT COUNT(*) from customer WHERE lastName < :key ORDER BY lastName DESC")
    fun customerNameCountAfter(key: String): Int

    @Query("SELECT * from customer WHERE lastName > :key ORDER BY lastName ASC LIMIT :limit")
    fun customerNameLoadBefore(key: String, limit: Int): List<Customer>

    @Query("SELECT COUNT(*) from customer WHERE lastName > :key ORDER BY lastName ASC")
    fun customerNameCountBefore(key: String): Int
}
