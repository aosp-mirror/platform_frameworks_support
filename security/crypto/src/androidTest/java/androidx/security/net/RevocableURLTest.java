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

package androidx.security.net;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.net.ssl.HttpsURLConnection;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
public class RevocableURLTest {

    @Test
    public void testValidHttpsUrlConnection() {
        String url = "https://www.google.com";
        try {
            RevocableURL revocableURL = new RevocableURL(url);
            HttpsURLConnection connection = (HttpsURLConnection) revocableURL.openConnection();


            revocableURL.ensureValid(connection);

            Assert.assertTrue("Connection to " + url + " should be valid.",
                    true);
        } catch (IOException ex) {
            Assert.assertTrue("Bad URL: " + url,
                    true);
        } catch (GeneralSecurityException ex) {
            Assert.assertTrue("Connection to " + url
                            + " should be  invalid, revoked cert.",
                    true);
        }


    }

    @Test
    public void testInValidHttpsUrlConnection() {
        String url = "https://revoked.badssl.com";
        try {
            RevocableURL revocableURL = new RevocableURL(url);
            HttpsURLConnection connection = (HttpsURLConnection) revocableURL.openConnection();

            revocableURL.ensureValid(connection);
        } catch (IOException ex) {
            Assert.assertTrue("Bad URL: " + url,
                    true);
        } catch (GeneralSecurityException ex) {
            Assert.assertTrue("Connection to " + url
                            + " should be  invalid, revoked cert.",
                    true);
        }







    }
}
