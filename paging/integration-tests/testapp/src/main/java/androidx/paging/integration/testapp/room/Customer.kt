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

import androidx.recyclerview.widget.DiffUtil
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Sample entity
 */
@Entity
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var name: String? = null,
    var lastName: String? = null
) {
    companion object {
        val DIFF_CALLBACK: DiffUtil.ItemCallback<Customer> =
            object : DiffUtil.ItemCallback<Customer>() {
                override fun areContentsTheSame(oldItem: Customer, newItem: Customer) =
                    oldItem == newItem

                override fun areItemsTheSame(oldItem: Customer, newItem: Customer) =
                    oldItem.id == newItem.id
            }
    }

    override fun equals(other: Any?): Boolean {
        when {
            this === other -> return true
            other == null || javaClass != other.javaClass -> return false
        }

        val customer = other as Customer
        return when {
            id != customer.id -> false
            if (name != null) name != customer.name else customer.name != null -> false
            lastName != null -> lastName == customer.lastName
            else -> customer.lastName == null
        }
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (lastName?.hashCode() ?: 0)
        return result
    }

    override fun toString() = "Customer{mId=$id, mName='$name', mLastName='$lastName'}"
}
