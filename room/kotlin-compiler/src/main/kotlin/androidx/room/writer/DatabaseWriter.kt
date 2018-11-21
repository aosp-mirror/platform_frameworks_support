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

package androidx.room.writer

import androidx.room.ext.AndroidTypeNames
import androidx.room.ext.CommonTypeNames
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.SupportDbTypeNames
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope
import androidx.room.vo.DaoMethod
import androidx.room.vo.Database
import com.google.auto.common.MoreElements
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.asClassName
import stripNonJava
import java.util.Locale

/**
 * Writes implementation of classes that were annotated with @Database.
 */
class DatabaseWriter(val database: Database) : ClassWriter(database.implTypeName) {
    override fun createTypeSpecBuilder(): TypeSpec.Builder {
        val builder = TypeSpec.classBuilder(database.implTypeName)
        builder.apply {
            superclass(database.typeName)
            addFunction(createCreateOpenHelper())
            addFunction(createCreateInvalidationTracker())
            addFunction(createClearAllTables())
        }
        addDaoImpls(builder)
        return builder
    }

    private fun createClearAllTables(): FunSpec {
        val scope = CodeGenScope(this)
        return FunSpec.builder("clearAllTables").apply {
            addStatement("super.assertNotMainThread()")
            val dbVar = scope.getTmpVar("_db")
            addStatement("val %L = super.getOpenHelper().getWritableDatabase()", dbVar)
            val deferVar = scope.getTmpVar("_supportsDeferForeignKeys")
            if (database.enableForeignKeys) {
                addStatement("val %L = %L.VERSION.SDK_INT >= %L.VERSION_CODES.LOLLIPOP",
                        deferVar, AndroidTypeNames.BUILD, AndroidTypeNames.BUILD)
            }
            addModifiers(KModifier.OVERRIDE)
            returns(UNIT)
            beginControlFlow("try").apply {
                if (database.enableForeignKeys) {
                    beginControlFlow("if (!%L)", deferVar).apply {
                        addStatement("%L.execSQL(%S)", dbVar, "PRAGMA foreign_keys = FALSE")
                    }
                    endControlFlow()
                }
                addStatement("super.beginTransaction()")
                if (database.enableForeignKeys) {
                    beginControlFlow("if (%L)", deferVar).apply {
                        addStatement("%L.execSQL(%S)", dbVar, "PRAGMA defer_foreign_keys = TRUE")
                    }
                    endControlFlow()
                }
                database.entities.sortedWith(EntityDeleteComparator()).forEach {
                    addStatement("%L.execSQL(%S)", dbVar, "DELETE FROM `${it.tableName}`")
                }
                addStatement("super.setTransactionSuccessful()")
            }
            nextControlFlow("finally").apply {
                addStatement("super.endTransaction()")
                if (database.enableForeignKeys) {
                    beginControlFlow("if (!%L)", deferVar).apply {
                        addStatement("%L.execSQL(%S)", dbVar, "PRAGMA foreign_keys = TRUE")
                    }
                    endControlFlow()
                }
                addStatement("%L.query(%S).close()", dbVar, "PRAGMA wal_checkpoint(FULL)")
                beginControlFlow("if (!%L.inTransaction())", dbVar).apply {
                    addStatement("%L.execSQL(%S)", dbVar, "VACUUM")
                }
                endControlFlow()
            }
            endControlFlow()
        }.build()
    }

    private fun createCreateInvalidationTracker(): FunSpec {
        val scope = CodeGenScope(this)
        return FunSpec.builder("createInvalidationTracker").apply {
            addModifiers(KModifier.OVERRIDE)
            addModifiers(KModifier.PROTECTED)
            returns(RoomTypeNames.INVALIDATION_TRACKER)
            val shadowTablesVar = "_shadowTablesMap"
            val shadowTablesTypeName = HashMap::class.asClassName().parameterizedBy(
                    CommonTypeNames.STRING, CommonTypeNames.STRING)
            val tableNames = database.entities.joinToString(",") {
                "\"${it.tableName}\""
            }
            val shadowTableNames = database.entities.mapNotNull {entity ->
                entity.shadowTableName?.let { entity.tableName to it}
            }
            addStatement("val %L = %T(%L)", shadowTablesVar,
                shadowTablesTypeName, shadowTableNames.size)
            shadowTableNames.forEach { (tableName, shadowTableName) ->
                addStatement("%L.put(%S, %S)", shadowTablesVar, tableName, shadowTableName)
            }
            val viewTablesVar = scope.getTmpVar("_viewTables")
            val tablesType = HashSet::class.typeName().parameterizedBy(CommonTypeNames.STRING)
            val viewTablesType = HashMap::class.typeName().parameterizedBy(
                    CommonTypeNames.STRING,
                    CommonTypeNames.SET.parameterizedBy(CommonTypeNames.STRING))
            addStatement("val %L = %T(%L)", viewTablesVar, viewTablesType,
                    database.views.size)
            for (view in database.views) {
                val tablesVar = scope.getTmpVar("_tables")
                addStatement("val %L = %T(%L)", tablesVar, tablesType,
                        view.tables.size)
                for (table in view.tables) {
                    addStatement("%L.add(%S)", tablesVar, table)
                }
                addStatement("%L.put(%S, %L)", viewTablesVar,
                        view.viewName.toLowerCase(Locale.US), tablesVar)
            }
            addStatement("return %T(this, %L, %L, %L)",
                    RoomTypeNames.INVALIDATION_TRACKER, shadowTablesVar, viewTablesVar, tableNames)
        }.build()
    }

    private fun addDaoImpls(builder: TypeSpec.Builder) {
        val scope = CodeGenScope(this)
        builder.apply {
            database.daoMethods.forEach { method ->
                val name = method.dao.typeName.simpleName.decapitalize().stripNonJava()
                val fieldName = scope.getTmpVar("_$name")
                val field = PropertySpec.builder(fieldName, method.dao.typeName.asNullable(),
                        KModifier.PRIVATE).mutable().addAnnotation(Volatile::class).initializer("null").build()
                addProperty(field)
                addFunction(createDaoGetter(field, method))
            }
        }
    }

    private fun createDaoGetter(field: PropertySpec, method: DaoMethod): FunSpec {
        return FunSpec.overriding(MoreElements.asExecutable(method.element)).apply {
            beginControlFlow("if (%N != null)", field).apply {
                addStatement("return %N", field)
            }
            nextControlFlow("else").apply {
                beginControlFlow("synchronized(this)").apply {
                    beginControlFlow("if(%N == null)", field).apply {
                        addStatement("%N = %T(this)", field, method.dao.implTypeName)
                    }
                    endControlFlow()
                    addStatement("return %N", field)
                }
                endControlFlow()
            }
            endControlFlow()
        }.build()
    }

    private fun createCreateOpenHelper(): FunSpec {
        val scope = CodeGenScope(this)
        return FunSpec.builder("createOpenHelper").apply {
            addModifiers(KModifier.PROTECTED)
            addModifiers(KModifier.OVERRIDE)
            returns(SupportDbTypeNames.SQLITE_OPEN_HELPER)

            val configParam = ParameterSpec.builder("configuration",
                RoomTypeNames.ROOM_DB_CONFIG).build()
            addParameter(configParam)

            val openHelperVar = scope.getTmpVar("_helper")
            val openHelperCode = scope.fork()
            SQLiteOpenHelperWriter(database)
                    .write(openHelperVar, configParam, openHelperCode)
            addCode(openHelperCode.builder().build())
            addStatement("return %L", openHelperVar)
        }.build()
    }
}
