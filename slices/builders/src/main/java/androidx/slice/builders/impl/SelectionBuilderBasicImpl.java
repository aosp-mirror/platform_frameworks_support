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

import androidx.slice.Slice;
import androidx.slice.builders.SelectionBuilder;

import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.SUBTYPE_CONTENT_DESCRIPTION;
import static android.app.slice.Slice.SUBTYPE_LAYOUT_DIRECTION;

/**
 * Serializes a SelectionBuilder's data into a SliceBuilder in basic form.
 *
 * This doesn't include the options, just a bare intent that requires the exposing app to display
 * its own selection interface.
 */
public class SelectionBuilderBasicImpl extends SelectionBuilderImpl {
    public SelectionBuilderBasicImpl(SelectionBuilder selectionBuilder) {
        super(selectionBuilder);
    }

    /**
     * Creates a basic slice based on a SelectionBuilder.
     *
     * This slice will just send the selection's intent without any selection; the app providing the
     * slice must provide a selection interface.
     *
     * @param sliceBuilder the Slice.Builder to use when building the slice
     */
    public void apply(Slice.Builder sliceBuilder) {
        if (getSelectionBuilder().getIntent() == null) {
            throw new IllegalArgumentException("SelectionBuilder must have an intent set");
        }

        Slice.Builder subsliceBuilder = new Slice.Builder(sliceBuilder);

        if (getSelectionBuilder().getTitle() != null) {
            subsliceBuilder.addText(getSelectionBuilder().getTitle(), null, HINT_TITLE);
        }

        if (getSelectionBuilder().getSubtitle() != null) {
            subsliceBuilder.addText(getSelectionBuilder().getSubtitle(), null);
        }

        if (getSelectionBuilder().getContentDescription() != null) {
            subsliceBuilder.addText(getSelectionBuilder().getContentDescription(),
                    SUBTYPE_CONTENT_DESCRIPTION);
        }

        if (getSelectionBuilder().getLayoutDirection() != -1) {
            subsliceBuilder.addInt(getSelectionBuilder().getLayoutDirection(),
                    SUBTYPE_LAYOUT_DIRECTION);
        }

        sliceBuilder.addAction(getSelectionBuilder().getIntent(), subsliceBuilder.build(), null);
    }
}
