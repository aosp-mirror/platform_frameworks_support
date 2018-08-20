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

package androidx.paging;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

class TransformedPage<T> {
    final List<T> mItems = new ArrayList<>();

    /**
     * List of 'original' indices represented by a (potentially mapped) item in the transformed page
     *
     * Note that we're careful to store the large component of the indices in
     * {@link #mItemOriginalIndicesOffset} - this means most of the indices in this list will be
     * very low numbers (up to a page count), which avoids autoboxing costs.
     */
    final List<Integer> mItemOriginalIndices = new ArrayList<>();

    private int mItemOriginalIndicesOffset;

    @SuppressWarnings("unchecked")
    public TransformedPage(
            @NonNull List<Transform> transforms,
            int offset,
            @NonNull List<Object> originalItems
    ) {
        this(transforms, offset, originalItems, null);
    }

    @SuppressWarnings("unchecked")
    public <In> TransformedPage(
            @NonNull List<Transform> transforms,
            int offset,
            @NonNull List<Object> originalItems,
            @Nullable SeparatorGenerator<In, T> injector
    ) {
        mItemOriginalIndicesOffset = offset;

        for (int i = 0; i < originalItems.size(); i++) {
            boolean include = true;
            Object current = originalItems.get(i);
            for (int transformIndex = 0; transformIndex < transforms.size(); transformIndex++) {
                Transform transform = transforms.get(transformIndex);

                include = transform.shouldRemain(current);
                if (!include) break;
                current = transform.map(current);
            }
            if (include) {
                if (injector != null && !mItems.isEmpty()) {
                    T injected =injector.generate((In) mItems.get(mItems.size() - 1), (In) current);
                    if (injected != null) {
                        mItems.add(injected);
                        int lastIndex = mItemOriginalIndices.get(mItemOriginalIndices.size() - 1);
                        mItemOriginalIndices.add(lastIndex);
                    }
                }

                mItems.add((T) current);
                mItemOriginalIndices.add(i);
            }
        }
    }

    public void offset(int offset) {
        mItemOriginalIndicesOffset += offset;
    }

    public T get(int index) {
        return mItems.get(index);
    }

    public int getOriginalIndex(int index) {
        return mItemOriginalIndicesOffset + mItemOriginalIndices.get(index);
    }
}
