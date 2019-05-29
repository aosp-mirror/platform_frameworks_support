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
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.compose.Ambient
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.core.withDensity
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.layout.Padding
import androidx.ui.material.Button
import androidx.ui.material.MaterialTheme
import androidx.ui.material.TransparentButton
import androidx.ui.material.borders.BorderRadius
import androidx.ui.material.borders.BorderSide
import androidx.ui.material.borders.RoundedRectangleBorder
import androidx.ui.material.themeColor
import androidx.ui.material.themeTextStyle
import androidx.ui.graphics.Color
import androidx.compose.Composable
import androidx.compose.Model
import androidx.compose.ambient
import androidx.compose.unaryPlus
import androidx.compose.composer
import androidx.compose.memo
import androidx.compose.onDispose
import androidx.ui.core.AndroidCraneView
import androidx.ui.core.ContextAmbient
import androidx.ui.core.IntPx
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.OnPositioned
import androidx.ui.core.PxPosition
import androidx.ui.material.ripple.CurrentRippleTheme
import androidx.ui.material.ripple.RippleTheme

@Model
class DialogTestModel {

    var isShown = false;

    var position: PxPosition = PxPosition(IntPx(0), IntPx(0))

}

val model = DialogTestModel()

@Composable
fun ButtonDemo() {
    val onClick: () -> Unit = { Log.e("ButtonDemo", "onClick") }
    CraneWrapper {
        MaterialTheme {
            Center {
                Column(mainAxisAlignment = MainAxisAlignment.SpaceEvenly) {
                    Button(onClick = onClick, text = "LONG TEXT")
                    Button(onClick = onClick, text = "SH")
                    TransparentButton(onClick = onClick, text = "NO BACKGROUND")
                    Button(
                        onClick = onClick,
                        color = +themeColor { secondary },
                        text = "SECONDARY COLOR")

                    val outlinedShape = +withDensity {
                        RoundedRectangleBorder(
                            side = BorderSide(Color(0xFF888888.toInt())),
                            // TODO(Andrey): Could shapes be more declarative, so we will copy
                            // the current default shape and just apply a new border color and
                            // not be forced to redefine the borderRadius as well?
                            borderRadius = BorderRadius.circular(
                                4.dp.toPx().value
                            )
                        )
                    }

                    TransparentButton(onClick = onClick, shape = outlinedShape, text = "OUTLINED")

                    val customColor = Color(0xFFFFFF00.toInt())
                    Button(
                        onClick = onClick,
                        text = "CUSTOM STYLE",
                        textStyle = +themeTextStyle { body2.copy(color = customColor) })
                    Button(onClick = onClick) {
                        Padding(padding = 16.dp) {
                            Text(text = "CUSTOM BUTTON!")
                        }
                    }

                    // TODO(Andrey): Disabled button has wrong bg and text color for now.
                    // Need to figure out where will we store their styling. Not a part of
                    // MaterialColors right now and specs are not clear about this.
                    Button(text = "DISABLED. TODO")

                    Button(onClick = { model.isShown = !model.isShown }, text = "Toggle dialog")





//                    OnChildPositioned(onPositioned = { position ->
//                        model.position = position.position
//                        //size = position.size
//                    }) {
//                        Button(onClick = { model.isShown = !model.isShown }, text = "Toggle dialog")
//                    }

                    Button(onClick = { model.isShown = !model.isShown }) {
                        if (model.isShown) {
                            Text("Close Dialog")
                            dialog()
                        } else {
                            Text("Open Dialog")
                        }
                    }

//                    if (model.isShown) {
//                        dialog(model.position) // { model.isShown = false }
//                    }

                }
            }
        }
    }
}

var i: Int = 0;

@Composable
fun dialog(/*onDismiss: (() -> Unit)?*/ /* position: PxPosition */) {
    val context = +ambient(ContextAmbient)



//    val textView1 = +memo { TextView(context).apply { text = "Compose ${i}"} }
//    val frameLayout1 = FrameLayout(context)
//    frameLayout1.addView(textView1)
//
//
//    var popup = PopupWindow(context)
//    popup.contentView = frameLayout1

    var popup = +memo {
        val textView1 = TextView(context).apply { text = "Compose ${i}"}
        val frameLayout1 = FrameLayout(context)
        frameLayout1.addView(textView1)

        val p = PopupWindow(context)
        p.contentView = frameLayout1
        p
    }

    OnPositioned { pos ->
        var position = pos.position
        var craneView = findAndroidCraneView(context as Activity)
        var location = IntArray(2)
        craneView.getLocationOnScreen(location)

        var rootView = (context as Activity).findViewById<View>(android.R.id.content).getRootView()
        popup.showAtLocation(rootView, Gravity.CENTER, location[0] + position.x.value.toInt(), location[1] + position.y.value.toInt() /* 200, 200 */)
    }

//    var craneView = findAndroidCraneView(context as Activity)
//    var location = IntArray(2)
//    craneView.getLocationOnScreen(location)
//
//    var rootView = (context as Activity).findViewById<View>(android.R.id.content).getRootView()
//    popup.showAtLocation(rootView, Gravity.NO_GRAVITY, location[0] + position.x.value.toInt(), location[1] + position.y.value.toInt() /* 200, 200 */)


//    var dialog = Dialog(context)
//
//
//
//    dialog.setContentView(frameLayout1)
//    dialog.show();
//
//    if (onDismiss != null) {
//        dialog.setOnDismissListener(object: DialogInterface.OnDismissListener {
//            override fun onDismiss(dialog: DialogInterface) {
//                onDismiss.invoke()
//            }
//        })
//    } else {
//        dialog.setCancelable(false)
//    }


    ++i

    +onDispose {
        popup.dismiss()
        //dialog.dismiss()
    }
}

internal fun findAndroidCraneView(activity: Activity): AndroidCraneView {
    val contentViewGroup = activity.findViewById<ViewGroup>(android.R.id.content)
    return findAndroidCraneView(contentViewGroup)!!
}

internal fun findAndroidCraneView(parent: ViewGroup): AndroidCraneView? {
    for (index in 0 until parent.childCount) {
        val child = parent.getChildAt(index)
        if (child is AndroidCraneView) {
            return child
        } else if (child is ViewGroup) {
            val craneView = findAndroidCraneView(child)
            if (craneView != null) {
                return craneView
            }
        }
    }
    return null
}