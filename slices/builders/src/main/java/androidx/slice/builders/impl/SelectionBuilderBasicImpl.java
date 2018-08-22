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

import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.SUBTYPE_CONTENT_DESCRIPTION;
import static android.app.slice.Slice.SUBTYPE_LAYOUT_DIRECTION;
import static androidx.annotation.RestrictTo.Scope.LIBRARY;

/**
 * @hide
 */
@RestrictTo(LIBRARY)
@RequiresApi(19)
public class SelectionBuilderBasicImpl extends SelectionBuilderImpl {
    public SelectionBuilderBasicImpl(SelectionBuilder selectionBuilder) {
        super(selectionBuilder);
    }

    /**
     * Creates a basic selection slice that simply calls the primary action.
     *
     * @param sliceBuilder the Slice.Builder to use when building the slice
     */
    @Override
    public void apply(Slice.Builder sliceBuilder) {
        getSelectionBuilder().check();

        sliceBuilder.addSubSlice(getSelectionBuilder().getPrimaryAction().buildSlice(sliceBuilder));
    }
}
