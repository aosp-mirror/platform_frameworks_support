/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package android.support.v7.preference;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import java.util.HashSet;
import java.util.Set;

public class MultiSelectListPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat {

    private Set<String> mNewValues = new HashSet<>();
    private boolean mPreferenceChanged;

    public static MultiSelectListPreferenceDialogFragmentCompat newInstance(String key) {
        final MultiSelectListPreferenceDialogFragmentCompat fragment =
                new MultiSelectListPreferenceDialogFragmentCompat();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    private MultiSelectListPreference getMultiSelectListPreference() {
        return (MultiSelectListPreference) getPreference();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        MultiSelectListPreference preference = getMultiSelectListPreference();

        if (preference.getEntries() == null || preference.getEntryValues() == null) {
            throw new IllegalStateException(
                    "MultiSelectListPreference requires an entries array and an entryValues array.");
        }

        boolean[] checkedItems = preference.getSelectedItems();
        final CharSequence[] entryValues = preference.getEntryValues();

        builder.setMultiChoiceItems(preference.getEntries(), checkedItems,
                new DialogInterface.OnMultiChoiceClickListener() {
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (isChecked) {
                            mPreferenceChanged |= mNewValues.add(entryValues[which].toString());
                        } else {
                            mPreferenceChanged |= mNewValues.remove(entryValues[which].toString());
                        }
                    }
                });

        mNewValues.clear();
        mNewValues.addAll(preference.getValues());
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult && mPreferenceChanged) {
            final MultiSelectListPreference preference = getMultiSelectListPreference();

            if (preference.callChangeListener(mNewValues)) {
                preference.setValues(mNewValues);
            }
        }

        mPreferenceChanged = false;
    }
}
