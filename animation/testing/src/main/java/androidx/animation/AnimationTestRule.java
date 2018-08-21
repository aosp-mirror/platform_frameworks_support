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

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * TODO: Mention caveat: 1) setFrameDelay/getFrameDelay will not be sensible.
 * 2) advanceTimeBy/getCurrentTime needs to be called on the same thread as where anims are started
 *
 */
public class AnimationTestRule implements TestRule {
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
    public void advanceTimeBy(long timeDelta) {
        if (Looper.myLooper() == null) {
            // Throw an exception
        }
        TestHandler.getInstance().advanceTimeBy(timeDelta);
    }
    /**
     * This needs to be called on the same thread as start().
     */
    public long getCurrentTime() {
        if (Looper.myLooper() == null) {
            // Throw an exception
        }
        return TestHandler.getInstance().getCurrentTime();
    }
}

