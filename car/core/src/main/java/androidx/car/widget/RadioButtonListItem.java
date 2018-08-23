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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.R;
import androidx.car.util.CarUxRestrictionsUtils;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to build a list item with {@link RadioButton}.
 *
 * <p>An item supports primary action and a radio button as supplemental action.
 *
 * <p>An item visually composes of 3 parts.
 * <ul>
 *     <li>{@code Primary Action}: represented by an icon of following types.
 *     <ul>
 *         <li>Primary Icon.
 *         <li>No Icon - no icon is shown.
 *         <li>Empty Icon - {@code Text} offsets start space as if there was an icon.
 *     </ul>
 *     <li>{@code Text}.
 *     <li>{@code Supplemental Action}: represented by {@link RadioButton}.
 * </ul>
 *
 * <p>Clicking {@code RadioButtonListItem} always checks the radio button.
 *
 * <p>When conflicting methods are called (e.g. setting primary action to both primary icon and
 * no icon), the last called method wins.
 */
public class RadioButtonListItem extends ListItem<RadioButtonListItem.ViewHolder> {

    @Retention(SOURCE)
    @IntDef({
            PRIMARY_ACTION_ICON_SIZE_SMALL, PRIMARY_ACTION_ICON_SIZE_MEDIUM,
            PRIMARY_ACTION_ICON_SIZE_LARGE})
    private @interface PrimaryActionIconSize {}

    /**
     * Small sized icon is the mostly commonly used size. It's the same as supplemental action icon.
     */
    public static final int PRIMARY_ACTION_ICON_SIZE_SMALL = 0;
    /**
     * Medium sized icon is slightly bigger than {@code SMALL} ones. It is intended for profile
     * pictures (avatar), in which case caller is responsible for passing in a circular image.
     */
    public static final int PRIMARY_ACTION_ICON_SIZE_MEDIUM = 1;
    /**
     * Large sized icon is as tall as a list item with only {@code title} text. It is intended for
     * album art.
     */
    public static final int PRIMARY_ACTION_ICON_SIZE_LARGE = 2;

    @Retention(SOURCE)
    @IntDef({
            PRIMARY_ACTION_TYPE_NO_ICON, PRIMARY_ACTION_TYPE_EMPTY_ICON,
            PRIMARY_ACTION_TYPE_ICON})
    private @interface PrimaryActionType {}

    private static final int PRIMARY_ACTION_TYPE_NO_ICON = 0;
    private static final int PRIMARY_ACTION_TYPE_EMPTY_ICON = 1;
    private static final int PRIMARY_ACTION_TYPE_ICON = 2;

    private final List<ViewBinder<ViewHolder>> mBinders = new ArrayList<>();
    private final Context mContext;
    private boolean mIsEnabled = true;

    @PrimaryActionType private int mPrimaryActionType = PRIMARY_ACTION_TYPE_NO_ICON;
    private Drawable mPrimaryActionIconDrawable;
    @PrimaryActionIconSize private int mPrimaryActionIconSize = PRIMARY_ACTION_ICON_SIZE_SMALL;

    private CharSequence mText;

    private boolean mIsChecked;
    private boolean mShowDivider;
    private CompoundButton.OnCheckedChangeListener mRadioButtonOnCheckedChangeListener;

    /**
     * Creates a {@link RadioButtonListItem.ViewHolder}.
     */
    public static ViewHolder createViewHolder(@NonNull View itemView) {
        return new ViewHolder(itemView);
    }

    public RadioButtonListItem(@NonNull Context context) {
        mContext = context;
        markDirty();
    }

    /**
     * Used by {@link ListItemAdapter} to choose layout to inflate for view holder.
     */
    @Override
    public int getViewType() {
        return ListItemAdapter.LIST_ITEM_TYPE_RADIO;
    }

