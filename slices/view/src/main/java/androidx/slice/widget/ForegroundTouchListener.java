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

package androidx.slice.widget;

import android.view.MotionEvent;
import android.view.View;

public class ForegroundTouchListener implements View.OnTouchListener {

    private final View mForeground;
    private final int[] mLoc = new int[2];

    public ForegroundTouchListener(View v) {
        mForeground = v;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            mForeground.getLocationOnScreen(mLoc);
            final int x = (int) (event.getRawX() - mLoc[0]);
            final int y = (int) (event.getRawY() - mLoc[1]);
            mForeground.getBackground().setHotspot(x, y);
        }
        int action = event.getActionMasked();
        if (action == android.view.MotionEvent.ACTION_DOWN) {
            mForeground.setPressed(true);
        } else if (action == android.view.MotionEvent.ACTION_CANCEL
                || action == android.view.MotionEvent.ACTION_UP
                || action == android.view.MotionEvent.ACTION_MOVE) {
            mForeground.setPressed(false);
        }
        return false;
    }
}
