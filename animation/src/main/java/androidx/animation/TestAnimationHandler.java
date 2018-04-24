/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.os.Looper;
import android.os.SystemClock;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * TODO: Mention caveat: 1) setFrameDelay/getFrameDelay will not be sensible.
 * 2) advanceTimeBy/getCurrentTime needs to be called on the same thread as where anims are started
 *
 */
public class TestAnimationHandler implements TestRule {

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                AnimationHandler.setupTestMode();
                try {
                    base.evaluate();
                } finally {
                    AnimationHandler.exitTestMode();
                }
            }
        };
    }

    /**
     * This needs to be called on the same thread as start().
     * @param timeDelta
     */
    public void advanceTimeBy(long timeDelta){
        if (Looper.myLooper() == null) {
            // Throw an exception
        }
        InternalTestHandler.getInstance().advanceTimeBy(timeDelta);
    }

    /**
     * This needs to be called on the same thread as start().
     */
    public long getCurrentTime() {
        if (Looper.myLooper() == null) {
            // Throw an exception
        }
        return InternalTestHandler.getInstance().getCurrentTime();
    }

    //TODO: find a more appropriate place for this
    static class InternalTestHandler extends AnimationHandler {

        static final ThreadLocal<InternalTestHandler> sTestHandler = new ThreadLocal<>();
        final long mStartTime;
        // TODO: keep the test flag among the handlers
        // TODO: Should the handler instance always be created on the UI thread?
        public static InternalTestHandler getInstance() {
            if (sTestHandler.get() == null) {
                sTestHandler.set(new InternalTestHandler());
            }
            return sTestHandler.get();
        }

        private InternalTestHandler() {
            super();
            setProvider(new TestProvider());
            mStartTime = SystemClock.uptimeMillis();
        }

        private long mTotalTimeDelta = 0;

        // TODO: annotate timeDelta to be non-negative
        public void advanceTimeBy(long timeDelta) {
            // Advance time & pulse a frame
            mTotalTimeDelta += timeDelta < 0 ? 0 : timeDelta;
            // produce a frame
            onAnimationFrame(getCurrentTime());
        }

        public long getCurrentTime() {
            return mStartTime + mTotalTimeDelta;
        }

        void addAnimationFrameCallback(final AnimationFrameCallback callback) {
            super.addAnimationFrameCallback(callback);
            callback.doAnimationFrame(getCurrentTime());
        }

        private class TestProvider implements AnimationFrameCallbackProvider {
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
}
