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

package androidx.security.crypto;

import static androidx.security.crypto.MasterKey.KEYSTORE_PATH_URI;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.StreamingAead;
import com.google.crypto.tink.config.TinkConfig;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.crypto.tink.proto.KeyTemplate;
import com.google.crypto.tink.streamingaead.StreamingAeadFactory;
import com.google.crypto.tink.streamingaead.StreamingAeadKeyTemplates;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.security.GeneralSecurityException;

/**
 * Class used to create and read encrypted files. Provides implementations
 * of EncryptedFileInput/Output Streams.
 */
public final class EncryptedFile {

    private static final String KEYSET_PREF_NAME =
            "__androidx_security_crypto_encrypted_file_pref__";
    private static final String KEYSET_ALIAS =
            "__androidx_security_crypto_encrypted_file_keyset__";

    final File mFile;
    final Context mContext;
    final String mKeysetPrefName;
    final String mKeysetAlias;
    final String mMasterKeyAlias;
    final StreamingAead mStreamingAead;


    EncryptedFile(
            @NonNull File file,
            @NonNull String masterKeyAlias,
            @NonNull StreamingAead streamingAead,
            @NonNull Context context,
            @NonNull String keysetPrefName,
            @NonNull String keysetAlias) {
        mFile = file;
        mContext = context;
        mMasterKeyAlias = masterKeyAlias;
        mKeysetPrefName = keysetPrefName;
        mKeysetAlias = keysetAlias;
        mStreamingAead = streamingAead;
    }

    /**
     * The encryption scheme to encrypt files.
     */
    public enum FileEncryptionScheme {
        /**
         * Uses Streaming Aead encryption to ensure that the value is encrypted. The AAD provided
         * is the file name.
         *
         * For more information please see the Tink documentation:
         *
         * @link com.google.crypto.tink.streamingaead.StreamingAeadKeyTemplates
         */
        AES256_GCM_HKDF_4KB(StreamingAeadKeyTemplates.AES256_GCM_HKDF_4KB);

        KeyTemplate mStreamingAeadKeyTemplate;

        FileEncryptionScheme(KeyTemplate keyTemplate) {
            mStreamingAeadKeyTemplate = keyTemplate;
        }

        KeyTemplate getKeyTemplate() {
            return mStreamingAeadKeyTemplate;
        }
    }

    /**
     * Builder class to configure EncryptedFile
     */
    public static final class Builder {

        public Builder(@NonNull File file,
                @NonNull Context context,
                @NonNull String masterKeyAlias,
                @NonNull FileEncryptionScheme fileEncryptionScheme) {
            mFile = file;
            mFileEncryptionScheme = fileEncryptionScheme;
            mContext = context;
            mMasterKeyAlias = masterKeyAlias;
        }

        File mFile;
        String mKeysetPrefName = KEYSET_PREF_NAME;
        String mKeysetAlias = KEYSET_ALIAS;
        final FileEncryptionScheme mFileEncryptionScheme;
        final Context mContext;
        final String mMasterKeyAlias;

        /**
         * @param keysetPrefName The SharedPreferences file to store the file Keyset.
         * @return This Builder
         */
        @NonNull
        public Builder setKeysetPrefName(@NonNull String keysetPrefName) {
            mKeysetPrefName = keysetPrefName;
            return this;
        }

        /**
         * @param keysetAlias The alias to store the Keyset in the file SharedPreferences
         * @return This Builder
         */
        @NonNull
        public Builder setKeysetAlias(@NonNull String keysetAlias) {
            mKeysetAlias = keysetAlias;
            return this;
        }

        /**
         * @return The configured Key information
         */
        @NonNull
        public EncryptedFile build() throws GeneralSecurityException, IOException {
            TinkConfig.register();

            KeysetHandle streadmingAeadKeysetHandle = new AndroidKeysetManager.Builder()
                    .withKeyTemplate(mFileEncryptionScheme.getKeyTemplate())
                    .withSharedPref(mContext, KEYSET_ALIAS, KEYSET_PREF_NAME)
                    .withMasterKeyUri(KEYSTORE_PATH_URI + mKeysetAlias)
                    .build().getKeysetHandle();

            StreamingAead streamingAead = StreamingAeadFactory.getPrimitive(
                    streadmingAeadKeysetHandle);

            EncryptedFile file = new EncryptedFile(mFile, mKeysetAlias, streamingAead,
                    mContext, mKeysetPrefName, mKeysetAlias);
            return file;
        }
    }

