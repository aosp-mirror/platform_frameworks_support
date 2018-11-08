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

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.core.util.ObjectsCompat;
import androidx.media.MediaSessionManager.RemoteUserInfo;
import androidx.media2.MediaSession.ControllerInfo;
import androidx.media2.MediaSession.MediaSessionImpl;
import androidx.media2.SessionCommand.CommandCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    final MediaSessionImpl mSessionImpl;

    ConnectedControllersManager(MediaSessionImpl session) {
        mSessionImpl = session;
    }
    public void addController(T controller, ControllerInfo controllerInfo,
            SessionCommandGroup commands) {
        if (controller == null || controllerInfo == null) {
            if (DEBUG) {
                throw new IllegalArgumentException("controller nor controllerInfo shouldn't be"
                        + " null");
            }
            return;
        }
        synchronized (mLock) {
            ControllerInfo savedInfo = getController(controller);
            if (savedInfo == null) {
                mControllerInfoMap.put(controller, controllerInfo);
                mControllerRecords.put(controllerInfo, new ConnectedControllerRecord(
                        controller, new SequencedFutureManager(), commands));
            } else if (!controllerInfo.equals(savedInfo)) {
                // already exist, but controllerInfo is changed.
                mControllerInfoMap.put(controller, controllerInfo);
                ConnectedControllerRecord record = mControllerRecords.remove(savedInfo);
                record.allowedCommands = commands;
                mControllerRecords.put(controllerInfo, record);
            } else {
                // already exist. Only update allowed commands.
                ConnectedControllerRecord record = mControllerRecords.get(controllerInfo);
                record.allowedCommands = commands;
            }
        }
        // TODO: Also notify controller connected.
    }

    public void updateAllowedCommands(ControllerInfo controllerInfo,
            SessionCommandGroup commands) {
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
        removeController(getController(controller));
    }

    public void removeController(final ControllerInfo controllerInfo) {
        if (controllerInfo == null) {
            return;
        }
        ConnectedControllerRecord record;
        synchronized (mLock) {
            record = mControllerRecords.remove(controllerInfo);
            mControllerInfoMap.remove(record.controller);
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

    public final boolean isConnected(ControllerInfo controllerInfo) {
        return mControllerRecords.get(controllerInfo) != null;
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
        ConnectedControllerRecord info = mControllerRecords.get(getController(controller));
        return info != null ? info.sequencedFutureManager : null;
    }

    public boolean isAllowedCommand(ControllerInfo controllerInfo, SessionCommand command) {
        ConnectedControllerRecord info = mControllerRecords.get(controllerInfo);
        return info != null && info.allowedCommands.hasCommand(command);
    }

    public boolean isAllowedCommand(ControllerInfo controllerInfo, @CommandCode int commandCode) {
        ConnectedControllerRecord info = mControllerRecords.get(controllerInfo);
        return info != null && info.allowedCommands.hasCommand(commandCode);
    }

    public ControllerInfo getController(T controller) {
        for (Map.Entry<T, ControllerInfo> e : mControllerInfoMap.entrySet()) {
            if (e.getKey() instanceof RemoteUserInfo) {
                // Only checks the package name and UID to workaround two things.
                // 1. In MediaBrowserServiceCompat, RemoteUserInfo from onGetRoot and other methods
                //    are differ even for the same controller.
                // 2. For key presses, RemoteUserInfo differs for individual key events.
                RemoteUserInfo remoteUserInfo = (RemoteUserInfo) e.getKey();
                RemoteUserInfo other = (RemoteUserInfo) controller;
                if (TextUtils.equals(remoteUserInfo.getPackageName(), other.getPackageName())
                        && remoteUserInfo.getUid() == other.getUid()) {
                    return e.getValue();
                }
            } else if (ObjectsCompat.equals(e.getKey(), controller)) {
                return e.getValue();
            }
        }
        return null;
    }

    private class ConnectedControllerRecord {
        public final T controller;
        public final SequencedFutureManager sequencedFutureManager;
        public SessionCommandGroup allowedCommands;

        ConnectedControllerRecord(T controller, SequencedFutureManager sequencedFutureManager,
                SessionCommandGroup allowedCommands) {
            this.controller = controller;
            this.sequencedFutureManager = sequencedFutureManager;
            this.allowedCommands = allowedCommands;
        }
    }
}