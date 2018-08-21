/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.graphics.Color;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.internal.view.SupportMenu;
import androidx.core.util.Preconditions;
import androidx.core.util.Supplier;

import java.lang.ref.WeakReference;

/**
 * Helper class for displaying the floating toolbar for a TextView.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.M)
public final class TextViewFloatingToolbarHelper {

    private final TextView mTextView;
    private final Rect mContentRect;
    private final FloatingToolbar mToolbar;

    private BackgroundSpan mHighlight = BackgroundSpan.TRANSPARENT;

    public TextViewFloatingToolbarHelper(TextView textView) {
        mTextView = Preconditions.checkNotNull(textView);
        mContentRect = new Rect();
        final FloatingToolbar toolbar = new FloatingToolbar(textView);
        mToolbar = toolbar;
        mToolbar.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final Menu menu = toolbar.getMenu();
                if (menu != null) {
                    setSelection();
                    final boolean ret = menu.performIdentifierAction(item.getItemId(), 0);
                    removeSelection();
                    return ret;
                }
                return false;
            }

            private void setSelection() {
                if (hasValidTextView()) {
                    final CharSequence text = mTextView.getText();
                    if (text instanceof Spannable) {
                        final Spannable spannable = (Spannable) text;
                        Selection.setSelection(
                                spannable,
                                spannable.getSpanStart(getHighlight()),
                                spannable.getSpanEnd(getHighlight()));
                    }
                }
            }

            private void removeSelection() {
                if (hasValidTextView()) {
                    final CharSequence text = mTextView.getText();
                    if (text instanceof Spannable) {
                        Selection.removeSelection((Spannable) text);
                    }
                }
            }
        });
        mToolbar.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                removeHighlight();
            }

            private void removeHighlight() {
                if (hasValidTextView()) {
                    final CharSequence text = mTextView.getText();
                    if (text instanceof Spannable) {
                        ((Spannable) text).removeSpan(getHighlight());
                    }
                }
            }
        });
        mToolbar.setDismissOnMenuItemClick(true);
    }

    /**
     * Asynchronously requests the floating toolbar to be shown.
     *
     * @param start text start index for positioning the toolbar
     * @param end text end index for positioning the toolbar
     * @param menu the menu that is rendered int the toolbar
     * @param showHighlight whether to highlight the text from start to end index when the toolbar
     *                      is shown
     */
    public void show(
            int start, int end, Menu menu, boolean showHighlight) {
        Preconditions.checkNotNull(menu);
        if (hasValidTextView()) {
            updateHighlight(showHighlight);
            dismiss();
            final CharSequence text = mTextView.getText();
            if (start >= 0 && end <= text.length() && start < end) {
                maybeShowMenu(menu, text, start, end);
            }
        }
    }

    /**
     * Callback when the textView's window focus changes.
     */
    void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus) {
            dismiss();
        }
    }

    /**
     * Callback when the textView's focus changes.
     */
    void onFocusChanged(boolean focused) {
        if (!focused) {
            dismiss();
        }
    }

    @VisibleForTesting
    public boolean isToolbarShowing() {
        return mToolbar.isShowing();
    }

    private void updateHighlight(boolean showHighlight) {
        if (showHighlight) {
            final int color = mTextView.getHighlightColor();
            if (mHighlight.getBackgroundColor() != changeAlpha(color, 20)) {
                mHighlight = new BackgroundSpan(changeAlpha(color, 20));
            }
        } else {
            mHighlight = BackgroundSpan.TRANSPARENT;
        }
    }

    @SuppressWarnings("WeakerAccess") /* Synthetic access. Should be private. */
    BackgroundColorSpan getHighlight() {
        return mHighlight;
    }

    private void dismiss() {
        mToolbar.hide();
        mToolbar.dismiss();
    }

    @SuppressWarnings("WeakerAccess") /* Synthetic access. Should be private. */
    void maybeShowMenu(Menu menu, CharSequence originalText, int start, int end) {
        if (hasValidTextView(originalText) && menu != null && menu.hasVisibleItems()) {
            setHighlight(start, end);
            updateContentRect(start, end);
            show(menu);
        }
    }

    private boolean hasValidTextView() {
        return mTextView.isAttachedToWindow();
    }

    private boolean hasValidTextView(CharSequence text) {
        return hasValidTextView() && mTextView.getText() == text;
    }

    private void setHighlight(final int start, final int end) {
        final CharSequence text = mTextView.getText();
        if (text instanceof Spannable) {
            Selection.removeSelection((Spannable) text);
            final FloatingToolbar toolbar = mToolbar;
            mTextView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (hasValidTextView(text) && toolbar.isShowing()) {
                        ((Spannable) text).setSpan(getHighlight(), start, end, 0);
                    }
                }
            }, 80);
        }
    }

    private void updateContentRect(int start, int end) {
        final int[] startXY = getCoordinates(start);
        final int[] endXY = getCoordinates(end);
        mContentRect.set(startXY[0], startXY[1], endXY[0], endXY[1]);
        mContentRect.sort();
    }

    private int[] getCoordinates(int index) {
        final Layout layout = mTextView.getLayout();
        final int line = layout.getLineForOffset(index);
        return convertToScreenCoordinates(
                layout.getPrimaryHorizontal(index), layout.getLineTop(line));
    }

    private int[] convertToScreenCoordinates(float x, float y) {
        final int[] xy = new int[2];
        mTextView.getLocationOnScreen(xy);
        return new int[]{
                (int) (x + mTextView.getTotalPaddingLeft() - mTextView.getScrollX() + xy[0]),
                (int) (y + mTextView.getTotalPaddingTop() - mTextView.getScrollY() + xy[1])};
    }

    private static int changeAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private void show(final Menu menu) {
        dismiss();
        mToolbar.setMenu((SupportMenu) menu)
                .setContentRect(mContentRect)
                .show();
    }

    /**
     * BackgroundColorSpan that is used to indicate the part of the text that is the subject of the
     * showing toolbar.
     */
    @VisibleForTesting
    public static final class BackgroundSpan extends BackgroundColorSpan {

        private static final BackgroundSpan TRANSPARENT = new BackgroundSpan(Color.TRANSPARENT);

        private BackgroundSpan(int color) {
            super(color);
        }
    }
}