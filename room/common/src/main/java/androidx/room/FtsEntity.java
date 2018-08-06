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

package androidx.room;

import androidx.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an entity. This class will have a mapping SQLite FTS table in the database.
 * <p>
 * <a href="https://www.sqlite.org/fts3.html">FTS3 and FTS4</a> are SQLite virtual table modules
 * that allows full-text searches to be performed on a set of documents.
 * <p>
 * An FtsEntity table always has a column named <code>rowid</code> that is the equivalent of an
 * <code>INTEGER PRIMARY KEY</code> index. Therefore an FtsEntity can only have a single field
 * annotated with {@link PrimaryKey}, it must be named <code>rowid</code> and must be of
 * <code>INTEGER</code> affinity. The field can be optionally omitted in the class but can still be
 * used in queries.
 * <p>
 * All fields in an FtsEntity must be of <code>TEXT</code> affinity, expect the for the 'rowid' and
 * 'languageid' fields.
 * <p>
 * Similar to an {@link Entity}, each FtsEntity must either have a no-arg constructor or a
 * constructor whose parameters match fields (based on type and name).
 * <p>
 * Example:
 * <pre>
 * {@literal @}FtsEntity
 * public class Mail {
 *   {@literal @}PrimaryKey
 *   {@literal @}ColumnInfo(name = "rowid")
 *   private final int rowId;
 *   private final String subject;
 *   private final String body;
 *
 *   public Mail(int rowId, String subject, String body) {
 *       this.rowId = rowId;
 *       this.subject = subject;
 *       this.body = body;
 *   }
 *
 *   public String getRowId() {
 *       return rowId;
 *   }
 *   public String getSubject() {
 *       return subject;
 *   }
 *   public void getBody() {
 *       return body;
 *   }
 * }
 * </pre>
 *
 * @see Entity
 * @see Dao
 * @see Database
 * @see PrimaryKey
 * @see ColumnInfo
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface FtsEntity {

    /**
     * The table name in the SQLite database. If not set, defaults to the class name.
     *
     * @return The SQLite tableName of the FtsEntity.
     */
    String tableName() default "";

    /**
     * The FTS extension module version when creating the virtual table.
     * <p>
     * Default value is {@link #FTS4}.
     *
     * @return The extension module to use for creating the virtual table. This is {@link #FTS3} or
     * {@link #FTS4}
     */
    @FTSVersion int version() default FTS4;

    /**
     * The tokenizer to be used in the FTS table.
     * <p>
     * The default value is {@link #SIMPLE}. Tokenizer arguments can be defined with
     * {@link #tokenizerArgs()}.
     *
     * @return The tokenizer to use on the FTS table. This is either {@link #SIMPLE},
     * {@link #PORTER} or {@link #UNICODE61}.
     * @see #tokenizerArgs()
     * @see <a href="https://www.sqlite.org/fts3.html#tokenizer">SQLite tokernizers
     * documentation</a>
     */
    @Tokenizer int tokenizer() default SIMPLE;

    /**
     * Optional arguments to configure the defined tokenizer.
     * <p>
     * Tokenizer arguments consist of an argument name, followed by an "=" character, followed by
     * the option value. For example, <code>separators=.</code> defines the dot character as an
     * additional separator when using the {@link #UNICODE61} tokenizer.
     * <p>
     * The available arguments that can be defined depend on the tokenizer defined, see the
     * <a href="https://www.sqlite.org/fts3.html#tokenizer">SQLite tokernizers documentation</a> for
     * details.
     *
     * @return A list of tokenizer arguments strings.
     */
    String[] tokenizerArgs() default {};

    /**
     * The column name to be used as 'languageid'.
     * <p>
     * Allows the FTS4 extension to use the defined column name to specify the language stored in
     * each row. When this is defined a field of type <code>INTEGER</code> with the same name must
     * exist in the class.
     * <p>
     * FTS queries are affected by defining this option, see
     * <a href=https://www.sqlite.org/fts3.html#the_languageid_option>the languageid= option
     * documentation</a> for details.
     * <p>
     * This option is only available in {@link #FTS4}.
     *
     * @return The column name to be used as 'languageid'.
     */
    String languageId() default "";

    /**
     * The FTS version to store matching information.
     * <p>
     * The default value is {@link #FTS4}. FTS4 disk space consumption is reduced by this option,
     * see <a href=https://www.sqlite.org/fts3.html#the_matchinfo_option>the matchinfo= option
     * documentation</a> for details.
     * <p>
     * This option is only available in {@link #FTS4}.
     *
     * @return The match info version, eithert {@link #FTS4} or {@link #FTS3}.
     */
    @MatchInfo int matchInfo() default FTS4;

    /**
     * The list of column names on the FTS table that won't be indexed.
     * <p>
     * This option is only available in {@link #FTS4}.
     *
     * @return A list of column names that will not be indexed by the FTS extension.
     * @see <a href="https://www.sqlite.org/fts3.html#the_notindexed_option">The notindexed= option
     * documentation</a>
     */
    String[] notIndexed() default {};

    /**
     * The list of prefix sizes to index.
     * <p>
     * This option is only available in {@link #FTS4}.
     *
     * @return A list of non-zero positive prefix sizes to index.
     * @see <a href="https://www.sqlite.org/fts3.html#the_prefix_option">The prefix= option
     * documentation</a>
     */
    int[] prefix() default {};

    /**
     * The preferred 'rowid' order of the FTS table.
     * <p>
     * The default value is {@link #ASC}. If many queries are run against the FTS table use
     * <code>ORDER BY row DESC</code> then it may improve performance to set this option to
     * {@link #DESC}, enabling the FTS module to store its data in a way that optimizes returning
     * results in descending order by rowid.
     * <p>
     * This option is only available in {@link #FTS4}.
     *
     * @return The preferred order, either {@link #ASC} or {@link #DESC}.
     */
    @Order int order() default ASC;

    /**
     * Version 3 of the extension module.
     *
     * @see #version()
     * @see #matchInfo()
     */
    int FTS3 = 1;

    /**
     * Version 4 of the extension module.
     *
     * @see #version()
     * @see #matchInfo()
     */
    int FTS4 = 2;

    @IntDef({FTS3, FTS4})
    @interface FTSVersion {
    }

    /**
     * The name of the default tokenizer used on FTS tables.
     *
     * @see #tokenizer()
     * @see #tokenizerArgs()
     */
    int SIMPLE = 0;

    /**
     * The name of the tokenizer based on the Porter Stemming Algorithm.
     *
     * @see #tokenizer()
     * @see #tokenizerArgs()
     */
    int PORTER = 1;

    /**
     * The name of a tokenizer implemented by the ICU library.
     * <p>
     * Not available in certain Android builds (e.g. vendor).
     *
     * @see #tokenizer()
     * @see #tokenizerArgs()
     */
    int ICU = 2;

    /**
     * The name of the tokenizer that extends the {@link #SIMPLE} according to rules in Unicode
     * Version 6.1
     *
     * @see #tokenizer()
     * @see #tokenizerArgs()
     */
    int UNICODE61 = 3;

    @IntDef({SIMPLE, PORTER, UNICODE61})
    @interface Tokenizer {
    }

    @IntDef({FTS3})
    @interface MatchInfo {
    }

    /**
     * Ascending returning order.
     *
     * @see #order()
     */
    int ASC = 0;

    /**
     * Descending returning order.
     *
     * @see #order()
     */
    int DESC = 1;

    @IntDef({ASC, DESC})
    @interface Order {
    }
}
