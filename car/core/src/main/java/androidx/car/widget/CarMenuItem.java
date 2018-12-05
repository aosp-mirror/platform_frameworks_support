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

package androidx.car.widget;

import android.content.Context;
import android.graphics.drawable.Icon;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;

/**
 * Class to build a {@code CarMenuItem} that appears in the {@link CarToolbar} menu.
 *
 * <p>When creating a {@code CarMenuItem}, the following must be specified:
 * <ul>
 *     <li>Title - Primary text that is shown on the item.
 *     <li>{@link View.OnClickListener} - Listener that handles the clicks on the item.
 * </ul>
 *
 * <p>Optionally, the following properties can also be specified:
 * <ul>
 *     <li>Icon - An {@link Icon} shown next to the title.
 *     <li>Style - A Resource Id that specifies the style of the item.
 *     <li>Priority - An int that determines the order of the item, lower is displayed first.
 *     <li>Enabled - A boolean that specifies whether the item is enabled or disabled.
 *     <li>Checkable - A boolean that specifies whether the item is checkable (a switch) or not.
 *     <li>DisplayBehavior - A {@link DisplayBehavior} that specifies where the item is displayed.
 * </ul>
 *
 * <p>{@code CarMenuItem}s are displayed based on their {@link DisplayBehavior} and their priority,
 * If not specified, the item will be appended to the end of {@code DisplayBehavior.IF_ROOM}.
 *
 */
public class CarMenuItem implements Comparable<CarMenuItem> {

    /**
     * Display behaviors for CarMenuItems, describes whether the items
     * will be displayed on the toolbar or in the overflow menu.
     */
    public enum DisplayBehavior {
        /**
         * The item is always displayed on the toolbar, never in the overflow menu.
         */
        ALWAYS,
        /**
         * The item is displayed on the toolbar if there's room, otherwise in the overflow menu.
         */
        IF_ROOM,
        /**
         * The item is never displayed on the toolbar, always in the overflow menu.
         */
        NEVER
    }

    private final CharSequence mTitle;
    private final View.OnClickListener mOnClickListener;
    private final Icon mIcon;
    private final int mStyleResId;
    private final int mPriority;
    private final boolean mIsEnabled;
    private final boolean mIsCheckable;
    private final DisplayBehavior mDisplayBehavior;

    CarMenuItem(Builder builder) {
        mTitle = builder.mTitle;
        mOnClickListener = builder.mOnClickListener;
        mIcon = builder.mIcon;
        mStyleResId = builder.mStyleResId;
        mPriority = builder.mPriority;
        mIsEnabled = builder.mIsEnabled;
        mIsCheckable = builder.mIsCheckable;
        mDisplayBehavior = builder.mDisplayBehavior;
    }

    /**
     * Builder for creating a {@link CarMenuItem}
     */
    public static final class Builder {
        private final Context mContext;
        CharSequence mTitle;
        @Nullable
        View.OnClickListener mOnClickListener;
        @Nullable
        Icon mIcon;
        //TODO(obadah): What should the default ResId be?
        int mStyleResId = -1;
        int mPriority;
        boolean mIsEnabled = true;
        boolean mIsCheckable;
        // If not specified, the item will be displayed only if there is room on the toolbar.
        DisplayBehavior mDisplayBehavior = DisplayBehavior.IF_ROOM;

        /**
         * Creates a new instance of the {@code Builder}.
         *
         * @param context The {@code Context} that the CarMenuItem is to be created in.
         */
        public Builder(@NonNull Context context) {
            mContext = context;
        }

        /**
         * Sets the title of the CarMenuItem.
         *
         * @param titleResId Res Id for the title of the CarMenuItem.
         * @return This {@code Builder} object to allow call chaining.
         */
        @NonNull
        public Builder setTitle(@StringRes int titleResId) {
            mTitle = mContext.getString(titleResId);
            return this;
        }

        /**
         * Sets the title of the CarMenuItem.
         *
         * @param title Title of the CarMenuItem.
         * @return This {@code Builder} object to allow call chaining.
         */
        @NonNull
        public Builder setTitle(CharSequence title) {
            mTitle = title;
            return this;
        }

        /**
         * Sets {@link View.OnClickListener} of the CarMenuItem.
         *
         * @param listener Click listener of the CarMenuItem.
         * @return This {@code Builder} object to allow call chaining.
         */
        @NonNull
        public Builder setOnClickListener(@NonNull View.OnClickListener listener) {
            mOnClickListener = listener;
            return this;
        }

