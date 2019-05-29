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

package androidx.ui.benchmark.test

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import android.os.Trace
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.benchmark.BenchmarkRule
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.Compose
import androidx.compose.ComposeTrace
import androidx.compose.Model
import androidx.compose.composer
import androidx.compose.compositionReference
import androidx.compose.disposeComposition
import androidx.compose.memo
import androidx.compose.setContent
import androidx.compose.state
import androidx.compose.trace
import androidx.compose.unaryPlus
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.baseui.ColoredRect
import androidx.ui.baseui.selection.ToggleableState
import androidx.ui.benchmark.DefaultBenchmarkActivity
import androidx.ui.core.AndroidCraneView
import androidx.ui.core.ContextAmbient
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Density
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Dp
import androidx.ui.core.FocusManagerAmbient
import androidx.ui.core.Ref
import androidx.ui.core.TestTag
import androidx.ui.core.TextInputServiceAmbient
import androidx.ui.core.dp
import androidx.ui.core.input.FocusManager
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.material.Checkbox
import androidx.ui.material.MaterialTheme
import androidx.ui.material.surface.Surface
import androidx.ui.test.DisableTransitions
import androidx.ui.test.assertIsChecked
import androidx.ui.test.assertIsNotChecked
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.Parameterized
import java.util.Arrays

@Model
class ColorModel(var color: Color) {

}

/**
 * These are just very simple initial benchmark samples.
 * TODO(pavlis): Figure out and implement more interesting benchmarks. Including comparison
 * to legacy.
 */
@LargeTest
@RunWith(Parameterized::class)
//@RunWith(JUnit4::class)
class SampleCompositionBenchmark(private val amountOfCheckboxes: Int) {

//    @Parameterized.Parameter(value = 0)
//    var mTestInteger: Int = 0
//
//    @Parameterized.Parameters
//    fun initParameters(): Collection<Array<Any>> {
//        return listOf(listOf(0), listOf(1))
//    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun initParameters(): Collection<Array<Any>> {
            return listOf(/*arrayListOf(0).toArray(), */arrayListOf(10).toArray())
        }
    }

    //@get:Rule
    //val benchmarkRule = BenchmarkRule()

    @get:Rule
    val activityRule = ActivityTestRule(DefaultBenchmarkActivity::class.java)

    @get:Rule
    val dsiableAnimationRule = DisableTransitions()

    /**
     * Measures how long it take to compose a basic crane wrapper into activity. This also includes
     * the time it takes to do one pass of measuring.
     */
    //@Test
//    @SuppressWarnings
//    fun composeJustRootIntoActivity() {
//        val state = benchmarkRule.getState()
//
//        activityRule.runOnUiThread(object : Runnable {
//            override fun run() {
//                //val root = AndroidCraneView(activityRule.activity)
//                //activityRule.activity.setContentView(root)
//
//                while (state.keepRunning()) {
//                    state.pauseTiming()
//                    val root = AndroidCraneView(activityRule.activity)
//                    activityRule.activity.setContentView(root)
//                    val focusManager = FocusManager()
//
//
//                    state.resumeTiming()
//                    val cc = Compose.composeInto(root.root, activityRule.activity) {
//                        ContextAmbient.Provider(value = activityRule.activity) {
//                            DensityAmbient.Provider(value = Density(activityRule.activity)) {
//                                FocusManagerAmbient.Provider(value = focusManager) {
//                                    TextInputServiceAmbient.Provider(value = root.textInputService) {
//                                        MaterialTheme {
//                                            Surface {
//                                                Column {
//                                                    for (i in 0..amountOfCheckboxes) {
//                                                        Checkbox(false, onCheckedChange = null)
//                                                    }
//                                                }
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//
//                    cc.compose()
//
//                    val widthSpec =
//                        View.MeasureSpec.makeMeasureSpec(root.measuredWidth, View.MeasureSpec.EXACTLY)
//                    val heightSpec =
//                        View.MeasureSpec.makeMeasureSpec(root.measuredHeight, View.MeasureSpec.EXACTLY)
//                    //compositionContext.compose()
//                    root.measure(widthSpec, heightSpec)
//                    root.layout(root.left, root.top, root.right, root.bottom)
//                    root.invalidate()
//
//                    state.pauseTiming()
//
//                    root.disposeComposition()
//
//                    state.resumeTiming()
//                    //Compose.disposeComposition(root.root, activityRule.activity)
//                }
//            }
//        });
//    }

