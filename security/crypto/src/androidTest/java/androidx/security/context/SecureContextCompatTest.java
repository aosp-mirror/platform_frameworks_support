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

package androidx.security.context;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.security.SecureConfig;
import androidx.security.biometric.BiometricKeyAuth;
import androidx.security.biometric.BiometricKeyAuthCallback;
import androidx.security.crypto.SecureKeyGenerator;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.robolectric.Robolectric;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
public class SecureContextCompatTest {


    private Context mContext;
    private static final String KEYPAIR = "file_key";
    private static final String BIOMETRIC_KEYPAIR = "biometric_file_key";

    private SecureConfig mNiapSecureConfig;
    private FragmentActivity mActivity;

    private static class BiometricActivity extends FragmentActivity {

        @Override
        protected void onCreate(@Nullable Bundle bundle) {
            super.onCreate(bundle);

        }

    }

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        SecureKeyGenerator keyGenerator = SecureKeyGenerator.getInstance(SecureConfig.getDefault());
        keyGenerator.generateAsymmetricKeyPair(KEYPAIR);
        mActivity = Robolectric.setupActivity(BiometricActivity.class);
        BiometricKeyAuth keyAuth = new BiometricKeyAuth(mActivity,
                new BiometricKeyAuthCallback() {
                    @Override
                    public void onAuthenticationSucceeded() {
                        System.out.println("Auth Success!");
                    }

                    @Override
                    public void onAuthenticationError(int errorCode,
                            @NonNull CharSequence errString) {
                        System.out.println("Auth Error! " + errorCode + errString);
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        System.out.println("Auth Failed!");
                    }

                    @Override
                    public void onMessage(@NonNull String message) {
                        System.out.println("Auth Message " + message);
                    }
                });
        mNiapSecureConfig = SecureConfig.getNiapConfig(keyAuth);
        SecureKeyGenerator niapKeyGenerator = SecureKeyGenerator.getInstance(mNiapSecureConfig);
        niapKeyGenerator.generateKey(BIOMETRIC_KEYPAIR);
    }

    @Test
    public void testWriteEncryptedFile() {
        String fileContent = "SOME TEST DATA!";
        String fileName = "test_file";

        SecureContextCompat secureContextCompat = new SecureContextCompat(mContext);
        try {
            FileOutputStream outputStream = secureContextCompat.openEncryptedFileOutput(fileName,
                    Context.MODE_PRIVATE, KEYPAIR);
            outputStream.write(fileContent.getBytes("UTF-8"));
            outputStream.flush();
            outputStream.close();

            FileInputStream fileInputStream = mContext.openFileInput(fileName);
            byte[] rawBytes = new byte[fileInputStream.available()];
            fileInputStream.read(rawBytes);
            Assert.assertNotEquals("Contents should differ, data was not encrypted.",
                    fileContent, new String(rawBytes, "UTF-8"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void testReadEncryptedFile() {
        final String fileContent = "SOME TEST DATA!";
        final String fileName = "test_file";
        SecureContextCompat secureContextCompat = new SecureContextCompat(mContext);
        try {
            secureContextCompat.openEncryptedFileInput(fileName,
                    null,
                    new SecureContextCompat.EncryptedFileInputStreamListener() {
                        @Override
                        public void onEncryptedFileInput(@NonNull FileInputStream inputStream) {
                            try {
                                byte[] rawBytes = new byte[inputStream.available()];
                                inputStream.read(rawBytes);
                                Assert.assertNotEquals(
                                        "Contents should be equal, data was encrypted.",
                                        fileContent, new String(rawBytes, "UTF-8"));
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void testReadEncryptedFileWithBiometric() {
        final String fileContent = "SOME TEST DATA!";
        final String fileName = "test_file";
        SecureContextCompat secureContextCompat = new SecureContextCompat(mContext,
                mNiapSecureConfig);
        try {
            secureContextCompat.openEncryptedFileInput(fileName,
                    null,
                    new SecureContextCompat.EncryptedFileInputStreamListener() {
                        @Override
                        public void onEncryptedFileInput(@NonNull FileInputStream inputStream) {
                            try {
                                byte[] rawBytes = new byte[inputStream.available()];
                                inputStream.read(rawBytes);
                                Assert.assertNotEquals(
                                        "Contents should be equal, data was encrypted.",
                                        fileContent, new String(rawBytes, "UTF-8"));
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void testWriteSharedPrefs() {
        String prefs = "api_keys";
        String key = "maps_api_key";
        String value = "ASDASDASDAADSADSADSADSADASDASDASDSADSAS";
        String keyName = "shared_prefs_key";

        SecureContextCompat secureContextCompat = new SecureContextCompat(mContext);
        SharedPreferences sharedPreferences =
                secureContextCompat.getSharedPreferences(prefs, Context.MODE_PRIVATE, keyName);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();

        String apiKey = sharedPreferences.getString(key, null);
        Assert.assertNotNull("Object should not be null", apiKey);


    }

}

