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

package androidx.appcompat.widget;

import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

/**
 * General status change listener for things happening in a TextView.
 *
 * @hide for library code only for now.
 */
@RestrictTo(LIBRARY_GROUP)
@UiThread
public abstract class OnTextViewStatusChangeListener {

    // TODO: Keeping this simple for now.
    // If we prefer to support more complex event data, we'd change the last param to more a more
    // complex param.
    // We can add more event methods when required.
    public void onWindowFocusChanged(TextView textView, boolean hasFocus) {}
    public void onFocusChanged(TextView textView, boolean hasFocus) {}
    public void onActionModeStatusChanged(TextView textView, int status) {}

    // TODO: Move to a separate class.
    public static final class ActionModeStatusChangeData {
        public static int STATUS_STARTED = 1;
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    static final class CompositeOnTextViewStatusChangeListener
            extends OnTextViewStatusChangeListener {

        private ArrayList<OnTextViewStatusChangeListener> listeners = new ArrayList<>();

        public void add(OnTextViewStatusChangeListener listener) {
            listeners.add(listener);
        }

        public void remove(OnTextViewStatusChangeListener listener) {
            listeners.remove(listener);
        }

        @Override
        public void onWindowFocusChanged(TextView textView, boolean hasFocus) {
            for (OnTextViewStatusChangeListener listener : listeners) {
                listener.onWindowFocusChanged(textView, hasFocus);
            }
        }

        @Override
        public void onFocusChanged(TextView textView, boolean hasFocus) {
            for (OnTextViewStatusChangeListener listener : listeners) {
                listener.onFocusChanged(textView, hasFocus);
            }
        }

        @Override
        public void onActionModeStatusChanged(TextView textView, int status) {
            for (OnTextViewStatusChangeListener listener : listeners) {
                listener.onActionModeStatusChanged(textView, status);
            }
        }
    }
}
