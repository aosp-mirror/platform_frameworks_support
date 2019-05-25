/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.integration.testapp.dao.ProductDao;
import androidx.room.integration.testapp.vo.Product;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class PrepackageTest {

    @Test
    public void createFromAsset() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("products.db");
        ProductsDatabase database = Room.databaseBuilder(
                context, ProductsDatabase.class, "products.db")
                .createFromAsset()
                .build();

        ProductDao dao = database.getProductDao();
        assertThat(dao.countProducts(), is(2));
    }

    @Test
    public void createFromAsset_badSchema() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("products_badSchema.db");
        ProductsDatabase database = Room.databaseBuilder(
                context, ProductsDatabase.class, "products_badSchema.db")
                .createFromAsset()
                .build();

        Throwable throwable = null;
        try {
            database.getProductDao().countProducts();
            fail("Opening database should fail due to bad schema.");
        } catch (Throwable t) {
            throwable = t;
        }
        assertThat(throwable, instanceOf(IllegalStateException.class));
        assertThat(throwable.getMessage(), containsString("Migration didn't properly handle"));
    }

    @Test
    public void createFromAsset_notFound() {
        Context context = ApplicationProvider.getApplicationContext();
        ProductsDatabase database = Room.databaseBuilder(
                context, ProductsDatabase.class, "products_notFound.db")
                .createFromAsset()
                .build();

        Throwable throwable = null;
        try {
            database.getProductDao().countProducts();
            fail("Opening database should fail due to asset file not found.");
        } catch (Throwable t) {
            throwable = t;
        }
        assertThat(throwable, instanceOf(RuntimeException.class));
        assertThat(throwable.getCause(), instanceOf(FileNotFoundException.class));
    }

    @Test
    public void createFromAsset_versionZero() {
        // A 0 version DB goes through the create path because SQLiteOpenHelper thinks the opened
        // DB was created from scratch. Therefore our onCreate callbacks will be called and we need
        // to validate the schema before completing opening the DB.
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("products_v0.db");
        ProductsDatabase database = Room.databaseBuilder(
                context, ProductsDatabase.class, "products_v0.db")
                .createFromAsset()
                .build();

        ProductDao dao = database.getProductDao();
        assertThat(dao.countProducts(), is(2));
    }

    @Test
    public void createFromAsset_versionZero_badSchema() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("products_v0_badSchema.db");
        ProductsDatabase database = Room.databaseBuilder(
                context, ProductsDatabase.class, "products_v0_badSchema.db")
                .createFromAsset()
                .build();

        Throwable throwable = null;
        try {
            database.getProductDao().countProducts();
            fail("Opening database should fail due to bad schema.");
        } catch (Throwable t) {
            throwable = t;
        }
        assertThat(throwable, instanceOf(IllegalStateException.class));
        assertThat(throwable.getMessage(), containsString("Migration didn't properly handle"));
    }

    @Test
    public void createFromAsset_closeAndReOpen() {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("products.db");
        ProductsDatabase database;

        database = Room.databaseBuilder(
                context, ProductsDatabase.class, "products.db")
                .createFromAsset()
                .build();
        assertThat(database.getProductDao().countProducts(), is(2));

        database.close();

        database = Room.databaseBuilder(
                context, ProductsDatabase.class, "products.db")
                .createFromAsset()
                .build();
        assertThat(database.getProductDao().countProducts(), is(2));
    }

    @Test
    public void createFromAsset_badDatabaseFile() {
        // A bad database file is a 'corrupted' database, it'll get deleted and a new file will be
        // created, the usual corrupted db recovery process.
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("products_badFile.db");
        ProductsDatabase database = Room.databaseBuilder(
                context, ProductsDatabase.class, "products_badFile.db")
                .createFromAsset()
                .build();

        ProductDao dao = database.getProductDao();
        assertThat(dao.countProducts(), is(0));
    }

    @Test
    public void createFromFile() throws IOException {
        Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase("products_external.db");

        // Copy from file from assets to data dir
        File dataDbFile = new File(context.getDataDir(), "products_external.db");
        InputStream toCopyInput = context.getAssets().open("databases/products.db");
        Files.copy(toCopyInput, dataDbFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        ProductsDatabase database = Room.databaseBuilder(
                context, ProductsDatabase.class, "products_external.db")
                .createFromFile(dataDbFile.getAbsolutePath())
                .build();

        ProductDao dao = database.getProductDao();
        assertThat(dao.countProducts(), is(2));
    }

    @Database(entities = Product.class, version = 1, exportSchema = false)
    abstract static class ProductsDatabase extends RoomDatabase {
        abstract ProductDao getProductDao();
    }
}
