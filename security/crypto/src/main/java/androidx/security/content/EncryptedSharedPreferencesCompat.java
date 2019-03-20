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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.security.crypto.EncryptedSharedPreferencesKeysets;
import androidx.security.crypto.MasterKey;
import androidx.security.crypto.TinkCipher;

import com.google.crypto.tink.subtle.Base64;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A class that provides an encrypted implementation for all SharedPreferences data.
 *
 */
public class EncryptedSharedPreferencesCompat implements SharedPreferences,
        SharedPreferences.Editor {

    private static final String TAG = "EncryptedSharedPrefs";
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    public static final String KEY_FILE_NAME =
            "_androidx_security_encrypted_shared_prefs_key_file_";
    public static final String VALUE_FILE_NAME =
            "_androidx_security_encrypted_shared_prefs_value_file_";
    public static final String KEY_KEYSET_ALIAS =
            "_androidx_security_encrypted_key_keyset_";
    public static final String VALUE_KEYSET_ALIAS =
            "_androidx_security_encrypted_value_keyset_";

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private Context mContext;
    private List<OnSharedPreferenceChangeListener> mListeners;
    private List<String> mKeysChanged;
    private String mFileName;
    private int mMode;

    MasterKey mMasterKey;

    private TinkCipher mTinkCipher;

    String mKeyFileName;
    String mValueFileName;
    String mKeyKeysetAlias;
    String mValueKeysetAlias;
    KeyEncryptionScheme mKeyEncryptionScheme;
    ValueEncryptionScheme mValueEncryptionScheme;

    private EncryptedSharedPreferencesKeysets mSharedPreferencesKeysets;

    EncryptedSharedPreferencesCompat(@NonNull String name, int mode,
                                            @NonNull MasterKey masterKey,
                                            @NonNull Context context,
                                            @NonNull KeyEncryptionScheme keyEncryptionScheme,
                                            @NonNull ValueEncryptionScheme valueEncryptionScheme)
            throws GeneralSecurityException, IOException {
        mContext = context;
        mFileName = name;
        mMode = mode;
        mSharedPreferences = mContext.getSharedPreferences(mFileName, mode);
        mMasterKey = masterKey;
        mSharedPreferencesKeysets = new EncryptedSharedPreferencesKeysets.Builder(
                context, masterKey, keyEncryptionScheme, valueEncryptionScheme)
                .build();
        mListeners = new ArrayList<>();
        mKeysChanged = new ArrayList<>();
        mTinkCipher = TinkCipher.getSharedPreferencesCipher(mMasterKey, mContext,
                mSharedPreferencesKeysets);
    }

    /**
     * Opens an instance of encrypted SharedPreferences
     *
     * @param fileName The name of the file to open; can not contain path separators.
     * @param mode Operating mode.
     * @return The SharedPreferences instance that encrypts all data.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    @NonNull
    public static SharedPreferences getEncryptedSharedPreferences(@NonNull String fileName,
            int mode,
            @NonNull MasterKey masterKey,
            @NonNull Context context,
            @NonNull KeyEncryptionScheme keyEncryptionScheme,
            @NonNull ValueEncryptionScheme valueEncryptionScheme)
            throws GeneralSecurityException, IOException {
        return new EncryptedSharedPreferencesCompat(fileName, mode, masterKey,
                context, keyEncryptionScheme, valueEncryptionScheme);
    }

    /**
     * The encryption scheme to encrypt keys.
     */
    public enum KeyEncryptionScheme {
        /**
         * Uses Deterministic Aead encryption to ensure that the key is encrypted the same way such
         * that the key can be used as a lookup mechanism.
         *
         * For more information please see the Tink documentation:
         *
         * @link com.google.crypto.tink.daead.DeterministicAeadKeyTemplates
         */
        AES256_SIV
    }

    /**
     * The encryption scheme to encrypt values.
     */
    public enum ValueEncryptionScheme {
        /**
         * Uses Aead encryption to ensure that the value is encrypted. The AAD provided is the key.
         *
         * For more information please see the Tink documentation:
         *
         * @link com.google.crypto.tink.aead.AeadKeyTemplates
         */
        AES256_GCM
    }

    /**
     * Builder class to configure EncryptedSharedPreferencesCompat
     */
    public static class Builder {

        public Builder(@NonNull String fileName, int mode,
                @NonNull Context context,
                @NonNull MasterKey masterKey,
                @NonNull KeyEncryptionScheme keyEncryptionScheme,
                @NonNull ValueEncryptionScheme valueEncryptionScheme) {
            mFileName = fileName;
            mMode = mode;
            mKeyEncryptionScheme = keyEncryptionScheme;
            mValueEncryptionScheme = valueEncryptionScheme;
            mContext = context;
            mMasterKey = masterKey;
            mKeyFileName = KEY_FILE_NAME;
            mValueFileName = VALUE_FILE_NAME;
            mKeyKeysetAlias = KEY_KEYSET_ALIAS;
            mValueKeysetAlias = VALUE_KEYSET_ALIAS;
        }

        String mFileName;
        int mMode;
        String mKeyFileName;
        String mValueFileName;
        String mKeyKeysetAlias;
        String mValueKeysetAlias;
        KeyEncryptionScheme mKeyEncryptionScheme;
        ValueEncryptionScheme mValueEncryptionScheme;
        Context mContext;
        MasterKey mMasterKey;

        /**
         * @param fileName The file name of the SharedPreferences instance
         * @return This Builder
         */
        @NonNull
        public Builder setFileName(@NonNull String fileName) {
            mFileName = fileName;
            return this;
        }

        /**
         * @param mode The mode of the file, usually Context.MODE_PRIVATE
         * @return This Builder
         */
        @NonNull
        public Builder setMode(int mode) {
            mMode = mode;
            return this;
        }

        /**
         * @param keyFileName The SharedPreferences file to store the Key Keyset.
         * @return This Builder
         */
        @NonNull
        public Builder setKeyFileName(@NonNull String keyFileName) {
            mKeyFileName = keyFileName;
            return this;
        }

        /**
         * @param valueFileName The SharedPreferences file to store the Value Keyset.
         * @return This Builder
         */
        @NonNull
        public Builder setValueFileName(@NonNull String valueFileName) {
            mValueFileName = valueFileName;
            return this;
        }

        /**
         * @param keyKeysetAlias The alias to store the Keyset in the Key SharedPreferences
         * @return This Builder
         */
        @NonNull
        public Builder setKeyKeysetAlias(@NonNull String keyKeysetAlias) {
            mKeyKeysetAlias = keyKeysetAlias;
            return this;
        }

        /**
         * @param valueKeysetAlias The alias to store the Keyset in the Value SharedPreferences
         *                         Keyset.
         * @return
         */
        @NonNull
        public Builder setValueKeysetAlias(@NonNull String valueKeysetAlias) {
            mValueKeysetAlias = valueKeysetAlias;
            return this;
        }

        /**
         * @param keyEncryptionScheme The encryption scheme to use for the key. Currently there is
         *                            only one option, but this is provided to ensure that it's
         *                            clear which algorithm is used.
         * @return
         */
        @NonNull
        public Builder setKeyEncryptionScheme(@NonNull KeyEncryptionScheme keyEncryptionScheme) {
            mKeyEncryptionScheme = keyEncryptionScheme;
            return this;
        }

        /**
         * @param valueEncryptionScheme The encryption scheme to use for the value. Currently there
         *                              is only one option, but this is provided to ensure that it's
         *                              clear which algorithm is used.
         * @return This Builder
         */
        @NonNull
        public Builder setValueEncryptionScheme(
                @NonNull ValueEncryptionScheme valueEncryptionScheme) {
            mValueEncryptionScheme = valueEncryptionScheme;
            return this;
        }

        /**
         * @param context The context
         * @return This Builder
         */
        @NonNull
        public Builder setContext(@NonNull Context context) {
            mContext = context;
            return this;
        }

        /**
         * @param masterKey The master key
         * @return This Builder
         */
        @NonNull
        public Builder setMasterKey(@NonNull MasterKey masterKey) {
            mMasterKey = masterKey;
            return this;
        }

        /**
         * @return The configured EncryptedSharedPreferences instance
         */
        @NonNull
        public EncryptedSharedPreferencesCompat build()
                throws GeneralSecurityException, IOException {
            EncryptedSharedPreferencesCompat sharedPrefs = new EncryptedSharedPreferencesCompat(
                    mFileName, mMode, mMasterKey, mContext,
                    mKeyEncryptionScheme, mValueEncryptionScheme);
            sharedPrefs.mKeyFileName = mKeyFileName;
            sharedPrefs.mValueFileName = mValueFileName;
            sharedPrefs.mKeyKeysetAlias = mKeyKeysetAlias;
            sharedPrefs.mValueKeysetAlias = mValueKeysetAlias;
            return sharedPrefs;
        }
    }

    // SharedPreferences.Editor methods

    @Override
    @NonNull
    public SharedPreferences.Editor putString(@Nullable String key, @Nullable String value) {
        byte[] stringBytes = value.getBytes(UTF_8);
        int stringByteLength = stringBytes.length;
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + Integer.BYTES + stringByteLength);
        buffer.putInt(EncryptedType.STRING.getId());
        buffer.putInt(stringByteLength);
        buffer.put(stringBytes);
        putEncryptedObject(key, buffer.array());
        return this;
    }

    @Override
    @NonNull
    public SharedPreferences.Editor putStringSet(@Nullable String key,
            @Nullable Set<String> values) {
        List<byte[]> byteValues = new ArrayList<>();
        int totalBytes = values.size() * Integer.BYTES;
        for (String strValue : values) {
            byte[] byteValue = strValue.getBytes(UTF_8);
            byteValues.add(byteValue);
            totalBytes += byteValue.length;
        }
        totalBytes += Integer.BYTES;
        ByteBuffer buffer = ByteBuffer.allocate(totalBytes);
        buffer.putInt(EncryptedType.STRING_SET.getId());
        for (byte[] bytes : byteValues) {
            buffer.putInt(bytes.length);
            buffer.put(bytes);
        }
        putEncryptedObject(key, buffer.array());
        return this;
    }

    @Override
    @NonNull
    public SharedPreferences.Editor putInt(@Nullable String key, int value) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + Integer.BYTES);
        buffer.putInt(EncryptedType.INT.getId());
        buffer.putInt(value);
        putEncryptedObject(key, buffer.array());
        return this;
    }

    @Override
    @NonNull
    public SharedPreferences.Editor putLong(@Nullable String key, long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + Long.BYTES);
        buffer.putInt(EncryptedType.LONG.getId());
        buffer.putLong(value);
        putEncryptedObject(key, buffer.array());
        return this;
    }

    @Override
    @NonNull
    public SharedPreferences.Editor putFloat(@Nullable String key, float value) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + Float.BYTES);
        buffer.putInt(EncryptedType.FLOAT.getId());
        buffer.putFloat(value);
        putEncryptedObject(key, buffer.array());
        return this;
    }

    @Override
    @NonNull
    public SharedPreferences.Editor putBoolean(@Nullable String key, boolean value) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + Integer.BYTES);
        buffer.putInt(EncryptedType.BOOLEAN.getId());
        buffer.putInt(value ? 1 : 0);
        putEncryptedObject(key, buffer.array());
        return this;
    }

    @Override
    @NonNull
    public SharedPreferences.Editor remove(@Nullable String key) {
        mEditor.remove(key);
        mKeysChanged.remove(encryptKey(key));
        return this;
    }

    @Override
    @NonNull
    public SharedPreferences.Editor clear() {
        mEditor.clear();
        mKeysChanged.clear();
        return this;
    }

    @Override
    public boolean commit() {
        try {
            return mEditor.commit();
        } finally {
            notifyListeners();
        }
    }

    @Override
    public void apply() {
        mEditor.apply();
        notifyListeners();
    }

    private void putEncryptedObject(final String key, byte[] value) {
        try {
            String encryptedKey = encryptKey(key);
            mKeysChanged.add(encryptedKey);
            byte[] cipherText = mTinkCipher.encryptAead(mContext, value, key.getBytes(UTF_8),
                    false);
            mEditor.putString(encryptedKey, Base64.encode(cipherText));
        } catch (GeneralSecurityException | IOException ex) {
            Log.e(TAG, "Could not encrypt object. " + ex.getMessage());
        }
    }

    private String encryptKey(String key) {
        String encryptedKey = null;
        try {
            byte[] encryptedKeyBytes = mTinkCipher.encryptAead(mContext, key.getBytes(UTF_8),
                    new byte[0],
                    true);
            encryptedKey = Base64.encode(encryptedKeyBytes);
        } catch (GeneralSecurityException | IOException ex) {
            Log.e(TAG, "Could not encrypt key. " + ex.getMessage());
        }
        return encryptedKey;
    }

    // SharedPreferences methods

    @Override
    @NonNull
    public Map<String, ?> getAll() {
        Map<String, ? super Object> allEntries = new HashMap<>();
        for (Map.Entry<String, ?> entry : mSharedPreferences.getAll().entrySet()) {
            String decryptedKey = decryptKey(entry.getKey());

            allEntries.put(decryptedKey,
                    getDecryptedObject(decryptedKey));
        }
        return allEntries;
    }

    @Nullable
    @Override
    public String getString(@Nullable String key, @Nullable String defValue) {
        Object value = getDecryptedObject(key);
        return (value != null && value instanceof String ? (String) value : defValue);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public Set<String> getStringSet(@Nullable String key, @Nullable Set<String> defValues) {
        Set<String> returnValues = new ArraySet<>();
        Object value = getDecryptedObject(key);
        if (value instanceof Set) {
            returnValues = (Set<String>) value;
        }
        return returnValues.size() > 0 ? returnValues : defValues;
    }

    @Override
    public int getInt(@Nullable String key, int defValue) {
        Object value = getDecryptedObject(key);
        return (value != null && value instanceof Integer ? (Integer) value : defValue);
    }

    @Override
    public long getLong(@Nullable String key, long defValue) {
        Object value = getDecryptedObject(key);
        return (value != null && value instanceof Long ? (Long) value : defValue);
    }

    @Override
    public float getFloat(@Nullable String key, float defValue) {
        Object value = getDecryptedObject(key);
        return (value != null && value instanceof Float ? (Float) value : defValue);
    }

    @Override
    public boolean getBoolean(@Nullable String key, boolean defValue) {
        Object value = getDecryptedObject(key);
        return (value != null && value instanceof Boolean ? (Boolean) value : defValue);
    }

    @Override
    public boolean contains(@Nullable String key) {
        String encryptedKey = encryptKey(key);
        return mSharedPreferences.contains(encryptedKey);
    }

    @Override
    @NonNull
    public Editor edit() {
        if (mEditor == null) {
            mEditor = mSharedPreferences.edit();
        }
        return this;
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(
            @NonNull OnSharedPreferenceChangeListener listener) {
        mListeners.add(listener);

    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(
            @NonNull OnSharedPreferenceChangeListener listener) {
        mListeners.remove(listener);
    }

    private void notifyListeners() {
        for (OnSharedPreferenceChangeListener listener : mListeners) {
            if (listener != null) {
                for (String key : mKeysChanged) {
                    String decryptedKey = decryptKey(key);
                    if (decryptedKey != null) {
                        listener.onSharedPreferenceChanged(this, decryptedKey);
                    }
                }
            }
        }
    }

    private String decryptKey(String encryptedKey) {
        String key = null;
        try {
            byte[] clearText = mTinkCipher.decryptAead(mContext,
                    Base64.decode(encryptedKey, Base64.DEFAULT),
                    new byte[0],
                    true);
            key = new String(clearText, UTF_8);
        } catch (GeneralSecurityException | IOException ex) {
            Log.e(TAG, "Could not decrypt key. " + ex.getMessage());
        }
        return key;
    }


    /**
     * Internal enum to set the type of encrypted data.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private enum EncryptedType {
        STRING(0),
        STRING_SET(1),
        INT(2),
        LONG(3),
        FLOAT(4),
        BOOLEAN(5);

        int mId;

        EncryptedType(int id) {
            mId = id;
        }

        public int getId() {
            return mId;
        }

        public static EncryptedType fromId(int id) {
            switch (id) {
                case 0:
                    return STRING;
                case 1:
                    return STRING_SET;
                case 2:
                    return INT;
                case 3:
                    return LONG;
                case 4:
                    return FLOAT;
                case 5:
                    return BOOLEAN;
            }
            return null;
        }
    }

    private Object getDecryptedObject(final String key) {
        Object returnValue = null;
        try {
            String encryptedKey = encryptKey(key);
            String encryptedValue = mSharedPreferences.getString(encryptedKey, null);
            if (encryptedValue != null) {
                byte[] cipherText = Base64.decode(encryptedValue, Base64.DEFAULT);
                byte[] value = mTinkCipher.decryptAead(mContext, cipherText,
                        key.getBytes(UTF_8), false);
                ByteBuffer buffer = ByteBuffer.allocate(value.length);
                buffer.put(value);
                buffer.position(0);
                int typeId = buffer.getInt();
                EncryptedType type = EncryptedType.fromId(typeId);
                switch(type) {
                    case STRING:
                        int stringLength = buffer.getInt();
                        byte[] stringBytes = new byte[stringLength];
                        buffer.get(stringBytes);
                        returnValue = new String(stringBytes, UTF_8);
                        break;
                    case INT:
                        returnValue = buffer.getInt();
                        break;
                    case LONG:
                        returnValue = buffer.getLong();
                        break;
                    case FLOAT:
                        returnValue = buffer.getFloat();
                        break;
                    case BOOLEAN:
                        returnValue = buffer.getInt() != 0;
                        break;
                    case STRING_SET:
                        Set<String> stringSet = new ArraySet<>();
                        while (buffer.hasRemaining()) {
                            int subStringLength = buffer.getInt();
                            byte[] subStringBytes = new byte[subStringLength];
                            buffer.get(subStringBytes);
                            stringSet.add(new String(subStringBytes, UTF_8));
                        }
                        returnValue = stringSet;
                        break;
                }
            }
        } catch (GeneralSecurityException | IOException ex) {
            Log.e(TAG, "Could not decrypt object. " + ex.getMessage());
        }
        return returnValue;
    }

}
