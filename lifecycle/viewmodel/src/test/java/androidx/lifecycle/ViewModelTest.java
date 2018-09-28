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

package androidx.lifecycle;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.Closeable;

@RunWith(JUnit4.class)
public class ViewModelTest {

    @Test
    public void testCloseableTag() {
        class CloseableImpl implements Closeable {
            boolean mWasClosed;
            @Override
            public void close() {
                mWasClosed = true;
            }
        }

        ViewModel vm = new ViewModel() {};
        CloseableImpl impl = new CloseableImpl();
        vm.setTagIfAbsent("totally_not_coroutine_context", impl);
        vm.clear();
        Assert.assertTrue(impl.mWasClosed);
    }
}
