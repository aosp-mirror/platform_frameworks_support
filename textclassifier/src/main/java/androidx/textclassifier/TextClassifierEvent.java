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

import android.os.Bundle;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A text classifier event.
 */
// TODO: Comprehensive javadoc.
// TODO: Implement parcelling.
public final class TextClassifierEvent {

    /** @hide **/
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CATEGORY_UNDEFINED, CATEGORY_SELECTION, CATEGORY_LINKIFY,
            CATEGORY_CONVERSATION_ACTIONS, CATEGORY_LANGUAGE_DETECTION})
    public @interface Category {
        // For custom event categories, use range 1000+.
    }
    /** Undefined category */
    public static final int CATEGORY_UNDEFINED = 0;
    /** Smart selection */
    public static final int CATEGORY_SELECTION = 1;
    /** Linkify */
    public static final int CATEGORY_LINKIFY = 2;
    /** Conversation actions */
    public static final int CATEGORY_CONVERSATION_ACTIONS = 3;
    /** Language detection */
    public static final int CATEGORY_LANGUAGE_DETECTION = 4;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TYPE_OVERTYPE, TYPE_COPY_ACTION, TYPE_PASTE_ACTION, TYPE_CUT_ACTION, TYPE_SHARE_ACTION,
            TYPE_SMART_ACTION, TYPE_SELECTION_DRAG, TYPE_SELECTION_DESTROYED, TYPE_OTHER_ACTION,
            TYPE_SELECT_ALL, TYPE_SELECTION_RESET, TYPE_SELECTION_STARTED, TYPE_SELECTION_MODIFIED,
            TYPE_SMART_SELECTION_SINGLE, TYPE_SMART_SELECTION_MULTI, TYPE_AUTO_SELECTION,
            TYPE_LINK_CLICKED, TYPE_ACTIONS_SHOWN})
    public @interface Type {
        // For custom event types, use range 1,000,000+.
    }
    /** User started a new selection. */
    public static final int TYPE_SELECTION_STARTED =
            android.view.textclassifier.SelectionEvent.EVENT_SELECTION_STARTED;
    /** User modified an existing selection. */
    public static final int TYPE_SELECTION_MODIFIED =
            android.view.textclassifier.SelectionEvent.EVENT_SELECTION_MODIFIED;
    /** Smart selection triggered for a single token (word). */
    public static final int TYPE_SMART_SELECTION_SINGLE =
            android.view.textclassifier.SelectionEvent.EVENT_SMART_SELECTION_SINGLE;
    /** Smart selection triggered spanning multiple tokens (words). */
    public static final int TYPE_SMART_SELECTION_MULTI =
            android.view.textclassifier.SelectionEvent.EVENT_SMART_SELECTION_MULTI;
    /** Something else other than user or the default TextClassifier triggered a selection. */
    public static final int TYPE_AUTO_SELECTION =
            android.view.textclassifier.SelectionEvent.EVENT_AUTO_SELECTION;
    /** Smart actions shown to the user. */
    public static final int TYPE_ACTIONS_SHOWN = 10;
    /** User clicked a link. */
    public static final int TYPE_LINK_CLICKED = 20;
    /** User typed over the selection. */
    public static final int TYPE_OVERTYPE =
            android.view.textclassifier.SelectionEvent.ACTION_OVERTYPE;
    /** User clicked on Copy action. */
    public static final int TYPE_COPY_ACTION =
            android.view.textclassifier.SelectionEvent.ACTION_COPY;
    /** User clicked on Paste action. */
    public static final int TYPE_PASTE_ACTION =
            android.view.textclassifier.SelectionEvent.ACTION_PASTE;
    /** User clicked on Cut action. */
    public static final int TYPE_CUT_ACTION =
            android.view.textclassifier.SelectionEvent.ACTION_CUT;
    /** User clicked on Share action. */
    public static final int TYPE_SHARE_ACTION =
            android.view.textclassifier.SelectionEvent.ACTION_SHARE;
    /** User clicked on a Smart action. */
    public static final int TYPE_SMART_ACTION =
            android.view.textclassifier.SelectionEvent.ACTION_SMART_SHARE;
    /** User dragged+dropped the selection. */
    public static final int TYPE_SELECTION_DRAG =
            android.view.textclassifier.SelectionEvent.ACTION_DRAG;
    /** Selection is destroyed. */
    public static final int TYPE_SELECTION_DESTROYED =
            android.view.textclassifier.SelectionEvent.ACTION_ABANDON;
    /** User clicked on a custom action. */
    public static final int TYPE_OTHER_ACTION =
            android.view.textclassifier.SelectionEvent.ACTION_OTHER;
    /** User clicked on Select All action */
    public static final int TYPE_SELECT_ALL =
            android.view.textclassifier.SelectionEvent.ACTION_SELECT_ALL;
    /** User reset the smart selection. */
    public static final int TYPE_SELECTION_RESET =
            android.view.textclassifier.SelectionEvent.ACTION_RESET;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @StringDef({WIDGET_TYPE_TEXTVIEW, WIDGET_TYPE_EDITTEXT, WIDGET_TYPE_UNSELECTABLE_TEXTVIEW,
            WIDGET_TYPE_WEBVIEW, WIDGET_TYPE_EDIT_WEBVIEW, WIDGET_TYPE_CUSTOM_TEXTVIEW,
            WIDGET_TYPE_CUSTOM_EDITTEXT, WIDGET_TYPE_CUSTOM_UNSELECTABLE_TEXTVIEW,
            WIDGET_TYPE_UNKNOWN, WIDGET_TYPE_NOTIFICATION})
    @interface WidgetType {
        // Use namespaces for non-standard widget types. e.g. com.pkg.myapp.widgettype
    }
    /** The widget involved in the text classifier event is a standard
     * {@link android.widget.TextView}. */
    public static final String WIDGET_TYPE_TEXTVIEW = TextClassifier.WIDGET_TYPE_TEXTVIEW;
    /** The widget involved in the text classifier event is a standard
     * {@link android.widget.EditText}. */
    public static final String WIDGET_TYPE_EDITTEXT = TextClassifier.WIDGET_TYPE_EDITTEXT;
    /** The widget involved in the text classifier event is a standard non-selectable
     * {@link android.widget.TextView}. */
    public static final String WIDGET_TYPE_UNSELECTABLE_TEXTVIEW =
            TextClassifier.WIDGET_TYPE_UNSELECTABLE_TEXTVIEW;
    /** The widget involved in the text classifier event is a standard
     * {@link android.webkit.WebView}. */
    public static final String WIDGET_TYPE_WEBVIEW = TextClassifier.WIDGET_TYPE_WEBVIEW;
    /** The widget involved in the text classifier event is a standard editable
     * {@link android.webkit.WebView}. */
    public static final String WIDGET_TYPE_EDIT_WEBVIEW = TextClassifier.WIDGET_TYPE_EDIT_WEBVIEW;
    /** The widget involved in the text classifier event is a custom text widget. */
    public static final String WIDGET_TYPE_CUSTOM_TEXTVIEW =
            TextClassifier.WIDGET_TYPE_CUSTOM_TEXTVIEW;
    /** The widget involved in the text classifier event is a custom editable text widget. */
    public static final String WIDGET_TYPE_CUSTOM_EDITTEXT =
            TextClassifier.WIDGET_TYPE_CUSTOM_EDITTEXT;
    /** The widget involved in the text classifier event is a custom non-selectable text widget. */
    public static final String WIDGET_TYPE_CUSTOM_UNSELECTABLE_TEXTVIEW =
            TextClassifier.WIDGET_TYPE_CUSTOM_UNSELECTABLE_TEXTVIEW;
    /** The widget involved in the text classifier event is of an unknown/unspecified type. */
    public static final String WIDGET_TYPE_UNKNOWN = TextClassifier.WIDGET_TYPE_UNKNOWN;
    /** The widget involved in the text classifier event is a notification widget. */
    public static final String WIDGET_TYPE_NOTIFICATION = "notification";

    @Category private final int mEventCategory;
    @Type private final int mEventType;
    @Nullable private final String mEntityType;
    @Nullable private final Source mEventSource;
    @Nullable private final String mResultId;
    private final int mEventIndex;
    private final long mEventTime;
    private final long mDurationSincePreviousEvent;
    private final long mDurationSinceStartEvent;
    private final Bundle mExtras;

    // Smart selection.
    private final int mStart;
    private final int mEnd;
    private final int mSmartStart;
    private final int mSmartEnd;

    // Smart action.
    private final List<Integer> mActionIndices;

    // Language detection.
    @Nullable private final String mLanguage;

    // Weaker access. SyntheticAccessor.
    TextClassifierEvent(
            int eventCategory,
            int eventType,
            String entityType,
            Source eventSource,
            String resultId,
            int eventIndex,
            long eventTime,
            long durationSincePreviousEvent,
            long durationSinceStartEvent,
            Bundle extras,
            int start,
            int end,
            int smartStart,
            int smartEnd,
            List<Integer> actionIndex,
            String language) {
        mEventCategory = eventCategory;
        mEventType = eventType;
        mEntityType = entityType;
        mEventSource = eventSource;
        mResultId = resultId;
        mEventIndex = eventIndex;
        mEventTime = eventTime;
        mDurationSincePreviousEvent = durationSincePreviousEvent;
        mDurationSinceStartEvent = durationSinceStartEvent;
        mExtras = extras;
        mStart = start;
        mEnd = end;
        mSmartStart = smartStart;
        mSmartEnd = smartEnd;
        mActionIndices = actionIndex;
        mLanguage = language;
    }

    /**
     * Returns the event category. e.g. {@link #CATEGORY_SELECTION}.
     */
    @Category
    public int getEventCategory() {
        return mEventCategory;
    }

    /**
     * Returns the event type. e.g. {@link #TYPE_SELECTION_STARTED}.
     */
    @Type
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
    public Bundle getExtras() {
        return BundleUtils.deepCopy(mExtras);
    }

    /**
     * For smart selection. Returns the relative word index of the start of the selection.
     */
    public int getStart() {
        return mStart;
    }

    /**
     * For smart selection. Returns the relative word (exclusive) index of the end of the selection.
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
     * For smart selection. Returns the relative word (exclusive) index of the end of the
     * smart selection.
     */
    public int getSmartEnd() {
        return mSmartEnd;
    }

    /**
     * Returns an immutable list of the indices of the actions relating to this event.
     * Actions are usually returned by the text classifier in priority order with the most
     * preferred action at index 0. This list gives an indication of the position of the actions
     * that are being reported.
     */
    @NonNull
    public List<Integer> getActionIndices() {
        return mActionIndices;
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

        private int mEventCategory;
        private int mEventType;
        @Nullable private String mEntityType;
        @Nullable private Source mEventSource;
        @Nullable private String mResultId;
        private int mEventIndex;
        private long mEventTime;
        private long mDurationSincePreviousEvent;
        private long mDurationSinceStartEvent;
        @Nullable private Bundle mExtras;
        private int mStart;
        private int mEnd;
        private int mSmartStart;
        private int mSmartEnd;
        private int[] mActionIndices;
        @Nullable private String mLanguage;

        /**
         * Sets the event category. e.g. {@link #CATEGORY_SELECTION}.
         */
        @NonNull
        public Builder setEventCategory(@Category int eventCategory) {
            mEventCategory = eventCategory;
            return this;
        }

        /**
         * Sets the event type. e.g. {@link #TYPE_SELECTION_STARTED}.
         */
        @NonNull
        public Builder setEventType(@Type int eventType) {
            mEventType = eventType;
            return this;
        }

        /**
         * Sets the entity type. e.g. {@link TextClassifier#TYPE_ADDRESS}.
         */
        @NonNull
        public Builder setEntityType(@Nullable @TextClassifier.EntityType String entityType) {
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
        public Builder setExtras(@NonNull Bundle extras) {
            mExtras = Preconditions.checkNotNull(extras);
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
         * For smart selection. Sets the relative word (exclusive) index of the end of the
         * selection.
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
         * For smart selection. Sets the relative word (exclusive) index of the end of the
         * smart selection.
         */
        @NonNull
        public Builder setSmartEnd(int smartEnd) {
            mSmartEnd = smartEnd;
            return this;
        }

        /**
         * Sets the indices of the actions involved in this event. Actions are usually returned by
         * the text classifier in priority order with the most preferred action at index 0.
         * This index gives an indication of the position of the action that is being reported.
         */
        @NonNull
        public Builder setActionIndices(int... actionIndices) {
            mActionIndices = actionIndices;
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
            mExtras = mExtras == null ? Bundle.EMPTY : BundleUtils.deepCopy(mExtras);
            List<Integer> actionIndices;
            if (mActionIndices != null) {
                actionIndices = new ArrayList<>(mActionIndices.length);
                for (int actionIndex : mActionIndices) {
                    actionIndices.add(actionIndex);
                }
                actionIndices = Collections.unmodifiableList(actionIndices);
            } else {
                actionIndices = Collections.emptyList();
            }
            return new TextClassifierEvent(
                    mEventCategory,
                    mEventType,
                    mEntityType,
                    mEventSource,
                    mResultId,
                    mEventIndex,
                    mEventTime,
                    mDurationSincePreviousEvent,
                    mDurationSinceStartEvent,
                    mExtras,
                    mStart,
                    mEnd,
                    mSmartStart,
                    mSmartEnd,
                    actionIndices,
                    mLanguage);
        }
        // TODO: Add build(boolean validate).
    }

    /**
     * The source of a text classifier event.
     */
    public static final class Source {

        private final String mApplicationPackageName;
        @WidgetType private final String mWidgetType;
        private final int mWidgetVersion;
        @Nullable private final String mWidgetClassName;

        /**
         * Initializes a text classifier event source.
         *
         * @param applicationPackageName application package name. e.g. "com.example.myapp"
         * @param widgetType widget type. e.g. {@link #WIDGET_TYPE_TEXTVIEW}
         * @param widgetVersion custom value for the widget version. 0 indicates not set
         * @param widgetClassName widget class name. e.g. "android.widget.TextView"
         */
        public Source(
                @NonNull String applicationPackageName,
                @WidgetType String widgetType,
                int widgetVersion,
                @Nullable String widgetClassName) {
            mApplicationPackageName = Preconditions.checkNotNull(applicationPackageName);
            mWidgetType = Preconditions.checkNotNull(widgetType);
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
         * Returns the widget type. e.g. {@link #WIDGET_TYPE_TEXTVIEW}.
         */
        @NonNull
        @WidgetType
        public String getWidgetType() {
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
