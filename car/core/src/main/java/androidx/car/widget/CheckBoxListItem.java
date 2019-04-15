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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.car.R;
import androidx.car.util.CarUxRestrictionsUtils;
import androidx.car.uxrestrictions.CarUxRestrictions;
import androidx.constraintlayout.widget.Guideline;

/**
 * Class to build a list item with {@link CheckBox}.
 *
 * <p>A checkbox list item is visually composed of 5 parts.
 * <ul>
 * <li>optional {@code Primary Action Icon}.
 * <li>optional {@code Title}.
 * <li>optional {@code Body}.
 * <li>optional {@code Divider}.
 * <li>A {@link CheckBox}.
 * </ul>
 */
public class CheckBoxListItem extends CompoundButtonListItem<CheckBoxListItem.ViewHolder> {

    /**
     * Creates a {@link ViewHolder}.
     */
    @NonNull
    public static ViewHolder createViewHolder(@NonNull View itemView) {
        return new ViewHolder(itemView);
    }

    public CheckBoxListItem(@NonNull Context context) {
        super(context);
    }

    /**
     * Used by {@link ListItemAdapter} to choose layout to inflate for view holder.
     */
    @Override
    public int getViewType() {
        return ListItemAdapter.LIST_ITEM_TYPE_CHECK_BOX;
    }

    /**
     * ViewHolder that contains necessary widgets for {@link CheckBoxListItem}.
     */
    public static final class ViewHolder extends CompoundButtonListItem.ViewHolder {

        private View[] mWidgetViews;

        private ViewGroup mContainerLayout;

        private ImageView mPrimaryIcon;

        private TextView mTitle;
        private TextView mBody;

        private Guideline mSupplementalGuideline;

        private CompoundButton mCompoundButton;
        private View mCompoundButtonDivider;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            mContainerLayout = itemView.findViewById(R.id.container);

            mPrimaryIcon = itemView.findViewById(R.id.primary_icon);

            mTitle = itemView.findViewById(R.id.title);
            mBody = itemView.findViewById(R.id.body);

            mSupplementalGuideline = itemView.findViewById(R.id.supplemental_actions_guideline);

            mCompoundButton = itemView.findViewById(R.id.checkbox_widget);
            mCompoundButtonDivider = itemView.findViewById(R.id.checkbox_divider);

            int minTouchSize = itemView.getContext().getResources()
                    .getDimensionPixelSize(R.dimen.car_touch_target_size);
            MinTouchTargetHelper.ensureThat(mCompoundButton).hasMinTouchSize(minTouchSize);

            // Each line groups relevant child views in an effort to help keep this view array
            // updated with actual child views in the ViewHolder.
            mWidgetViews = new View[]{
                    mPrimaryIcon,
                    mTitle, mBody,
                    mCompoundButton, mCompoundButtonDivider,
            };
        }

        /**
         * Updates child views with current car UX restrictions.
         *
         * <p>{@code Text} might be truncated to meet length limit required by regulation.
         *
         * @param restrictionsInfo current car UX restrictions.
         */
        @Override
        public void onUxRestrictionsChanged(CarUxRestrictions restrictionsInfo) {
            CarUxRestrictionsUtils.apply(itemView.getContext(), restrictionsInfo, getBody());
        }

        @NonNull
        public ImageView getPrimaryIcon() {
            return mPrimaryIcon;
        }

        @NonNull
        public TextView getTitle() {
            return mTitle;
        }

        @NonNull
        public TextView getBody() {
            return mBody;
        }

        @NonNull
        public View getCompoundButtonDivider() {
            return mCompoundButtonDivider;
        }

        @NonNull
        public CompoundButton getCompoundButton() {
            return mCompoundButton;
        }

        @NonNull
        Guideline getSupplementalGuideline() {
            return mSupplementalGuideline;
        }

        @NonNull
        View[] getWidgetViews() {
            return mWidgetViews;
        }

        @NonNull
        public ViewGroup getContainerLayout() {
            return mContainerLayout;
        }
    }
}
