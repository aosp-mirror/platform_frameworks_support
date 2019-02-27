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

import androidx.build.ArtifactsPredicate.All
import androidx.build.ArtifactsPredicate.Benchmark
import androidx.build.ArtifactsPredicate.Exact
import androidx.build.ArtifactsPredicate.Group
import androidx.build.Strategy.Ignore
import androidx.build.Strategy.Prebuilts
import androidx.build.Strategy.TipOfTree

val RELEASE_RULE = docsRules("public", false) {
    prebuilts(LibraryGroups.ACTIVITY.group, "1.0.0-alpha04")
    prebuilts(LibraryGroups.ANNOTATION.group, "1.1.0-alpha01")
    ignore(LibraryGroups.APPCOMPAT.group, "appcompat-resources")
    prebuilts(LibraryGroups.APPCOMPAT.group, "1.1.0-alpha02")
    prebuilts(LibraryGroups.ARCH_CORE.group, "2.0.0")
    prebuilts(LibraryGroups.ASYNCLAYOUTINFLATER.group, "1.0.0")
    prebuilts(LibraryGroups.BIOMETRIC.group, "biometric", "1.0.0-alpha03")
    prebuilts(LibraryGroups.BROWSER.group, "1.0.0")
    ignore(LibraryGroups.CAR.group, "car-moderator")
    prebuilts(LibraryGroups.CAR.group, "car-cluster", "1.0.0-alpha5")
    prebuilts(LibraryGroups.CAR.group, "car", "1.0.0-alpha5")
            .addStubs("car/stubs/android.car.jar")
    prebuilts(LibraryGroups.CARDVIEW.group, "1.0.0")
    prebuilts(LibraryGroups.COLLECTION.group, "1.1.0-alpha02")
    prebuilts(LibraryGroups.CONCURRENT.group, "1.0.0-alpha03")
    prebuilts(LibraryGroups.CONTENTPAGER.group, "1.0.0")
    prebuilts(LibraryGroups.COORDINATORLAYOUT.group, "1.1.0-alpha01")
    prebuilts(LibraryGroups.CORE.group, "core", "1.1.0-alpha04")
    prebuilts(LibraryGroups.CORE.group, "core-ktx", "1.1.0-alpha04")
    prebuilts(LibraryGroups.CURSORADAPTER.group, "1.0.0")
    prebuilts(LibraryGroups.CUSTOMVIEW.group, "1.0.0")
    prebuilts(LibraryGroups.DOCUMENTFILE.group, "1.0.0")
    prebuilts(LibraryGroups.DRAWERLAYOUT.group, "1.0.0")
    prebuilts(LibraryGroups.DYNAMICANIMATION.group, "dynamicanimation-ktx", "1.0.0-alpha01")
    prebuilts(LibraryGroups.DYNAMICANIMATION.group, "1.0.0")
    prebuilts(LibraryGroups.EMOJI.group, "1.0.0")
    prebuilts(LibraryGroups.ENTERPRISE.group, "1.0.0-alpha01")
    prebuilts(LibraryGroups.EXIFINTERFACE.group, "1.0.0")
    prebuilts(LibraryGroups.FRAGMENT.group, "1.1.0-alpha04")
    prebuilts(LibraryGroups.GRIDLAYOUT.group, "1.0.0")
    prebuilts(LibraryGroups.HEIFWRITER.group, "1.0.0")
    prebuilts(LibraryGroups.INTERPOLATOR.group, "1.0.0")
    prebuilts(LibraryGroups.LEANBACK.group, "1.1.0-alpha01")
    prebuilts(LibraryGroups.LEGACY.group, "1.0.0")
    ignore(LibraryGroups.LIFECYCLE.group, "lifecycle-savedstate-core")
    ignore(LibraryGroups.LIFECYCLE.group, "lifecycle-savedstate-fragment")
    ignore(LibraryGroups.LIFECYCLE.group, "lifecycle-viewmodel-savedstate")
    ignore(LibraryGroups.LIFECYCLE.group, "lifecycle-viewmodel-fragment")
    ignore(LibraryGroups.LIFECYCLE.group, "lifecycle-livedata-ktx")
    ignore(LibraryGroups.LIFECYCLE.group, "lifecycle-livedata-core-ktx")
    ignore(LibraryGroups.LIFECYCLE.group, "lifecycle-compiler")
    prebuilts(LibraryGroups.LIFECYCLE.group, "2.1.0-alpha02")
    prebuilts(LibraryGroups.LOADER.group, "1.1.0-alpha01")
    prebuilts(LibraryGroups.LOCALBROADCASTMANAGER.group, "1.1.0-alpha01")
    prebuilts(LibraryGroups.MEDIA.group, "media", "1.1.0-alpha01")
    // TODO: Rename media-widget to media2-widget after 1.0.0-alpha06
    prebuilts(LibraryGroups.MEDIA.group, "media-widget", "1.0.0-alpha06")
    ignore(LibraryGroups.MEDIA2.group, "media2-widget")
    ignore(LibraryGroups.MEDIA2.group, "media2-exoplayer")
    prebuilts(LibraryGroups.MEDIA2.group, "1.0.0-alpha03")
    prebuilts(LibraryGroups.MEDIAROUTER.group, "1.1.0-alpha01")
    ignore(LibraryGroups.NAVIGATION.group, "navigation-testing")
    prebuilts(LibraryGroups.NAVIGATION.group, "1.0.0-rc02")
    prebuilts(LibraryGroups.PAGING.group, "2.1.0")
    prebuilts(LibraryGroups.PALETTE.group, "1.0.0")
    prebuilts(LibraryGroups.PERCENTLAYOUT.group, "1.0.0")
    prebuilts(LibraryGroups.PERSISTENCE.group, "2.0.0")
    prebuilts(LibraryGroups.PREFERENCE.group, "preference-ktx", "1.1.0-alpha03")
    prebuilts(LibraryGroups.PREFERENCE.group, "1.1.0-alpha03")
    prebuilts(LibraryGroups.PRINT.group, "1.0.0")
    prebuilts(LibraryGroups.RECOMMENDATION.group, "1.0.0")
    prebuilts(LibraryGroups.RECYCLERVIEW.group, "recyclerview", "1.1.0-alpha02")
    prebuilts(LibraryGroups.RECYCLERVIEW.group, "recyclerview-selection", "1.1.0-alpha01")
    prebuilts(LibraryGroups.REMOTECALLBACK.group, "1.0.0-alpha01")
    prebuilts(LibraryGroups.ROOM.group, "2.1.0-alpha04")
    prebuilts(LibraryGroups.SLICE.group, "slice-builders", "1.0.0")
    prebuilts(LibraryGroups.SLICE.group, "slice-builders-ktx", "1.0.0-alpha6")
    prebuilts(LibraryGroups.SLICE.group, "slice-core", "1.0.0")
    // TODO: land prebuilts
//    prebuilts(LibraryGroups.SLICE.group, "slice-test", "1.0.0")
    ignore(LibraryGroups.SLICE.group, "slice-test")
    prebuilts(LibraryGroups.SLICE.group, "slice-view", "1.0.0")
    prebuilts(LibraryGroups.SLIDINGPANELAYOUT.group, "1.0.0")
    prebuilts(LibraryGroups.SWIPEREFRESHLAYOUT.group, "1.1.0-alpha01")
    prebuilts(LibraryGroups.TEXTCLASSIFIER.group, "1.0.0-alpha02")
    prebuilts(LibraryGroups.TRANSITION.group, "1.1.0-alpha01")
    prebuilts(LibraryGroups.TVPROVIDER.group, "1.0.0")
    prebuilts(LibraryGroups.VECTORDRAWABLE.group, "1.1.0-alpha01")
    prebuilts(LibraryGroups.VECTORDRAWABLE.group, "vectordrawable-animated", "1.1.0-alpha01")
    prebuilts(LibraryGroups.VIEWPAGER.group, "1.0.0")
    prebuilts(LibraryGroups.VIEWPAGER2.group, "1.0.0-alpha01")
    prebuilts(LibraryGroups.WEAR.group, "1.0.0")
            .addStubs("wear/wear_stubs/com.google.android.wearable-stubs.jar")
    prebuilts(LibraryGroups.WEBKIT.group, "1.0.0")
    prebuilts(LibraryGroups.WORKMANAGER.group, "1.0.0-rc02")
    default(Ignore)
}

