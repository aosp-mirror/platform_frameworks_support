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

package androidx.lifecycle

import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@SmallTest
class LifecycleRegistryTest {

    private lateinit var lifecycleOwner: LifecycleOwner
    private lateinit var lifecycle: Lifecycle
    private lateinit var registry: LifecycleRegistry

    @Before
    fun init() {
        lifecycleOwner = mock(LifecycleOwner::class.java)
        lifecycle = mock(Lifecycle::class.java)
        `when`(lifecycleOwner.lifecycle).thenReturn(lifecycle)
        registry = LifecycleRegistry(lifecycleOwner)
    }

    @Test
    fun getState() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        assertThat(registry.state).isEqualTo(Lifecycle.State.RESUMED)

        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        assertThat(registry.state).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @Test
    fun setState() {
        registry.state = Lifecycle.State.RESUMED
        assertThat(registry.currentState).isEqualTo(Lifecycle.State.RESUMED)

        registry.state = Lifecycle.State.DESTROYED
        assertThat(registry.currentState).isEqualTo(Lifecycle.State.DESTROYED)
    }
}
