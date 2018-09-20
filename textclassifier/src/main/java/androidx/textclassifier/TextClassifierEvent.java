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

package androidx.textclassifier;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;

/**
 * A text classifier event.
 */
// TODO: Comprehensive javadoc.
// TODO: Implement parcelling via toBundle()/fromBundle().
public final class TextClassifierEvent {

    // TODO: Include constants.
    // TODO: Include TextClassificationSessionId.

    private final int mEventType;
    @Nullable private final String mEntityType;
    @Nullable private final Source mEventSource;
    @Nullable private final String mResultId;
    private final int mEventIndex;
    private final long mEventTime;
    private final long mDurationSincePreviousEvent;
    private final long mDurationSinceStartEvent;
    private final Bundle mBundle;

    // Smart selection.
    private final int mStart;
    private final int mEnd;
    private final int mSmartStart;
    private final int mSmartEnd;
    private final int mInvocationType;

    // Smart action.
    private final int mActionIndex;

    // Language detection.
    @Nullable private final String mLanguage;

    // Weaker access. SyntheticAccessor.
    TextClassifierEvent(
            int eventType,
            String entityType,
            Source eventSource,
            String resultId,
            int eventIndex,
            long eventTime,
            long durationSincePreviousEvent,
            long durationSinceStartEvent,
            Bundle bundle,
            int start,
            int end,
            int smartStart,
            int smartEnd,
            int invocationType,
            int actionIndex,
            String language) {
        mEventType = eventType;
        mEntityType = entityType;
        mEventSource = eventSource;
        mResultId = resultId;
        mEventIndex = eventIndex;
        mEventTime = eventTime;
        mDurationSincePreviousEvent = durationSincePreviousEvent;
        mDurationSinceStartEvent = durationSinceStartEvent;
        mBundle = bundle;
        mStart = start;
        mEnd = end;
        mSmartStart = smartStart;
        mSmartEnd = smartEnd;
        mInvocationType = invocationType;
        mActionIndex = actionIndex;
        mLanguage = language;
    }

    /**
     * Returns the event type. e.g.
     */
    public int getEventType() {
        return mEventType;
    }

    /**
     * Returns the entity type. e.g. {@link TextClassifier#TYPE_ADDRESS}.
     */
    @Nullable
    public String getEntityType() {
        return mEntityType;
    }

    /**
     * Returns the event source.
     */
    @Nullable
    public Source getEventSource() {
        return mEventSource;
    }

    /**
     * Returns the id of the text classifier result related to this event.
     */
    @Nullable
    public String getResultId() {
        return mResultId;
    }

    /**
     * Returns the index of this event in the series of event it belongs to.
     */
    public int getEventIndex() {
        return mEventIndex;
    }

    /**
     * Returns the time this event occurred. This is the number of milliseconds since
     * January 1, 1970, 00:00:00 GMT. 0 indicates not set.
     */
    public long getEventTime() {
        return mEventTime;
    }

    /**
     * Returns the number of milliseconds since the previous event. 0 indicates not set.
     */
    public long getDurationSincePreviousEvent() {
        return mDurationSincePreviousEvent;
    }

    /**
     * Returns the number of milliseconds since the event at index 0. 0 indicates not set.
     * @see #getEventIndex()
     */
    public long getDurationSinceStartEvent() {
        return mDurationSinceStartEvent;
    }

    /**
     * Returns a bundle containing non-structured extra information about this event.
     *
     * <p><b>NOTE: </b>Each call to this method returns a new bundle copy so clients should
     * prefer to hold a reference to the returned bundle rather than frequently calling this
     * method.
     *
     * <p><b>NOTE: </b>Do not modify the internals of this bundle.
     */
    @NonNull
    public Bundle getBundle() {
        return new Bundle(mBundle);
    }

    /**
     * For smart selection. Returns the relative word index of the start of the selection.
     */
    public int getStart() {
        return mStart;
    }

