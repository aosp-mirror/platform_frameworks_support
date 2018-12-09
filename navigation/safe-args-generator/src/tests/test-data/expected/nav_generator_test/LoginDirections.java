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

package foo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import androidx.navigation.NavDirections;
import java.lang.Override;

public class LoginDirections {
    @NonNull
    public static ActionDone actionDone() {
        return new ActionDone();
    }

    public static class ActionDone implements NavDirections {
        private Bundle bundle = new Bundle();

        public ActionDone() {
        }

        @Override
        @NonNull
        public Bundle getArguments() {
            return bundle;
        }

        @Override
        public int getActionId() {
            return foo.R.id.action_done;
        }
    }
}