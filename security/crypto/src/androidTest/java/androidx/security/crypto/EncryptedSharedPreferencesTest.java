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

package androidx.security.crypto;

import static android.content.Context.MODE_PRIVATE;

import static androidx.security.crypto.MasterKeys.KEYSTORE_PATH_URI;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.ArraySet;

import androidx.test.core.app.ApplicationProvider;

import com.google.crypto.tink.DeterministicAead;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.config.TinkConfig;
import com.google.crypto.tink.daead.DeterministicAeadFactory;
import com.google.crypto.tink.daead.DeterministicAeadKeyTemplates;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.crypto.tink.subtle.Base64;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.security.KeyStore;
import java.util.Set;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
public class EncryptedSharedPreferencesTest {

    private Context mContext;
    private String mKeyAlias;

    private static final String PREFS_FILE = "test_shared_prefs";

    @Before
    public void setup() throws Exception {

        mContext = ApplicationProvider.getApplicationContext();

        // Delete all previous keys and shared preferences.

        String filePath = mContext.getFilesDir().getParent() + "/shared_prefs/"
                + "__androidx_security__crypto_encrypted_prefs__";
        File deletePrefFile = new File(filePath);
        deletePrefFile.delete();

        SharedPreferences notEncryptedSharedPrefs = mContext.getSharedPreferences(PREFS_FILE,
                MODE_PRIVATE);
        notEncryptedSharedPrefs.edit().clear().commit();

        filePath = mContext.getFilesDir().getParent() + "/shared_prefs/"
                + PREFS_FILE;
        deletePrefFile = new File(filePath);
        deletePrefFile.delete();

        SharedPreferences encryptedSharedPrefs = mContext.getSharedPreferences("TinkTestPrefs",
                MODE_PRIVATE);
        encryptedSharedPrefs.edit().clear().commit();

        filePath = mContext.getFilesDir().getParent() + "/shared_prefs/"
                + "TinkTestPrefs";
        deletePrefFile = new File(filePath);
        deletePrefFile.delete();

        // Delete MasterKeys
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        keyStore.deleteEntry("_androidx_security_master_key_");

        mKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
    }

    @Test
    public void testWriteSharedPrefs() throws Exception {

        SharedPreferences sharedPreferences = EncryptedSharedPreferences
                .create(PREFS_FILE,
                        mKeyAlias, mContext,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);

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

        // Null Key Test
        String nullKey = null;
        String nullStringValue = "NULL_KEY";
        editor.putString(nullKey, nullStringValue);

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

        // Null Key Test Assertion
        Assert.assertEquals(nullKey + " has the wrong value",
                nullStringValue,
                sharedPreferences.getString(nullKey, null));

        Assert.assertTrue(nullKey + " should exist", sharedPreferences.contains(nullKey));

        // Test Remove
        editor.remove(nullKey);
        editor.commit();

        Assert.assertEquals(nullKey + " should have been removed.",
                null,
                sharedPreferences.getString(nullKey, null));

        Assert.assertFalse(nullKey + " should not exist",
                sharedPreferences.contains(nullKey));

        // Null String Key and value Test Assertion
        editor.putString(null, null);
        editor.putStringSet(null, null);
        editor.commit();
        Assert.assertEquals(null + " should not have a value",
                null,
                sharedPreferences.getString(null, null));

        // Null StringSet Key and value Test Assertion

        Assert.assertEquals(null + " should not have a value",
                null,
                sharedPreferences.getStringSet(null, null));

        // Test overwriting keys
        String twiceKey = "KeyTwice";
        String twiceVal1 = "FirstVal";
        String twiceVal2 = "SecondVal";
        editor.putString(twiceKey, twiceVal1);
        editor.commit();

        Assert.assertEquals(twiceVal1 + " should be the value",
                twiceVal1,
                sharedPreferences.getString(twiceKey, null));

        editor.putString(twiceKey, twiceVal2);
        editor.commit();

        Assert.assertEquals(twiceVal2 + " should be the value",
                twiceVal2,
                sharedPreferences.getString(twiceKey, null));
    }

    @Test
    public void testWriteSharedPrefsTink() throws Exception {
        String tinkTestPrefs = "TinkTestPrefs";
        String testKey = "TestKey";
        String testValue = "TestValue";

        SharedPreferences encryptedSharedPreferences = EncryptedSharedPreferences
                .create(tinkTestPrefs,
                        mKeyAlias, mContext,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);

        SharedPreferences.Editor encryptedEditor = encryptedSharedPreferences.edit();
        encryptedEditor.putString(testKey, testValue);
        encryptedEditor.commit();

        // Set up Tink
        TinkConfig.register();

        KeysetHandle daeadKeysetHandle = new AndroidKeysetManager.Builder()
                .withKeyTemplate(DeterministicAeadKeyTemplates.AES256_SIV)
                .withSharedPref(mContext,
                        "__androidx_security_crypto_encrypted_prefs_key_keyset__", tinkTestPrefs)
                .withMasterKeyUri(KEYSTORE_PATH_URI + "_androidx_security_master_key_")
                .build().getKeysetHandle();

        DeterministicAead deterministicAead = DeterministicAeadFactory.getPrimitive(
                daeadKeysetHandle);
        byte[] encryptedKey = deterministicAead.encryptDeterministically(testKey.getBytes(UTF_8),
                tinkTestPrefs.getBytes());
        String encodedKey = Base64.encode(encryptedKey);
        SharedPreferences  sharedPreferences = mContext.getSharedPreferences(tinkTestPrefs,
                MODE_PRIVATE);

        boolean keyExists = sharedPreferences.contains(encodedKey);
        Assert.assertTrue("Key should exist if Tink is compatible.", keyExists);



    }

}
