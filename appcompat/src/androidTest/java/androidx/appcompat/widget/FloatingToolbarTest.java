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

package androidx.appcompat.widget;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.actionWithAssertions;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withTagValue;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static com.google.common.truth.Truth.assertThat;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.TextView;

import androidx.appcompat.test.R;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.test.annotation.UiThreadTest;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link FloatingToolbar}.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
public final class FloatingToolbarTest {

    @Rule
    public ActivityTestRule<? extends FloatingToolbarActivity> mActivityTestRule =
            new ActivityTestRule<>(FloatingToolbarActivity.class);

    private FloatingToolbar mFloatingToolbar;
    private Context mContext;

    @Before
    public void setUp() {
        final Activity activity = mActivityTestRule.getActivity();
        mContext = activity;
        final Menu menu = new MenuBuilder(mContext);
        menu.add("One");
        menu.add("Two");
        menu.add("Three");

        final TextView widget = activity.findViewById(R.id.textview);
        mFloatingToolbar = new FloatingToolbar(widget);
        mFloatingToolbar.setMenu(menu);
    }

    @Test
    @UiThreadTest
    public void setMenu() throws Exception {
        final Menu menu = new MenuBuilder(mContext);
        menu.add("Mine");
        mFloatingToolbar.setMenu(menu);
        assertThat(mFloatingToolbar.getMenu()).isEqualTo(menu);
    }

    @Test
    @UiThreadTest
    public void show() throws Exception {
        assertThat(mFloatingToolbar.isShowing()).isFalse();
        mFloatingToolbar.show();
        assertThat(mFloatingToolbar.isShowing()).isTrue();
    }

    @Test
    @UiThreadTest
    public void show_noItems() throws Exception {
        mFloatingToolbar.setMenu(new MenuBuilder(mContext));
        mFloatingToolbar.show();
        assertThat(mFloatingToolbar.isShowing()).isFalse();
    }

    @Test
    @UiThreadTest
    public void show_noVisibleItems() throws Exception {
        final Menu menu = new MenuBuilder(mContext);
        final MenuItem item = menu.add("Mine");
        mFloatingToolbar.setMenu(menu);
        mFloatingToolbar.show();
        assertThat(mFloatingToolbar.isShowing()).isTrue();
        item.setVisible(false);
        mFloatingToolbar.updateLayout();
        assertThat(mFloatingToolbar.isShowing()).isFalse();
    }

    @Test
    @UiThreadTest
    public void dismiss() throws Exception {
        mFloatingToolbar.show();
        assertThat(mFloatingToolbar.isShowing()).isTrue();
        mFloatingToolbar.dismiss();
        assertThat(mFloatingToolbar.isShowing()).isFalse();
    }

    @Test
    @UiThreadTest
    public void hide() throws Exception {
        mFloatingToolbar.show();
        assertThat(mFloatingToolbar.isHidden()).isFalse();
        mFloatingToolbar.hide();
        assertThat(mFloatingToolbar.isHidden()).isTrue();
    }

    @Test
    public void ui_onDismissListener() throws Exception {
        final OnDismissListener onDismissListener = mock(OnDismissListener.class);
        mFloatingToolbar.setOnDismissListener(onDismissListener);
        onWidget().perform(showFloatingToolbar());
        onWidget().perform(dismissFloatingToolbar());
        verify(onDismissListener).onDismiss();
    }

    @Test
    public void ui_showFloatingToolbar() throws Exception {
        onWidget().perform(showFloatingToolbar());
        onFloatingToolbar().check(matches(isDisplayed()));
    }

    @Test
    public void ui_itemClick() throws Exception {
        final Menu menu = new MenuBuilder(mContext);
        final MenuItem menuItem = menu.add("Mine");
        mFloatingToolbar.setMenu(menu);
        final OnMenuItemClickListener onClickListener = mock(OnMenuItemClickListener.class);
        mFloatingToolbar.setOnMenuItemClickListener(onClickListener);

        onWidget().perform(showFloatingToolbar());
        onFloatingToolBarItem(menuItem.getTitle()).perform(click());

        verify(onClickListener).onMenuItemClick(menuItem);
        onFloatingToolbar().check(matches(isDisplayed()));
    }

    @Test
    public void ui_dismissOnItemClick() throws Exception {
        final Menu menu = new MenuBuilder(mContext);
        final MenuItem menuItem = menu.add("Mine");
        mFloatingToolbar.setMenu(menu);
        mFloatingToolbar.setDismissOnMenuItemClick(true);

        onWidget().perform(showFloatingToolbar());
        onFloatingToolBarItem(menuItem.getTitle()).perform(click());

        assertThat(mFloatingToolbar.isShowing()).isFalse();
    }

    private ViewAction showFloatingToolbar() {
        return floatingToolbarAction(
                new Runnable() {
                    @Override
                    public void run() {
                        mFloatingToolbar.show();
                    }
                }, "Show floating toolbar");
    }

    private ViewAction dismissFloatingToolbar() {
        return floatingToolbarAction(
                new Runnable() {
                    @Override
                    public void run() {
                        mFloatingToolbar.dismiss();
                    }
                }, "Dismiss floating toolbar");
    }

    private static ViewInteraction onWidget() {
        return onView(withId(R.id.textview));
    }

    private static ViewAction floatingToolbarAction(
            final Runnable toolbarMethod,
            final String methodDescription) {
        return actionWithAssertions(
                new ViewAction() {
                    @Override
                    public Matcher<View> getConstraints() {
                        return any(View.class);
                    }

                    @Override
                    public String getDescription() {
                        return methodDescription;
                    }

                    @Override
                    public void perform(UiController uiController, View view) {
                        toolbarMethod.run();
                        uiController.loopMainThreadForAtLeast(400);
                    }
                });
    }

    private static ViewInteraction onFloatingToolbar() {
        final Object tag = FloatingToolbarConstants.FLOATING_TOOLBAR_TAG;
        return onView(withTagValue(is(tag)))
                .inRoot(allOf(
                        isPlatformPopup(),
                        withDecorView(hasDescendant(withTagValue(is(tag))))));
    }

    public static ViewInteraction onFloatingToolBarItem(CharSequence itemLabel) {
        final Object tag = FloatingToolbarConstants.FLOATING_TOOLBAR_TAG;
        return onView(withText(itemLabel.toString()))
                .inRoot(withDecorView(hasDescendant(withTagValue(is(tag)))));
    }
}
