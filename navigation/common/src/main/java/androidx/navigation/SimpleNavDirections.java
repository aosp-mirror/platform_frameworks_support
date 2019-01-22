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

package androidx.navigation;

import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * A simple implementation of {@link NavDirections} without any arguments.
 */
public class SimpleNavDirections implements NavDirections {

    private final int mActionId;

    public SimpleNavDirections(int actionId) {
        this.mActionId = actionId;
    }

    @Override
    public int getActionId() {
        return mActionId;
    }

    @Nullable
    @Override
    public Bundle getArguments() {
        return new Bundle();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        SimpleNavDirections that = (SimpleNavDirections) object;
        if (getActionId() != that.getActionId()) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + getActionId();
        return result;
    }

    @Override
    public String toString() {
        return "SimpleNavDirections(actionId=" + getActionId() + ")";
    }
}
