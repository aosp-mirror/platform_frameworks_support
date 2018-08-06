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

import androidx.room.ext.getAsInt
import androidx.room.ext.getAsIntList
import androidx.room.ext.getAsString
import androidx.room.ext.getAsStringList
import androidx.room.parser.FtsOrder
import androidx.room.parser.FtsVersion
import androidx.room.parser.SQLTypeAffinity
import androidx.room.parser.Tokenizer
import androidx.room.processor.cache.Cache
import androidx.room.vo.Field
import androidx.room.vo.FtsEntity
import androidx.room.vo.FtsOptions
import androidx.room.vo.LanguageId
import androidx.room.vo.PrimaryKey
import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.MoreElements
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement

class FtsEntityProcessor internal constructor(
    baseContext: Context,
    element: TypeElement,
    referenceStack: LinkedHashSet<Name> = LinkedHashSet()
) : EntityProcessor(baseContext, element, referenceStack) {

    override fun process(): androidx.room.vo.FtsEntity {
        return context.cache.entities.get(Cache.EntityKey(element)) {
            doProcess()
        } as androidx.room.vo.FtsEntity
    }

    private fun doProcess(): FtsEntity {
        val pojo = PojoProcessor.createFor(
                context = context,
                element = element,
                bindingScope = FieldProcessor.BindingScope.TWO_WAY,
                parent = null,
                referenceStack = referenceStack).process()

        context.checker.check(pojo.relations.isEmpty(), element, ProcessorErrors.RELATION_IN_ENTITY)

        val annotation = MoreElements.getAnnotationMirror(element,
                androidx.room.FtsEntity::class.java).orNull()
        val tableName: String
        val ftsVersion: FtsVersion
        val ftsOptions: FtsOptions
        if (annotation != null) {
            tableName = extractTableName(element, annotation)
            ftsVersion = FtsVersion.fromAnnotationValue(
                    AnnotationMirrors.getAnnotationValue(annotation, "version")
                            .getAsInt(androidx.room.FtsEntity.FTS4)!!)
            ftsOptions = getAnnotationFTSOptions(ftsVersion, annotation)
        } else {
            tableName = element.simpleName.toString()
            ftsVersion = FtsVersion.FTS4
            ftsOptions = getAnnotationFTSOptions(ftsVersion, annotation)
        }

        // The %_content table contains the unadulterated data inserted by the user into the FTS
        // virtual table. See: https://www.sqlite.org/fts3.html#shadow_tables
        val shadowTableName = "${tableName}_content"
        val primaryKey = findAndValidatePrimaryKey(pojo.fields)
        val languageId = findAndValidateLanguageId(pojo.fields, ftsOptions.languageIdColumnName)

        with(pojo.fields.map { it.columnName }) {
            ftsOptions.notIndexedColumns.filterNot { contains(it) }
        }.let {
            context.checker.check(it.isEmpty(), element, ProcessorErrors.missingNotIndexedField(it))
        }

        context.checker.check(ftsOptions.prefixSizes.all { it > 0 },
                element, ProcessorErrors.INVALID_FTS_ENTITY_PREFIX_SIZES)

        pojo.fields.filterNot {
            it.affinity == SQLTypeAffinity.TEXT ||
                    it == primaryKey.fields.first() ||
                    it == languageId.field
        }.forEach {
            context.logger.e(it.element, ProcessorErrors.INVALID_FTS_ENTITY_FIELD_AFFINITY)
        }

        return FtsEntity(
                element = element,
                tableName = tableName,
                type = pojo.type,
                fields = pojo.fields,
                embeddedFields = pojo.embeddedFields,
                primaryKey = primaryKey,
                constructor = pojo.constructor,
                ftsVersion = ftsVersion,
                ftsOptions = ftsOptions,
                shadowTableName = shadowTableName)
    }

    private fun getAnnotationFTSOptions(
        version: FtsVersion,
        annotation: AnnotationMirror?
    ): FtsOptions {
        if (annotation == null) {
            return FtsOptions(
                    tokenizer = Tokenizer.SIMPLE,
                    tokenizerArgs = emptyList(),
                    languageIdColumnName = "",
                    matchInfo = FtsVersion.FTS4,
                    notIndexedColumns = emptyList(),
                    prefixSizes = emptyList(),
                    preferredOrder = FtsOrder.ASC)
        }

        val tokenizer = Tokenizer.fromAnnotationValue(
                AnnotationMirrors.getAnnotationValue(annotation, "tokenizer")
                        .getAsInt(androidx.room.FtsEntity.SIMPLE)!!)
        val tokenizerArgs = AnnotationMirrors.getAnnotationValue(annotation, "tokenizerArgs")
                .getAsStringList()

        val languageIdColumnName = AnnotationMirrors.getAnnotationValue(annotation, "languageId")
                .getAsString() ?: ""
        context.checker.check(version != FtsVersion.FTS3 || languageIdColumnName.isEmpty(),
                element, ProcessorErrors.unsupportedFTS3Option("languageid"))

        val matchInfo = FtsVersion.fromAnnotationValue(
                AnnotationMirrors.getAnnotationValue(annotation, "matchInfo")
                        .getAsInt(androidx.room.FtsEntity.FTS4)!!)
        context.checker.check(version != FtsVersion.FTS3 || matchInfo == FtsVersion.FTS4,
                element, ProcessorErrors.unsupportedFTS3Option("matchinfo"))

        val notIndexedColumns =
                AnnotationMirrors.getAnnotationValue(annotation, "notIndexed")
                        .getAsStringList()
        context.checker.check(version != FtsVersion.FTS3 || notIndexedColumns.isEmpty(),
                element, ProcessorErrors.unsupportedFTS3Option("notindexed"))

        val prefixSizes = AnnotationMirrors.getAnnotationValue(annotation, "prefix")
                .getAsIntList()
        context.checker.check(version != FtsVersion.FTS3 || prefixSizes.isEmpty(),
                element, ProcessorErrors.unsupportedFTS3Option("prefix"))

        val preferredOrder = FtsOrder.fromAnnotationValue(
                AnnotationMirrors.getAnnotationValue(annotation, "order")
                        .getAsInt(androidx.room.FtsEntity.ASC)!!)
        context.checker.check(version != FtsVersion.FTS3 || preferredOrder == FtsOrder.ASC,
                element, ProcessorErrors.unsupportedFTS3Option("order"))

        return FtsOptions(
                tokenizer = tokenizer,
                tokenizerArgs = tokenizerArgs,
                languageIdColumnName = languageIdColumnName,
                matchInfo = matchInfo,
                notIndexedColumns = notIndexedColumns,
                prefixSizes = prefixSizes,
                preferredOrder = preferredOrder)
    }

    private fun findAndValidatePrimaryKey(fields: List<Field>): PrimaryKey {
        val primaryKeys = fields.mapNotNull { field ->
            MoreElements.getAnnotationMirror(field.element, androidx.room.PrimaryKey::class.java)
                    .orNull()?.let {
                        PrimaryKey(
                                declaredIn = field.element.enclosingElement,
                                fields = listOf(field),
                                autoGenerateId = true)
            }
        }
        if (primaryKeys.isEmpty()) {
            return PrimaryKey.MISSING
        }
        context.checker.check(primaryKeys.size == 1, element,
                ProcessorErrors.TOO_MANY_PRIMARY_KEYS_IN_FTS_ENTITY)
        val primaryKey = primaryKeys.first()
        context.checker.check(primaryKey.columnNames.first() == "rowid",
                primaryKey.declaredIn ?: element,
                ProcessorErrors.INVALID_FTS_ENTITY_PRIMARY_KEY_NAME)
        context.checker.check(primaryKey.fields.first().affinity == SQLTypeAffinity.INTEGER,
                primaryKey.declaredIn ?: element,
                ProcessorErrors.INVALID_FTS_ENTITY_PRIMARY_KEY_AFFINITY)
        return primaryKey
    }

    private fun findAndValidateLanguageId(
        fields: List<Field>,
        languageIdColumnName: String
    ): LanguageId {
        if (languageIdColumnName.isEmpty()) {
            return LanguageId.MISSING
        }

        val languageIdField = fields.firstOrNull { it.columnName == languageIdColumnName }
        if (languageIdField == null) {
            context.logger.e(element, ProcessorErrors.missingLanguageIdField(languageIdColumnName))
            return LanguageId.MISSING
        }

        context.checker.check(languageIdField.affinity == SQLTypeAffinity.INTEGER,
                languageIdField.element, ProcessorErrors.INVALID_FTS_ENTITY_LANGUAGE_ID_AFFINITY)
        return LanguageId(languageIdField.element, languageIdField)
    }
}