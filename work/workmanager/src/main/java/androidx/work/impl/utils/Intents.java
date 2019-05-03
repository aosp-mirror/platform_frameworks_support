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

package androidx.work.impl.utils;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Utility methods for WorkManager intents.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Intents {

    private static final String EXTRA_WORKMANAGER_VERIFICATION = "WORKMANAGER_VERIFICATION";

    /**
     * Creates an intent with a known, verifiable extra.
     *
     * @param context The context to pass to the intent
     * @param clazz The class name to pass to the intent
     * @return An intent with a known extra that's verifiable via {@link #verifyIntent(Intent)}
     */
    public static @NonNull Intent createVerifiableIntent(
            @NonNull Context context,
            @NonNull Class<?> clazz) {
        Intent intent = new Intent(context, clazz);
        intent.putExtra(EXTRA_WORKMANAGER_VERIFICATION, clazz.getName());
        return intent;
    }

    /**
     * Verifies an intent was created via {@link #createVerifiableIntent(Context, Class)}.
     *
     * @param intent The intent to verify
     * @return {@code true} if it contains a known extra; {@code false} otherwise
     */
    public static boolean verifyIntent(@NonNull Intent intent) {
        return intent.hasExtra(EXTRA_WORKMANAGER_VERIFICATION);
    }

    private Intents() {
    }
}
