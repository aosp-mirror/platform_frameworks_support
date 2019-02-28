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

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.leanback.R;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class PresenterBinderTest {
    private Context mContext;

    @Before
    public void setup() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
    }

    static class MyRow extends Row {
        public int sequenceNumber;
    }

    static class MyRowPresenter extends RowPresenter {
        @Override
        protected RowPresenter.ViewHolder createRowViewHolder(ViewGroup parent) {
            TextView view = new TextView(parent.getContext());
            view.setId(R.id.row_content);
            return new RowPresenter.ViewHolder(view);
        }

        @Override
        protected void onBindRowViewHolder(RowPresenter.ViewHolder vh, Object item) {
            super.onBindRowViewHolder(vh, item);
            ((TextView) vh.view).setText("by Presenter " + item);
        }

    }

    static class MyRowPresenterBinder1 extends RowPresenterBinder<
            MyRowPresenter, RowPresenter.ViewHolder, MyRow> {

        @Override
        public void onBindRowViewHolder(@NonNull MyRowPresenter presenter,
                @NonNull RowPresenter.ViewHolder viewHolder, @NonNull MyRow item) {
            super.onBindRowViewHolder(presenter, viewHolder, item);
            ((TextView) viewHolder.view).setText("by binder1: " + item.sequenceNumber);
        }
    }

    static class MyRowPresenterBinder2 extends RowPresenterBinder<
            MyRowPresenter, RowPresenter.ViewHolder, MyRow> {

        @Override
        public void onBindRowViewHolder(@NonNull MyRowPresenter presenter,
                @NonNull RowPresenter.ViewHolder viewHolder, @NonNull MyRow item) {
            super.onBindRowViewHolder(presenter, viewHolder, item);
            ((TextView) viewHolder.view).setText("by binder2: " + item.sequenceNumber);
        }
    }

    @Test
    public void testRowPresenterBinder() {
        MyRowPresenter presenter = new MyRowPresenter();
        presenter.setBinderSelector(new PresenterBinderSelector() {
            final MyRowPresenterBinder1 mBinder1 = new MyRowPresenterBinder1();
            final MyRowPresenterBinder2 mBinder2 = new MyRowPresenterBinder2();
            @Override
            public PresenterBinder getPresenterBinder(@NonNull Object item) {
                if (0 == (((MyRow) item).sequenceNumber % 2)) {
                    return mBinder1;
                } else {
                    return mBinder2;
                }
            }
        });
        Context context = new ContextThemeWrapper(mContext, R.style.Theme_Leanback);
        FrameLayout parent = new FrameLayout(context);
        Presenter.ViewHolder viewHolder = presenter.onCreateViewHolder(parent);
        MyRowPresenter.ViewHolder rowViewHolder = presenter.getRowViewHolder(viewHolder);
        TextView titleView = rowViewHolder.getHeaderViewHolder().mTitleView;
        TextView textView = (TextView) rowViewHolder.view;
        MyRow row = new MyRow();
        row.setHeaderItem(new HeaderItem("rowTitle"));
        row.sequenceNumber = 1;
        presenter.performBindViewHolder(viewHolder, row);
        assertEquals("by binder2: 1", textView.getText());
        assertEquals("rowTitle", titleView.getText());
        row.setHeaderItem(new HeaderItem("rowTitle changed"));
        row.sequenceNumber = 4;
        presenter.performBindViewHolder(viewHolder, row);
        assertEquals("by binder1: 4", textView.getText());
        assertEquals("rowTitle changed", titleView.getText());
    }
}
