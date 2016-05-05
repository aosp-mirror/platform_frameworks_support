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
 * limitations under the License.
 */

package android.support.v7.app;

import org.junit.Test;

import android.app.Dialog;
import android.os.Bundle;

public class DialogTestCase extends BaseInstrumentationTestCase<WindowDecorActionBarActivity> {

    public DialogTestCase() {
        super(WindowDecorActionBarActivity.class);
    }

    @Test
    public void testDialogFragmentShows() {
        getInstrumentation().waitForIdleSync();

        TestDialogFragment fragment = new TestDialogFragment();
        fragment.show(getActivity().getSupportFragmentManager(), null);

        getInstrumentation().waitForIdleSync();

        assertNotNull("Dialog was null", fragment.getDialog());
        assertTrue("Dialog was not being shown", fragment.getDialog().isShowing());
    }

    @Test
    public void testDialogFragmentShowAfterStop() {
        stopActivity();
        TestDialogFragment fragment = new TestDialogFragment();
        try {
            fragment.show(getActivity().getSupportFragmentManager(), null);
            fail("epecting exception");
        } catch (IllegalStateException e) {
            assertEquals("Can not perform this action after onSaveInstanceState", e.getMessage());
        }
        startActivity();
    }

    @Test
    public void testDialogFragmentShowsWithTransaction() {
        getInstrumentation().waitForIdleSync();

        TestDialogFragment fragment = new TestDialogFragment();
        fragment.show(getActivity().getSupportFragmentManager().beginTransaction(), null);

        getInstrumentation().waitForIdleSync();

        assertNotNull("Dialog was null", fragment.getDialog());
        assertTrue("Dialog was not being shown", fragment.getDialog().isShowing());
    }

    @Test
    public void testDialogFragmentShowsWithTransactionAfterStop() {
        stopActivity();
        TestDialogFragment fragment = new TestDialogFragment();
        try {
            fragment.show(getActivity().getSupportFragmentManager().beginTransaction(), null);
            fail("epecting exception");
        } catch (IllegalStateException e) {
            assertEquals("Can not perform this action after onSaveInstanceState", e.getMessage());
        }
        startActivity();
    }

    @Test
    public void testDialogFragmentShowsAllowingStateLoss() {
        getInstrumentation().waitForIdleSync();

        TestDialogFragment fragment = new TestDialogFragment();
        fragment.showAllowingStateLoss(getActivity().getSupportFragmentManager(), null);

        getInstrumentation().waitForIdleSync();

        assertNotNull("Dialog was null", fragment.getDialog());
        assertTrue("Dialog was not being shown", fragment.getDialog().isShowing());
    }

    @Test
    public void testDialogFragmentShowsAllowingStateLossAfterStop() {
        stopActivity();

        TestDialogFragment fragment = new TestDialogFragment();
        fragment.showAllowingStateLoss(getActivity().getSupportFragmentManager(), null);

        startActivity();

        assertNotNull("Dialog was null", fragment.getDialog());
        assertTrue("Dialog was not being shown", fragment.getDialog().isShowing());
    }

    @Test
    public void testDialogFragmentShowsTransactionAllowingStateLoss() {
        getInstrumentation().waitForIdleSync();

        TestDialogFragment fragment = new TestDialogFragment();
        fragment.showAllowingStateLoss(getActivity().getSupportFragmentManager().beginTransaction(), null);

        getInstrumentation().waitForIdleSync();

        assertNotNull("Dialog was null", fragment.getDialog());
        assertTrue("Dialog was not being shown", fragment.getDialog().isShowing());
    }

    @Test
    public void testDialogFragmentShowsWithTransactionAllowingStateLossAfterStop() {
        stopActivity();

        TestDialogFragment fragment = new TestDialogFragment();
        fragment.showAllowingStateLoss(getActivity().getSupportFragmentManager().beginTransaction(), null);

        startActivity();

        assertNotNull("Dialog was null", fragment.getDialog());
        assertTrue("Dialog was not being shown", fragment.getDialog().isShowing());
    }

    private void stopActivity(){
        getInstrumentation().runOnMainSync(new Runnable(){
            @Override
            public void run(){
                final WindowDecorActionBarActivity a = getActivity();
                getInstrumentation().callActivityOnStop(a);
            }
        });
        getInstrumentation().waitForIdleSync();
    }

    private void startActivity(){
        getInstrumentation().runOnMainSync(new Runnable(){
            @Override
            public void run(){
                final WindowDecorActionBarActivity a = getActivity();
                getInstrumentation().callActivityOnStart(a);
            }
        });
        getInstrumentation().waitForIdleSync();
    }

    public static class TestDialogFragment extends AppCompatDialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle("Test")
                    .setMessage("Message")
                    .setPositiveButton("Button", null)
                    .create();
        }
    }
}

