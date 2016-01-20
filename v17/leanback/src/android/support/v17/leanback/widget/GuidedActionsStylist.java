/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build.VERSION;
import android.support.annotation.NonNull;
import android.support.v17.leanback.R;
import android.support.v17.leanback.transition.TransitionHelper;
import android.support.v17.leanback.transition.TransitionListener;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Checkable;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

/**
 * GuidedActionsStylist is used within a {@link android.support.v17.leanback.app.GuidedStepFragment}
 * to supply the right-side panel where users can take actions. It consists of a container for the
 * list of actions, and a stationary selector view that indicates visually the location of focus.
 * GuidedActionsStylist has two different layouts: default is for normal actions including text,
 * radio, checkbox etc, the other when {@link #setAsButtonActions()} is called is recommended for
 * button actions such as "yes", "no".
 * <p>
 * Many aspects of the base GuidedActionsStylist can be customized through theming; see the
 * theme attributes below. Note that these attributes are not set on individual elements in layout
 * XML, but instead would be set in a custom theme. See
 * <a href="http://developer.android.com/guide/topics/ui/themes.html">Styles and Themes</a>
 * for more information.
 * <p>
 * If these hooks are insufficient, this class may also be subclassed. Subclasses may wish to
 * override the {@link #onProvideLayoutId} method to change the layout used to display the
 * list container and selector, or the {@link #onProvideItemLayoutId} method to change the layout
 * used to display each action.
 * <p>
 * Note: If an alternate list layout is provided, the following view IDs must be supplied:
 * <ul>
 * <li>{@link android.support.v17.leanback.R.id#guidedactions_list}</li>
 * </ul><p>
 * These view IDs must be present in order for the stylist to function. The list ID must correspond
 * to a {@link VerticalGridView} or subclass.
 * <p>
 * If an alternate item layout is provided, the following view IDs should be used to refer to base
 * elements:
 * <ul>
 * <li>{@link android.support.v17.leanback.R.id#guidedactions_item_content}</li>
 * <li>{@link android.support.v17.leanback.R.id#guidedactions_item_title}</li>
 * <li>{@link android.support.v17.leanback.R.id#guidedactions_item_description}</li>
 * <li>{@link android.support.v17.leanback.R.id#guidedactions_item_icon}</li>
 * <li>{@link android.support.v17.leanback.R.id#guidedactions_item_checkmark}</li>
 * <li>{@link android.support.v17.leanback.R.id#guidedactions_item_chevron}</li>
 * </ul><p>
 * These view IDs are allowed to be missing, in which case the corresponding views in {@link
 * GuidedActionsStylist.ViewHolder} will be null.
 * <p>
 * In order to support editable actions, the view associated with guidedactions_item_title should
 * be a subclass of {@link android.widget.EditText}, and should satisfy the {@link
 * ImeKeyMonitor} interface.
 *
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedStepImeAppearingAnimation
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedStepImeDisappearingAnimation
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionsSelectorDrawable
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionsListStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedSubActionsListStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedButtonActionsListStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionItemContainerStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionItemCheckmarkStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionItemIconStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionItemContentStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionItemTitleStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionItemDescriptionStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionItemChevronStyle
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionPressedAnimation
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionUnpressedAnimation
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionEnabledChevronAlpha
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionDisabledChevronAlpha
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionTitleMinLines
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionTitleMaxLines
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionDescriptionMinLines
 * @attr ref android.support.v17.leanback.R.styleable#LeanbackGuidedStepTheme_guidedActionVerticalPadding
 * @see android.R.styleable#Theme_listChoiceIndicatorSingle
 * @see android.R.styleable#Theme_listChoiceIndicatorMultiple
 * @see android.support.v17.leanback.app.GuidedStepFragment
 * @see GuidedAction
 */
public class GuidedActionsStylist implements FragmentAnimationProvider {

    /**
     * Default viewType that associated with default layout Id for the action item.
     * @see #getItemViewType(GuidedAction)
     * @see #onProvideItemLayoutId(int)
     * @see #onCreateViewHolder(ViewGroup, int)
     */
    public static final int VIEW_TYPE_DEFAULT = 0;

    /**
     * ViewHolder caches information about the action item layouts' subviews. Subclasses of {@link
     * GuidedActionsStylist} may also wish to subclass this in order to add fields.
     * @see GuidedAction
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {

        private GuidedAction mAction;
        private View mContentView;
        private TextView mTitleView;
        private TextView mDescriptionView;
        private ImageView mIconView;
        private ImageView mCheckmarkView;
        private ImageView mChevronView;
        private boolean mInEditing;
        private boolean mInEditingDescription;
        private final boolean mIsSubAction;

        /**
         * Constructs an ViewHolder and caches the relevant subviews.
         */
        public ViewHolder(View v) {
            this(v, false);
        }

