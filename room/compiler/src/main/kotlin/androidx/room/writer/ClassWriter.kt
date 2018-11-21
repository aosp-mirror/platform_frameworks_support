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

import androidx.room.RoomProcessor
import androidx.room.ext.typeName
import androidx.room.ext.writeTo
import androidx.room.solver.CodeGenScope.Companion.CLASS_PROPERTY_PREFIX
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import javax.annotation.processing.ProcessingEnvironment

/**
 * Base class for all writers that can produce a class.
 */
abstract class ClassWriter(private val className: ClassName) {
    private val sharedPropertySpecs = mutableMapOf<String, PropertySpec>()
    private val sharedFunSpecs = mutableMapOf<String, FunSpec>()
    private val sharedFieldNames = mutableSetOf<String>()
    private val sharedMethodNames = mutableSetOf<String>()

    abstract fun createTypeSpecBuilder(): TypeSpec.Builder

    fun write(processingEnv: ProcessingEnvironment) {
        val builder = createTypeSpecBuilder()
        sharedPropertySpecs.values.forEach { builder.addProperty(it) }
        sharedFunSpecs.values.forEach { builder.addFunction(it) }
        addGeneratedAnnotationIfAvailable(builder, processingEnv)
        addSuppressUnchecked(builder)
        FileSpec.get(className.packageName, builder.build()).writeTo(processingEnv.filer)
    }

    private fun addSuppressUnchecked(builder: TypeSpec.Builder) {
        val suppressSpec = AnnotationSpec.builder(SuppressWarnings::class.typeName()).addMember(
                "value",
                "%S",
                "unchecked"
        ).build()
        builder.addAnnotation(suppressSpec)
    }

    private fun addGeneratedAnnotationIfAvailable(adapterTypeSpecBuilder: TypeSpec.Builder,
                                                  processingEnv: ProcessingEnvironment) {
        val generatedAnnotationAvailable = processingEnv
                .elementUtils
                .getTypeElement("$GENERATED_PACKAGE.$GENERATED_NAME") != null
        if (generatedAnnotationAvailable) {
            val className = ClassName(GENERATED_PACKAGE, GENERATED_NAME)
            val generatedAnnotationSpec =
                    AnnotationSpec.builder(className).addMember(
                            "value",
                            "%S",
                            RoomProcessor::class.java.canonicalName).build()
            adapterTypeSpecBuilder.addAnnotation(generatedAnnotationSpec)
        }
    }

    private fun makeUnique(set: MutableSet<String>, value: String): String {
        if (!value.startsWith(CLASS_PROPERTY_PREFIX)) {
            return makeUnique(set, "$CLASS_PROPERTY_PREFIX$value")
        }
        if (set.add(value)) {
            return value
        }
        var index = 1
        while (true) {
            if (set.add("${value}_$index")) {
                return "${value}_$index"
            }
            index++
        }
    }

    fun getOrCreateField(sharedField: SharedPropertySpec): PropertySpec {
        return sharedPropertySpecs.getOrPut(sharedField.getUniqueKey(), {
            sharedField.build(this, makeUnique(sharedFieldNames, sharedField.baseName))
        })
    }

    fun getOrCreateMethod(sharedMethod: SharedFunSpec): FunSpec {
        return sharedFunSpecs.getOrPut(sharedMethod.getUniqueKey(), {
            sharedMethod.build(this, makeUnique(sharedMethodNames, sharedMethod.baseName))
        })
    }

    abstract class SharedPropertySpec(val baseName: String, val type: TypeName) {

        abstract fun getUniqueKey(): String

        abstract fun prepare(writer: ClassWriter, builder: PropertySpec.Builder)

        fun build(classWriter: ClassWriter, name: String): PropertySpec {
            val builder = PropertySpec.builder(name, type)
            prepare(classWriter, builder)
            return builder.build()
        }
    }

    abstract class SharedFunSpec(val baseName: String) {

        abstract fun getUniqueKey(): String
        abstract fun prepare(methodName: String, writer: ClassWriter, builder: FunSpec.Builder)

        fun build(writer: ClassWriter, name: String): FunSpec {
            val builder = FunSpec.builder(name)
            prepare(name, writer, builder)
            return builder.build()
        }
    }

    companion object {
        private const val GENERATED_PACKAGE = "javax.annotation"
        private const val GENERATED_NAME = "Generated"
    }
}
