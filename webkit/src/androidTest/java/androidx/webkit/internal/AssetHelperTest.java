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

package androidx.webkit.internal;

import static androidx.webkit.internal.AssetHelper.FileOutsideMountedDirectoryException;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@RunWith(AndroidJUnit4.class)
public class AssetHelperTest {
    private static final String TAG = "AssetHelperTest";

    private static final String TEST_STRING = "Just a test";
    private AssetHelper mAssetHelper;
    private File mInternalStorageTestDir;

    @Before
    public void setup() {
        Context context = InstrumentationRegistry.getContext();
        mAssetHelper = new AssetHelper(context);
        mInternalStorageTestDir = new File(context.getFilesDir(), "test_dir");
        mInternalStorageTestDir.mkdirs();
    }

    @After
    public void tearDown() {
        recursivelyDeleteFile(mInternalStorageTestDir);
    }

    @Test
    @SmallTest
    public void testOpenExistingResource() {
        InputStream stream = mAssetHelper.openResource(Uri.parse("raw/test.txt"));

        Assert.assertNotNull("failed to open resource raw/test.txt", stream);
        Assert.assertEquals(readAsString(stream), TEST_STRING);
    }

    @Test
    @SmallTest
    public void testOpenExistingResourceWithLeadingSlash() {
        InputStream stream = mAssetHelper.openResource(Uri.parse("/raw/test"));

        Assert.assertNotNull("failed to open resource /raw/test.txt with leading slash", stream);
        Assert.assertEquals(readAsString(stream), TEST_STRING);
    }

    @Test
    @SmallTest
    public void testOpenExistingResourceWithNoExtension() {
        InputStream stream = mAssetHelper.openResource(Uri.parse("raw/test"));

        Assert.assertNotNull("failed to open resource raw/test with no extension", stream);
        Assert.assertEquals(readAsString(stream), TEST_STRING);
    }

    @Test
    @SmallTest
    public void testOpenInvalidResources() {
        Assert.assertNull("raw/nonexist_file.html doesn't exist - should fail",
                          mAssetHelper.openResource(Uri.parse("raw/nonexist_file.html")));

        Assert.assertNull("test.txt doesn't have a resource type - should fail",
                          mAssetHelper.openResource(Uri.parse("test.txt")));

        Assert.assertNull("resource with \"/android_res\" prefix should fail",
                          mAssetHelper.openResource(Uri.parse("/android_res/raw/test.txt")));
    }

    @Test
    @SmallTest
    public void testOpenExistingAsset() {
        InputStream stream = mAssetHelper.openAsset(Uri.parse("text/test.txt"));

        Assert.assertNotNull("failed to open asset text/test.txt", stream);
        Assert.assertEquals(readAsString(stream), TEST_STRING);
    }

    @Test
    @SmallTest
    public void testOpenExistingAssetWithLeadingSlash() {
        InputStream stream = mAssetHelper.openAsset(Uri.parse("/text/test.txt"));

        Assert.assertNotNull("failed to open asset /text/test.txt with leading slash", stream);
        Assert.assertEquals(readAsString(stream), TEST_STRING);
    }

    @Test
    @SmallTest
    public void testOpenInvalidAssets() {
        Assert.assertNull("nonexist_file.html doesn't exist - should fail",
                          mAssetHelper.openAsset(Uri.parse("nonexist_file.html")));

        Assert.assertNull("asset with \"/android_asset\" prefix should fail",
                          mAssetHelper.openAsset(Uri.parse("/android_asset/test.txt")));
    }

    @Test
    @SmallTest
    public void testOpenFileFromInternalStorage() throws Throwable {
        final String testPath = "some_file.txt";
        writeToInternalStorage(testPath, TEST_STRING);

        InputStream stream = mAssetHelper.openFile(mInternalStorageTestDir, testPath);
        Assert.assertNotNull("Should be able to open \"" + testPath + "\" from internal storage",
                stream);
        Assert.assertEquals(readAsString(stream), TEST_STRING);
    }

    @Test
    @SmallTest
    public void testOpenNonExistingFileInInternalStorage() throws Throwable {
        final String testPath = "some/path/to/non_exist_file.txt";
        InputStream stream = mAssetHelper.openFile(mInternalStorageTestDir, testPath);
        Assert.assertNull("Should not be able to open a non existing file from internal storage",
                stream);
    }

