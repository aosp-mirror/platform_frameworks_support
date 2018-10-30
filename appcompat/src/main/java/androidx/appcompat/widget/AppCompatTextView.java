/*
 * Copyright (C) 2015 The Android Open Source Project
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

package androidx.appcompat.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.textclassifier.TextClassifier;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.TypefaceCompat;
import androidx.core.text.PrecomputedTextCompat;
import androidx.core.view.TintableBackgroundView;
import androidx.core.widget.AutoSizeableTextView;
import androidx.core.widget.TextViewCompat;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * A {@link TextView} which supports compatible features on older versions of the platform,
 * including:
 * <ul>
 * <li>Allows dynamic tint of its background via the background tint methods in
 * {@link androidx.core.view.ViewCompat}.</li>
 * <li>Allows setting of the background tint using
 * {@link androidx.appcompat.R.attr#backgroundTint} and
 * {@link androidx.appcompat.R.attr#backgroundTintMode}.</li>
 * <li>Supports auto-sizing via {@link androidx.core.widget.TextViewCompat} by allowing to instruct
 * a {@link TextView} to let the size of the text expand or contract automatically to fill its
 * layout based on the TextView's characteristics and boundaries. The style attributes associated
 * with auto-sizing are
 * {@link androidx.appcompat.R.attr#autoSizeTextType},
 * {@link androidx.appcompat.R.attr#autoSizeMinTextSize},
 * {@link androidx.appcompat.R.attr#autoSizeMaxTextSize},
 * {@link androidx.appcompat.R.attr#autoSizeStepGranularity} and
 * {@link androidx.appcompat.R.attr#autoSizePresetSizes}, all of which work back to
 * {@link VERSION_CODES#ICE_CREAM_SANDWICH Ice Cream Sandwich}.</li>
 * </ul>
 *
 * <p>This will automatically be used when you use {@link TextView} in your layouts
 * and the top-level activity / dialog is provided by
 * <a href="{@docRoot}topic/libraries/support-library/packages.html#v7-appcompat">appcompat</a>.
 * You should only need to manually use this class when writing custom views.</p>
 */
