/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.Context;
import android.support.v4.view.WindowInsetsCompat;
import android.util.AttributeSet;

/**
 * Expose hasSystemWindowInsets() for testing.
 */
public class NavigationTestView extends NavigationView {

    boolean mHashSystemWindowInsets;

    public NavigationTestView(Context context) {
        this(context, null);
    }

    public NavigationTestView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NavigationTestView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onInsetsChanged(WindowInsetsCompat insets) {
        super.onInsetsChanged(insets);
        mHashSystemWindowInsets = insets.hasSystemWindowInsets();
    }

    public boolean hasSystemWindowInsets() {
        return mHashSystemWindowInsets;
    }
}
