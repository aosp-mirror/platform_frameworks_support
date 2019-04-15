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
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.car.R;
import androidx.car.util.CarUxRestrictionsUtils;
import androidx.car.uxrestrictions.CarUxRestrictions;
import androidx.constraintlayout.widget.Guideline;

/**
 * Class to build a list item with {@link Switch}.
 *
 * <p>An item supports primary action and a switch as supplemental action.
 *
 * <p>An item visually composes of 3 parts; each part may contain multiple views.
 * <ul>
 * <li>{@code Primary Action}: represented by an icon of following types.
 * <ul>
 * <li>Primary Icon - icon size could be large or small.
 * <li>No Icon - no icon is shown.
 * <li>Empty Icon - {@code Text} offsets start space as if there was an icon.
 * </ul>
 * <li>{@code Text}: supports any combination of the following text views.
 * <ul>
 * <li>Title
 * <li>Body
 * </ul>
 * <li>{@code Supplemental Action}: represented by {@link Switch}.
 * </ul>
 *
 * <p>{@code SwitchListItem} binds data to {@link ViewHolder} based on components selected.
 *
 * <p>When conflicting setter methods are called (e.g. setting primary action to both primary icon
 * and no icon), the last called method wins.
 */
public class SwitchListItem extends CompoundButtonListItem<SwitchListItem.ViewHolder> {

    /**
     * Creates a {@link ViewHolder}.
     */
    @NonNull
    public static ViewHolder createViewHolder(@NonNull View itemView) {
        return new ViewHolder(itemView);
    }

    /**
     * Used by {@link ListItemAdapter} to choose layout to inflate for view holder.
     */
    @Override
    public int getViewType() {
        return ListItemAdapter.LIST_ITEM_TYPE_SWITCH;
    }

    public SwitchListItem(@NonNull Context context) {
        super(context);
    }

    /**
     * ViewHolder that contains necessary widgets for {@link SwitchListItem}.
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

            mCompoundButton = itemView.findViewById(R.id.switch_widget);
            mCompoundButtonDivider = itemView.findViewById(R.id.switch_divider);

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
