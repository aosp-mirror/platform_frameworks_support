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

package androidx.security.content;

import android.content.SharedPreferences;
import android.util.ArraySet;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.security.SecureConfig;
import androidx.security.crypto.SecureCipher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * A class that provides an encrypted implementation for all SharedPreferences data.
 *
 * @see androidx.security.context.SecureContextCompat#getSharedPreferences(String, int, String)
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SecureSharedPreferencesCompat implements SharedPreferences, SharedPreferences.Editor {

    private static final String TAG = "SecureSharedPreferencesCompat";

    private static final String IV_KEY = "__iv";


    private String mKeyAlias;
    private SecureConfig mSecureConfig;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private List<OnSharedPreferenceChangeListener> mListeners;

    public SecureSharedPreferencesCompat(@NonNull String keyAlias,
            @NonNull SharedPreferences sharedPreferences) {
        this(keyAlias, sharedPreferences, SecureConfig.getDefault());
    }

    public SecureSharedPreferencesCompat(@NonNull String keyAlias,
            @NonNull SharedPreferences sharedPreferences,
            @NonNull SecureConfig secureConfig) {
        mKeyAlias = keyAlias;
        mSharedPreferences = sharedPreferences;
        mSecureConfig = secureConfig;
        mListeners = new ArrayList<>();
    }

    // SharedPreferences.Editor methods

    @Override
    public SharedPreferences.Editor putString(String key, @Nullable String value) {
        putEncryptedObject(key, String.class.getName(), value);
        return this;
    }

    @Override
    public SharedPreferences.Editor putStringSet(String key, @Nullable Set<String> values) {
        putEncryptedObject(key, Set.class.getName(), new JSONArray(values));
        return this;
    }

    @Override
    public SharedPreferences.Editor putInt(String key, int value) {
        putEncryptedObject(key, Integer.class.getName(), value);
        return this;
    }

    @Override
    public SharedPreferences.Editor putLong(String key, long value) {
        putEncryptedObject(key, Long.class.getName(), value);
        return this;
    }

    @Override
    public SharedPreferences.Editor putFloat(String key, float value) {
        putEncryptedObject(key, Float.class.getName(), value);
        return this;
    }

    @Override
    public SharedPreferences.Editor putBoolean(String key, boolean value) {
        putEncryptedObject(key, Boolean.class.getName(), value);
        return this;
    }

    @Override
    public SharedPreferences.Editor remove(String key) {
        mEditor.remove(key);
        mEditor.remove(key + IV_KEY);
        notifyListeners(key);
        return this;
    }

    @Override
    public SharedPreferences.Editor clear() {
        mEditor.clear();
        return this;
    }

    @Override
    public boolean commit() {
        return mEditor.commit();
    }

    @Override
    public void apply() {
        mEditor.apply();
    }

    private byte[] wrap(String key, String type, Object value) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("key", key);
            jsonObject.put("type", type);
            jsonObject.put("value", value);

            return Base64.decode(jsonObject.toString(), Base64.DEFAULT);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void putEncryptedObject(final String key, String type, Object value) {
        byte[] data = wrap(key, type, value);
        SecureCipher secureCipher = SecureCipher.getInstance(mSecureConfig);
        secureCipher.encrypt(mKeyAlias, data, new SecureCipher.SecureSymmetricEncryptionListener() {
                    @Override
                    public void encryptionComplete(@NonNull byte[] cipherText, @NonNull byte[] iv) {
                        String encrypted = Base64.encodeToString(cipherText, Base64.DEFAULT);
                        String ivString = Base64.encodeToString(iv, Base64.DEFAULT);
                        mEditor.putString(key, encrypted);
                        mEditor.putString(key + IV_KEY, ivString);
                        notifyListeners(key);
                    }
                });
    }

    // SharedPreferences methods

    @Override
    public Map<String, ?> getAll() {
        Map<String, ? super Object> allEntries = new HashMap<>();
        for (Map.Entry<String, ?> entry : mSharedPreferences.getAll().entrySet()) {
            String key = entry.getKey();
            if (!key.endsWith(IV_KEY)) {
                allEntries.put(key, getDecryptedObject(key, null));
            }
        }
        return allEntries;
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        Object value = getDecryptedObject(key, String.class.getName());
        return (value != null && value instanceof String ? (String) value : defValue);
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        Set<String> returnValues = null;
        Object value = getDecryptedObject(key, Set.class.getName());
        if (value instanceof JSONArray) {
            returnValues = new ArraySet<>();
            JSONArray jsonArray = (JSONArray) value;
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    returnValues.add(jsonArray.getString(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            returnValues = defValues;
        }
        return returnValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        Object value = getDecryptedObject(key, Integer.class.getName());
        return (value != null && value instanceof Integer ? (Integer) value : defValue);
    }

    @Override
    public long getLong(String key, long defValue) {
        Object value = getDecryptedObject(key, Long.class.getName());
        return (value != null && value instanceof Long ? (Long) value : defValue);
    }

    @Override
    public float getFloat(String key, float defValue) {
        Object value = getDecryptedObject(key, Float.class.getName());
        return (value != null && value instanceof Float ? (Float) value : defValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        Object value = getDecryptedObject(key, Boolean.class.getName());
        return (value != null && value instanceof Boolean ? (Boolean) value : defValue);
    }

    @Override
    public boolean contains(String key) {
        return mSharedPreferences.contains(key);
    }

    @Override
    public Editor edit() {
        if (mEditor == null) {
            mEditor = mSharedPreferences.edit();
        }
        return mEditor;
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(
            OnSharedPreferenceChangeListener listener) {
        mListeners.add(listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(
            OnSharedPreferenceChangeListener listener) {
        mListeners.remove(listener);
    }

    private void notifyListeners(String key) {
        for (OnSharedPreferenceChangeListener listener : mListeners) {
            if (listener != null) {
                listener.onSharedPreferenceChanged(this, key);
            }
        }
    }

    private Object getDecryptedObject(final String key, String type) {
        Object returnValue = null;
        final JSONObject returnValueWrapper = new JSONObject();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        String encryptedValue = mSharedPreferences.getString(key, null);
        String ivBase64 = mSharedPreferences.getString(key + IV_KEY, null);
        if (encryptedValue != null && ivBase64 != null) {
            byte[] cipherText = Base64.decode(encryptedValue, Base64.DEFAULT);
            byte[] iv = Base64.decode(ivBase64, Base64.DEFAULT);
            SecureCipher secureCipher = SecureCipher.getInstance(mSecureConfig);
            secureCipher.decrypt(mKeyAlias, cipherText, iv,
                    new SecureCipher.SecureDecryptionListener() {
                        @Override
                        public void decryptionComplete(@NonNull byte[] clearText) {
                            String jsonString = Base64.encodeToString(clearText, Base64.DEFAULT);
                            try {
                                returnValueWrapper.put("json", jsonString);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            countDownLatch.countDown();
                        }
                    });
        }
        try {
            countDownLatch.await();

            JSONObject jsonObject = new JSONObject(returnValueWrapper.getString("json"));
            String jsonKey = jsonObject.getString("key");
            String jsonType = jsonObject.getString("type");
            if (key != null && key.equals(jsonKey)
                    && (type == null || (type != null && type.equals(jsonType)))) {
                returnValue = returnValueWrapper.get("value");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnValue;
    }

}