//
//    open class A {
//
//    }
//
//    class B1 : A() {
//
//    }
//
//    class B2 : A() {
//
//    }


    @Test
    @SuppressWarnings
    fun startAndSee() {
        var rootCache: AndroidCraneView? = null;

        val model = ColorModel(Color.Black);

        activityRule.runOnUiThread(object : Runnable {
            override fun run() {
                //val root = AndroidCraneView(activityRule.activity)
                //activityRule.activity.setContentView(root)

                val root = AndroidCraneView(activityRule.activity)
                rootCache = root;
                activityRule.activity.setContentView(root)
                val focusManager = FocusManager()

                ComposeTrace.resetData()

                val memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
                Log.e("FILIP","Used memory ${memory}")

                val cc = Compose.composeInto(root.root, activityRule.activity) {
                    ContextAmbient.Provider(value = activityRule.activity) {
                        DensityAmbient.Provider(value = Density(activityRule.activity)) {
                            FocusManagerAmbient.Provider(value = focusManager) {
                                TextInputServiceAmbient.Provider(value = root.textInputService) {
                                    MaterialTheme {
                                        Surface {
                                            Column {
                                                for (i in 0..amountOfCheckboxes) {
//                                                    //if (i == 0) {
//                                                        var myState = +state { true }
//                                                        TestTag("firstCheckbox"+i) {
//                                                            Checkbox(myState.value, onCheckedChange = { myState.value = !myState.value } )
//                                                        }
//                                                    //} else {
//                                                     //   Checkbox(true, onCheckedChange = null)
//                                                    //}

                                                    if (i == 0) {
                                                        ColoredRect(color = model.color, width = 100.dp, height = 50.dp)
                                                    }
                                                    ColoredRect(color = Color.Green, width = 100.dp, height = 50.dp)

                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


                //cc.compose()

                for (i in 0 .. 50) {

                    if (model.color == Color.Purple) {
                        model.color = Color.Blue
                    } else {
                        model.color = Color.Purple
                    }

                    val start = System.nanoTime()
                    cc.compose()
                    val end = System.nanoTime()

                    Log.e("FILIP", "$i) Compose took ${(end-start)/1000000}ms, and ended up with ${(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000}Kb")
                }

                //cc.compose()

                val widthSpec =
                    View.MeasureSpec.makeMeasureSpec(root.measuredWidth, View.MeasureSpec.EXACTLY)
                val heightSpec =
                    View.MeasureSpec.makeMeasureSpec(root.measuredHeight, View.MeasureSpec.EXACTLY)
                //compositionContext.compose()
                //root.measure(widthSpec, heightSpec)
                //root.layout(root.left, root.top, root.right, root.bottom)

                //Compose.disposeComposition(root.root, activityRule.activity)

            }

        });
        if (true) {
            return
        }

//        val b1 = B1() as A
//        val b2 = B2() as A
//
//        var isA = false
//
//        trace("900is") {
//            for (i in 0..900) {
//                if (i % 2 == 0) {
//                    isA = isA && b1 is B1;
//                } else {
//                    isA = isA && b2 is B2;
//                }
//            }
//        }
//
//        Log.e("FILIP", "boolean: " + isA)


        Log.e("FILIP", "");
        Log.e("FILIP", "Data coming in!");
        Log.e("FILIP", ComposeTrace.data.dumpGroupedData())

        val memory2 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        Log.e("FILIP","Used memory ${memory2}")

        Thread.sleep(20000)

        ComposeTrace.resetData()

        activityRule.runOnUiThread(object : Runnable {
            override fun run() {
                model.color = Color.Purple
            }
        })


        //findByTag("firstCheckbox0")
        //    .doClick()
            //.assertIsNotChecked()

        Thread.sleep(20000)

        Log.e("FILIP", "");
        Log.e("FILIP", "After change: Data coming in!");
        Log.e("FILIP", ComposeTrace.data.dumpGroupedData())

    }

    //@Test
    @SuppressWarnings
    fun legacyExample() {

        activityRule.runOnUiThread(object : Runnable {
            override fun run() {
                val activity = activityRule.activity;

                val rootView = trace("codeSetup") {
                    val linearLayout = LinearLayout(activity)
                    activity.setContentView(linearLayout)
                    for (i in 0..10) {
                        val checkbox = CheckBox(activity)
                        linearLayout.addView(checkbox)
                    }
                    linearLayout
                }



                val widthSpec =
                    View.MeasureSpec.makeMeasureSpec(
                        1000,
                        View.MeasureSpec.AT_MOST
                    )
                val heightSpec =
                    View.MeasureSpec.makeMeasureSpec(
                        1000,
                        View.MeasureSpec.AT_MOST
                    )

                rootView.invalidate()

                trace("linearLayout:Measure") {
                    rootView.measure(1000, 1000)

                }
                trace("linearLayout:Layout") {
                    rootView.layout(rootView.left, rootView.top, rootView.right, rootView.bottom)
                }

                val picture = Picture()

                //val bitmap = Bitmap.createBitmap(widthSpec, heightSpec, Bitmap.Config.ARGB_8888)
                val canvas = picture.beginRecording(1000, 1000)
                //val canvas = Canvas(bitmap)



                trace("linearLayout:Draw") {
                    rootView.draw(canvas)
                }

                picture.endRecording()

                //val frame = FrameLayout(activity)
                //frame.setBackgroundColor(0xFFFF00)

                val imageView = ImageView(activity)
                imageView.setBackgroundColor(0xFFFF00)
                //frame.addView(imageView)
                imageView.setImageBitmap(Bitmap.createBitmap(picture))
                activity.setContentView(imageView)
            }
        })


        Thread.sleep(20000)

        Log.e("FILIP", ComposeTrace.data.dumpGroupedData())

    }



            // This is possible due to the fact that composeInto is synchronous.
//            activityRule.activity.setContent {
//                CraneWrapper {
//                    MaterialTheme {
//                        Surface {
//                            Column { }
//                        }
//                    }
//                }
//            }
//
//            state.pauseTiming()
//            activityRule.launchActivity(null)
//            state.resumeTiming()

            // Every setContent allocates new frame layout and adds composition. This would lead to
            // gradually increasing memory and would skew the results. So we need to decompose.
            //activityRule.activity.disposeComposition()

//    }

    /**
     * Measures how long it take to compose and measure 10 checkboxes. This also includes the time
     * of composing the main wrapper, see [composeJustRootIntoActivity] for the overhead.
     */
//    //@UiThreadTest
//    //@Test
//    fun composeCheckboxIntoActivity_10() {
//        val state = benchmarkRule.getState()
//
//        while (state.keepRunning()) {
//            // This is possible due to the fact that composeInto is synchronous.
//            activityRule.activity.setContent {
//                CraneWrapper {
//                    MaterialTheme {
//                        Surface {
//                            Column {
//                                CreateCheckBox(times = 10)
//                            }
//                        }
//                    }
//                }
//            }
//            //activityRule.activity.disposeComposition()
//        }
//    }
//
//    /**
//     * Measures how long it take to compose and measure 30 checkboxes. This also includes the time
//     * of composing the main wrapper, see [composeJustRootIntoActivity] for the overhead.
//     */
//    //@UiThreadTest
//    //@Test
//    fun composeCheckboxActivity_50() {
//        val state = benchmarkRule.getState()
//
//        while (state.keepRunning()) {
//            // This is possible due to the fact that composeInto is synchronous.
//            activityRule.activity.setContent {
//                CraneWrapper {
//                    MaterialTheme {
//                        Surface {
//                            Column {
//                                CreateCheckBox(times = 50)
//                            }
//                        }
//                    }
//                }
//            }
//            //activityRule.activity.disposeComposition()
//        }
//    }

    @Composable
    fun CreateCheckBox(times: Int) {
        for (i in 0..times) {
            Checkbox(false, onCheckedChange = null)
        }
    }

//    @Composable
//    fun CraneWrapper(@Children children: @Composable() () -> Unit) {
//        val rootRef = +memo { Ref<AndroidCraneView>() }
//
//        // TODO(nona): Tie the focus manger lifecycle to Window, otherwise FocusManager won't work with
//        //             nested AndroidCraneView case
//
//
////        <AndroidCraneView ref=rootRef>
////            val reference = +compositionReference()
////            val rootLayoutNode = rootRef.value?.root ?: error("Failed to create root platform view")
////            val context = rootRef.value?.context ?: composer.composer.context
////            Compose.composeInto(container = rootLayoutNode, context = context, parent = reference) {
////                ContextAmbient.Provider(value = context) {
////                    DensityAmbient.Provider(value = Density(context)) {
////                        FocusManagerAmbient.Provider(value = focusManager) {
////                            TextInputServiceAmbient.Provider(value = rootRef.value?.textInputService) {
////                                children()
////                            }
////                        }
////                    }
////                }
////            }
////        </AndroidCraneView>
//
//            ContextAmbient.Provider(value = context) {
//                DensityAmbient.Provider(value = Density(context)) {
//                    FocusManagerAmbient.Provider(value = focusManager) {
//                        TextInputServiceAmbient.Provider(value = rootRef.value?.textInputService) {
//                            children()
//                        }
//                    }
//                }
//            }
//    }
}