val TIP_OF_TREE = docsRules("tipOfTree", true) {
    // TODO: remove once we'll figure out our strategy about it
    ignore(LibraryGroups.CONCURRENT.group)
    default(TipOfTree)
}

/**
 * Rules are resolved in addition order. So if you have two rules that specify how docs should be
 * built for a module, first defined rule wins.
 */
fun docsRules(
    name: String,
    offline: Boolean,
    init: PublishDocsRulesBuilder.() -> Unit
): PublishDocsRules {
    val f = PublishDocsRulesBuilder(name, offline)
    f.init()
    return f.build()
}

class PublishDocsRulesBuilder(private val name: String, private val offline: Boolean) {

    private val rules: MutableList<DocsRule> = mutableListOf(DocsRule(Benchmark, Ignore))
    /**
     * docs for projects within [groupName] will be built from sources.
     */
    fun tipOfTree(groupName: String) {
        rules.add(DocsRule(Group(groupName), TipOfTree))
    }

    /**
     * docs for a project with the given [groupName] and [name] will be built from sources.
     */
    fun tipOfTree(groupName: String, name: String) {
        rules.add(DocsRule(Exact(groupName, name), TipOfTree))
    }

    /**
     * docs for a project with the given [groupName] and [name] will be built from a prebuilt with
     * the given [version].
     */
    fun prebuilts(groupName: String, moduleName: String, version: String): Prebuilts {
        val strategy = Prebuilts(Version(version))
        rules.add(DocsRule(Exact(groupName, moduleName), strategy))
        return strategy
    }

