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
 * <p>The following properties can be specified:
 * <ul>
 *     <li>Title - Primary text that is shown on the item.
 *     <li>{@link View.OnClickListener} - Listener that handles the clicks on the item.
 *     <li>Icon - An {@link Icon} shown next to the title.
 *     <li>Style - A Resource Id that specifies the style of the item.
 *     <li>Enabled - A boolean that specifies whether the item is enabled or disabled.
 *     <li>Checkable - A boolean that specifies whether the item is checkable (a switch) or not.
 *     <li>DisplayBehavior - A {@link DisplayBehavior} that specifies where the item is displayed.
 * </ul>
 *
 * <p>{@code CarMenuItem}s are added to the {@link CarToolbar} from
 * right-to-left. Items marked as overflow will always be displayed
 * in the overflow menu, everything else will be displayed if there
 * is enough room, otherwise they are moved to the overflow menu.
 *
 *
 *
 * <p>{@code CarMenuItem}s are added to the {@link CarToolbar} from
 * right-to-left. All items marked with display behavior ALWAYS are
 * displayed, then any items marked IF_ROOM if there is space left.
 *
 * <p>Properties such as the title, style, and isEnabled can be modified
 * after creation, and as such, have setters in the class and the builder.
 *
 */
public class CarMenuItem {
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
    private CharSequence mTitle;
    private final View.OnClickListener mOnClickListener;
    private final Icon mIcon;
    private int mStyleResId;
    private boolean mIsEnabled;
    private final boolean mIsCheckable;
    private final DisplayBehavior mDisplayBehavior;

    CarMenuItem(Builder builder) {
        mTitle = builder.mTitle;
        mOnClickListener = builder.mOnClickListener;
        mIcon = builder.mIcon;
        mStyleResId = builder.mStyleResId;
        mIsEnabled = builder.mIsEnabled;
        mIsCheckable = builder.mIsCheckable;
        mDisplayBehavior = builder.mDisplayBehavior;
    }

    /**
     * Builder for creating a {@link CarMenuItem}
     */
    public static final class Builder {
        @NonNull
        CharSequence mTitle = "";
        @Nullable
        View.OnClickListener mOnClickListener;
        @Nullable
        Icon mIcon;
        int mStyleResId = -1;
        boolean mIsEnabled = true;
        boolean mIsCheckable;
        // If not specified, the item will be displayed only if there is room on the toolbar.
        DisplayBehavior mDisplayBehavior = DisplayBehavior.IF_ROOM;

        /**
         * Creates a new instance of the {@code Builder}.
         */
        public Builder() {
        }

        /**
         * Sets the title of the CarMenuItem.
         *
         * @param context Context that the CarToolbar is in.
         * @param titleResId Res Id for the title of the CarMenuItem.
         * @return This {@code Builder} object to allow call chaining.
         */
        @NonNull
        public Builder setTitle(Context context, @StringRes int titleResId) {
            mTitle = context.getString(titleResId);
            return this;
        }

        /**
         * Sets the title of the CarMenuItem.
         *
         * @param title Title of the CarMenuItem.
         * @return This {@code Builder} object to allow call chaining.
         */
        @NonNull
        public Builder setTitle(@NonNull CharSequence title) {
            mTitle = title;
            return this;
        }

        /**
         * Sets {@link View.OnClickListener} of the CarMenuItem.
         *
         * @param listener OnClick listener of the CarMenuItem.
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
         * <p>Checkable items are rendered as switch widgets.
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
         *
         * @return A {@link CarMenuItem} built with the provided information.
         */
        @NonNull
        public CarMenuItem build() {
            return new CarMenuItem(this);
        }
    }

    /**
     * Sets the title of the CarMenuItem.
     *
     * @param titleResId Res Id for the title of the CarMenuItem.
     */
    public void setTitle(@NonNull Context context, @StringRes int titleResId) {
        mTitle = context.getString(titleResId);
    }

    /**
     * Sets the title of the CarMenuItem.
     *
     * @param title Title of the CarMenuItem.
     */
    public void setTitle(@NonNull CharSequence title) {
        mTitle = title;
    }

    /**
     * Sets the style of the CarMenuItem.
     *
     * @param styleResId Res Id of the style to be used for the CarMenuItem.
     */
    public void setStyle(@StyleRes int styleResId) {
        mStyleResId = styleResId;
    }

    /**
     * Sets whether the CarMenuItem is enabled or disabled.
     *
     * <p>Items are enabled by default.
     *
     * @param enabled {@code true} if the CarMenuItem is enabled.
     */
    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
    }

    /**
     * Returns the icon of the CarMenuItem.
     */
    @Nullable
    public Icon getIcon() {
        return mIcon;
    }

    /**
     * Returns the title of the CarMenuItem.
     */
    @NonNull
    public CharSequence getTitle() {
        return mTitle;
    }

    /**
     * Returns the Res Id of the CarMenuItem's style.
     */
    public int getStyleResId() {
        return mStyleResId;
    }

    /**
     * Returns {@code true} if the CarMenuItem is enabled.
     */
    public boolean isEnabled() {
        return mIsEnabled;
    }

    /**
     * Returns {@code true} if the CarMenuItem is checkable.
     */
    public boolean isCheckable() {
        return mIsCheckable;
    }

    /**
     * Returns The {@link DisplayBehavior} of the CarMenuItem.
     */
    @NonNull
    public DisplayBehavior getDisplayBehavior() {
        return mDisplayBehavior;
    }

    /**
     * Returns the {@link View.OnClickListener} of the CarMenuItem.
     */
    @Nullable
    public View.OnClickListener getOnClickListener() {
        return mOnClickListener;
    }
}
