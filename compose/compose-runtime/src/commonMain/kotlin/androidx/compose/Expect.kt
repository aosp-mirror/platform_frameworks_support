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

expect class Stack<E>() {
    fun push(item: E): E
    fun pop(): E
    fun clear()
    fun peek(): E
    fun isEmpty(): Boolean
    val size: Int
    operator fun get(index: Int): E
}

expect class BitSet() {
    fun set(bitIndex: Int)
    fun or(set: BitSet)
    fun clear(bitIndex: Int)
    operator fun get(bitIndex: Int): Boolean
}

expect open class ThreadLocal<T>() {
    fun get(): T?
    fun set(value: T?)
    protected open fun initialValue(): T?
}

expect class WeakHashMap<K, V>(): MutableMap<K, V>

internal expect fun arraycopy(source: Any, sourcePos: Int, dest: Any, destPos: Int, len: Int)

expect fun identityHashCode(instance: Any?): Int

expect interface ViewParent

expect open class View {
    fun getParent(): ViewParent
    fun getContext(): Context
    fun getTag(key: Int): Any
    fun setTag(key: Int, tag: Any?)
}

expect abstract class ViewGroup: View {
    fun removeAllViews()
}

expect class Activity

expect abstract class Context

expect class FrameLayout(context: Context)

expect fun Activity.getRootView(): ViewGroup?

expect fun Activity.getRootViewWithFallback(): ViewGroup

expect inline fun <R> synchronized(lock: Any, block: () -> R): R

expect abstract class Reference<T> {
    fun get(): T?
}

expect class WeakReference<T>(instance: T): Reference<T>

expect class Looper

expect object LooperWrapper {
    fun myLooper(): Looper?
    fun getMainLooper(): Looper
}

expect class Handler(looper: Looper) {
    fun postAtFrontOfQueue(block: () -> Unit): Boolean
}

expect object Choreographer {
    fun postFrameCallback(callback: (Long) -> Unit)
    fun postFrameCallbackDelayed(delayMillis: Long, callback: (Long) -> Unit)
    fun removeFrameCallback(callback: (Long) -> Unit)
}

@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CONSTRUCTOR
)
expect annotation class MainThread()

@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.CONSTRUCTOR
)
expect annotation class TestOnly()

@Target(AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
expect annotation class CheckResult(
    val suggest: String
)