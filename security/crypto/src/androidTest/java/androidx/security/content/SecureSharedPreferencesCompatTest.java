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

import androidx.fragment.app.FragmentActivity;
import androidx.security.SecureConfig;
import androidx.security.context.SecureContextCompat;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
public class SecureSharedPreferencesCompatTest {

    private Context mContext;
    private static final String KEYPAIR = "file_key";
    private static final String BIOMETRIC_KEYPAIR_ASYM = "asym_biometric_file_key";
    private static final String BIOMETRIC_KEYPAIR = "biometric_file_key";

    private SecureConfig mBiometricSecureConfig;
    private FragmentActivity mActivity;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("api_keys",
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();
    }

    @Test
    public void testWriteSharedPrefs() {
        String prefs = "api_keys";
        String key = "maps_api_key";
        String value = "ASDASDASDAADSADSADSADSADASDASDASDSADSAS";
        String keyName = "shared_prefs_key";

        // Commenting out now, since this won't work until Tink can accept an Aead instead of a Uri
        SecureContextCompat secureContextCompat = new SecureContextCompat(mContext);
        SharedPreferences sharedPreferences =
                secureContextCompat.getSharedPreferences(prefs, Context.MODE_PRIVATE, keyName);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();

        SharedPreferences notEncryptedSharedPrefs = mContext.getSharedPreferences(prefs,
                Context.MODE_PRIVATE);

        System.out.println("Non Encrypted Key Value: "
                + notEncryptedSharedPrefs.getString(key, null));
        System.out.println("All UnEncrypted Data: " + notEncryptedSharedPrefs.getAll().toString());

        String apiKey = sharedPreferences.getString(key, null);

        System.out.println(apiKey);

        Assert.assertNotNull("Object should not be null", apiKey);


    }

}
