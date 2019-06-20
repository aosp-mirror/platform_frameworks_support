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

package androidx.build

/**
 * The list of versions codes of all the libraries in this project.
 */
object LibraryVersions {
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)
    /**
     * Version code of the support library components.
     */
    val SUPPORT_LIBRARY = Version("1.0.0")

=======
    val ACTIVITY = Version("1.1.0-alpha01")
    val ADS_IDENTIFIER = Version("1.0.0-alpha01")
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
    val ANIMATION = Version("1.0.0-alpha01")
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)

    /**
     * Version code for Room
     */
    val ROOM = Version("2.1.0-alpha01")

    /**
     * Version code for Persistence (SupportSQLite)
     */
    val PERSISTENCE = Version("2.0.0")

    /**
     * Version code for Lifecycle extensions (ProcessLifecycleOwner, Fragment support)
     */
    val LIFECYCLE = Version("2.1.0-alpha01")

    val LIFECYCLES_SAVEDSTATE = Version("1.0.0-alpha01")
    /**
     * Version code for Paging
     */
    val PAGING = Version("2.1.0-alpha01")

    /**
     * Version code for shared code of flatfoot
     */
    val ARCH_CORE = Version("2.0.0")

    /**
     * Version code for shared code of flatfoot runtime
     */
    val ARCH_RUNTIME = Version("2.0.1-alpha01")

    /**
     * Version code for shared testing code of flatfoot
     */
    val ARCH_CORE_TESTING = ARCH_CORE

    /**
     * Version code for Navigation
     */
    val NAVIGATION = Version("1.0.0-alpha07")

    /**
     * Version code for WorkManager
     */
    val WORKMANAGER = Version("1.0.0-alpha10")

    /**
     * Version code for Jetifier
     */
    val JETIFIER = Version("1.0.0-beta01")

    /**
     * Version code for Appcompat
     */
    val ACTIVITY = Version("1.0.0-alpha01")

    /**
     * Version code for Appcompat
     */
    val APPCOMPAT = Version("1.1.0-alpha01")

    /**
     * Version code for Car
     */
    val CAR = Version("1.0.0-alpha5")

    /**
     * Version code for Car
     */
    val CAR_MODERATOR = Version("1.0.0-alpha1")

    /*
     * Version code for mediarouter (depends on media2)
     */
    val MEDIAROUTER = Version("1.1.0-alpha01")

    /**
     * Version code for media-widget (depends on media2)
     */
    val MEDIA_WIDGET = Version("1.0.0-alpha5")

    /**
     * Version code for Core
     */
    val CORE = Version("1.1.0-alpha01")

    /**
     * Version code for GridLayout
     */
    val GRIDLAYOUT = Version("1.1.0-alpha01")

    /**
     * Version code for media2
     */
    val MEDIA2 = Version("1.0.0-alpha03")

    /**
     * Version code for media2-exoplayer
     */
    val MEDIA2_EXOPLAYER = Version("1.0.0-alpha01")

    /*
     * Version code for Benchmark
     */
    val BENCHMARK = Version("1.0.0-alpha01")

    /**
     * Version code for Leanback
     */
    val LEANBACK = Version("1.1.0-alpha01")

    /**
     * Version code for leanback-preference
     */
    val LEANBACK_PREFERENCE = Version("1.1.0-alpha01")

    /**
     * Version code for Webkit
     */
    val WEBKIT = Version("1.1.0-alpha01")

    /**
     * Version code for remote-callbacks
     */
    val REMOTECALLBACK = Version("1.0.0-alpha01")

    /**
     * Version code for slice-builders-ktx
     */
    val SLICE_BUILDERS_KTX = Version("1.0.0-alpha6")

    /**
     * Version code for slice-* modules.
     */
    val SLICE = Version("1.1.0-alpha01")

    /**
     * Version code for versionedparcelable module.
     */
    val VERSIONED_PARCELABLE = Version("1.1.0-alpha01")

    /**
     * Version code for slice-* modules.
     */
