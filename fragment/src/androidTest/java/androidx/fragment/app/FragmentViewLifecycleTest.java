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

package androidx.fragment.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.test.FragmentTestActivity;
import androidx.fragment.test.R;
import androidx.lifecycle.GenericLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FragmentViewLifecycleTest {

    @Rule
    public ActivityTestRule<FragmentTestActivity> mActivityRule =
            new ActivityTestRule<>(FragmentTestActivity.class);

    @Test
    @UiThreadTest
    public void testFragmentViewLifecycle() {
        final FragmentTestActivity activity = mActivityRule.getActivity();
        final FragmentManager fm = activity.getSupportFragmentManager();

        final StrictViewFragment fragment = new StrictViewFragment();
        fragment.setLayoutId(R.layout.fragment_a);
        fm.beginTransaction().add(R.id.content, fragment).commitNow();
        assertEquals(Lifecycle.State.RESUMED,
                fragment.getViewLifecycleOwner().getLifecycle().getCurrentState());
    }

    @Test
    @UiThreadTest
    public void testFragmentViewLifecycleNullView() {
        final FragmentTestActivity activity = mActivityRule.getActivity();
        final FragmentManager fm = activity.getSupportFragmentManager();

        final Fragment fragment = new Fragment();
        fm.beginTransaction().add(fragment, "fragment").commitNow();
        try {
            fragment.getViewLifecycleOwner();
            fail("getViewLifecycleOwner should be unavailable if onCreateView returned null");
        } catch (IllegalStateException expected) {
            // Expected
        }
    }

    @Test
    @UiThreadTest
    public void testObserveInOnCreateViewNullView() {
        final FragmentTestActivity activity = mActivityRule.getActivity();
        final FragmentManager fm = activity.getSupportFragmentManager();

        final Fragment fragment = new ObserveInOnCreateViewFragment();
        try {
            fm.beginTransaction().add(fragment, "fragment").commitNow();
            fail("Fragments accessing view lifecycle should fail if onCreateView returned null");
        } catch (IllegalStateException expected) {
            // We need to clean up the Fragment to avoid it still being around
            // when the instrumentation test Activity pauses. Real apps would have
            // just crashed right after onCreateView().
            fm.beginTransaction().remove(fragment).commitNow();
        }
    }

    @Test
    public void testFragmentViewLifecycleRunOnCommit() throws Throwable {
        final FragmentTestActivity activity = mActivityRule.getActivity();
        final FragmentManager fm = activity.getSupportFragmentManager();

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final StrictViewFragment fragment = new StrictViewFragment();
        fragment.setLayoutId(R.layout.fragment_a);
        fm.beginTransaction().add(R.id.content, fragment).runOnCommit(new Runnable() {
            @Override
            public void run() {
                assertEquals(Lifecycle.State.RESUMED,
                        fragment.getViewLifecycleOwner().getLifecycle().getCurrentState());
                countDownLatch.countDown();

            }
        }).commit();
        countDownLatch.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testFragmentViewLifecycleOwnerLiveData() throws Throwable {
        final FragmentTestActivity activity = mActivityRule.getActivity();
        final FragmentManager fm = activity.getSupportFragmentManager();

        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final StrictViewFragment fragment = new StrictViewFragment();
        fragment.setLayoutId(R.layout.fragment_a);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragment.getViewLifecycleOwnerLiveData().observe(activity,
                        new Observer<LifecycleOwner>() {
                            @Override
                            public void onChanged(LifecycleOwner lifecycleOwner) {
                                if (lifecycleOwner != null) {
                                    assertTrue("Fragment View LifecycleOwner should be "
                                                    + "only be set after  onCreateView()",
                                            fragment.mOnCreateViewCalled);
                                    countDownLatch.countDown();
                                } else {
                                    assertTrue("Fragment View LifecycleOwner should be "
                                            + "set to null after onDestroyView()",
                                            fragment.mOnDestroyViewCalled);
                                    countDownLatch.countDown();
                                }
                            }
                        });
                fm.beginTransaction().add(R.id.content, fragment).commitNow();
                // Now remove the Fragment to trigger the destruction of the view
                fm.beginTransaction().remove(fragment).commitNow();
            }
        });
        countDownLatch.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testViewLifecycleInFragmentLifecycle() throws Throwable {
        final FragmentTestActivity activity = mActivityRule.getActivity();
        final FragmentManager fm = activity.getSupportFragmentManager();

        final CountDownLatch upwardCountDownLatch = new CountDownLatch(2);
        final CountDownLatch downwardCountDownLatch = new CountDownLatch(2);
        final StrictViewFragment fragment = new StrictViewFragment();
        fragment.setLayoutId(R.layout.fragment_a);
        final Lifecycle.State[] state = new Lifecycle.State[4];
        final GenericLifecycleObserver onStartObserver = mock(GenericLifecycleObserver.class);
        final GenericLifecycleObserver onResumeObserver = mock(GenericLifecycleObserver.class);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragment.getLifecycle().addObserver(new GenericLifecycleObserver() {
                    private Lifecycle.State getViewLifecyleState() {
                        return fragment.getViewLifecycleOwner().getLifecycle().getCurrentState();
                    }

                    @Override
                    public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
                        if (event == Lifecycle.Event.ON_START) {
                            state[0] = getViewLifecyleState();
                            upwardCountDownLatch.countDown();
                            // The Fragment view lifecycle changes after the Fragment lifecycle
                            // so attach a listener to ensure we get the ON_START event there
                            fragment.getViewLifecycleOwner().getLifecycle()
                                    .addObserver(onStartObserver);
                        } else if (event == Lifecycle.Event.ON_RESUME) {
                            // Remove the listener so we only capture events up to this point
                            fragment.getViewLifecycleOwner().getLifecycle()
                                    .removeObserver(onStartObserver);
                            state[1] = getViewLifecyleState();
                            upwardCountDownLatch.countDown();
                            // The Fragment view lifecycle changes after the Fragment lifecycle
                            // so attach a listener to ensure we get the ON_RESUME event there
                            fragment.getViewLifecycleOwner().getLifecycle()
                                    .addObserver(onResumeObserver);
                        } else if (event == Lifecycle.Event.ON_PAUSE) {
                            state[2] = getViewLifecyleState();
                            downwardCountDownLatch.countDown();
                        } else if (event == Lifecycle.Event.ON_STOP) {
                            state[3] = getViewLifecyleState();
                            downwardCountDownLatch.countDown();
                        }
                    }
                });
                fm.beginTransaction().add(R.id.content, fragment).commitNow();
            }
        });

        upwardCountDownLatch.await(1, TimeUnit.SECONDS);

        // Confirm we are still created when the Fragment's onStart happens
        assertEquals("View Lifecycle should still be created when the Fragment is started",
                state[0], Lifecycle.State.CREATED);
        // Now check to see if our onStartObserver got a ON_START event before onResume
        verify(onStartObserver)
                .onStateChanged(fragment.getViewLifecycleOwner(), Lifecycle.Event.ON_CREATE);
        verify(onStartObserver)
                .onStateChanged(fragment.getViewLifecycleOwner(), Lifecycle.Event.ON_START);
        verifyNoMoreInteractions(onStartObserver);

        // Confirm we are still started when the Fragment's onResume happens
        assertEquals("View Lifecycle should still be started when the Fragment is resumed",
                state[1], Lifecycle.State.STARTED);
        // Now check to see if our onResumeObserver got a ON_RESUME event after the
        // Fragment was resumed
        verify(onResumeObserver, timeout(1000))
                .onStateChanged(fragment.getViewLifecycleOwner(), Lifecycle.Event.ON_CREATE);
        verify(onResumeObserver, timeout(1000))
                .onStateChanged(fragment.getViewLifecycleOwner(), Lifecycle.Event.ON_START);
        verify(onResumeObserver, timeout(1000))
                .onStateChanged(fragment.getViewLifecycleOwner(), Lifecycle.Event.ON_RESUME);
        verifyNoMoreInteractions(onResumeObserver);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragment.getViewLifecycleOwner().getLifecycle()
                        .removeObserver(onResumeObserver);
                // Now remove the Fragment to trigger the destruction of the view
                fm.beginTransaction().remove(fragment).commitNow();
            }
        });

        downwardCountDownLatch.await(1, TimeUnit.SECONDS);
        assertEquals("View Lifecycle should be started when the Fragment is paused",
                state[2], Lifecycle.State.STARTED);
        assertEquals("View Lifecycle should be created when the Fragment is stopped",
                state[3], Lifecycle.State.CREATED);
    }

    @Test
    @UiThreadTest
    public void testFragmentViewLifecycleDetach() {
        final FragmentTestActivity activity = mActivityRule.getActivity();
        final FragmentManager fm = activity.getSupportFragmentManager();

        final ObservingFragment fragment = new ObservingFragment();
        fragment.setLayoutId(R.layout.fragment_a);
        fm.beginTransaction().add(R.id.content, fragment).commitNow();
        LifecycleOwner viewLifecycleOwner = fragment.getViewLifecycleOwner();
        assertEquals(Lifecycle.State.RESUMED,
                viewLifecycleOwner.getLifecycle().getCurrentState());
        assertTrue("LiveData should have active observers when RESUMED",
                fragment.mLiveData.hasActiveObservers());

        fm.beginTransaction().detach(fragment).commitNow();
        assertEquals(Lifecycle.State.DESTROYED,
                viewLifecycleOwner.getLifecycle().getCurrentState());
        assertFalse("LiveData should not have active observers after detach()",
                fragment.mLiveData.hasActiveObservers());
        try {
            fragment.getViewLifecycleOwner();
            fail("getViewLifecycleOwner should be unavailable after onDestroyView");
        } catch (IllegalStateException expected) {
            // Expected
        }
    }

    @Test
    @UiThreadTest
    public void testFragmentViewLifecycleReattach() {
        final FragmentTestActivity activity = mActivityRule.getActivity();
        final FragmentManager fm = activity.getSupportFragmentManager();

        final ObservingFragment fragment = new ObservingFragment();
        fragment.setLayoutId(R.layout.fragment_a);
        fm.beginTransaction().add(R.id.content, fragment).commitNow();
        LifecycleOwner viewLifecycleOwner = fragment.getViewLifecycleOwner();
        assertEquals(Lifecycle.State.RESUMED,
                viewLifecycleOwner.getLifecycle().getCurrentState());
        assertTrue("LiveData should have active observers when RESUMED",
                fragment.mLiveData.hasActiveObservers());

        fm.beginTransaction().detach(fragment).commitNow();
        // The existing view lifecycle should be destroyed
        assertEquals(Lifecycle.State.DESTROYED,
                viewLifecycleOwner.getLifecycle().getCurrentState());
        assertFalse("LiveData should not have active observers after detach()",
                fragment.mLiveData.hasActiveObservers());

        fm.beginTransaction().attach(fragment).commitNow();
        assertNotEquals("A new view LifecycleOwner should be returned after reattachment",
                viewLifecycleOwner, fragment.getViewLifecycleOwner());
        assertEquals(Lifecycle.State.RESUMED,
                fragment.getViewLifecycleOwner().getLifecycle().getCurrentState());
        assertTrue("LiveData should have active observers when RESUMED",
                fragment.mLiveData.hasActiveObservers());
    }

    public static class ObserveInOnCreateViewFragment extends Fragment {
        MutableLiveData<Boolean> mLiveData = new MutableLiveData<>();
        private Observer<Boolean> mOnCreateViewObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean value) {
            }
        };

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            mLiveData.observe(getViewLifecycleOwner(), mOnCreateViewObserver);
            assertTrue("LiveData should have observers after onCreateView observe",
                    mLiveData.hasObservers());
            // Return null - oops!
            return null;
        }

    }

    public static class ObservingFragment extends StrictViewFragment {
        MutableLiveData<Boolean> mLiveData = new MutableLiveData<>();
        private Observer<Boolean> mOnCreateViewObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean value) {
            }
        };
        private Observer<Boolean> mOnViewCreatedObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean value) {
            }
        };
        private Observer<Boolean> mOnViewStateRestoredObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean value) {
            }
        };

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            mLiveData.observe(getViewLifecycleOwner(), mOnCreateViewObserver);
            assertTrue("LiveData should have observers after onCreateView observe",
                    mLiveData.hasObservers());
            return super.onCreateView(inflater, container, savedInstanceState);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mLiveData.observe(getViewLifecycleOwner(), mOnViewCreatedObserver);
            assertTrue("LiveData should have observers after onViewCreated observe",
                    mLiveData.hasObservers());
        }

        @Override
        public void onViewStateRestored(Bundle savedInstanceState) {
            super.onViewStateRestored(savedInstanceState);
            mLiveData.observe(getViewLifecycleOwner(), mOnViewStateRestoredObserver);
            assertTrue("LiveData should have observers after onViewStateRestored observe",
                    mLiveData.hasObservers());
        }
    }
}
