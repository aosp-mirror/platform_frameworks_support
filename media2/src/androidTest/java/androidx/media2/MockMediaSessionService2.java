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

package androidx.media2;

import androidx.media2.MediaSession2.SessionCallback;
import androidx.media2.TestUtils.SyncHandler;

import java.util.concurrent.Executor;

/**
 * Mock implementation of {@link MediaSessionService2} for testing.
 */
public class MockMediaSessionService2 extends MediaSessionService2 {
    // Keep in sync with the AndroidManifest.xml
    public static final String ID = "TestSession";

    private MediaSession2 mSession;

    @Override
    public void onCreate() {
        TestServiceRegistry.getInstance().setServiceInstance(this);
        super.onCreate();
    }

    @Override
    public MediaSession2 onCreateSession(String sessionId) {
        final MockPlayer player = new MockPlayer(1);
        final MockPlaylistAgent agent = new MockPlaylistAgent();
        final SyncHandler handler = (SyncHandler) TestServiceRegistry.getInstance().getHandler();
        final Executor executor = new Executor() {
            @Override
            public void execute(Runnable runnable) {
                handler.post(runnable);
            }
        };
        SessionCallback sessionCallback = TestServiceRegistry.getInstance().getSessionCallback();
        if (sessionCallback == null) {
            // Ensures non-null
            sessionCallback = new SessionCallback() {};
        }
        mSession = new MediaSession2.Builder(this)
                .setPlayer(player)
                .setPlaylistAgent(agent)
                .setSessionCallback(executor, sessionCallback)
                .setId(sessionId)
                .build();
        return mSession;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        TestServiceRegistry.getInstance().cleanUp();
    }
}
