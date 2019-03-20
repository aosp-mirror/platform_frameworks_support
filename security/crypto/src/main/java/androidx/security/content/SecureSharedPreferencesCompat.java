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

import android.content.Context;
import android.content.SharedPreferences;
import android.util.ArraySet;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.security.SecureConfig;
import androidx.security.crypto.SecureCipher;

import com.google.crypto.tink.subtle.Base64;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private String mKeyAlias;
    private SecureConfig mSecureConfig;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private Context mContext;
    private List<OnSharedPreferenceChangeListener> mListeners;

    public SecureSharedPreferencesCompat(@NonNull String keyAlias,
            @NonNull SharedPreferences sharedPreferences, Context context) {
        this(keyAlias, sharedPreferences, SecureConfig.getDefault(), context);
    }

    public SecureSharedPreferencesCompat(@NonNull String keyAlias,
            @NonNull SharedPreferences sharedPreferences,
            @NonNull SecureConfig secureConfig, Context context) {
        mKeyAlias = keyAlias;
        mSharedPreferences = sharedPreferences;
        mSecureConfig = secureConfig;
        mListeners = new ArrayList<>();
        mContext = context;
    }

    // SharedPreferences.Editor methods

    @Override
    public SharedPreferences.Editor putString(String key, @Nullable String value) {
        putEncryptedObject(key, String.class.getSimpleName(), value.getBytes(UTF_8));
        return this;
    }

    @Override
    public SharedPreferences.Editor putStringSet(String key, @Nullable Set<String> values) {
        List<byte[]> byteValues = new ArrayList<>();
        int totalBytes = values.size() * Integer.BYTES;
        for (String strValue : values) {
            byte[] byteValue = strValue.getBytes(UTF_8);
            byteValues.add(byteValue);
            totalBytes += byteValue.length;
        }
        ByteBuffer buffer = ByteBuffer.allocate(totalBytes);
        for (byte[] bytes : byteValues) {
            buffer.putInt(bytes.length);
            buffer.put(bytes);
        }
        putEncryptedObject(key, Set.class.getSimpleName(), buffer.array());
        return this;
    }

    @Override
    public SharedPreferences.Editor putInt(String key, int value) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(value);
        putEncryptedObject(key, Integer.class.getSimpleName(), buffer.array());
        return this;
    }

    @Override
    public SharedPreferences.Editor putLong(String key, long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(value);
        putEncryptedObject(key, Long.class.getSimpleName(), buffer.array());
        return this;
    }

    @Override
    public SharedPreferences.Editor putFloat(String key, float value) {
        ByteBuffer buffer = ByteBuffer.allocate(Float.BYTES);
        buffer.putFloat(value);
        putEncryptedObject(key, Float.class.getSimpleName(), buffer.array());
        return this;
    }

    @Override
    public SharedPreferences.Editor putBoolean(String key, boolean value) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(value ? 1 : 0);
        putEncryptedObject(key, Boolean.class.getSimpleName(), buffer.array());
        return this;
    }

    @Override
    public SharedPreferences.Editor remove(String key) {
        mEditor.remove(key);
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

    private void putEncryptedObject(final String key, String type, byte[] value) {
        SecureCipher secureCipher = SecureCipher.getInstance(mSecureConfig, mContext);
        String encryptedKey = encryptKey(key, type);
        byte[] cipherText = secureCipher.encryptAead(mContext, value, key.getBytes(UTF_8),
                false);
        mEditor.putString(encryptedKey, Base64.encode(cipherText));
        notifyListeners(key);
    }

    private String encryptKey(String key, String type) {
        String encryptedKey = null;
        SecureCipher secureCipher = SecureCipher.getInstance(mSecureConfig, mContext);
        byte[] encryptedKeyBytes = secureCipher.encryptAead(mContext, key.getBytes(UTF_8),
                type.getBytes(UTF_8),
                true);
        encryptedKey = Base64.encode(encryptedKeyBytes) + ":" + type;
        return encryptedKey;
    }

    // SharedPreferences methods

    @Override
    public Map<String, ?> getAll() {
        Map<String, ? super Object> allEntries = new HashMap<>();
        for (Map.Entry<String, ?> entry : mSharedPreferences.getAll().entrySet()) {
            Pair<String, String> keyType = getKeyType(entry.getKey());
            String encryptedKey = keyType.first;
            String type = keyType.second;
            String decryptedKey = decryptKey(encryptedKey, type);
            allEntries.put(decryptedKey,
                    getDecryptedObject(decryptedKey, type));
        }
        return allEntries;
    }

    private Pair<String, String> getKeyType(String encryptedKey) {
        String key = null;
        String type = null;
        int typeIndex = encryptedKey.lastIndexOf(":");
        if (typeIndex > 0) {
            key = encryptedKey.substring(0, typeIndex);
            type = encryptedKey.substring(typeIndex + 1);
        }
        return new Pair<>(key, type);
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        Object value = getDecryptedObject(key, String.class.getSimpleName());
        return (value != null && value instanceof String ? (String) value : defValue);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        Set<String> returnValues = new ArraySet<>();
        Object value = getDecryptedObject(key, Set.class.getSimpleName());
        if (value instanceof Set) {
            returnValues = (Set<String>) value;
        }
        return returnValues.size() > 0 ? returnValues : defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        Object value = getDecryptedObject(key, Integer.class.getSimpleName());
        return (value != null && value instanceof Integer ? (Integer) value : defValue);
    }

    @Override
    public long getLong(String key, long defValue) {
        Object value = getDecryptedObject(key, Long.class.getSimpleName());
        return (value != null && value instanceof Long ? (Long) value : defValue);
    }

    @Override
    public float getFloat(String key, float defValue) {
        Object value = getDecryptedObject(key, Float.class.getSimpleName());
        return (value != null && value instanceof Float ? (Float) value : defValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        Object value = getDecryptedObject(key, Boolean.class.getSimpleName());
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
        return this;
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

    private String decryptKey(String encryptedKey, String type) {
        String key = null;
        SecureCipher secureCipher = SecureCipher.getInstance(mSecureConfig, mContext);
        byte[] clearText = secureCipher.decryptAead(mContext, Base64.decode(encryptedKey),
                type.getBytes(UTF_8),
                true);
        key = new String(clearText, UTF_8);
        return key;
    }

    private Object getDecryptedObject(final String key, String type) {
        Object returnValue = null;
        String encryptedKey = encryptKey(key, type);
        String encryptedValue = mSharedPreferences.getString(encryptedKey, null);
        if (encryptedValue != null) {
            byte[] cipherText = Base64.decode(encryptedValue, Base64.DEFAULT);
            SecureCipher secureCipher = SecureCipher.getInstance(mSecureConfig, mContext);
            byte[] value = secureCipher.decryptAead(mContext, cipherText,
                    key.getBytes(UTF_8), false);
            switch(type) {
                case "String":
                    returnValue = new String(value, UTF_8);
                    break;
                case "Integer":
                    ByteBuffer intBuffer = ByteBuffer.allocate(Integer.BYTES);
                    intBuffer.put(value);
                    returnValue = intBuffer.getInt();
                    break;
                case "Long":
                    ByteBuffer longBuffer = ByteBuffer.allocate(Long.BYTES);
                    longBuffer.put(value);
                    returnValue = longBuffer.getLong();
                    break;
                case "Float":
                    ByteBuffer floatBuffer = ByteBuffer.allocate(Float.BYTES);
                    floatBuffer.put(value);
                    returnValue = floatBuffer.getFloat();
                    break;
                case "Boolean":
                    ByteBuffer booleanBuffer = ByteBuffer.allocate(Integer.BYTES);
                    booleanBuffer.put(value);
                    returnValue = booleanBuffer.getInt() != 0;
                    break;
                case "Set":
                    ByteBuffer setBuffer = ByteBuffer.allocate(value.length);
                    setBuffer.put(value);
                    Set<String> stringSet = new ArraySet<>();
                    while (setBuffer.hasRemaining()) {
                        int stringLength = setBuffer.getInt();
                        byte[] stringBytes = new byte[stringLength];
                        setBuffer.get(stringBytes);
                        stringSet.add(new String(stringBytes, UTF_8));
                    }
                    returnValue = stringSet;
                    break;
            }
        }
        return returnValue;
    }

}