    @Override
    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
    }

    @NonNull
    protected Context getContext() {
        return mContext;
    }

    /**
     * Get whether the UI widget is {@code checked}.
     *
     * <p>The return value is in sync with UI state.
     *
     * @return {@code true} if the widget is {@code checked}.
     */
    public boolean isChecked() {
        return mIsChecked;
    }

    /**
     * Sets {@code Primary Action} to be empty icon.
     *
     * <p>{@code Text} would have a start margin as if {@code Primary Action} were set to primary
     * icon.
     */
    public void setPrimaryActionEmptyIcon() {
        mPrimaryActionType = PRIMARY_ACTION_TYPE_EMPTY_ICON;
        markDirty();
    }

    /**
     * Sets {@code Primary Action} to have no icon. Text would align to the start of item.
     */
    public void setPrimaryActionNoIcon() {
        mPrimaryActionType = PRIMARY_ACTION_TYPE_NO_ICON;
        markDirty();
    }

    /**
     * Sets {@code Primary Action} to be represented by an icon.
     *
     * @param iconResId the resource identifier of the drawable.
     */
    public void setPrimaryActionIcon(@DrawableRes int iconResId, @PrimaryActionIconSize int size) {
        setPrimaryActionIcon(mContext.getDrawable(iconResId), size);
    }

    /**
     * Sets {@code Primary Action} to be represented by an icon.
     *
     * @param drawable the Drawable to set, or null to clear the content.
     */
    public void setPrimaryActionIcon(Drawable drawable, @PrimaryActionIconSize int size) {
        mPrimaryActionType = PRIMARY_ACTION_TYPE_ICON;
        mPrimaryActionIconDrawable = drawable;
        mPrimaryActionIconSize = size;
        markDirty();
    }

    /**
     * Sets text to be displayed next to icon.
     *
     * @param text Text to be displayed, or {@code null} to clear the content.
     */
    public void setText(@Nullable CharSequence text) {
        mText = text;
        markDirty();
    }

    /**
     * Sets whether to display a vertical bar that separates {@code text} and
     * {@code Supplemental Icon}.
     */
    @Override
    public void setShowDivider(boolean showDivider) {
        mShowDivider = showDivider;
        markDirty();
    }

    /**
     * Sets {@link android.widget.CompoundButton.OnCheckedChangeListener} of {@code radio button}.
     */
    public void setOnCheckedChangeListener(
            @NonNull CompoundButton.OnCheckedChangeListener listener) {
        mRadioButtonOnCheckedChangeListener = listener;
        markDirty();
    }

    /**
     * Sets the state of {@code RadioButton}.
     *
     * @param isChecked {@code true} to check the button; {@code false} to uncheck it.
     */
    public void setChecked(boolean isChecked) {
        if (mIsChecked == isChecked) {
            return;
        }
        mIsChecked = isChecked;
        markDirty();
    }

    /**
     * Calculates the layout params for views in {@link ViewHolder}.
     */
    @Override
    protected void resolveDirtyState() {
        mBinders.clear();

        // Create binders that adjust layout params of each view.
        setPrimaryAction();
        setTextInternal();
        setRadioButton();
        setOnClickListenerToCheckRadioButton();
    }

    private void setPrimaryAction() {
        setPrimaryIconContent();
        setPrimaryIconLayout();
    }

    private void setTextInternal() {
        setTextContent();
        setTextStartMargin();
    }

    private void setRadioButton() {
        mBinders.add(vh -> {
            // Clear listener before setting checked to avoid listener is notified every time
            // we bind to view holder.
            vh.getRadioButton().setOnCheckedChangeListener(null);
            vh.getRadioButton().setChecked(mIsChecked);
            // Keep internal checked state in sync with UI by wrapping listener.
            vh.getRadioButton().setOnCheckedChangeListener((buttonView, isChecked) -> {
                mIsChecked = isChecked;
                if (mRadioButtonOnCheckedChangeListener != null) {
                    mRadioButtonOnCheckedChangeListener.onCheckedChanged(buttonView, isChecked);
                }
            });

            if (mShowDivider) {
                vh.getRadioButtonDivider().setVisibility(View.VISIBLE);
            }
        });
    }

    private void setPrimaryIconContent() {
        switch (mPrimaryActionType) {
            case PRIMARY_ACTION_TYPE_ICON:
                mBinders.add(vh -> {
                    vh.getPrimaryIcon().setVisibility(View.VISIBLE);
                    vh.getPrimaryIcon().setImageDrawable(mPrimaryActionIconDrawable);
                });
                break;
            case PRIMARY_ACTION_TYPE_EMPTY_ICON:
            case PRIMARY_ACTION_TYPE_NO_ICON:
                // Do nothing.
                break;
            default:
                throw new IllegalStateException("Unknown primary action type.");
        }
    }

    /**
     * Sets the size, start margin, and vertical position of primary icon.
     *
     * <p>Large icon will have no start margin, and always align center vertically.
     *
     * <p>Small/medium icon will have start margin, and uses a top margin such that it is "pinned"
     * at the same position in list item regardless of item height.
     */
    private void setPrimaryIconLayout() {
        if (mPrimaryActionType == PRIMARY_ACTION_TYPE_EMPTY_ICON
                || mPrimaryActionType == PRIMARY_ACTION_TYPE_NO_ICON) {
            return;
        }

        // Size of icon.
        @DimenRes int sizeResId;
        switch (mPrimaryActionIconSize) {
            case PRIMARY_ACTION_ICON_SIZE_SMALL:
                sizeResId = R.dimen.car_primary_icon_size;
                break;
            case PRIMARY_ACTION_ICON_SIZE_MEDIUM:
                sizeResId = R.dimen.car_avatar_icon_size;
                break;
            case PRIMARY_ACTION_ICON_SIZE_LARGE:
                sizeResId = R.dimen.car_single_line_list_item_height;
                break;
            default:
                throw new IllegalStateException("Unknown primary action icon size.");
        }

        int iconSize = mContext.getResources().getDimensionPixelSize(sizeResId);

        // Start margin of icon.
        int startMargin;
        switch (mPrimaryActionIconSize) {
            case PRIMARY_ACTION_ICON_SIZE_SMALL:
            case PRIMARY_ACTION_ICON_SIZE_MEDIUM:
                startMargin = mContext.getResources().getDimensionPixelSize(R.dimen.car_keyline_1);
                break;
            case PRIMARY_ACTION_ICON_SIZE_LARGE:
                startMargin = 0;
                break;
            default:
                throw new IllegalStateException("Unknown primary action icon size.");
        }

        mBinders.add(vh -> {
            ConstraintLayout.LayoutParams layoutParams =
                    (ConstraintLayout.LayoutParams) vh.getPrimaryIcon().getLayoutParams();
            layoutParams.height = layoutParams.width = iconSize;
            layoutParams.setMarginStart(startMargin);

            vh.getPrimaryIcon().requestLayout();
        });
    }

    private void setTextContent() {
        if (!TextUtils.isEmpty(mText)) {
            mBinders.add(vh -> {
                vh.getText().setTextAppearance(getTitleTextAppearance());
                vh.getText().setVisibility(View.VISIBLE);
                vh.getText().setText(mText);
            });
        }
    }

    /**
     * Sets start margin of text view depending on icon type.
     */
    private void setTextStartMargin() {
        @DimenRes int startMarginResId;
        switch (mPrimaryActionType) {
            case PRIMARY_ACTION_TYPE_NO_ICON:
                startMarginResId = R.dimen.car_keyline_1;
                break;
            case PRIMARY_ACTION_TYPE_EMPTY_ICON:
                startMarginResId = R.dimen.car_keyline_3;
                break;
            case PRIMARY_ACTION_TYPE_ICON:
                startMarginResId = mPrimaryActionIconSize == PRIMARY_ACTION_ICON_SIZE_LARGE
                        ? R.dimen.car_keyline_4
                        : R.dimen.car_keyline_3;  // Small and medium sized icon.
                break;
            default:
                throw new IllegalStateException("Unknown primary action type.");
        }
        int startMargin = mContext.getResources().getDimensionPixelSize(startMarginResId);
        mBinders.add(vh -> {
            ViewGroup.MarginLayoutParams layoutParams =
                    (ViewGroup.MarginLayoutParams) vh.getText().getLayoutParams();
            layoutParams.setMarginStart(startMargin);
            vh.getText().requestLayout();
        });
    }

    // Clicking the item always checks radio button.
    private void setOnClickListenerToCheckRadioButton() {
        mBinders.add(vh -> {
            vh.itemView.setClickable(true);
            vh.itemView.setOnClickListener(v -> vh.getRadioButton().setChecked(true));
        });
    }

    /**
     * Hides all views in {@link ViewHolder} then applies ViewBinders to adjust view layout params.
     */
    @Override
    protected void onBind(ViewHolder viewHolder) {
        // Hide all subviews then apply view binders to adjust subviews.
        hideSubViews(viewHolder);
        for (ViewBinder binder : mBinders) {
            binder.bind(viewHolder);
        }

        for (View v : viewHolder.getWidgetViews()) {
            v.setEnabled(mIsEnabled);
        }
    }

    private void hideSubViews(ViewHolder vh) {
        for (View v : vh.getWidgetViews()) {
            v.setVisibility(View.GONE);
        }
        // Radio button is always visible.
        vh.getRadioButton().setVisibility(View.VISIBLE);
    }

    /**
     * Holds views of RadioButtonListItem.
     */
    public static class ViewHolder extends ListItem.ViewHolder {

        private final View[] mWidgetViews;

        private ViewGroup mContainerLayout;

        private ImageView mPrimaryIcon;
        private TextView mText;

        private View mRadioButtonDivider;
        private RadioButton mRadioButton;

        private View mClickInterceptor;

        public ViewHolder(View itemView) {
            super(itemView);

            mContainerLayout = itemView.findViewById(R.id.container);

            mPrimaryIcon = itemView.findViewById(R.id.primary_icon);
            mText = itemView.findViewById(R.id.text);

            mRadioButton = itemView.findViewById(R.id.radio_button);
            mRadioButtonDivider = itemView.findViewById(R.id.radio_button_divider);

            int minTouchSize = itemView.getContext().getResources()
                    .getDimensionPixelSize(R.dimen.car_touch_target_size);

            MinTouchTargetHelper.ensureThat(mRadioButton)
                    .hasMinTouchSize(minTouchSize);

            mClickInterceptor = itemView.findViewById(R.id.click_interceptor);
            // The radio button is always touch-able, so activate the intercept view.
            mClickInterceptor.setClickable(true);
            mClickInterceptor.setVisibility(View.VISIBLE);

            // Each line groups relevant child views in an effort to help keep this view array
            // updated with actual child views in the ViewHolder.
            mWidgetViews = new View[]{
                    mPrimaryIcon, mText,
                    mRadioButton, mRadioButtonDivider};
        }

        @NonNull
        public ViewGroup getContainerLayout() {
            return mContainerLayout;
        }

        @NonNull
        public ImageView getPrimaryIcon() {
            return mPrimaryIcon;
        }

        @NonNull
        public TextView getText() {
            return mText;
        }

        @NonNull
        public RadioButton getRadioButton() {
            return mRadioButton;
        }

        @NonNull
        public View getRadioButtonDivider() {
            return mRadioButtonDivider;
        }

        @NonNull
        View[] getWidgetViews() {
            return mWidgetViews;
        }

        /**
         * Returns the view that will intercept clicks beneath the radio button.
         */
        @NonNull
        View getClickInterceptView() {
            return mClickInterceptor;
        }

        @Override
        public void onUxRestrictionsChanged(
                androidx.car.uxrestrictions.CarUxRestrictions restrictionInfo) {
            CarUxRestrictionsUtils.apply(itemView.getContext(), restrictionInfo, getText());
        }
    }
}