public class AppCompatTextView extends TextView implements TintableBackgroundView,
        AutoSizeableTextView {

    private final AppCompatBackgroundHelper mBackgroundTintHelper;
    private final AppCompatTextHelper mTextHelper;
    private final AppCompatTextClassifierHelper mTextClassifierHelper;

    @Nullable
    private Future<PrecomputedTextCompat> mPrecomputedTextFuture;

    public AppCompatTextView(Context context) {
        this(context, null);
    }

    public AppCompatTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public AppCompatTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(TintContextWrapper.wrap(context), attrs, defStyleAttr);

        mBackgroundTintHelper = new AppCompatBackgroundHelper(this);
        mBackgroundTintHelper.loadFromAttributes(attrs, defStyleAttr);

        mTextHelper = new AppCompatTextHelper(this);
        mTextHelper.loadFromAttributes(attrs, defStyleAttr);
        mTextHelper.applyCompoundDrawablesTints();

        mTextClassifierHelper = new AppCompatTextClassifierHelper(this);
    }

    @Override
    public void setBackgroundResource(@DrawableRes int resId) {
        super.setBackgroundResource(resId);
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.onSetBackgroundResource(resId);
        }
    }

    @Override
    public void setBackgroundDrawable(Drawable background) {
        super.setBackgroundDrawable(background);
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.onSetBackgroundDrawable(background);
        }
    }

    /**
     * This should be accessed via
     * {@link androidx.core.view.ViewCompat#setBackgroundTintList(android.view.View, ColorStateList)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void setSupportBackgroundTintList(@Nullable ColorStateList tint) {
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.setSupportBackgroundTintList(tint);
        }
    }

    /**
     * This should be accessed via
     * {@link androidx.core.view.ViewCompat#getBackgroundTintList(android.view.View)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    @Nullable
    public ColorStateList getSupportBackgroundTintList() {
        return mBackgroundTintHelper != null
                ? mBackgroundTintHelper.getSupportBackgroundTintList() : null;
    }

    /**
     * This should be accessed via
     * {@link androidx.core.view.ViewCompat#setBackgroundTintMode(android.view.View, PorterDuff.Mode)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void setSupportBackgroundTintMode(@Nullable PorterDuff.Mode tintMode) {
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.setSupportBackgroundTintMode(tintMode);
        }
    }

    /**
     * This should be accessed via
     * {@link androidx.core.view.ViewCompat#getBackgroundTintMode(android.view.View)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    @Nullable
    public PorterDuff.Mode getSupportBackgroundTintMode() {
        return mBackgroundTintHelper != null
                ? mBackgroundTintHelper.getSupportBackgroundTintMode() : null;
    }

    @Override
    public void setTextAppearance(Context context, int resId) {
        super.setTextAppearance(context, resId);
        if (mTextHelper != null) {
            mTextHelper.onSetTextAppearance(context, resId);
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mBackgroundTintHelper != null) {
            mBackgroundTintHelper.applySupportBackgroundTint();
        }
        if (mTextHelper != null) {
            mTextHelper.applyCompoundDrawablesTints();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mTextHelper != null) {
            mTextHelper.onLayout(changed, left, top, right, bottom);
        }
    }

    @Override
    public void setTextSize(int unit, float size) {
        if (PLATFORM_SUPPORTS_AUTOSIZE) {
            super.setTextSize(unit, size);
        } else {
            if (mTextHelper != null) {
                mTextHelper.setTextSize(unit, size);
            }
        }
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        if (mTextHelper != null && !PLATFORM_SUPPORTS_AUTOSIZE && mTextHelper.isAutoSizeEnabled()) {
            mTextHelper.autoSizeText();
        }
    }

    /**
     * This should be accessed via
     * {@link androidx.core.widget.TextViewCompat#setAutoSizeTextTypeWithDefaults(
     *TextView, int)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void setAutoSizeTextTypeWithDefaults(
            @TextViewCompat.AutoSizeTextType int autoSizeTextType) {
        if (PLATFORM_SUPPORTS_AUTOSIZE) {
            super.setAutoSizeTextTypeWithDefaults(autoSizeTextType);
        } else {
            if (mTextHelper != null) {
                mTextHelper.setAutoSizeTextTypeWithDefaults(autoSizeTextType);
            }
        }
    }

    /**
     * This should be accessed via
     * {@link androidx.core.widget.TextViewCompat#setAutoSizeTextTypeUniformWithConfiguration(
     *TextView, int, int, int, int)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void setAutoSizeTextTypeUniformWithConfiguration(
            int autoSizeMinTextSize,
            int autoSizeMaxTextSize,
            int autoSizeStepGranularity,
            int unit) throws IllegalArgumentException {
        if (PLATFORM_SUPPORTS_AUTOSIZE) {
            super.setAutoSizeTextTypeUniformWithConfiguration(
                    autoSizeMinTextSize, autoSizeMaxTextSize, autoSizeStepGranularity, unit);
        } else {
            if (mTextHelper != null) {
                mTextHelper.setAutoSizeTextTypeUniformWithConfiguration(
                        autoSizeMinTextSize, autoSizeMaxTextSize, autoSizeStepGranularity, unit);
            }
        }
    }

    /**
     * This should be accessed via
     * {@link androidx.core.widget.TextViewCompat#setAutoSizeTextTypeUniformWithPresetSizes(
     *TextView, int[], int)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public void setAutoSizeTextTypeUniformWithPresetSizes(@NonNull int[] presetSizes, int unit)
            throws IllegalArgumentException {
        if (PLATFORM_SUPPORTS_AUTOSIZE) {
            super.setAutoSizeTextTypeUniformWithPresetSizes(presetSizes, unit);
        } else {
            if (mTextHelper != null) {
                mTextHelper.setAutoSizeTextTypeUniformWithPresetSizes(presetSizes, unit);
            }
        }
    }

    /**
     * This should be accessed via
     * {@link androidx.core.widget.TextViewCompat#getAutoSizeTextType(TextView)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    @TextViewCompat.AutoSizeTextType
    public int getAutoSizeTextType() {
        if (PLATFORM_SUPPORTS_AUTOSIZE) {
            return super.getAutoSizeTextType() == TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM
                    ? TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM
                    : TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE;
        } else {
            if (mTextHelper != null) {
                return mTextHelper.getAutoSizeTextType();
            }
        }
        return TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE;
    }

    /**
     * This should be accessed via
     * {@link androidx.core.widget.TextViewCompat#getAutoSizeStepGranularity(TextView)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public int getAutoSizeStepGranularity() {
        if (PLATFORM_SUPPORTS_AUTOSIZE) {
            return super.getAutoSizeStepGranularity();
        } else {
            if (mTextHelper != null) {
                return mTextHelper.getAutoSizeStepGranularity();
            }
        }
        return -1;
    }

    /**
     * This should be accessed via
     * {@link androidx.core.widget.TextViewCompat#getAutoSizeMinTextSize(TextView)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public int getAutoSizeMinTextSize() {
        if (PLATFORM_SUPPORTS_AUTOSIZE) {
            return super.getAutoSizeMinTextSize();
        } else {
            if (mTextHelper != null) {
                return mTextHelper.getAutoSizeMinTextSize();
            }
        }
        return -1;
    }

    /**
     * This should be accessed via
     * {@link androidx.core.widget.TextViewCompat#getAutoSizeMaxTextSize(TextView)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public int getAutoSizeMaxTextSize() {
        if (PLATFORM_SUPPORTS_AUTOSIZE) {
            return super.getAutoSizeMaxTextSize();
        } else {
            if (mTextHelper != null) {
                return mTextHelper.getAutoSizeMaxTextSize();
            }
        }
        return -1;
    }

    /**
     * This should be accessed via
     * {@link androidx.core.widget.TextViewCompat#getAutoSizeTextAvailableSizes(TextView)}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public int[] getAutoSizeTextAvailableSizes() {
        if (PLATFORM_SUPPORTS_AUTOSIZE) {
            return super.getAutoSizeTextAvailableSizes();
        } else {
            if (mTextHelper != null) {
                return mTextHelper.getAutoSizeTextAvailableSizes();
            }
        }
        return new int[0];
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return AppCompatHintHelper.onCreateInputConnection(super.onCreateInputConnection(outAttrs),
                outAttrs, this);
    }

    @Override
    public void setFirstBaselineToTopHeight(@Px @IntRange(from = 0) int firstBaselineToTopHeight) {
        if (Build.VERSION.SDK_INT >= 28) {
            super.setFirstBaselineToTopHeight(firstBaselineToTopHeight);
        } else {
            TextViewCompat.setFirstBaselineToTopHeight(this, firstBaselineToTopHeight);
        }
    }

    @Override
    public void setLastBaselineToBottomHeight(
            @Px @IntRange(from = 0) int lastBaselineToBottomHeight) {
        if (Build.VERSION.SDK_INT >= 28) {
            super.setLastBaselineToBottomHeight(lastBaselineToBottomHeight);
        } else {
            TextViewCompat.setLastBaselineToBottomHeight(this,
                    lastBaselineToBottomHeight);
        }
    }

    @Override
    public int getFirstBaselineToTopHeight() {
        return TextViewCompat.getFirstBaselineToTopHeight(this);
    }

    @Override
    public int getLastBaselineToBottomHeight() {
        return TextViewCompat.getLastBaselineToBottomHeight(this);
    }

    @Override
    public void setLineHeight(@Px @IntRange(from = 0) int lineHeight) {
        TextViewCompat.setLineHeight(this, lineHeight);
    }

    /**
     * See
     * {@link TextViewCompat#setCustomSelectionActionModeCallback(TextView, ActionMode.Callback)}
     */
    @Override
    public void setCustomSelectionActionModeCallback(ActionMode.Callback actionModeCallback) {
        super.setCustomSelectionActionModeCallback(TextViewCompat
                .wrapCustomSelectionActionModeCallback(this, actionModeCallback));
    }

    /**
     * Gets the parameters for text layout precomputation, for use with
     * {@link PrecomputedTextCompat}.
     *
     * @return a current {@link PrecomputedTextCompat.Params}
     * @see PrecomputedTextCompat
     */
    @NonNull
    public PrecomputedTextCompat.Params getTextMetricsParamsCompat() {
        return TextViewCompat.getTextMetricsParams(this);
    }

    /**
     * Apply the text layout parameter.
     *
     * Update the TextView parameters to be compatible with {@link PrecomputedTextCompat.Params}.
     *
     * @see PrecomputedTextCompat
     */
    public void setTextMetricsParamsCompat(@NonNull PrecomputedTextCompat.Params params) {
        TextViewCompat.setTextMetricsParams(this, params);
    }

    /**
     * Sets the PrecomputedTextCompat to the TextView.
     *
     * If the given PrecomputeTextCompat is not compatible with textView, throws an
     * IllegalArgumentException.
     *
     * @param precomputed the precomputed text
     * @throws IllegalArgumentException if precomputed text is not compatible with textView.
     */
    public void setPrecomputedText(@NonNull PrecomputedTextCompat precomputed) {
        TextViewCompat.setPrecomputedText(this, precomputed);
    }

    private void consumeTextFutureAndSetBlocking() {
        if (mPrecomputedTextFuture != null) {
            try {
                Future<PrecomputedTextCompat> future = mPrecomputedTextFuture;
                mPrecomputedTextFuture = null;
                TextViewCompat.setPrecomputedText(this, future.get());
            } catch (InterruptedException | ExecutionException e) {
                // ignore
            }
        }
    }

    @Override
    public CharSequence getText() {
        consumeTextFutureAndSetBlocking();
        return super.getText();
    }

    /**
     * Sets the {@link TextClassifier} for this TextView.
     */
    @Override
    @RequiresApi(api = 26)
    public void setTextClassifier(@Nullable TextClassifier textClassifier) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P || mTextClassifierHelper == null) {
            super.setTextClassifier(textClassifier);
            return;
        }
        mTextClassifierHelper.setTextClassifier(textClassifier);
    }

    /**
     * Returns the {@link TextClassifier} used by this TextView.
     * If no TextClassifier has been set, this TextView uses the default set by the
     * {@link android.view.textclassifier.TextClassificationManager}.
     */
    @Override
    @NonNull
    @RequiresApi(api = 26)
    public TextClassifier getTextClassifier() {
        // The null check is necessary because getTextClassifier is called when we are invoking
        // the super class's constructor.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P || mTextClassifierHelper == null) {
            return super.getTextClassifier();
        }
        return mTextClassifierHelper.getTextClassifier();
    }

    /**
     * Set the precomputed text future.
     *
     * This method sets future of the precomputed text instead of immediately applying text to the
     * TextView. Anything layout related property changes, text size, typeface, letter spacing, etc
     * after this method call will causes IllegalArgumentException during View measurement.
     *
     * See {@link PrecomputedTextCompat#getTextFuture} for more detail.
     *
     * @param future a future for the precomputed text
     * @see PrecomputedTextCompat#getTextFuture
     */
    public void setTextFuture(@Nullable Future<PrecomputedTextCompat> future) {
        mPrecomputedTextFuture = future;
        if (future != null) {
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        consumeTextFutureAndSetBlocking();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void setCompoundDrawablesWithIntrinsicBounds(int left, int top, int right, int bottom) {
        final Context context = getContext();
        setCompoundDrawablesWithIntrinsicBounds(
                left != 0 ? AppCompatResources.getDrawable(context, left) : null,
                top != 0 ? AppCompatResources.getDrawable(context, top) : null,
                right != 0 ? AppCompatResources.getDrawable(context, right) : null,
                bottom != 0 ? AppCompatResources.getDrawable(context, bottom) : null);
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void setCompoundDrawablesRelativeWithIntrinsicBounds(
            int start, int top, int end, int bottom) {
        final Context context = getContext();
        setCompoundDrawablesRelativeWithIntrinsicBounds(
                start != 0 ? AppCompatResources.getDrawable(context, start) : null,
                top != 0 ? AppCompatResources.getDrawable(context, top) : null,
                end != 0 ? AppCompatResources.getDrawable(context, end) : null,
                bottom != 0 ? AppCompatResources.getDrawable(context, bottom) : null);
    }

    @Override
    public void setTypeface(@Nullable Typeface tf, int style) {
        Typeface typefaceFromFamily = null;
        if (Build.VERSION.SDK_INT < VERSION_CODES.LOLLIPOP
                && tf != null && tf.getStyle() != style) {
            typefaceFromFamily = TypefaceCompat.getBestFontFromFamily(getContext(),
                    getContext().getResources(), tf, style);
        }

        super.setTypeface(typefaceFromFamily != null ? typefaceFromFamily : tf, style);
    }
}
