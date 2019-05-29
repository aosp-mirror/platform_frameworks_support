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

package androidx.ui.material.demos

import android.app.Activity
import android.os.Bundle
import androidx.compose.setContent
import androidx.compose.composer
import android.content.DialogInterface
import android.app.AlertDialog
import android.app.Dialog
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView


class ButtonActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ButtonDemo()}


//        var dialog = Dialog(this)
//
//        val textView1 = TextView(this).apply { text = "Compose 1" }
//        val frameLayout1 = FrameLayout(this)
//        frameLayout1.addView(textView1)
//
//        dialog.setContentView(frameLayout1)
//        dialog.show();


//        AlertDialog.Builder(this)
//            .setTitle("Delete entry")
//            .setMessage("Are you sure you want to delete this entry?")
//
//            // Specifying a listener allows you to take an action before dismissing the dialog.
//            // The dialog is automatically dismissed when a dialog button is clicked.
//            .setPositiveButton(
//                android.R.string.yes,
//                object: DialogInterface.OnClickListener {
//                    override fun onClick(dialog: DialogInterface, which: Int) {
//                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//                    }
//                }
//            )
//            // A null listener allows the button to dismiss the dialog and take no further action.
//            .setNegativeButton(android.R.string.no, null)
//            .setIcon(android.R.drawable.ic_dialog_alert)
//            .show()


    }
}

