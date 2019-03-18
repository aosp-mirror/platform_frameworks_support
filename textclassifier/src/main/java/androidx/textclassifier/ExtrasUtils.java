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

package androidx.textclassifier;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Utilities for inserting/retrieving data into/from textclassifier related objects.
 */
public final class ExtrasUtils {

    private static final String EXTRA_FROM_TEXT_CLASSIFIER =
            "android.view.textclassifier.extra.FROM_TEXT_CLASSIFIER";
    private static final String ENTITY_TYPE = "entity-type";
    private static final String SCORE = "score";
    private static final String TEXT_LANGUAGES = "text-languages";

    private ExtrasUtils() {}

    /**
     * Returns the top language found in the textclassifier extras in the intent. This may return
     * null if the data could not be found.
     */
    @Nullable
    public static Locale getTopLanguage(@Nullable Intent intent) {
        // NOTE: This is (and should be) a copy of the related platform code.
        // It is hard to test this code returns something on a given platform because we can't
        // guarantee the TextClassifier implementation that will be used to send the intent.
        // Depend on the the platform tests instead and avoid this code running out of sync with
        // what is expected of each platform. Note that the code may differ from platform to
        // platform but that will be a bad idea as it will be hard to manage.
        // TODO: Include a "put" counterpart of this method so that other TextClassifier
        // implementations may use it to put language data into the generated intent in a way that
        // this method can retrieve it.
        if (intent == null) {
            return null;
        }
        final Bundle tcBundle = intent.getBundleExtra(EXTRA_FROM_TEXT_CLASSIFIER);
        if (tcBundle == null) {
            return null;
        }
        final Bundle textLanguagesExtra = tcBundle.getBundle(TEXT_LANGUAGES);
        if (textLanguagesExtra == null) {
            return null;
        }
        final String[] languages = textLanguagesExtra.getStringArray(ENTITY_TYPE);
        final float[] scores = textLanguagesExtra.getFloatArray(SCORE);
        if (languages == null || scores == null
                || languages.length == 0 || languages.length != scores.length) {
            return null;
        }
        int highestScoringIndex = 0;
        for (int i = 1; i < languages.length; i++) {
            if (scores[highestScoringIndex] < scores[i]) {
                highestScoringIndex = i;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // This API is not available before lollipop.
            // We currently don't expect pre-lollipop platforms setting this data.
            // This however will change in the future.
            return Locale.forLanguageTag(languages[highestScoringIndex]);
        }
        return null;
    }
}
