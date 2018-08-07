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

package androidx.textclassifier.widget;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.collection.ArrayMap;
import androidx.core.app.RemoteActionCompat;
import androidx.core.internal.view.SupportMenu;
import androidx.core.util.Preconditions;
import androidx.textclassifier.R;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

/**
 * Controls displaying of actions in the floating toolbar.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@RequiresApi(Build.VERSION_CODES.M)
@TargetApi(Build.VERSION_CODES.M)
public final class ToolbarController {

    private static final String LOG_TAG = "ToolbarController";
    private static final int ORDER_START = 50;
    private static final int ALPHA = 20;
    private static final int HIGHLIGHT_DELAY_MS = 80;

    private final TextView mTextView;
    private final Rect mContentRect;
    private final FloatingToolbar mToolbar;
    private final BackgroundSpan mHighlight;

    private static WeakReference<ToolbarController> sInstance = new WeakReference<>(null);

    /**
     * Returns the singleton instance of the toolbar controller for the specified textView.
     */
    public static ToolbarController getInstance(TextView textView) {
        final ToolbarController controller = sInstance.get();
        if (controller == null || controller.mTextView != textView) {
            sInstance = new WeakReference<>(new ToolbarController(textView));
        }
        return sInstance.get();
    }

    private ToolbarController(TextView textView) {
        mTextView = Preconditions.checkNotNull(textView);
        mContentRect = new Rect();
        mHighlight = new BackgroundSpan(withAlpha(mTextView.getHighlightColor()));
        mToolbar = new FloatingToolbar(textView);
        mToolbar.setOnMenuItemClickListener(new OnMenuItemClickListener(mToolbar));
        mToolbar.setDismissOnMenuItemClick(true);
    }

    /**
     * Shows the floating toolbar with the specified actions.
     *
     * @param actions actions to show in the toolbar
     * @param start text start index for positioning the toolbar
     * @param end text end index for positioning the toolbar
     */
    public void show(List<RemoteActionCompat> actions, int start, int end) {
        Preconditions.checkNotNull(actions);
        mToolbar.hide();
        mToolbar.dismiss();
        setOnDismissListener(mTextView, mToolbar);
        final SupportMenu menu = createMenu(mTextView, mHighlight, actions);
        if (hasValidTextView(mTextView) && menu.hasVisibleItems()) {
            final CharSequence text = mTextView.getText();
            if (start >= 0 && end <= text.length() && start < end) {
                setHighlight(mTextView, mHighlight, start, end, mToolbar, HIGHLIGHT_DELAY_MS);
                updateContentRect(mTextView, start, end);
                mToolbar.setMenu(menu)
                        .setContentRect(mContentRect)
                        .show();
            }
        }
    }

    private static void setHighlight(
            final TextView textView, final BackgroundSpan highlight,
            final int start, final int end, final FloatingToolbar toolbar, long delayMs) {
        final CharSequence text = textView.getText();
        if (text instanceof Spannable) {
            // Reset the selection to where the link is.
            Selection.setSelection((Spannable) textView.getText(), start, end);
            Selection.removeSelection((Spannable) text);

            removeHighlight(textView);
            final String originalText = text.toString();
            textView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (hasValidTextView(textView)
                            && originalText.equals(textView.getText().toString())
                            && toolbar.isShowing()) {
                        ((Spannable) text).setSpan(highlight, start, end, 0);
                    }
                }
            }, delayMs);
        }
    }

    private static void removeHighlight(TextView textView) {
        if (hasValidTextView(textView)) {
            final CharSequence text = textView.getText();
            if (text instanceof Spannable) {
                final Spannable spannable = (Spannable) text;
                final BackgroundSpan[] spans =
                        spannable.getSpans(0, text.length(), BackgroundSpan.class);
                for (BackgroundSpan span : spans) {
                    spannable.removeSpan(span);
                }
            }
        }
    }

    private static boolean hasValidTextView(TextView textView) {
        return textView.isAttachedToWindow();
    }

    private void updateContentRect(TextView textView, int start, int end) {
        final int[] startXY = getCoordinates(textView, start);
        final int[] endXY = getCoordinates(textView, end);
        mContentRect.set(startXY[0], startXY[1], endXY[0], endXY[1]);
        mContentRect.sort();
    }

    private static int[] getCoordinates(TextView textView, int index) {
        final Layout layout = textView.getLayout();
        final int line = layout.getLineForOffset(index);
        final int x = (int) layout.getPrimaryHorizontal(index);
        final int y = layout.getLineTop(line);
        final int[] xy = new int[2];
        textView.getLocationOnScreen(xy);
        return new int[]{
                x + textView.getTotalPaddingLeft() - textView.getScrollX() + xy[0],
                y + textView.getTotalPaddingTop() - textView.getScrollY() + xy[1]};
    }

    private static int withAlpha(int color) {
        return Color.argb(ALPHA, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static SupportMenu createMenu(
            final TextView textView,
            final BackgroundSpan highlight,
            List<RemoteActionCompat> actions) {
        final MenuBuilder menu = new MenuBuilder(textView.getContext());
        final Map<MenuItem, PendingIntent> menuActions = new ArrayMap<>();
        final int size = actions.size();
        for (int i = 0; i < size; i++) {
            final RemoteActionCompat action = actions.get(i);
            final MenuItem item = menu.add(
                    FloatingToolbarConstants.MENU_ID_ASSIST  /* groupId */,
                    i == 0 ? FloatingToolbarConstants.MENU_ID_ASSIST : i  /* itemId */,
                    i == 0 ? 0 : ORDER_START + i  /* order */,
                    action.getTitle()  /* title */);
            if (action.shouldShowIcon()) {
                item.setIcon(action.getIcon().loadDrawable(textView.getContext()));
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                item.setContentDescription(action.getContentDescription());
            }
            item.setShowAsAction(i == 0
                    ? MenuItem.SHOW_AS_ACTION_ALWAYS
                    : MenuItem.SHOW_AS_ACTION_NEVER);
            menuActions.put(item, action.getActionIntent());
        }

        menu.add(Menu.NONE, android.R.id.copy, 1,
                android.R.string.copy)
                .setAlphabeticShortcut('c')
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, android.R.id.shareText, 2,
                R.string.abc_share)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menu.setCallback(new MenuBuilder.Callback() {
            @Override
            public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
                final PendingIntent intent = menuActions.get(item);
                if (intent != null) {
                    try {
                        intent.send();
                    } catch (PendingIntent.CanceledException e) {
                        Log.e(LOG_TAG, "Error performing smart action", e);
                    }
                    return true;
                }
                switch (item.getItemId()) {
                    case android.R.id.copy:
                        copyText(textView, highlight);
                        return true;
                    case android.R.id.shareText:
                        shareText(textView, highlight);
                        return true;
                }
                return false;
            }

            @Override
            public void onMenuModeChange(MenuBuilder menu) {}
        });
        return menu;
    }

    private static void copyText(TextView textView, BackgroundSpan highlight) {
        final ClipboardManager clipboard =
                textView.getContext().getSystemService(ClipboardManager.class);
        final String text = getHighlightedText(textView, highlight);
        if (clipboard != null && text != null) {
            try {
                clipboard.setPrimaryClip(ClipData.newPlainText(null, text));
            } catch (Throwable t) {
                Log.d(LOG_TAG, "Error copying text: " + t.getMessage());
            }
        }
    }

    private static void shareText(TextView textView, BackgroundSpan highlight) {
        final String text = getHighlightedText(textView, highlight);
        if (text != null) {
            final Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.removeExtra(android.content.Intent.EXTRA_TEXT);
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);
            textView.getContext().startActivity(Intent.createChooser(sharingIntent, null));
        }
    }

    @Nullable
    private static String getHighlightedText(TextView textView, BackgroundSpan highlight) {
        final CharSequence text = textView.getText();
        if (text instanceof Spannable) {
            final Spannable spannable = (Spannable) text;
            final int start = spannable.getSpanStart(highlight);
            final int end = spannable.getSpanEnd(highlight);
            final int min = Math.max(0, Math.min(start, end));
            final int max = Math.max(0, Math.max(start, end));
            return textView.getText().subSequence(min, max).toString();
        }
        return null;
    }

    private static void setOnDismissListener(TextView textView, FloatingToolbar toolbar) {
        final ViewTreeObserver observer = textView.getViewTreeObserver();
        final OnWindowFocusChangeListener onWindowFocusChangeListener =
                new OnWindowFocusChangeListener(toolbar);
        final OnTextViewFocusChangeListener onTextViewFocusChangeListener =
                new OnTextViewFocusChangeListener(textView, toolbar);
        final OnTextViewDetachedListener onTextViewDetachedListener =
                new OnTextViewDetachedListener(toolbar);
        observer.addOnWindowFocusChangeListener(onWindowFocusChangeListener);
        observer.addOnGlobalFocusChangeListener(onTextViewFocusChangeListener);
        observer.addOnWindowAttachListener(onTextViewDetachedListener);
        toolbar.setOnDismissListener(
                new OnToolbarDismissListener(
                        textView,
                        onWindowFocusChangeListener,
                        onTextViewFocusChangeListener,
                        onTextViewDetachedListener));
    }

    private static final class OnWindowFocusChangeListener
            implements ViewTreeObserver.OnWindowFocusChangeListener {

        private final FloatingToolbar mToolbar;

        private OnWindowFocusChangeListener(FloatingToolbar toolbar) {
            mToolbar = Preconditions.checkNotNull(toolbar);
        }

        @Override
        public void onWindowFocusChanged(boolean hasFocus) {
            if (!hasFocus) {
                mToolbar.dismiss();
            }
        }
    }

    private static final class OnTextViewFocusChangeListener
            implements ViewTreeObserver.OnGlobalFocusChangeListener {

        private final TextView mTextView;
        private final FloatingToolbar mToolbar;

        private OnTextViewFocusChangeListener(TextView textView, FloatingToolbar toolbar) {
            mTextView = Preconditions.checkNotNull(textView);
            mToolbar = Preconditions.checkNotNull(toolbar);
        }

        @Override
        public void onGlobalFocusChanged(View v, View v1) {
            if (!mTextView.hasFocus()) {
                mToolbar.dismiss();
            }
        }
    }

    private static final class OnTextViewDetachedListener
            implements ViewTreeObserver.OnWindowAttachListener {

        private final FloatingToolbar mToolbar;

        private OnTextViewDetachedListener(FloatingToolbar toolbar) {
            mToolbar = Preconditions.checkNotNull(toolbar);
        }

        @Override
        public void onWindowAttached() {}

        @Override
        public void onWindowDetached() {
            mToolbar.dismiss();
        }
    }

    private static final class OnMenuItemClickListener implements MenuItem.OnMenuItemClickListener {

        private final FloatingToolbar mToolbar;

        private OnMenuItemClickListener(FloatingToolbar toolbar) {
            mToolbar = Preconditions.checkNotNull(toolbar);
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            final Menu menu = mToolbar.getMenu();
            if (menu != null) {
                return menu.performIdentifierAction(item.getItemId(), 0);
            }
            return false;
        }
    }

    private static final class OnToolbarDismissListener implements PopupWindow.OnDismissListener {

        private final TextView mTextView;
        private final OnWindowFocusChangeListener mOnWindowFocusChangeListener;
        private final OnTextViewFocusChangeListener mOnFocusChangeListener;
        private final OnTextViewDetachedListener mOnTextViewDetachedListener;

        private OnToolbarDismissListener(
                TextView textView,
                OnWindowFocusChangeListener onWindowFocusChangeListener,
                OnTextViewFocusChangeListener onTextViewFocusChangeListener,
                OnTextViewDetachedListener onTextViewDetachedListener) {
            mTextView = Preconditions.checkNotNull(textView);
            mOnWindowFocusChangeListener = Preconditions.checkNotNull(onWindowFocusChangeListener);
            mOnFocusChangeListener = Preconditions.checkNotNull(onTextViewFocusChangeListener);
            mOnTextViewDetachedListener = Preconditions.checkNotNull(onTextViewDetachedListener);
        }

        @Override
        public void onDismiss() {
            removeHighlight(mTextView);
            final ViewTreeObserver observer = mTextView.getViewTreeObserver();
            observer.removeOnWindowFocusChangeListener(mOnWindowFocusChangeListener);
            observer.removeOnGlobalFocusChangeListener(mOnFocusChangeListener);
            observer.removeOnWindowAttachListener(mOnTextViewDetachedListener);
        }
    }

    /**
     * BackgroundColorSpan that is used to indicate the part of the text that is the subject of the
     * showing toolbar.
     */
    @VisibleForTesting
    public static final class BackgroundSpan extends BackgroundColorSpan {
        private BackgroundSpan(int color) {
            super(color);
        }
    }
}
