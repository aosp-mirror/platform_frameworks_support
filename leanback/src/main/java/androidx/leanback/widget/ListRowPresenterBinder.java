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

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

/**
 * Base PresenterBinder class to bind a {@link ListRow} object to a
 * {@link ListRowPresenter.ViewHolder}.
 *
 * @param <ListRowPresenterT> The type of ListRowPresenter.
 * @param <ListRowVH> The type of ListRowPresenter.ViewHolder.
 * @param <ListRowT> The type of ListRow.
 */
public class ListRowPresenterBinder<
            ListRowPresenterT extends ListRowPresenter,
            ListRowVH extends ListRowPresenter.ViewHolder,
            ListRowT extends ListRow>
        extends RowPresenterBinder<ListRowPresenterT, ListRowVH, ListRowT> {

    @CallSuper
    @Override
    public void onBindRowViewHolder(@NonNull ListRowPresenterT presenter,
            @NonNull ListRowVH viewHolder, @NonNull ListRowT rowItem) {
        super.onBindRowViewHolder(presenter, viewHolder, rowItem);
        viewHolder.mItemBridgeAdapter.setAdapter(rowItem.getAdapter());
        viewHolder.mGridView.setAdapter(viewHolder.mItemBridgeAdapter);
        viewHolder.mGridView.setContentDescription(rowItem.getContentDescription());
    }

}
