/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.webkit.internal;

import android.net.Uri;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WebUriMatcherTest {

    @Test
    @SmallTest
    public void testFullHttpUriMatching() {
        final Object pathMatch = new Object();
        WebUriMatcher<Object> matcher = new WebUriMatcher<>();
        matcher.addUri("http", "org.chromium", "/some/path", pathMatch);

        Assert.assertEquals("--failed 11 --", pathMatch, matcher.match(Uri.parse("http://org.chromium/some/path")));
        Assert.assertEquals("--failed 12 --", pathMatch, matcher.match(Uri.parse("http://org.chromium/some/path?asd")));
        Assert.assertEquals("--failed 13 --", pathMatch, matcher.match(Uri.parse("http://org.chromium/some/path#asd")));
        Assert.assertEquals("--failed 18 --", pathMatch, matcher.match(Uri.parse("http://org.chromium/some/path/other")));

        Assert.assertNull("--failed 14 --", matcher.match(Uri.parse("https://org.chromium/some/path")));
        Assert.assertNull("--failed 15 --", matcher.match(Uri.parse("http://org.chromium/somepath")));
        Assert.assertNull("--failed 16 --", matcher.match(Uri.parse("http://org.chromium/")));
        Assert.assertNull("--failed 17 --", matcher.match(Uri.parse("http://org.chromium/some/other")));
    }

    @Test
    @SmallTest
    public void testFullHttpsUrlMatching() {
        final Object pathMatch = new Object();
        WebUriMatcher<Object> matcher = new WebUriMatcher<>();
        matcher.addUri("https", "org.chromium", "/some/path", pathMatch);

        Assert.assertEquals("--failed 21 --", pathMatch, matcher.match(Uri.parse("https://org.chromium/some/path")));
        Assert.assertEquals("--failed 22 --", pathMatch, matcher.match(Uri.parse("https://org.chromium/some/path?asd")));
        Assert.assertEquals("--failed 23 --", pathMatch, matcher.match(Uri.parse("https://org.chromium/some/path#asd")));
        Assert.assertEquals("--failed 28 --", pathMatch, matcher.match(Uri.parse("https://org.chromium/some/path/other")));

        Assert.assertNull("--failed 24 --", matcher.match(Uri.parse("http://org.chromium/some/path")));
        Assert.assertNull("--failed 25 --", matcher.match(Uri.parse("https://org.chromium/somepath")));
        Assert.assertNull("--failed 26 --", matcher.match(Uri.parse("https://org.chromium/")));
        Assert.assertNull("--failed 27 --", matcher.match(Uri.parse("https://org.chromium/some/other")));
    }

    @Test
    @SmallTest
    public void testHttpUrlSingleStarMatching() {
        final Object pathMatch = new Object();
        WebUriMatcher<Object> matcher = new WebUriMatcher<>();
        matcher.addUri("http", "org.chromium", "/path/", pathMatch);

        Assert.assertEquals("--failed 31 --", pathMatch, matcher.match(Uri.parse("http://org.chromium/path/a")));
        Assert.assertEquals("--failed 32 --", pathMatch, matcher.match(Uri.parse("http://org.chromium/path/a?asd")));
        Assert.assertEquals("--failed 33 --", pathMatch, matcher.match(Uri.parse("http://org.chromium/path/a#asd")));

        Assert.assertNull("--failed 34 --", matcher.match(Uri.parse("https://org.chromium/pa")));
        Assert.assertNull("--failed 35 --", matcher.match(Uri.parse("http://org.chromium/path")));
        Assert.assertNull("--failed 36 --", matcher.match(Uri.parse("http://org.chromium/path?asd")));
        Assert.assertNull("--failed 37 --", matcher.match(Uri.parse("http://org.chromium/path#asd")));
        Assert.assertNull("--failed 38 --", matcher.match(Uri.parse("http://org.chromium/other/path")));
    }
}

