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

package androidx.core.widget;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.graphics.drawable.GradientDrawable;
import android.support.v4.BaseInstrumentationTestCase;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.test.R;
import androidx.test.filters.LargeTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NestedScrollViewScrollingTest extends
        BaseInstrumentationTestCase<TestContentViewActivity> {

    private NestedScrollView mNestedScrollView;
    private View mChild;

    public NestedScrollViewScrollingTest() {
        super(TestContentViewActivity.class);
    }

    @Test
    public void smoothScrollBy_scrollsEntireDistanceIncludingMargins() throws Throwable {
        setup(200);
        setChildMargins(20, 30);
        attachToActivity(100);

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final int expectedTarget = 150;
        final int scrollDistance = 150;
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNestedScrollView.setOnScrollChangeListener(
                        new NestedScrollView.OnScrollChangeListener() {

                            @Override
                            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY,
                                    int oldScrollX, int oldScrollY) {
                                if (scrollY == expectedTarget) {
                                    countDownLatch.countDown();
                                }
                            }
                        });
                mNestedScrollView.smoothScrollBy(0, scrollDistance);
            }
        });
        assertThat(countDownLatch.await(2000, TimeUnit.SECONDS), is(true));

        assertThat(mNestedScrollView.getScrollY(), is(expectedTarget));
    }

    private void setup(int childHeight) {
        mChild = new View(mActivityTestRule.getActivity());
        mChild.setMinimumWidth(100);
        mChild.setMinimumHeight(childHeight);
        mChild.setBackgroundDrawable(
                new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFFF0000, 0xFF00FF00}));

        mNestedScrollView = new NestedScrollView(mActivityTestRule.getActivity());
        mNestedScrollView.setBackgroundColor(0xFF0000FF);
        mNestedScrollView.addView(mChild);
    }

    private void setChildMargins(int top, int bottom) {
        NestedScrollView.LayoutParams childLayoutParams =
                new NestedScrollView.LayoutParams(100, 100);
        childLayoutParams.topMargin = top;
        childLayoutParams.bottomMargin = bottom;
        mChild.setLayoutParams(childLayoutParams);
    }

    private void attachToActivity(int nestedScrollViewHeight) throws Throwable {
        mNestedScrollView.setLayoutParams(new ViewGroup.LayoutParams(100, nestedScrollViewHeight));

        final TestContentView testContentView =
                mActivityTestRule.getActivity().findViewById(R.id.testContentView);
        testContentView.expectLayouts(1);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                testContentView.addView(mNestedScrollView);
            }
        });
        testContentView.awaitLayouts(2);
    }
}
