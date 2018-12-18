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

package androidx.navigation.safe.args.generator.kotlin

import androidx.navigation.safe.args.generator.NavWriter
import androidx.navigation.safe.args.generator.ObjectArrayType
import androidx.navigation.safe.args.generator.ObjectType
import androidx.navigation.safe.args.generator.ext.toCamelCase
import androidx.navigation.safe.args.generator.ext.toCamelCaseAsVar
import androidx.navigation.safe.args.generator.models.Action
import androidx.navigation.safe.args.generator.models.Destination
import androidx.navigation.safe.args.generator.models.accessor
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import java.lang.IllegalArgumentException

class KotlinNavWriter(
    private val useAndroidX: Boolean = false
) : NavWriter<KotlinCodeFile> {

    override fun generateDirectionsCodeFile(
        destination: Destination,
        parentDirection: KotlinCodeFile?
    ): KotlinCodeFile {
        val destName = destination.name
            ?: throw IllegalStateException("Destination with actions must have name")
        val className = ClassName(destName.packageName(), "${destName.simpleName()}Directions")

        val actionTypes = destination.actions.map { action ->
            action to generateDirectionTypeSpec(action)
        }

        val actionsFunSpec = actionTypes.map { (action, actionTypeSpec) ->
            val typeName = ClassName("", actionTypeSpec.name!!)
            val parameters = action.args.map { arg ->
                ParameterSpec.builder(
                    name = arg.sanitizedName,
                    type = arg.type.typeName().copy(nullable = arg.isNullable)
                ).apply {
                    arg.defaultValue?.let {
                        defaultValue(it.write())
                    }
                }.build()
            }
            FunSpec.builder(action.id.javaIdentifier.toCamelCaseAsVar()).apply {
                returns(typeName)
                addParameters(parameters)
                if (action.args.isEmpty()) {
                    addStatement("return %T", typeName)
                } else {
                    addStatement(
                        "return %T(${parameters.joinToString(", ") { it.name }})",
                        typeName
                    )
                }
            }.build()
        }

        val typeSpec = TypeSpec.classBuilder(className)
            .addTypes(actionTypes.map { (_, type) -> type })
            .addType(
                TypeSpec.companionObjectBuilder()
                    .addFunctions(actionsFunSpec)
                    .build()
            )
            .build()

        return FileSpec.builder(className.packageName, className.simpleName)
            .addType(typeSpec)
            .build()
            .toCodeFile()
    }

    internal fun generateDirectionTypeSpec(action: Action): TypeSpec {
        val className = ClassName("", action.id.javaIdentifier.toCamelCase())

        val getActionIdFunSpec = FunSpec.builder("getActionId")
            .addModifiers(KModifier.OVERRIDE)
            .returns(Int::class)
            .addStatement("return %N", action.id.accessor())
            .build()

        val getArgumentsFunSpec = FunSpec.builder("getArguments").apply {
            addModifiers(KModifier.OVERRIDE)
            if (action.args.any { it.type is ObjectType }) {
                addAnnotation(
                    AnnotationSpec.builder(Suppress::class)
                        .addMember("%S", "CAST_NEVER_SUCCEEDS")
                        .build()
                )
            }
            returns(BUNDLE_CLASSNAME)
            val resultVal = "__result"
            addStatement("val %L = %T()", resultVal, BUNDLE_CLASSNAME)
            action.args.forEach { arg ->
                arg.type.addBundlePutStatement(this, arg, resultVal, arg.sanitizedName)
            }
            addStatement("return %L", resultVal)
        }.build()

        val constructorFunSpec = FunSpec.constructorBuilder()
            .addModifiers(KModifier.INTERNAL)
            .addParameters(action.args.map { arg ->
                ParameterSpec.builder(
                    name = arg.sanitizedName,
                    type = arg.type.typeName().copy(nullable = arg.isNullable)
                ).build()
            })
            .build()

        return if (action.args.isEmpty()) {
            TypeSpec.objectBuilder(className)
        } else {
            TypeSpec.classBuilder(className)
                .addModifiers(KModifier.DATA)
                .primaryConstructor(constructorFunSpec)
                .addProperties(action.args.map { arg ->
                    PropertySpec.builder(
                        arg.sanitizedName,
                        arg.type.typeName().copy(nullable = arg.isNullable)
                    ).initializer(arg.sanitizedName).build()
                })
        }.addSuperinterface(NAV_DIRECTION_CLASSNAME)
            .addFunction(getActionIdFunSpec)
            .addFunction(getArgumentsFunSpec)
            .build()
    }

    override fun generateArgsCodeFile(destination: Destination): KotlinCodeFile {
        val destName = destination.name
            ?: throw IllegalStateException("Destination with actions must have name")
        val className = ClassName(destName.packageName(), "${destName.simpleName()}Args")

        val constructorFunSpec = FunSpec.constructorBuilder()
            .addParameters(destination.args.map { arg ->
                ParameterSpec.builder(
                    name = arg.sanitizedName,
                    type = arg.type.typeName().copy(nullable = arg.isNullable)
                ).apply { arg.defaultValue?.let { defaultValue(it.write()) } }.build()
            })
            .build()

        val toBundleFunSpec = FunSpec.builder("toBundle").apply {
            if (destination.args.any { it.type is ObjectType }) {
                addAnnotation(
                    AnnotationSpec.builder(Suppress::class)
                        .addMember("%S", "CAST_NEVER_SUCCEEDS")
                        .build()
                )
            }
            returns(BUNDLE_CLASSNAME)
            val resultVal = "__result"
            addStatement("val %L = %T()", resultVal, BUNDLE_CLASSNAME)
            destination.args.forEach { arg ->
                arg.type.addBundlePutStatement(this, arg, resultVal, arg.sanitizedName)
            }
            addStatement("return %L", resultVal)
        }.build()

        val fromBundleFunSpec = FunSpec.builder("fromBundle").apply {
            if (destination.args.any { it.type is ObjectArrayType }) {
                addAnnotation(
                    AnnotationSpec.builder(Suppress::class)
                        .addMember("%S", "UNCHECKED_CAST")
                        .build()
                )
            }
            returns(className)
            val bundleParamName = "bundle"
            addParameter(bundleParamName, BUNDLE_CLASSNAME)
            addStatement(
                "%L.setClassLoader(%T::class.java.classLoader)",
                bundleParamName,
                className
            )
            val tempVariables = destination.args.map { arg ->
                val tempVal = "__${arg.sanitizedName}"
                addStatement(
                    "val %L : %T",
                    tempVal,
                    arg.type.typeName().copy(nullable = arg.type.allowsNullable())
                )
                beginControlFlow("if (%L.containsKey(%S))", bundleParamName, arg.name)
                arg.type.addBundleGetStatement(this, arg, tempVal, bundleParamName)
                if (arg.type.allowsNullable() && !arg.isNullable) {
                    beginControlFlow("if (%L == null)", tempVal).apply {
                        addStatement(
                            "throw·%T(%S)",
                            IllegalArgumentException::class.asTypeName(),
                            "Argument \"${arg.name}\" is marked as non-null but was passed a " +
                                    "null value."
                        )
                    }
                    endControlFlow()
                }
                nextControlFlow("else")
                val defaultValue = arg.defaultValue
                if (defaultValue != null) {
                    addStatement("%L = %L", tempVal, arg.defaultValue.write())
                } else {
                    addStatement(
                        "throw·%T(%S)",
                        IllegalArgumentException::class.asTypeName(),
                        "Required argument \"${arg.name}\" is missing and does not have an " +
                                "android:defaultValue"
                    )
                }
                endControlFlow()
                return@map tempVal
            }
            addStatement("return %T(${tempVariables.joinToString(", ") { it }})", className)
        }.build()

        val typeSpec = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.DATA)
            .primaryConstructor(constructorFunSpec)
            .addProperties(destination.args.map { arg ->
                PropertySpec.builder(
                    arg.sanitizedName,
                    arg.type.typeName().copy(nullable = arg.isNullable)
                ).initializer(arg.sanitizedName).build()
            })
            .addFunction(toBundleFunSpec)
            .addType(
                TypeSpec.companionObjectBuilder()
                    .addFunction(fromBundleFunSpec)
                    .build()
            )
            .build()

        return FileSpec.builder(className.packageName, className.simpleName)
            .addType(typeSpec)
            .build()
            .toCodeFile()
    }
}