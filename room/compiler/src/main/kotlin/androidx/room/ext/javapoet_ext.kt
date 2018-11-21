/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room.ext

import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import javax.annotation.processing.Filer
import javax.lang.model.type.TypeMirror
import javax.tools.StandardLocation
import kotlin.reflect.KClass

val S = "\$S"

fun KClass<*>.typeName() = this.asTypeName()
fun KClass<*>.arrayTypeName() = ARRAY.parameterizedBy(this.typeName())
fun TypeMirror.arrayTypeName() = ARRAY.parameterizedBy(this.typeName())
fun TypeName.arrayTypeName() = ARRAY.parameterizedBy(this)
fun TypeMirror.typeName() =  asTypeName()

object SupportDbTypeNames {
    val DB: ClassName = ClassName("androidx.sqlite.db", "SupportSQLiteDatabase")
    val SQLITE_STMT: ClassName =
            ClassName("androidx.sqlite.db", "SupportSQLiteStatement")
    val SQLITE_OPEN_HELPER: ClassName =
            ClassName("androidx.sqlite.db", "SupportSQLiteOpenHelper")
    val SQLITE_OPEN_HELPER_CALLBACK: ClassName =
            ClassName("androidx.sqlite.db", "SupportSQLiteOpenHelper.Callback")
    val SQLITE_OPEN_HELPER_CONFIG: ClassName =
            ClassName("androidx.sqlite.db", "SupportSQLiteOpenHelper.Configuration")
    val QUERY: ClassName =
            ClassName("androidx.sqlite.db", "SupportSQLiteQuery")
}

object RoomTypeNames {
    val STRING_UTIL: ClassName = ClassName("androidx.room.util", "StringUtil")
    val ROOM_DB: ClassName = ClassName("androidx.room", "RoomDatabase")
    val ROOM_DB_CONFIG: ClassName = ClassName("androidx.room",
            "DatabaseConfiguration")
    val INSERTION_ADAPTER: ClassName =
            ClassName("androidx.room", "EntityInsertionAdapter")
    val DELETE_OR_UPDATE_ADAPTER: ClassName =
            ClassName("androidx.room", "EntityDeletionOrUpdateAdapter")
    val SHARED_SQLITE_STMT: ClassName =
            ClassName("androidx.room", "SharedSQLiteStatement")
    val INVALIDATION_TRACKER: ClassName =
            ClassName("androidx.room", "InvalidationTracker")
    val INVALIDATION_OBSERVER: ClassName =
            ClassName("androidx.room.InvalidationTracker", "Observer")
    val ROOM_SQL_QUERY: ClassName =
            ClassName("androidx.room", "RoomSQLiteQuery")
    val OPEN_HELPER: ClassName =
            ClassName("androidx.room", "RoomOpenHelper")
    val OPEN_HELPER_DELEGATE: ClassName =
            ClassName("androidx.room", "RoomOpenHelper.Delegate")
    val TABLE_INFO: ClassName =
            ClassName("androidx.room.util", "TableInfo")
    val TABLE_INFO_COLUMN: ClassName =
            ClassName("androidx.room.util", "TableInfo.Column")
    val TABLE_INFO_FOREIGN_KEY: ClassName =
            ClassName("androidx.room.util", "TableInfo.ForeignKey")
    val TABLE_INFO_INDEX: ClassName =
            ClassName("androidx.room.util", "TableInfo.Index")
    val FTS_TABLE_INFO: ClassName =
            ClassName("androidx.room.util", "FtsTableInfo")
    val VIEW_INFO: ClassName =
            ClassName("androidx.room.util", "ViewInfo")
    val LIMIT_OFFSET_DATA_SOURCE: ClassName =
            ClassName("androidx.room.paging", "LimitOffsetDataSource")
    val DB_UTIL: ClassName =
            ClassName("androidx.room.util", "DBUtil")
    val CURSOR_UTIL: ClassName =
            ClassName("androidx.room.util", "CursorUtil")
}

object PagingTypeNames {
    val DATA_SOURCE: ClassName =
            ClassName("androidx.paging", "DataSource")
    val POSITIONAL_DATA_SOURCE: ClassName =
            ClassName("androidx.paging", "PositionalDataSource")
    val DATA_SOURCE_FACTORY: ClassName =
            ClassName("androidx.paging", "DataSource.Factory")
}

object LifecyclesTypeNames {
    val LIVE_DATA: ClassName = ClassName("androidx.lifecycle", "LiveData")
    val COMPUTABLE_LIVE_DATA: ClassName = ClassName("androidx.lifecycle",
            "ComputableLiveData")
}

object AndroidTypeNames {
    val CURSOR: ClassName = ClassName("android.database", "Cursor")
    val ARRAY_MAP: ClassName = ClassName("androidx.collection", "ArrayMap")
    val LONG_SPARSE_ARRAY: ClassName = ClassName("androidx.collection", "LongSparseArray")
    val BUILD: ClassName = ClassName("android.os", "Build")
}

object CommonTypeNames {
    val LIST = ClassName("java.util", "List")
    val SET = ClassName("java.util", "Set")
    val STRING = ClassName("kotlin.lang", "String")
    val INTEGER = ClassName("java.lang", "Integer")
    val OPTIONAL = ClassName("java.util", "Optional")
}

object GuavaBaseTypeNames {
    val OPTIONAL = ClassName("com.google.common.base", "Optional")
}

object GuavaUtilConcurrentTypeNames {
    val LISTENABLE_FUTURE = ClassName("com.google.common.util.concurrent", "ListenableFuture")
}

object RxJava2TypeNames {
    val FLOWABLE = ClassName("io.reactivex", "Flowable")
    val OBSERVABLE = ClassName("io.reactivex", "Observable")
    val MAYBE = ClassName("io.reactivex", "Maybe")
    val SINGLE = ClassName("io.reactivex", "Single")
    val COMPLETABLE = ClassName("io.reactivex", "Completable")
}

object ReactiveStreamsTypeNames {
    val PUBLISHER = ClassName("org.reactivestreams", "Publisher")
}

object RoomGuavaTypeNames {
    val GUAVA_ROOM = ClassName("androidx.room.guava", "GuavaRoom")
}

object RoomRxJava2TypeNames {
    val RX_ROOM = ClassName("androidx.room", "RxRoom")
    val RX_ROOM_CREATE_FLOWABLE = "createFlowable"
    val RX_ROOM_CREATE_OBSERVABLE = "createObservable"
    val RX_EMPTY_RESULT_SET_EXCEPTION = ClassName("androidx.room",
            "EmptyResultSetException")
}

fun TypeName.defaultValue(): String {
    return if (nullable) {
        "null"
    } else if (this == BOOLEAN) {
        "false"
    } else {
        "0"
    }
}

fun FileSpec.writeTo(filer: Filer) {
    val filerSourceFile = filer.createResource(StandardLocation.SOURCE_OUTPUT,
        this.packageName, "$name.kt")
    try {
        println("Writing $name.kt")
        filerSourceFile.openWriter().use { writer -> writeTo(writer) }
    } catch (e: Exception) {
        try {
            filerSourceFile.delete()
        } catch (ignored: Exception) {
        }
        throw RuntimeException("$name.kt FUCKED", e)
    }
}