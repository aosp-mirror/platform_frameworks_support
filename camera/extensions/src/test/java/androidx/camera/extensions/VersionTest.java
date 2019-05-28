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

package androidx.camera.extensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VersionTest {

    @Test
    public void testVersionEqual() {
        Version version1 = Version.create(1, 0, 0, "test");
        Version version1_patch = Version.create(1, 0, 1, "test");
        Version version1_minor = Version.create(1, 1, 0, "test");
        Version version1_description = Version.create(1, 0, 0, "description");

        Version version2 = Version.create(2, 0, 0, "test");

        assertTrue(version1.equals(version1_description));
        assertFalse(version1.equals(version1_patch));
        assertFalse(version1.equals(version1_minor));
        assertFalse(version1.equals(version2));

        assertTrue(version1.isSameMajorMinor(version1_patch));
        assertTrue(version1.isSameMajorMinor(version1_description));
        assertFalse(version1.isSameMajorMinor(version1_minor));
        assertFalse(version1.isSameMajorMinor(version2));
    }

    @Test
    public void testParseStringVersion() {

        Version version1 = Version.parse("1.2.3-description");
        assertNotNull(version1);
        assertEquals(version1.getMajor(), 1);
        assertEquals(version1.getMinor(), 2);
        assertEquals(version1.getPatch(), 3);
        assertEquals(version1.getDescription(), "description");

        Version version2 = Version.parse("4.5.6");
        assertNotNull(version2);
        assertEquals(version2.getDescription(), "");

        Version version3 = Version.parse("01.002.0003");
        assertNotNull(version3);
        assertEquals(version3.getMajor(), 1);
        assertEquals(version3.getMinor(), 2);
        assertEquals(version3.getPatch(), 3);


        // Test invalid input version string.
        assertNull(Version.parse("1.0"));
        assertNull(Version.parse("1. 0.0"));
        assertNull(Version.parse("1..0"));
        assertNull(Version.parse("1.0.a"));
        assertNull(Version.parse("1.0.0."));
        assertNull(Version.parse("1.0.0.description"));

        assertNull(Version.parse("1.0.0.0"));
        assertNull(Version.parse("1.0.-0"));
        assertNull(Version.parse("1.0.-0"));
        assertNull(Version.parse("(1.0.0)"));
        assertNull(Version.parse(" 1.0.0 "));
    }
}
