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

package androidx.sparklers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.slice.widget.SliceView;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import dalvik.system.PathClassLoader;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SparklersTest {

    private final Context mContext = InstrumentationRegistry.getContext();

    @Test
    public void testPolicyBuilder() throws SparkException {
        String vers = "1.1.0";
        LoadPolicy policy = new LoadPolicy.Builder("test.library")
                .setLocalVersion(vers)
                .setLoadMode(LoadPolicy.ONLY_LOAD)
                .setMinVersion("1.0.0")
                .disableVersion("1.0.0-alpha02")
                .disableVersion("1.0.0-alpha03")
                .build();

        assertEquals(vers, policy.getLocalVersion());
        assertEquals("1.0.0", policy.getMinVersion());
        assertEquals("test.library", policy.getLibrary());
        assertEquals(LoadPolicy.ONLY_LOAD, policy.getLoadMode());
        assertEquals(2, policy.getDisabledVersions().size());
        assertTrue(policy.getDisabledVersions().contains("1.0.0-alpha02"));
        assertTrue(policy.getDisabledVersions().contains("1.0.0-alpha03"));
    }

    @Test
    public void testPolicyXml() throws SparkException {
        String vers = "1.1.0";
        LoadPolicy policy = LoadPolicy.readLoadPolicy(mContext, R.xml.test_policy);

        assertEquals(vers, policy.getLocalVersion());
        assertEquals("1.0.0", policy.getMinVersion());
        assertEquals("test.library", policy.getLibrary());
        assertEquals(LoadPolicy.ONLY_LOAD, policy.getLoadMode());
        assertEquals(2, policy.getDisabledVersions().size());
        assertTrue(policy.getDisabledVersions().contains("1.0.0-alpha02"));
        assertTrue(policy.getDisabledVersions().contains("1.0.0-alpha03"));
    }

    @Test
    public void testLoad() throws Exception {
        LoadPolicy policy = LoadPolicy.readLoadPolicy(mContext, R.xml.slice_policy);
        Spark spark = Sparklers.loadSpark(mContext, policy);
        assertNotNull(spark);
        assertEquals(Spark.SOURCE_LOADED, spark.getSource());
        assertNotEquals(getClass().getClassLoader(), spark.getClassLoader());
        Runnable r = (Runnable) new PathClassLoader(mContext.getApplicationInfo().sourceDir,
                spark.getClassLoader()).loadClass("androidx.sparklers.LoadedClassCheck")
                .getConstructor().newInstance();
        r.run();
    }
}
