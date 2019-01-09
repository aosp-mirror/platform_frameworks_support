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

/*
 * Test cases for the forked UriMatcher.
 */
@RunWith(AndroidJUnit4.class)
public class UriMatcherTest {
    private final Object mNoMatchObject = new Object();

    @Test
    @SmallTest
    public void testFullHttpUrlMatching() {
        Object pathMatch = new Object();
        UriMatcher<Object> matcher = new UriMatcher<>(mNoMatchObject);
        matcher.addURI("http", "org.chromium", "/some/path", pathMatch);

        Assert.assertEquals(pathMatch, matcher.match(Uri.parse("http://org.chromium/some/path")));
        Assert.assertEquals(pathMatch, matcher.match(Uri.parse("http://org.chromium/some/path?asd")));
        Assert.assertEquals(pathMatch, matcher.match(Uri.parse("http://org.chromium/some/path#asd")));

        Assert.assertNull(matcher.match(Uri.parse("https://org.chromium/some/path")));
        Assert.assertNull(matcher.match(Uri.parse("http://org.chromium/somepath")));
        Assert.assertNull(matcher.match(Uri.parse("http://org.chromium/")));
        Assert.assertNull(matcher.match(Uri.parse("http://org.chromium/some/other")));
        Assert.assertNull(matcher.match(Uri.parse("http://org.chromium/some/path/other")));

        Assert.assertEquals(mNoMatchObject, matcher.match(Uri.parse("http:")));
        Assert.assertEquals(mNoMatchObject, matcher.match(Uri.parse("https:")));
    }

    @Test
    @SmallTest
    public void testFullHttpsUrlMatching() {
        Object pathMatch = new Object();
        UriMatcher<Object> matcher = new UriMatcher<>(mNoMatchObject);
        matcher.addURI("https", "org.chromium", "/some/path", pathMatch);

        Assert.assertEquals(pathMatch, matcher.match(Uri.parse("https://org.chromium/some/path")));
        Assert.assertEquals(pathMatch, matcher.match(Uri.parse("https://org.chromium/some/path?asd")));
        Assert.assertEquals(pathMatch, matcher.match(Uri.parse("https://org.chromium/some/path#asd")));

        Assert.assertNull(matcher.match(Uri.parse("http://org.chromium/some/path")));
        Assert.assertNull(matcher.match(Uri.parse("https://org.chromium/somepath")));
        Assert.assertNull(matcher.match(Uri.parse("https://org.chromium/")));
        Assert.assertNull(matcher.match(Uri.parse("https://org.chromium/some/other")));
        Assert.assertNull(matcher.match(Uri.parse("https://org.chromium/some/path/other")));

        Assert.assertEquals(mNoMatchObject, matcher.match(Uri.parse("http:")));
        Assert.assertEquals(mNoMatchObject, matcher.match(Uri.parse("https:")));
    }

    @Test
    @SmallTest
    public void testHttpUrlSingleStarMatching() {
        Object pathMatch = new Object();
        UriMatcher<Object> matcher = new UriMatcher<>(mNoMatchObject);
        matcher.addURI("http", "org.chromium", "/path/*", pathMatch);

        Assert.assertEquals(pathMatch, matcher.match(Uri.parse("http://org.chromium/path/a")));
        Assert.assertEquals(pathMatch, matcher.match(Uri.parse("http://org.chromium/path/a?asd")));
        Assert.assertEquals(pathMatch, matcher.match(Uri.parse("http://org.chromium/path/a#asd")));

        Assert.assertNull(matcher.match(Uri.parse("https://org.chromium/pa")));
        Assert.assertNull(matcher.match(Uri.parse("http://org.chromium/path")));
        Assert.assertNull(matcher.match(Uri.parse("http://org.chromium/path?asd")));
        Assert.assertNull(matcher.match(Uri.parse("http://org.chromium/path#asd")));
        Assert.assertNull(matcher.match(Uri.parse("http://org.chromium/path/a/b/c")));
        Assert.assertNull(matcher.match(Uri.parse("http://org.chromium/other/path")));
    }

    @Test
    @SmallTest
    public void testHttpUrlDoubleStarMatching() {
        Object pathMatch = new Object();
        UriMatcher<Object> matcher = new UriMatcher<>(mNoMatchObject);
        matcher.addURI("http", "org.chromium", "/path/**", pathMatch);

        Assert.assertEquals(pathMatch, matcher.match(Uri.parse("http://org.chromium/path/a")));
        Assert.assertEquals(pathMatch, matcher.match(Uri.parse("http://org.chromium/path/a/b/c")));
        Assert.assertEquals(pathMatch, matcher.match(Uri.parse("http://org.chromium/path/a?asd")));
        Assert.assertEquals(pathMatch, matcher.match(Uri.parse("http://org.chromium/path/a#asd")));
        Assert.assertEquals(pathMatch, matcher.match(Uri.parse("http://org.chromium/path/a/b?asd")));
        Assert.assertEquals(pathMatch, matcher.match(Uri.parse("http://org.chromium/path/a/b#asd")));

        Assert.assertNull(matcher.match(Uri.parse("https://org.chromium/pa")));
        Assert.assertNull(matcher.match(Uri.parse("http://org.chromium/path")));
        Assert.assertNull(matcher.match(Uri.parse("http://org.chromium/path?asd")));
        Assert.assertNull(matcher.match(Uri.parse("http://org.chromium/path#asd")));
        Assert.assertNull(matcher.match(Uri.parse("http://org.chromium/other/path")));
    }
}

