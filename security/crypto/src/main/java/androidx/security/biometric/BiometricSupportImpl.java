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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;

import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;

import android.util.Log;

import androidx.security.crypto.CipherCompat;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

// TODO Replace with Androix Biometric!
// TODO Evaluate if this is needed after AndroidX Biometric integration

@TargetApi(Build.VERSION_CODES.P)
public abstract class BiometricSupportImpl extends BiometricPrompt.AuthenticationCallback implements BiometricSupport {

    private static final String TAG = "BiometricSupportImpl";

    private Activity activity;
    Context context;
    private CipherCompat.SecureAuthCallback secureAuthCallback;
    private static final String keyName = "biometric_key";
    private CountDownLatch countDownLatch = null;

    public BiometricSupportImpl(Activity activity, Context context) {
        this.activity = activity;
        this.context = context;

    }

    @Override
    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
        //super.onAuthenticationSucceeded(result);
        dismissBiometricDialog();
        onAuthenticationSucceeded();
        Log.i(TAG, "Fingerprint success!");
        secureAuthCallback.authComplete(BiometricStatus.SUCCESS);
        countDownLatch.countDown();
    }

    @Override
    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
        super.onAuthenticationHelp(helpCode, helpString);
        onMessage(String.valueOf(helpString));
        onAuthenticationFailed();
        secureAuthCallback.authComplete(BiometricStatus.CANCELLED);
        countDownLatch.countDown();
    }


    @Override
    public void onAuthenticationError(int errorCode, CharSequence errString) {
        super.onAuthenticationError(errorCode, errString);
        onMessage(String.valueOf(errString));
        onAuthenticationFailed();
        secureAuthCallback.authComplete(BiometricStatus.FAILED);
        countDownLatch.countDown();
    }

    @Override
    public void onAuthenticationFailed() {
        super.onAuthenticationFailed();
        onAuthenticationFailed();
        secureAuthCallback.authComplete(BiometricStatus.FAILED);
        countDownLatch.countDown();
    }


    @Override
    public void dismissBiometricDialog() {
        // TODO implement this for older versions
    }


    @TargetApi(Build.VERSION_CODES.P)
    public void showAuthDialog(String title, String subtitle, String description, String negativeButtonText, CipherCompat.SecureAuthCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            countDownLatch = new CountDownLatch(1);
            this.secureAuthCallback = callback;

            final BiometricPrompt.AuthenticationCallback authenticationCallback = this;
            final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(2, 2, 0,
                    TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    new BiometricPrompt.Builder(context)
                            .setTitle("Please Auth for key usage.")
                            .setSubtitle("Key used for encrypting files")
                            .setDescription("User authentication required to access key.")
                            .setNegativeButton("Cancel", context.getMainExecutor(), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    onAuthenticationCancelled();
                                }
                            })
                            .build().authenticate(new CancellationSignal(), threadPoolExecutor,
                            authenticationCallback);
                }
            });

            try {
                countDownLatch.await();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // handle earlier versions
        }


    }

    public void authenticate(final Cipher cipher, CipherCompat.SecureAuthCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            countDownLatch = new CountDownLatch(1);
            this.secureAuthCallback = callback;

            final BiometricPrompt.CryptoObject cryptoObject = new BiometricPrompt.CryptoObject(cipher);
            final BiometricPrompt.AuthenticationCallback authenticationCallback = this;
            final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(2, 2, 0,
                    TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    new BiometricPrompt.Builder(context)
                            .setTitle("Please Auth for key usage.")
                            .setSubtitle("Key used for encrypting files")
                            .setDescription("User authentication required to access key.")
                            .setNegativeButton("Cancel", context.getMainExecutor(), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    onAuthenticationCancelled();
                                }
                            })
                            .build().authenticate(cryptoObject, new CancellationSignal(), threadPoolExecutor,
                            authenticationCallback);
                }
            });

            try {
                countDownLatch.await();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // handle earlier versions
        }


    }

    private void generateBiometricKey() {
        try {

            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            keyGenerator.init(new
                    KeyGenParameterSpec.Builder(keyName, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());

            keyGenerator.generateKey();

        } catch (KeyStoreException
                | NoSuchAlgorithmException
                | NoSuchProviderException
                | InvalidAlgorithmParameterException
                | CertificateException
                | IOException exc) {
            exc.printStackTrace();
        }
    }

    private Cipher initCipher() {
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);

        } catch (NoSuchAlgorithmException |
                NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get Cipher", e);
        }

        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(keyName,
                    null);
            cipher.init(Cipher.ENCRYPT_MODE, key);


        } catch (KeyPermanentlyInvalidatedException e) {

        } catch (KeyStoreException | CertificateException
                | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {

            throw new RuntimeException("Failed to init Cipher", e);
        }
        return cipher;
    }

}
