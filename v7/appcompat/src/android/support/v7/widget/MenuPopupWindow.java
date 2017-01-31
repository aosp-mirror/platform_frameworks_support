/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package android.support.v7.widget;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.v4.view.ViewCompat;
import android.support.v7.view.menu.ListMenuItemView;
import android.support.v7.view.menu.MenuAdapter;
import android.support.v7.view.menu.MenuBuilder;
import android.transition.Transition;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.PopupWindow;

import java.lang.reflect.Method;

/**
 * A MenuPopupWindow represents the popup window for menu.
 *
 * MenuPopupWindow is mostly same as ListPopupWindow, but it has customized
 * behaviors specific to menus,
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class MenuPopupWindow extends ListPopupWindow implements MenuItemHoverListener {
    private static final String TAG = "MenuPopupWindow";

    private static Method sSetTouchModalMethod;

    static {
        try {
            sSetTouchModalMethod = PopupWindow.class.getDeclaredMethod(
                    "setTouchModal", boolean.class);
        } catch (NoSuchMethodException e) {
            Log.i(TAG, "Could not find method setTouchModal() on PopupWindow. Oh well.");
        }
    }

    private MenuItemHoverListener mHoverListener;

    public MenuPopupWindow(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    DropDownListView createDropDownListView(Context context, boolean hijackFocus) {
        MenuDropDownListView view = new MenuDropDownListView(context, hijackFocus);
        view.setHoverListener(this);
        return view;
    }

    public void setEnterTransition(Object enterTransition) {
        if (Build.VERSION.SDK_INT >= 23) {
            mPopup.setEnterTransition((Transition) enterTransition);
        }
    }

    public void setExitTransition(Object exitTransition) {
        if (Build.VERSION.SDK_INT >= 23) {
            mPopup.setExitTransition((Transition) exitTransition);
        }
    }

    public void setHoverListener(MenuItemHoverListener hoverListener) {
        mHoverListener = hoverListener;
    }

    /**
     * Set whether this window is touch modal or if outside touches will be sent to
     * other windows behind it.
     */
    public void setTouchModal(final boolean touchModal) {
        if (sSetTouchModalMethod != null) {
            try {
                sSetTouchModalMethod.invoke(mPopup, touchModal);
            } catch (Exception e) {
                Log.i(TAG, "Could not invoke setTouchModal() on PopupWindow. Oh well.");
            }
        }
    }

    @Override
    public void onItemHoverEnter(@NonNull MenuBuilder menu, @NonNull MenuItem item) {
        // Forward up the chain
        if (mHoverListener != null) {
            mHoverListener.onItemHoverEnter(menu, item);
        }
    }

    @Override
    public void onItemHoverExit(@NonNull MenuBuilder menu, @NonNull MenuItem item) {
        // Forward up the chain
        if (mHoverListener != null) {
            mHoverListener.onItemHoverExit(menu, item);
        }
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static class MenuDropDownListView extends DropDownListView {
        final int mAdvanceKey;
        final int mRetreatKey;

        private MenuItemHoverListener mHoverListener;
        private MenuItem mHoveredMenuItem;

        public MenuDropDownListView(Context context, boolean hijackFocus) {
            super(context, hijackFocus);

            final Resources res = context.getResources();
            final Configuration config = res.getConfiguration();
            if (Build.VERSION.SDK_INT >= 17
                    && ViewCompat.LAYOUT_DIRECTION_RTL == config.getLayoutDirection()) {
                mAdvanceKey = KeyEvent.KEYCODE_DPAD_LEFT;
                mRetreatKey = KeyEvent.KEYCODE_DPAD_RIGHT;
            } else {
                mAdvanceKey = KeyEvent.KEYCODE_DPAD_RIGHT;
                mRetreatKey = KeyEvent.KEYCODE_DPAD_LEFT;
            }
        }

        public void setHoverListener(MenuItemHoverListener hoverListener) {
            mHoverListener = hoverListener;
        }

        public void clearSelection() {
            setSelection(INVALID_POSITION);
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            ListMenuItemView selectedItem = (ListMenuItemView) getSelectedView();
            if (selectedItem != null && keyCode == mAdvanceKey) {
                if (selectedItem.isEnabled() && selectedItem.getItemData().hasSubMenu()) {
                    performItemClick(
                            selectedItem,
                            getSelectedItemPosition(),
                            getSelectedItemId());
                }
                return true;
            } else if (selectedItem != null && keyCode == mRetreatKey) {
                setSelection(INVALID_POSITION);

                // Close only the top-level menu.
                ((MenuAdapter) getAdapter()).getAdapterMenu().close(false /* closeAllMenus */);
                return true;
            }
            return super.onKeyDown(keyCode, event);
        }

        @Override
        public boolean onHoverEvent(MotionEvent ev) {
            // Dispatch any changes in hovered item index to the listener.
            if (mHoverListener != null) {
                // The adapter may be wrapped. Adjust the index if necessary.
                final int headersCount;
                final MenuAdapter menuAdapter;
                final ListAdapter adapter = getAdapter();
                if (adapter instanceof HeaderViewListAdapter) {
                    final HeaderViewListAdapter headerAdapter = (HeaderViewListAdapter) adapter;
                    headersCount = headerAdapter.getHeadersCount();
                    menuAdapter = (MenuAdapter) headerAdapter.getWrappedAdapter();
                } else {
                    headersCount = 0;
                    menuAdapter = (MenuAdapter) adapter;
                }

                // Find the menu item for the view at the event coordinates.
                MenuItem menuItem = null;
                if (ev.getAction() != MotionEvent.ACTION_HOVER_EXIT) {
                    final int position = pointToPosition((int) ev.getX(), (int) ev.getY());
                    if (position != INVALID_POSITION) {
                        final int itemPosition = position - headersCount;
                        if (itemPosition >= 0 && itemPosition < menuAdapter.getCount()) {
                            menuItem = menuAdapter.getItem(itemPosition);
                        }
                    }
                }

                final MenuItem oldMenuItem = mHoveredMenuItem;
                if (oldMenuItem != menuItem) {
                    final MenuBuilder menu = menuAdapter.getAdapterMenu();
                    if (oldMenuItem != null) {
                        mHoverListener.onItemHoverExit(menu, oldMenuItem);
                    }

                    mHoveredMenuItem = menuItem;

                    if (menuItem != null) {
                        mHoverListener.onItemHoverEnter(menu, menuItem);
                    }
                }
            }

            return super.onHoverEvent(ev);
        }
    }
}