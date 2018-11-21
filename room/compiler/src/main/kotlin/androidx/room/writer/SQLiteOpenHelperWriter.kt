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

import androidx.annotation.VisibleForTesting
import androidx.room.ext.RoomTypeNames
import androidx.room.ext.SupportDbTypeNames
import androidx.room.solver.CodeGenScope
import androidx.room.vo.Database
import androidx.room.vo.DatabaseView
import androidx.room.vo.Entity
import androidx.room.vo.FtsEntity
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import java.util.ArrayDeque

/**
 * The threshold amount of statements in a validateMigration() method before creating additional
 * secondary validate methods.
 */
const val VALIDATE_CHUNK_SIZE = 1000

/**
 * Create an open helper using SupportSQLiteOpenHelperFactory
 */
class SQLiteOpenHelperWriter(val database: Database) {
    fun write(outVar: String, configuration: ParameterSpec, scope: CodeGenScope) {
        scope.builder().apply {
            val sqliteConfigVar = scope.getTmpVar("_sqliteConfig")
            val callbackVar = scope.getTmpVar("_openCallback")
            addStatement("val %L = %T(%N, %L, %S, %S)",
                    callbackVar, RoomTypeNames.OPEN_HELPER, configuration,
                    createOpenCallback(scope), database.identityHash, database.legacyIdentityHash)
            // build configuration
            addStatement(
                    """
                    val %L = %T.builder(%N.context)
                    .name(%N.name)
                    .callback(%L)
                    .build()
                    """.trimIndent(),
                    sqliteConfigVar,
                    SupportDbTypeNames.SQLITE_OPEN_HELPER_CONFIG,
                    configuration, configuration, callbackVar)
            addStatement("val %N = %N.sqliteOpenHelperFactory.create(%L)",
                    outVar, configuration, sqliteConfigVar)
        }
    }

    private fun createOpenCallback(scope: CodeGenScope): TypeSpec {
//        return TypeSpec.anonymousClassBuilder(L, database.version).apply {
        return TypeSpec.anonymousClassBuilder().apply {
            superclass(RoomTypeNames.OPEN_HELPER_DELEGATE)
            addSuperclassConstructorParameter("%L", database.version)
            addFunction(createCreateAllTables())
            addFunction(createDropAllTables())
            addFunction(createOnCreate(scope.fork()))
            addFunction(createOnOpen(scope.fork()))
            addFunction(createOnPreMigrate())
            addFunction(createOnPostMigrate())
            addFunctions(createValidateMigration(scope.fork()))
        }.build()
    }

    private fun createValidateMigration(scope: CodeGenScope): List<FunSpec> {
        val methodSpecs = mutableListOf<FunSpec>()
        val entities = ArrayDeque(database.entities)
        val views = ArrayDeque(database.views)
        val dbParam = ParameterSpec.builder( "_db", SupportDbTypeNames.DB).build()
        while (!entities.isEmpty() || !views.isEmpty()) {
            val isPrimaryMethod = methodSpecs.isEmpty()
            val methodName = if (isPrimaryMethod) {
                "validateMigration"
            } else {
                "validateMigration${methodSpecs.size + 1}"
            }
            methodSpecs.add(FunSpec.builder(methodName).apply {
                if (isPrimaryMethod) {
                    addModifiers(KModifier.PROTECTED, KModifier.OVERRIDE)
                } else {
                    addModifiers(KModifier.PRIVATE)
                }
                addParameter(dbParam)
                var statementCount = 0
                while (!entities.isEmpty() && statementCount < VALIDATE_CHUNK_SIZE) {
                    val methodScope = scope.fork()
                    val entity = entities.poll()
                    val validationWriter = when (entity) {
                        is FtsEntity -> FtsTableInfoValidationWriter(entity)
                        else -> TableInfoValidationWriter(entity)
                    }
                    validationWriter.write(dbParam, methodScope)
                    addCode(methodScope.builder().build())
                    statementCount += validationWriter.statementCount()
                }
                while (!views.isEmpty() && statementCount < VALIDATE_CHUNK_SIZE) {
                    val methodScope = scope.fork()
                    val view = views.poll()
                    val validationWriter = ViewInfoValidationWriter(view)
                    validationWriter.write(dbParam, methodScope)
                    addCode(methodScope.builder().build())
                    statementCount += validationWriter.statementCount()
                }
            }.build())
        }

        // If there are secondary validate methods then add invocation statements to all of them
        // from the primary method.
        if (methodSpecs.size > 1) {
            methodSpecs[0] = methodSpecs[0].toBuilder().apply {
                methodSpecs.drop(1).forEach {
                    addStatement("${it.name}(%N)", dbParam)
                }
            }.build()
        }

        return methodSpecs
    }