        /**
         * Constructs an ViewHolder for sub action and caches the relevant subviews.
         */
        public ViewHolder(View v, boolean isSubAction) {
            super(v);

            mContentView = v.findViewById(R.id.guidedactions_item_content);
            mTitleView = (TextView) v.findViewById(R.id.guidedactions_item_title);
            mDescriptionView = (TextView) v.findViewById(R.id.guidedactions_item_description);
            mIconView = (ImageView) v.findViewById(R.id.guidedactions_item_icon);
            mCheckmarkView = (ImageView) v.findViewById(R.id.guidedactions_item_checkmark);
            mChevronView = (ImageView) v.findViewById(R.id.guidedactions_item_chevron);
            mIsSubAction = isSubAction;
        }

        /**
         * Returns the content view within this view holder's view, where title and description are
         * shown.
         */
        public View getContentView() {
            return mContentView;
        }

        /**
         * Returns the title view within this view holder's view.
         */
        public TextView getTitleView() {
            return mTitleView;
        }

        /**
         * Convenience method to return an editable version of the title, if possible,
         * or null if the title view isn't an EditText.
         */
        public EditText getEditableTitleView() {
            return (mTitleView instanceof EditText) ? (EditText)mTitleView : null;
        }

        /**
         * Returns the description view within this view holder's view.
         */
        public TextView getDescriptionView() {
            return mDescriptionView;
        }

        /**
         * Convenience method to return an editable version of the description, if possible,
         * or null if the description view isn't an EditText.
         */
        public EditText getEditableDescriptionView() {
            return (mDescriptionView instanceof EditText) ? (EditText)mDescriptionView : null;
        }

        /**
         * Returns the icon view within this view holder's view.
         */
        public ImageView getIconView() {
            return mIconView;
        }

        /**
         * Returns the checkmark view within this view holder's view.
         */
        public ImageView getCheckmarkView() {
            return mCheckmarkView;
        }

        /**
         * Returns the chevron view within this view holder's view.
         */
        public ImageView getChevronView() {
            return mChevronView;
        }

        /**
         * Returns true if the TextView is in editing title or description, false otherwise.
         */
        public boolean isInEditing() {
            return mInEditing;
        }

        /**
         * Returns true if the TextView is in editing description, false otherwise.
         */
        public boolean isInEditingDescription() {
            return mInEditingDescription;
        }

        /**
         * @return Current editing title view or description view or null if not in editing.
         */
        public View getEditingView() {
            if (mInEditing) {
                return mInEditingDescription ?  mDescriptionView : mTitleView;
            } else {
                return null;
            }
        }

        /**
         * @return True if bound action is inside {@link GuidedAction#getSubActions()}, false
         * otherwise.
         */
        public boolean isSubAction() {
            return mIsSubAction;
        }

