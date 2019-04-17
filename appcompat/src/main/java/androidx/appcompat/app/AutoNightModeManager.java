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

package androidx.appcompat.app;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

/**
 * @hide
 */
@VisibleForTesting
@RestrictTo(LIBRARY)
abstract class AutoNightModeManager {
    private BroadcastReceiver mReceiver;

    final AppCompatDelegateImpl mDelegate;
    final Context mContext;

    AutoNightModeManager(AppCompatDelegateImpl delegate) {
        mDelegate = delegate;
        mContext = delegate.mContext;
    }

    @AppCompatDelegate.ApplyableNightMode
    abstract int getApplyableNightMode();

    void onChange() {
        mDelegate.applyDayNight();
    }

    void setup() {
        cleanup();

        final IntentFilter filter = createIntentFilterForBroadcastReceiver();
        if (filter == null || filter.countActions() == 0) {
            // Null or empty IntentFilter, skip
            return;
        }

        if (mReceiver == null) {
            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    onChange();
                }
            };
        }
        mContext.registerReceiver(mReceiver, filter);
    }

    @Nullable
    abstract IntentFilter createIntentFilterForBroadcastReceiver();

    void cleanup() {
        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }
}

/**
 * @hide
 */
@VisibleForTesting
@RestrictTo(LIBRARY)
class AutoTimeNightModeManager extends AutoNightModeManager {
    private final TwilightManager mTwilightManager;

    AutoTimeNightModeManager(@NonNull AppCompatDelegateImpl delegate) {
        super(delegate);
        mTwilightManager = TwilightManager.getInstance(mContext);
    }

    @AppCompatDelegate.ApplyableNightMode
    @Override
    public int getApplyableNightMode() {
        return mTwilightManager.isNight() ? MODE_NIGHT_YES : MODE_NIGHT_NO;
    }

    @Override
    IntentFilter createIntentFilterForBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_TICK);
        return filter;
    }
}

/**
 * @hide
 */
@VisibleForTesting
@RestrictTo(LIBRARY)
class AutoBatteryNightModeManager extends AutoNightModeManager {
    private final PowerManager mPowerManager;

    AutoBatteryNightModeManager(@NonNull AppCompatDelegateImpl delegate) {
        super(delegate);
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
    }

    @AppCompatDelegate.ApplyableNightMode
    @Override
    public int getApplyableNightMode() {
        if (Build.VERSION.SDK_INT >= 21) {
            return mPowerManager.isPowerSaveMode() ? MODE_NIGHT_YES : MODE_NIGHT_NO;
        }
        return MODE_NIGHT_NO;
    }

    @Override
    IntentFilter createIntentFilterForBroadcastReceiver() {
        if (Build.VERSION.SDK_INT >= 21) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
            return filter;
        }
        return null;
    }
}