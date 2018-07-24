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

package androidx.sparklers;

import static android.content.pm.PackageManager.GET_SIGNATURES;

import static androidx.sparklers.LoadPolicy.ONLY_LOAD;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * Sparklers allow dynamic selection of a library implementation from
 * either local code or loaded from another library based on policy
 * that can be specified in XML (for easy resource overlaying).
 */
public class Sparklers {

    private static final String TAG = "Sparklers";

    private Sparklers() {
    }

    /**
     * Class that turns a {@link LoadPolicy} into a {@link Spark}.
     */
    @NonNull
    public static Spark loadSpark(Context context, LoadPolicy policy) throws SparkException {
        if (policy.getLoadMode() == LoadPolicy.ONLY_LOCAL) {
            if (policy.getLocalVersion() == null) {
                throw new SparkException(
                        "Policy is only local, but no local implementation exists");
            }
            return createLocal(context, policy);
        }
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(policy.getLibrary(), GET_SIGNATURES);
            if (info.signatures == null || info.signatures.length != 1) {
                throw new Exception("No valid signature");
            }
            Signature sig = info.signatures[0];
            String sha256 = getSHA256(sig.toByteArray());
            if (policy.getReleaseSha256() == null || !policy.getReleaseSha256().equals(sha256)) {
                if (Build.TYPE.equals("user") || policy.getDebugSha256() == null
                        || !policy.getDebugSha256().equals(sha256)) {
                    throw new Exception("Incorrect signature " + sha256);
                }
            }
            String loadVersion = info.versionName;
            if (policy.getDisabledVersions().contains(loadVersion)) {
                throw new Exception("Version blacklisted.");
            }
            if (compareVersions(policy.getMinVersion(), loadVersion) < 0) {
                throw new Exception("Version too old.");
            }
            int versionDiff = compareVersions(policy.getLoadMode() == ONLY_LOAD ? null
                    : policy.getLocalVersion(), loadVersion);
            if (versionDiff > 0) {
                // The non-local version is newer, use that.
                return createLoadable(context, loadVersion, info);
            } else if (versionDiff < 0) {
                // The local version is newer, use that.
                return createLocal(context, policy);
            } else {
                // The versions are equivalent, use policy to select.
                if (policy.getLoadMode() == LoadPolicy.PREFER_LOCAL) {
                    return createLocal(context, policy);
                } else {
                    return createLoadable(context, loadVersion, info);
                }
            }
        } catch (Exception e) {
            if (policy.getLocalVersion() == null) {
                throw new SparkException("No local or loadable implementation found for "
                        + policy.getLibrary(), e);
            }
            return createLocal(context, policy);
        }
    }

    private static String getSHA256(byte[] sig)
            throws NoSuchProviderException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA256");
        digest.update(sig);
        byte[] hashtext = digest.digest();
        return bytesToHex(hashtext);
    }

    private static String bytesToHex(byte[] bytes) {
        final char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static Spark createLoadable(Context context, String loadVersion, PackageInfo info)
            throws SparkException {
        try {
            Context loadedContext = context.createPackageContext(info.packageName,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            return new Spark(loadedContext, loadedContext.getClassLoader(), loadVersion,
                    Spark.SOURCE_LOADED);
        } catch (PackageManager.NameNotFoundException e) {
            throw new SparkException("Loadable implementation cannot be loaded", e);
        }
    }

    private static Spark createLocal(Context context, LoadPolicy policy) {
        return new Spark(context, context.getClassLoader(), policy.getLocalVersion(),
                Spark.SOURCE_LOCAL);
    }

    private static int compareVersions(String first, String second) throws SparkException {
        if (first == null) {
            Log.d(TAG, "No local implementation available");
            return 1;
        }
        int[] f = parseVersion(first);
        int[] s = parseVersion(second);
        if (f.length == 0) {
            throw new SparkException("Invalid local version " + first);
        }
        if (s.length == 0) {
            Log.w(TAG, "Invalid loadable version " + second);
            return -1;
        }
        if (f[0] != s[0]) {
            Log.w(TAG, "Major version change, defaulting to local implementation");
            return -1;
        }
        int minLength = Math.min(f.length, s.length);
        for (int i = 1; i < minLength; i++) {
            if (f[i] < s[i]) {
                return 1;
            }
            if (f[i] > s[i]) {
                return -1;
            }
        }
        return 0;
    }

    private static int[] parseVersion(String str) {
        String[] fields = str.split("\\.|-");
        int[] ret = new int[fields.length];
        for (int i = 0; i < fields.length; i++) {
            int extra = 200;
            if (fields[i].startsWith("alpha")) {
                extra = 0;
                fields[i] = fields[i].replace("alpha", "");
            } else if (fields[i].startsWith("beta")) {
                extra = 100;
                fields[i] = fields[i].replace("beta", "");
            }
            ret[i] = Integer.parseInt(fields[i]) + extra;
        }
        return ret;
    }
}