    /**
     * Opens a FileOutputStream for writing that automatically encrypts the data based on the
     * provided settings.
     *
     * @return The FileOutputStream that encrypts all data.
     * @throws GeneralSecurityException when a bad master key or keyset has been used
     * @throws IOException when the file was not available
     */
    @NonNull
    public FileOutputStream openFileOutput()
            throws GeneralSecurityException, IOException {
        if (mFile.exists()) {
            throw new IOException("output file already exists, please use a new file: "
                    + mFile.getName());
        }
        FileOutputStream fileOutputStream = new FileOutputStream(mFile);
        OutputStream encryptingStream = mStreamingAead.newEncryptingStream(fileOutputStream,
                mFile.getName().getBytes(UTF_8));
        return new EncryptedFileOutputStream(fileOutputStream.getFD(), encryptingStream);
    }

    /**
     * Opens a FileInputStream that reads encrypted files based on the previous settings.
     *
     * Please ensure that the same MasterKey and EncryptedFileKeyset are  used to decrypt or it
     * will cause failures.
     *
     * @return The input stream to read previously encrypted data.
     * @throws GeneralSecurityException when a bad master key or keyset has been used
     * @throws IOException when the file was not found
     */
    @NonNull
    public FileInputStream openFileInput()
            throws GeneralSecurityException, IOException {
        if (!mFile.exists()) {
            throw new IOException("file doesn't exist: " + mFile.getName());
        }
        FileInputStream fileInputStream = new FileInputStream(mFile);
        InputStream encryptingStream = mStreamingAead.newDecryptingStream(fileInputStream,
                mFile.getName().getBytes(UTF_8));
        return new EncryptedFileInputStream(fileInputStream.getFD(), encryptingStream);
    }

    /**
     * Encrypted file output stream
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    class EncryptedFileOutputStream extends FileOutputStream {

        private static final String TAG = "EncryptedFOS";

        private final OutputStream mEncryptedOutputStream;

        EncryptedFileOutputStream(FileDescriptor descriptor, OutputStream encryptedOutputStream) {
            super(descriptor);
            mEncryptedOutputStream = encryptedOutputStream;
        }

        @Override
        public void write(@NonNull byte[] b) throws IOException {
            mEncryptedOutputStream.write(b);
        }

        @Override
        public void write(int b) throws IOException {
            mEncryptedOutputStream.write(b);
        }

        @Override
        public void write(@NonNull byte[] b, int off, int len) throws IOException {
            mEncryptedOutputStream.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            mEncryptedOutputStream.close();
        }

        @NonNull
        @Override
        public FileChannel getChannel() {
            throw new UnsupportedOperationException("For encrypted files, please open the "
                    + "relevant FileInput/FileOutputStream.");
        }

        protected void finalize() throws IOException {
            super.finalize();
        }

        @Override
        public void flush() throws IOException {
            mEncryptedOutputStream.flush();
        }

    }

    /**
     * Encrypted file input stream
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    class EncryptedFileInputStream extends FileInputStream {

        // Was 25 characters, truncating to fix compile error
        private static final String TAG = "EncryptedFIS";

        private final InputStream mEncryptedInputStream;

        EncryptedFileInputStream(FileDescriptor descriptor,
                InputStream encryptedInputStream) {
            super(descriptor);
            mEncryptedInputStream = encryptedInputStream;
        }

        @Override
        public int read() throws IOException {
            return mEncryptedInputStream.read();
        }

        @Override
        public int read(@NonNull byte[] b) throws IOException {
            return mEncryptedInputStream.read(b);
        }

        @Override
        public int read(@NonNull byte[] b, int off, int len) throws IOException {
            return mEncryptedInputStream.read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            return mEncryptedInputStream.skip(n);
        }

        @Override
        public int available() throws IOException {
            return mEncryptedInputStream.available();
        }

        @Override
        public void close() throws IOException {
            mEncryptedInputStream.close();
        }

        @Override
        public FileChannel getChannel() {
            throw new UnsupportedOperationException("For encrypted files, please open the "
                    + "relevant FileInput/FileOutputStream.");
        }

        protected void finalize() throws IOException {
            super.finalize();
        }

        @Override
        public synchronized void mark(int readlimit) {
            mEncryptedInputStream.mark(readlimit);
        }

        @Override
        public synchronized void reset() throws IOException {
            mEncryptedInputStream.reset();
        }

        @Override
        public boolean markSupported() {
            return mEncryptedInputStream.markSupported();
        }

    }

}
