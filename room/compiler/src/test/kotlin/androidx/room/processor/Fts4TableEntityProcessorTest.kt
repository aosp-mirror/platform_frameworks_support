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

package androidx.room.processor

import androidx.room.parser.FtsVersion
import androidx.room.parser.SQLTypeAffinity
import androidx.room.parser.Tokenizer
import androidx.room.vo.CallType
import androidx.room.vo.Field
import androidx.room.vo.FieldGetter
import androidx.room.vo.FieldSetter
import com.google.testing.compile.JavaFileObjects
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import javax.lang.model.type.TypeKind

@RunWith(JUnit4::class)
class Fts4TableEntityProcessorTest : BaseFtsEntityParserTest() {

    override fun getFtsVersion() = 4

    @Test
    fun simple() {
        singleEntity("""
                @PrimaryKey
                @ColumnInfo(name = "rowid")
                private int rowId;
                public int getRowId() { return rowId; }
                public void setRowId(int id) { this.rowId = rowId; }
            """
        ) { entity, invocation ->
            assertThat(entity.type.toString(), CoreMatchers.`is`("foo.bar.MyEntity"))
            assertThat(entity.fields.size, CoreMatchers.`is`(1))
            val field = entity.fields.first()
            val intType = invocation.processingEnv.typeUtils.getPrimitiveType(TypeKind.INT)
            assertThat(field, CoreMatchers.`is`(Field(
                    element = field.element,
                    name = "rowId",
                    type = intType,
                    columnName = "rowid",
                    affinity = SQLTypeAffinity.INTEGER)))
            assertThat(field.setter, `is`(FieldSetter("setRowId", intType, CallType.METHOD)))
            assertThat(field.getter, `is`(FieldGetter("getRowId", intType, CallType.METHOD)))
            assertThat(entity.primaryKey.fields, `is`(listOf(field)))
            assertThat(entity.shadowTableName, `is`("MyEntity_content"))
            assertThat(entity.ftsVersion, `is`(FtsVersion.FTS4))
        }.compilesWithoutError()
    }

    @Test
    fun omittedRowId() {
        singleEntity("""
                private String content;
                public String getContent() { return content; }
                public void setContent(String content) { this.content = content; }
            """
        ) { _, _ -> }.compilesWithoutError()
    }

    @Test
    fun missingPrimaryKeyAnnotation() {
        singleEntity("""
                @ColumnInfo(name = "rowid")
                private int rowId;
                public int getRowId(){ return rowId; }
                public void setRowId(int rowId) { this.rowId = rowId; }
                """) { _, _ -> }
                .failsToCompile()
                .withErrorContaining(ProcessorErrors.MISSING_PRIMARY_KEYS_ANNOTATION_IN_ROW_ID)
    }

    @Test
    fun badPrimaryKeyName() {
        singleEntity("""
                @PrimaryKey
                private int id;
                public int getId(){ return id; }
                public void setId(int id) { this.id = id; }
                """) { _, _ -> }
                .failsToCompile()
                .withErrorContaining(ProcessorErrors.INVALID_FTS_ENTITY_PRIMARY_KEY_NAME)
    }

    @Test
    fun badPrimaryKeyAffinity() {
        singleEntity("""
                @PrimaryKey
                @ColumnInfo(name = "rowid")
                private String rowId;
                public String getRowId(){ return rowId; }
                public void setRowId(String rowId) { this.rowId = rowId; }
                """) { _, _ -> }
                .failsToCompile()
                .withErrorContaining(ProcessorErrors.INVALID_FTS_ENTITY_PRIMARY_KEY_AFFINITY)
    }

    @Test
    fun multiplePrimaryKeys() {
        singleEntity("""
                @PrimaryKey
                public int oneId;
                @PrimaryKey
                public int twoId;
                """) { _, _ -> }
                .failsToCompile()
                .withErrorContaining(ProcessorErrors.TOO_MANY_PRIMARY_KEYS_IN_FTS_ENTITY)
    }

    @Test
    fun badForeignKey() {
        singleEntity("""
                @PrimaryKey
                public int rowid;
                @ForeignKey(entity = MyEntity.class, parentColumns = {}, childColumns = {})
                public int fkId;
                """) { _, _ -> }
                .failsToCompile()
                .withErrorContaining(ProcessorErrors.INVALID_FOREIGN_KEY_IN_FTS_ENTITY)
    }

