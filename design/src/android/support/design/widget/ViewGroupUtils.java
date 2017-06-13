/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.design.widget;

import android.annotation.TargetApi;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.view.ViewGroup;

class ViewGroupUtils {

    private static final ThreadLocal<RectF> sRectF = new ThreadLocal<>();

    private interface ViewGroupUtilsImpl {
        void offsetDescendantRect(ViewGroup parent, View child, RectF rect);
    }

    private static class ViewGroupUtilsImplBase implements ViewGroupUtilsImpl {
        private static final ThreadLocal<Rect> sRectRounded = new ThreadLocal<>();

        ViewGroupUtilsImplBase() {
        }

        @Override
        public void offsetDescendantRect(ViewGroup parent, View child, RectF rect) {
            Rect roundedRect = sRectRounded.get();
            if (roundedRect == null) {
                roundedRect = new Rect();
                sRectRounded.set(roundedRect);
            }

            rect.round(roundedRect);

            parent.offsetDescendantRectToMyCoords(child, roundedRect);
            // View#offsetDescendantRectToMyCoords includes scroll offsets of the last child.
            // We need to reverse it here so that we get the rect of the view itself rather
            // than its content.
            roundedRect.offset(child.getScrollX(), child.getScrollY());

            rect.set(roundedRect);
        }
    }

    @RequiresApi(11)
    @TargetApi(11)
    private static class ViewGroupUtilsImplHoneycomb implements ViewGroupUtilsImpl {
        ViewGroupUtilsImplHoneycomb() {
        }

        @Override
        public void offsetDescendantRect(ViewGroup parent, View child, RectF rect) {
            ViewGroupUtilsHoneycomb.offsetDescendantRect(parent, child, rect);
        }
    }

    private static final ViewGroupUtilsImpl IMPL;

    static {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 11) {
            IMPL = new ViewGroupUtilsImplHoneycomb();
        } else {
            IMPL = new ViewGroupUtilsImplBase();
        }
    }

    /**
     * This is a port of the common
     * {@link ViewGroup#offsetDescendantRectToMyCoords(android.view.View, android.graphics.Rect)}
     * from the framework, but adapted to take transformations into account. The result
     * will be the bounding rect of the real transformed rect.
     *
     * @param descendant view defining the original coordinate system of rect
     * @param rect (in/out) the rect to offset from descendant to this view's coordinate system
     */
    static void offsetDescendantRect(ViewGroup parent, View descendant, RectF rect) {
        IMPL.offsetDescendantRect(parent, descendant, rect);
    }

    /**
     * Retrieve the transformed bounding rect of an arbitrary descendant view.
     * This does not need to be a direct child.
     *
     * @param descendant descendant view to reference
     * @param out rect to set to the bounds of the descendant view
     */
    static void getDescendantRect(ViewGroup parent, View descendant, RectF out) {
        out.set(0, 0, descendant.getWidth(), descendant.getHeight());
        offsetDescendantRect(parent, descendant, out);
    }

    /**
     * Retrieve the transformed bounding rect of an arbitrary descendant view.
     * This does not need to be a direct child.
     *
     * @param descendant descendant view to reference
     * @param out rect to set to the bounds of the descendant view
     */
    static void getDescendantRect(ViewGroup parent, View descendant, Rect out) {
        RectF rectF = sRectF.get();
        if (rectF == null) {
            rectF = new RectF();
            sRectF.set(rectF);
        }

        rectF.set(0, 0, descendant.getWidth(), descendant.getHeight());
        offsetDescendantRect(parent, descendant, rectF);
        rectF.round(out);
    }

}