    /**
     * docs for projects within [groupName] will be built from prebuilts with the given [version]
     */
    fun prebuilts(groupName: String, version: String) = prebuilts(groupName, Version(version))

    /**
     * docs for projects within [groupName] will be built from prebuilts with the given [version]
     */
    fun prebuilts(groupName: String, version: Version): Prebuilts {
        val strategy = Prebuilts(version)
        rules.add(DocsRule(Group(groupName), strategy))
        return strategy
    }

    /**
     * defines a default strategy for building docs
     */
    fun default(strategy: Strategy) {
        rules.add(DocsRule(All, strategy))
    }

    /**
     * docs for projects within [groupName] won't be built
     */
    fun ignore(groupName: String) {
        rules.add(DocsRule(Group(groupName), Ignore))
    }

    /**
     * docs for a specified project won't be built
     */
    fun ignore(groupName: String, name: String) {
        rules.add(DocsRule(Exact(groupName, name), Ignore))
    }

    fun build() = PublishDocsRules(name, offline, rules)
}

sealed class ArtifactsPredicate {
    abstract fun apply(inGroup: String, inName: String): Boolean
    object All : ArtifactsPredicate() {
        override fun apply(inGroup: String, inName: String) = true
    }
    class Group(val group: String) : ArtifactsPredicate() {
        override fun apply(inGroup: String, inName: String) = inGroup == group
        override fun toString() = "\"$group\""
    }
    class Exact(val group: String, val name: String) : ArtifactsPredicate() {
        override fun apply(inGroup: String, inName: String) = group == inGroup && name == inName
        override fun toString() = "\"$group\", \"$name\""
    }

    object Benchmark : ArtifactsPredicate() {
        override fun apply(inGroup: String, inName: String) = inName.endsWith("-benchmark")
    }
}

data class DocsRule(val predicate: ArtifactsPredicate, val strategy: Strategy) {
    override fun toString(): String {
        if (predicate is All) {
            return "default($strategy)"
        }
        return when (strategy) {
            is Prebuilts -> "prebuilts($predicate, \"${strategy.version}\")"
            is Ignore -> "ignore($predicate)"
            is TipOfTree -> "tipOfTree($predicate)"
        }
    }
}

sealed class Strategy {
    object TipOfTree : Strategy()
    object Ignore : Strategy()
    class Prebuilts(val version: Version) : Strategy() {
        var stubs: MutableList<String>? = null
        fun addStubs(path: String) {
            if (stubs == null) {
                stubs = mutableListOf()
            }
            stubs!!.add(path)
        }

        override fun toString() = "Prebuilts(\"$version\")"
        fun dependency(extension: SupportLibraryExtension): String {
            return "${extension.mavenGroup?.group}:${extension.project.name}:$version"
        }
    }
}

class PublishDocsRules(val name: String, val offline: Boolean, private val rules: List<DocsRule>) {
    fun resolve(extension: SupportLibraryExtension): DocsRule? {
        val mavenGroup = extension.mavenGroup
        return if (mavenGroup == null) null else resolve(mavenGroup.group, extension.project.name)
    }

    fun resolve(groupName: String, moduleName: String): DocsRule {
        return rules.find { it.predicate.apply(groupName, moduleName) } ?: throw Error()
    }
}
