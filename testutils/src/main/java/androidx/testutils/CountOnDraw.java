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

package androidx.testutils;

import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.RequiresApi;

import java.util.concurrent.CountDownLatch;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
class CountOnDraw implements ViewTreeObserver.OnDrawListener {
    final CountDownLatch mLatch;
    final View mView;

    CountOnDraw(CountDownLatch latch, View view) {
        this.mLatch = latch;
        this.mView = view;
    }

    @Override
    public void onDraw() {
        mView.post(new Runnable() {
            @Override
            public void run() {
                mView.getViewTreeObserver().removeOnDrawListener(CountOnDraw.this);
                mLatch.countDown();
            }
        });
    }
}
