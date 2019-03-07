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

package androidx.car.app;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.car.R;
import androidx.car.util.DropShadowScrollListener;
import androidx.car.widget.ListItem;
import androidx.car.widget.ListItemAdapter;
import androidx.car.widget.ListItemProvider;
import androidx.car.widget.PagedListView;
import androidx.car.widget.PagedScrollBarView;
import androidx.car.widget.RadioButtonListItem;
import androidx.car.widget.TextListItem;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * A subclass of {@link Dialog} that is tailored for the car environment. This dialog can display a
 * title, body text, a fixed list of single choice items and up to two buttons -- a positive and
 * negative button. Single choice items use a radio button to indicate selection.
 *
 * <p>Note that this dialog cannot be created with an empty list.
 */
public class CarSingleSelectionDialog extends Dialog {
    private static final String TAG = "CarSingleSelectionDialog";

    private final CharSequence mTitle;
    private final CharSequence mBodyText;

    private final CharSequence mPositiveButtonText;
    private final CharSequence mNegativeButtonText;

    private ListItemAdapter mAdapter;
    private int mSelectedItem;

    private TextView mTitleView;
    private TextView mBodyTextView;

    /** synthetic access **/
    @SuppressWarnings("WeakerAccess")
    PagedListView mList;
    private PagedScrollBarView mScrollBarView;

    @Nullable
    private final OnSelectionListener mOnSelectionListener;

    /** Flag for if a touch on the scrim of the dialog will dismiss it. */
    private boolean mDismissOnTouchOutside;

