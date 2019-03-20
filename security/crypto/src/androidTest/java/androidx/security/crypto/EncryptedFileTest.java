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

import static androidx.security.crypto.EncryptedFile.FILE_NAME;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
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
import java.security.KeyStore;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
public class EncryptedFileTest {


    private Context mContext;
    private MasterKey mMasterKey;
    private MasterKey mLockedKey;

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    public class KeyUnlockActivity extends FragmentActivity {

        public KeyUnlockActivity() {
            super();
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            if (resultCode == RESULT_OK) {
                try {
                    lockedWriteReadEncryptedFile();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Before
    public void setup() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();

        SharedPreferences sharedPreferences = mContext.getSharedPreferences(
                FILE_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().commit();


        // Delete old keys for testing
        String filePath = mContext.getFilesDir().getParent() + "/shared_prefs/"
                + FILE_NAME;
        File deletePrefFile = new File(filePath);
        deletePrefFile.delete();

        filePath = mContext.getFilesDir().getParent() + "nothing_to_see_here";
        deletePrefFile = new File(filePath);
        deletePrefFile.delete();

        // Delete MasterKey
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        keyStore.deleteEntry(MasterKey.MASTER_KEY_ALIAS);


        KeyGenParameterSpec.Builder lockedKeyBuilder = new KeyGenParameterSpec.Builder(
                MasterKey.MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(10)
                .setUnlockedDeviceRequired(true)
                .setKeySize(256);
        mLockedKey = MasterKey.getOrCreate(lockedKeyBuilder.build());
        mMasterKey = MasterKey.getOrCreate(MasterKey.createAES256GCMKeyGenParameterSpec());
    }

    @Test
    public void testWriteReadEncryptedFile() throws Exception {
        final String fileContent = "Don't tell anyone...";
        final String fileName = "nothing_to_see_here";

        // Write

        EncryptedFile encryptedFile = new EncryptedFile.Builder(fileName, mContext, mMasterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB)
                .build();

        FileOutputStream outputStream = encryptedFile.openFileOutput();
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

        FileInputStream inputStream = encryptedFile.openFileInput();
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

    /*@Test
    public void testLockedWriteReadEncryptedFile() throws Exception {
        ActivityTestRule<KeyUnlockActivity> rule = new
                ActivityTestRule<>(KeyUnlockActivity.class);
        mLockedKey.promptForUnlock(rule.getActivity(), 1, "Unlock?",
                "Testing Unlock");
    }*/

    @Test
    public void lockedWriteReadEncryptedFile() throws Exception {
        final String fileContent = "Don't tell anyone...";
        final String fileName = "nothing_to_see_here";

        // Write to file with Locked key

        EncryptedFile encryptedFile = new EncryptedFile.Builder(fileName, mContext, mLockedKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB)
                .build();
        boolean writeFailed = false;
        try {
            FileOutputStream outputStream = encryptedFile.openFileOutput();
            outputStream.write(fileContent.getBytes("UTF-8"));
            outputStream.flush();
            outputStream.close();
        } catch (UserNotAuthenticatedException ex) {
            writeFailed = true;
        }

        Assert.assertTrue("Write should have failed, locked key.", writeFailed);
    }
}

