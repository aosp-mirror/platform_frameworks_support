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
import java.io.InputStream;

import javax.net.ssl.HttpsURLConnection;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
public class ValidatedUrlTest {

    @Test
    public void testValidHttpsUrlConnection() {
        String url = "https://www.google.com";
        try {
            ValidatedUrl validatedURL = new ValidatedUrl(url, true);
            HttpsURLConnection connection = validatedURL.openConnection();
            InputStream inputStream = connection.getInputStream();
            inputStream.read();

            Assert.assertTrue("Connection to " + url + " should be valid.",
                    true);
        } catch (IOException ex) {
            Assert.assertTrue("Bad URL: " + url,
                    true);
        } catch (SecurityException ex) {
            Assert.assertTrue("Connection to " + url
                            + " should be  invalid, revoked cert.",
                    true);
        }


    }

    @Test
    public void testInValidHttpsUrlConnection() {
        String[] urls = new String[] {
                "https://revoked.badssl.com",
                "https://expired.badssl.com",
                "https://untrusted-root.badssl.com",
                "https://self-signed.badssl.com",
                "https://wrong.host.badssl.com",
                "https://pinning-test.badssl.com"
        };
        for (String url : urls) {
            boolean badUrl = false;
            try {
                ValidatedUrl validatedURL = new ValidatedUrl(url, true);
                HttpsURLConnection connection = validatedURL.openConnection();
                InputStream inputStream = connection.getInputStream();
                inputStream.read();

                Assert.assertTrue("Connection should have be denied: " + url,
                        false);
            } catch (IOException ex) {
                Assert.assertTrue("Bad URL: " + url,
                        true);
                badUrl = true;
            } catch (SecurityException ex) {
                ex.printStackTrace();
                Assert.assertTrue("Connection to " + url
                                + " should be  invalid, revoked cert.",
                        true);
                badUrl = true;
            }

            Assert.assertTrue("Should have caught " + url, badUrl);
        }
    }

    @Test
    public void testNoOCSPdHttpsUrlConnection() {
        String[] urls = new String[] {
                "https://revoked.badssl.com",
                "https://pinning-test.badssl.com"
        };
        for (String url : urls) {
            boolean badUrl = false;
            try {
                ValidatedUrl validatedURL = new ValidatedUrl(url, false);
                HttpsURLConnection connection = validatedURL.openConnection();
                InputStream inputStream = connection.getInputStream();
                inputStream.read();

                Assert.assertFalse("Connection should gone through: " + url,
                        false);
            } catch (IOException ex) {
                Assert.assertTrue("Bad URL: " + url,
                        true);
                badUrl = true;
            } catch (SecurityException ex) {
                ex.printStackTrace();
                Assert.assertTrue("Connection to " + url
                                + " should be  invalid, revoked cert.",
                        true);
                badUrl = true;
            }

            Assert.assertFalse("Should have not caught " + url, badUrl);
        }
    }
}
