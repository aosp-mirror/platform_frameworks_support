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

package androidx.viewpager2.adapter;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.GenericLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Similar in behavior to {@link FragmentStatePagerAdapter}
 * <p>
 * Lifecycle within {@link RecyclerView}:
 * <ul>
 * <li>{@link RecyclerView.ViewHolder} initially an empty {@link FrameLayout}, serves as a
 * re-usable container for a {@link Fragment} in later stages.
 * <li>{@link RecyclerView.Adapter#onBindViewHolder} we ask for a {@link Fragment} for the
 * position. If we already have the fragment, or have previously saved its state, we use those.
 * <li>{@link RecyclerView.Adapter#onAttachedToWindow} we attach the {@link Fragment} to a
 * container.
 * <li>{@link RecyclerView.Adapter#onViewRecycled} and
 * {@link RecyclerView.Adapter#onFailedToRecycleView} we remove, save state, destroy the
 * {@link Fragment}.
 * </ul>
 */
public abstract class FragmentStateAdapter extends
        RecyclerView.Adapter<FragmentViewHolder> implements StatefulAdapter {
    private static final String STATE_ARG_KEYS = "keys";
    private static final String STATE_ARG_VALUES = "values";

    private final LongSparseArray<Fragment> mFragments = new LongSparseArray<>();
    private final LongSparseArray<Fragment.SavedState> mSavedStates = new LongSparseArray<>();

    private final FragmentManager mFragmentManager;

    public FragmentStateAdapter(@NonNull FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
        super.setHasStableIds(true);
    }

    /**
     * Provide a Fragment associated with the specified position.
     */
    public abstract @NonNull Fragment getItem(int position);

    @NonNull
    @Override
    public final FragmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return FragmentViewHolder.create(parent);
    }

    @Override
    public final void onBindViewHolder(final @NonNull FragmentViewHolder holder, int position) {
        holder.mFragment = getFragment(position);

        /** Special case when {@link RecyclerView} decides to keep the {@link container}
         * attached to the window, but not to the view hierarchy (i.e. parent is null) */
        final FrameLayout container = holder.getContainer();
        if (ViewCompat.isAttachedToWindow(container)) {
            if (container.getParent() != null) {
                throw new IllegalStateException("Design assumption violated.");
            }
            container.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                        int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    if (container.getParent() != null) {
                        container.removeOnLayoutChangeListener(this);
                        onViewAttachedToWindow(holder);
                    }
                }
            });
        }
    }

    private Fragment getFragment(int position) {
        long itemId = getItemId(position);
        Fragment activeFragment = mFragments.get(itemId);
        if (activeFragment != null) {
            return activeFragment;
        }

        Fragment newFragment = getItem(position);
        newFragment.setInitialSavedState(mSavedStates.get(itemId));
        mFragments.put(itemId, newFragment);
        return newFragment;
    }

    @Override
    public final void onViewAttachedToWindow(@NonNull FragmentViewHolder holder) {
        Fragment fragment = holder.mFragment;
        FrameLayout container = holder.getContainer();
        View view = fragment.getView();

        /*
        possible states:
        - fragment: { added, notAdded }
        - view: { created, notCreated }
        - view: { attached, notAttached }

        combinations:
        - { f:added, v:created, v:attached } -> check if attached to the right container
        - { f:added, v:created, v:notAttached} -> attach view to container
        - { f:added, v:notCreated, v:attached } -> impossible
        - { f:added, v:notCreated, v:notAttached} -> schedule callback for when created
        - { f:notAdded, v:created, v:attached } -> illegal state
        - { f:notAdded, v:created, v:notAttached } -> illegal state
        - { f:notAdded, v:notCreated, v:attached } -> impossible
        - { f:notAdded, v:notCreated, v:notAttached } -> add, create, attach
         */

        // { f:notAdded, v:created, v:attached } -> illegal state
        // { f:notAdded, v:created, v:notAttached } -> illegal state
        if (!fragment.isAdded() && view != null) {
            throw new IllegalStateException("Design assumption violated.");
        }

        // { f:added, v:notCreated, v:notAttached} -> schedule callback for when created
        if (fragment.isAdded() && view == null) {
            scheduleViewAttachDetach(fragment, container);
            return;
        }

        // { f:added, v:created, v:attached } -> check if attached to the right container
        if (fragment.isAdded() && view.getParent() != null) {
            if (view.getParent() != container) {
                ((FrameLayout) view.getParent()).removeAllViews();
                addViewToContainer(view, container);
            }
            return;
        }

        // { f:added, v:created, v:notAttached} -> attach view to container
        if (fragment.isAdded()) {
            view.getParent();
            addViewToContainer(view, container);
            return;
        }

        // { f:notAdded, v:notCreated, v:notAttached } -> add, create, attach
        scheduleViewAttachDetach(fragment, container);
        mFragmentManager.beginTransaction().add(fragment, "f" + holder.getItemId()).commitNow();
    }

    private void scheduleViewAttachDetach(final Fragment fragment, final FrameLayout container) {
        // After a config change Fragments that were in FragmentManager will be recreated. Since
        // ViewHolder container ids are dynamically generated, we opted to manually handle
        // attaching Fragment views to containers. For consistency, we use the same mechanism for
        // all Fragment views.
        mFragmentManager.registerFragmentLifecycleCallbacks(
                new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentViewCreated(@NonNull FragmentManager fm,
                            @NonNull Fragment f, @NonNull View v,
                            @Nullable Bundle savedInstanceState) {
                        if (f == fragment) {
                            fm.unregisterFragmentLifecycleCallbacks(this);
                            addViewToContainer(v, container);
                        }
                    }
                }, false);

        // We detach Fragment views from a container when the Fragment is getting destroyed.
        // Without this, Fragments recreated on rotation will try to attach to containers that no
        // longer exist (ViewHolder container ids are dynamically generated).
        fragment.getLifecycle().addObserver(new GenericLifecycleObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source,
                    @NonNull Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    Fragment f = (Fragment) source;
                    View v = f.getView();
                    if (v != null && v.getParent() != null) {
                        FrameLayout container = (FrameLayout) v.getParent();
                        checkChildCount(container, 1).removeAllViews();
                    }
                }
            }
        });
    }

    private void addViewToContainer(@NonNull View v, FrameLayout container) {
        checkChildCount(container, 0).addView(v);
    }

    private FrameLayout checkChildCount(FrameLayout container, int desiredChildCount) {
        if (container.getChildCount() != desiredChildCount) {
            throw new IllegalStateException("Design assumption violated.");
        }
        return container;
    }

    @Override
    public final void onViewRecycled(@NonNull FragmentViewHolder holder) {
        removeFragment(holder);
    }

    @Override
    public final boolean onFailedToRecycleView(@NonNull FragmentViewHolder holder) {
        // This happens when a ViewHolder is in a transient state (e.g. during custom
        // animation). We don't have sufficient information on how to clear up what lead to
        // the transient state, so we are throwing away the ViewHolder to stay on the
        // conservative side.
        removeFragment(holder);
        return false; // don't recycle the view
    }

    private void removeFragment(@NonNull FragmentViewHolder holder) {
        removeFragment(holder.mFragment, holder.getItemId());
        holder.getContainer().removeAllViews();
        holder.mFragment = null;
    }

    /**
     * Removes a Fragment and commits the operation.
     */
    private void removeFragment(Fragment fragment, long itemId) {
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        removeFragment(fragment, itemId, fragmentTransaction);
        fragmentTransaction.commitNow();
    }

    /**
     * Adds a remove operation to the transaction, but does not commit.
     */
    private void removeFragment(Fragment fragment, long itemId,
            @NonNull FragmentTransaction fragmentTransaction) {
        if (fragment == null) {
            return;
        }

        if (fragment.isAdded() && containsItem(itemId)) {
            mSavedStates.put(itemId, mFragmentManager.saveFragmentInstanceState(fragment));
        }

        mFragments.remove(itemId);
        fragmentTransaction.remove(fragment);
    }

    /**
     * Default implementation works for collections that don't add, move, remove items.
     * <p>
     * TODO(b/122670460): add lint rule
     * When overriding, also override {@link #containsItem(long)}.
     * <p>
     * If the item is not a part of the collection, return {@link RecyclerView#NO_ID}.
     *
     * @param position Adapter position
     * @return stable item id {@link RecyclerView.Adapter#hasStableIds()}
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Default implementation works for collections that don't add, move, remove items.
     * <p>
     * TODO(b/122670460): add lint rule
     * When overriding, also override {@link #getItemId(int)}
     */
    public boolean containsItem(long itemId) {
        return itemId >= 0 && itemId < getItemCount();
    }

    @Override
    public final void setHasStableIds(boolean hasStableIds) {
        throw new UnsupportedOperationException(
                "Stable Ids are required for the adapter to function properly, and the adapter "
                        + "takes care of setting the flag.");
    }

    @Override
    public @NonNull Parcelable saveState() {
        Bundle savedState = new Bundle(mFragments.size() + 2);

        /** save references to active fragments */
        for (int ix = 0; ix < mFragments.size(); ix++) {
            long itemId = mFragments.keyAt(ix);
            Fragment fragment = mFragments.get(itemId);
            if (fragment != null && fragment.isAdded()) {
                mFragmentManager.putFragment(savedState, "f" + itemId, fragment);
            }
        }

        /** Write {@link mSavedStates) into a {@link Parcelable} */
        final int length = mSavedStates.size();
        long[] keys = new long[length];
        Fragment.SavedState[] values = new Fragment.SavedState[length];
        for (int ix = 0; ix < length; ix++) {
            long itemId = mSavedStates.keyAt(ix);
            if (containsItem(itemId)) {
                keys[ix] = itemId;
                values[ix] = mSavedStates.get(keys[ix]);
            }
        }

        /** TODO(b/122670461): use custom {@link Parcelable} instead of Bundle to save space */
        savedState.putLongArray(STATE_ARG_KEYS, keys);
        savedState.putParcelableArray(STATE_ARG_VALUES, values);
        return savedState;
    }

    @Override
    public void restoreState(@NonNull Parcelable savedState) {
        try {
            Bundle bundle = (Bundle) savedState;
            for (String key : bundle.keySet()) {
                if (key.startsWith("f")) {
                    Fragment fragment = mFragmentManager.getFragment(bundle, key);
                    long itemId = Long.parseLong(key.substring(1));
                    mFragments.put(itemId, fragment);
                }
            }
            long[] keys = bundle.getLongArray(STATE_ARG_KEYS);
            Fragment.SavedState[] values =
                    (Fragment.SavedState[]) bundle.getParcelableArray(STATE_ARG_VALUES);
            //noinspection ConstantConditions
            if (keys.length != values.length) {
                throw new IllegalStateException();
            }

            mSavedStates.clear();
            for (int ix = 0; ix < keys.length; ix++) {
                long itemId = keys[ix];
                if (containsItem(itemId)) {
                    mSavedStates.put(itemId, values[ix]);
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid savedState passed to the adapter.", ex);
        }
    }
}
