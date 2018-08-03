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

package androidx.slice.builders.impl;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.slice.Slice;
import androidx.slice.builders.SelectionBuilder;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

/**
 * @hide
 */
@RestrictTo(LIBRARY)
@RequiresApi(19)
public abstract class SelectionBuilderImpl {
    private final SelectionBuilder mSelectionBuilder;

    public SelectionBuilderImpl(SelectionBuilder selectionBuilder) {
        mSelectionBuilder = selectionBuilder;
    }

    public SelectionBuilder getSelectionBuilder() {
        return mSelectionBuilder;
    }

    /**
     * Builds a slice containing the selection passed to the constructor.
     *
     * @param sliceBuilder the Slice.Builder to use when building the slice
     */
    public abstract void apply(Slice.Builder sliceBuilder);
}
