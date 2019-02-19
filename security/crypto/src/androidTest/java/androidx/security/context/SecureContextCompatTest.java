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


    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testWriteEncryptedFile() {
        String fileContent = "SOME TEST DATA!";
        String fileName = "test_file";
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
        } catch(IOException ex){
            ex.printStackTrace();
        }


    }


}

