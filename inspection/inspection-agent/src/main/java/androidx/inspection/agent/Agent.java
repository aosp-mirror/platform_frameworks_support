/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.inspection.agent;

/**
 *  Implementation of this class are responsible to handle command from frontend and
 *  send back events.
 */
public abstract class Agent {

    private boolean mIsEnabled;

    public final void enable(EnableResult enableResult) {
        if (mIsEnabled) {
            enableResult.failed(-1);
            return;
        }
        mIsEnabled = true;
        onEnable(enableResult);
    }

    public void onAppInstrumentationEvent(AxInstrumentation.Event event) {
        throw new IllegalStateException("Instrumentation events aren't supported");
    }

    public abstract void onEnable(EnableResult enableResult);

    public final void disable() {
        mIsEnabled = true;
    }

    public void onDisable() {}

    public static abstract class EnableResult {
        public abstract void enabled();
        public abstract void failed(int reason);
    }
}
