/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v13.app;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.support.annotation.RequiresApi;

@RequiresApi(23)
@TargetApi(23)
class FragmentCompat23 {
    public static void requestPermissions(Fragment fragment, String[] permissions,
            int requestCode) {
        fragment.requestPermissions(permissions, requestCode);
    }

    public static boolean shouldShowRequestPermissionRationale(Fragment fragment,
            String permission) {
        return fragment.shouldShowRequestPermissionRationale(permission);
    }
}
