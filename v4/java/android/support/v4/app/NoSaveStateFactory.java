/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v4.app;

import android.view.View;

class NoSaveStateFactory {

    interface NoSaveStateFactoryImpl {
        public View getNoSaveStateView(View view);
    }

    static class BaseNoSaveStateFactoryImpl implements NoSaveStateFactoryImpl {

        @Override
        public View getNoSaveStateView(View view) {
            return NoSaveStateFrameLayout.wrap(view);
        }
    }

    static class HCNoSaveStateFactoryImpl implements NoSaveStateFactoryImpl {

        @Override
        public View getNoSaveStateView(View view) {
            return NoSaveStateFactoryHoneycomb.getNoSaveStateView(view);
        }
    }

    private static final NoSaveStateFactoryImpl IMPL;
    static {
        final int version = android.os.Build.VERSION.SDK_INT;
        if (version >= 11) {
            IMPL = new HCNoSaveStateFactoryImpl();
        } else {
            IMPL = new BaseNoSaveStateFactoryImpl();
        }
    }

    public static View getNoSaveStateView(View view) {
        return IMPL.getNoSaveStateView(view);
    }
}
