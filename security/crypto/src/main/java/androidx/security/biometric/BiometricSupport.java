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
package androidx.security.biometric;

import androidx.security.crypto.CipherCompat;

import javax.crypto.Cipher;

// TODO Evaluate if this is needed after AndroidX Biometric integration

public interface BiometricSupport {

    public enum BiometricStatus {
        SUCCESS(0),
        FAILED(1),
        CANCELLED(2);

        private final int type;

        BiometricStatus(int type) {
            this.type = type;
        }

        public int getType() {
            return this.type;
        }

        public static BiometricStatus fromId(int id) {
            switch (id) {
                case 0:
                    return SUCCESS;
                case 1:
                    return FAILED;
                case 2:
                    return CANCELLED;
            }
            return CANCELLED;
        }
    }

    void onAuthenticationSucceeded();

    void onAuthenticationHelp(int helpCode, CharSequence helpString);

    void onAuthenticationError(int errorCode, CharSequence errString);

    void onAuthenticationCancelled();

    void onAuthenticationFailed();

    void dismissBiometricDialog();

    void onMessage(String message);

    void authenticate(Cipher cipher, CipherCompat.SecureAuthCallback callback);

}
