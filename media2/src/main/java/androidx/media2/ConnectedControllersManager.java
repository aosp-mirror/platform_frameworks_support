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

import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.media2.MediaSession2.ControllerInfo;
import androidx.media2.MediaSession2.MediaSession2Impl;
import androidx.media2.SessionCommand2.CommandCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages connected {@link ControllerInfo}. This is thread-safe.
 */
class ConnectedControllersManager<T> {
    static final String TAG = "MS2ControllerMgr";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private final ArrayMap<T, ControllerInfo> mControllerInfoMap = new ArrayMap<>();
    @GuardedBy("mLock")
    private final ArrayMap<ControllerInfo, ConnectedControllerRecord> mControllerRecords =
            new ArrayMap<>();

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final MediaSession2Impl mSessionImpl;

    ConnectedControllersManager(MediaSession2Impl session) {
        mSessionImpl = session;
    }

    public void addController(T controller, ControllerInfo controllerInfo,
            SessionCommandGroup2 commands) {
        if (controller == null || controllerInfo == null) {
            if (DEBUG) {
                throw new IllegalArgumentException("controller nor controllerInfo shouldn't be"
                        + " null");
            }
            return;
        }
        synchronized (mLock) {
            if (mControllerInfoMap.containsKey(controller)) {
                // already exist. Only update allowed commands.
                ConnectedControllerRecord record = mControllerRecords.get(controllerInfo);
                record.allowedCommands = commands;
            } else {
                mControllerInfoMap.put(controller, controllerInfo);
                mControllerRecords.put(controllerInfo, new ConnectedControllerRecord(
                        controller, new SequencedFutureManager(), commands));
            }
        }
        // TODO: Also notify controller connected.
    }

    public void updateAllowedCommands(ControllerInfo controllerInfo,
            SessionCommandGroup2 commands) {
        if (controllerInfo == null) {
            return;
        }
        synchronized (mLock) {
            ConnectedControllerRecord record = mControllerRecords.get(controllerInfo);
            if (record != null) {
                record.allowedCommands = commands;
                return;
            }
        }
        // TODO: Also notify allowed command changes here.
    }

    public void removeController(T controller) {
        if (controller == null) {
            return;
        }
        removeController(mControllerInfoMap.get(controller));
    }

    public void removeController(final ControllerInfo controllerInfo) {
        if (controllerInfo == null) {
            return;
        }
        ConnectedControllerRecord record;
        synchronized (mLock) {
            record = mControllerRecords.get(controllerInfo);
            mControllerInfoMap.remove(record.controller);
            mControllerRecords.remove(controllerInfo);
        }

        if (DEBUG) {
            Log.d(TAG, "Controller " + controllerInfo + " is disconnected");
        }
        record.sequencedFutureManager.close();
        mSessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (mSessionImpl.isClosed()) {
                    return;
                }
                mSessionImpl.getCallback().onDisconnected(mSessionImpl.getInstance(),
                        controllerInfo);
            }
        });
    }

    public final List<ControllerInfo> getConnectedControllers() {
        ArrayList<ControllerInfo> controllers = new ArrayList<>();
        synchronized (mLock) {
            controllers.addAll(mControllerInfoMap.values());
        }
        return controllers;
    }

    public final boolean isConnected(ControllerInfo controller) {
        return mControllerRecords.get(controller) != null;
    }

    /**
     * Gets the sequenced future manager.
     *
     * @param controllerInfo controller info
     * @return sequenced future manager. Can be {@code null} if the controller was null or
     *         disconencted.
     */
    @Nullable
    public final SequencedFutureManager getSequencedFutureManager(
            @Nullable ControllerInfo controllerInfo) {
        ConnectedControllerRecord info = mControllerRecords.get(controllerInfo);
        return info != null ? info.sequencedFutureManager : null;
    }

    /**
     * Gets the sequenced future manager.
     *
     * @param controller key
     * @return sequenced future manager. Can be {@code null} if the controller was null or
     *         disconencted.
     */
    public SequencedFutureManager getSequencedFutureManager(@Nullable T controller) {
        ConnectedControllerRecord info = mControllerRecords.get(mControllerInfoMap.get(controller));
        return info != null ? info.sequencedFutureManager : null;
    }

    public boolean isAllowedCommand(ControllerInfo controllerInfo, SessionCommand2 command) {
        ConnectedControllerRecord info = mControllerRecords.get(controllerInfo);
        return info != null && info.allowedCommands.hasCommand(command);
    }

    public boolean isAllowedCommand(ControllerInfo controllerInfo, @CommandCode int commandCode) {
        ConnectedControllerRecord info = mControllerRecords.get(controllerInfo);
        return info != null && info.allowedCommands.hasCommand(commandCode);
    }

    public ControllerInfo getController(T controller) {
        return mControllerInfoMap.get(controller);
    }

    private class ConnectedControllerRecord {
        public final T controller;
        public final SequencedFutureManager sequencedFutureManager;
        public SessionCommandGroup2 allowedCommands;

        ConnectedControllerRecord(T controller, SequencedFutureManager sequencedFutureManager,
                SessionCommandGroup2 allowedCommands) {
            this.controller = controller;
            this.sequencedFutureManager = sequencedFutureManager;
            this.allowedCommands = allowedCommands;
        }
    }
}