    private final ViewTreeObserver.OnGlobalLayoutListener mLayoutListener =
            new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    updateScrollbar();
                    // Remove this listener because the listener for the scroll state will be
                    // enough to keep the scrollbar in sync.
                    mList.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            };

    CarSingleSelectionDialog(Context context, Builder builder) {
        super(context, getDialogTheme(context));

        mTitle = builder.mTitle;
        mBodyText = builder.mSubtitle;
        mSelectedItem = builder.mSelectedItem;
        mOnSelectionListener = builder.mOnSelectionListener;
        mPositiveButtonText = builder.mPositiveButtonText;
        mNegativeButtonText = builder.mNegativeButtonText;

        initializeWithItems(builder.mItems);
    }

    @Override
    public void setTitle(CharSequence title) {
        // Ideally this method should be private; the dialog should only be modifiable through the
        // Builder. Unfortunately, this method is defined with the Dialog itself and is public.
        // So, throw an error if this method is ever called.
        throw new UnsupportedOperationException("Title should only be set from the Builder");
    }

    /**
     * @see Dialog#setCanceledOnTouchOutside(boolean)
     */
    @Override
    public void setCanceledOnTouchOutside(boolean cancel) {
        super.setCanceledOnTouchOutside(cancel);
        // Need to override this method to save the value of cancel.
        mDismissOnTouchOutside = cancel;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setContentView(R.layout.car_selction_dialog);

        // Ensure that the dialog takes up the entire window. This is needed because the scrollbar
        // needs to be drawn off the dialog.
        WindowManager.LayoutParams layoutParams = window.getAttributes();
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        window.setAttributes(layoutParams);

        // The container for this dialog takes up the entire screen. As a result, need to manually
        // listen for clicks and dismiss the dialog when necessary.
        window.findViewById(R.id.container).setOnClickListener(v -> handleTouchOutside());

        initializeTitle();
        initializeBodyText();
        initializeList();
        initializeScrollbar();
        initializeButtons();

        // Need to set this elevation listener last because the text and list need to be
        // initialized first.
        initializeTextElevationListener();
    }

    private void initializeButtons() {
        boolean isButtonPresent = false;
        Window window = getWindow();
        Button positiveButtonView = window.findViewById(R.id.positive_button);
        if (!TextUtils.isEmpty(mPositiveButtonText)) {
            isButtonPresent = true;
            positiveButtonView.setText(mPositiveButtonText);
            positiveButtonView.setOnClickListener(v -> {
                if (mOnSelectionListener != null) {
                    mOnSelectionListener.onSelection(mSelectedItem);
                }
                dismiss();
            });
        } else {
            positiveButtonView.setVisibility(View.GONE);
        }

        Button negativeButtonView = window.findViewById(R.id.negative_button);
        if (!TextUtils.isEmpty(mNegativeButtonText)) {
            isButtonPresent = true;
            negativeButtonView.setText(mNegativeButtonText);
            negativeButtonView.setOnClickListener(v -> dismiss());
        } else {
            negativeButtonView.setVisibility(View.GONE);
        }

        if (!isButtonPresent) {
            window.findViewById(R.id.button_panel).setVisibility(View.GONE);
        }
    }

    private void initializeTitle() {
        mTitleView = getWindow().findViewById(R.id.title);
        mTitleView.setText(mTitle);
        mTitleView.setVisibility(!TextUtils.isEmpty(mTitle) ? View.VISIBLE : View.GONE);
    }

    private void initializeBodyText() {
        mBodyTextView = getWindow().findViewById(R.id.bodyText);
        mBodyTextView.setText(mBodyText);
        mBodyTextView.setVisibility(!TextUtils.isEmpty(mBodyText) ? View.VISIBLE : View.GONE);
    }

    private void initializeTextElevationListener() {
        if (mTitleView.getVisibility() != View.GONE) {
            mList.setOnScrollListener(new DropShadowScrollListener(mTitleView));
        } else {
            if (mBodyTextView.getVisibility() != View.GONE) {
                mList.setOnScrollListener(new DropShadowScrollListener(mBodyTextView));
            }
        }
    }

    @Override
    protected void onStop() {
        // Cleanup to ensure that no stray view observers are still attached.
        if (mList != null) {
            mList.getViewTreeObserver().removeOnGlobalLayoutListener(mLayoutListener);
        }

        super.onStop();
    }

    private void initializeList() {
        mList = getWindow().findViewById(R.id.list);
        mList.setMaxPages(PagedListView.UNLIMITED_PAGES);
        mList.setAdapter(mAdapter);
        mList.setDividerVisibilityManager(mAdapter);

        // Ensure that when the list is scrolled, the scrollbar updates to reflect the new position.
        mList.getRecyclerView().addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                updateScrollbar();
            }
        });

        // Update if the scrollbar should be visible after the PagedListView has finished
        // laying itself out. This is needed because the only way to the state of scrollbar is to
        // see the items after they have been laid out.
        mList.getViewTreeObserver().addOnGlobalLayoutListener(mLayoutListener);
    }

    /**
     * Initializes the scrollbar that appears off the dialog. This scrollbar is not the one that
     * usually appears with the PagedListView, but mimics it in functionality.
     */
    private void initializeScrollbar() {
        mScrollBarView = getWindow().findViewById(R.id.scrollbar);

        mScrollBarView.setPaginationListener(new PagedScrollBarView.PaginationListener() {
            @Override
            public void onPaginate(int direction) {
                switch (direction) {
                    case PagedScrollBarView.PaginationListener.PAGE_UP:
                        mList.pageUp();
                        break;
                    case PagedScrollBarView.PaginationListener.PAGE_DOWN:
                        mList.pageDown();
                        break;
                    default:
                        Log.e(TAG, "Unknown pagination direction (" + direction + ")");
                }
            }

            @Override
            public void onAlphaJump() {
            }
        });
    }

    /**
     * Handles if a touch has been detected outside of the dialog. If
     * {@link #mDismissOnTouchOutside} has been set, then the dialog will be dismissed.
     */
    private void handleTouchOutside() {
        if (mDismissOnTouchOutside) {
            dismiss();
        }
    }

    /**
     * Initializes {@link #mAdapter} to display the items in the given array. It utilizes the
     * {@link TextListItem} but only populates the title field with the the values in the array.
     */
    private void initializeWithItems(List<DialogSelectionItem> items) {
        Context context = getContext();
        List<ListItem> listItems = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            listItems.add(createItem(/* text= */ items.get(i), /* position= */ i));
        }

        mAdapter = new CarSelectionAdapter(context, new ListItemProvider.ListProvider(listItems),
                mSelectedItem);
    }

    /**
     * Creates the {@link RadioButtonListItem} that represents an item in the {@code
     * CarSingleSelectionDialog}.
     *
     * @param {@link   DialogSelectionItem} to display as a {@code RadioButtonListItem}.
     * @param position The position of the item in the list.
     */
    private RadioButtonListItem createItem(DialogSelectionItem dialogSelectionItem, int position) {
        RadioButtonListItem item = new RadioButtonListItem(getContext());
        item.setTitle(dialogSelectionItem.mTitle);
        item.setBody(dialogSelectionItem.mSubtitle);

        item.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mSelectedItem = position;
            }
        });

        return item;
    }

    /**
     * Determines if scrollbar should be visible or not and shows/hides it accordingly.
     *
     * <p>If this is being called as a result of adapter changes, it should be called after the new
     * layout has been calculated because the method of determining scrollbar visibility uses the
     * current layout.
     *
     * <p>If this is called after an adapter change but before the new layout, the visibility
     * determination may not be correct.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void updateScrollbar() {
        RecyclerView recyclerView = mList.getRecyclerView();

        boolean isAtStart = mList.isAtStart();
        boolean isAtEnd = mList.isAtEnd();

        if ((isAtStart && isAtEnd)) {
            mScrollBarView.setVisibility(View.INVISIBLE);
            return;
        }

        mScrollBarView.setVisibility(View.VISIBLE);
        mScrollBarView.setUpEnabled(!isAtStart);
        mScrollBarView.setDownEnabled(!isAtEnd);

        // Assume the list scrolls vertically because we control the list and know the
        // LayoutManager cannot change.
        mScrollBarView.setParameters(
                recyclerView.computeVerticalScrollRange(),
                recyclerView.computeVerticalScrollOffset(),
                recyclerView.computeVerticalScrollExtent(),
                /* animate= */ false);

        getWindow().getDecorView().invalidate();
    }

    /**
     * Returns the style that has been assigned to {@code carDialogTheme} in the
     * current theme that is inflating this dialog. If a style has not been defined, a default
     * style will be returned.
     */
    @StyleRes
    private static int getDialogTheme(Context context) {
        TypedValue outValue = new TypedValue();
        boolean hasStyle =
                context.getTheme().resolveAttribute(R.attr.carDialogTheme, outValue, true);
        return hasStyle ? outValue.resourceId : R.style.Theme_Car_Dark_Dialog;
    }

    /** Used to listen for {@link CarSingleSelectionDialog} selection events. */
    public interface OnSelectionListener {
        /** Called when the positive button is clicked after making a selection */
        void onSelection(int position);
    }

    private static class CarSelectionAdapter extends ListItemAdapter {

        private int mLastCheckedPosition;

        CarSelectionAdapter(Context context, ListItemProvider itemProvider,
                int defaultCheckedItem) {
            super(context, itemProvider, ListItemAdapter.BackgroundStyle.SOLID);
            mLastCheckedPosition = defaultCheckedItem;
        }

        @Override
        public boolean getShowDivider(@IntRange(from = 0) int position) {
            return false;
        }

        @Override
        public void onBindViewHolder(ListItem.ViewHolder vh, int position) {
            super.onBindViewHolder(vh, position);

            RadioButtonListItem.ViewHolder viewHolder = (RadioButtonListItem.ViewHolder) vh;

            viewHolder.getRadioButton().setChecked(mLastCheckedPosition == position);
            viewHolder.getRadioButton().setOnCheckedChangeListener((buttonView, isChecked) -> {
                mLastCheckedPosition = position;
                // Refresh other radio button list items.
                notifyDataSetChanged();
            });
        }
    }

    /**
     * A struct that holds data for a selection item. A selection item is a combination of the
     * item title and optional subtitle.
     */
    public static class DialogSelectionItem {

        final CharSequence mTitle;
        final CharSequence mSubtitle;

        /**
         * Creates a DialogSelectionItem.
         *
         * @param title    The title of the item. .
         * @param subtitle A list of items associated with this section. This list cannot be
         *                 {@code null} or empty.
         */
        public DialogSelectionItem(CharSequence title, CharSequence subtitle) {
            if (TextUtils.isEmpty(title)) {
                throw new IllegalArgumentException("Title cannot be empty.");
            }

            mTitle = title;
            mSubtitle = subtitle;
        }
    }

    /**
     * Builder class that can be used to create a {@link CarSingleSelectionDialog} by configuring
     * the options for the list and behavior of the dialog.
     */
    public static final class Builder {
        private final Context mContext;

        CharSequence mTitle;
        CharSequence mSubtitle;
        List<DialogSelectionItem> mItems;
        int mSelectedItem;
        OnSelectionListener mOnSelectionListener;

        CharSequence mPositiveButtonText;
        CharSequence mNegativeButtonText;

        private boolean mCancelable = true;
        private OnCancelListener mOnCancelListener;
        private OnDismissListener mOnDismissListener;

        /**
         * Creates a new instance of the {@code Builder}.
         *
         * @param context The {@code Context} that the dialog is to be created in.
         */
        public Builder(Context context) {
            mContext = context;
        }

        /**
         * Sets the title of the dialog to be the given string resource.
         *
         * @param titleId The resource id of the string to be used as the title.
         *                Text style will be retained.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
        public Builder setTitle(@StringRes int titleId) {
            mTitle = mContext.getText(titleId);
            return this;
        }

        /**
         * Sets the title of the dialog for be the given string.
         *
         * @param title The string to be used as the title.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
        public Builder setTitle(CharSequence title) {
            mTitle = title;
            return this;
        }

        /**
         * Sets the body text of the dialog to be the given string resource.
         *
         * @param bodyTextId The resource id of the string to be used as the subtitle.
         *                   Text style will be retained.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
        public Builder setBodyText(@StringRes int bodyTextId) {
            mSubtitle = mContext.getText(bodyTextId);
            return this;
        }

        /**
         * Sets the bodyText of the dialog for be the given string.
         *
         * @param bodyText The string to be used as the title.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
        public Builder setBodyText(CharSequence bodyText) {
            mSubtitle = bodyText;
            return this;
        }

        /**
         * Sets the items that should appear in the list.
         *
         * <p>If a {@link DialogInterface.OnClickListener} is given, then it will be notified
         * of the click.The {@code which} parameter of the
         * {@link DialogInterface.OnClickListener#onClick(DialogInterface, int)} method will be
         * the position of the item. This position maps to the index of the item in the given list.
         *
         * <p>The provided list of items cannot be {@code null} or empty. Passing an empty list
         * to this method will throw can exception.
         * *
         *
         * @param items               The items that will appear in the list.
         * @param selectedItem        Specifies which item is checked. If -1 no
         *                            items are checked.
         * @param onSelectionListener The listener that will be notified of a selection.
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
        public Builder setItems(@NonNull List<DialogSelectionItem> items, int selectedItem,
                @Nullable OnSelectionListener onSelectionListener) {
            if (items.size() == 0) {
                throw new IllegalArgumentException("Provided list of items cannot be empty.");
            }

            mItems = items;
            mSelectedItem = selectedItem;
            mOnSelectionListener = onSelectionListener;
            return this;
        }


        /**
         * Set a listener to be invoked when the positive button of the dialog is pressed.
         *
         * @param textId The resource id of the text to display in the positive button
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setPositiveButton(@StringRes int textId) {
            mPositiveButtonText = mContext.getText(textId);
            return this;
        }

        /**
         * Set a listener to be invoked when the positive button of the dialog is pressed.
         *
         * @param text The text to display in the positive button
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setPositiveButton(CharSequence text) {
            mPositiveButtonText = text;
            return this;
        }

        /**
         * Set a listener to be invoked when the negative button of the dialog is pressed.
         *
         * @param textId The resource id of the text to display in the negative button
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setNegativeButton(@StringRes int textId) {
            mNegativeButtonText = mContext.getText(textId);
            return this;
        }

        /**
         * Set a listener to be invoked when the negative button of the dialog is pressed.
         *
         * @param text The text to display in the negative button
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setNegativeButton(CharSequence text) {
            mNegativeButtonText = text;
            return this;
        }

        /**
         * Sets whether the dialog is cancelable or not. Default is {@code true}.
         *
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
        public Builder setCancelable(boolean cancelable) {
            mCancelable = cancelable;
            return this;
        }

        /**
         * Sets the callback that will be called if the dialog is canceled.
         *
         * <p>Even in a cancelable dialog, the dialog may be dismissed for reasons other than
         * being canceled or one of the supplied choices being selected.
         * If you are interested in listening for all cases where the dialog is dismissed
         * and not just when it is canceled, see {@link #setOnDismissListener(OnDismissListener)}.
         *
         * @param onCancelListener The listener to be invoked when this dialog is canceled.
         * @return This {@code Builder} object to allow for chaining of calls.
         * @see #setCancelable(boolean)
         * @see #setOnDismissListener(OnDismissListener)
         */
        @NonNull
        public Builder setOnCancelListener(OnCancelListener onCancelListener) {
            mOnCancelListener = onCancelListener;
            return this;
        }

        /**
         * Sets the callback that will be called when the dialog is dismissed for any reason.
         *
         * @return This {@code Builder} object to allow for chaining of calls.
         */
        @NonNull
        public Builder setOnDismissListener(OnDismissListener onDismissListener) {
            mOnDismissListener = onDismissListener;
            return this;
        }

        /**
         * Creates an {@link CarSingleSelectionDialog} with the arguments supplied to this {@code
         * Builder}.
         *
         * <p>If {@link #setItems(List, int, OnSelectionListener)} is never called,
         * then calling this method will throw an exception.
         *
         * <p>Calling this method does not display the dialog. Utilize this dialog within a
         * {@link androidx.fragment.app.DialogFragment} to show the dialog.
         */
        public CarSingleSelectionDialog create() {
            // Check that the dialog was created with a list of items.
            if (mItems == null || mItems.size() == 0) {
                throw new IllegalStateException(
                        "CarSingleSelectionDialog cannot be created with a non-empty list.");
            }

            CarSingleSelectionDialog dialog = new CarSingleSelectionDialog(mContext, /* builder= */
                    this);

            dialog.setCancelable(mCancelable);
            dialog.setCanceledOnTouchOutside(mCancelable);
            dialog.setOnCancelListener(mOnCancelListener);
            dialog.setOnDismissListener(mOnDismissListener);

            return dialog;
        }
    }
}