        /**
         * Sets the style of the CarMenuItem.
         *
         * @param styleResId Res Id of the style to be used for the CarMenuItem.
         * @return This {@code Builder} object to allow call chaining.
         */
        @NonNull
        public Builder setStyle(@StyleRes int styleResId) {
            mStyleResId = styleResId;
            return this;
        }

        /**
         * Sets the icon of the CarMenuItem.
         *
         * @param icon Icon of the CarMenuItem.
         * @return This {@code Builder} object to allow call chaining.
         */
        @NonNull
        public Builder setIcon(@NonNull Icon icon) {
            mIcon = icon;
            return this;
        }

        /**
         * Sets whether the CarMenuItem is enabled or disabled.
         *
         * <p>Items are enabled by default.
         *
         * @param enabled {@code true} if the CarMenuItem is enabled.
         * @return This {@code Builder} object to allow call chaining.
         */
        @NonNull
        public Builder setEnabled(boolean enabled) {
            mIsEnabled = enabled;
            return this;
        }

        /**
         * Sets whether the CarMenuItem is checkable or not.
         *
         * <p>Checkable items are rendered as a Switch.
         *
         * @param checkable {@code true} if the CarMenuItem is checkable.
         * @return This {@code Builder} object to allow call chaining.
         */
        @NonNull
        public Builder setCheckable(boolean checkable) {
            mIsCheckable = checkable;
            return this;
        }

        /**
         * Sets the display behavior of the CarMenuItem.
         *
         * The display behavior determines whether the item is displayed on
         * the Toolbar or in the overflow menu, see {@link DisplayBehavior}.
         *
         * @param displayBehavior Display behavior of the CarMenuItem.
         * @return This {@code Builder} object to allow call chaining.
         */
        @NonNull
        public Builder setDisplayBehavior(@NonNull DisplayBehavior displayBehavior) {
            mDisplayBehavior = displayBehavior;
            return this;
        }

        /**
         * Sets the display priority of the CarMenuItem.
         *
         * <p>CarMenuItems are sorted by priority, lower is displayed first.
         *
         * @param priority priority of the CarMenuItem.
         * @return This {@code Builder} object to allow call chaining.
         */
        @NonNull
        public Builder setPriority(int priority) {
            mPriority = priority;
            return this;
        }

        /**
         *
         * @return A {@link CarMenuItem} built with the provided information.
         */
        @NonNull
        public CarMenuItem build() {
            return new CarMenuItem(this);
        }
    }

    /**
     * @return The icon of the CarMenuItem.
     */
    @Nullable
    public Icon getIcon() {
        return mIcon;
    }

    /**
     * @return The title of the CarMenuItem.
     */
    public CharSequence getTitle() {
        return mTitle;
    }

    /**
     * @return The Res Id of the CarMenuItem's style.
     */
    public int getStyleResId() {
        return mStyleResId;
    }

    /**
     * @return The display priority of the CarMenuItem.
     */
    public int getPriority() {
        return mPriority;
    }

    /**
     * @return {@code true} if the CarMenuItem is enabled.
     */
    public boolean isEnabled() {
        return mIsEnabled;
    }

    /**
     * @return {@code true} if the CarMenuItem is checkable.
     */
    public boolean isCheckable() {
        return mIsCheckable;
    }

    /**
     * @return The display behavior of the CarMenuItem.
     */
    @NonNull
    public DisplayBehavior getDisplayBehavior() {
        return mDisplayBehavior;
    }

    /**
     * @return The {@link View.OnClickListener} of the CarMenuItem.
     */
    @Nullable
    public View.OnClickListener getOnClickListener() {
        return mOnClickListener;
    }

    @Override
    public int compareTo(@NonNull CarMenuItem item) {
        // DisplayBehavior is the dominant sort criteria, DisplayBehavior.ALWAYS comes
        // before DisplayBehavior.IF_ROOM, which comes before DisplayBehavior.NEVER.
        if (mDisplayBehavior != item.getDisplayBehavior()) {
            return mDisplayBehavior.compareTo(item.getDisplayBehavior());
        }
        // Sort by priority if both items have the same DisplayBehavior.
        if (mPriority > item.getPriority()) {
            return -1;
        } else if (mPriority < item.getPriority()) {
            return 1;
        } else {
            // If the two item have the same priority, sort alphabetically.
            // TODO(obadah): Can two items have the same priority? Throw error?
            return String.valueOf(mTitle).compareTo(String.valueOf(item.getTitle()));
        }
    }
}
