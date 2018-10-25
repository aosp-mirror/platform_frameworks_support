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

package androidx.paging.integration.testapp.twolayer

import android.annotation.SuppressLint
import android.app.Application
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.concurrent.futures.ResolvableFuture
import androidx.lifecycle.AndroidViewModel
import androidx.paging.Config
import androidx.paging.DataSource
import androidx.paging.ItemKeyedDataSource
import androidx.paging.PageKeyedDataSource
import androidx.paging.RemoteDataProvider
import androidx.paging.toLiveData
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.lang.Thread.sleep
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@Entity
data class Item(
    @PrimaryKey val id: Int,
    val name: String)

@Dao
interface ItemDao {
    @Query("DELETE FROM item")
    fun removeAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addItems(items: List<Item>)

    @Query("SELECT * FROM item ORDER BY id ASC")
    fun loadPaged(): DataSource.Factory<Int, Item>

    @Query("SELECT COUNT (*) FROM item")
    fun count(): Int
}


// Separate network representation of items

@Entity
data class NetworkKey(val key: String)

data class NetworkItem(
    val id: Int, // different type just as proof of concept
    val name: String)

fun NetworkItem.toItem() = Item(id, name)

@Entity
data class StoredNetworkKey(
    @PrimaryKey val paginationId: Int,
    val key: String
)

@Dao
interface StoredNetworkKeyDao {
    // network 'next' key stored in DB
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(key: StoredNetworkKey)

    @Query("SELECT * FROM storedNetworkKey WHERE paginationId = :id")
    fun getKey(id: Int) : NetworkKey
}

@Database(entities = [Item::class, StoredNetworkKey::class], version = 1)
abstract class ItemDatabase : RoomDatabase() {
    abstract val itemDao: ItemDao
    abstract val networkKeyDao: StoredNetworkKeyDao
}


// Network DataSource
@SuppressLint("SyntheticAccessor")
class NetworkItemDataSource : PageKeyedDataSource<NetworkKey, NetworkItem>() {
    val networkExecutor = Executors.newSingleThreadExecutor()

    override fun loadInitial(
        params: LoadInitialParams<NetworkKey>,
        callback: LoadInitialCallback<NetworkKey, NetworkItem>
    ) {
        networkExecutor.run {
            sleep(200)
            val page: Page = PAGE_MAP[INIT_KEY]!!
            callback.onResult(page.data, page.prev, page.next)
        }
    }

    override fun loadBefore(params: LoadParams<NetworkKey>,
            callback: LoadCallback<NetworkKey, NetworkItem>) {
        networkExecutor.run {
            sleep(200)
            val page: Page = PAGE_MAP[params.key]!!
            callback.onResult(page.data, page.prev)
        }
    }

    override fun loadAfter(
        params: LoadParams<NetworkKey>,
        callback: LoadCallback<NetworkKey, NetworkItem>
    ) {
        networkExecutor.run {
            sleep(200)
            val page: Page = PAGE_MAP[params.key]!!
            callback.onResult(page.data, page.next)
        }
    }
}

private fun fetchPage(key: NetworkKey = INIT_KEY) : Page {
    return PAGE_MAP[key]!!
}

// Synthetic source data for mock network source
private data class Page(
    val prev: NetworkKey?,
    val data: List<NetworkItem>,
    val next: NetworkKey?)

class ItemStorageManager(val itemDatabase: ItemDatabase) :
    ItemKeyedDataSource.StorageManager<NetworkKey, NetworkItem>() {

    override fun replaceData(data: List<NetworkItem>) {
        itemDatabase.runInTransaction {
            itemDatabase.itemDao.removeAll()
            itemDatabase.itemDao.addItems(data.map { it.toItem() })
        }
    }

    override fun storeData(nextKey: NetworkKey, data: MutableList<NetworkItem>) {
        itemDatabase.runInTransaction {
            itemDatabase.itemDao.addItems(data.map { it.toItem() })
            itemDatabase.networkKeyDao.insert(StoredNetworkKey(0, nextKey.key))
        }
    }

    override fun restoreKey(): NetworkKey? {
        return itemDatabase.networkKeyDao.getKey(0)
    }
}

class ItemViewModel(
    application: Application
) : AndroidViewModel(application) {

    @SuppressLint("SyntheticAccessor")
    val database = Room.databaseBuilder(
        application, ItemDatabase::class.java, "mydb"
    ).build()

    private val remoteDataProvider = RemoteDataProvider(
        dataSource = NetworkItemDataSource(),
        storageManager = ItemStorageManager(database)
    )

    val liveData = database.itemDao.loadPaged().toLiveData(
        Config(pageSize = 10, enablePlaceholders = true),
        boundaryCallback = remoteDataProvider.getBoundaryCallback()
    )
}



@SuppressLint("SyntheticAccessor")
private val PAGE_MAP = HashMap<NetworkKey, Page>().apply {
    val pageCount = 10
    for (i in 1..pageCount) {
        val data = List(4) { NetworkItem(pageCount * 4 + it, "name $i $it") }

        val prev = if (i > 1) NetworkKey("key " + (i - 1)) else null
        val next = if (i < pageCount) NetworkKey("key " + (i + 1)) else null
        put(NetworkKey("key $i"), Page(prev, data, next))
    }
}
private val INIT_KEY: NetworkKey = NetworkKey("key 1")

@Suppress("FunctionName")
fun <NetKey, NetValue> RemoteDataProvider(
    storeExecutor: Executor = ArchTaskExecutor.getIOThreadExecutor(),
    notifyExecutor: Executor = ArchTaskExecutor.getIOThreadExecutor(),
    dataSource: DataSource<NetKey, NetValue>,
    storageManager: ItemKeyedDataSource.StorageManager<NetKey, NetValue>
) : RemoteDataProvider<NetKey, NetValue> {
    return RemoteDataProvider(storeExecutor, notifyExecutor, dataSource, storageManager)
}

