/*
 * Copyright (C) 2016 The Android Open Source Project
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
package android.support.v17.leanback.app;

import android.app.Activity;
import android.os.Bundle;
import android.support.v17.leanback.test.R;

/**
 * Test activity containing {@link VideoFragment}.
 */
public class VideoFragmentTestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_fragment_with_controls);
    }

    public void replaceVideoFragment() {
        getFragmentManager().beginTransaction()
                .replace(R.id.video_fragment, new VideoTestFragment())
                .commitAllowingStateLoss();
    }
}
