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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

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
public class EncryptedFile {

    public static final String FILE_NAME = "_androidx_security_encrypted_file_file_";
    public static final String KEYSET_ALIAS = "_androidx_security_encrypted_file_keyset_";

    private Context mContext;

    String mKeyFileName;
    String mFileKeysetAlias;


    MasterKey mMasterKey;
    EncryptedFileKeyset mEncryptedFileKeyset;

    EncryptedFile(
            @NonNull MasterKey masterKey,
            @NonNull FileEncryptionScheme fileEncryptionScheme,
            @NonNull Context context) {
        mContext = context;
        mMasterKey = masterKey;
        mEncryptedFileKeyset = new EncryptedFileKeyset.Builder(mContext, mMasterKey,
                fileEncryptionScheme)
                .build();
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
        AES256_GCM_HKDF_4KB
    }

    /**
     * Builder class to configure EncryptedFile
     */
    public static class Builder {

        public Builder(@NonNull Context context,
                @NonNull MasterKey masterKey,
                @NonNull FileEncryptionScheme fileEncryptionScheme) {
            mKeyFileName = FILE_NAME;
            mFileKeysetAlias = KEYSET_ALIAS;
            mFileEncryptionScheme = fileEncryptionScheme;
            mContext = context;
            mMasterKey = masterKey;
        }

        String mKeyFileName;
        String mFileKeysetAlias;
        FileEncryptionScheme mFileEncryptionScheme;
        Context mContext;
        MasterKey mMasterKey;

        /**
         * @param keyFileName The SharedPreferences file to store the file Keyset.
         * @return This Builder
         */
        @NonNull
        public Builder setKeyFileName(@NonNull String keyFileName) {
            mKeyFileName = keyFileName;
            return this;
        }

        /**
         * @param fileKeysetAlias The alias to store the Keyset in the file SharedPreferences
         * @return This Builder
         */
        @NonNull
        public Builder setFileKeysetAlias(@NonNull String fileKeysetAlias) {
            mFileKeysetAlias = fileKeysetAlias;
            return this;
        }

        /**
         * @param fileEncryptionScheme The encryption scheme to use for the files. Currently there
         *                              is only one option, but this is provided to ensure that it's
         *                              clear which algorithm is used.
         * @return This Builder
         */
        @NonNull
        public Builder setFileEncryptionScheme(
                @NonNull FileEncryptionScheme fileEncryptionScheme) {
            mFileEncryptionScheme = fileEncryptionScheme;
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
         * @return The configured Key information
         */
        @NonNull
        public EncryptedFile build() throws GeneralSecurityException, IOException {
            EncryptedFile file = new EncryptedFile(mMasterKey, mFileEncryptionScheme, mContext);
            file.mKeyFileName = mKeyFileName;
            file.mFileKeysetAlias = mFileKeysetAlias;
            return file;
        }
    }

    /**
     * Opens a FileOutputStream for writing that automatically encrypts the data based on the
     * provided settings.
     *
     * @param fileName The name of the file to open; can not contain path separators.
     * @param mode Operating mode.
     * @return The FileOutputStream that encrypts all data.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    @NonNull
    public FileOutputStream openEncryptedFileOutput(@NonNull String fileName, int mode)
            throws GeneralSecurityException, IOException {
        return new EncryptedFile(mMasterKey,
                FileEncryptionScheme.AES256_GCM_HKDF_4KB,
                mContext)
                .getFileOutputStream(fileName, mode);
    }

    /**
     * Opens a FileInputStream that reads encrypted files based on the previous settings.
     *
     * Please ensure that the same MasterKey and EncryptedFileKeyset are  used to decrypt or it
     * will cause failures.
     *
     * @param fileName The name of the file to open; can not contain path separators.
     * @return The input stream to read previously encrypted data.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    @NonNull
    public FileInputStream openEncryptedFileInput(@NonNull String fileName)
            throws GeneralSecurityException, IOException {
        return new EncryptedFile(mMasterKey,
                FileEncryptionScheme.AES256_GCM_HKDF_4KB,
                mContext).getFileInputStream(fileName);
    }

    /**
     * @return  the file output stream
     */
    @NonNull
    private FileOutputStream getFileOutputStream(@NonNull String fileName, int mode)
            throws GeneralSecurityException, IOException {
        return new EncryptedFileOutputStream(fileName,
                mContext.openFileOutput(fileName, mode), mContext);
    }

    /**
     * @return the file input stream
     */
    @NonNull
    private FileInputStream getFileInputStream(@NonNull String fileName)
            throws GeneralSecurityException, IOException {
        return new EncryptedFileInputStream(fileName,
                mContext.openFileInput(fileName), mContext);
    }

    /**
     * Encrypted file output stream
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    class EncryptedFileOutputStream extends FileOutputStream {

        private static final String TAG = "EncryptedFOS";

        private FileOutputStream mFileOutputStream;
        private String mFileName;
        private OutputStream mEncryptedOutputStream;
        private Context mContext;

        EncryptedFileOutputStream(String fileName,
                FileOutputStream fileOutputStream, Context context)
                throws GeneralSecurityException, IOException {
            super(fileOutputStream.getFD());
            mFileOutputStream = fileOutputStream;
            mFileName = fileName;
            mContext = context;
            setupEncryptedStream();
        }

        private void setupEncryptedStream() throws GeneralSecurityException, IOException {
            TinkCipher tinkCipher = TinkCipher.getFileCipher(mMasterKey, mContext,
                    mEncryptedFileKeyset);
            mEncryptedOutputStream = tinkCipher.createEncryptedStream(mFileOutputStream,
                    mFileName);
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

        @Override
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

        private FileInputStream mFileInputStream;
        private String mFileName;
        private InputStream mEncryptedInputStream;
        private Context mContext;

        EncryptedFileInputStream(String fileName,
                FileInputStream fileInputStream, Context context)
                throws GeneralSecurityException, IOException {
            super(fileInputStream.getFD());
            mFileInputStream = fileInputStream;
            mFileName = fileName;
            mContext = context;
            setupDecryptedStream();
        }

        private void setupDecryptedStream() throws  GeneralSecurityException, IOException {
            TinkCipher tinkCipher = TinkCipher.getFileCipher(mMasterKey, mContext,
                    mEncryptedFileKeyset);
            mEncryptedInputStream = tinkCipher.createDecryptionStream(mFileInputStream,
                    mFileName);
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

        @Override
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
