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

package androidx.textclassifier.widget;

import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appcompat.R;

/**
 * FloatingToolbar constants.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(api = Build.VERSION_CODES.M)
public final class FloatingToolbarConstants {

    private FloatingToolbarConstants() {}

    public static final Object FLOATING_TOOLBAR_TAG = "floating_toolbar";
    public static final Object MAIN_PANEL_TAG = "main_panel";
    public static final Object OVERFLOW_PANEL_TAG = "main_overflow";
    public static final int MENU_ID_ASSIST = R.id.textAssist;
}
