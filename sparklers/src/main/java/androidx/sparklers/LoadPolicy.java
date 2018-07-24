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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.XmlRes;
import androidx.core.util.Preconditions;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings for how to select where to load implementations from.
 *
 * Local refers to a version included within the app, load refers to
 * loading the implementation from a separate APK.
 */
public final class LoadPolicy {

    private static final String TAG = "LoadPolicy";

    /**
     * When both versions are equivalent, use the local implementation.
     */
    public static final int PREFER_LOCAL = 0;
    /**
     * When both versions are equivalent, use the loadable implementation.
     */
    public static final int PREFER_LOAD = 1;
    /**
     * Only load the local implementation.
     */
    public static final int ONLY_LOCAL = 2;
    /**
     * Only load the loadable implementation.
     */
    public static final int ONLY_LOAD = 3;

    private static final String XML_TAG_LOAD_POLICY = "LoadPolicy";
    private static final String XML_TAG_DISABLE_VERSION = "DisableVersion";

    @IntDef({PREFER_LOCAL, PREFER_LOAD, ONLY_LOCAL, ONLY_LOAD})
    public @interface LoadMode {
    }

    private final String mReleaseSha1;
    private final String mDebugSha1;
    private final List<String> mDisabled;
    private final String mLibrary;
    private final String mLocalVersion;
    private final int mMode;
    private final String mMinVersion;

    LoadPolicy(String library, List<String> disabled, String version, int mode,
            String minVersion, String sha1, String debugSha1) {
        mLibrary = library;
        mDisabled = disabled;
        mLocalVersion = version;
        mMode = mode;
        mMinVersion = minVersion;
        mReleaseSha1 = sha1;
        mDebugSha1 = debugSha1;
    }

    public String getReleaseSha1() {
        return mReleaseSha1;
    }

    public String getDebugSha1() {
        return mDebugSha1;
    }

    public String getLibrary() {
        return mLibrary;
    }

    public List<String> getDisabledVersions() {
        return mDisabled;
    }

    public String getLocalVersion() {
        return mLocalVersion;
    }

    @LoadMode
    public int getLoadMode() {
        return mMode;
    }

    public String getMinVersion() {
        return mMinVersion;
    }

    /**
     * Read a policy from an XML resource.
     */
    public static LoadPolicy readLoadPolicy(Context context, @XmlRes int xmlResource)
            throws SparkException {
        Resources res = context.getResources();
        XmlResourceParser parser = res.getXml(xmlResource);
        try {
            AttributeSet attrs = Xml.asAttributeSet(parser);
            int type;
            // Traverse to the first start tag
            while ((type = parser.next()) != XmlResourceParser.END_DOCUMENT
                    && type != XmlResourceParser.START_TAG) {
                // Nothing to do here.
            }

            if (!XML_TAG_LOAD_POLICY.equals(parser.getName())) {
                throw new SparkException("Meta-data does not start with loadPolicy tag", null);
            }
            TypedArray policy = res.obtainAttributes(attrs, R.styleable.loadPolicy);
            String library = policy.getString(R.styleable.loadPolicy_library);
            if (library == null) {
                throw new SparkException("The library must be specified", null);
            }
            LoadPolicy.Builder builder = new LoadPolicy.Builder(library);
            String version = policy.getString(R.styleable.loadPolicy_localVersion);
            if (version != null) {
                builder.setLocalVersion(version);
            }
            String minVersion = policy.getString(R.styleable.loadPolicy_minVersion);
            if (minVersion != null) {
                builder.setMinVersion(minVersion);
            }
            builder.setLoadMode(policy.getInt(R.styleable.loadPolicy_loadMode, PREFER_LOCAL));
            String debugSha1 = policy.getString(R.styleable.loadPolicy_debugSha1);
            if (debugSha1 != null) {
                builder.setDebugSha1(debugSha1);
            }
            String releaseSha1 = policy.getString(R.styleable.loadPolicy_releaseSha1);
            if (releaseSha1 != null) {
                builder.setReleaseSha1(releaseSha1);
            }

            int outerDepth = parser.getDepth();
            while ((type = parser.next()) != XmlResourceParser.END_DOCUMENT
                    && (type != XmlResourceParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlResourceParser.END_TAG) {
                    continue;
                }
                if (XML_TAG_DISABLE_VERSION.equals(parser.getName())) {
                    TypedArray array = res.obtainAttributes(attrs,
                            R.styleable.loadPolicy_disableVersion);
                    String disabled = array.getString(R.styleable.loadPolicy_disableVersion_value);
                    if (disabled != null) {
                        builder.disableVersion(disabled);
                    } else {
                        Log.w(TAG, "disableVersion must have the versioned specified");
                    }
                }
            }
            return builder.build();
        } catch (XmlPullParserException e) {
            throw new SparkException("Malformed XML", e);
        } catch (IOException e) {
            throw new SparkException("Can't read policy", e);
        } finally {
            parser.close();
        }
    }

    /**
     * Builder for {@link LoadPolicy}.
     */
    public static class Builder {
        private final List<String> mDisabled = new ArrayList<>();
        private final String mLibrary;

        private String mVersion;
        private int mMode = PREFER_LOAD;
        private String mMinVersion;
        private String mSha1;
        private String mDebugSha1;

        /**
         * @param library The package of the library being loaded.
         */
        public Builder(@NonNull String library) {
            Preconditions.checkNotNull(library);
            mLibrary = library;
        }

        /**
         * Set a version available in the current classpath.
         */
        public Builder setLocalVersion(@NonNull String version) {
            if (mDisabled.contains(version)) {
                Log.d(TAG, "Local version is in blacklist, disabling local");
                version = null;
            }
            mVersion = version;
            return this;
        }

        /**
         * Set SHA1 of signing cert of library APKs for release builds.
         */
        public Builder setReleaseSha1(String sha1) {
            mSha1 = sha1;
            return this;
        }

        /**
         * Set SHA1 of signing cert of library APKs for debug builds.
         */
        public Builder setDebugSha1(String sha1) {
            mDebugSha1 = sha1;
            return this;
        }

        /**
         * Set the preferences around what is loaded.
         */
        public Builder setLoadMode(@LoadMode int mode) {
            mMode = mode;
            return this;
        }

        /**
         * Set the minimum version required for the implementation.
         */
        public Builder setMinVersion(@NonNull String minVersion) {
            mMinVersion = minVersion;
            return this;
        }

        /**
         * Blacklist a specific version from being loaded.
         */
        public Builder disableVersion(@NonNull String version) {
            if (version.equals(mVersion)) {
                Log.d(TAG, "Local version is in blacklist, disabling local");
                mVersion = null;
            }
            mDisabled.add(version);
            return this;
        }

        /**
         * Build the policy.
         */
        public LoadPolicy build() {
            return new LoadPolicy(mLibrary, mDisabled, mVersion, mMode, mMinVersion,
                    mSha1, mDebugSha1);
        }
    }

}
