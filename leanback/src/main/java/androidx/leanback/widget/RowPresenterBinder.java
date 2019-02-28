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
 * Base PresenterBinder class to bind a {@link Row} object to a {@link RowPresenter.ViewHolder}.
 *
 * @see ListRowPresenterBinder
 * @param <RowPresenterT> The type of RowPresenter.
 * @param <RowVH> The type of RowPresenter.ViewHolder.
 * @param <RowT> The type of Row.
 */
public abstract class RowPresenterBinder<
            RowPresenterT extends RowPresenter,
            RowVH extends RowPresenter.ViewHolder,
            RowT extends Row>
        extends PresenterBinder<RowPresenterT, Presenter.ViewHolder, RowT> {

    @Override
    public final void onBindViewHolder(@NonNull RowPresenterT presenter,
            @NonNull Presenter.ViewHolder viewHolder, @NonNull RowT item) {
        onBindRowViewHolder(presenter, (RowVH) presenter.getRowViewHolder(viewHolder), item);
    }

    /**
     * Binding row header and row content. This is the base implementation that implemented
     * header, subclass must call super.onBindRowViewHolder.
     * @param presenter Type of the RowPresenter.
     * @param viewHolder Type of the RowPresenter.ViewHolder.
     * @param item Type of the Row.
     */
    @CallSuper
    public void onBindRowViewHolder(@NonNull RowPresenterT presenter,
            @NonNull RowVH viewHolder,  @NonNull RowT item) {
        viewHolder.mRowObject = item;
        viewHolder.mRow = item;
        if (viewHolder.mHeaderViewHolder != null && viewHolder.getRow() != null) {
            RowHeaderPresenter headerPresenter = presenter.getHeaderPresenter();
            if (headerPresenter != null) {
                headerPresenter.performBindViewHolder(viewHolder.mHeaderViewHolder, item);
            }
        }
    }
}