    @Test
    @SmallTest
    public void testOpenFileOutsideSubdirectoryInInternalStorage_validAccess() throws Throwable {
        writeToInternalStorage("/some/path/to/file_1.txt", TEST_STRING);
        writeToInternalStorage("/some/path/file_2.txt", TEST_STRING);

        InputStream stream = mAssetHelper.openFile(new File(mInternalStorageTestDir, "/some/path/"),
                "/to/./file_1.txt");
        Assert.assertNotNull(
                "Should be able to open \"file_1.txt\" from the mounted internal storage directory",
                 stream);
        Assert.assertEquals(readAsString(stream), TEST_STRING);

        stream = mAssetHelper.openFile(new File(mInternalStorageTestDir, "/some/path/"),
                 "/to/../file_2.txt");
        Assert.assertNotNull(
                "Should be able to open \"file_2.txt\" from the mounted internal storage directory",
                 stream);
        Assert.assertEquals(readAsString(stream), TEST_STRING);
    }

    @Test(expected = FileOutsideMountedDirectoryException.class)
    @SmallTest
    public void testOpenFileOutsideSubdirectoryInInternalStorage_invalidAccess() throws Throwable {
        writeToInternalStorage("/some/path/to/file_1.txt", TEST_STRING);
        writeToInternalStorage("/some/path/file_2.txt", TEST_STRING);

        // Should fail with a FileOutsideMountedDirectoryException thrown as it is accessing a file
        // outside the parent directory.
        InputStream stream =
                mAssetHelper.openFile(new File(mInternalStorageTestDir, "/some/path/to"),
                        "/../file_2.txt");
    }

    private static String readAsString(InputStream is) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[512];
        int len = 0;
        try {
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            return new String(os.toByteArray(), "utf-8");
        } catch (IOException e) {
            Log.e(TAG, "exception when reading the string", e);
            return "";
        }
    }

    // star.svg and star.svgz contain the same data. AssetHelper should decompress the
    // svgz automatically. Load both from assets and assert that they're equal.
    @Test
    @SmallTest
    public void testSvgzAsset() throws IOException {
        InputStream svgStream = null;
        InputStream svgzStream = null;
        try {
            svgStream = assertOpen(Uri.parse("star.svg"));
            byte[] expectedData = readFully(svgStream);

            svgzStream = assertOpen(Uri.parse("star.svgz"));
            byte[] actualData = readFully(svgzStream);

            Assert.assertArrayEquals(
                    "Decompressed star.svgz doesn't match star.svg", expectedData, actualData);
        } finally {
            if (svgStream != null) svgStream.close();
            if (svgzStream != null) svgzStream.close();
        }
    }

    private InputStream assertOpen(Uri uri) {
        InputStream stream = mAssetHelper.openAsset(uri);
        Assert.assertNotNull("Failed to open \"" + uri + "\"", stream);
        return stream;
    }

    private byte[] readFully(InputStream stream) throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        for (;;) {
            int len = stream.read(buf);
            if (len < 1) break;
            data.write(buf, 0, len);
        }
        return data.toByteArray();
    }

    private void writeToInternalStorage(String path, String content)
                  throws IOException {
        File file = new File(mInternalStorageTestDir, path);
        File parentDir = new File(file.getParent());
        parentDir.mkdirs();
        FileOutputStream fos = new FileOutputStream(file);
        try {
            fos.write(content.getBytes("utf-8"));
        } finally {
            fos.close();
        }
    }

    /**
     * Delete the given File and (if it's a directory) everything within it.
     * @param currentFile The file or directory to delete. Does not need to exist.
     * @return Whether currentFile does not exist afterwards.
     */
    private static boolean recursivelyDeleteFile(File currentFile) {
        if (!currentFile.exists()) {
            return true;
        }
        if (currentFile.isDirectory()) {
            File[] files = currentFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    recursivelyDeleteFile(file);
                }
            }
        }

        boolean ret = currentFile.delete();
        if (!ret) {
            Log.e(TAG, "Failed to delete: " + currentFile);
        }
        return ret;
    }
}
