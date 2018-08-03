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

package androidx.remotecallback;

import androidx.annotation.RestrictTo;

/**
 * Holder class that represents an input argument passed by the caller of this
 * callback.
 *
 * Remote Callbacks allow for caller supplied inputs to be handled alongside
 * receiver specified arguments. For instance, if a `SLIDER_VALUE` input is
 * declared, an app could easily create callbacks for several sliders.
 *
 * <pre>// These are callbacks that could be hooked up to sliders.
 * createRemoteCallback(context, "setSliderValue", R.id.slider_1, SLIDER_VALUE);
 * createRemoteCallback(context, "setSliderValue", R.id.slider_2, SLIDER_VALUE);
 * createRemoteCallback(context, "setSliderValue", R.id.slider_3, SLIDER_VALUE);
 * // This could be attached to a reset button.
 * createRemoteCallback(context, "setSliderValue", R.id.slider_1, 0);
 *
 * public void setSliderValue(int slideId, int newValue) {
 *   ...
 * }</pre>
 */
public class RemoteInputHolder {

    private final String mKeyName;
    private final Class<?> mType;

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public RemoteInputHolder(String keyName, Class<?> type) {
        mKeyName = keyName;
        mType = type;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public String getKey() {
        return mKeyName;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Class<?> getType() {
        return mType;
    }
}
