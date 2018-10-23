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

package androidx.car.cluster.navigation;

import static androidx.car.cluster.navigation.utils.Assertions.assertThrows;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.net.Uri;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link Image} serialization
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ImageTest {
    private static final String TEST_CONTENT_URI = "content://foo";
    private static final int TEST_WIDTH = 123;
    private static final int TEST_HEIGHT = 234;

    /**
     * Test a few equality conditions
     */
    @Test
    public void equality() {
        Image expected = createSampleImage();

        assertEquals(expected, createSampleImage());
        assertNotEquals(expected, new Image.Builder()
                .setContentUri("content://bar")
                .setIsTintable(true)
                .setOriginalSize(TEST_WIDTH, TEST_HEIGHT)
                .build());
        assertNotEquals(expected, new Image.Builder()
                .setContentUri(TEST_CONTENT_URI)
                .setIsTintable(false)
                .setOriginalSize(TEST_WIDTH, TEST_HEIGHT)
                .build());
        assertNotEquals(expected, new Image.Builder()
                .setContentUri(TEST_CONTENT_URI)
                .setIsTintable(false)
                .setOriginalSize(1, TEST_HEIGHT)
                .build());
        assertNotEquals(expected, new Image.Builder()
                .setContentUri(TEST_CONTENT_URI)
                .setIsTintable(false)
                .setOriginalSize(TEST_WIDTH, 1)
                .build());

        assertEquals(expected.hashCode(), new Image.Builder()
                .setContentUri(TEST_CONTENT_URI)
                .setIsTintable(true)
                .setOriginalSize(TEST_WIDTH, TEST_HEIGHT)
                .build()
                .hashCode());
    }

    /**
     * Tests the output of the {@link Image.Builder}
     */
    @Test
    public void builder_outputShouldMatchExpected() {
        Image image = createSampleImage();

        assertEquals(TEST_CONTENT_URI, image.getRawContentUri());
        assertEquals(true, image.isTintable());
        assertEquals(TEST_WIDTH, image.getOriginalWidth());
        assertEquals(TEST_HEIGHT, image.getOriginalHeight());
    }

    /**
     * Returns a sample {@link Image} instance for testing.
     */
    public static Image createSampleImage() {
        return new Image.Builder()
                    .setContentUri(TEST_CONTENT_URI)
                    .setIsTintable(true)
                    .setOriginalSize(TEST_WIDTH, TEST_HEIGHT)
                    .build();
    }

    /**
     * Tests {@link Image.Builder} can be used to produce more than one instance.
     */
    @Test
    public void builder_shouldBeReusable() {
        final String alternativeContentUri = "content://bar";

        Image.Builder builder = new Image.Builder()
                .setContentUri(TEST_CONTENT_URI)
                .setIsTintable(true)
                .setOriginalSize(TEST_WIDTH, TEST_HEIGHT);
        Image image1 = builder.build();
        Image image2 = builder.build();
        assertEquals(image1, image2);

        builder.setContentUri(alternativeContentUri);
        Image image3 = builder.build();
        assertEquals(image3, new Image.Builder()
                .setContentUri(alternativeContentUri)
                .setIsTintable(true)
                .setOriginalSize(TEST_WIDTH, TEST_HEIGHT)
                .build());
    }

    /**
     * {@link Image.Builder#setContentUri(String)} must be called.
     */
    @Test(expected = NullPointerException.class)
    public void builder_contentUriIsMandatory() {
        new Image.Builder().setOriginalSize(TEST_WIDTH, TEST_HEIGHT).build();
    }

    /**
     * Tests that passing strings not starting with 'content://' to
     * {@link Image.Builder#setContentUri(String)} throws an exception.
     */
    @Test
    public void builder_contentUriOnlyAcceptsContentSchema() {
        Image.Builder builder = new Image.Builder();
        assertThrows(NullPointerException.class, () -> builder.setContentUri(null));
        assertThrows(IllegalArgumentException.class, () -> builder.setContentUri(""));
        assertThrows(IllegalArgumentException.class, () -> builder.setContentUri("http://foo"));
        assertThrows(IllegalArgumentException.class, () -> builder.setContentUri("content:/foo"));
        assertThrows(IllegalArgumentException.class, () -> builder.setContentUri("content//foo"));
    }

    /**
     * Even if a content URI was not received, {@link Image#getRawContentUri()} should return an
     * empty string.
     */
    @Test
    public void contentUri_unsetContentUriReturnsEmptyRawUri() {
        assertEquals("", new Image().getRawContentUri());
    }

    /**
     * If {@link Image.Builder#setContentUri(String)} is not used, then
     * {@link Image#getContentUri(int, int)} should return null.
     */
    @Test
    public void contentUri_unsetContentUriRetursNullUri() {
        assertEquals(null, new Image().getContentUri(TEST_WIDTH, TEST_HEIGHT));
    }

    /**
     * {@link Image#getContentUri(int, int)} should provide a fully formed URI containing with
     * and height parameters.
     */
    @Test
    public void contentUri_requestMustContainWidthAndHeightParameters() {
        Image image = new Image.Builder()
                .setContentUri(TEST_CONTENT_URI)
                .setOriginalSize(1, 1)
                .build();

        assertEquals(Uri.parse("content://foo?w=123&h=234"),
                image.getContentUri(TEST_WIDTH, TEST_HEIGHT));
    }

    /**
     * {@link Image#getContentUri(int, int)} throws an exception if size is not provided.
     */
    @Test
    public void contentUri_widthAndHeightParametersMustBePositive() {
        Image image = new Image.Builder()
                .setContentUri(TEST_CONTENT_URI)
                .setOriginalSize(1, 1)
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                image.getContentUri(0, TEST_HEIGHT));
        assertThrows(IllegalArgumentException.class, () ->
                image.getContentUri(TEST_WIDTH, 0));
    }
}
