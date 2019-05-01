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

package androidx.enterprise.feedback;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.TestCase.fail;

import android.content.ContextWrapper;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests {@link KeyedAppStatesReporter}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = 21)
public class KeyedAppStatesReporterTest {
    private final ContextWrapper mContext = ApplicationProvider.getApplicationContext();

    @Test
    @SmallTest
    public void getInstance_nullContext_throwsNullPointerException() {
        SingletonKeyedAppStatesReporter.resetSingleton();
        try {
            KeyedAppStatesReporter.getInstance(null);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    @Test
    @SmallTest
    public void getInstance_returnsSingletonKeyedAppStatesReporter() {
        SingletonKeyedAppStatesReporter.resetSingleton();

        assertThat(KeyedAppStatesReporter.getInstance(mContext))
            .isInstanceOf(SingletonKeyedAppStatesReporter.class);
    }
}
