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

package androidx.ui.flutter_test

import androidx.ui.core.Duration
import androidx.ui.engine.geometry.Offset
import androidx.ui.gestures.events.PointerCancelEvent
import androidx.ui.gestures.events.PointerDownEvent
import androidx.ui.gestures.events.PointerMoveEvent
import androidx.ui.gestures.events.PointerUpEvent

// / A class for generating coherent artificial pointer events.
// /
// / You can use this to manually simulate individual events, but the
// / simplest way to generate coherent gestures is to use [TestGesture].
// /
// / Multiple [TestPointer]s created with the same pointer identifier will
// / interfere with each other if they are used in parallel.
class TestPointer(
    // / The pointer identifier used for events generated by this object.
    // /
    // / Set when the object is constructed. Defaults to 1.
    private val pointer: Int = 1
) {

    // / Whether the pointer simulated by this object is currently down.
    // /
    // / A pointer is released (goes up) by calling [up] or [cancel].
    // /
    // / Once a pointer is released, it can no longer generate events.
    var isDown: Boolean = false
        private set

    // / The position of the last event sent by this object.
    // /
    // / If no event has ever been sent by this object, returns null.
    var location: Offset? = null
        private set

    // / Create a [PointerDownEvent] at the given location.
    // /
    // / By default, the time stamp on the event is [Duration.zero]. You
    // / can give a specific time stamp by passing the `timeStamp`
    // / argument.
    fun down(newLocation: Offset, timeStamp: Duration = Duration.zero): PointerDownEvent {
        assert(!isDown)
        isDown = true
        location = newLocation
        return PointerDownEvent(
            timeStamp = timeStamp,
            pointer = pointer,
            position = newLocation
        )
    }

    // / Create a [PointerMoveEvent] to the given location.
    // /
    // / By default, the time stamp on the event is [Duration.zero]. You
    // / can give a specific time stamp by passing the `timeStamp`
    // / argument.
    fun move(newLocation: Offset, timeStamp: Duration = Duration.zero): PointerMoveEvent {
        assert(isDown)
        val delta: Offset = newLocation - location!!
        location = newLocation
        return PointerMoveEvent(
            timeStamp = timeStamp,
            pointer = pointer,
            position = newLocation,
            delta = delta
        )
    }

    // / Create a [PointerUpEvent].
    // /
    // / By default, the time stamp on the event is [Duration.zero]. You
    // / can give a specific time stamp by passing the `timeStamp`
    // / argument.
    // /
    // / The object is no longer usable after this method has been called.
    fun up(timeStamp: Duration = Duration.zero): PointerUpEvent {
        assert(isDown)
        isDown = false
        return PointerUpEvent(
            timeStamp = timeStamp,
            pointer = pointer,
            position = location!!
        )
    }

    // / Create a [PointerCancelEvent].
    // /
    // / By default, the time stamp on the event is [Duration.zero]. You
    // / can give a specific time stamp by passing the `timeStamp`
    // / argument.
    // /
    // / The object is no longer usable after this method has been called.
    fun cancel(timeStamp: Duration = Duration.zero): PointerCancelEvent {
        assert(isDown)
        isDown = false
        return PointerCancelEvent(
            timeStamp = timeStamp,
            pointer = pointer,
            position = location!!
        )
    }
}
