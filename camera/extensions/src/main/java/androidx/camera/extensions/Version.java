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

package androidx.camera.extensions;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.google.auto.value.AutoValue;

import java.math.BigInteger;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class encapsulating a version with major, minor, patch and description values.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@AutoValue
abstract class Version implements Comparable<Version> {
    private static final Pattern VERSION_STRING_PATTERN =
            Pattern.compile("(\\d+)(?:\\.(\\d+))(?:\\.(\\d+))(?:\\-(.+))?");

    /**
     * Parses a string into a version object.
     *
     * @param versionString string in the format "1.2.3" or "1.2.3-Description"
     *                      (major.minor.patch[-description])
     * @return parsed Version object or <tt>null</tt> if the versionString format invalid.
     */
    public static Version parse(String versionString) {
        if (TextUtils.isEmpty(versionString)) {
            return null;
        }

        Matcher matcher = VERSION_STRING_PATTERN.matcher(versionString);
        if (!matcher.matches()) {
            return null;
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));
        String description = matcher.group(4);
        return create(major, minor, patch, description);
    }

    /**
     * Creates a new instance of Version with the given parameters.
     */
    public static Version create(int major, int minor, int patch, String description) {
        return new AutoValue_Version(major, minor, patch, description);
    }

    /** Prevent subclassing */
    Version() {
    }

    abstract int getMajor();

    abstract int getMinor();

    abstract int getPatch();

    abstract String getDescription();

    @NonNull
    public String toVersionString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getMajor() + "." + getMinor() + "." + getPatch());
        if (!TextUtils.isEmpty(getDescription())) {
            sb.append("-" + getDescription());
        }
        return sb.toString();
    }

    @Override
    public int compareTo(Version other) {
        return createBigInteger(this).compareTo(createBigInteger(other));
    }

    private static BigInteger createBigInteger(Version version) {
        return BigInteger.valueOf(version.getMajor())
                .shiftLeft(32)
                .or(BigInteger.valueOf(version.getMinor()))
                .shiftLeft(32)
                .or(BigInteger.valueOf(version.getPatch()));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Version)) {
            return false;
        }

        Version otherVersionObj = (Version) obj;

        return Objects.equals(getMajor(), otherVersionObj.getMajor())
                && Objects.equals(getMinor(), otherVersionObj.getMinor())
                && Objects.equals(getPatch(), otherVersionObj.getPatch());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMajor(), getMinor(), getPatch());
    }
}