=======
    val ANIMATION_TESTING = Version("1.1.0-alpha01")
    val ANNOTATION = Version("1.2.0-alpha01")
    val APPCOMPAT = Version("1.1.0-beta02")
    val ARCH_CORE = Version("2.1.0-rc01")
    val ARCH_CORE_TESTING = ARCH_CORE
    val ARCH_RUNTIME = Version("2.1.0-rc01")
    val ASYNCLAYOUTINFLATER = Version("1.1.0-alpha01")
    val AUTOFILL = Version("1.0.0-alpha01")
    val BENCHMARK = Version("1.0.0-alpha03")
    val BIOMETRIC = Version("1.0.0-alpha05")
    val BROWSER = Version("1.2.0-alpha04")
    val CAMERA = Version("1.0.0-alpha03")
    val CAR = Version("1.0.0-alpha8")
    val CAR_CLUSTER = Version("1.0.0-alpha6")
    val CAR_MODERATOR = Version("1.0.0-alpha1")
    val CARDVIEW = Version("1.1.0-alpha01")
    val COLLECTION = Version("1.2.0-alpha01")
    val CONTENTPAGER = Version("1.1.0-alpha01")
    val COMPOSE = Version("1.0.0-alpha01")
    val COORDINATORLAYOUT = Version("1.1.0-beta02")
    val CORE = Version("1.2.0-alpha02")
    val CORE_ROLE = Version("1.0.0-alpha01")
    val CURSORADAPTER = Version("1.1.0-alpha01")
    val CUSTOMVIEW = Version("1.1.0-alpha01")
    val DOCUMENTFILE = Version("1.1.0-alpha01")
    val DRAWERLAYOUT = Version("1.1.0-alpha02")
    val DYNAMICANIMATION = Version("1.1.0-alpha02")
    val DYNAMICANIMATION_KTX = Version("1.0.0-alpha03")
    val EMOJI = Version("1.1.0-alpha01")
    val ENTERPRISE = Version("1.0.0-alpha03")
    val EXIFINTERFACE = Version("1.1.0-alpha02")
    val FRAGMENT = Version("1.2.0-alpha01")
    val FUTURES = Version("1.0.0-beta02")
    val GRIDLAYOUT = Version("1.1.0-alpha01")
    val HEIFWRITER = Version("1.1.0-alpha01")
    val INSPECTION = Version("1.0.0-alpha01")
    val INTERPOLATOR = Version("1.1.0-alpha01")
    val JETIFIER = Version("1.0.0-rc01")
    val LEANBACK = Version("1.1.0-alpha03")
    val LEANBACK_PREFERENCE = Version("1.1.0-alpha03")
    val LEGACY = Version("1.1.0-alpha01")
    val LOCALBROADCASTMANAGER = Version("1.1.0-alpha02")
    val LIFECYCLE = Version("2.2.0-alpha02")
    val LIFECYCLES_SAVEDSTATE = Version("1.0.0-alpha02")
    val LOADER = Version("1.2.0-alpha01")
    val MEDIA = Version("1.2.0-alpha01")
    val MEDIA2_COMMON = Version("1.1.0-alpha01")
    val MEDIA2_PLAYER = Version("1.1.0-alpha01")
    val MEDIA2_SESSION = Version("1.1.0-alpha01")
    val MEDIA2_EXOPLAYER = Version("1.0.0-rc01")
    val MEDIA2_WIDGET = Version("1.0.0-alpha07")
    val MEDIAROUTER = Version("1.2.0-alpha01")
    val NAVIGATION = Version("2.1.0-alpha06")
    val NAVIGATION_TESTING = Version("1.0.0-alpha08") // Unpublished
    val PAGING = Version("2.2.0-alpha01")
    val PALETTE = Version("1.1.0-alpha01")
    val PRINT = Version("1.1.0-alpha01")
    val PERCENTLAYOUT = Version("1.1.0-alpha01")
    val PERSISTENCE = Version("2.0.1")
    val PREFERENCE = Version("1.1.0-beta02")
    val RECOMMENDATION = Version("1.1.0-alpha01")
    val RECYCLERVIEW = Version("1.1.0-alpha07")
    val REMOTECALLBACK = Version("1.0.0-alpha02")
    val ROOM = Version("2.2.0-alpha01")
    val SAVEDSTATE = Version("1.0.0-rc01")
    val SECURITY = Version("1.0.0-alpha03")
    val SECURITY_IDENTITY_CREDENTIAL = Version("1.0.0-alpha01")
    val SHARETARGET = Version("1.0.0-alpha03")
    val SLICE = Version("1.1.0-alpha02")
    val SLICE_BENCHMARK = Version("1.1.0-alpha02")
    val SLICE_BUILDERS_KTX = Version("1.0.0-alpha08")
    val SLIDINGPANELAYOUT = Version("1.1.0-alpha01")
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
    val SWIPE_REFRESH_LAYOUT = Version("1.1.0-alpha01")
<<<<<<< HEAD   (138046 Merge "Snap for 5059817 from 82004b8f0965236345dce1144b09e2e)

    /**
     * Version code for Biometric
     */
    val BIOMETRIC = Version("1.0.0-alpha02")

    /**
     * Version code for Preference
     */
    val PREFERENCE = Version("1.1.0-alpha01")

    /**
     * Version code for TextClassifier
     */
    val TEXTCLASSIFIER = Version("1.0.0-alpha01")

    /**
     * Version code for RecyclerView
     */
    val RECYCLERVIEW = Version("1.1.0-alpha01")

    /**
     * Version code for Fragment
     */
    val FRAGMENT = Version("1.1.0-alpha01")

    val FUTURES = Version("1.0.0-alpha02")

    val COLLECTION = Version("1.1.0-alpha01")

    /**
     * Version code for CoordinatorLayout
     */
    val COORDINATORLAYOUT = Version("1.1.0-alpha01")

    /**
     * Version code for Transition
     */
    val TRANSITION = Version("1.1.0-alpha01")
=======
    val TEST_SCREENSHOT = Version("1.0.0-alpha01")
    val TEXTCLASSIFIER = Version("1.0.0-alpha03")
    val TRANSITION = Version("1.2.0-alpha02")
    val TVPROVIDER = Version("1.1.0-alpha01")
    val UI = Version("1.0.0-alpha01")
    val VECTORDRAWABLE = Version("1.1.0-beta03")
    val VECTORDRAWABLE_ANIMATED = Version("1.1.0-beta03")
    val VERSIONED_PARCELABLE = Version("1.2.0-alpha01")
    val VIEWPAGER = Version("1.1.0-alpha01")
    val VIEWPAGER2 = Version("1.0.0-alpha06")
    val WEAR = Version("1.1.0-alpha01")
    val WEBKIT = Version("1.1.0-alpha02")
    val WORK = Version("2.1.0-beta02")
>>>>>>> BRANCH (d55bc8 Merge "Replacing "WORKMANAGER" with "WORK" in each build.gra)
}
