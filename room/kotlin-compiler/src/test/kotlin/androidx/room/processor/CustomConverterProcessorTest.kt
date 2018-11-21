/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room.processor

import androidx.room.TypeConverter
import androidx.room.ext.typeName
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_EMPTY_CLASS
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_MISSING_NOARG_CONSTRUCTOR
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_MUST_BE_PUBLIC
import androidx.room.processor.ProcessorErrors.TYPE_CONVERTER_UNBOUND_GENERIC
import androidx.room.testing.TestInvocation
import androidx.room.vo.CustomTypeConverter
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import simpleRun
import java.util.Date
import javax.tools.JavaFileObject

@RunWith(JUnit4::class)
class CustomConverterProcessorTest {
    companion object {
        val CONVERTER = ClassName("foo.bar", "MyConverter")!!
        val CONVERTER_QNAME = CONVERTER.packageName + "." + CONVERTER.simpleName
        val CONTAINER = JavaFileObjects.forSourceString("foo.bar.Container",
                """
                package foo.bar;
                import androidx.room.*;
                @TypeConverters(foo.bar.MyConverter.class)
                public class Container {}
                """)
    }

    @Test
    fun validCase() {
        singleClass(createConverter(SHORT.asNullable(), CHAR.asNullable())) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(SHORT.asNullable() as TypeName))
            assertThat(converter?.toTypeName, `is`(CHAR.asNullable() as TypeName))
        }.compilesWithoutError()
    }

    @Test
    fun primitiveFrom() {
        singleClass(createConverter(SHORT, CHAR.asNullable())) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(SHORT as TypeName))
            assertThat(converter?.toTypeName, `is`(CHAR.asNullable() as TypeName))
        }.compilesWithoutError()
    }

    @Test
    fun primitiveTo() {
        singleClass(createConverter(INT.asNullable(), DOUBLE)) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(INT.asNullable() as TypeName))
            assertThat(converter?.toTypeName, `is`(DOUBLE as TypeName))
        }.compilesWithoutError()
    }

    @Test
    fun primitiveBoth() {
        singleClass(createConverter(INT, DOUBLE)) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(INT as TypeName))
            assertThat(converter?.toTypeName, `is`(DOUBLE as TypeName))
        }.compilesWithoutError()
    }

    @Test
    fun nonNullButNotasNullableed() {
        val string = String::class.typeName()
        val date = Date::class.typeName()
        singleClass(createConverter(string, date)) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(string as TypeName))
            assertThat(converter?.toTypeName, `is`(date as TypeName))
        }
    }

    @Test
    fun parametrizedTypeUnbound() {
        val typeVarT = TypeVariableName("T")
        val list = List::class.typeName().parameterizedBy(typeVarT)
        val typeVarK = TypeVariableName("K")
        val map = Map::class.typeName().parameterizedBy(typeVarK, typeVarT)
        singleClass(createConverter(list, map, listOf(typeVarK, typeVarT))) {
            _, _ ->
        }.failsToCompile().withErrorContaining(TYPE_CONVERTER_UNBOUND_GENERIC)
    }

    @Test
    fun parametrizedTypeSpecific() {
        val string = String::class.typeName()
        val date = Date::class.typeName()
        val list = List::class.typeName().parameterizedBy(string)
        val map = Map::class.typeName().parameterizedBy(string, date)
        singleClass(createConverter(list, map)) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(list as TypeName))
            assertThat(converter?.toTypeName, `is`(map as TypeName))
        }.compilesWithoutError()
    }

    @Test
    fun testNoConverters() {
        singleClass(JavaFileObjects.forSourceString(CONVERTER_QNAME,
                """
                package ${CONVERTER.packageName};
                public class ${CONVERTER.simpleName} {
                }
                """)) { _, _ ->
        }.failsToCompile().withErrorContaining(TYPE_CONVERTER_EMPTY_CLASS)
    }

    @Test
    fun checkNoArgConstructor() {
        singleClass(JavaFileObjects.forSourceString(CONVERTER_QNAME,
                """
                package ${CONVERTER.packageName};
                import androidx.room.TypeConverter;

                public class ${CONVERTER.simpleName} {
                    public ${CONVERTER.simpleName}(int x) {}
                    @TypeConverter
                    public int x(short y) {return 0;}
                }
                """)) { _, _ ->
        }.failsToCompile().withErrorContaining(TYPE_CONVERTER_MISSING_NOARG_CONSTRUCTOR)
    }

    @Test
    fun checkNoArgConstructor_withStatic() {
        singleClass(JavaFileObjects.forSourceString(CONVERTER_QNAME,
                """
                package ${CONVERTER.packageName};
                import androidx.room.TypeConverter;

                public class ${CONVERTER.simpleName} {
                    public ${CONVERTER.simpleName}(int x) {}
                    @TypeConverter
                    public static int x(short y) {return 0;}
                }
                """)) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(SHORT as TypeName))
            assertThat(converter?.toTypeName, `is`(INT as TypeName))
            assertThat(converter?.isStatic, `is`(true))
        }.compilesWithoutError()
    }

    @Test
    fun checkPublic() {
        singleClass(JavaFileObjects.forSourceString(CONVERTER_QNAME,
                """
                package ${CONVERTER.packageName};
                import androidx.room.TypeConverter;

                public class ${CONVERTER.simpleName} {
                    @TypeConverter static int x(short y) {return 0;}
                    @TypeConverter private static int y(boolean y) {return 0;}
                }
                """)) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(SHORT as TypeName))
            assertThat(converter?.toTypeName, `is`(INT as TypeName))
            assertThat(converter?.isStatic, `is`(true))
        }.failsToCompile().withErrorContaining(TYPE_CONVERTER_MUST_BE_PUBLIC).and()
                .withErrorCount(2)
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    @Test
    fun parametrizedTypeBoundViaParent() {
        val typeVarT = TypeVariableName("T")
        val list = List::class.typeName().parameterizedBy(typeVarT)
        val typeVarK = TypeVariableName("K")
        val map = Map::class.typeName().parameterizedBy(typeVarK, typeVarT)

        val baseConverter = createConverter(list, map, listOf(typeVarT, typeVarK))
        val extendingQName = "foo.bar.Extending"
        val extendingClass = JavaFileObjects.forSourceString(extendingQName,
                "package foo.bar;\n" +
                        TypeSpec.classBuilder(ClassName.bestGuess(extendingQName)).apply {
                            superclass(
                                    CONVERTER.parameterizedBy(String::class.typeName(),
                                    Integer::class.typeName()))
                        }.build().toString())

        simpleRun(baseConverter, extendingClass) { invocation ->
            val element = invocation.processingEnv.elementUtils.getTypeElement(extendingQName)
            val converter = CustomConverterProcessor(invocation.context, element)
                    .process().firstOrNull()
            assertThat(converter?.fromTypeName, `is`(
                    List::class.typeName().parameterizedBy(String::class.typeName()) as TypeName
            ))
            assertThat(converter?.toTypeName, `is`(Map::class.typeName().parameterizedBy(
                    Integer::class.typeName(), String::class.typeName()) as TypeName
            ))
        }.compilesWithoutError()
    }

    @Test
    fun checkDuplicates() {
        singleClass(
                createConverter(SHORT.asNullable(), CHAR.asNullable(), duplicate = true)
        ) { converter, _ ->
            assertThat(converter?.fromTypeName, `is`(SHORT.asNullable() as TypeName))
            assertThat(converter?.toTypeName, `is`(CHAR.asNullable() as TypeName))
        }.failsToCompile().withErrorContaining("Multiple methods define the same conversion")
    }

    private fun createConverter(
        from: TypeName,
        to: TypeName,
        typeVariables: List<TypeVariableName> = emptyList(),
        duplicate: Boolean = false
    ): JavaFileObject {
        val code = TypeSpec.classBuilder(CONVERTER).apply {
            addTypeVariables(typeVariables)
            fun buildMethod(name: String) = FunSpec.builder(name).apply {
                addAnnotation(TypeConverter::class.java)
                returns(to)
                addParameter(ParameterSpec.builder("input", from).build())
                if (!to.nullable) {
                    addStatement("return 0")
                } else {
                    addStatement("return null")
                }
            }.build()
            addFunction(buildMethod("convertF"))
            if (duplicate) {
                addFunction(buildMethod("convertF2"))
            }
        }.build().toString()
        return JavaFileObjects.forSourceString(CONVERTER.toString(),
                "package ${CONVERTER.packageName};\n$code")
    }

    private fun singleClass(
            vararg jfo: JavaFileObject,
            handler: (CustomTypeConverter?, TestInvocation) -> Unit
    ): CompileTester {
        return simpleRun(*((jfo.toList() + CONTAINER).toTypedArray())) { invocation ->
            val processed = CustomConverterProcessor.findConverters(invocation.context,
                    invocation.processingEnv.elementUtils.getTypeElement("foo.bar.Container"))
            handler(processed.converters.firstOrNull()?.custom, invocation)
        }
    }
}