    /**
     * For smart selection. Returns the relative word index of the end of the selection.
     */
    public int getEnd() {
        return mEnd;
    }

    /**
     * For smart selection. Returns the relative word index of the start of the smart selection.
     */
    public int getSmartStart() {
        return mSmartStart;
    }

    /**
     * For smart selection. Returns the relative word index of the start of the smart selection.
     */
    public int getSmartEnd() {
        return mSmartEnd;
    }

    /**
     * Returns the invocation type. e.g.
     */
    public int getInvocationType() {
        return mInvocationType;
    }

    /**
     * Returns the index of the action relating to this event. Actions are usually returned by the
     * text classifier in priority order with the most preferred action at index 0. This index
     * gives an indication of the position of the action that is being reported.
     */
    public int getActionIndex() {
        return mActionIndex;
    }

    /**
     * For language detection. Returns the language tag for the detected locale.
     * @see java.util.Locale#forLanguageTag(String).
     */
    @Nullable
    public String getLanguage() {
        return mLanguage;
    }

    /**
     * Builder to build a text classifier event.
     */
    public static final class Builder {

        private int mEventType;
        @Nullable private String mEntityType;
        @Nullable private Source mEventSource;
        @Nullable private String mResultId;
        private int mEventIndex;
        private long mEventTime;
        private long mDurationSincePreviousEvent;
        private long mDurationSinceStartEvent;
        @Nullable private Bundle mBundle;
        private int mStart;
        private int mEnd;
        private int mSmartStart;
        private int mSmartEnd;
        private int mInvocationType;
        private int mActionIndex;
        @Nullable private String mLanguage;

        /**
         * Sets the event type. e.g.
         */
        @NonNull
        public Builder setEventType(int eventType) {
            mEventType = eventType;
            return this;
        }

        /**
         * Sets the entity type. e.g. {@link TextClassifier#TYPE_ADDRESS}.
         */
        @NonNull
        public Builder setEntityType(@Nullable String entityType) {
            mEntityType = entityType;
            return this;
        }

        /**
         * Sets the event source.
         */
        @NonNull
        public Builder setEventSource(@Nullable Source eventSource) {
            mEventSource = eventSource;
            return this;
        }

        /**
         * Sets the id of the text classifier result related to this event.
         */
        @NonNull
        public Builder setResultId(@Nullable String resultId) {
            mResultId = resultId;
            return this;
        }

        /**
         * Sets the index of this events in the series of events it belongs to.
         */
        @NonNull
        public Builder setEventIndex(int eventIndex) {
            mEventIndex = eventIndex;
            return this;
        }

        /**
         * Sets the time this event occurred. This is the number of milliseconds since
         * January 1, 1970, 00:00:00 GMT. 0 indicates not set.
         */
        @NonNull
        public Builder setEventTime(long eventTime) {
            mEventTime = eventTime;
            return this;
        }

        /**
         * Sets the number of milliseconds since the previous event. 0 indicates not set.
         */
        @NonNull
        public Builder setDurationSincePreviousEvent(long durationSincePreviousEvent) {
            mDurationSincePreviousEvent = durationSincePreviousEvent;
            return this;
        }

        /**
         * Sets the number of milliseconds since the event at index 0. 0 indicates not set.
         * @see #getEventIndex()
         */
        @NonNull
        public Builder setDurationSinceStartEvent(long durationSinceStartEvent) {
            mDurationSinceStartEvent = durationSinceStartEvent;
            return this;
        }

        /**
         * Sets a bundle containing non-structured extra information about the event.
         *
         * <p><b>NOTE: </b>Prefer to set only immutable values on the bundle otherwise, avoid
         * updating the internals of this bundle as it may have unexpected consequences on the
         * clients of the built event object. For similar reasons, avoid depending on mutable
         * objects in this bundle.
         */
        @NonNull
        public Builder setBundle(@NonNull Bundle bundle) {
            mBundle = Preconditions.checkNotNull(bundle);
            return this;
        }

