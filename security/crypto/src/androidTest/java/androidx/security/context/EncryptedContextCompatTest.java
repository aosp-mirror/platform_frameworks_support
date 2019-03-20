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

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.security.EncryptionConfig;
import androidx.security.util.TinkMasterKeyGenerator;
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
import java.security.GeneralSecurityException;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
public class EncryptedContextCompatTest {


    private Context mContext;
    private static final String BIOMETRIC_KEYPAIR_ASYM = "asym_biometric_file_key";
    private static final String BIOMETRIC_KEYPAIR = "biometric_file_key";

    private EncryptionConfig mBiometricEncryptionConfig;
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
        EncryptedContextCompat encryptedContextCompat = new EncryptedContextCompat(mContext);
        try {
            FileOutputStream outputStream = encryptedContextCompat.openEncryptedFileOutput(fileName,
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
        } catch (GeneralSecurityException | IOException ex) {
            ex.printStackTrace();
        }



        //final String STREAMING_AEAD_KEYS = "streaming_aead_keys";

        // Read
        try {
            FileInputStream rawInputStream = mContext.openFileInput(fileName);

            /*TinkMasterKeyGenerator.AsyncAead masterKey = TinkMasterKeyGenerator
            .getOrCreateMasterKey(
            EncryptionConfig.getAES256GCMConfig());
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
                    rawInputStream.getChannel(), fileName.getBytes(TinkMasterKeyGenerator.UTF_8));


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
                    TinkMasterKeyGenerator.UTF_8));*/


            //InputStream stream = streamingAead.newDecryptingStream(rawInputStream,
            //        fileName.getBytes(TinkMasterKeyGenerator.UTF_8));


            byte[] rawCipherBytes = new byte[rawInputStream.available()];
            rawInputStream.read(rawCipherBytes);
            System.out.println("RAW Input Stream: " + new String(rawCipherBytes,
                    TinkMasterKeyGenerator.UTF_8));
            rawInputStream.close();


            FileInputStream inputStream = encryptedContextCompat.openEncryptedFileInput(fileName);
            byte[] rawBytes = new byte[inputStream.available()];
            int status = inputStream.read(rawBytes);
            System.out.println("Status = " + status);
            System.out.println("Decrypted Data: " + new String(rawBytes,
                    TinkMasterKeyGenerator.UTF_8));
            Assert.assertEquals(
                    "Contents should be equal, data was encrypted.",
                    fileContent, new String(rawBytes, "UTF-8"));

        } catch (GeneralSecurityException | IOException ex) {
            ex.printStackTrace();
        }
    }


}