        /**
         * @return Currently bound action.
         */
        public GuidedAction getAction() {
            return mAction;
        }
    }

    private static String TAG = "GuidedActionsStylist";

    private ViewGroup mMainView;
    private VerticalGridView mActionsGridView;
    private VerticalGridView mSubActionsGridView;
    private View mBgView;
    private View mContentView;
    private boolean mButtonActions;

    // Cached values from resources
    private float mEnabledTextAlpha;
    private float mDisabledTextAlpha;
    private float mEnabledDescriptionAlpha;
    private float mDisabledDescriptionAlpha;
    private float mEnabledChevronAlpha;
    private float mDisabledChevronAlpha;
    private int mTitleMinLines;
    private int mTitleMaxLines;
    private int mDescriptionMinLines;
    private int mVerticalPadding;
    private int mDisplayHeight;

    private GuidedAction mExpandedAction = null;
    private Object mExpandTransition;

    /**
     * Creates a view appropriate for displaying a list of GuidedActions, using the provided
     * inflater and container.
     * <p>
     * <i>Note: Does not actually add the created view to the container; the caller should do
     * this.</i>
     * @param inflater The layout inflater to be used when constructing the view.
     * @param container The view group to be passed in the call to
     * <code>LayoutInflater.inflate</code>.
     * @return The view to be added to the caller's view hierarchy.
     */
    public View onCreateView(LayoutInflater inflater, ViewGroup container) {
        mMainView = (ViewGroup) inflater.inflate(onProvideLayoutId(), container, false);
        mContentView = mMainView.findViewById(mButtonActions ? R.id.guidedactions_content2 :
                R.id.guidedactions_content);
        mBgView = mMainView.findViewById(mButtonActions ? R.id.guidedactions_list_background2 :
                R.id.guidedactions_list_background);
        if (mMainView instanceof VerticalGridView) {
            mActionsGridView = (VerticalGridView) mMainView;
        } else {
            mActionsGridView = (VerticalGridView) mMainView.findViewById(mButtonActions ?
                    R.id.guidedactions_list2 : R.id.guidedactions_list);
            if (mActionsGridView == null) {
                throw new IllegalStateException("No ListView exists.");
            }
            mActionsGridView.setWindowAlignmentOffset(0);
            mActionsGridView.setWindowAlignmentOffsetPercent(50f);
            mActionsGridView.setWindowAlignment(VerticalGridView.WINDOW_ALIGN_NO_EDGE);
            if (!mButtonActions) {
                mSubActionsGridView = (VerticalGridView) mMainView.findViewById(
                        R.id.guidedactions_sub_list);
            }
        }

        // Cache widths, chevron alpha values, max and min text lines, etc
        Context ctx = mMainView.getContext();
        TypedValue val = new TypedValue();
        mEnabledChevronAlpha = getFloat(ctx, val, R.attr.guidedActionEnabledChevronAlpha);
        mDisabledChevronAlpha = getFloat(ctx, val, R.attr.guidedActionDisabledChevronAlpha);
        mTitleMinLines = getInteger(ctx, val, R.attr.guidedActionTitleMinLines);
        mTitleMaxLines = getInteger(ctx, val, R.attr.guidedActionTitleMaxLines);
        mDescriptionMinLines = getInteger(ctx, val, R.attr.guidedActionDescriptionMinLines);
        mVerticalPadding = getDimension(ctx, val, R.attr.guidedActionVerticalPadding);
        mDisplayHeight = ((WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getHeight();

        mEnabledTextAlpha = Float.valueOf(ctx.getResources().getString(R.string
                .lb_guidedactions_item_unselected_text_alpha));
        mDisabledTextAlpha = Float.valueOf(ctx.getResources().getString(R.string
                .lb_guidedactions_item_disabled_text_alpha));
        mEnabledDescriptionAlpha = Float.valueOf(ctx.getResources().getString(R.string
                .lb_guidedactions_item_unselected_description_text_alpha));
        mDisabledDescriptionAlpha = Float.valueOf(ctx.getResources().getString(R.string
                .lb_guidedactions_item_disabled_description_text_alpha));
        return mMainView;
    }

    /**
     * Choose the layout resource for button actions in {@link #onProvideLayoutId()}.
     */
    public void setAsButtonActions() {
        if (mMainView != null) {
            throw new IllegalStateException("setAsButtonActions() must be called before creating "
                    + "views");
        }
        mButtonActions = true;
    }

    /**
     * Returns true if it is button actions list, false for normal actions list.
     * @return True if it is button actions list, false for normal actions list.
     */
    public boolean isButtonActions() {
        return mButtonActions;
    }

    /**
     * Called when destroy the View created by GuidedActionsStylist.
     */
    public void onDestroyView() {
        mExpandedAction = null;
        mExpandTransition = null;
        mActionsGridView = null;
        mSubActionsGridView = null;
        mContentView = null;
        mBgView = null;
        mMainView = null;
    }

    /**
     * Returns the VerticalGridView that displays the list of GuidedActions.
     * @return The VerticalGridView for this presenter.
     */
    public VerticalGridView getActionsGridView() {
        return mActionsGridView;
    }

    /**
     * Returns the VerticalGridView that displays the sub actions list of an expanded action.
     * @return The VerticalGridView that displays the sub actions list of an expanded action.
     */
    public VerticalGridView getSubActionsGridView() {
        return mSubActionsGridView;
    }

    /**
     * Provides the resource ID of the layout defining the host view for the list of guided actions.
     * Subclasses may override to provide their own customized layouts. The base implementation
     * returns {@link android.support.v17.leanback.R.layout#lb_guidedactions} or
     * {@link android.support.v17.leanback.R.layout#lb_guidedbuttonactions} if
     * {@link #isButtonActions()} is true. If overridden, the substituted layout should contain
     * matching IDs for any views that should be managed by the base class; this can be achieved by
     * starting with a copy of the base layout file.
     *
     * @return The resource ID of the layout to be inflated to define the host view for the list of
     *         GuidedActions.
     */
    public int onProvideLayoutId() {
        return mButtonActions ? R.layout.lb_guidedbuttonactions : R.layout.lb_guidedactions;
    }

    /**
     * Return view type of action, each different type can have differently associated layout Id.
     * Default implementation returns {@link #VIEW_TYPE_DEFAULT}.
     * @param action  The action object.
     * @return View type that used in {@link #onProvideItemLayoutId(int)}.
     */
    public int getItemViewType(GuidedAction action) {
        return VIEW_TYPE_DEFAULT;
    }

    /**
     * Provides the resource ID of the layout defining the view for an individual guided actions.
     * Subclasses may override to provide their own customized layouts. The base implementation
     * returns {@link android.support.v17.leanback.R.layout#lb_guidedactions_item}. If overridden,
     * the substituted layout should contain matching IDs for any views that should be managed by
     * the base class; this can be achieved by starting with a copy of the base layout file. Note
     * that in order for the item to support editing, the title view should both subclass {@link
     * android.widget.EditText} and implement {@link ImeKeyMonitor}; see {@link
     * GuidedActionEditText}.  To support different types of Layouts, override {@link
     * #onProvideItemLayoutId(int)}.
     * @return The resource ID of the layout to be inflated to define the view to display an
     * individual GuidedAction.
     */
    public int onProvideItemLayoutId() {
        return R.layout.lb_guidedactions_item;
    }

    /**
     * Provides the resource ID of the layout defining the view for an individual guided actions.
     * Subclasses may override to provide their own customized layouts. The base implementation
     * returns {@link android.support.v17.leanback.R.layout#lb_guidedactions_item}. If overridden,
     * the substituted layout should contain matching IDs for any views that should be managed by
     * the base class; this can be achieved by starting with a copy of the base layout file. Note
     * that in order for the item to support editing, the title view should both subclass {@link
     * android.widget.EditText} and implement {@link ImeKeyMonitor}; see {@link
     * GuidedActionEditText}.
     * @param viewType View type returned by {@link #getItemViewType(GuidedAction)}
     * @return The resource ID of the layout to be inflated to define the view to display an
     * individual GuidedAction.
     */
    public int onProvideItemLayoutId(int viewType) {
        if (viewType == VIEW_TYPE_DEFAULT) {
            return onProvideItemLayoutId();
        } else {
            throw new RuntimeException("ViewType " + viewType +
                    " not supported in GuidedActionsStylist");
        }
    }

    /**
     * Constructs a {@link ViewHolder} capable of representing {@link GuidedAction}s. Subclasses
     * may choose to return a subclass of ViewHolder.  To support different view types, override
     * {@link #onCreateViewHolder(ViewGroup, int)}
     * <p>
     * <i>Note: Should not actually add the created view to the parent; the caller will do
     * this.</i>
     * @param parent The view group to be used as the parent of the new view.
     * @return The view to be added to the caller's view hierarchy.
     */
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(onProvideItemLayoutId(), parent, false);
        return new ViewHolder(v, parent == mSubActionsGridView);
    }

    /**
     * Constructs a {@link ViewHolder} capable of representing {@link GuidedAction}s. Subclasses
     * may choose to return a subclass of ViewHolder.
     * <p>
     * <i>Note: Should not actually add the created view to the parent; the caller will do
     * this.</i>
     * @param parent The view group to be used as the parent of the new view.
     * @param viewType The viewType returned by {@link #getItemViewType(GuidedAction)}
     * @return The view to be added to the caller's view hierarchy.
     */
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_DEFAULT) {
            return onCreateViewHolder(parent);
        }
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(onProvideItemLayoutId(viewType), parent, false);
        return new ViewHolder(v, parent == mSubActionsGridView);
    }

    /**
     * Binds a {@link ViewHolder} to a particular {@link GuidedAction}.
     * @param vh The view holder to be associated with the given action.
     * @param action The guided action to be displayed by the view holder's view.
     * @return The view to be added to the caller's view hierarchy.
     */
    public void onBindViewHolder(ViewHolder vh, GuidedAction action) {

        vh.mAction = action;
        if (vh.mTitleView != null) {
            vh.mTitleView.setText(action.getTitle());
            vh.mTitleView.setAlpha(action.isEnabled() ? mEnabledTextAlpha : mDisabledTextAlpha);
            vh.mTitleView.setFocusable(false);
        }
        if (vh.mDescriptionView != null) {
            vh.mDescriptionView.setText(action.getDescription());
            vh.mDescriptionView.setVisibility(TextUtils.isEmpty(action.getDescription()) ?
                    View.GONE : View.VISIBLE);
            vh.mDescriptionView.setAlpha(action.isEnabled() ? mEnabledDescriptionAlpha :
                mDisabledDescriptionAlpha);
            vh.mDescriptionView.setFocusable(false);
        }
        // Clients might want the check mark view to be gone entirely, in which case, ignore it.
        if (vh.mCheckmarkView != null) {
            onBindCheckMarkView(vh, action);
        }
        setIcon(vh.mIconView, action);

        if (action.hasMultilineDescription()) {
            if (vh.mTitleView != null) {
                vh.mTitleView.setMaxLines(mTitleMaxLines);
                if (vh.mDescriptionView != null) {
                    vh.mDescriptionView.setMaxHeight(getDescriptionMaxHeight(
                            vh.itemView.getContext(), vh.mTitleView));
                }
            }
        } else {
            if (vh.mTitleView != null) {
                vh.mTitleView.setMaxLines(mTitleMinLines);
            }
            if (vh.mDescriptionView != null) {
                vh.mDescriptionView.setMaxLines(mDescriptionMinLines);
            }
        }
        setEditingMode(vh, action, false);
        if (action.isFocusable()) {
            vh.itemView.setFocusable(true);
            ((ViewGroup) vh.itemView).setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        } else {
            vh.itemView.setFocusable(false);
            ((ViewGroup) vh.itemView).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        }
        setupImeOptions(vh, action);

        updateChevronAndVisibility(vh);
    }

    /**
     * Called by {@link #onBindViewHolder(ViewHolder, GuidedAction)} to setup IME options.  Default
     * implementation assigns {@link EditorInfo#IME_ACTION_DONE}.  Subclass may override.
     * @param vh The view holder to be associated with the given action.
     * @param action The guided action to be displayed by the view holder's view.
     */
    protected void setupImeOptions(ViewHolder vh, GuidedAction action) {
        setupNextImeOptions(vh.getEditableTitleView());
        setupNextImeOptions(vh.getEditableDescriptionView());
    }

    private void setupNextImeOptions(EditText edit) {
        if (edit != null) {
            edit.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        }
    }

    public void setEditingMode(ViewHolder vh, GuidedAction action, boolean editing) {
        if (editing != vh.mInEditing) {
            vh.mInEditing = editing;
            onEditingModeChange(vh, action, editing);
        }
    }

    protected void onEditingModeChange(ViewHolder vh, GuidedAction action, boolean editing) {
        action = vh.getAction();
        TextView titleView = vh.getTitleView();
        TextView descriptionView = vh.getDescriptionView();
        if (editing) {
            CharSequence editTitle = action.getEditTitle();
            if (titleView != null && editTitle != null) {
                titleView.setText(editTitle);
            }
            CharSequence editDescription = action.getEditDescription();
            if (descriptionView != null && editDescription != null) {
                descriptionView.setText(editDescription);
            }
            if (action.isDescriptionEditable()) {
                if (descriptionView != null) {
                    descriptionView.setVisibility(View.VISIBLE);
                    descriptionView.setInputType(action.getDescriptionEditInputType());
                }
                vh.mInEditingDescription = true;
            } else {
                vh.mInEditingDescription = false;
                if (titleView != null) {
                    titleView.setInputType(action.getEditInputType());
                }
            }
        } else {
            if (titleView != null) {
                titleView.setText(action.getTitle());
            }
            if (descriptionView != null) {
                descriptionView.setText(action.getDescription());
            }
            if (vh.mInEditingDescription) {
                if (descriptionView != null) {
                    descriptionView.setVisibility(TextUtils.isEmpty(action.getDescription()) ?
                            View.GONE : View.VISIBLE);
                    descriptionView.setInputType(action.getDescriptionInputType());
                }
                vh.mInEditingDescription = false;
            } else {
                if (titleView != null) {
                    titleView.setInputType(action.getInputType());
                }
            }
        }
    }

    /**
     * Animates the view holder's view (or subviews thereof) when the action has had its focus
     * state changed.
     * @param vh The view holder associated with the relevant action.
     * @param focused True if the action has become focused, false if it has lost focus.
     */
    public void onAnimateItemFocused(ViewHolder vh, boolean focused) {
        // No animations for this, currently, because the animation is done on
        // mSelectorView
    }

    /**
     * Animates the view holder's view (or subviews thereof) when the action has had its press
     * state changed.
     * @param vh The view holder associated with the relevant action.
     * @param pressed True if the action has been pressed, false if it has been unpressed.
     */
    public void onAnimateItemPressed(ViewHolder vh, boolean pressed) {
        int attr = pressed ? R.attr.guidedActionPressedAnimation :
                R.attr.guidedActionUnpressedAnimation;
        createAnimator(vh.itemView, attr).start();
    }

    /**
     * Resets the view holder's view to unpressed state.
     * @param vh The view holder associated with the relevant action.
     */
    public void onAnimateItemPressedCancelled(ViewHolder vh) {
        createAnimator(vh.itemView, R.attr.guidedActionUnpressedAnimation).end();
    }

    /**
     * Animates the view holder's view (or subviews thereof) when the action has had its check state
     * changed. Default implementation calls setChecked() if {@link ViewHolder#getCheckmarkView()}
     * is instance of {@link Checkable}.
     *
     * @param vh The view holder associated with the relevant action.
     * @param checked True if the action has become checked, false if it has become unchecked.
     * @see #onBindCheckMarkView(ViewHolder, GuidedAction)
     */
    public void onAnimateItemChecked(ViewHolder vh, boolean checked) {
        if (vh.mCheckmarkView instanceof Checkable) {
            ((Checkable) vh.mCheckmarkView).setChecked(checked);
        }
    }

    /**
     * Sets states of check mark view, called by {@link #onBindViewHolder(ViewHolder, GuidedAction)}
     * when action's checkset Id is other than {@link GuidedAction#NO_CHECK_SET}. Default
     * implementation assigns drawable loaded from theme attribute
     * {@link android.R.attr#listChoiceIndicatorMultiple} for checkbox or
     * {@link android.R.attr#listChoiceIndicatorSingle} for radio button. Subclass rarely needs
     * override the method, instead app can provide its own drawable that supports transition
     * animations, change theme attributes {@link android.R.attr#listChoiceIndicatorMultiple} and
     * {@link android.R.attr#listChoiceIndicatorSingle} in {android.support.v17.leanback.R.
     * styleable#LeanbackGuidedStepTheme}.
     *
     * @param vh The view holder associated with the relevant action.
     * @param action The GuidedAction object to bind to.
     * @see #onAnimateItemChecked(ViewHolder, boolean)
     */
    public void onBindCheckMarkView(ViewHolder vh, GuidedAction action) {
        if (action.getCheckSetId() != GuidedAction.NO_CHECK_SET) {
            vh.mCheckmarkView.setVisibility(View.VISIBLE);
            int attrId = action.getCheckSetId() == GuidedAction.CHECKBOX_CHECK_SET_ID ?
                    android.R.attr.listChoiceIndicatorMultiple :
                    android.R.attr.listChoiceIndicatorSingle;
            final Context context = vh.mCheckmarkView.getContext();
            Drawable drawable = null;
            TypedValue typedValue = new TypedValue();
            if (context.getTheme().resolveAttribute(attrId, typedValue, true)) {
                drawable = ContextCompat.getDrawable(context, typedValue.resourceId);
            }
            vh.mCheckmarkView.setImageDrawable(drawable);
            if (vh.mCheckmarkView instanceof Checkable) {
                ((Checkable) vh.mCheckmarkView).setChecked(action.isChecked());
            }
        } else {
            vh.mCheckmarkView.setVisibility(View.GONE);
        }
    }

    /**
     * Sets states of chevron view, called by {@link #onBindViewHolder(ViewHolder, GuidedAction)}.
     * Subclass may override.
     *
     * @param vh The view holder associated with the relevant action.
     * @param action The GuidedAction object to bind to.
     */
    public void onBindChevronView(ViewHolder vh, GuidedAction action) {
        final boolean hasNext = action.hasNext();
        final boolean hasSubActions = action.hasSubActions();
        if (hasNext || hasSubActions) {
            vh.mChevronView.setVisibility(View.VISIBLE);
            vh.mChevronView.setAlpha(action.isEnabled() ? mEnabledChevronAlpha :
                    mDisabledChevronAlpha);
            if (hasNext) {
                float r = mMainView != null
                        && mMainView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL ? 180f : 0f;
                vh.mChevronView.setRotation(r);
            } else if (action == mExpandedAction) {
                vh.mChevronView.setRotation(270);
            } else {
                vh.mChevronView.setRotation(90);
            }
        } else {
            vh.mChevronView.setVisibility(View.GONE);

        }
    }

    /**
     * Expands or collapse the sub actions list view.
     * @param avh When not null, fill sub actions list of this ViewHolder into sub actions list and
     * hide the other items in main list.  When null, collapse the sub actions list.
     */
    public void setExpandedViewHolder(ViewHolder avh) {
        if (mSubActionsGridView == null || isInExpandTransition()) {
            return;
        }
        if (isExpandTransitionSupported()) {
            startExpandedTransition(avh);
        } else {
            onUpdateExpandedViewHolder(avh);
        }
    }

    /**
     * Returns true if it is running an expanding or collapsing transition, false otherwise.
     * @return True if it is running an expanding or collapsing transition, false otherwise.
     */
    public boolean isInExpandTransition() {
        return mExpandTransition != null;
    }

    /**
     * Returns if expand/collapse animation is supported.  When this method returns true,
     * {@link #startExpandedTransition(ViewHolder)} will be used.  When this method returns false,
     * {@link #onUpdateExpandedViewHolder(ViewHolder)} will be called.
     * @return True if it is running an expanding or collapsing transition, false otherwise.
     */
    public boolean isExpandTransitionSupported() {
        return VERSION.SDK_INT >= 21;
    }

    /**
     * Start transition to expand or collapse GuidedActionStylist.
     * @param avh When not null, the GuidedActionStylist expands the sub actions of avh.  When null
     * the GuidedActionStylist will collapse sub actions.
     */
    public void startExpandedTransition(ViewHolder avh) {
        ViewHolder focusAvh = null; // expand / collapse view holder
        final int count = mActionsGridView.getChildCount();
        for (int i = 0; i < count; i++) {
            ViewHolder vh = (ViewHolder) mActionsGridView
                    .getChildViewHolder(mActionsGridView.getChildAt(i));
            if (avh == null && vh.itemView.getVisibility() == View.VISIBLE) {
                // going to collapse this one.
                focusAvh = vh;
                break;
            } else if (avh != null && vh.getAction() == avh.getAction()) {
                // going to expand this one.
                focusAvh = vh;
                break;
            }
        }
        if (focusAvh == null) {
            // huh?
            onUpdateExpandedViewHolder(avh);
            return;
        }
        Object set = TransitionHelper.createTransitionSet(false);
        Object slideAndFade = TransitionHelper.createFadeAndShortSlide(Gravity.TOP | Gravity.BOTTOM,
                (float) focusAvh.itemView.getHeight());
        Object changeFocusItemTransform = TransitionHelper.createChangeTransform();
        Object changeFocusItemBounds = TransitionHelper.createChangeBounds(false);
        Object fadeGrid = TransitionHelper.createFadeTransition(TransitionHelper.FADE_IN |
                TransitionHelper.FADE_OUT);
        Object changeGridBounds = TransitionHelper.createChangeBounds(false);
        if (avh == null) {
            TransitionHelper.setStartDelay(slideAndFade, 150);
            TransitionHelper.setStartDelay(changeFocusItemTransform, 100);
            TransitionHelper.setStartDelay(changeFocusItemBounds, 100);
        } else {
            TransitionHelper.setStartDelay(fadeGrid, 100);
            TransitionHelper.setStartDelay(changeGridBounds, 100);
            TransitionHelper.setStartDelay(changeFocusItemTransform, 50);
            TransitionHelper.setStartDelay(changeFocusItemBounds, 50);
        }
        for (int i = 0; i < count; i++) {
            ViewHolder vh = (ViewHolder) mActionsGridView
                    .getChildViewHolder(mActionsGridView.getChildAt(i));
            if (vh == focusAvh) {
                // going to expand/collapse this one.
                TransitionHelper.include(changeFocusItemTransform, vh.itemView);
                TransitionHelper.include(changeFocusItemBounds, vh.itemView);
            } else {
                // going to slide this item to top / bottom.
                TransitionHelper.include(slideAndFade, vh.itemView);
            }
        }
        TransitionHelper.include(fadeGrid, mSubActionsGridView);
        TransitionHelper.include(changeGridBounds, mSubActionsGridView);
        TransitionHelper.addTransition(set, slideAndFade);
        TransitionHelper.addTransition(set, changeFocusItemTransform);
        TransitionHelper.addTransition(set, changeFocusItemBounds);
        TransitionHelper.addTransition(set, fadeGrid);
        TransitionHelper.addTransition(set, changeGridBounds);
        mExpandTransition = set;
        TransitionHelper.addTransitionListener(mExpandTransition, new TransitionListener() {
            @Override
            public void onTransitionEnd(Object transition) {
                mExpandTransition = null;
            }
        });
        if (avh != null && mSubActionsGridView.getTop() != avh.itemView.getTop()) {
            // For expanding, set the initial position of subActionsGridView before running
            // a ChangeBounds on it.
            final ViewHolder toUpdate = avh;
            mSubActionsGridView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                        int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    if (mSubActionsGridView == null) {
                        return;
                    }
                    mSubActionsGridView.removeOnLayoutChangeListener(this);
                    mMainView.post(new Runnable() {
                        public void run() {
                            if (mMainView == null) {
                                return;
                            }
                            TransitionHelper.beginDelayedTransition(mMainView, mExpandTransition);
                            onUpdateExpandedViewHolder(toUpdate);
                        }
                    });
                }
            });
            ViewGroup.MarginLayoutParams lp =
                    (ViewGroup.MarginLayoutParams) mSubActionsGridView.getLayoutParams();
            lp.topMargin = avh.itemView.getTop();
            lp.height = 0;
            mSubActionsGridView.setLayoutParams(lp);
            return;
        }
        TransitionHelper.beginDelayedTransition(mMainView, mExpandTransition);
        onUpdateExpandedViewHolder(avh);
    }

    /**
     * @return True if sub actions list is expanded.
     */
    public boolean isSubActionsExpanded() {
        return mExpandedAction != null;
    }

    /**
     * @return Current expanded GuidedAction or null if not expanded.
     */
    public GuidedAction getExpandedAction() {
        return mExpandedAction;
    }

    /**
     * Expand or collapse GuidedActionStylist.
     * @param avh When not null, the GuidedActionStylist expands the sub actions of avh.  When null
     * the GuidedActionStylist will collapse sub actions.
     */
    public void onUpdateExpandedViewHolder(ViewHolder avh) {
        if (avh == null) {
            mExpandedAction = null;
        } else if (avh.getAction() != mExpandedAction) {
            mExpandedAction = avh.getAction();
        }
        // In expanding mode, notifyItemChange on expanded item will reset the translationY by
        // the default ItemAnimator.  So disable ItemAnimation in expanding mode.
        mActionsGridView.setAnimateChildLayout(false);
        final int count = mActionsGridView.getChildCount();
        for (int i = 0; i < count; i++) {
            ViewHolder vh = (ViewHolder) mActionsGridView
                    .getChildViewHolder(mActionsGridView.getChildAt(i));
            updateChevronAndVisibility(vh);
        }
        if (mSubActionsGridView != null) {
            if (avh != null) {
                ViewGroup.MarginLayoutParams lp =
                        (ViewGroup.MarginLayoutParams) mSubActionsGridView.getLayoutParams();
                lp.topMargin = avh.itemView.getTop();
                lp.height = ViewGroup.MarginLayoutParams.MATCH_PARENT;
                mSubActionsGridView.setLayoutParams(lp);
                mSubActionsGridView.setVisibility(View.VISIBLE);
                mSubActionsGridView.requestFocus();
                mSubActionsGridView.setSelectedPosition(0);
                ((GuidedActionAdapter) mSubActionsGridView.getAdapter())
                        .setActions(avh.getAction().getSubActions());
            } else {
                mSubActionsGridView.setVisibility(View.INVISIBLE);
                ViewGroup.MarginLayoutParams lp =
                        (ViewGroup.MarginLayoutParams) mSubActionsGridView.getLayoutParams();
                lp.height = 0;
                mSubActionsGridView.setLayoutParams(lp);
                ((GuidedActionAdapter) mSubActionsGridView.getAdapter())
                        .setActions(Collections.EMPTY_LIST);
                mActionsGridView.requestFocus();
            }
        }
    }

    private void updateChevronAndVisibility(ViewHolder vh) {
        if (!vh.isSubAction()) {
            if (mExpandedAction == null) {
                vh.itemView.setVisibility(View.VISIBLE);
                vh.itemView.setTranslationY(0);
            } else if (vh.getAction() == mExpandedAction) {
                vh.itemView.setVisibility(View.VISIBLE);
                vh.itemView.setTranslationY(- vh.itemView.getHeight());
            } else {
                vh.itemView.setVisibility(View.INVISIBLE);
                vh.itemView.setTranslationY(0);
            }
        }
        if (vh.mChevronView != null) {
            onBindChevronView(vh, vh.getAction());
        }
    }

    /*
     * ==========================================
     * FragmentAnimationProvider overrides
     * ==========================================
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public void onImeAppearing(@NonNull List<Animator> animators) {
        animators.add(createAnimator(mContentView, R.attr.guidedStepImeAppearingAnimation));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onImeDisappearing(@NonNull List<Animator> animators) {
        animators.add(createAnimator(mContentView, R.attr.guidedStepImeDisappearingAnimation));
    }

    /*
     * ==========================================
     * Private methods
     * ==========================================
     */

    private float getFloat(Context ctx, TypedValue typedValue, int attrId) {
        ctx.getTheme().resolveAttribute(attrId, typedValue, true);
        // Android resources don't have a native float type, so we have to use strings.
        return Float.valueOf(ctx.getResources().getString(typedValue.resourceId));
    }

    private int getInteger(Context ctx, TypedValue typedValue, int attrId) {
        ctx.getTheme().resolveAttribute(attrId, typedValue, true);
        return ctx.getResources().getInteger(typedValue.resourceId);
    }

    private int getDimension(Context ctx, TypedValue typedValue, int attrId) {
        ctx.getTheme().resolveAttribute(attrId, typedValue, true);
        return ctx.getResources().getDimensionPixelSize(typedValue.resourceId);
    }

    private static Animator createAnimator(View v, int attrId) {
        Context ctx = v.getContext();
        TypedValue typedValue = new TypedValue();
        ctx.getTheme().resolveAttribute(attrId, typedValue, true);
        Animator animator = AnimatorInflater.loadAnimator(ctx, typedValue.resourceId);
        animator.setTarget(v);
        return animator;
    }

    private boolean setIcon(final ImageView iconView, GuidedAction action) {
        Drawable icon = null;
        if (iconView != null) {
            Context context = iconView.getContext();
            icon = action.getIcon();
            if (icon != null) {
                // setImageDrawable resets the drawable's level unless we set the view level first.
                iconView.setImageLevel(icon.getLevel());
                iconView.setImageDrawable(icon);
                iconView.setVisibility(View.VISIBLE);
            } else {
                iconView.setVisibility(View.GONE);
            }
        }
        return icon != null;
    }

    /**
     * @return the max height in pixels the description can be such that the
     *         action nicely takes up the entire screen.
     */
    private int getDescriptionMaxHeight(Context context, TextView title) {
        // The 2 multiplier on the title height calculation is a
        // conservative estimate for font padding which can not be
        // calculated at this stage since the view hasn't been rendered yet.
        return (int)(mDisplayHeight - 2*mVerticalPadding - 2*mTitleMaxLines*title.getLineHeight());
    }

}
