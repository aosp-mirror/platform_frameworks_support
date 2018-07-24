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

import android.content.Context;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.reflect.InvocationTargetException;

/**
 * A Spark is a loaded instance of the library.
 */
public class Spark {

    /**
     * Constant indicating that this spark implementation is provided from within this APK.
     */
    public static final int SOURCE_LOCAL = 0;
    /**
     * Constant indicating that this spark implementation was loaded from another APK.
     */
    public static final int SOURCE_LOADED = 1;

    @IntDef({SOURCE_LOCAL, SOURCE_LOADED})
    public @interface SourceType {

    }

    private final Context mContext;
    private final ClassLoader mLoader;
    private final String mVersion;
    private final int mSource;

    Spark(@NonNull Context context, @NonNull ClassLoader loader, @NonNull String version,
            @SourceType int source) {
        mContext = context;
        mLoader = loader;
        mVersion = version;
        mSource = source;
    }

    /**
     * Returns the context that should be used when communicating with this Spark implementation.
     */
    @NonNull
    public Context getContext() {
        return mContext;
    }

    /**
     * Gets the classloader that can provider classes within this loaded spark.
     */
    public @NonNull ClassLoader getClassLoader() {
        return mLoader;
    }

    /**
     * Gets the version of the implemantation of this Spark.
     */
    public @NonNull String getVersion() {
        return mVersion;
    }

    /**
     * Gets where this spark implementation has been loaded from.
     */
    public @SourceType int getSourceType() {
        return mSource;
    }

    /**
     * Creates an instance of a class from the classloader for this Spark.
     */
    @SuppressWarnings("TypeParameterUnusedInFormals")
    public <T> T load(String cls) throws SparkException {
        try {
            return (T) mLoader.loadClass(cls).getConstructor().newInstance();
        } catch (InstantiationException e) {
            throw new SparkException("Can't instantiate", e);
        } catch (IllegalAccessException e) {
            throw new SparkException("No access", e);
        } catch (InvocationTargetException e) {
            throw new SparkException("Implementation exception", e);
        } catch (NoSuchMethodException e) {
            throw new SparkException(cls + " is missing default constructor", e);
        } catch (ClassNotFoundException e) {
            throw new SparkException(cls + " does not exist", e);
        }
    }
}
