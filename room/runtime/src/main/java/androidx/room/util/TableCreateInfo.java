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

package androidx.room.util;

import android.database.Cursor;

import androidx.annotation.RestrictTo;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * A data class that holds the SQL create statement for a given table.
 * <p>
 * The create statement maps directly to the statement obtained from the {@code sqlite_master}
 * table.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TableCreateInfo {

    /**
     * The table name
     */
    public final String name;

    /**
     * The create SQL statement
     */
    public final String sql;

    public TableCreateInfo(String name, String sql) {
        this.name = name;
        this.sql = sql;
    }

    /**
     * Reads the table create statement from the given database.
     *
     * @param database  The database to read the information from.
     * @param tableName The table name.
     * @return A TableCreateInfo containing the create statement for the provided table name.
     */
    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public static TableCreateInfo read(SupportSQLiteDatabase database, String tableName) {
        String sql = "";
        Cursor cursor = database.query(
                "SELECT * FROM sqlite_master WHERE `name` = '" + tableName + "'");
        try {
            if (cursor.moveToFirst()) {
                sql = cursor.getString(cursor.getColumnIndexOrThrow("sql"));
            }
        } finally {
            cursor.close();
        }
        return new TableCreateInfo(tableName, sql);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TableCreateInfo that = (TableCreateInfo) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return sql != null ? isCreateSqlEqual(sql, that.sql) : that.sql == null;
    }

    private static boolean isCreateSqlEqual(String sql1, String sql2) {
        // "IF NOT EXISTS" is processed before the statement is inserted into sqlite_master.
        // Therefore it we strip it out so its not considered as part of the equality check.
        // See: https://www.mail-archive.com/sqlite-users@mailinglists.sqlite.org/msg111639.html
        String strippedSql1 = sql1.replace("IF NOT EXISTS ", "");
        String strippedSql2 = sql2.replace("IF NOT EXISTS ", "");

        return strippedSql1.equals(strippedSql2);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (sql != null ? sql.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TableCreateInfo{"
                + "name='" + name + '\''
                + ", sql='" + sql + '\''
                + '}';
    }
}
