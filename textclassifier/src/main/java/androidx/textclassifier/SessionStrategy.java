/*
 * Copyright (C) 2018 The Android Open Source Project
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
import androidx.annotation.RestrictTo;

/**
 * Strategy on session handling in {@link TextClassifier}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
interface SessionStrategy {

    void destroy();

    void reportSelectionEvent(@NonNull SelectionEvent event);

    boolean isDestroyed();

    TextClassification classifyText(@NonNull TextClassification.Request request);

    TextLinks generateLinks(@NonNull TextLinks.Request request);

    TextSelection suggestSelection(@NonNull TextSelection.Request request);

    int getMaxGenerateLinksTextLength();
}
