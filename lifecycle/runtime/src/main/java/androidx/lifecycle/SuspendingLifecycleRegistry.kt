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

package androidx.lifecycle

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import java.lang.IllegalStateException
import java.lang.RuntimeException

class SuspendingLifecycleRegistry internal constructor(
    private val ownerThreadId: Long,
    private val lifecycleRegistry: LifecycleRegistry,
    private val channel: SendChannel<LifecycleMessage>
) : Lifecycle() {

    override fun removeObserver(observer: LifecycleObserver) {
        checkCallingThread("removeObserver")
        lifecycleRegistry.removeObserver(observer)
    }

    override fun getCurrentState(): State {
        return lifecycleRegistry.currentState
    }

    suspend fun addObserverSuspending(observer: LifecycleObserver) {
        val message = LifecycleMessage.AddObserver(observer)
        try {
            channel.send(message)
        } catch (e: ClosedSendChannelException) {
            // swallow exception, channel is closed once lifecycle is destroyed, no reason add new observers
            return
        }

        message.result.await()
    }

    suspend fun removeObserverSuspending(observer: LifecycleObserver) {
        val message = LifecycleMessage.RemoveObserver(observer)
        try {
            channel.send(message)
        } catch (e: ClosedSendChannelException) {
            // swallow exception, channel is closed once lifecycle is destroyed, no reason to
            // try to remove
            return
        }
        message.result.await()
    }

    suspend fun handleEvent(event: Lifecycle.Event) {
        val message = LifecycleMessage.HandleLifecycleEvent(event)
        channel.send(message)
        message.result.await()
    }

    override fun addObserver(observer: LifecycleObserver) {
        checkCallingThread("addObserver")
        lifecycleRegistry.addObserver(observer)
    }

    private fun checkCallingThread(callname: String) {
        val callingThreadId = Thread.currentThread().id
        if (ownerThreadId != callingThreadId) {
            throw IllegalStateException(
                "Method $callname can not be called only from " +
                        "thread with id $callingThreadId"
            )
        }
    }
}

internal sealed class LifecycleMessage {
    val result: CompletableDeferred<Unit> = CompletableDeferred()

    class AddObserver(val observer: LifecycleObserver) : LifecycleMessage()
    class RemoveObserver(val observer: LifecycleObserver) : LifecycleMessage()
    class HandleLifecycleEvent(val event: Lifecycle.Event) : LifecycleMessage()
}

fun SuspendingLifecycleRegistry(owner: LifecycleOwner): SuspendingLifecycleRegistry {
    val looper = Looper.myLooper()
        ?: throw IllegalStateException("Can not create SuspendingLifecycleRegistry " +
                "on thread without handler")
    // TODO: is exception handler needed?
    val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        throw RuntimeException("Internal error in SuspendingLifecycleRegistry", throwable)
    }
    val registry = LifecycleRegistry(owner)
    val channel = Channel<LifecycleMessage>()
    registry.addObserver(LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) channel.close()
    })
    val scope = CoroutineScope(Job() + Handler(looper).asCoroutineDispatcher() + exceptionHandler)
    scope.launch {
        for (msg in channel) {
            when (msg) {
                is LifecycleMessage.AddObserver -> registry.addObserver(msg.observer)
                is LifecycleMessage.RemoveObserver -> registry.addObserver(msg.observer)
                is LifecycleMessage.HandleLifecycleEvent -> registry.handleLifecycleEvent(msg.event)
            }
            msg.result.complete(Unit)
        }
    }
    return SuspendingLifecycleRegistry(Thread.currentThread().id, registry, channel)
}