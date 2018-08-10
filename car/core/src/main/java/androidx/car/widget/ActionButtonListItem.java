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

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.car.R;
import androidx.car.util.CarUxRestrictionsUtils;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to build a list item that has up to two action buttons.
 *
 * <p>An item visually composes of 3 parts; each part may contain multiple views.
 * <ul>
 *     <li>{@code Primary Action}: represented by an icon of following types.
 *     <ul>
 *         <li>Primary Icon - icon size could be large or small.
 *         <li>No Icon - no icon is shown.
 *         <li>Empty Icon - {@code Text} offsets start space as if there was an icon.
 *     </ul>
 *     <li>{@code Text}: supports any combination of the following text views.
 *     <ul>
 *         <li>Title
 *         <li>Body
 *     </ul>
 *     <li>{@code Supplemental Action}: Up to two action buttons.
 * </ul>
 *
 * <p>{@code ActionButtonListItem} binds data to {@link ViewHolder} based on components selected.
 *
 * <p>When conflicting setter methods are called (e.g. setting primary action to both primary icon
 * and no icon), the last called method wins.
 */
public class ActionButtonListItem extends ListItem<ActionButtonListItem.ViewHolder> {
    @Retention(SOURCE)
    @IntDef({PRIMARY_ACTION_ICON_SIZE_SMALL, PRIMARY_ACTION_ICON_SIZE_MEDIUM,
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

    @Retention(SOURCE)
    @IntDef({ SUPPLEMENTAL_ACTION_ONE_ACTION, SUPPLEMENTAL_ACTION_TWO_ACTIONS })
    private @interface SupplementalActionType {}

    private static final int SUPPLEMENTAL_ACTION_ONE_ACTION = 0;
    private static final int SUPPLEMENTAL_ACTION_TWO_ACTIONS = 1;

    @PrimaryActionType private int mPrimaryActionType = PRIMARY_ACTION_TYPE_NO_ICON;
    private Drawable mPrimaryActionIconDrawable;
    @PrimaryActionIconSize private int mPrimaryActionIconSize = PRIMARY_ACTION_ICON_SIZE_SMALL;

    private final Context mContext;
    private boolean mIsEnabled = true;
    private final List<ViewBinder<ViewHolder>> mBinders = new ArrayList<>();

    private String mTitle;
    private String mBody;
    private boolean mIsBodyPrimary;

    @SupplementalActionType private int mSupplementalActionType = SUPPLEMENTAL_ACTION_ONE_ACTION;

    private String mAction1Text;
    private View.OnClickListener mAction1OnClickListener;
    private boolean mShowAction1Divider;
    private String mAction2Text;
    private View.OnClickListener mAction2OnClickListener;
    private boolean mShowAction2Divider;

    private View.OnClickListener mOnClickListener;

    /**
     * Creates a {@link ActionButtonListItem.ViewHolder}.
     */
    public static ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    public ActionButtonListItem(Context context) {
        mContext = context;
        markDirty();
    }

    /**
     * Used by {@link ListItemAdapter} to choose layout to inflate for view holder.
     */
    @Override
    public int getViewType() {
        return ListItemAdapter.LIST_ITEM_TYPE_ACTION_BUTTONS;
    }

    /**
     * Hides all views in {@link ActionButtonListItem.ViewHolder} then applies ViewBinders to
     * adjust view layout params.
     */
    @Override
    public void onBind(ActionButtonListItem.ViewHolder viewHolder) {
        hideSubViews(viewHolder);
        for (ViewBinder<ViewHolder> binder : mBinders) {
            binder.bind(viewHolder);
        }

        for (View v : viewHolder.getWidgetViews()) {
            v.setEnabled(mIsEnabled);
        }
        // ActionButtonListItem supports clicking on the item so we also update the entire itemView.
        viewHolder.itemView.setEnabled(mIsEnabled);
    }

    /** Sets the title text appearance from the specified style resource. */
    @Override
    void setTitleTextAppearance(@StyleRes int titleTextAppearance) {
        super.setTitleTextAppearance(titleTextAppearance);
        setTextContent();
    }

    /** Sets the body text appearance from the specified style resource. */
    @Override
    void setBodyTextAppearance(@StyleRes int bodyTextAppearance) {
        super.setBodyTextAppearance(bodyTextAppearance);
        setTextContent();
    }

    @Override
    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
    }

    /**
     * Calculates the layout params for views in {@link ViewHolder}.
     */
    @Override
    protected void resolveDirtyState() {
        mBinders.clear();

        // Create binders that adjust layout params of each view.
        setPrimaryAction();
        setText();
        setSupplementalActions();
        setOnClickListener();
    }

    private void setPrimaryAction() {
        setPrimaryIconContent();
        setPrimaryIconLayout();
    }

    private void setText() {
        setTextContent();
        setTextVerticalMargin();
        setTextStartMargin();
    }

    private void setOnClickListener() {
        mBinders.add(vh -> {
            vh.itemView.setOnClickListener(mOnClickListener);
            vh.itemView.setClickable(mOnClickListener != null);
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

            if (mPrimaryActionIconSize == PRIMARY_ACTION_ICON_SIZE_LARGE) {
                // A large icon is always vertically centered.
                layoutParams.verticalBias = 0.5f;
                layoutParams.topMargin = 0;
            } else {
                // Align the icon to the top of the parent. This allows the topMargin to shift it
                // down relative to the top.
                layoutParams.verticalBias = 0f;

                // For all other icon sizes, the icon should be centered within the height of
                // car_double_line_list_item_height. Note: the actual height of the item can be
                // larger than this.
                int itemHeight = mContext.getResources().getDimensionPixelSize(
                        R.dimen.car_double_line_list_item_height);
                layoutParams.topMargin = (itemHeight - iconSize) / 2;
            }

            vh.getPrimaryIcon().requestLayout();
        });
    }

    private void setTextContent() {
        if (!TextUtils.isEmpty(mTitle)) {
            mBinders.add(vh -> {
                vh.getTitle().setVisibility(View.VISIBLE);
                vh.getTitle().setText(mTitle);
            });
        }
        if (!TextUtils.isEmpty(mBody)) {
            mBinders.add(vh -> {
                vh.getBody().setVisibility(View.VISIBLE);
                vh.getBody().setText(mBody);
            });
        }

        if (mIsBodyPrimary) {
            mBinders.add(vh -> {
                vh.getTitle().setTextAppearance(getBodyTextAppearance());
                vh.getBody().setTextAppearance(getTitleTextAppearance());
            });
        } else {
            mBinders.add(vh -> {
                vh.getTitle().setTextAppearance(getTitleTextAppearance());
                vh.getBody().setTextAppearance(getBodyTextAppearance());
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
            ViewGroup.MarginLayoutParams titleLayoutParams =
                    (ViewGroup.MarginLayoutParams) vh.getTitle().getLayoutParams();
            titleLayoutParams.setMarginStart(startMargin);
            vh.getTitle().requestLayout();

            ViewGroup.MarginLayoutParams bodyLayoutParams =
                    (ViewGroup.MarginLayoutParams) vh.getBody().getLayoutParams();
            bodyLayoutParams.setMarginStart(startMargin);
            vh.getBody().requestLayout();
        });
    }

    /**
     * Sets top/bottom margins of {@code Title} and {@code Body}.
     */
    private void setTextVerticalMargin() {
        // Set all relevant fields in layout params to avoid carried over params when the item
        // gets bound to a recycled view holder.
        if (!TextUtils.isEmpty(mTitle) && TextUtils.isEmpty(mBody)) {
            // Title only - view is aligned center vertically by itself.
            mBinders.add(vh -> {
                ViewGroup.MarginLayoutParams layoutParams =
                        (ViewGroup.MarginLayoutParams) vh.getTitle().getLayoutParams();
                layoutParams.topMargin = 0;
                vh.getTitle().requestLayout();
            });
        } else if (TextUtils.isEmpty(mTitle) && !TextUtils.isEmpty(mBody)) {
            mBinders.add(vh -> {
                // Body uses top and bottom margin.
                int margin = mContext.getResources().getDimensionPixelSize(
                        R.dimen.car_padding_3);
                ViewGroup.MarginLayoutParams layoutParams =
                        (ViewGroup.MarginLayoutParams) vh.getBody().getLayoutParams();
                layoutParams.topMargin = margin;
                layoutParams.bottomMargin = margin;
                vh.getBody().requestLayout();
            });
        } else {
            mBinders.add(vh -> {
                Resources resources = mContext.getResources();
                int padding2 = resources.getDimensionPixelSize(R.dimen.car_padding_2);

                // Title has a top margin
                ViewGroup.MarginLayoutParams titleLayoutParams =
                        (ViewGroup.MarginLayoutParams) vh.getTitle().getLayoutParams();
                titleLayoutParams.topMargin = padding2;
                vh.getTitle().requestLayout();

                // Body is below title with no margin and has bottom margin.
                ViewGroup.MarginLayoutParams bodyLayoutParams =
                        (ViewGroup.MarginLayoutParams) vh.getBody().getLayoutParams();
                bodyLayoutParams.topMargin = 0;
                bodyLayoutParams.bottomMargin = padding2;
                vh.getBody().requestLayout();
            });
        }
    }

    /**
     * Sets up view(s) for supplemental action.
     */
    private void setSupplementalActions() {
        switch (mSupplementalActionType) {
            case SUPPLEMENTAL_ACTION_TWO_ACTIONS:
                mBinders.add(vh -> {
                    vh.getAction2().setVisibility(View.VISIBLE);
                    if (mShowAction2Divider) {
                        vh.getAction2Divider().setVisibility(View.VISIBLE);
                    }

                    vh.getAction2().setText(mAction2Text);
                    vh.getAction2().setOnClickListener(mAction2OnClickListener);

                    vh.getAction1().setVisibility(View.VISIBLE);
                    if (mShowAction1Divider) {
                        vh.getAction1Divider().setVisibility(View.VISIBLE);
                    }

                    vh.getAction1().setText(mAction1Text);
                    vh.getAction1().setOnClickListener(mAction1OnClickListener);
                });
                break;
            case SUPPLEMENTAL_ACTION_ONE_ACTION:
                mBinders.add(vh -> {
                    vh.getAction1().setVisibility(View.VISIBLE);
                    if (mShowAction1Divider) {
                        vh.getAction1Divider().setVisibility(View.VISIBLE);
                    }

                    vh.getAction1().setText(mAction1Text);
                    vh.getAction1().setOnClickListener(mAction1OnClickListener);
                });
                break;
            default:
                throw new IllegalStateException("Unknown supplemental action type.");
        }
    }

    /**
     * Sets {@link View.OnClickListener} of {@code ActionButtonListItem}.
     */
    public void setOnClickListener(View.OnClickListener listener) {
        mOnClickListener = listener;
        markDirty();
    }

    /**
     * Sets {@code Primary Action} to be represented by an icon.
     *
     * @param iconResId the resource identifier of the drawable.
     * @param size The size of the icon. Must be one of {@link #PRIMARY_ACTION_ICON_SIZE_SMALL},
     *             {@link #PRIMARY_ACTION_ICON_SIZE_MEDIUM}, or
     *             {@link #PRIMARY_ACTION_ICON_SIZE_LARGE}.
     */
    public void setPrimaryActionIcon(@DrawableRes int iconResId, @PrimaryActionIconSize int size) {
        setPrimaryActionIcon(mContext.getDrawable(iconResId), size);
    }

    /**
     * Sets {@code Primary Action} to be represented by an icon.
     *
     * @param drawable the Drawable to set, or null to clear the content.
     * @param size The size of the icon. Must be one of {@link #PRIMARY_ACTION_ICON_SIZE_SMALL},
     *             {@link #PRIMARY_ACTION_ICON_SIZE_MEDIUM}, or
     *             {@link #PRIMARY_ACTION_ICON_SIZE_LARGE}.
     */
    public void setPrimaryActionIcon(@Nullable Drawable drawable, @PrimaryActionIconSize int size) {
        mPrimaryActionType = PRIMARY_ACTION_TYPE_ICON;
        mPrimaryActionIconDrawable = drawable;
        mPrimaryActionIconSize = size;
        markDirty();
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
     * Sets the title of item.
     *
     * <p>Primary text is {@code Title} by default. It can be set by
     * {@link #setBody(String, boolean)}
     *
     * <p>{@code Title} text is limited to one line, and ellipses at the end.
     *
     * @param title text to display as title.
     */
    public void setTitle(String title) {
        mTitle = title;
        markDirty();
    }

    /**
     * Sets the body text of item.
     *
     * <p>Text beyond length required by regulation will be truncated. Defaults {@code Title}
     * text as the primary.
     * @param body text to be displayed.
     */
    public void setBody(String body) {
        setBody(body, false);
    }

    /**
     * Sets the body text of item.
     *
     * @param body text to be displayed.
     * @param asPrimary sets {@code Body Text} as primary text of item.
     */
    public void setBody(String body, boolean asPrimary) {
        mBody = body;
        mIsBodyPrimary = asPrimary;

        markDirty();
    }

    /**
     * Sets {@code Supplemental Action} to be represented by an {@code Action Button}.
     *
     * @param text button text to display.
     * @param showDivider whether to display a vertical bar that separates {@code Text} and
     *                    {@code Action Button}.
     * @param listener the callback that will run when action button is clicked.
     */
    public void setAction(String text, boolean showDivider, View.OnClickListener listener) {
        if (TextUtils.isEmpty(text)) {
            throw new IllegalArgumentException("Action text cannot be empty.");
        }
        if (listener == null) {
            throw new IllegalArgumentException("Action OnClickListener cannot be null.");
        }
        mSupplementalActionType = SUPPLEMENTAL_ACTION_ONE_ACTION;

        mAction1Text = text;
        mAction1OnClickListener = listener;
        mShowAction1Divider = showDivider;

        markDirty();
    }

    /**
     * Sets {@code Supplemental Action} to be represented by two {@code Action Button}s.
     *
     * <p>These two action buttons will be aligned towards item end.
     *
     * @param action1Text button text to display - this button will be closer to item end.
     * @param action2Text button text to display.
     */
    public void setActions(String action1Text, boolean showAction1Divider,
            View.OnClickListener action1OnClickListener,
            String action2Text, boolean showAction2Divider,
            View.OnClickListener action2OnClickListener) {
        if (TextUtils.isEmpty(action1Text) || TextUtils.isEmpty(action2Text)) {
            throw new IllegalArgumentException("Action text cannot be empty.");
        }
        if (action1OnClickListener == null || action2OnClickListener == null) {
            throw new IllegalArgumentException("Action OnClickListener cannot be null.");
        }
        mSupplementalActionType = SUPPLEMENTAL_ACTION_TWO_ACTIONS;

        mAction1Text = action1Text;
        mAction1OnClickListener = action1OnClickListener;
        mShowAction1Divider = showAction1Divider;
        mAction2Text = action2Text;
        mAction2OnClickListener = action2OnClickListener;
        mShowAction2Divider = showAction2Divider;

        markDirty();
    }

    private void hideSubViews(ViewHolder vh) {
        for (View v : vh.getWidgetViews()) {
            v.setVisibility(View.GONE);
        }
    }

    /**
     * Holds the children views of {@link ActionButtonListItem}.
     */
    public static class ViewHolder extends ListItem.ViewHolder {
        private final View[] mWidgetViews;

        private ImageView mPrimaryIcon;

        private TextView mTitle;
        private TextView mBody;

        private Button mAction1;
        private View mAction1Divider;

        private Button mAction2;
        private View mAction2Divider;

        private View mClickInterceptor;

        public ViewHolder(View itemView) {
            super(itemView);

            mPrimaryIcon = itemView.findViewById(R.id.primary_icon);

            mTitle = itemView.findViewById(R.id.title);
            mBody = itemView.findViewById(R.id.body);

            mAction1 = itemView.findViewById(R.id.action1);
            mAction1Divider = itemView.findViewById(R.id.action1_divider);
            mAction2 = itemView.findViewById(R.id.action2);
            mAction2Divider = itemView.findViewById(R.id.action2_divider);

            mClickInterceptor = itemView.findViewById(R.id.click_interceptor);

            // Each line groups relevant child views in an effort to help keep this view array
            // updated with actual child views in the ViewHolder.
            mWidgetViews = new View[] {
                    // Primary action.
                    mPrimaryIcon,
                    // Text.
                    mTitle, mBody,
                    // Supplemental actions
                    mAction1, mAction1Divider, mAction2, mAction2Divider,
            };
        }

        @Override
        protected void applyUxRestrictions(@NonNull CarUxRestrictions restrictions) {
            CarUxRestrictionsUtils.apply(itemView.getContext(), restrictions, getBody());
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
        public Button getAction1() {
            return mAction1;
        }

        @NonNull
        public View getAction1Divider() {
            return mAction1Divider;
        }

        @NonNull
        public Button getAction2() {
            return mAction2;
        }

        @NonNull
        public View getAction2Divider() {
            return mAction2Divider;
        }

        @NonNull
        View[] getWidgetViews() {
            return mWidgetViews;
        }

        /**
         * Returns the view that will intercept clicks beneath the supplemental icon and action
         * views.
         */
        @NonNull
        View getClickInterceptView() {
            return mClickInterceptor;
        }
    }
}
