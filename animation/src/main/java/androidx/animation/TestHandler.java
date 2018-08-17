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

package androidx.animation;

import android.os.SystemClock;

class TestHandler extends  AnimationHandler {
    static final ThreadLocal<TestHandler> sTestHandler = new ThreadLocal<>();
    final long mStartTime;

    public static TestHandler getInstance() {
        if (sTestHandler.get() == null) {
            sTestHandler.set(new TestHandler());
        }
        return sTestHandler.get();
    }

    private TestHandler() {
        super();
        setProvider(new TestProvider());
        mStartTime = SystemClock.uptimeMillis();
    }

    private long mTotalTimeDelta = 0;

    public void advanceTimeBy(long timeDelta) {
        // Advance time & pulse a frame
        mTotalTimeDelta += timeDelta < 0 ? 0 : timeDelta;
        // produce a frame
        onAnimationFrame(getCurrentTime());
    }

    public long getCurrentTime() {
        return mStartTime + mTotalTimeDelta;
    }

    @Override
    void addAnimationFrameCallback(final AnimationFrameCallback callback) {
        super.addAnimationFrameCallback(callback);
        callback.doAnimationFrame(getCurrentTime());
    }

    private static class TestProvider implements AnimationFrameCallbackProvider {
        TestProvider() {}

        @Override
        public void postFrameCallback() {
        }

        @Override
        public void setFrameDelay(long delay) {
        }

        @Override
        public long getFrameDelay() {
            return 0;
        }
    }
}
