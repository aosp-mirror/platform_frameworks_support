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

package androidx.car.widget;

import android.graphics.drawable.Icon;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A tab in the {@link CarTabBarView}.
 */
public class CarTabItem {
    @Nullable private Icon mIcon;
    @Nullable private CharSequence mText;

    CarTabItem(Builder builder) {
        mIcon = builder.mIcon;
        mText = builder.mText;
    }

    /**
     * Returns the icon that had been set for this tab during construction.
     *
     * @return The set icon or {@code null} if none set.
     */
    @Nullable
    public Icon getIcon() {
        return mIcon;
    }

    /**
     * Returns the text that had been set for this tab during construction.
     *
     * @return The text of the tab or {@code null} if none set.
     */
    @Nullable
    public CharSequence getText() {
        return mText;
    }

    /**
     * {@code Builder} class for {@link CarTabItem}s.
     */
    public static final class Builder {
        @Nullable Icon mIcon;
        @Nullable CharSequence mText;

        /**
         * Sets the icon that for the tab item.
         *
         * @param icon The icon in the tab.
         * @return This {@code Builder} for method chaining.
         */
        @NonNull
        public Builder setIcon(@Nullable Icon icon) {
            mIcon = icon;
            return this;
        }

        /**
         * Sets the text of the tab.
         *
         * @param text The texts that appears within the tab.
         * @return This {@code Builder} for method chaining.
         */
        @NonNull
        public Builder setText(@Nullable CharSequence text) {
            mText = text;
            return this;
        }

        /**
         * Creates and returns the corresponding {@code CarTabItem} .
         *
         * @return A new {@code CarTabItem}.
         */
        @NonNull
        public CarTabItem build() {
            return new CarTabItem(this);
        }
    }
}