    private fun createOnCreate(scope: CodeGenScope): FunSpec {
        return FunSpec.builder("onCreate").apply {
            addModifiers(KModifier.PROTECTED, KModifier.OVERRIDE)
            addParameter( "_db", SupportDbTypeNames.DB)
            invokeCallbacks(scope, "onCreate")
        }.build()
    }

    private fun createOnOpen(scope: CodeGenScope): FunSpec {
        return FunSpec.builder("onOpen").apply {
            addModifiers(KModifier.OVERRIDE)
            addParameter("_db", SupportDbTypeNames.DB)
            addStatement("mDatabase = _db")
            if (database.enableForeignKeys) {
                addStatement("_db.execSQL(%S)", "PRAGMA foreign_keys = ON")
            }
            addStatement("internalInitInvalidationTracker(_db)")
            invokeCallbacks(scope, "onOpen")
        }.build()
    }

    private fun createCreateAllTables(): FunSpec {
        return FunSpec.builder("createAllTables").apply {
            addModifiers(KModifier.OVERRIDE)
            addParameter( "_db", SupportDbTypeNames.DB)
            database.bundle.buildCreateQueries().forEach {
                addStatement("_db.execSQL(%S)", it)
            }
        }.build()
    }

    private fun createDropAllTables(): FunSpec {
        return FunSpec.builder("dropAllTables").apply {
            addModifiers(KModifier.OVERRIDE)
            addParameter( "_db", SupportDbTypeNames.DB)
            database.entities.forEach {
                addStatement("_db.execSQL(%S)", createDropTableQuery(it))
            }
            database.views.forEach {
                addStatement("_db.execSQL(%S)", createDropViewQuery(it))
            }
        }.build()
    }

    private fun createOnPreMigrate(): FunSpec {
        return FunSpec.builder("onPreMigrate").apply {
            addModifiers(KModifier.OVERRIDE)
            addParameter( "_db", SupportDbTypeNames.DB)
            addStatement("%T.dropFtsSyncTriggers(%L)", RoomTypeNames.DB_UTIL, "_db")
        }.build()
    }

    private fun createOnPostMigrate(): FunSpec {
        return FunSpec.builder("onPostMigrate").apply {
            addModifiers(KModifier.OVERRIDE)
            addParameter( "_db", SupportDbTypeNames.DB)
            database.entities.filterIsInstance(FtsEntity::class.java)
                    .filter { it.ftsOptions.contentEntity != null }
                    .flatMap { it.contentSyncTriggerCreateQueries }
                    .forEach { syncTriggerQuery ->
                        addStatement("_db.execSQL(%S)", syncTriggerQuery)
                    }
        }.build()
    }

    private fun FunSpec.Builder.invokeCallbacks(scope: CodeGenScope, methodName: String) {
        beginControlFlow("mCallbacks?.forEach").apply {
                addStatement("it.%N(_db)", methodName)
        }
        endControlFlow()
    }

    @VisibleForTesting
    fun createTableQuery(entity: Entity): String {
        return entity.createTableQuery
    }

    @VisibleForTesting
    fun createViewQuery(view: DatabaseView): String {
        return view.createViewQuery
    }

    @VisibleForTesting
    fun createDropTableQuery(entity: Entity): String {
        return "DROP TABLE IF EXISTS `${entity.tableName}`"
    }

    @VisibleForTesting
    fun createDropViewQuery(view: DatabaseView): String {
        return "DROP VIEW IF EXISTS `${view.viewName}`"
    }
}
