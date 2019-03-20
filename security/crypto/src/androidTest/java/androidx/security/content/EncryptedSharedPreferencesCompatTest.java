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

package androidx.security.content;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.ArraySet;

import androidx.security.crypto.EncryptedSharedPreferencesKeysets;
import androidx.security.crypto.MasterKey;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Set;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
public class EncryptedSharedPreferencesCompatTest {

    private Context mContext;
    private MasterKey mMasterKey;

    private static final String PREFS_FILE = "test_shared_prefs";

    @Before
    public void setup() {

        mContext = ApplicationProvider.getApplicationContext();

        // Delete all previous keys and shared preferences.

        String filePath = mContext.getFilesDir().getParent() + "/shared_prefs/"
                + EncryptedSharedPreferencesKeysets.KEY_FILE_NAME;
        File deletePrefFile = new File(filePath);
        deletePrefFile.delete();

        filePath = mContext.getFilesDir().getParent() + "/shared_prefs/"
                + EncryptedSharedPreferencesKeysets.VALUE_FILE_NAME;
        deletePrefFile = new File(filePath);
        deletePrefFile.delete();

        SharedPreferences notEncryptedSharedPrefs = mContext.getSharedPreferences(PREFS_FILE,
                Context.MODE_PRIVATE);
        notEncryptedSharedPrefs.edit().clear().commit();

        filePath = mContext.getFilesDir().getParent() + "/shared_prefs/"
                + PREFS_FILE;
        deletePrefFile = new File(filePath);
        deletePrefFile.delete();

        mMasterKey = MasterKey.getOrCreate(MasterKey.MasterKeyEncryptionScheme.AES256_GCM);
    }

    @Test
    public void testWriteSharedPrefs() throws Exception {
        EncryptedSharedPreferencesKeysets keysets = EncryptedSharedPreferencesKeysets.getDefault(
                mContext, mMasterKey,
                EncryptedSharedPreferencesKeysets.KeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferencesKeysets.ValueEncryptionScheme.AES256_GCM);

        SharedPreferences sharedPreferences = EncryptedSharedPreferencesCompat
                .getEncryptedSharedPreferences(PREFS_FILE,
                Context.MODE_PRIVATE, keysets);



        SharedPreferences.Editor editor = sharedPreferences.edit();

        // String Test
        final String stringTestKey = "StringTest";
        final String stringTestValue = "THIS IS A TEST STRING";
        editor.putString(stringTestKey, stringTestValue);

        SharedPreferences.OnSharedPreferenceChangeListener listener =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    @Override
                    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                            String key) {
                        Assert.assertEquals(stringTestValue,
                                sharedPreferences.getString(stringTestKey, null));
                    }
                };

        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);


        // String Set Test
        String stringSetTestKey = "StringSetTest";
        Set<String> stringSetValue = new ArraySet<>();
        stringSetValue.add("Test1");
        stringSetValue.add("Test2");
        editor.putStringSet(stringSetTestKey, stringSetValue);


        // Int Test
        String intTestKey = "IntTest";
        int intTestValue = 1000;
        editor.putInt(intTestKey, intTestValue);

        // Long Test
        String longTestKey = "LongTest";
        long longTestValue = 500L;
        editor.putLong(longTestKey, longTestValue);

        // Boolean Test
        String booleanTestKey = "BooleanTest";
        boolean booleanTestValue = true;
        editor.putBoolean(booleanTestKey, booleanTestValue);

        // Float Test
        String floatTestKey = "FloatTest";
        float floatTestValue = 250.5f;
        editor.putFloat(floatTestKey, floatTestValue);

        editor.commit();

        // String Test Assertion
        Assert.assertEquals(stringTestKey + " has the wrong value",
                stringTestValue,
                sharedPreferences.getString(stringTestKey, null));

        // StringSet Test Assertion
        Set<String> stringSetPrefsValue = sharedPreferences.getStringSet(stringSetTestKey, null);
        String stringSetTestValue = null;
        if (!stringSetPrefsValue.isEmpty()) {
            stringSetTestValue = stringSetPrefsValue.iterator().next();
        }
        Assert.assertEquals(stringSetTestKey + " has the wrong value",
                ((ArraySet<String>) stringSetValue).valueAt(0),
                stringSetTestValue);

        // Int Test Assertion
        Assert.assertEquals(intTestKey + " has the wrong value",
                intTestValue,
                sharedPreferences.getInt(intTestKey, 0));

        // Long Test Assertion
        Assert.assertEquals(longTestKey + " has the wrong value",
                longTestValue,
                sharedPreferences.getLong(longTestKey, 0L));

        // Boolean Test Assertion
        Assert.assertEquals(booleanTestKey + " has the wrong value",
                booleanTestValue,
                sharedPreferences.getBoolean(booleanTestKey, false));

        // Float Test Assertion
        Assert.assertEquals(floatTestValue,
                sharedPreferences.getFloat(floatTestKey, 0.0f),
                0.0f);


    }

}
