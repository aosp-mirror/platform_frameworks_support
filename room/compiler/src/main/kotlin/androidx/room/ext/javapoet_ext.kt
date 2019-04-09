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

import androidx.room.RoomProcessor
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import java.util.concurrent.Callable
import javax.lang.model.element.Modifier
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

val L = "\$L"
val T = "\$T"
val N = "\$N"
val S = "\$S"

fun KClass<*>.typeName() = ClassName.get(this.java)
fun KClass<*>.arrayTypeName() = ArrayTypeName.of(typeName())
fun TypeMirror.typeName() = TypeName.get(this)

/**
 * Map of dejetified packages names. Useful for letting Room know which packages names to use when
 * generating code in a dejetified environment. To use this map add a resource file named
 * 'dejetifier.config' containing one key-value pair per line separated by '=' where the key is the
 * androidx package name to dejetify and the value is the dejetified package name.
 *
 * Example of a typical config:
 * ```
 * # Room dejetifier packages for JavaPoet class names.
 * androidx.sqlite = android.arch.persistence
 * androidx.room = android.arch.persistence.room
 * androidx.paging = android.arch.paging
 * androidx.collection = com.android.support
 * ```
 */
private val PACKAGE_NAME_OVERRIDES: Map<String, String> by lazy {
    RoomProcessor::class.java.classLoader.getResourceAsStream("dejetifier.config")?.reader()?.use {
        try {
            it.readLines()
                .filterNot { it.startsWith('#') }
                .associate { it.split('=').let { split -> split[0].trim() to split[1].trim() } }
        } catch (ex: Exception) {
            throw RuntimeException("Malformed dejetifier.config file.", ex)
        }
    } ?: emptyMap()
}

internal val SQLITE_PACKAGE = PACKAGE_NAME_OVERRIDES.getOrDefault("androidx.sqlite")
internal val ROOM_PACKAGE = PACKAGE_NAME_OVERRIDES.getOrDefault("androidx.room")
internal val PAGING_PACKAGE = PACKAGE_NAME_OVERRIDES.getOrDefault("androidx.paging")
internal val COLLECTION_PACKAGE = PACKAGE_NAME_OVERRIDES.getOrDefault("androidx.collection")

object SupportDbTypeNames {
    val DB: ClassName = ClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteDatabase")
    val SQLITE_STMT: ClassName =
            ClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteStatement")
    val SQLITE_OPEN_HELPER: ClassName =
            ClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteOpenHelper")
    val SQLITE_OPEN_HELPER_CALLBACK: ClassName =
            ClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteOpenHelper.Callback")
    val SQLITE_OPEN_HELPER_CONFIG: ClassName =
            ClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteOpenHelper.Configuration")
    val QUERY: ClassName =
            ClassName.get("$SQLITE_PACKAGE.db", "SupportSQLiteQuery")
}

object RoomTypeNames {
    val STRING_UTIL: ClassName = ClassName.get("$ROOM_PACKAGE.util", "StringUtil")
    val ROOM_DB: ClassName = ClassName.get(ROOM_PACKAGE, "RoomDatabase")
    val ROOM_DB_KT: ClassName = ClassName.get(ROOM_PACKAGE, "RoomDatabaseKt")
    val ROOM_DB_CONFIG: ClassName = ClassName.get(ROOM_PACKAGE, "DatabaseConfiguration")
    val INSERTION_ADAPTER: ClassName =
            ClassName.get(ROOM_PACKAGE, "EntityInsertionAdapter")
    val DELETE_OR_UPDATE_ADAPTER: ClassName =
            ClassName.get(ROOM_PACKAGE, "EntityDeletionOrUpdateAdapter")
    val SHARED_SQLITE_STMT: ClassName =
            ClassName.get(ROOM_PACKAGE, "SharedSQLiteStatement")
    val INVALIDATION_TRACKER: ClassName =
            ClassName.get(ROOM_PACKAGE, "InvalidationTracker")
    val INVALIDATION_OBSERVER: ClassName =
            ClassName.get("$ROOM_PACKAGE.InvalidationTracker", "Observer")
    val ROOM_SQL_QUERY: ClassName =
            ClassName.get(ROOM_PACKAGE, "RoomSQLiteQuery")
    val OPEN_HELPER: ClassName =
            ClassName.get(ROOM_PACKAGE, "RoomOpenHelper")
    val OPEN_HELPER_DELEGATE: ClassName =
            ClassName.get(ROOM_PACKAGE, "RoomOpenHelper.Delegate")
    val TABLE_INFO: ClassName =
            ClassName.get("$ROOM_PACKAGE.util", "TableInfo")
    val TABLE_INFO_COLUMN: ClassName =
            ClassName.get("$ROOM_PACKAGE.util", "TableInfo.Column")
    val TABLE_INFO_FOREIGN_KEY: ClassName =
            ClassName.get("$ROOM_PACKAGE.util", "TableInfo.ForeignKey")
    val TABLE_INFO_INDEX: ClassName =
            ClassName.get("$ROOM_PACKAGE.util", "TableInfo.Index")
    val FTS_TABLE_INFO: ClassName =
            ClassName.get("$ROOM_PACKAGE.util", "FtsTableInfo")
    val VIEW_INFO: ClassName =
            ClassName.get("$ROOM_PACKAGE.util", "ViewInfo")
    val LIMIT_OFFSET_DATA_SOURCE: ClassName =
            ClassName.get("$ROOM_PACKAGE.paging", "LimitOffsetDataSource")
    val DB_UTIL: ClassName =
            ClassName.get("$ROOM_PACKAGE.util", "DBUtil")
    val CURSOR_UTIL: ClassName =
            ClassName.get("$ROOM_PACKAGE.util", "CursorUtil")
}

