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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
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
import java.nio.charset.Charset;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
public class EncryptedFileTest {


    private Context mContext;

    private MasterKey mMasterKey;

    private FragmentActivity mActivity;

    private static final Charset UTF_8 = Charset.forName("UTF-8");

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

        SharedPreferences sharedPreferences = mContext.getSharedPreferences(
                EncryptedFileKeyset.FILE_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().commit();


        // Delete old keys for testing
        String filePath = mContext.getFilesDir().getParent() + "/shared_prefs/"
                + EncryptedFileKeyset.FILE_NAME;
        File deletePrefFile = new File(filePath);
        deletePrefFile.delete();

        filePath = mContext.getFilesDir().getParent() + "nothing_to_see_here";
        deletePrefFile = new File(filePath);
        deletePrefFile.delete();

        mMasterKey = MasterKey.getOrCreate(MasterKey.MasterKeyEncryptionScheme.AES256_GCM);

    }

    @Test
    public void testWriteReadEncryptedFile() throws Exception {
        final String fileContent = "Don't tell anyone...";
        final String fileName = "nothing_to_see_here";

        // Write
        EncryptedFileKeyset fileKeyset = EncryptedFileKeyset.getOrCreate(mContext, mMasterKey,
                EncryptedFileKeyset.FileEncryptionScheme.AES256_GCM_HKDF_4KB);

        FileOutputStream outputStream = EncryptedFile.openEncryptedFileOutput(fileName,
                Context.MODE_PRIVATE, fileKeyset);
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
                UTF_8));
        rawStream.close();


        FileInputStream inputStream = EncryptedFile.openEncryptedFileInput(fileName,
                fileKeyset);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int nextByte = inputStream.read();
        while (nextByte != -1) {
            byteArrayOutputStream.write(nextByte);
            nextByte = inputStream.read();
        }

        byte[] plainText = byteArrayOutputStream.toByteArray();

        System.out.println("Decrypted Data: " + new String(plainText,
                UTF_8));

        Assert.assertEquals(
                "Contents should be equal, data was encrypted.",
                fileContent, new String(plainText, "UTF-8"));
        inputStream.close();
    }


}

