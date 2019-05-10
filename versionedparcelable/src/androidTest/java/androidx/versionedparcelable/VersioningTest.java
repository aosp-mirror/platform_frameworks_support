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

package androidx.versionedparcelable;

import static org.junit.Assert.assertEquals;

import androidx.collection.ArrayMap;
import androidx.test.filters.SmallTest;
import androidx.versionedparcelable.testclasses.ClassA_Version1;
import androidx.versionedparcelable.testclasses.ClassA_Version2;
import androidx.versionedparcelable.testclasses.ClassB_Version1;
import androidx.versionedparcelable.testclasses.ClassB_Version2;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;

@SmallTest
public class VersioningTest {

    @Test
    public void testAddNewFieldWithLowerIdValue() {
        ClassA_Version1 v1 = new ClassA_Version1(42);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ParcelUtils.toOutputStream(
                v1, outputStream,
                getReadCache(ClassA_Version1.class, ClassA_Version2.class),
                getWriteCache(ClassA_Version1.class, ClassA_Version2.class),
                getVersionMap(ClassA_Version1.class, ClassA_Version2.class));

        byte[] buf = outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(buf);
        ClassA_Version2 v2 = ParcelUtils.fromInputStream(inputStream);

        assertEquals(42, v2.getSomeValue());
        assertEquals(0, v2.getSomeOtherValue());
    }

    @Test
    public void testAddNewFieldInListItem() {
        ClassA_Version1 e1 = new ClassA_Version1(42);
        ClassA_Version1 e2 = new ClassA_Version1(24);
        ClassB_Version1 v1 = new ClassB_Version1(new ClassA_Version1[] { e1, e2 });

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ParcelUtils.toOutputStream(
                v1, outputStream,
                getReadCache(
                        ClassA_Version1.class, ClassA_Version2.class,
                        ClassB_Version1.class, ClassB_Version2.class),
                getWriteCache(
                        ClassA_Version1.class, ClassA_Version2.class,
                        ClassB_Version1.class, ClassB_Version2.class),
                getVersionMap(
                        ClassA_Version1.class, ClassA_Version2.class,
                        ClassB_Version1.class, ClassB_Version2.class));

        byte[] buf = outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(buf);
        ClassB_Version2 v2 = ParcelUtils.fromInputStream(inputStream);

        assertEquals(2, v2.getItems().length);
        assertEquals(42, v2.getItems()[0].getSomeValue());
        assertEquals(0, v2.getItems()[0].getSomeOtherValue());
        assertEquals(24, v2.getItems()[1].getSomeValue());
        assertEquals(0, v2.getItems()[1].getSomeOtherValue());
    }

    private static ArrayMap<String, Class> getVersionMap(Class<?>... classes) {
        ArrayMap<String, Class> parcelizerMap = new ArrayMap<>();
        try {
            for (int i = 0; i < classes.length; i += 2) {
                parcelizerMap.put(
                        classes[i].getName(),
                        Class.forName(classes[i + 1].getName() + "Parcelizer"));
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return parcelizerMap;
    }

    private static ArrayMap<String, Method> getReadCache(Class<?>... classes) {
        ArrayMap<String, Method> cache = new ArrayMap<>();
        try {
            for (int i = 0; i < classes.length; i += 2) {
                Class cls = Class.forName(classes[i + 1].getName() + "Parcelizer");
                cache.put(
                        classes[i].getName(),
                        cls.getDeclaredMethod("read", VersionedParcel.class));
            }
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return cache;
    }

    private static ArrayMap<String, Method> getWriteCache(Class<?>... classes) {
        ArrayMap<String, Method> cache = new ArrayMap<>();
        try {
            for (int i = 0; i < classes.length; i += 2) {
                Class cls = Class.forName(classes[i].getName() + "Parcelizer");
                cache.put(
                        classes[i].getName(),
                        cls.getDeclaredMethod("write", classes[i], VersionedParcel.class));
            }
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        return cache;
    }
}
