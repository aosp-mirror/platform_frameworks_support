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

package com.example.androidx.preference;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.leanback.preference.LeanbackPreferenceFragment;
import androidx.leanback.preference.LeanbackSettingsFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragment;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceScreen;

/**
 * Demo activity using a LeanbackSettingsFragment to display a preference hierarchy.
 */
@RequiresApi(JELLY_BEAN_MR1)
public class LeanbackPreferences extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(android.R.id.content,
                    new SettingsFragment()).commit();
        }
    }

    /**
     * LeanbackSettingsFragment that manages the DemoFragment that displays the preference hierarchy
     */
    //BEGIN_INCLUDE(leanback_preferences)
    public static class SettingsFragment extends LeanbackSettingsFragment {
        @Override
        public void onPreferenceStartInitialScreen() {
            startPreferenceFragment(new DemoFragment());
        }

        @Override
        public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
            final Fragment f =
                    Fragment.instantiate(getActivity(), pref.getFragment(), pref.getExtras());
            f.setTargetFragment(caller, 0);
            if (f instanceof PreferenceFragment || f instanceof PreferenceDialogFragment) {
                startPreferenceFragment(f);
            } else {
                startImmersiveFragment(f);
            }
            return true;
        }

        /**
         * This callback is used to handle navigation between nested preference screens. If you only
         * have one screen of preferences or are using separate fragments for different screens you
         * do not need to implement this.
         */
        @Override
        public boolean onPreferenceStartScreen(PreferenceFragment caller, PreferenceScreen pref) {
            final Fragment fragment = new DemoFragment();
            final Bundle args = new Bundle(1);
            args.putString(PreferenceFragment.ARG_PREFERENCE_ROOT, pref.getKey());
            fragment.setArguments(args);
            startPreferenceFragment(fragment);
            return true;
        }
    }

    /**
     * LeanbackPreferenceFragment that sets the preference hierarchy from XML
     */
    public static class DemoFragment extends LeanbackPreferenceFragment {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Load the preferences from an XML resource
            setPreferencesFromResource(R.xml.preferences, rootKey);
        }
    }
    //END_INCLUDE(leanback_preferences)
}
