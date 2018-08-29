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

package androidx.preference.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.preference.DropDownPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.InstrumentationRegistry;
import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for default dynamic summary behaviour in {@link ListPreference},
 * {@link DropDownPreference}, and {@link EditTextPreference}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class DynamicSummaryTest {

    private ListPreference mListPreference;
    private DropDownPreference mDropDownPreference;
    private EditTextPreference mEditTextPreference;
    private PreferenceScreen mScreen;

    private static final String NOT_SET = "Not set";
    private static final String NEW_VALUE = "New Value";
    private static final String KEY = "key";

    // User visible list of options which are persisted as the corresponding value in VALUES
    private static final String[] ENTRIES = {NEW_VALUE};
    // Values from this get persisted when the user selects an entry from ENTRIES
    private static final String[] VALUES = {KEY};

    @Before
    @UiThreadTest
    public void setUp() {
        Context context = InstrumentationRegistry.getTargetContext();
        PreferenceManager preferenceManager = new PreferenceManager(context);
        mScreen = preferenceManager.createPreferenceScreen(context);

        // Mark the screen and children added to it as attached to the main hierarchy
        mScreen.onAttached();

        mListPreference = new ListPreference(context);
        mListPreference.setPersistent(false);
        mListPreference.setEntries(ENTRIES);
        mListPreference.setEntryValues(VALUES);

        mDropDownPreference = new DropDownPreference(context);
        mDropDownPreference.setPersistent(false);
        mDropDownPreference.setEntries(ENTRIES);
        mDropDownPreference.setEntryValues(VALUES);

        mEditTextPreference = new EditTextPreference(context);
        mEditTextPreference.setPersistent(false);
    }

    @Test
    @UiThreadTest
    public void noValueSaved_summaryDisplaysNotSet() {
        mListPreference.setDynamicSummary(true);
        mDropDownPreference.setDynamicSummary(true);
        mEditTextPreference.setDynamicSummary(true);

        mScreen.addPreference(mListPreference);
        mScreen.addPreference(mDropDownPreference);
        mScreen.addPreference(mEditTextPreference);

        assertEquals(NOT_SET, mListPreference.getSummary());
        assertEquals(NOT_SET, mDropDownPreference.getSummary());
        assertEquals(NOT_SET, mEditTextPreference.getSummary());
    }

    @Test
    @UiThreadTest
    public void valueSaved_summaryCorrectlySet() {
        mListPreference.setDynamicSummary(true);
        mDropDownPreference.setDynamicSummary(true);
        mEditTextPreference.setDynamicSummary(true);

        mListPreference.setValue(KEY);
        mDropDownPreference.setValue(KEY);
        mEditTextPreference.setText(NEW_VALUE);

        mScreen.addPreference(mListPreference);
        mScreen.addPreference(mDropDownPreference);
        mScreen.addPreference(mEditTextPreference);

        assertEquals(NEW_VALUE, mListPreference.getSummary());
        assertEquals(NEW_VALUE, mDropDownPreference.getSummary());
        assertEquals(NEW_VALUE, mEditTextPreference.getSummary());
    }

    @Test
    @UiThreadTest
    public void valueChanged_summaryIsUpdated() {
        mListPreference.setDynamicSummary(true);
        mDropDownPreference.setDynamicSummary(true);
        mEditTextPreference.setDynamicSummary(true);

        mScreen.addPreference(mListPreference);
        mScreen.addPreference(mDropDownPreference);
        mScreen.addPreference(mEditTextPreference);

        assertEquals(NOT_SET, mListPreference.getSummary());
        assertEquals(NOT_SET, mDropDownPreference.getSummary());
        assertEquals(NOT_SET, mEditTextPreference.getSummary());

        mListPreference.setValue(KEY);
        mDropDownPreference.setValue(KEY);
        mEditTextPreference.setText(NEW_VALUE);

        assertEquals(NEW_VALUE, mListPreference.getSummary());
        assertEquals(NEW_VALUE, mDropDownPreference.getSummary());
        assertEquals(NEW_VALUE, mEditTextPreference.getSummary());
    }

    @Test
    @UiThreadTest
    public void dynamicSummaryNotSet_summaryNotSetOrUpdated() {
        // Dynamic summary is disabled by default

        mScreen.addPreference(mListPreference);
        mScreen.addPreference(mDropDownPreference);
        mScreen.addPreference(mEditTextPreference);

        assertFalse(mListPreference.hasDynamicSummary());
        assertFalse(mDropDownPreference.hasDynamicSummary());
        assertFalse(mEditTextPreference.hasDynamicSummary());

        assertNull(mListPreference.getSummary());
        assertNull(mDropDownPreference.getSummary());
        assertNull(mEditTextPreference.getSummary());

        mListPreference.setValue(KEY);
        mDropDownPreference.setValue(KEY);
        mEditTextPreference.setText(NEW_VALUE);

        assertNull(mListPreference.getSummary());
        assertNull(mDropDownPreference.getSummary());
        assertNull(mEditTextPreference.getSummary());
    }

    @Test
    @UiThreadTest
    public void dynamicSummaryDisabled_summaryNotSetOrUpdated() {
        mListPreference.setDynamicSummary(false);
        mDropDownPreference.setDynamicSummary(false);
        mEditTextPreference.setDynamicSummary(false);

        mScreen.addPreference(mListPreference);
        mScreen.addPreference(mDropDownPreference);
        mScreen.addPreference(mEditTextPreference);

        assertFalse(mListPreference.hasDynamicSummary());
        assertFalse(mDropDownPreference.hasDynamicSummary());
        assertFalse(mEditTextPreference.hasDynamicSummary());

        assertNull(mListPreference.getSummary());
        assertNull(mDropDownPreference.getSummary());
        assertNull(mEditTextPreference.getSummary());

        mListPreference.setValue(KEY);
        mDropDownPreference.setValue(KEY);
        mEditTextPreference.setText(NEW_VALUE);

        assertNull(mListPreference.getSummary());
        assertNull(mDropDownPreference.getSummary());
        assertNull(mEditTextPreference.getSummary());
    }
}
