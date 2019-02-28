/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.leanback.widget;

import android.view.View;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * PresenterBinder is a class used by {@link Presenter} to bind views. {@link Presenter}
 * uses {@link Presenter#getBinderSelector()} to pick the PresenterBinder.
 *
 * <p>
 * For example:
 * A MovieCardBinder and a TvCardBinder both belongs to ImageCardPresenter, they will share the
 * same ImageCardPresenter.ViewHolder in RecyclerView pool. App sets a
 * {@link ClassPresenterBinderSelector} on the ImageCardPresenter to select either MovieCardBinder
 * or ShowCardBinder based on item's class is Movie or Show.
 *
 * @param <PresenterT> The Presenter type associated with PresenterBinder.
 * @param <VH> The Presenter.ViewHolder type associated with PresenterBinder.
 * @param <ItemT> The data object type.
 *
 * @see Presenter#setBinderSelector(PresenterBinderSelector)
 */
public abstract class PresenterBinder<PresenterT extends Presenter, VH extends Presenter.ViewHolder,
        ItemT> {

    /**
     * Binds a {@link View} to an item.
     * @param presenter The associated Presenter. When multiple PresenterBinders in the same
     *                  {@link ObjectAdapter} uses same Presenter, they will share same ViewHolder
     *                  in RecyclerView pool.
     * @param viewHolder  The ViewHolder which should be updated to represent the contents of the
     *                    item.
     * @param item        The item which should be bound to view holder.
     */
    public abstract void onBindViewHolder(@NonNull PresenterT presenter, @NonNull VH viewHolder,
            @NonNull ItemT item);

    /**
     * Binds a {@link View} to an item with a list of payloads.
     * @param presenter The associated Presenter. When multiple PresenterBinders in the same
     *                  {@link ObjectAdapter} uses same Presenter, they will share same ViewHolder
     *                  in RecyclerView pool.
     * @param viewHolder  The ViewHolder which should be updated to represent the contents of the
     *                    item.
     * @param item        The item which should be bound to view holder.
     * @param payloads    A non-null list of merged payloads. Can be empty list if requires full
     *                    update.
     */
    public void onBindViewHolder(@NonNull PresenterT presenter, @NonNull VH viewHolder,
            @NonNull ItemT item, @NonNull List<Object> payloads) {
        onBindViewHolder(presenter, viewHolder, item);
    }
}
