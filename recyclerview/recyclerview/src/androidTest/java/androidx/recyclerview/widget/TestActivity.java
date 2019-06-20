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

package androidx.recyclerview.widget;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

public class TestActivity extends Activity {
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
    // This is not great but the only way to do this until test runner adds support to not kill
    // activities after tests.
    private static final String TEST_RUNNER =
            MonitoringInstrumentation.class.getCanonicalName() + "$" + MonitoringInstrumentation
                    .ActivityFinisher.class.getSimpleName();
=======
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
    private volatile TestedFrameLayout mContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);

        mContainer = new TestedFrameLayout(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        setContentView(mContainer);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public TestedFrameLayout getContainer() {
        return mContainer;
    }

    @Override
    public void finish() {
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
        if (!mAllowFinish) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            // this is terrible but easy workaround for selective finishing
            for (StackTraceElement element : stackTrace) {

                if (TEST_RUNNER.equals(element.getClassName())) {
                    // don't allow activity finisher to finish this.
                    return;
                }
            }
        }
=======
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
        super.finish();
    }
}
