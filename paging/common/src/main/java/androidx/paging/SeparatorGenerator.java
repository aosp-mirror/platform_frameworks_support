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

package androidx.paging;

import androidx.annotation.Nullable;

public interface SeparatorGenerator<In, Out> {
    /**
     * Create a separator between previous and next if desired.
     *
     * This can be used e.g. to create list separators such as date headers when item dates differ,
     * or letter separators when first characters differ.
     *
     * @param previous The item before the potential separator. Note that this may be null when
     *                 checking if a separator is at the top of the list data.
     * @param next The item after the potential separator. Note that this may be null when checking
     *             if a separator is at the bottom of the list data.
     * @return The separator, or null if no separator should be created.
     */
    @Nullable
    Out generate(@Nullable In previous, @Nullable In next);
}
