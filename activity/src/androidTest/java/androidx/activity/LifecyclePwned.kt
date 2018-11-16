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

package androidx.activity

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import java.lang.ref.WeakReference


fun ComponentActivity.addOnBackPressedCallback1(
    owner: LifecycleOwner,
    callback: () -> Boolean
) : Subscription {
    if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
        return object : Subscription {}
    }

    val handler = OnBackPressedCallback {
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            callback()
        } else {
            false
        }
    }
    addOnBackPressedCallback(handler)
    return SubscriptionImpl(this, owner.lifecycle, handler)
}

interface Subscription {
    fun cancel() {}
}

class SubscriptionImpl(
    private val componentActivity: ComponentActivity,
    private val lifecycle: Lifecycle,
    private val backPressedCallback: OnBackPressedCallback
): Subscription {

    private val lifecycleObserver = object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            cancel()
        }
    }

    init {
        lifecycle.addObserver(lifecycleObserver)
    }

    override fun cancel() {
        lifecycle.removeObserver(lifecycleObserver)
        componentActivity.removeOnBackPressedCallback(backPressedCallback)
    }
}



fun ComponentActivity.addOnBackPressedCallback2(
    owner: LifecycleOwner,
    callback: () -> Boolean
): OnBackPressedCallback? {
    if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
        return null
    }
    val handler = OnBackPressedCallback {
        if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            callback()
        } else {
            false
        }
    }
    addOnBackPressedCallback(handler)
    owner.lifecycle.addObserver(WeakObserver(handler, this))
    return handler
}

class WeakObserver(handler: OnBackPressedCallback, componentActivity: ComponentActivity): LifecycleObserver {
    private val weakHandler = WeakReference(handler)
    private val weakActivity = WeakReference(componentActivity)

    @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
    fun onEvent(owner: LifecycleOwner, event: Lifecycle.Event) {
        val handler = weakHandler.get()
        val activity = weakActivity.get()
        if (handler == null) {
            owner.lifecycle.removeObserver(this)
        } else if (event == Lifecycle.Event.ON_DESTROY && activity != null) {
            activity.removeOnBackPressedCallback(handler)
        }
    }
}