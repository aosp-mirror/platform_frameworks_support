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


package androidx.security.config;

public enum TrustAnchorOptions {
    USER_SYSTEM(0),
    SYSTEM_ONLY(1),
    USER_ONLY(2),
    LIMITED_SYSTEM(3);

    private final int type;

    TrustAnchorOptions(int type) {
        this.type = type;
    }

    public int getType() {
        return this.type;
    }

    public static TrustAnchorOptions fromId(int id) {
        switch (id) {
            case 0:
                return USER_SYSTEM;
            case 1:
                return SYSTEM_ONLY;
            case 2:
                return USER_ONLY;
            case 3:
                return LIMITED_SYSTEM;
        }
        return USER_SYSTEM;
    }

}