object PagingTypeNames {
    val DATA_SOURCE: ClassName =
            ClassName.get(PAGING_PACKAGE, "DataSource")
    val POSITIONAL_DATA_SOURCE: ClassName =
            ClassName.get(PAGING_PACKAGE, "PositionalDataSource")
    val DATA_SOURCE_FACTORY: ClassName =
            ClassName.get(PAGING_PACKAGE, "DataSource.Factory")
}

object LifecyclesTypeNames {
    val LIVE_DATA: ClassName = ClassName.get("androidx.lifecycle", "LiveData")
    val COMPUTABLE_LIVE_DATA: ClassName = ClassName.get("androidx.lifecycle",
            "ComputableLiveData")
}

object AndroidTypeNames {
    val CURSOR: ClassName = ClassName.get("android.database", "Cursor")
    val BUILD: ClassName = ClassName.get("android.os", "Build")
}

object CollectionTypeNames {
    val ARRAY_MAP: ClassName = ClassName.get(COLLECTION_PACKAGE, "ArrayMap")
    val LONG_SPARSE_ARRAY: ClassName = ClassName.get(COLLECTION_PACKAGE, "LongSparseArray")
}

object CommonTypeNames {
    val LIST = ClassName.get("java.util", "List")
    val SET = ClassName.get("java.util", "Set")
    val STRING = ClassName.get("java.lang", "String")
    val INTEGER = ClassName.get("java.lang", "Integer")
    val OPTIONAL = ClassName.get("java.util", "Optional")
}

object GuavaBaseTypeNames {
    val OPTIONAL = ClassName.get("com.google.common.base", "Optional")
}

object GuavaUtilConcurrentTypeNames {
    val LISTENABLE_FUTURE = ClassName.get("com.google.common.util.concurrent", "ListenableFuture")
}

object RxJava2TypeNames {
    val FLOWABLE = ClassName.get("io.reactivex", "Flowable")
    val OBSERVABLE = ClassName.get("io.reactivex", "Observable")
    val MAYBE = ClassName.get("io.reactivex", "Maybe")
    val SINGLE = ClassName.get("io.reactivex", "Single")
    val COMPLETABLE = ClassName.get("io.reactivex", "Completable")
}

object ReactiveStreamsTypeNames {
    val PUBLISHER = ClassName.get("org.reactivestreams", "Publisher")
}

object RoomGuavaTypeNames {
    val GUAVA_ROOM = ClassName.get("$ROOM_PACKAGE.guava", "GuavaRoom")
}

object RoomRxJava2TypeNames {
    val RX_ROOM = ClassName.get(ROOM_PACKAGE, "RxRoom")
    val RX_ROOM_CREATE_FLOWABLE = "createFlowable"
    val RX_ROOM_CREATE_OBSERVABLE = "createObservable"
    val RX_EMPTY_RESULT_SET_EXCEPTION = ClassName.get(ROOM_PACKAGE, "EmptyResultSetException")
}

object RoomCoroutinesTypeNames {
    val COROUTINES_ROOM = ClassName.get(ROOM_PACKAGE, "CoroutinesRoom")
}

object KotlinTypeNames {
    val UNIT = ClassName.get("kotlin", "Unit")
    val CONTINUATION = ClassName.get("kotlin.coroutines", "Continuation")
    val COROUTINE_SCOPE = ClassName.get("kotlinx.coroutines", "CoroutineScope")
}

fun TypeName.defaultValue(): String {
    return if (!isPrimitive) {
        "null"
    } else if (this == TypeName.BOOLEAN) {
        "false"
    } else {
        "0"
    }
}

fun CallableTypeSpecBuilder(
    parameterTypeName: TypeName,
    callBody: MethodSpec.Builder.() -> Unit
) = TypeSpec.anonymousClassBuilder("").apply {
    superclass(ParameterizedTypeName.get(Callable::class.typeName(), parameterTypeName))
    addMethod(MethodSpec.methodBuilder("call").apply {
        returns(parameterTypeName)
        addException(Exception::class.typeName())
        addModifiers(Modifier.PUBLIC)
        addAnnotation(Override::class.java)
        callBody()
    }.build())
}

fun Function2TypeSpecBuilder(
    parameter1: Pair<TypeName, String>,
    parameter2: Pair<TypeName, String>,
    returnTypeName: TypeName,
    callBody: MethodSpec.Builder.() -> Unit
) = TypeSpec.anonymousClassBuilder("").apply {
    val (param1TypeName, param1Name) = parameter1
    val (param2TypeName, param2Name) = parameter2
    superclass(
        ParameterizedTypeName.get(
            Function2::class.typeName(),
            param1TypeName,
            param2TypeName,
            returnTypeName
        )
    )
    addMethod(MethodSpec.methodBuilder("invoke").apply {
        addParameter(param1TypeName, param1Name)
        addParameter(param2TypeName, param2Name)
        returns(returnTypeName)
        addModifiers(Modifier.PUBLIC)
        addAnnotation(Override::class.java)
        callBody()
    }.build())
}

private fun Map<String, String>.getOrDefault(key: String) = getOrDefault(key, key)