        /**
         * For smart selection. Sets the relative word index of the start of the selection.
         */
        @NonNull
        public Builder setStart(int start) {
            mStart = start;
            return this;
        }

        /**
         * For smart selection. Sets the relative word index of the end of the selection.
         */
        @NonNull
        public Builder setEnd(int end) {
            mEnd = end;
            return this;
        }

        /**
         * For smart selection. Sets the relative word index of the start of the smart selection.
         */
        @NonNull
        public Builder setSmartStart(int smartStart) {
            mSmartStart = smartStart;
            return this;
        }

        /**
         * For smart selection. Sets the relative word index of the end of the smart selection.
         */
        @NonNull
        public Builder setSmartEnd(int smartEnd) {
            mSmartEnd = smartEnd;
            return this;
        }

        /**
         * Sets the invocation type. e.g.
         */
        @NonNull
        public Builder setInvocationType(int invocationType) {
            mInvocationType = invocationType;
            return this;
        }

        /**
         * Sets the index of the action relating to this event. Actions are usually returned by the
         * text classifier in priority order with the most preferred action at index 0. This index
         * gives an indication of the position of the action that is being reported.
         */
        @NonNull
        public Builder setActionIndex(int actionIndex) {
            mActionIndex = actionIndex;
            return this;
        }

        /**
         * For language detection. Sets the language tag for the detected locale.
         * @see java.util.Locale#forLanguageTag(String).
         */
        @NonNull
        public Builder setLanguage(@Nullable String language) {
            mLanguage = language;
            return this;
        }

        /**
         * Builds and returns a text classifier event.
         */
        @NonNull
        public TextClassifierEvent build() {
            if (mBundle == null) {
                mBundle = new Bundle();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mBundle = mBundle.deepCopy();
            } else {
                mBundle = new Bundle(mBundle);
            }
            return new TextClassifierEvent(
                    mEventType,
                    mEntityType,
                    mEventSource,
                    mResultId,
                    mEventIndex,
                    mEventTime,
                    mDurationSincePreviousEvent,
                    mDurationSinceStartEvent,
                    mBundle,
                    mStart,
                    mEnd,
                    mSmartStart,
                    mSmartEnd,
                    mInvocationType,
                    mActionIndex,
                    mLanguage);
        }
    }

    /**
     * The source of a text classifier event.
     */
    public static final class Source {

        private final String mApplicationPackageName;
        private final int mWidgetType;
        private final int mWidgetVersion;
        @Nullable private final String mWidgetClassName;

        /**
         * Initializes a text classifier event source.
         *
         * @param applicationPackageName application package name. e.g. "com.example.myapp"
         * @param widgetType widget type. e.g.
         * @param widgetVersion custom value for the widget version. 0 indicates not set
         * @param widgetClassName widget class name. e.g. "android.widget.TextView"
         */
        public Source(
                @NonNull String applicationPackageName,
                int widgetType,
                int widgetVersion,
                @Nullable String widgetClassName) {
            mApplicationPackageName = Preconditions.checkNotNull(applicationPackageName);
            mWidgetType = widgetType;
            mWidgetVersion = widgetVersion;
            mWidgetClassName = widgetClassName;
        }

        /**
         * Returns the application package name.
         */
        @NonNull
        public String getApplicationPackageName() {
            return mApplicationPackageName;
        }

        /**
         * Returns the widget type. e.g.
         */
        public int getWidgetType() {
            return mWidgetType;
        }

        /**
         * Returns the custom value for the widget version. 0 indicates not set.
         */
        public int getWidgetVersion() {
            return mWidgetVersion;
        }

        /**
         * Returns the widget class name. e.g. "android.widget.TextView"
         */
        @Nullable
        public String getWidgetClassName() {
            return mWidgetClassName;
        }
    }
}
