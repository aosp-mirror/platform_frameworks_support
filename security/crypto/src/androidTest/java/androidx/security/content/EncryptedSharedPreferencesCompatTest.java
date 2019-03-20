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

    @Before
    public void setup() {
        String prefs = "api_keys";
        mContext = ApplicationProvider.getApplicationContext();

        String filePath = mContext.getFilesDir().getParent() + "/shared_prefs/"
                + EncryptedSharedPreferencesKeysets.KEY_FILE_NAME;
        File deletePrefFile = new File(filePath);
        deletePrefFile.delete();

        filePath = mContext.getFilesDir().getParent() + "/shared_prefs/"
                + EncryptedSharedPreferencesKeysets.VALUE_FILE_NAME;
        deletePrefFile = new File(filePath);
        deletePrefFile.delete();

        SharedPreferences notEncryptedSharedPrefs = mContext.getSharedPreferences(prefs,
                Context.MODE_PRIVATE);
        notEncryptedSharedPrefs.edit().clear().commit();

        filePath = mContext.getFilesDir().getParent() + "/shared_prefs/"
                + prefs;
        deletePrefFile = new File(filePath);
        deletePrefFile.delete();

        mMasterKey = MasterKey.getOrCreate(MasterKey.MasterKeyEncryptionScheme.AES256_GCM);
    }

    @Test
    public void testWriteSharedPrefs() throws Exception {
        String prefs = "api_keys";
        String key = "maps_api_key";
        String value = "ASDASDASDAADSADSADSADSADASDASDASDSADSAS";
        String keyName = "shared_prefs_key";

        EncryptedSharedPreferencesKeysets keysets = EncryptedSharedPreferencesKeysets.getDefault(
                mContext, mMasterKey,
                EncryptedSharedPreferencesKeysets.KeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferencesKeysets.ValueEncryptionScheme.AES256_GCM);

        SharedPreferences sharedPreferences = new EncryptedSharedPreferencesCompat(prefs,
                Context.MODE_PRIVATE, mMasterKey, keysets, mContext);

        Set<String> s = new ArraySet<>();
        s.add("Test1");
        s.add("Test2");

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.putInt("IntTest", 25);
        editor.putBoolean("BoolTest", true);
        editor.putLong("LongTest", 100L);
        editor.putStringSet("StringsSetTest", s);
        editor.putFloat("FloatTest", 20.5f);
        editor.commit();

        SharedPreferences notEncryptedSharedPrefs = mContext.getSharedPreferences(prefs,
                Context.MODE_PRIVATE);

        System.out.println("Non Encrypted Key Value: "
                + notEncryptedSharedPrefs.getString(key, null));
        System.out.println("All UnEncrypted Data: " + notEncryptedSharedPrefs.getAll().toString());

        String apiKey = sharedPreferences.getString(key, null);

        System.out.println(apiKey);

        System.out.println("All values " + sharedPreferences.getAll().toString());

        Assert.assertNotNull("Object should not be null", apiKey);


    }

}
