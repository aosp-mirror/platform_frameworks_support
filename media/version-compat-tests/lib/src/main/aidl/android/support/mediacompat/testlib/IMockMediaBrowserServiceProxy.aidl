/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (String controllerId, the "License");
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

package android.support.mediacompat.testlib;

interface IMockMediaBrowserServiceProxy {
    void cleanUp();

    void overrideOnGetRoot(String givenPackageName, int givenUid, in Bundle givenRootHints,
            String resultRootId, in Bundle resultRootExtras);
    void overrideOnLoadChildren(String givenParentId, in List<Bundle> resultList);
    void overrideOnLoadChildrenWithOptions(String givenParentId, in Bundle givenOptions,
            in List<Bundle> resultList);
    void overrideOnLoadItem(String givenItemId, in Bundle resultItem);
    void overrideOnSearch(String givenQuery, in Bundle givenExtras, in List<Bundle> resultList);
}
