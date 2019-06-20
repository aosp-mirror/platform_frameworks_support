<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
=======
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

package androidx.camera.core.impl.utils.futures;

import static java.util.concurrent.atomic.AtomicIntegerFieldUpdater.newUpdater;
import static java.util.concurrent.atomic.AtomicReferenceFieldUpdater.newUpdater;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A helper which does some thread-safe operations for aggregate futures, which must be implemented
 * differently in GWT. Namely:
 *
 * <ul>
 * <li>Lazily initializes a set of seen exceptions
 * <li>Decrements a counter atomically
 * </ul>
 *
 * <p>Copied and adapted from Guava.
 */
@SuppressWarnings("unchecked") // Casts verified
abstract class AggregateFutureState {
    // Lazily initialized the first time we see an exception; not released until all the input
    // futures & this future completes. Released when the future releases the reference to the
    // running state
    @SuppressWarnings("WeakerAccess") // Avoiding synthetic accessor.
    volatile Set<Throwable> mSeenExceptions = null;

    @SuppressWarnings("WeakerAccess") // Avoiding synthetic accessor.
    volatile int mRemaining;

    private static final AtomicHelper ATOMIC_HELPER;

    private static final Logger sLogger = Logger.getLogger(AggregateFutureState.class.getName());

    static {
        AtomicHelper helper;
        Throwable thrownReflectionFailure = null;
        try {
            helper =
                    new SafeAtomicHelper(
                            newUpdater(AggregateFutureState.class, (Class) Set.class,
                                    "mSeenExceptions"),
                            newUpdater(AggregateFutureState.class, "mRemaining"));
        } catch (Throwable reflectionFailure) {
            // Some Android 5.0.x Samsung devices have bugs in JDK reflection APIs that cause
            // getDeclaredField to throw a NoSuchFieldException when the field is definitely there.
            // For these users fallback to a suboptimal implementation, based on synchronized.
            // This will be a definite performance hit to those users.
            thrownReflectionFailure = reflectionFailure;
            helper = new SynchronizedAtomicHelper();
        }
        ATOMIC_HELPER = helper;
        // Log after all static init is finished; if an installed logger uses any Futures
        // methods, it shouldn't break in cases where reflection is missing/broken.
        if (thrownReflectionFailure != null) {
            sLogger.log(Level.SEVERE, "SafeAtomicHelper is broken!", thrownReflectionFailure);
        }
    }

    AggregateFutureState(int remainingFutures) {
        this.mRemaining = remainingFutures;
    }

    final Set<Throwable> getOrInitSeenExceptions() {
        /*
         * The initialization of mSeenExceptions has to be more complicated than we'd like. The
         * simple approach would be for each caller CAS it from null to a Set populated with its
         * exception. But there's another race: If the first thread fails with an exception and a
         * second thread immediately fails with the same exception:
         *
         * Thread1: calls setException(), which returns true, context switch before it can CAS
         * mSeenExceptions to its exception
         *
         * Thread2: calls setException(), which returns false, CASes mSeenExceptions to its
         * exception, and wrongly believes that its exception is new (leading it to logging it when
         * it shouldn't)
         *
         * Our solution is for threads to CAS mSeenExceptions from null to a Set population with
         * the initial exception, no matter which thread does the work. This ensures that
         * mSeenExceptions always contains not just the current thread's exception but also the
         * initial thread's.
         */
        Set<Throwable> seenExceptionsLocal = mSeenExceptions;
        if (seenExceptionsLocal == null) {
            ConcurrentHashMap<Throwable, Boolean> backingMap = new ConcurrentHashMap<>();
            seenExceptionsLocal = Collections.newSetFromMap(backingMap);
            /*
             * Other handleException() callers may see this as soon as we publish it. We need to
             * populate it with the initial failure before we do, or else they may think that the
             * initial failure has never been seen before.
             */
            addInitialException(seenExceptionsLocal);

            ATOMIC_HELPER.compareAndSetSeenExceptions(this, null, seenExceptionsLocal);
            /*
             * If another handleException() caller created the set, we need to use that copy in
             * case yet other callers have added to it.
             *
             * This read is guaranteed to get us the right value because we only set this once
             * (here).
             */
            seenExceptionsLocal = mSeenExceptions;
        }
        return seenExceptionsLocal;
    }

    /** Populates {@code seen} with the exception that was passed to {@code setException}. */
    abstract void addInitialException(Set<Throwable> seen);

    final int decrementRemainingAndGet() {
        return ATOMIC_HELPER.decrementAndGetRemainingCount(this);
    }

    private abstract static class AtomicHelper {
        /** Atomic compare-and-set of the {@link AggregateFutureState#mSeenExceptions} field. */
        abstract void compareAndSetSeenExceptions(
                AggregateFutureState state, Set<Throwable> expect, Set<Throwable> update);

        /** Atomic decrement-and-get of the {@link AggregateFutureState#mRemaining} field. */
        abstract int decrementAndGetRemainingCount(AggregateFutureState state);
    }

    private static final class SafeAtomicHelper extends AtomicHelper {
        final AtomicReferenceFieldUpdater<AggregateFutureState, Set<Throwable>>
                mSeenExceptionsUpdater;

        final AtomicIntegerFieldUpdater<AggregateFutureState> mRemainingCountUpdater;

        SafeAtomicHelper(
                AtomicReferenceFieldUpdater<AggregateFutureState, Set<Throwable>> exceptionsUpdater,
                AtomicIntegerFieldUpdater<AggregateFutureState> remainingCountUpdater) {
            this.mSeenExceptionsUpdater = exceptionsUpdater;
            this.mRemainingCountUpdater = remainingCountUpdater;
        }

        @Override
        void compareAndSetSeenExceptions(
                AggregateFutureState state, Set<Throwable> expect, Set<Throwable> update) {
            mSeenExceptionsUpdater.compareAndSet(state, expect, update);
        }

        @Override
        int decrementAndGetRemainingCount(AggregateFutureState state) {
            return mRemainingCountUpdater.decrementAndGet(state);
        }
    }

    static final class SynchronizedAtomicHelper extends AtomicHelper {
        @Override
        void compareAndSetSeenExceptions(
                AggregateFutureState state, Set<Throwable> expect, Set<Throwable> update) {
            synchronized (state) {
                if (state.mSeenExceptions == expect) {
                    state.mSeenExceptions = update;
                }
            }
        }

        @Override
        int decrementAndGetRemainingCount(AggregateFutureState state) {
            synchronized (state) {
                state.mRemaining--;
                return state.mRemaining;
            }
        }
    }
}
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
