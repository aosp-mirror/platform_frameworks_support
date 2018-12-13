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

package androidx.car.util;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.view.View;

import androidx.annotation.RestrictTo;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Utility class that helps navigating in GridLayoutManager.
 *
 * <p>Assumes parameter {@code RecyclerView} uses {@link GridLayoutManager}.
 *
 * <p>Assumes the orientation of {@code GridLayoutManager} is vertical.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class GridLayoutManagerUtils {
    private GridLayoutManagerUtils() {}

    /**
     * Returns the number of items in the first row of a RecyclerView that has a
     * {@link GridLayoutManager} as its {@code LayoutManager}.
     *
     * @param recyclerView RecyclerView that uses GridLayoutManager as LayoutManager.
     * @return number of items in the first row in {@code RecyclerView}.
     */
    public static int getFirstRowItemCount(RecyclerView recyclerView) {
        GridLayoutManager manager = (GridLayoutManager) recyclerView.getLayoutManager();
        int itemCount = recyclerView.getAdapter().getItemCount();
        int spanCount = manager.getSpanCount();

        int spanSum = 0;
        int numOfItems = 0;
        while (numOfItems < itemCount && spanSum < spanCount) {
            spanSum += manager.getSpanSizeLookup().getSpanSize(numOfItems);
            numOfItems++;
        }
        return numOfItems;
    }

    /**
     * Returns the span index of an item.
     */
    public static int getSpanIndex(View item) {
        GridLayoutManager.LayoutParams layoutParams =
                ((GridLayoutManager.LayoutParams) item.getLayoutParams());
        return layoutParams.getSpanIndex();
    }

    /**
     * Returns the span size of an item. {@code item} must be already laid out.
     */
    public static int getSpanSize(View item) {
        GridLayoutManager.LayoutParams layoutParams =
                ((GridLayoutManager.LayoutParams) item.getLayoutParams());
        return layoutParams.getSpanSize();
    }

    /**
     * Returns the child view of the last item that is on the same row as input {@code view}.
     *
     * @param view The view to inspect.
     * @param parent {@link RecyclerView} that contains the given view.
     */
    public static View getLastViewOnSameRow(View view, RecyclerView parent) {
        GridLayoutManager glm =  ((GridLayoutManager) parent.getLayoutManager());
        int spanCount = glm.getSpanCount();

        int spanSum = getSpanIndex(view) + getSpanSize(view);

        int position = parent.getChildAdapterPosition(view);
        int itemCount = parent.getAdapter().getItemCount();
        while (position < itemCount) {
            View current = glm.findViewByPosition(position);
            View next = glm.findViewByPosition(position + 1);
            if (next == null) {
                // Assuming views in the same row are all laid out.
                // Next row is not laid out yet. We are at the last view.
                return current;
            }

            int spanSize = getSpanSize(next);
            if (spanSum + spanSize > spanCount) {
                return current;
            }
            spanSum += spanSize;
            position++;
        }
        return glm.findViewByPosition(itemCount - 1);
    }

    /**
     * Returns whether or not the given view is on the last row of a {@code RecyclerView} with a
     * {@link GridLayoutManager}.
     *
     * @param view The view to inspect.
     * @param parent {@link RecyclerView} that contains the given view.
     * @return {@code true} if the given view is on the last row of the {@code RecyclerView}.
     */
    public static boolean isOnLastRow(View view, RecyclerView parent) {
        View lastViewOnSameRow = getLastViewOnSameRow(view, parent);
        int lastViewOnSameRowPosition = parent.getChildAdapterPosition(lastViewOnSameRow);
        return lastViewOnSameRowPosition == parent.getAdapter().getItemCount() - 1;
    }
}
