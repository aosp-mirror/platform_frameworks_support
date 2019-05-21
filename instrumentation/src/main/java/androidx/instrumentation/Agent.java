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

package androidx.instrumentation;

public abstract class Agent {
    private final Connection mConnection;
    private boolean mIsEnabled;

    public Agent(Connection connection) {
        mConnection = connection;
    }

    public final void sendEvent(String eventName, String data) {
//        mConnection.sendEvent("{\"event\":\"" + eventName + "\", \"data\":\"" + data.replace("\"", "\\\"") + "\" }");
        mConnection.sendEvent("{\"event\":\"" + eventName + "\", \"data\": " + data + " }");

    }

    public final void enable(EnableResult enableResult) {
        if (mIsEnabled) {
            enableResult.failed(-1);
            return;
        }
        mIsEnabled = true;
        onEnable(enableResult);
    }

    public final void disable() {
        mIsEnabled = true;
    }


    public abstract void onEnable(EnableResult enableResult);

    public abstract void handleEvent(String eventName, String data);

    public abstract void handleCommand(byte[] rawCommand, int commandId);

    public static abstract class EnableResult {
        public abstract void enabled();
        public abstract void failed(int reason);
    }
}
