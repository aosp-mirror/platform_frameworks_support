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
<<<<<<< HEAD   (80d066 Merge "Merge empty history for sparse-5530831-L2560000030742)
import android.graphics.drawable.Icon;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
=======
>>>>>>> BRANCH (393684 Merge "Merge cherrypicks of [961903] into sparse-5567208-L67)
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

<<<<<<< HEAD   (80d066 Merge "Merge empty history for sparse-5530831-L2560000030742)
import androidx.annotation.DimenRes;
import androidx.annotation.IntDef;
=======
>>>>>>> BRANCH (393684 Merge "Merge cherrypicks of [961903] into sparse-5567208-L67)
import androidx.annotation.NonNull;
import androidx.car.R;
import androidx.car.util.CarUxRestrictionsUtils;
import androidx.car.uxrestrictions.CarUxRestrictions;
import androidx.car.widget.ListItemAdapter.ListItemType;
import androidx.constraintlayout.widget.Guideline;

/**
 * Class to build a list item with {@link RadioButton}.
 *
<<<<<<< HEAD   (80d066 Merge "Merge empty history for sparse-5530831-L2560000030742)
 * <p>A radio button list item visually composes of 3 parts.
=======
 * <p>A radio button list item is visually composed of 5 parts.
>>>>>>> BRANCH (393684 Merge "Merge cherrypicks of [961903] into sparse-5567208-L67)
 * <ul>
<<<<<<< HEAD   (80d066 Merge "Merge empty history for sparse-5530831-L2560000030742)
 *     <li>optional {@code Primary Action Icon}.
 *     <li>optional {@code Text}.
 *     <li>A {@link RadioButton}.
=======
 * <li>A {@link RadioButton}.
 * <li>optional {@code Divider}.
 * <li>optional {@code Primary Action Icon}.
 * <li>optional {@code Title}.
 * <li>optional {@code Body}.
>>>>>>> BRANCH (393684 Merge "Merge cherrypicks of [961903] into sparse-5567208-L67)
 * </ul>
 */
<<<<<<< HEAD   (80d066 Merge "Merge empty history for sparse-5530831-L2560000030742)
public class RadioButtonListItem extends ListItem<RadioButtonListItem.ViewHolder> {

    @Retention(SOURCE)
    @IntDef({
            PRIMARY_ACTION_ICON_SIZE_SMALL, PRIMARY_ACTION_ICON_SIZE_MEDIUM,
            PRIMARY_ACTION_ICON_SIZE_LARGE})
    private @interface PrimaryActionIconSize {}
=======
public final class RadioButtonListItem extends
        CompoundButtonListItem<RadioButtonListItem.ViewHolder> {

>>>>>>> BRANCH (393684 Merge "Merge cherrypicks of [961903] into sparse-5567208-L67)
    /**
<<<<<<< HEAD   (80d066 Merge "Merge empty history for sparse-5530831-L2560000030742)
     * Small sized icon is the mostly commonly used size.
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

    private final List<ViewBinder<ViewHolder>> mBinders = new ArrayList<>();
    private final Context mContext;
    private boolean mIsEnabled = true;

    @Nullable private Icon mPrimaryActionIcon;
    @PrimaryActionIconSize private int mPrimaryActionIconSize = PRIMARY_ACTION_ICON_SIZE_SMALL;

    private int mTextStartMargin;
    private CharSequence mText;

    private boolean mIsChecked;
    private boolean mShowRadioButtonDivider;
    private CompoundButton.OnCheckedChangeListener mRadioButtonOnCheckedChangeListener;

    /**
     * Creates a {@link RadioButtonListItem.ViewHolder}.
=======
     * Creates a {@link ViewHolder}.
     *
     * @return a {@link ViewHolder} for this {@link RadioButtonListItem}.
>>>>>>> BRANCH (393684 Merge "Merge cherrypicks of [961903] into sparse-5567208-L67)
     */
    @NonNull
    public static ViewHolder createViewHolder(@NonNull View itemView) {
        return new ViewHolder(itemView);
    }

    /**
     * Creates a {@link RadioButtonListItem} that will be used to display a list item with a
     * {@link RadioButton}.
     *
     * @param context The context to be used by this {@link RadioButtonListItem}.
     */
    public RadioButtonListItem(@NonNull Context context) {
<<<<<<< HEAD   (80d066 Merge "Merge empty history for sparse-5530831-L2560000030742)
        mContext = context;
        markDirty();
=======
        super(context);
    }

    /**
     * Returns whether the compound button will be placed at the end of the list item layout. This
     * value is used to determine start margins for the {@code Title} and {@code Body}.
     *
     * @return Whether compound button is placed at the end of the list item layout.
     */
    @Override
    public boolean isCompoundButtonPositionEnd() {
        return false;
>>>>>>> BRANCH (393684 Merge "Merge cherrypicks of [961903] into sparse-5567208-L67)
    }

    /**
     * Used by {@link ListItemAdapter} to choose layout to inflate for view holder.
     *
     * @return Type of this {@link CompoundButtonListItem}.
     */
    @ListItemType
    @Override
    public int getViewType() {
        return ListItemAdapter.LIST_ITEM_TYPE_RADIO;
    }

    /**
     * ViewHolder that contains necessary widgets for {@link RadioButtonListItem}.
     */
    public static final class ViewHolder extends CompoundButtonListItem.ViewHolder {

<<<<<<< HEAD   (80d066 Merge "Merge empty history for sparse-5530831-L2560000030742)
    /**
     * Get whether the radio button is checked.
     *
     * <p>The return value is in sync with UI state.
     *
     * @return {@code true} if the widget is checked; {@code false} otherwise.
     */
    public boolean isChecked() {
        return mIsChecked;
    }

    /**
     * Sets {@code Primary Action} to be represented by an icon. The size of icon automatically
     * adjusts the start of {@code Text}.
     *
     * @param icon the icon to set as primary action. Setting {@code null} clears the icon and
     *             aligns text to the start of list item; {@code size} will be ignored.
     * @param size constant that represents the size of icon. See
     *             {@link #PRIMARY_ACTION_ICON_SIZE_SMALL},
     *             {@link #PRIMARY_ACTION_ICON_SIZE_MEDIUM}, and
     *             {@link #PRIMARY_ACTION_ICON_SIZE_LARGE}.
     *             If {@code null} is passed in for icon, size will be ignored.
     */
    public void setPrimaryActionIcon(@NonNull Icon icon, @PrimaryActionIconSize int size) {
        mPrimaryActionIcon = icon;
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
     * Sets the start margin of text.
     */
    public void setTextStartMargin(@DimenRes int dimenRes) {
        mTextStartMargin = mContext.getResources().getDimensionPixelSize(dimenRes);
        markDirty();
    }

    /**
     * Sets whether to display a vertical bar that separates {@code text} and radio button.
     */
    public void setShowRadioButtonDivider(boolean showDivider) {
        mShowRadioButtonDivider = showDivider;
        markDirty();
    }

    /**
     * Sets {@link android.widget.CompoundButton.OnCheckedChangeListener} of radio button.
     */
    public void setOnCheckedChangeListener(
            @NonNull CompoundButton.OnCheckedChangeListener listener) {
        mRadioButtonOnCheckedChangeListener = listener;
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

            vh.getRadioButtonDivider().setVisibility(
                    mShowRadioButtonDivider ? View.VISIBLE : View.GONE);
        });
    }

    private void setPrimaryIconContent() {
        mBinders.add(vh -> {
            if (mPrimaryActionIcon == null) {
                vh.getPrimaryIcon().setVisibility(View.GONE);
            } else {
                vh.getPrimaryIcon().setVisibility(View.VISIBLE);
                mPrimaryActionIcon.loadDrawableAsync(getContext(),
                        drawable -> vh.getPrimaryIcon().setImageDrawable(drawable),
                        new Handler(Looper.getMainLooper()));
            }
        });
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
        if (mPrimaryActionIcon == null) {
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
            ViewGroup.MarginLayoutParams layoutParams =
                    (ViewGroup.MarginLayoutParams) vh.getPrimaryIcon().getLayoutParams();
            layoutParams.height = layoutParams.width = iconSize;
            layoutParams.setMarginStart(startMargin);

            vh.getPrimaryIcon().requestLayout();
        });
    }

    private void setTextContent() {
        if (!TextUtils.isEmpty(mText)) {
            mBinders.add(vh -> {
                vh.getText().setVisibility(View.VISIBLE);
                vh.getText().setText(mText);
            });
        }
    }

    /**
     * Sets start margin of text view depending on icon type.
     */
    private void setTextStartMargin() {
        int offset = 0;
        if (mPrimaryActionIcon != null) {
            // If there is an icon, offset text to accommodate it.
            @DimenRes int startMarginResId =
                    mPrimaryActionIconSize == PRIMARY_ACTION_ICON_SIZE_LARGE
                            ? R.dimen.car_keyline_4
                            : R.dimen.car_keyline_3;  // Small and medium sized icon.
            offset = mContext.getResources().getDimensionPixelSize(startMarginResId);
        }

        int startMargin = offset + mTextStartMargin;
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
    public static final class ViewHolder extends ListItem.ViewHolder {

        private final View[] mWidgetViews;
=======
        private View[] mWidgetViews;
>>>>>>> BRANCH (393684 Merge "Merge cherrypicks of [961903] into sparse-5567208-L67)

        private ViewGroup mContainerLayout;

        private ImageView mPrimaryIcon;
<<<<<<< HEAD   (80d066 Merge "Merge empty history for sparse-5530831-L2560000030742)
        private TextView mText;
=======

        private TextView mTitle;
        private TextView mBody;
>>>>>>> BRANCH (393684 Merge "Merge cherrypicks of [961903] into sparse-5567208-L67)

        private Guideline mSupplementalGuideline;

        private CompoundButton mCompoundButton;
        private View mCompoundButtonDivider;

        /**
         * Creates a {@link ViewHolder} for a {@link RadioButtonListItem}.
         *
         * @param itemView The view to be used to display a {@link RadioButtonListItem}.
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            mContainerLayout = itemView.findViewById(R.id.container);

            mPrimaryIcon = itemView.findViewById(R.id.primary_icon);
<<<<<<< HEAD   (80d066 Merge "Merge empty history for sparse-5530831-L2560000030742)
            mText = itemView.findViewById(R.id.text);
=======

            mTitle = itemView.findViewById(R.id.title);
            mBody = itemView.findViewById(R.id.body);
>>>>>>> BRANCH (393684 Merge "Merge cherrypicks of [961903] into sparse-5567208-L67)

            mSupplementalGuideline = itemView.findViewById(R.id.supplemental_actions_guideline);

            mCompoundButton = itemView.findViewById(R.id.radiobutton_widget);
            mCompoundButtonDivider = itemView.findViewById(R.id.radiobutton_divider);

            int minTouchSize = itemView.getContext().getResources()
                    .getDimensionPixelSize(R.dimen.car_touch_target_size);
            MinTouchTargetHelper.ensureThat(mCompoundButton).hasMinTouchSize(minTouchSize);

            // Each line groups relevant child views in an effort to help keep this view array
            // updated with actual child views in the ViewHolder.
            mWidgetViews = new View[]{
<<<<<<< HEAD   (80d066 Merge "Merge empty history for sparse-5530831-L2560000030742)
                    mPrimaryIcon, mText,
                    mRadioButton, mRadioButtonDivider};
=======
                    mPrimaryIcon,
                    mTitle, mBody,
                    mCompoundButton, mCompoundButtonDivider,
            };
>>>>>>> BRANCH (393684 Merge "Merge cherrypicks of [961903] into sparse-5567208-L67)
        }

        /**
         * Updates child views with current car UX restrictions.
         *
         * <p>{@code Text} might be truncated to meet length limit required by regulation.
         *
         * @param restrictionsInfo current car UX restrictions.
         */
        @Override
        public void onUxRestrictionsChanged(@NonNull CarUxRestrictions restrictionsInfo) {
            CarUxRestrictionsUtils.apply(itemView.getContext(), restrictionsInfo, getBody());
        }

        /**
         * Returns the primary icon view within this view holder's view.
         *
         * @return Icon view within this view holder's view.
         */
        @NonNull
        public ImageView getPrimaryIcon() {
            return mPrimaryIcon;
        }

        /**
         * Returns the title view within this view holder's view.
         *
         * @return Title view within this view holder's view.
         */
        @NonNull
<<<<<<< HEAD   (80d066 Merge "Merge empty history for sparse-5530831-L2560000030742)
        public TextView getText() {
            return mText;
=======
        public TextView getTitle() {
            return mTitle;
        }

        /**
         * Returns the body view within this view holder's view.
         *
         * @return Body view within this view holder's view.
         */
        @NonNull
        public TextView getBody() {
            return mBody;
>>>>>>> BRANCH (393684 Merge "Merge cherrypicks of [961903] into sparse-5567208-L67)
        }

        /**
         * Returns the compound button divider view within this view holder's view.
         *
         * @return Compound button divider view within this view holder's view.
         */
        @NonNull
        public View getCompoundButtonDivider() {
            return mCompoundButtonDivider;
        }

        /**
         * Returns the compound button within this view holder's view.
         *
         * @return Compound button within this view holder's view.
         */
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

<<<<<<< HEAD   (80d066 Merge "Merge empty history for sparse-5530831-L2560000030742)
        @Override
        public void onUxRestrictionsChanged(
                androidx.car.uxrestrictions.CarUxRestrictions restrictionInfo) {
            CarUxRestrictionsUtils.apply(itemView.getContext(), restrictionInfo, getText());
=======
        /**
         * Returns the container layout of this view holder.
         *
         * @return Container layout of this view holder.
         */
        @NonNull
        public ViewGroup getContainerLayout() {
            return mContainerLayout;
>>>>>>> BRANCH (393684 Merge "Merge cherrypicks of [961903] into sparse-5567208-L67)
        }
    }
}
