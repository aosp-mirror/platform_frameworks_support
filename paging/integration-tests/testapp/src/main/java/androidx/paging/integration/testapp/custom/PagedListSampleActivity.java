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

package androidx.paging.integration.testapp.custom;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.paging.integration.testapp.R;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Sample PagedList activity with artificial data source.
 */
public class PagedListSampleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view);
        final PagedListItemViewModel viewModel = ViewModelProviders.of(this)
                .get(PagedListItemViewModel.class);

        final PagedListItemAdapter adapter = new PagedListItemAdapter();
        final RecyclerView recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setAdapter(adapter);
        viewModel.getLivePagedList().observe(this, adapter::submitList);
        final Button button = findViewById(R.id.button);
        button.setOnClickListener(v -> viewModel.invalidateList());
    }
}
