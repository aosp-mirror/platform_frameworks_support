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

import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATE_DATA;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATE_KEY;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATE_MESSAGE;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.APP_STATE_SEVERITY;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.enterprise.feedback.KeyedAppState.Severity;

import com.google.auto.value.AutoValue;

/**
 * A keyed app state received from an app. This contains all of the information added by the app to
 * the {@link KeyedAppState} as well as the {@link #packageName()} and {@link #timestamp()} added
 * when the state was received.
 */
@AutoValue
public abstract class ReceivedKeyedAppState {

    /** Create a {@link ReceivedKeyedAppStateBuilder}. */
    public static ReceivedKeyedAppStateBuilder builder() {
        return new AutoValue_ReceivedKeyedAppState.Builder();
    }

    /** Assumes {@link KeyedAppState#isValid(Bundle)}. */
    static ReceivedKeyedAppState fromBundle(Bundle bundle, String packageName, long timestamp) {
        if (!KeyedAppState.isValid(bundle)) {
            throw new IllegalArgumentException("Bundle is not valid");
        }

        return builder()
                .setPackageName(packageName)
                .setTimestamp(timestamp)
                .setKey(bundle.getString(APP_STATE_KEY))
                .setSeverity(bundle.getInt(APP_STATE_SEVERITY))
                .setMessage(bundle.getString(APP_STATE_MESSAGE))
                .setData(bundle.getString(APP_STATE_DATA))
                .build();
    }

    /**
     * The name of the package which submitted the states.
     *
     * <p>This is automatically set to the correct value by the receiver; it is NOT self-reported by
     * the app sending the feedback.
     */
    public abstract String packageName();

    /**
     * The unix timestamp, in milliseconds, when the states were received.
     *
     * <p>This is automatically set to the correct value by the receiver; it is NOT self-reported by
     * the app sending the feedback.
     */
    public abstract long timestamp();

    /** See {@link KeyedAppState#key()} */
    public abstract String key();

    /** See {@link KeyedAppState#severity()} */
    public abstract int severity();

    /** See {@link KeyedAppState#message()} */
    @Nullable
    public abstract String message();

    /** See {@link KeyedAppState#data()} */
    @Nullable
    public abstract String data();

    /** The builder for {@link ReceivedKeyedAppState}. */
    @AutoValue.Builder
    public abstract static class ReceivedKeyedAppStateBuilder {

        /** Set {@link ReceivedKeyedAppState#packageName()}. */
        public abstract ReceivedKeyedAppStateBuilder setPackageName(String packageName);

        /** Set {@link ReceivedKeyedAppState#timestamp()}. */
        public abstract ReceivedKeyedAppStateBuilder setTimestamp(long timestamp);

        /** Set {@link ReceivedKeyedAppState#key()}. */
        public abstract ReceivedKeyedAppStateBuilder setKey(String key);

        /** Set {@link ReceivedKeyedAppState#severity()}. */
        public abstract ReceivedKeyedAppStateBuilder setSeverity(@Severity int severity);

        /** Set {@link ReceivedKeyedAppState#message()}. */
        public abstract ReceivedKeyedAppStateBuilder setMessage(String message);

        /** Set {@link ReceivedKeyedAppState#data()}. */
        public abstract ReceivedKeyedAppStateBuilder setData(String data);

        /**
         * Instantiate the {@link ReceivedKeyedAppState}.
         *
         * <p>Assumes the key and severity are set.
         */
        public abstract ReceivedKeyedAppState build();
    }
}
