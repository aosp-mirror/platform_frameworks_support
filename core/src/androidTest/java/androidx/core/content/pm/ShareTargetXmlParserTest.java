/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.content.pm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.ContextWrapper;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ShareTargetXmlParserTest {

    // These values must match the content of shortcuts.xml resource
    private static final String TEST_MIME_TYPE_1 = "text/plain";
    private static final String TEST_ACTION_1 = "com.test.googlex.TEST_ACTION1";
    private static final String TEST_CATEGORY_1 = "com.test.googlex.category.CATEGORY1";
    private static final String TEST_MIME_TYPE_2 = "video/mp4";
    private static final String TEST_ACTION_2 = "com.test.googlex.TEST_ACTION5";
    private static final String TEST_CATEGORY_2 = "com.test.googlex.category.CATEGORY5";
    private static final String TEST_PACKAGE_NAME = "com.test.googlex.directshare";
    private static final String TEST_CLASS_NAME = "com.test.googlex.directshare.TestActivity";

    private Context mContext;

    @Before
    public void setup() {
        mContext = spy(new ContextWrapper(InstrumentationRegistry.getContext()));
    }

    /**
     * Tests if ShareTargetXmlParser is able to:
     * a) locate the xml resource and read it
     * b) ignore the legacy shortcut definitions if any
     * c) drop incomplete share-target definitions
     * d) read and return the expected share-targets
     */
    @Test
    public void testGetShareTargets() {
        ArrayList<ShareTargetCompat> shareTargets = ShareTargetXmlParser.getShareTargets(mContext);

        assertNotNull(shareTargets);
        assertEquals(2, shareTargets.size());

        assertEquals(TEST_MIME_TYPE_1, shareTargets.get(0).mDataType);
        assertEquals(TEST_ACTION_1, shareTargets.get(0).mIntent.getAction());
        assertEquals(TEST_PACKAGE_NAME,
                shareTargets.get(0).mIntent.getComponent().getPackageName());
        assertEquals(TEST_CLASS_NAME, shareTargets.get(0).mIntent.getComponent().getClassName());
        assertEquals(TEST_CATEGORY_1, shareTargets.get(0).mCategory);

        assertEquals(TEST_MIME_TYPE_2, shareTargets.get(1).mDataType);
        assertEquals(TEST_ACTION_2, shareTargets.get(1).mIntent.getAction());
        assertEquals(TEST_PACKAGE_NAME,
                shareTargets.get(1).mIntent.getComponent().getPackageName());
        assertEquals(TEST_CLASS_NAME, shareTargets.get(1).mIntent.getComponent().getClassName());
        assertEquals(TEST_CATEGORY_2, shareTargets.get(1).mCategory);
    }
}
