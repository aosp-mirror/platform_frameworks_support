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
 * limitations under the License.
 */
package android.support.v4.app;

import static org.junit.Assert.assertEquals;

import android.app.Instrumentation;
import android.os.Handler;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.v4.app.test.FragmentTestActivity;
import android.util.Pair;
import android.view.ViewGroup;
import android.view.animation.Animation;

public class FragmentTestUtil {
    private static final Runnable DO_NOTHING = new Runnable() {
        @Override
        public void run() {
        }
    };

    public static void waitForExecution(final ActivityTestRule<FragmentTestActivity> rule) {
        // Wait for two cycles. When starting a postponed transition, it will post to
        // the UI thread and then the execution will be added onto the queue after that.
        // The two-cycle wait makes sure fragments have the opportunity to complete both
        // before returning.
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(DO_NOTHING);
        instrumentation.runOnMainSync(DO_NOTHING);
    }

    public static boolean executePendingTransactions(
            final ActivityTestRule<FragmentTestActivity> rule) {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        final boolean[] ret = new boolean[1];
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                ret[0] =
                        rule.getActivity().getSupportFragmentManager().executePendingTransactions();
            }
        });
        return ret[0];
    }

    public static boolean popBackStackImmediate(final ActivityTestRule<FragmentTestActivity> rule) {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        final boolean[] ret = new boolean[1];
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                ret[0] = rule.getActivity().getSupportFragmentManager().popBackStackImmediate();
            }
        });
        return ret[0];
    }

    public static boolean popBackStackImmediate(final ActivityTestRule<FragmentTestActivity> rule,
            final int id, final int flags) {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        final boolean[] ret = new boolean[1];
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                ret[0] = rule.getActivity().getSupportFragmentManager().popBackStackImmediate(id,
                        flags);
            }
        });
        return ret[0];
    }

    public static boolean popBackStackImmediate(final ActivityTestRule<FragmentTestActivity> rule,
            final String name, final int flags) {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        final boolean[] ret = new boolean[1];
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                ret[0] = rule.getActivity().getSupportFragmentManager().popBackStackImmediate(name,
                        flags);
            }
        });
        return ret[0];
    }

    public static void setContentView(final ActivityTestRule<FragmentTestActivity> rule,
            final int layoutId) {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                rule.getActivity().setContentView(layoutId);
            }
        });
    }

    public static void assertChildren(ViewGroup container, Fragment... fragments) {
        final int numFragments = fragments == null ? 0 : fragments.length;
        assertEquals("There aren't the correct number of fragment Views in its container",
                numFragments, container.getChildCount());
        for (int i = 0; i < numFragments; i++) {
            assertEquals("Wrong Fragment View order for [" + i + "]", container.getChildAt(i),
                    fragments[i].getView());
        }
    }

    public static FragmentController createController(ActivityTestRule<FragmentTestActivity> rule) {
        final FragmentController[] controller = new FragmentController[1];
        final FragmentTestActivity activity = rule.getActivity();
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                Handler handler = new Handler();
                HostCallbacks hostCallbacks = new HostCallbacks(activity, handler, 0);
                controller[0] = FragmentController.createController(hostCallbacks);
            }
        });
        return controller[0];
    }

    public static void resume(final FragmentController fragmentController,
            final Pair<Parcelable, FragmentManagerNonConfig> savedState) {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                fragmentController.attachHost(null);
                if (savedState != null) {
                    fragmentController.restoreAllState(savedState.first, savedState.second);
                }
                fragmentController.dispatchCreate();
                fragmentController.dispatchActivityCreated();
                fragmentController.noteStateNotSaved();
                fragmentController.execPendingActions();
                fragmentController.dispatchStart();
                fragmentController.reportLoaderStart();
                fragmentController.dispatchResume();
                fragmentController.execPendingActions();
            }
        });
    }

    public static Pair<Parcelable, FragmentManagerNonConfig> destroy(
            final FragmentController fragmentController) {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        final Pair<Parcelable, FragmentManagerNonConfig>[] result = new Pair[1];
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                fragmentController.dispatchPause();
                final Parcelable savedState = fragmentController.saveAllState();
                final FragmentManagerNonConfig nonConfig =
                        fragmentController.retainNestedNonConfig();
                fragmentController.dispatchStop();
                fragmentController.doLoaderStop(false);
                fragmentController.dispatchDestroy();
                fragmentController.doLoaderDestroy();
                result[0] = Pair.create(savedState, nonConfig);
            }
        });
        return result[0];
    }

    public static boolean waitForAnimationEnd(long timeout, final Animation animation) {
        long endTime = SystemClock.uptimeMillis() + timeout;
        final boolean[] hasEnded = new boolean[1];
        Runnable check = new Runnable() {
            @Override
            public void run() {
                hasEnded[0] = animation.hasEnded();
            }
        };
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        do {
            SystemClock.sleep(10);
            instrumentation.runOnMainSync(check);
        } while (!hasEnded[0] && SystemClock.uptimeMillis() < endTime);
        return hasEnded[0];
    }
}
