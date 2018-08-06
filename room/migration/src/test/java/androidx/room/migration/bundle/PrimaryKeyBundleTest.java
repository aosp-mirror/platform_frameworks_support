/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.room.migration.bundle;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

@RunWith(JUnit4.class)
public class PrimaryKeyBundleTest {
    @Test
    public void schemaEquality_same_equal() {
        PrimaryKeyBundle bundle = new PrimaryKeyBundle(true,
                Arrays.asList("foo", "bar"));
        PrimaryKeyBundle other = new PrimaryKeyBundle(true,
                Arrays.asList("foo", "bar"));
        assertThat(bundle.isSchemaEqual(other), is(true));
    }

    @Test
    public void schemaEquality_diffAutoGen_notEqual() {
        PrimaryKeyBundle bundle = new PrimaryKeyBundle(true,
                Arrays.asList("foo", "bar"));
        PrimaryKeyBundle other = new PrimaryKeyBundle(false,
                Arrays.asList("foo", "bar"));
        assertThat(bundle.isSchemaEqual(other), is(false));
    }

    @Test
    public void schemaEquality_diffColumns_notEqual() {
        PrimaryKeyBundle bundle = new PrimaryKeyBundle(true,
                Arrays.asList("foo", "baz"));
        PrimaryKeyBundle other = new PrimaryKeyBundle(true,
                Arrays.asList("foo", "bar"));
        assertThat(bundle.isSchemaEqual(other), is(false));
    }

    @Test
    public void schemaEquality_diffColumnOrder_notEqual() {
        PrimaryKeyBundle bundle = new PrimaryKeyBundle(true,
                Arrays.asList("foo", "bar"));
        PrimaryKeyBundle other = new PrimaryKeyBundle(true,
                Arrays.asList("bar", "foo"));
        assertThat(bundle.isSchemaEqual(other), is(false));
    }
}
