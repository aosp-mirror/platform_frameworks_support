/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.database.AbstractWindowedCursor;
import android.database.Cursor;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.HashMap;

/**
 * Database utilities for Room
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DBUtil {

    /**
     * Performs the SQLiteQuery on the given database.
     * <p>
     * This util method encapsulates copying the cursor if the {@code maybeCopy} parameter is
     * {@code true} and either the api level is below a certain threshold or the full result of the
     * query does not fit in a single window.
     *
     * @param db          The database to perform the query on.
     * @param sqLiteQuery The query to perform.
     * @param maybeCopy   True if the result cursor should maybe be copied, false otherwise.
     * @return Result of the query.
     */
    @NonNull
    public static Cursor query(RoomDatabase db, SupportSQLiteQuery sqLiteQuery, boolean maybeCopy) {
        final Cursor cursor = db.query(sqLiteQuery);
        if (maybeCopy && cursor instanceof AbstractWindowedCursor) {
            AbstractWindowedCursor windowedCursor = (AbstractWindowedCursor) cursor;
            int rowsInCursor = windowedCursor.getCount(); // Should fill the window.
            int rowsInWindow;
            if (windowedCursor.hasWindow()) {
                rowsInWindow = windowedCursor.getWindow().getNumRows();
            } else {
                rowsInWindow = rowsInCursor;
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || rowsInWindow < rowsInCursor) {
                return CursorUtil.copyAndClose(windowedCursor);
            }
        }

        return cursor;
    }

    /**
     * Synchronizes the database views. After calling this method, the database will have all the
     * specified views and no other views.
     *
     * @param db    The database.
     * @param views The array of views. Each view is an array of its name and CREATE VIEW SQL. The
     *              views are created or updated in the order as they are stored in this array.
     */
    public static void syncViews(SupportSQLiteDatabase db, String[][] views) {
        Cursor cursor = db.query("SELECT name, sql FROM sqlite_master WHERE type = 'view'");
        HashMap<String, String> existingViews = new HashMap<>();
        //noinspection TryFinallyCanBeTryWithResources
        try {
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                String sql = cursor.getString(1);
                existingViews.put(name, sql);
            }
        } finally {
            cursor.close();
        }
        for (String[] view : views) {
            String name = view[0];
            String sql = view[1];
            if (existingViews.containsKey(name)) {
                String existingSql = existingViews.get(name);
                existingViews.remove(name);
                if (existingSql.equals(sql)) {
                    continue;
                } else {
                    db.execSQL("DROP VIEW `" + name + "`");
                }
            }
            db.execSQL(sql);
        }
        for (String name : existingViews.keySet()) {
            db.execSQL("DROP VIEW `" + name + "`");
        }
    }

    private DBUtil() {
    }
}
