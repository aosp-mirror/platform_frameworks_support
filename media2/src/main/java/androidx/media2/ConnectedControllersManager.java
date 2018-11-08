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

    public void addController(T key, ControllerInfo controller,
            SessionCommandGroup2 commands) {
        if (key == null || controller == null) {
            if (DEBUG) {
                throw new IllegalArgumentException("key nor controller shouldn't be null");
            }
            return;
        }
        synchronized (mLock) {
            for (int i = 0; i < mConnectedControllerInfos.size(); i++) {
                ConnectedControllerInfo connectedControllerInfo = mConnectedControllerInfos.get(i);
                if (connectedControllerInfo.contains(key)) {
                    // already exist. Only update allowed commands.
                    mConnectedControllerInfos.set(i,
                            new ConnectedControllerInfo(connectedControllerInfo.controllerInfo,
                                    connectedControllerInfo.key, commands,
                                    connectedControllerInfo.sequencedFutureManager));
                    return;
                }
            }
            mConnectedControllerInfos.add(new ConnectedControllerInfo(
                    controller, key, commands, new SequencedFutureManager()));
        }
        // TODO: Also notify controller connected.
    }

    public void updateAllowedCommands(ControllerInfo controller, SessionCommandGroup2 commands) {
        if (controller == null) {
            return;
        }
        synchronized (mLock) {
            for (int i = 0; i < mConnectedControllerInfos.size(); i++) {
                ConnectedControllerInfo connectedControllerInfo = mConnectedControllerInfos.get(i);
                if (connectedControllerInfo.contains(controller)) {
                    mConnectedControllerInfos.set(i,
                            new ConnectedControllerInfo(connectedControllerInfo.controllerInfo,
                                    connectedControllerInfo.key, commands,
                                    connectedControllerInfo.sequencedFutureManager));
                    return;
                }
            }
        }
        // TODO: Also notify allowed command changes here.
    }

    public void removeController(T key) {
        if (key == null) {
            return;
        }
        ConnectedControllerInfo removedController = null;
        synchronized (mLock) {
            for (ConnectedControllerInfo info : mConnectedControllerInfos) {
                if (info.contains(key)) {
                    mConnectedControllerInfos.remove(info);
                    removedController = info;
                    break;
                }
            }
        }
        onControllerRemoved(removedController);
    }

    public void removeController(ControllerInfo controller) {
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

    private void onControllerRemoved(final ConnectedControllerInfo info) {
        if (mSessionImpl.isClosed() || info == null || info.controllerInfo == null) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "Controller " + info.controllerInfo + " is disconnected");
        }
        info.sequencedFutureManager.close();
        mSessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (mSessionImpl.isClosed()) {
                    return;
                }
                mSessionImpl.getCallback().onDisconnected(mSessionImpl.getInstance(),
                        info.controllerInfo);
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
     * @param controller controller
     * @return sequenced future manager. Can be {@code null} if the controller was null or
     *         disconencted.
     */
    @Nullable
    public final SequencedFutureManager getSequencedFutureManager(
            @Nullable ControllerInfo controller) {
        ConnectedControllerInfo info = getConnectedControllerInfo(controller);
        return info != null ? info.sequencedFutureManager : null;
    }

    /**
     * Gets the sequenced future manager.
     *
     * @param key key
     * @return sequenced future manager. Can be {@code null} if the controller was null or
     *         disconencted.
     */
    public SequencedFutureManager getSequencedFutureManager(@Nullable T key) {
        ConnectedControllerInfo info = getConnectedControllerInfo(key);
        return info != null ? info.sequencedFutureManager : null;
    }

    public boolean isAllowedCommand(ControllerInfo controller, SessionCommand2 command) {
        ConnectedControllerInfo info = getConnectedControllerInfo(controller);
        return info != null && info.allowedCommands.hasCommand(command);
    }

    public boolean isAllowedCommand(ControllerInfo controller, @CommandCode int commandCode) {
        ConnectedControllerInfo info = getConnectedControllerInfo(controller);
        return info != null && info.allowedCommands.hasCommand(commandCode);
    }

    public ControllerInfo getController(T key) {
        ConnectedControllerInfo info = getConnectedControllerInfo(key);
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

    private ConnectedControllerInfo getConnectedControllerInfo(T key) {
        if (key == null) {
            return null;
        }
        synchronized (mLock) {
            for (ConnectedControllerInfo info : mConnectedControllerInfos) {
                if (info.contains(key)) {
                    return info;
                }
            }
        }
        return null;
    }

    private class ConnectedControllerInfo {
        public final ControllerInfo controllerInfo;
        public final T key;
        public final SessionCommandGroup2 allowedCommands;
        public final SequencedFutureManager sequencedFutureManager;

        ConnectedControllerInfo(ControllerInfo controllerInfo, T key,
                SessionCommandGroup2 allowedCommands,
                SequencedFutureManager sequencedFutureManager) {
            this.controllerInfo = controllerInfo;
            this.key = key;
            this.allowedCommands = allowedCommands;
            this.sequencedFutureManager = sequencedFutureManager;
        }

        public boolean contains(ControllerInfo controllerInfo) {
            return ObjectsCompat.equals(this.controllerInfo, controllerInfo);
        }

        public boolean contains(T key) {
            if (key instanceof RemoteUserInfo) {
                // Only checks the package name and UID to workaround two things.
                // 1. In MediaBrowserServiceCompat, RemoteUserInfo from onGetRoot and other methods
                //    are differ even for the same controller.
                // 2. For key presses, RemoteUserInfo differs for individual key events.
                RemoteUserInfo remoteUserInfo = (RemoteUserInfo) key;
                RemoteUserInfo other = (RemoteUserInfo) key;
                if (TextUtils.equals(remoteUserInfo.getPackageName(), other.getPackageName())
                        && remoteUserInfo.getUid() == other.getUid()) {
                    return true;
                }
            }
            return ObjectsCompat.equals(this.key, key);
        }
    }
}