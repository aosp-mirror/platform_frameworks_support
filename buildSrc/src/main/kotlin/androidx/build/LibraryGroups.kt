/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.build

/**
 * The list of maven group names of all the libraries in this project.
 */
object LibraryGroups {
<<<<<<< HEAD   (5155e6 Merge "Merge empty history for sparse-5513738-L3500000031735)
    const val ACTIVITY = "androidx.activity"
    const val ANIMATION = "androidx.animation"
    const val ANNOTATION = "androidx.annotation"
    const val APPCOMPAT = "androidx.appcompat"
    const val ASYNCLAYOUTINFLATER = "androidx.asynclayoutinflater"
    const val BIOMETRIC = "androidx.biometric"
    const val BROWSER = "androidx.browser"
    const val BENCHMARK = "androidx.benchmark"
    const val CAR = "androidx.car"
    const val CARDVIEW = "androidx.cardview"
    const val COLLECTION = "androidx.collection"
    const val CONCURRENT = "androidx.concurrent"
    const val CONTENTPAGER = "androidx.contentpager"
    const val COORDINATORLAYOUT = "androidx.coordinatorlayout"
    const val CORE = "androidx.core"
    const val CURSORADAPTER = "androidx.cursoradapter"
    const val CUSTOMVIEW = "androidx.customview"
    const val DOCUMENTFILE = "androidx.documentfile"
    const val DRAWERLAYOUT = "androidx.drawerlayout"
    const val DYNAMICANIMATION = "androidx.dynamicanimation"
    const val EMOJI = "androidx.emoji"
    const val ENTERPRISE = "androidx.enterprise"
    const val EXIFINTERFACE = "androidx.exifinterface"
    const val FRAGMENT = "androidx.fragment"
    const val GRIDLAYOUT = "androidx.gridlayout"
    const val HEIFWRITER = "androidx.heifwriter"
    const val INTERPOLATOR = "androidx.interpolator"
    const val LEANBACK = "androidx.leanback"
    const val LEGACY = "androidx.legacy"
    const val LOADER = "androidx.loader"
    const val LOCALBROADCASTMANAGER = "androidx.localbroadcastmanager"
    const val MEDIA = "androidx.media"
    const val MEDIA2 = "androidx.media2"
    const val MEDIAROUTER = "androidx.mediarouter"
    const val PALETTE = "androidx.palette"
    const val PERCENTLAYOUT = "androidx.percentlayout"
    const val PREFERENCE = "androidx.preference"
    const val PRINT = "androidx.print"
    const val RECOMMENDATION = "androidx.recommendation"
    const val RECYCLERVIEW = "androidx.recyclerview"
    const val SAVEDSTATE = "androidx.savedstate"
    const val SECURITY = "androidx.security"
    const val SHARETARGET = "androidx.sharetarget"
    const val SLICE = "androidx.slice"
    const val REMOTECALLBACK = "androidx.remotecallback"
    const val SLIDINGPANELAYOUT = "androidx.slidingpanelayout"
    const val SWIPEREFRESHLAYOUT = "androidx.swiperefreshlayout"
    const val TEXTCLASSIFIER = "androidx.textclassifier"
    const val TRANSITION = "androidx.transition"
    const val TVPROVIDER = "androidx.tvprovider"
    const val VECTORDRAWABLE = "androidx.vectordrawable"
    const val VIEWPAGER = "androidx.viewpager"
    const val VIEWPAGER2 = "androidx.viewpager2"
    const val WEAR = "androidx.wear"
    const val WEBKIT = "androidx.webkit"
    const val ROOM = "androidx.room"
    const val PERSISTENCE = "androidx.sqlite"
    const val LIFECYCLE = "androidx.lifecycle"
    const val ARCH_CORE = "androidx.arch.core"
    const val PAGING = "androidx.paging"
    const val NAVIGATION = "android.arch.navigation"
    const val JETIFIER = "com.android.tools.build.jetifier"
    const val WORKMANAGER = "android.arch.work"
    const val VERSIONEDPARCELABLE = "androidx.versionedparcelable"
=======
    val ACTIVITY = LibraryGroup("androidx.activity", false)
    val ANIMATION = LibraryGroup("androidx.animation", false)
    val ANNOTATION = LibraryGroup("androidx.annotation", false)
    val APPCOMPAT = LibraryGroup("androidx.appcompat", false)
    val ARCH_CORE = LibraryGroup("androidx.arch.core", false)
    val ASYNCLAYOUTINFLATER = LibraryGroup("androidx.asynclayoutinflater", false)
    val AUTOFILL = LibraryGroup("androidx.autofill", false)
    val BIOMETRIC = LibraryGroup("androidx.biometric", false)
    val BROWSER = LibraryGroup("androidx.browser", false)
    val BENCHMARK = LibraryGroup("androidx.benchmark", false)
    val CAMERA = LibraryGroup("androidx.camera", false)
    val CAR = LibraryGroup("androidx.car", false)
    val CARDVIEW = LibraryGroup("androidx.cardview", false)
    val COLLECTION = LibraryGroup("androidx.collection", false)
    val CONCURRENT = LibraryGroup("androidx.concurrent", false)
    val CONTENTPAGER = LibraryGroup("androidx.contentpager", false)
    val COORDINATORLAYOUT = LibraryGroup("androidx.coordinatorlayout", false)
    val COMPOSE = LibraryGroup("androidx.compose", false)
    val CORE = LibraryGroup("androidx.core", false)
    val CURSORADAPTER = LibraryGroup("androidx.cursoradapter", false)
    val CUSTOMVIEW = LibraryGroup("androidx.customview", false)
    val DOCUMENTFILE = LibraryGroup("androidx.documentfile", false)
    val DRAWERLAYOUT = LibraryGroup("androidx.drawerlayout", false)
    val DYNAMICANIMATION = LibraryGroup("androidx.dynamicanimation", false)
    val EMOJI = LibraryGroup("androidx.emoji", false)
    val ENTERPRISE = LibraryGroup("androidx.enterprise", false)
    val EXIFINTERFACE = LibraryGroup("androidx.exifinterface", false)
    val FRAGMENT = LibraryGroup("androidx.fragment", false)
    val GRIDLAYOUT = LibraryGroup("androidx.gridlayout", false)
    val HEIFWRITER = LibraryGroup("androidx.heifwriter", false)
    val INTERPOLATOR = LibraryGroup("androidx.interpolator", false)
    val JETIFIER = LibraryGroup("com.android.tools.build.jetifier", false)
    val LEANBACK = LibraryGroup("androidx.leanback", false)
    val LEGACY = LibraryGroup("androidx.legacy", false)
    val LIFECYCLE = LibraryGroup("androidx.lifecycle", false)
    val LOADER = LibraryGroup("androidx.loader", false)
    val LOCALBROADCASTMANAGER = LibraryGroup("androidx.localbroadcastmanager", false)
    val MEDIA = LibraryGroup("androidx.media", false)
    val MEDIA2 = LibraryGroup("androidx.media2", false)
    val MEDIAROUTER = LibraryGroup("androidx.mediarouter", false)
    val NAVIGATION = LibraryGroup("androidx.navigation", false)
    val PAGING = LibraryGroup("androidx.paging", false)
    val PALETTE = LibraryGroup("androidx.palette", false)
    val PERCENTLAYOUT = LibraryGroup("androidx.percentlayout", false)
    val PERSISTENCE = LibraryGroup("androidx.sqlite", false)
    val PREFERENCE = LibraryGroup("androidx.preference", false)
    val PRINT = LibraryGroup("androidx.print", false)
    val RECOMMENDATION = LibraryGroup("androidx.recommendation", false)
    val RECYCLERVIEW = LibraryGroup("androidx.recyclerview", false)
    val SAVEDSTATE = LibraryGroup("androidx.savedstate", false)
    val SECURITY = LibraryGroup("androidx.security", false)
    val SHARETARGET = LibraryGroup("androidx.sharetarget", false)
    val SLICE = LibraryGroup("androidx.slice", false)
    val REMOTECALLBACK = LibraryGroup("androidx.remotecallback", false)
    val ROOM = LibraryGroup("androidx.room", false)
    val SLIDINGPANELAYOUT = LibraryGroup("androidx.slidingpanelayout", false)
    val SWIPEREFRESHLAYOUT = LibraryGroup("androidx.swiperefreshlayout", false)
    val TEXTCLASSIFIER = LibraryGroup("androidx.textclassifier", false)
    val TRANSITION = LibraryGroup("androidx.transition", false)
    val TVPROVIDER = LibraryGroup("androidx.tvprovider", false)
    val UI = LibraryGroup("androidx.ui", false)
    val VECTORDRAWABLE = LibraryGroup("androidx.vectordrawable", false)
    val VERSIONEDPARCELABLE = LibraryGroup("androidx.versionedparcelable", false)
    val VIEWPAGER = LibraryGroup("androidx.viewpager", false)
    val VIEWPAGER2 = LibraryGroup("androidx.viewpager2", false)
    val WEAR = LibraryGroup("androidx.wear", false)
    val WEBKIT = LibraryGroup("androidx.webkit", false)
    val WORKMANAGER = LibraryGroup("androidx.work", false)
>>>>>>> BRANCH (c64117 Merge "Merge cherrypicks of [968275] into sparse-5587371-L78)
}
