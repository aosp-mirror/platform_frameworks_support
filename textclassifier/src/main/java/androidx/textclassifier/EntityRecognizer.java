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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Pattern;

/**
 * An EntityRecognizer returns a TextClassifier entity type given a pattern and the text in which
 * it occurs.
 */
// TODO: Rename?
public interface EntityRecognizer {

    /**
     * @param pattern pattern found in the text
     * @param s the text
     * @param start index where the pattern starts
     * @param end index where the pattern ends
     * @return the entity type for the text or null if it shouldn't be considered to have a valid
     *      entity type
     */
    @Nullable
    String getEntityType(@NonNull Pattern pattern, @NonNull CharSequence s, int start, int end);
}