    @Test
    fun nonDefaultTokenizer() {
        singleEntity("""
                @PrimaryKey
                @ColumnInfo(name = "rowid")
                private int rowId;
                public int getRowId() { return rowId; }
                public void setRowId(int id) { this.rowId = rowId; }
                """,
                attributes = hashMapOf("tokenizer" to "FtsOptions.PORTER")
        ) { entity, _ ->
            assertThat(entity.ftsOptions.tokenizer, `is`(Tokenizer.PORTER))
        }.compilesWithoutError()
    }

    @Test
    fun badExternalContentEntity_notAnEntity() {
        val contentSrc = JavaFileObjects.forSourceString("foo.bar.Content",
                """
                package foo.bar;
                import androidx.room.*;

                public class Content {
                    String text;
                }
                """)
        singleEntity("""
                @PrimaryKey
                public int rowid;
                public String text;
                public String extraData;
                """,
                attributes = hashMapOf("contentEntity" to "Content.class"),
                jfos = listOf(contentSrc)
        ) { _, _ -> }
                .failsToCompile()
                .withErrorContaining(ProcessorErrors.externalContentNotAnEntity("foo.bar.Content"))
    }

    @Test
    fun badExternalContentEntity_missingFields() {
        val contentSrc = JavaFileObjects.forSourceString("foo.bar.Content",
                """
                package foo.bar;
                import androidx.room.*;

                @Entity
                public class Content {
                    @PrimaryKey
                    int id;
                    String text;
                }
                """)
        singleEntity("""
                @PrimaryKey
                public int rowid;
                public String text;
                public String extraData;
                """,
                attributes = hashMapOf("contentEntity" to "Content.class"),
                jfos = listOf(contentSrc)
        ) { _, _ -> }
                .failsToCompile()
                .withErrorContaining(ProcessorErrors.missingFtsContentField("foo.bar.MyEntity",
                        "extraData", "foo.bar.Content"))
    }

    @Test
    fun missingLanguageIdField() {
        singleEntity("""
                @PrimaryKey
                @ColumnInfo(name = "rowid")
                public int rowId;
                public String body;
                """,
                attributes = hashMapOf("languageId" to "\"lid\"")
        ) { _, _ -> }
                .failsToCompile()
                .withErrorContaining(ProcessorErrors.missingLanguageIdField("lid"))
    }

    @Test
    fun badLanguageIdAffinity() {
        singleEntity("""
                @PrimaryKey
                @ColumnInfo(name = "rowid")
                public int rowId;
                public String body;
                public String lid;
                """,
                attributes = hashMapOf("languageId" to "\"lid\"")
        ) { _, _ -> }
                .failsToCompile()
                .withErrorContaining(ProcessorErrors.INVALID_FTS_ENTITY_LANGUAGE_ID_AFFINITY)
    }

    @Test
    fun missingNotIndexedField() {
        singleEntity("""
                @PrimaryKey
                @ColumnInfo(name = "rowid")
                public int rowId;
                """,
                attributes = hashMapOf("notIndexed" to "{\"body\"}")
        ) { _, _ -> }
                .failsToCompile()
                .withErrorContaining(ProcessorErrors.missingNotIndexedField(listOf("body")))
    }

    @Test
    fun badPrefixValue_zero() {
        singleEntity("""
                @PrimaryKey
                @ColumnInfo(name = "rowid")
                public int rowId;
                public String body;
                """,
                attributes = hashMapOf("prefix" to "{0,2}")
        ) { _, _ -> }
                .failsToCompile()
                .withErrorContaining(ProcessorErrors.INVALID_FTS_ENTITY_PREFIX_SIZES)
    }

    @Test
    fun badPrefixValue_negative() {
        singleEntity("""
                @PrimaryKey
                @ColumnInfo(name = "rowid")
                public int rowId;
                public String body;
                """,
                attributes = hashMapOf("prefix" to "{-2,2}")
        ) { _, _ -> }
                .failsToCompile()
                .withErrorContaining(ProcessorErrors.INVALID_FTS_ENTITY_PREFIX_SIZES)
    }
}