/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v4.app;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.annotation.TargetApi;
import android.app.Notification;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;

/**
 * Interface implemented by notification compat builders that support
 * an accessor for {@link Notification.Builder}. {@link Notification.Builder}
 * was introduced in HoneyComb.
 *
 * @hide
 */
@RequiresApi(11)
@TargetApi(11)
@RestrictTo(LIBRARY_GROUP)
public interface NotificationBuilderWithBuilderAccessor {
    public Notification.Builder getBuilder();
    public Notification build();
}
