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
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.security.SecureConfig;
import androidx.security.biometric.BiometricKeyAuth;
import androidx.security.biometric.BiometricKeyAuthCallback;
import androidx.security.crypto.SecureKeyGenerator;
import androidx.security.util.TinkUtil;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
public class SecureContextCompatTest {


    private Context mContext;
    private static final String BIOMETRIC_KEYPAIR_ASYM = "asym_biometric_file_key";
    private static final String BIOMETRIC_KEYPAIR = "biometric_file_key";

    private SecureConfig mBiometricSecureConfig;
    private FragmentActivity mActivity;

    //@Rule
    public ActivityScenario<BiometricActivity> mActivityScenario;

    public static class BiometricActivity extends FragmentActivity {

        FragmentActivity mFragmentActivity;

        @Override
        protected void onCreate(@Nullable Bundle bundle) {
            super.onCreate(bundle);
            mFragmentActivity = this;
            System.out.println("Constructor...");
        }

        @Override
        protected void onResume() {
            super.onResume();
            System.out.println("Lets do some crypto stuff..");
        }


    }

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testWriteReadEncryptedFile() {
        final String fileContent = "SOME TEST DATA!";
        final String fileName = "test_file";

        // Write
        SecureContextCompat secureContextCompat = new SecureContextCompat(mContext);
        try {
            FileOutputStream outputStream = secureContextCompat.openEncryptedFileOutput(fileName,
                    Context.MODE_PRIVATE);
            outputStream.write(fileContent.getBytes("UTF-8"));
            outputStream.flush();
            outputStream.close();

            FileInputStream fileInputStream = mContext.openFileInput(fileName);
            byte[] rawBytes = new byte[fileInputStream.available()];
            fileInputStream.read(rawBytes);
            Assert.assertNotEquals("Contents should differ, data was not encrypted.",
                    fileContent, new String(rawBytes, "UTF-8"));
            fileInputStream.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }



        //final String STREAMING_AEAD_KEYS = "streaming_aead_keys";

        // Read
        try {
            FileInputStream rawInputStream = mContext.openFileInput(fileName);

            /*TinkUtil.AsyncAead masterKey = TinkUtil.getOrCreateMasterKey(
            SecureConfig.getDefault());
            TinkConfig.register();
            TinkAndroidKeysetManager streamingAaeadKeysetManager =
                    new TinkAndroidKeysetManager.Builder()
                            .withKeyTemplate(StreamingAeadKeyTemplates.AES256_CTR_HMAC_SHA256_4KB)
                            .withSharedPref(mContext, STREAMING_AEAD_KEYS,
                                    STREAMING_AEAD_KEYS + "_file")
                            .withMasterKey(masterKey)
                            .build();

            KeysetHandle keysetHandle = streamingAaeadKeysetManager.getKeysetHandle();
            StreamingAead streamingAead = StreamingAeadFactory.getPrimitive(keysetHandle);

            ReadableByteChannel readableByteChannel = streamingAead.newDecryptingChannel(
                    rawInputStream.getChannel(), fileName.getBytes(TinkUtil.UTF_8));


            ByteBuffer buffer = ByteBuffer.allocate(200);
            buffer.clear();
            int cnt = readableByteChannel.read(buffer);
            if (cnt > 0) {
                System.out.println("Bytes processed...." + cnt);
            } else if (cnt == -1) {
                System.out.println("End of plaintext detected.");
            } else if (cnt == 0) {
                System.out.println("No ciphertext available.");
            }
            System.out.println("Raw bytes from Buffer " + new String(buffer.array(),
                    TinkUtil.UTF_8));*/


            //InputStream stream = streamingAead.newDecryptingStream(rawInputStream,
            //        fileName.getBytes(TinkUtil.UTF_8));


            byte[] rawCipherBytes = new byte[rawInputStream.available()];
            rawInputStream.read(rawCipherBytes);
            System.out.println("RAW Input Stream: " + new String(rawCipherBytes, TinkUtil.UTF_8));
            rawInputStream.close();


            FileInputStream inputStream = secureContextCompat.openEncryptedFileInput(fileName);
            byte[] rawBytes = new byte[inputStream.available()];
            int status = inputStream.read(rawBytes);
            System.out.println("Status = " + status);
            System.out.println("Decrypted Data: " + new String(rawBytes, TinkUtil.UTF_8));
            Assert.assertEquals(
                    "Contents should be equal, data was encrypted.",
                    fileContent, new String(rawBytes, "UTF-8"));

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    // This test has issues with thread blocking still, seems like the test Activity doesn't
    // handle threading executors properly.
    //@Test
    public void testWriteReadEncryptedFileWithBiometric() {
        String fileContent = "SOME TEST DATA!";
        String fileName = "test_file_bio";

        mBiometricSecureConfig = SecureConfig.getBiometricConfig(null);
        SecureContextCompat secureContextCompat = new SecureContextCompat(mContext,
                mBiometricSecureConfig);

        SecureKeyGenerator biometricKeyGenerator = SecureKeyGenerator
                .getInstance(mBiometricSecureConfig);
        biometricKeyGenerator.generateKey(BIOMETRIC_KEYPAIR);
        biometricKeyGenerator.generateAsymmetricKeyPair(BIOMETRIC_KEYPAIR_ASYM);
        try {
            FileOutputStream outputStream = secureContextCompat.openEncryptedFileOutput(fileName,
                    Context.MODE_PRIVATE);
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

        ActivityScenario.launch(BiometricActivity.class).onActivity(
                new ActivityScenario.ActivityAction<BiometricActivity>() {
                    @Override
                    public void perform(BiometricActivity activity) {
                        System.out.println("In perform.....!!!");
                        BiometricKeyAuth keyAuth = new BiometricKeyAuth(activity,
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
                        mBiometricSecureConfig = SecureConfig.getBiometricConfig(keyAuth);


                        final String fileContent = "SOME TEST DATA!";
                        final String fileName = "test_file_bio";
                        SecureContextCompat secureContextCompat = new SecureContextCompat(mContext,
                                mBiometricSecureConfig);
                        try {
                            FileInputStream inputStream = secureContextCompat
                                    .openEncryptedFileInput(fileName);
                            byte[] rawBytes = new byte[inputStream.available()];
                            inputStream.read(rawBytes);
                            Assert.assertNotEquals(
                                    "Contents should be equal, data was "
                                            + "encrypted.",
                                    fileContent,
                                    new String(rawBytes, "UTF-8"));



                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                });

    }

}

