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

package androidx.work.foreground;

import android.app.Notification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Metadata used for surfacing a user visible {@link android.app.Notification}.
 */
public class NotificationMetadata {

    private final String mTag;
    private final int mId;
    private final Notification mNotification;

    // Synthetic access
    NotificationMetadata(@NonNull Builder builder) {
        mTag = builder.getTag();
        mId = builder.getId();
        mNotification = builder.getNotification();
    }

    /**
     * @return The tag used to identify a {@link Notification}.
     */
    @Nullable
    public String getTag() {
        return mTag;
    }

    /**
     * @return The {@link Notification} id.
     */
    public int getId() {
        return mId;
    }

    /**
     * @return The user visible {@link Notification}
     */
    @NonNull
    public Notification getNotification() {
        return mNotification;
    }

    /**
     * A {@link NotificationMetadata} Builder.
     */
    public static class Builder {
        @Nullable
        private String mTag;
        private int mId;
        private Notification mNotification;

        /**
         * @return The tag used to identify a {@link Notification}
         */
        @Nullable
        public String getTag() {
            return mTag;
        }

        /**
         * @param tag The tag used to identify a {@link Notification}
         * @return The instance of {@link Builder} for chaining
         */
        public Builder setTag(@NonNull String tag) {
            mTag = tag;
            return this;
        }

        /**
         * @return The {@link Notification} id
         */
        public int getId() {
            return mId;
        }

        /**
         * @param id The {@link Notification} id
         * @return The instance of {@link Builder} for chaining
         */
        public Builder setId(int id) {
            mId = id;
            return this;
        }

        /**
         * @return The user visible {@link Notification}
         */
        @NonNull
        public Notification getNotification() {
            return mNotification;
        }

        /**
         * @param notification The user visible {@link Notification}
         * @return The instance of {@link Builder} for chaining
         */
        public Builder setNotification(@NonNull Notification notification) {
            mNotification = notification;
            return this;
        }

        /**
         * @return The {@link NotificationMetadata}
         */
        @NonNull
        public NotificationMetadata build() {
            return new NotificationMetadata(this);
        }
    }
}
