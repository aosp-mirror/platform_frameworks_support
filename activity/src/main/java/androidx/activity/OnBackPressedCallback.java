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

package androidx.activity;

import androidx.lifecycle.LifecycleOwner;

<<<<<<< HEAD   (eaf9a7 Merge "Merge empty history for sparse-5522318-L8750000030582)
=======
import java.util.concurrent.CopyOnWriteArrayList;

>>>>>>> BRANCH (736a74 Merge "Merge cherrypicks of [955240] into sparse-5522907-L19)
/**
 * Interface for handling {@link ComponentActivity#onBackPressed()} callbacks without
 * strongly coupling that implementation to a subclass of {@link ComponentActivity}.
 *
 * @see ComponentActivity#addOnBackPressedCallback(LifecycleOwner, OnBackPressedCallback)
 * @see ComponentActivity#removeOnBackPressedCallback(OnBackPressedCallback)
 */
<<<<<<< HEAD   (eaf9a7 Merge "Merge empty history for sparse-5522318-L8750000030582)
public interface OnBackPressedCallback {
=======
public abstract class OnBackPressedCallback {

    private boolean mEnabled;
    private CopyOnWriteArrayList<Cancellable> mCancellables = new CopyOnWriteArrayList<>();

>>>>>>> BRANCH (736a74 Merge "Merge cherrypicks of [955240] into sparse-5522907-L19)
    /**
     * Callback for handling the {@link ComponentActivity#onBackPressed()} event.
     *
     * @return True if you handled the {@link ComponentActivity#onBackPressed()} event. No
     * further {@link OnBackPressedCallback} instances will be called if you return true.
     */
    boolean handleOnBackPressed();
}
