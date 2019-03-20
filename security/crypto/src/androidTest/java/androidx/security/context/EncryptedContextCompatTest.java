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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
public class EncryptedContextCompatTest {


    private Context mContext;

    private FragmentActivity mActivity;

    private EncryptionConfig mEncryptionConfig;

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
    public void setup() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
        mEncryptionConfig = EncryptionConfig.getAES256GCMConfig();

        SharedPreferences sharedPreferences = mContext.getSharedPreferences(
                EncryptionConfig.ENCRYPTED_FILE_KEYSET, Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().commit();


        // Delete old keys for testing
        String filePath = mContext.getFilesDir().getParent() + "/shared_prefs/"
                + EncryptionConfig.ENCRYPTED_FILE_KEYSET;
        File deletePrefFile = new File(filePath);
        deletePrefFile.delete();

        filePath = mContext.getFilesDir().getParent() + "such_secret_wow";
        deletePrefFile = new File(filePath);
        deletePrefFile.delete();

        // Write over existing master key.
        // Delete previous keys
        //AeadMasterKeyGenerator aeadMasterKeyGenerator = AeadMasterKeyGenerator
        //        .getInstance(mEncryptionConfig);
        //aeadMasterKeyGenerator.generateKey(EncryptionConfig.MASTER_KEY);

        //AeadCipher cipher = AeadCipher.getInstance(mEncryptionConfig, mContext);
        //cipher.deleteKeys(mContext);

        //cipher.rotateKeys(mContext);

    }

    @Test
    public void testWriteReadEncryptedFile() throws Exception {
        final String fileContent = "Don't tell anyone... Seriously... please don't tell anyone!";
        final String fileName = "such_secret_wow";

        // Write
        EncryptedContextCompat encryptedContextCompat = new EncryptedContextCompat(mContext,
                mEncryptionConfig);

        FileOutputStream outputStream = encryptedContextCompat.openEncryptedFileOutput(fileName,
                Context.MODE_PRIVATE);
        outputStream.write(fileContent.getBytes("UTF-8"));
        outputStream.flush();
        outputStream.close();

        FileInputStream rawStream = mContext.openFileInput(fileName);
        ByteArrayOutputStream rawByteArrayOutputStream = new ByteArrayOutputStream();
        int rawNextByte = rawStream.read();
        while (rawNextByte != -1) {
            rawByteArrayOutputStream.write(rawNextByte);
            rawNextByte = rawStream.read();
        }
        byte[] rawCipherText = rawByteArrayOutputStream.toByteArray();
        System.out.println("Raw CipherText = " + new String(rawCipherText,
                TinkMasterKeyGenerator.UTF_8));
        rawStream.close();


        FileInputStream inputStream = encryptedContextCompat.openEncryptedFileInput(fileName);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int nextByte = inputStream.read();
        while (nextByte != -1) {
            byteArrayOutputStream.write(nextByte);
            nextByte = inputStream.read();
        }

        byte[] plainText = byteArrayOutputStream.toByteArray();

        System.out.println("Decrypted Data: " + new String(plainText,
                TinkMasterKeyGenerator.UTF_8));

        Assert.assertEquals(
                "Contents should be equal, data was encrypted.",
                fileContent, new String(plainText, "UTF-8"));
        inputStream.close();
    }


}

