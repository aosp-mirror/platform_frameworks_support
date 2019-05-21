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

package androidx.inspection.agent;

public class AxInstrumentation {

    public static abstract class Event {
        private final String mGroup;

        public Event(String group) {
            mGroup = group;
        }

        public String getGroup() {
            return mGroup;
        }
    }

    public static void report(Event event) {
        reportInner(event);
        System.out.println("!!! Internal implementation AxInstrumentation got an event");
    }

    static void reportInner(Object object) {

    }
}

