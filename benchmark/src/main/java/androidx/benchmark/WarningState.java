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

package androidx.benchmark;

import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;

class WarningState {
    private static final String TAG = "Benchmark";

    static final String WARNING_PREFIX;
    private static String sWarningString;

    static {
        ApplicationInfo appInfo = InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getApplicationInfo();
        String warningPrefix = "";
        String warningString = "";
        if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            warningPrefix += "DEBUGGABLE_";
            warningString += "\nWARNING: Debuggable Benchmark\n"
                    + "    Benchmark is running with debuggable=true, which drastically reduces\n"
                    + "    runtime performance in order to support debugging features. Run\n"
                    + "    benchmarks with debuggable=false. Debuggable affects execution speed\n"
                    + "    in ways that mean benchmark improvements might not carry over to a\n"
                    + "    real user's experience (or even regress release performance).\n";
        }
        if (isEmulator()) {
            warningPrefix += "EMULATOR_";
            warningString += "\nWARNING: Running on Emulator\n"
                    + "    Benchmark is running on an emulator, which is not representative of\n"
                    + "    real user devices. Use a physical device to benchmark. Emulator\n"
                    + "    benchmark improvements might not carry over to a real user's\n"
                    + "    experience (or even regress real device performance).\n";
        }
        if (Build.FINGERPRINT.contains(":eng/")) {
            warningPrefix += "ENG-BUILD_";
            warningString += "\nWARNING: Running on Eng Build\n"
                    + "    Benchmark is running on device flashed with a '-eng' build. Eng builds\n"
                    + "    of the platform drastically reduce performance to enable testing\n"
                    + "    changes quickly. For this reason they should not be used for\n"
                    + "    benchmarking. Use a '-user' or '-userdebug' system image.\n";
        }

        try {
            if (isDeviceRooted() && !isCpuLocked()) {
                warningPrefix += "UNLOCKED_";
                warningString += "\nWARNING: unstable CPU clocks\n"
                        + "    Benchmark is running on device with a CPU that has at least one\n"
                        + "    core configured with a variable clock speed. This can lead to\n"
                        + "    inconsistent results due to CPU throttling, which depends on \n"
                        + "    external factors. To lock the CPU clock speed, set the values of\n"
                        + "    scaling_max_freq, scaling_min_freq, and scaling_setspeed to a\n"
                        + "    constant value. Typically, these files are located in\n"
                        + "    /sys/devices/system/cpu/cpu*/cpufreq/. If you have followed these\n"
                        + "    steps and are still seeing this warning, it is possible you have\n"
                        + "    locked the device's clock speed to the minimum possible value.\n";
            }
        } catch (IOException e) {
            // Failed to detect whether the device is rooted.
            Log.d(TAG, "Error while reading device state", e);
        }

        WARNING_PREFIX = warningPrefix;

        if (!warningString.isEmpty()) {
            sWarningString = warningString;
            for (String line : sWarningString.split("\n")) {
                Log.w(TAG, line);
            }
            Log.w(TAG, "");
        }
    }

    @Nullable
    static String acquireWarningStringForLogging() {
        String ret = sWarningString;
        sWarningString = null;
        return ret;
    }

    private static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    private static boolean isDeviceRooted() throws IOException {
        // Check for existence of any bins used to gain root access.
        final String[] suBinPaths = {"/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
                "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
                "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"};

        for (String path : suBinPaths) {
            if (new File(path).exists()) {
                return true;
            }
        }

        // Check for unofficially signed builds of Android.
        final String buildTags = android.os.Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys");
    }

    private static boolean isCpuLocked() {
        // If any core is detected to have a variable clock speed, this flag is set to false.
        boolean didLockCpuClock = true;
        final File cpu_dir = new File("/sys/devices/system/cpu");
        final String[] coreDirs = cpu_dir.list(new FilenameFilter() {
            @Override
            public boolean accept(File current, String name) {
                return new File(current, name).isDirectory() && name.matches("^cpu[0-9]+");
            }
        });

        // Check cpu clock speed locking by testing against each core's minimum possible clock
        // speed.
        for (final String coreDir : coreDirs) {
            try {
                final int cpuMinFreq = Integer.parseInt(readFirstLineFromFile(
                        "/sys/devices/system/cpu/" + coreDir + "/cpufreq/cpuinfo_min_freq"));
                final int scaleCurFreq = Integer.parseInt(readFirstLineFromFile(
                        "/sys/devices/system/cpu/" + coreDir + "/cpufreq/scaling_cur_freq"));
                final boolean coreMightBeLocked = scaleCurFreq != cpuMinFreq;
                didLockCpuClock = didLockCpuClock && coreMightBeLocked;
            } catch (NumberFormatException | IOException e) {
                // Failed to read the cpu clock speed! This can happen in a number of cases where
                // the required files are either missing due to running on an emulator or when the
                // files have been tampered with / not generated by the OS for some cores.
                if (!isEmulator()) {
                    Log.d(TAG, "Error while reading cpu state", e);
                }
            }
        }

        return didLockCpuClock;
    }

    /**
     * Read the first line of a file as a String.
     *
     * Inherently deprecated helper which can be replaced with Files from java.nio APIs once this
     * library's min_sdk is above 26.
     */
    private static String readFirstLineFromFile(String path) throws IOException {
        final BufferedReader reader = new BufferedReader(new FileReader(path));
        final String line = reader.readLine();
        reader.close();

        return line;
    }
}
