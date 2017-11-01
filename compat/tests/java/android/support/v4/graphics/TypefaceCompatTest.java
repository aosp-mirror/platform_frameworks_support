/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.v4.graphics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.compat.test.R;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.v4.content.res.FontResourcesParserCompat;
import android.support.v4.content.res.FontResourcesParserCompat.FamilyResourceEntry;
import android.support.v4.content.res.FontResourcesParserCompat.ProviderResourceEntry;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.provider.FontRequest;
import android.support.v4.provider.FontsContractCompat;
import android.support.v4.provider.MockFontProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SmallTest
public class TypefaceCompatTest {

    public Context mContext;
    public Resources mResources;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mResources = mContext.getResources();
        MockFontProvider.prepareFontFiles(mContext);
    }

    @After
    public void tearDown() {
        MockFontProvider.cleanUpFontFiles(mContext);
    }

    // Signature to be used for authentication to access content provider.
    // In this test case, the content provider and consumer live in the same package, self package's
    // signature works.
    private static final List<List<byte[]>> SIGNATURE;
    static {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNATURES);
            ArrayList<byte[]> out = new ArrayList<>();
            for (Signature sig : info.signatures) {
                out.add(sig.toByteArray());
            }
            SIGNATURE = new ArrayList<>();
            SIGNATURE.add(out);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper method to get the used font resource id by typeface.
     *
     * If the typeface is created from one of the R.font.large_a, R.font.large_b, R.font.large_c or
     * R.font.large_d resource, this method returns the resource id used by the typeface.
     */
    private static int getSelectedFontResourceId(Typeface typeface) {
        // The glyph for "a" in R.font.large_a font has a 3em width and glyph for "b", "c" and "d"
        // have 1em width. Similarly, The glyph for "b" in R.font.large_b font, the glyph for "c"
        // in R.font.large_c font, the glyph for "d" in R.font.large_d font has 3em width and the
        // glyph for the rest characters have 1em. Thus we can get the resource id of the source
        // font file by comparing width of "a", "b", "c" and "d".
        Paint p = new Paint();
        p.setTypeface(typeface);
        final int[] ids = { R.font.large_a, R.font.large_b, R.font.large_c, R.font.large_d };
        final float[] widths = {
            p.measureText("a"), p.measureText("b"), p.measureText("c"), p.measureText("d")
        };

        int maxIndex = Integer.MIN_VALUE;
        float maxValue = Float.MIN_VALUE;
        for (int i = 0; i < widths.length; ++i) {
            if (maxValue < widths[i]) {
                maxIndex = i;
                maxValue = widths[i];
            }
        }
        return ids[maxIndex];
    }

    /**
     * Helper method to obtain ProviderResourceEntry with overwriting correct signatures.
     */
    private ProviderResourceEntry getProviderResourceEntry(int id) {
        final ProviderResourceEntry entry;
        try {
            entry = (ProviderResourceEntry) FontResourcesParserCompat.parse(
                    mResources.getXml(id), mResources);
        } catch (XmlPullParserException | IOException e) {
            throw new RuntimeException(e);
        }
        final FontRequest parsedRequest = entry.getRequest();
        final FontRequest request = new FontRequest(parsedRequest.getProviderAuthority(),
                parsedRequest.getProviderPackage(), parsedRequest.getQuery(), SIGNATURE);
        return new ProviderResourceEntry(request, entry.getFetchStrategy(), entry.getTimeout());
    }

    public static class FontCallback extends ResourcesCompat.FontCallback {
        private final CountDownLatch mLatch;
        Typeface mTypeface;

        FontCallback(CountDownLatch latch) {
            mLatch = latch;
        }

        @Override
        public void onFontRetrieved(@NonNull Typeface typeface) {
            mTypeface = typeface;
            mLatch.countDown();
        }

        @Override
        public void onFontRetrievalFailed(int reason) {
            mLatch.countDown();
        }
    }

    @Test
    public void testCreateFromResourcesFamilyXml_resourceFont_asyncloading() throws Exception {
        final Instrumentation inst = InstrumentationRegistry.getInstrumentation();
        CountDownLatch latch = new CountDownLatch(1);
        final FontCallback callback = new FontCallback(latch);

        inst.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                TypefaceCompat.createFromResourcesFamilyXml(mContext,
                        getProviderResourceEntry(R.font.styletest_async_providerfont), mResources,
                        R.font.styletest_async_providerfont, Typeface.NORMAL, callback,
                        null /* handler */, false /* isXmlRequest */);
            }
        });
        assertTrue(latch.await(5L, TimeUnit.SECONDS));

        assertEquals(R.font.large_a, getSelectedFontResourceId(callback.mTypeface));

        latch = new CountDownLatch(1);
        final FontCallback callback2 = new FontCallback(latch);
        inst.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                TypefaceCompat.createFromResourcesFamilyXml(mContext,
                        getProviderResourceEntry(R.font.styletest_async_providerfont), mResources,
                        R.font.styletest_async_providerfont, Typeface.ITALIC, callback2,
                        null /* handler */, false /* isXmlRequest */);
            }
        });
        assertTrue(latch.await(5L, TimeUnit.SECONDS));
        assertEquals(R.font.large_b, getSelectedFontResourceId(callback2.mTypeface));

        latch = new CountDownLatch(1);
        final FontCallback callback3 = new FontCallback(latch);
        inst.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                TypefaceCompat.createFromResourcesFamilyXml(mContext,
                        getProviderResourceEntry(R.font.styletest_async_providerfont), mResources,
                        R.font.styletest_async_providerfont, Typeface.BOLD, callback3,
                        null /* handler */, false /* isXmlRequest */);
            }
        });
        assertTrue(latch.await(5L, TimeUnit.SECONDS));
        assertEquals(R.font.large_c, getSelectedFontResourceId(callback3.mTypeface));

        latch = new CountDownLatch(1);
        final FontCallback callback4 = new FontCallback(latch);
        inst.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                TypefaceCompat.createFromResourcesFamilyXml(mContext,
                        getProviderResourceEntry(R.font.styletest_async_providerfont), mResources,
                        R.font.styletest_async_providerfont, Typeface.BOLD_ITALIC, callback4,
                        null /* handler */, false /* isXmlRequest */);
            }
        });
        assertTrue(latch.await(5L, TimeUnit.SECONDS));
        assertEquals(R.font.large_d, getSelectedFontResourceId(callback4.mTypeface));
    }

    @Test
    public void testProviderFont_xmlRequest() {
        Typeface typeface = TypefaceCompat.createFromResourcesFamilyXml(mContext,
                getProviderResourceEntry(R.font.samplexmldownloadedfontblocking), mResources,
                R.font.samplexmldownloadedfontblocking, Typeface.NORMAL, null,
                null /* handler */, true /* isXmlRequest */);

        assertNotNull(typeface);
        assertNotEquals(Typeface.DEFAULT, typeface);
    }

    @Test
    public void testProviderFont_nonXmlRequest_noCallback() {
        // If we don't give a callback, the request should be blocking.
        Typeface typeface = TypefaceCompat.createFromResourcesFamilyXml(mContext,
                getProviderResourceEntry(R.font.samplexmldownloadedfontblocking), mResources,
                R.font.samplexmldownloadedfontblocking, Typeface.NORMAL, null,
                null /* handler */, false /* isXmlRequest */);

        assertNotNull(typeface);
        assertNotEquals(Typeface.DEFAULT, typeface);
    }

    @Test
    public void testProviderFont_nonXmlRequest_withCallback() throws InterruptedException {
        Instrumentation inst = InstrumentationRegistry.getInstrumentation();
        CountDownLatch latch = new CountDownLatch(1);
        final FontCallback callback = new FontCallback(latch);
        FontsContractCompat.resetCache();

        final Typeface[] result = new Typeface[1];
        inst.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                result[0] = TypefaceCompat.createFromResourcesFamilyXml(mContext,
                        getProviderResourceEntry(R.font.samplexmldownloadedfontblocking),
                        mResources, R.font.samplexmldownloadedfontblocking, Typeface.NORMAL,
                        callback, null /* handler */, false /* isXmlRequest */);
            }
        });
        assertTrue(latch.await(5L, TimeUnit.SECONDS));
        assertNotNull(callback.mTypeface);
        assertNull(result[0]);
    }

    @Test
    public void testProviderFont_nonXmlRequest_withCallback_cached() throws InterruptedException {
        Instrumentation inst = InstrumentationRegistry.getInstrumentation();
        CountDownLatch latch = new CountDownLatch(1);
        final FontCallback callback = new FontCallback(latch);
        FontsContractCompat.resetCache();

        final Typeface[] result = new Typeface[2];
        inst.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                result[0] = TypefaceCompat.createFromResourcesFamilyXml(mContext,
                        getProviderResourceEntry(R.font.samplexmldownloadedfontblocking),
                        mResources, R.font.samplexmldownloadedfontblocking, Typeface.NORMAL,
                        callback, null /* handler */, false /* isXmlRequest */);
            }
        });
        assertTrue(latch.await(5L, TimeUnit.SECONDS));
        assertNotNull(callback.mTypeface);
        assertNull(result[0]);

        latch = new CountDownLatch(1);
        final FontCallback callback2 = new FontCallback(latch);

        inst.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                result[1] = TypefaceCompat.createFromResourcesFamilyXml(mContext,
                        getProviderResourceEntry(R.font.samplexmldownloadedfontblocking),
                        mResources, R.font.samplexmldownloadedfontblocking, Typeface.NORMAL,
                        callback2, null /* handler */, false /* isXmlRequest */);
            }
        });
        assertTrue(latch.await(5L, TimeUnit.SECONDS));
        assertNotNull(callback2.mTypeface);
        assertNotNull(result[1]);
    }

    @Test
    public void testCreateFromResourcesFamilyXml_resourceFont() throws Exception {
        @SuppressLint("ResourceType")
        // We are retrieving the XML font as an XML resource for testing purposes.
        final FamilyResourceEntry entry = FontResourcesParserCompat.parse(
                mResources.getXml(R.font.styletestfont), mResources);
        Typeface typeface = TypefaceCompat.createFromResourcesFamilyXml(mContext, entry, mResources,
                R.font.styletestfont, Typeface.NORMAL, null /* callback */, null /* handler */,
                false /* isXmlRequest */);
        Typeface cachedTypeface = TypefaceCompat.findFromCache(
                mResources, R.font.styletestfont, Typeface.NORMAL);
        assertNotNull(cachedTypeface);
        assertEquals(typeface, cachedTypeface);
        typeface = Typeface.create(typeface, Typeface.NORMAL);
        // styletestfont has a node of fontStyle="normal" fontWeight="400" font="@font/large_a".
        assertEquals(R.font.large_a, getSelectedFontResourceId(typeface));

        typeface = TypefaceCompat.createFromResourcesFamilyXml(mContext, entry, mResources,
                R.font.styletestfont, Typeface.ITALIC, null /* callback */, null /* handler */,
                false /* isXmlRequest */);
        cachedTypeface = TypefaceCompat.findFromCache(
                mResources, R.font.styletestfont, Typeface.ITALIC);
        assertNotNull(cachedTypeface);
        assertEquals(typeface, cachedTypeface);
        typeface = Typeface.create(typeface, Typeface.ITALIC);
        // styletestfont has a node of fontStyle="italic" fontWeight="400" font="@font/large_b".
        assertEquals(R.font.large_b, getSelectedFontResourceId(typeface));

        typeface = TypefaceCompat.createFromResourcesFamilyXml(mContext, entry, mResources,
                R.font.styletestfont, Typeface.BOLD, null /* callback */, null /* handler */,
                false /* isXmlRequest */);
        cachedTypeface = TypefaceCompat.findFromCache(
                mResources, R.font.styletestfont, Typeface.BOLD);
        assertNotNull(cachedTypeface);
        assertEquals(typeface, cachedTypeface);
        typeface = Typeface.create(typeface, Typeface.BOLD);
        // styletestfont has a node of fontStyle="normal" fontWeight="700" font="@font/large_c".
        assertEquals(R.font.large_c, getSelectedFontResourceId(typeface));

        typeface = TypefaceCompat.createFromResourcesFamilyXml(mContext, entry, mResources,
                R.font.styletestfont, Typeface.BOLD_ITALIC, null /* callback */,
                null /* handler */, false /* isXmlRequest */);
        cachedTypeface = TypefaceCompat.findFromCache(
                mResources, R.font.styletestfont, Typeface.BOLD_ITALIC);
        assertNotNull(cachedTypeface);
        assertEquals(typeface, cachedTypeface);
        typeface = Typeface.create(typeface, Typeface.BOLD_ITALIC);
        // styletestfont has a node of fontStyle="italic" fontWeight="700" font="@font/large_d".
        assertEquals(R.font.large_d, getSelectedFontResourceId(typeface));
    }

    @Test
    public void testCreateFromResourcesFontFile() {
        Typeface typeface = TypefaceCompat.createFromResourcesFontFile(mContext, mResources,
                R.font.large_a, "res/font/large_a.ttf", Typeface.NORMAL);
        assertNotNull(typeface);
        Typeface cachedTypeface = TypefaceCompat.findFromCache(
                mResources, R.font.large_a, Typeface.NORMAL);
        assertNotNull(cachedTypeface);
        assertEquals(typeface, cachedTypeface);
        assertEquals(R.font.large_a, getSelectedFontResourceId(typeface));

        typeface = TypefaceCompat.createFromResourcesFontFile(mContext, mResources, R.font.large_b,
                "res/font/large_b.ttf", Typeface.NORMAL);
        assertNotNull(typeface);
        cachedTypeface = TypefaceCompat.findFromCache(
                mResources, R.font.large_b, Typeface.NORMAL);
        assertNotNull(cachedTypeface);
        assertEquals(typeface, cachedTypeface);
        assertEquals(R.font.large_b, getSelectedFontResourceId(typeface));
    }
}
