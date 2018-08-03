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

package androidx.slice.builders;

import android.app.PendingIntent;
import android.graphics.drawable.Icon;

import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.util.Pair;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;


/**
 * Builder to construct a selection which can be added to a {@link ListBuilder}.
 */
@RequiresApi(19)
public class SelectionBuilder {
    private List<Pair<String, CharSequence>> mOptions;
    private Set<String> mOptionKeys;
    private int mSelectedOption;
    private Icon mOptionIcon;
    private PendingIntent mIntent;

    private CharSequence mTitle;
    private CharSequence mSubtitle;
    private CharSequence mContentDescription;
    private int mLayoutDirection;

    /**
     * Creates a SelectionBuilder with no options.
     */
    public SelectionBuilder() {
        mSelectedOption = 0;
        mLayoutDirection = -1;
    }

    /**
     * Adds an option to this SelectionBuilder.
     *
     * The new option will be appended to the list of options.
     *
     * @param optionKey the key that will be returned if the user selects this option
     * @param optionText the text that will be displayed to the user for this option
     * @return this SelectionBuilder
     */
    public SelectionBuilder addOption(String optionKey, CharSequence optionText) {
        if (mOptionKeys.contains(optionKey)) {
            throw new IllegalArgumentException("optionKey " + optionKey + " is a duplicate");
        }

        mOptions.add(new Pair<>(optionText, optionKey));
        mOptionKeys.add(optionKey);
        return this;
    }

    /**
     * Sets the intent to send when the user makes a selection or tries to.
     *
     * If the user makes a selection, the intent sent will include TODO: extra.
     *
     * If the user didn't make a selection (for example, if the slice was displayed as an icon and
     * the user tapped the icon, the intent sent will not include TODO: extra.
     *
     * @param intent the intent to send when the user makes a selection or tries to.
     * @return this SelectionBuilder
     */
    public SelectionBuilder setIntent(@NonNull PendingIntent intent) {
        mIntent = intent;
        return this;
    }

    /**
     * Sets which option is selected by default.
     *
     * @param selectedOption the index of the selected option, at least 0 and less than the number
     *                       of calls to {@link #addOption(String,String)}.
     * @return this SelectionBuilder
     */
    public SelectionBuilder setSelectedOption(int selectedOption) {
        if (selectedOption < 0 || selectedOption >= mOptions.size()) {
            throw new IllegalArgumentException(
                    "selectedOption " + selectedOption
                            + " out of range (0, " + mOptions.size() + "]");
        }

        mSelectedOption = selectedOption;
        return this;
    }

    /**
     * Sets the icon to be displayed next to every option if the selection is displayed as a list of
     * items with radio buttons.
     *
     * @param optionIcon the icon to be displayed next to options
     * @return this SelectionBuilder
     */
    public SelectionBuilder setOptionIcon(Icon optionIcon) {
        mOptionIcon = optionIcon;
        return this;
    }

    /**
     * Sets the title.
     *
     * @param title the title
     * @return this SelectionBuilder
     */
    public SelectionBuilder setTitle(@Nullable CharSequence title) {
        mTitle = title;
        return this;
    }

    /**
     * Sets the subtitle.
     *
     * @param subtitle the subtitle
     * @return this SelectionBuilder
     */
    public SelectionBuilder setSubtitle(@Nullable CharSequence subtitle) {
        mSubtitle = subtitle;
        return this;
    }

    /**
     * Sets the content description.
     *
     * @param contentDescription the content description
     * @return this SelectionBuilder
     */
    public SelectionBuilder setContentDescription(@Nullable CharSequence contentDescription) {
        mContentDescription = contentDescription;
        return this;
    }

    /**
     * Sets the layout direction.
     *
     * @param layoutDirection the layout direction
     * @return this SelectionBuilder
     */
    public SelectionBuilder setLayoutDirection(
            @androidx.slice.builders.ListBuilder.LayoutDirection int layoutDirection) {
        mLayoutDirection = layoutDirection;
        return this;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public List<Pair<String, String>> getOptions() {
        return mOptions;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public PendingIntent getIntent() {
        return mIntent;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public int getSelectedOption() {
        return mSelectedOption;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public Icon getOptionIcon() {
        return mOptionIcon;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public CharSequence getTitle() {
        return mTitle;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public CharSequence getSubtitle() {
        return mSubtitle;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public CharSequence getContentDescription() {
        return mContentDescription;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY)
    public int getLayoutDirection() {
        return mLayoutDirection;
    }
}
