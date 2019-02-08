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
package androidx.appcompat.widget;

/**
 * In addition to all tinting-related tests done by the base class, this class provides
 * tests specific to {@link AppCompatSeekBar} class.
 */
public class AppCompatSeekBarTest extends
        AppCompatBaseViewTest<AppCompatSeekBarActivity, AppCompatSeekBar> {

    public AppCompatSeekBarTest() {
        super(AppCompatSeekBarActivity.class);
    }

    @Override
    protected boolean hasBackgroundByDefault() {
        return true;
    }
}
