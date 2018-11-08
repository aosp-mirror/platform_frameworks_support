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
import androidx.core.util.ObjectsCompat;
import androidx.media.MediaSessionManager.RemoteUserInfo;
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
    private final List<ConnectedControllerInfo> mConnectedControllerInfos = new ArrayList<>();

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
            for (int i = 0; i < mConnectedControllerInfos.size(); i++) {
                ConnectedControllerInfo connectedControllerInfo = mConnectedControllerInfos.get(i);
                if (connectedControllerInfo.contains(controller)) {
                    // already exist. Only update allowed commands.
                    mConnectedControllerInfos.set(i,
                            new ConnectedControllerInfo(connectedControllerInfo.controllerInfo,
                                    connectedControllerInfo.controller, commands,
                                    connectedControllerInfo.sequencedFutureManager));
                    return;
                }
            }
            mConnectedControllerInfos.add(new ConnectedControllerInfo(
                    controllerInfo, controller, commands, new SequencedFutureManager()));
        }
        // TODO: Also notify controller connected.
    }

    public void updateAllowedCommands(ControllerInfo controllerInfo,
            SessionCommandGroup2 commands) {
        if (controllerInfo == null) {
            return;
        }
        synchronized (mLock) {
            for (int i = 0; i < mConnectedControllerInfos.size(); i++) {
                ConnectedControllerInfo connectedControllerInfo = mConnectedControllerInfos.get(i);
                if (connectedControllerInfo.contains(controllerInfo)) {
                    mConnectedControllerInfos.set(i,
                            new ConnectedControllerInfo(connectedControllerInfo.controllerInfo,
                                    connectedControllerInfo.controller, commands,
                                    connectedControllerInfo.sequencedFutureManager));
                    return;
                }
            }
        }
        // TODO: Also notify allowed command changes here.
    }

    public void removeController(T controller) {
        if (controller == null) {
            return;
        }
        ConnectedControllerInfo removedController = null;
        synchronized (mLock) {
            for (ConnectedControllerInfo info : mConnectedControllerInfos) {
                if (info.contains(controller)) {
                    mConnectedControllerInfos.remove(info);
                    removedController = info;
                    break;
                }
            }
        }
        onControllerRemoved(removedController);
    }

    public void removeController(ControllerInfo controllerInfo) {
        if (controllerInfo == null) {
            return;
        }
        ConnectedControllerInfo removedController = null;
        synchronized (mLock) {
            for (ConnectedControllerInfo info : mConnectedControllerInfos) {
                if (info.contains(controllerInfo)) {
                    mConnectedControllerInfos.remove(info);
                    removedController = info;
                    break;
                }
            }
        }
        onControllerRemoved(removedController);
    }

    private void onControllerRemoved(final ConnectedControllerInfo connectedControllerInfo) {
        if (mSessionImpl.isClosed() || connectedControllerInfo == null
                || connectedControllerInfo.controllerInfo == null) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "Controller " + connectedControllerInfo.controllerInfo + " is disconnected");
        }
        connectedControllerInfo.sequencedFutureManager.close();
        mSessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (mSessionImpl.isClosed()) {
                    return;
                }
                mSessionImpl.getCallback().onDisconnected(mSessionImpl.getInstance(),
                        connectedControllerInfo.controllerInfo);
            }
        });
    }

    public final List<ControllerInfo> getConnectedControllers() {
        ArrayList<ControllerInfo> controllers = new ArrayList<>();
        synchronized (mLock) {
            for (ConnectedControllerInfo info : mConnectedControllerInfos) {
                controllers.add(info.controllerInfo);
            }
        }
        return controllers;
    }

    public final boolean isConnected(ControllerInfo controller) {
        return getConnectedControllerInfo(controller) != null;
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
        ConnectedControllerInfo info = getConnectedControllerInfo(controllerInfo);
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
        ConnectedControllerInfo info = getConnectedControllerInfo(controller);
        return info != null ? info.sequencedFutureManager : null;
    }

    public boolean isAllowedCommand(ControllerInfo controllerInfo, SessionCommand2 command) {
        ConnectedControllerInfo info = getConnectedControllerInfo(controllerInfo);
        return info != null && info.allowedCommands.hasCommand(command);
    }

    public boolean isAllowedCommand(ControllerInfo controllerInfo, @CommandCode int commandCode) {
        ConnectedControllerInfo info = getConnectedControllerInfo(controllerInfo);
        return info != null && info.allowedCommands.hasCommand(commandCode);
    }

    public ControllerInfo getController(T controller) {
        ConnectedControllerInfo info = getConnectedControllerInfo(controller);
        return info != null ? info.controllerInfo : null;
    }

    private ConnectedControllerInfo getConnectedControllerInfo(ControllerInfo controllerInfo) {
        if (controllerInfo == null) {
            return null;
        }
        synchronized (mLock) {
            for (ConnectedControllerInfo info : mConnectedControllerInfos) {
                if (info.contains(controllerInfo)) {
                    return info;
                }
            }
        }
        return null;
    }

    private ConnectedControllerInfo getConnectedControllerInfo(T controller) {
        if (controller == null) {
            return null;
        }
        synchronized (mLock) {
            for (ConnectedControllerInfo info : mConnectedControllerInfos) {
                if (info.contains(controller)) {
                    return info;
                }
            }
        }
        return null;
    }

    private class ConnectedControllerInfo {
        public final ControllerInfo controllerInfo;
        public final T controller;
        public final SessionCommandGroup2 allowedCommands;
        public final SequencedFutureManager sequencedFutureManager;

        ConnectedControllerInfo(ControllerInfo controllerInfo, T controller,
                SessionCommandGroup2 allowedCommands,
                SequencedFutureManager sequencedFutureManager) {
            this.controllerInfo = controllerInfo;
            this.controller = controller;
            this.allowedCommands = allowedCommands;
            this.sequencedFutureManager = sequencedFutureManager;
        }

        public boolean contains(ControllerInfo controllerInfo) {
            return ObjectsCompat.equals(this.controllerInfo, controllerInfo);
        }

        public boolean contains(T controller) {
            if (controller instanceof RemoteUserInfo) {
                // Only checks the package name and UID to workaround two things.
                // 1. In MediaBrowserServiceCompat, RemoteUserInfo from onGetRoot and other methods
                //    are differ even for the same controller.
                // 2. For key presses, RemoteUserInfo differs for individual key events.
                RemoteUserInfo remoteUserInfo = (RemoteUserInfo) this.controller;
                RemoteUserInfo other = (RemoteUserInfo) controller;
                if (TextUtils.equals(remoteUserInfo.getPackageName(), other.getPackageName())
                        && remoteUserInfo.getUid() == other.getUid()) {
                    return true;
                }
            }
            return ObjectsCompat.equals(this.controller, controller);
        }
    }
}