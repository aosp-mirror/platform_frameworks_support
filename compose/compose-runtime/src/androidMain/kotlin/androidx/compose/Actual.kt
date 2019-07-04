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

package androidx.compose

actual typealias Stack<E> = java.util.Stack<E>

actual typealias BitSet = java.util.BitSet

actual open class ThreadLocal<T> actual constructor(): java.lang.ThreadLocal<T>() {
    actual override fun get(): T? {
        return super.get()
    }

    actual override fun set(value: T?) {
        super.set(value)
    }

    actual override fun initialValue(): T? {
        return super.initialValue()
    }
}

actual typealias WeakHashMap<K, V> = java.util.WeakHashMap<K, V>

internal actual fun arraycopy(source: Any, sourcePos: Int, dest: Any, destPos: Int, len: Int)
        = System.arraycopy(source, sourcePos, dest, destPos, len)

actual fun identityHashCode(instance: Any?): Int = System.identityHashCode(instance)

actual typealias ViewParent = android.view.ViewParent

actual typealias View = android.view.View

actual typealias ViewGroup = android.view.ViewGroup

actual typealias Activity = android.app.Activity

actual typealias Context = android.content.Context

actual typealias FrameLayout = android.widget.FrameLayout

actual fun Activity.getRootView(): ViewGroup? {
    return window
        .decorView
        .findViewById<ViewGroup>(android.R.id.content)
        .getChildAt(0) as? ViewGroup
}

actual fun Activity.getRootViewWithFallback(): ViewGroup {
    return getRootView() ?: FrameLayout(this).also { setContentView(it) }
}

actual inline fun <R> synchronized(lock: Any, block: () -> R): R {
    kotlin.synchronized(lock) {
        return block()
    }
}

actual typealias Reference<T> = java.lang.ref.Reference<T>

actual typealias WeakReference<T> = java.lang.ref.WeakReference<T>

actual typealias Looper = android.os.Looper

actual object LooperWrapper {
    actual fun myLooper(): Looper? = android.os.Looper.myLooper()
    actual fun getMainLooper(): Looper = android.os.Looper.getMainLooper()
}

actual class Handler {
    val handler: android.os.Handler

    actual constructor(looper: Looper) {
        handler = android.os.Handler(looper)
    }
    actual fun postAtFrontOfQueue(block: () -> Unit): Boolean {
        return handler.postAtFrontOfQueue { block() }
    }
}

actual object Choreographer {
    actual fun postFrameCallback(callback: (Long) -> Unit) {
        android.view.Choreographer.getInstance().postFrameCallback(callback)
    }
    actual fun postFrameCallbackDelayed(delayMillis: Long, callback: (Long) -> Unit) {
        android.view.Choreographer.getInstance().postFrameCallbackDelayed(callback, delayMillis)
    }
    actual fun removeFrameCallback(callback: (Long) -> Unit) {
        android.view.Choreographer.getInstance().removeFrameCallback(callback)
    }
}

actual typealias MainThread = androidx.annotation.MainThread

actual typealias TestOnly = org.jetbrains.annotations.TestOnly

actual typealias CheckResult = androidx.annotation.CheckResult

actual object Trace {
    actual fun beginSection(name: String) = android.os.Trace.beginSection(name)
    actual fun endSection() = android.os.Trace.endSection()
}