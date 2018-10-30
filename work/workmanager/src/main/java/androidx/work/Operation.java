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

package androidx.work;

import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * An operation being performed by {@link WorkManager} on a dedicated background thread.
 */
public interface Operation {

    /**
     * Gets a {@link LiveData} of the Operation {@link State}.
     *
     * @return A {@link LiveData} of the Operation {@link State}.
     */
    LiveData<State> getStatus();

    /**
     * Gets the result of the {@link Operation}. This represents the *terminal* state of the
     * {@link Operation}.
     *
     * @return a {@link ListenableFuture} of the {@link Operation}s {@link State}.
     */
    ListenableFuture<State> getResult();

    /**
     * The {@link Operation} state.
     */
    abstract class State {

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        State() {
            // Restricting access to the constructor, to give Operation.State a sealed class
            // like behavior.
        }

        /**
         * This represents an {@link Operation} which is successful.
         */
        public static final class Successful extends Operation.State {

            /**
             * The only instance of Successful that one might need.
             */
            public static final Successful INSTANCE = new Successful();

            private Successful() {
                super();
            }

            @Override
            @NonNull
            public String toString() {
                return "Operation.Successful";
            }
        }

        /**
         * This represents an {@link Operation} which is in progress.
         */
        public static final class InProgress extends Operation.State {
            /**
             * The only instance of InProgress that one might need.
             */
            public static final InProgress INSTANCE = new InProgress();

            private InProgress() {
                super();
            }

            @Override
            @NonNull
            public String toString() {
                return "Operation.InProgress";
            }
        }

        /**
         * This represents an {@link Operation} which has failed.
         */
        public static final class Failed extends Operation.State {

            private final Throwable mThrowable;

            public Failed(@NonNull Throwable exception) {
                super();
                mThrowable = exception;
            }

            /**
             * @return The {@link Throwable} which caused the {@link Operation} to fail.
             */
            @NonNull
            public Throwable getException() {
                return mThrowable;
            }

            @Override
            @NonNull
            public String toString() {
                return String.format("Operation.Failed (%s)", mThrowable.getMessage());
            }
        }
    }
}
