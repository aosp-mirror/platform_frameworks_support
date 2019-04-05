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

package androidx.preference;

<<<<<<< HEAD   (69f76e Merge "Merge empty history for sparse-5425228-L6310000028962)
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;
=======
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;
>>>>>>> BRANCH (bf79df Merge "Merge cherrypicks of [940699] into sparse-5433600-L95)

import androidx.annotation.RestrictTo;

/**
 * @hide
 */
<<<<<<< HEAD   (69f76e Merge "Merge empty history for sparse-5425228-L6310000028962)
@RestrictTo(LIBRARY_GROUP_PREFIX)
=======
@RestrictTo(LIBRARY_GROUP)
>>>>>>> BRANCH (bf79df Merge "Merge cherrypicks of [940699] into sparse-5433600-L95)
public class AndroidResources {

    public static final int ANDROID_R_ICON_FRAME = android.R.id.icon_frame;
    public static final int ANDROID_R_LIST_CONTAINER = android.R.id.list_container;
    public static final int ANDROID_R_SWITCH_WIDGET = android.R.id.switch_widget;
    public static final int ANDROID_R_PREFERENCE_FRAGMENT_STYLE
            = android.R.attr.preferenceFragmentStyle;
    public static final int ANDROID_R_EDITTEXT_PREFERENCE_STYLE
            = android.R.attr.editTextPreferenceStyle;

    private AndroidResources() {}
}
