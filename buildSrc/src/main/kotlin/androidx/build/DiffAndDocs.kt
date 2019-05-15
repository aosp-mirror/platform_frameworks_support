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

import androidx.build.Strategy.Prebuilts
import androidx.build.Strategy.TipOfTree
<<<<<<< HEAD   (80d066 Merge "Merge empty history for sparse-5530831-L2560000030742)
import androidx.build.checkapi.ApiXmlConversionTask
import androidx.build.checkapi.CheckApiTasks
import androidx.build.checkapi.hasApiTasks
import androidx.build.checkapi.initializeApiChecksForProject
=======
>>>>>>> BRANCH (393684 Merge "Merge cherrypicks of [961903] into sparse-5567208-L67)
import androidx.build.doclava.ChecksConfig
import androidx.build.doclava.DEFAULT_DOCLAVA_CONFIG
import androidx.build.doclava.DoclavaTask
import androidx.build.docs.ConcatenateFilesTask
import androidx.build.docs.GenerateDocsTask
import androidx.build.gradle.isRoot
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
<<<<<<< HEAD   (80d066 Merge "Merge empty history for sparse-5530831-L2560000030742)
=======
import com.android.build.gradle.api.SourceKind
>>>>>>> BRANCH (393684 Merge "Merge cherrypicks of [961903] into sparse-5567208-L67)
import com.google.common.base.Preconditions
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.util.PatternSet
import java.io.File
import java.net.URLClassLoader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.tools.ToolProvider
import kotlin.collections.set

private const val DOCLAVA_DEPENDENCY = "com.android:doclava:1.0.6"

data class DacOptions(val libraryroot: String, val dataname: String)

class DiffAndDocs private constructor(
    root: Project,
    supportRootFolder: File,
    dacOptions: DacOptions,
    additionalRules: List<PublishDocsRules> = emptyList()
) {
    private val anchorTask: TaskProvider<Task>
    private var docsProject: Project? = null

    private val rules: List<PublishDocsRules>
    private val docsTasks: MutableMap<String, TaskProvider<GenerateDocsTask>> = mutableMapOf()
    private val aggregateOldApiTxtsTask: TaskProvider<ConcatenateFilesTask>
    private val aggregateNewApiTxtsTask: TaskProvider<ConcatenateFilesTask>

    init {
        val doclavaConfiguration = root.configurations.create("doclava")
        doclavaConfiguration.dependencies.add(root.dependencies.create(DOCLAVA_DEPENDENCY))

        // tools.jar required for com.sun.javadoc
        // TODO this breaks the ability to use JDK 9+ for compilation.
        doclavaConfiguration.dependencies.add(root.dependencies.create(root.files(
                (ToolProvider.getSystemToolClassLoader() as URLClassLoader).urLs)))

        rules = additionalRules + TIP_OF_TREE
        docsProject = root.findProject(":docs-fake")
        anchorTask = root.tasks.register("anchorDocsTask")
        val generateSdkApiTask = createGenerateSdkApiTask(root, doclavaConfiguration)
        val now = LocalDateTime.now()
        // The diff output assumes that each library is of the same version,
        // but our libraries may each be of different versions
        // So, we display the date as the new version
        val newVersion = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val offlineOverride = root.processProperty("offlineDocs")

        rules.forEach {
            val offline = if (offlineOverride != null) {
                offlineOverride == "true"
            } else {
                it.offline
            }

            val task = createGenerateDocsTask(
                project = root, generateSdkApiTask = generateSdkApiTask,
                doclavaConfig = doclavaConfiguration,
                supportRootFolder = supportRootFolder, dacOptions = dacOptions,
                destDir = File(root.docsDir(), it.name),
                taskName = "${it.name}DocsTask",
                offline = offline)
            docsTasks[it.name] = task
            val createDistDocsTask = createDistDocsTask(root, task, it.name)
            anchorTask.configure {
                it.dependsOn(createDistDocsTask)
            }
        }

        root.tasks.create("generateDocs") { task ->
            task.group = JavaBasePlugin.DOCUMENTATION_GROUP
            task.description = "Generates distribution artifact for d.android.com-style docs."
            task.dependsOn(docsTasks[TIP_OF_TREE.name])
        }

<<<<<<< HEAD   (80d066 Merge "Merge empty history for sparse-5530831-L2560000030742)
        val docletClasspath = doclavaConfiguration.resolve()

=======
        val oldOutputTxt = File(root.docsDir(), "previous.txt")
>>>>>>> BRANCH (393684 Merge "Merge cherrypicks of [961903] into sparse-5567208-L67)
        aggregateOldApiTxtsTask = root.tasks.register("aggregateOldApiTxts",
            ConcatenateFilesTask::class.java) {
            it.Output = File(root.docsDir(), "previous.txt")
        }

<<<<<<< HEAD   (80d066 Merge "Merge empty history for sparse-5530831-L2560000030742)
        val oldApisTask = root.tasks.register("oldApisXml",
            ApiXmlConversionTask::class.java) {
            it.classpath = root.files(docletClasspath)
            it.dependsOn(doclavaConfiguration)

            it.inputApiFile = aggregateOldApiTxtsTask.get().Output
            it.dependsOn(aggregateOldApiTxtsTask)

            it.outputApiXmlFile = File(root.docsDir(), "previous.xml")
        }

=======
        val newApiTxt = File(root.docsDir(), newVersion)
>>>>>>> BRANCH (393684 Merge "Merge cherrypicks of [961903] into sparse-5567208-L67)
        aggregateNewApiTxtsTask = root.tasks.register("aggregateNewApiTxts",
            ConcatenateFilesTask::class.java) {
            it.Output = File(root.docsDir(), newVersion)
        }
<<<<<<< HEAD   (80d066 Merge "Merge empty history for sparse-5530831-L2560000030742)

        val newApisTask = root.tasks.register("newApisXml",
            ApiXmlConversionTask::class.java) {
            it.classpath = root.files(docletClasspath)

            it.inputApiFile = aggregateNewApiTxtsTask.get().Output
            it.dependsOn(aggregateNewApiTxtsTask)

            it.outputApiXmlFile = File(root.docsDir(), "$newVersion.xml")
        }

        val jdiffConfiguration = root.configurations.create("jdiff")
        jdiffConfiguration.dependencies.add(root.dependencies.create(JDIFF_DEPENDENCY))
        jdiffConfiguration.dependencies.add(root.dependencies.create(XML_PARSER_APIS_DEPENDENCY))
        jdiffConfiguration.dependencies.add(root.dependencies.create(XERCES_IMPL_DEPENDENCY))

        generateDiffsTask = createGenerateDiffsTask(root,
            oldApisTask,
            newApisTask,
            jdiffConfiguration)

        generateDiffsTask.configure { diffTask ->
            docsTasks.values.forEach { docs ->
                diffTask.dependsOn(docs)
            }
        }
=======
>>>>>>> BRANCH (393684 Merge "Merge cherrypicks of [961903] into sparse-5567208-L67)
    }

    companion object {
        private const val EXT_NAME = "DIFF_AND_DOCS_EXT"
        /**
         * Returns the instance of DiffAndDocs from the Root project
         */
        fun get(project: Project): DiffAndDocs {
            return project.rootProject.extensions.findByName(EXT_NAME) as? DiffAndDocs
                ?: throw IllegalStateException("must call configureDiffAndDocs first")
        }

        /**
         * Initialization that should happen only once (and on the root project).
         * Returns the anchor task
         */
        fun configureDiffAndDocs(
            root: Project,
            supportRootFolder: File,
            dacOptions: DacOptions,
            additionalRules: List<PublishDocsRules> = emptyList()
        ): TaskProvider<Task> {
            Preconditions.checkArgument(root.isRoot, "Must pass the root project")
            Preconditions.checkState(root.extensions.findByName(EXT_NAME) == null,
                "Cannot initialize DiffAndDocs twice")
            val instance = DiffAndDocs(
                root = root,
                supportRootFolder = supportRootFolder,
                dacOptions = dacOptions,
                additionalRules = additionalRules
            )
            root.extensions.add(EXT_NAME, instance)
            instance.setupDocsProject()
            return instance.anchorTask
        }
    }

    private fun prebuiltSources(
        root: Project,
        mavenId: String,
        originName: String,
        originRule: DocsRule
    ): FileTree {
        val configName = "docs-temp_${mavenId.replace(":", "-")}"
        val configuration = root.configurations.create(configName)
        root.dependencies.add(configName, mavenId)

        val artifacts = try {
            configuration.resolvedConfiguration.resolvedArtifacts
        } catch (e: ResolveException) {
            root.logger.error("Failed to find prebuilts for $mavenId. " +
                    "A matching rule $originRule in docsRules(\"$originName\") " +
                    "in PublishDocsRules.kt requires it. You should either add a prebuilt, " +
                    "or add overriding \"ignore\" or \"tipOfTree\" rules")
            throw e
        }

        val artifact = artifacts.find { it.moduleVersion.id.toString() == mavenId }
                ?: throw GradleException()

        val folder = artifact.file.parentFile
        val tree = root.zipTree(File(folder, "${artifact.file.nameWithoutExtension}-sources.jar"))
                .matching {
                    it.exclude("**/*.MF")
                    it.exclude("**/*.aidl")
                    it.exclude("**/*.html")
                    it.exclude("**/*.kt")
                    it.exclude("**/META-INF/**")
                    it.exclude("**/OWNERS")
                }
        root.configurations.remove(configuration)
        return tree
    }

    private fun setupDocsProject() {
        docsProject?.afterEvaluate { docs ->
            val appExtension = docs.extensions.findByType(AppExtension::class.java)
                    ?: throw GradleException("Android app plugin is missing on docsProject")

            rules.forEach { rule ->
                appExtension.productFlavors.create(rule.name) {
                    it.dimension = "library-group"
                }
            }
            appExtension.applicationVariants.all { v ->
                val taskProvider = docsTasks[v.flavorName]
                if (v.buildType.name == "release" && taskProvider != null) {
                    registerAndroidProjectForDocsTask(taskProvider, v)
                    taskProvider.configure {
                        it.exclude { fileTreeElement ->
                            fileTreeElement.path.endsWith(v.rFile())
                        }
                    }
                }
            }
        }

        docsProject?.let { docsProject ->
            docsProject.beforeEvaluate {
                docsProject.rootProject.subprojects.asSequence()
                    .filter { docsProject != it }
                    .forEach { docsProject.evaluationDependsOn(it.path) }
            }
        }
    }

    fun registerPrebuilts(extension: SupportLibraryExtension) =
            docsProject?.afterEvaluate { docs ->
        val depHandler = docs.dependencies
        val root = docs.rootProject
        rules.forEach { rule ->
            val resolvedRule = rule.resolve(extension)
            val strategy = resolvedRule?.strategy
            if (strategy is Prebuilts) {
                val dependency = strategy.dependency(extension)
                depHandler.add("${rule.name}Implementation", dependency)
                strategy.stubs?.forEach { path ->
                    depHandler.add("${rule.name}CompileOnly", root.files(path))
                }
                docsTasks[rule.name]!!.configure {
                    it.source(prebuiltSources(root, dependency, rule.name, resolvedRule))
                }
            }
        }
    }

    private fun tipOfTreeTasks(
        extension: SupportLibraryExtension,
        setup: (TaskProvider<out DoclavaTask>) -> Unit
    ) {
        rules.filter { rule -> rule.resolve(extension)?.strategy == TipOfTree }
                .mapNotNull { rule -> docsTasks[rule.name] }
                .forEach(setup)
    }

    /**
     * Registers a Java project to be included in docs generation, local API file generation, and
     * local API diff generation tasks.
     */
    fun registerJavaProject(project: Project, extension: SupportLibraryExtension) {
        val compileJava = project.tasks.named("compileJava", JavaCompile::class.java)

        registerPrebuilts(extension)

        tipOfTreeTasks(extension) { task ->
            registerJavaProjectForDocsTask(task, compileJava)
        }
<<<<<<< HEAD   (80d066 Merge "Merge empty history for sparse-5530831-L2560000030742)

        registerJavaProjectForDocsTask(generateDiffsTask, compileJava)
        if (!hasApiTasks(project, extension)) {
            return
        }

        val tasks = initializeApiChecksForProject(project,
                aggregateOldApiTxtsTask, aggregateNewApiTxtsTask)
        registerJavaProjectForDocsTask(tasks.generateApi, compileJava)
        setupApiVersioningInDocsTasks(extension, tasks)
        addCheckApiTasksToGraph(tasks)
        registerJavaProjectForDocsTask(tasks.generateLocalDiffs, compileJava)
        val generateApiDiffsArchiveTask = createGenerateLocalApiDiffsArchiveTask(project,
                tasks.generateLocalDiffs)
        generateApiDiffsArchiveTask.configure {
            it.dependsOn(tasks.generateLocalDiffs)
        }
=======
>>>>>>> BRANCH (393684 Merge "Merge cherrypicks of [961903] into sparse-5567208-L67)
    }

    /**
     * Registers an Android project to be included in global docs generation, local API file
     * generation, and local API diff generation tasks.
     */
    fun registerAndroidProject(
        project: Project,
        library: LibraryExtension,
        extension: SupportLibraryExtension
    ) {

        registerPrebuilts(extension)
        library.libraryVariants.all { variant ->
            if (variant.name == Release.DEFAULT_PUBLISH_CONFIG) {
                // include R.file generated for prebuilts
                rules.filter { it.resolve(extension)?.strategy is Prebuilts }.forEach { rule ->
                    docsTasks[rule.name]?.configure {
                        it.include { fileTreeElement ->
                            fileTreeElement.path.endsWith(variant.rFile())
                        }
                    }
                }

                tipOfTreeTasks(extension) { task ->
                    registerAndroidProjectForDocsTask(task, variant)
                }

                if (!hasApiTasks(project, extension)) {
                    return@all
                }
                val tasks = initializeApiChecksForProject(project, aggregateOldApiTxtsTask,
                        aggregateNewApiTxtsTask)
                registerAndroidProjectForDocsTask(tasks.generateApi, variant)
                setupApiVersioningInDocsTasks(extension, tasks)
                addCheckApiTasksToGraph(tasks)
                registerAndroidProjectForDocsTask(tasks.generateLocalDiffs, variant)
                val generateApiDiffsArchiveTask = createGenerateLocalApiDiffsArchiveTask(project,
                        tasks.generateLocalDiffs)
                generateApiDiffsArchiveTask.configure {
                    it.dependsOn(tasks.generateLocalDiffs)
                }
            }
        }
    }
<<<<<<< HEAD   (80d066 Merge "Merge empty history for sparse-5530831-L2560000030742)

    private fun setupApiVersioningInDocsTasks(
        extension: SupportLibraryExtension,
        checkApiTasks: CheckApiTasks
    ) {
        rules.forEach { rules ->
            val project = extension.project
            val strategy = rules.resolve(extension)?.strategy
            val version = if (strategy is Prebuilts) {
                strategy.version
            } else {
                extension.project.version()
            }
            docsTasks[rules.name]!!.configure { docs ->
                // Track API change history.
                docs.addSinceFilesFrom(project.projectDir)
                // Associate current API surface with the Maven artifact.
                val artifact = "${project.group}:${project.name}:$version"
                docs.addArtifact(checkApiTasks.generateApi.get().apiFile!!.absolutePath, artifact)
                docs.dependsOn(checkApiTasks.generateApi)
            }
        }
    }

    private fun addCheckApiTasksToGraph(tasks: CheckApiTasks) {
        docsTasks.values.forEach { docs ->
            docs.configure {
                it.dependsOn(tasks.generateApi)
            }
        }
        anchorTask.configure {
            it.dependsOn(tasks.checkApi)
        }
    }
=======
>>>>>>> BRANCH (393684 Merge "Merge cherrypicks of [961903] into sparse-5567208-L67)
}

/**
 * Registers a Java project on the given Javadocs task.
 * <p>
 * <ul>
 * <li>Sets up a dependency to ensure the project is compiled prior to running the task
 * <li>Adds the project's source files to the Javadoc task's source files
 * <li>Adds the project's compilation classpath (e.g. dependencies) to the task classpath to ensure
 *     that references in the source files may be resolved
 * <li>Adds the project's output artifacts to the task classpath to ensure that source references to
 *     generated code may be resolved
 * </ul>
 */
private fun registerJavaProjectForDocsTask(
    docsTaskProvider: TaskProvider<out Javadoc>,
    javaCompileTaskProvider: TaskProvider<JavaCompile>
) {
    docsTaskProvider.configure { docsTask ->
        docsTask.dependsOn(javaCompileTaskProvider)
        var javaCompileTask = javaCompileTaskProvider.get()
        docsTask.source(javaCompileTask.source)
        val project = docsTask.project
        docsTask.classpath += project.files(javaCompileTask.classpath) +
                project.files(javaCompileTask.destinationDir)
    }
}

/**
 * Registers an Android project on the given Javadocs task.
 * <p>
 * @see #registerJavaProjectForDocsTask
 */
private fun registerAndroidProjectForDocsTask(
    task: TaskProvider<out Javadoc>,
    releaseVariant: BaseVariant
) {
    // This code makes a number of unsafe assumptions about Android Gradle Plugin,
    // and there's a good chance that this will break in the near future.
    val javaCompileProvider = releaseVariant.javaCompileProvider
    task.configure {
        it.dependsOn(javaCompileProvider)
        it.include { fileTreeElement ->
            fileTreeElement.name != "R.java" ||
                    fileTreeElement.path.endsWith(releaseVariant.rFile())
        }
        it.source(javaCompileProvider.map {
            it.source
        })
        it.classpath += releaseVariant.getCompileClasspath(null) +
                it.project.files(javaCompileProvider.get().destinationDir)
    }
}

// Generates a distribution artifact for online docs.
private fun createDistDocsTask(
    project: Project,
    generateDocs: TaskProvider<out DoclavaTask>,
    ruleName: String = ""
): TaskProvider<Zip> = project.tasks.register("dist${ruleName}Docs", Zip::class.java) {
    it.apply {
        dependsOn(generateDocs)
        group = JavaBasePlugin.DOCUMENTATION_GROUP
        description = "Generates distribution artifact for d.android.com-style documentation."
        from(generateDocs.map {
            it.destinationDir
        })
<<<<<<< HEAD   (80d066 Merge "Merge empty history for sparse-5530831-L2560000030742)
        baseName = "android-support-$ruleName-docs"
        version = getBuildId()
        destinationDir = project.getDistributionDirectory()
=======
        val baseName = "android-support-$ruleName-docs"
        val buildId = getBuildId()
        archiveBaseName.set(baseName)
        archiveVersion.set(buildId)
        destinationDirectory.set(project.getDistributionDirectory())
        group = JavaBasePlugin.DOCUMENTATION_GROUP
        val filePath = "${project.getDistributionDirectory().canonicalPath}/"
        val fileName = "$baseName-$buildId.zip"
        val destinationFile = filePath + fileName
        description = "Zips $ruleName Java documentation (generated via Doclava in the " +
            "style of d.android.com) into $destinationFile"
>>>>>>> BRANCH (393684 Merge "Merge cherrypicks of [961903] into sparse-5567208-L67)
        doLast {
            logger.lifecycle("'Wrote API reference to $destinationFile")
        }
    }
}

/**
 * Creates a task to generate an API file from the platform SDK's source and stub JARs.
 * <p>
 * This is useful for federating docs against the platform SDK when no API XML file is available.
 */
private fun createGenerateSdkApiTask(project: Project, doclavaConfig: Configuration): DoclavaTask =
        project.tasks.createWithConfig("generateSdkApi", DoclavaTask::class.java) {
            dependsOn(doclavaConfig)
            description = "Generates API files for the current SDK."
            setDocletpath(doclavaConfig.resolve())
            destinationDir = project.docsDir()
            classpath = androidJarFile(project)
            source(project.zipTree(androidSrcJarFile(project))
                .matching(PatternSet().include("**/*.java")))
            exclude("**/overview.html") // TODO https://issuetracker.google.com/issues/116699307
            apiFile = sdkApiFile(project)
            generateDocs = false
            coreJavadocOptions {
                addStringOption("stubpackages", "android.*")
            }
        }

private val GENERATEDOCS_HIDDEN = listOf(105, 106, 107, 111, 112, 113, 115, 116, 121)
private val GENERATE_DOCS_CONFIG = ChecksConfig(
        warnings = emptyList(),
        hidden = GENERATEDOCS_HIDDEN + DEFAULT_DOCLAVA_CONFIG.hidden,
        errors = ((101..122) - GENERATEDOCS_HIDDEN)
)

private fun createGenerateDocsTask(
    project: Project,
    generateSdkApiTask: DoclavaTask,
    doclavaConfig: Configuration,
    supportRootFolder: File,
    dacOptions: DacOptions,
    destDir: File,
    taskName: String = "generateDocs",
    offline: Boolean
): TaskProvider<GenerateDocsTask> =
        project.tasks.register(taskName, GenerateDocsTask::class.java) {
            it.apply {
                dependsOn(generateSdkApiTask, doclavaConfig)
                group = JavaBasePlugin.DOCUMENTATION_GROUP
<<<<<<< HEAD   (80d066 Merge "Merge empty history for sparse-5530831-L2560000030742)
                description = "Generates d.android.com-style documentation. To generate offline " +
                        "docs use \'-PofflineDocs=true\' parameter."
=======
                description = "Generates Java documentation in the style of d.android.com. To " +
                        "generate offline docs use \'-PofflineDocs=true\' parameter.  Places the " +
                        "documentation in $destDir"
>>>>>>> BRANCH (393684 Merge "Merge cherrypicks of [961903] into sparse-5567208-L67)

                setDocletpath(doclavaConfig.resolve())
                destinationDir = File(destDir, if (offline) "offline" else "online")
                classpath = androidJarFile(project)
                checksConfig = GENERATE_DOCS_CONFIG
                addSinceFilesFrom(supportRootFolder)

                coreJavadocOptions {
                    addStringOption("templatedir",
                        "$supportRootFolder/../../external/doclava/res/assets/templates-sdk")
                    addStringOption("samplesdir", "$supportRootFolder/samples")
                    addMultilineMultiValueOption("federate").value = listOf(
                        listOf("Android", "https://developer.android.com")
                    )
                    addMultilineMultiValueOption("federationapi").value = listOf(
                        listOf("Android", generateSdkApiTask.apiFile?.absolutePath)
                    )
                    addMultilineMultiValueOption("hdf").value = listOf(
                        listOf("android.whichdoc", "online"),
                        listOf("android.hasSamples", "true"),
                        listOf("dac", "true")
                    )

                    // Specific to reference docs.
                    if (!offline) {
                        addStringOption("toroot", "/")
                        addBooleanOption("devsite", true)
                        addBooleanOption("yamlV2", true)
                        addStringOption("dac_libraryroot", dacOptions.libraryroot)
                        addStringOption("dac_dataname", dacOptions.dataname)
                    }
                }

                addArtifactsAndSince()
            }
        }

<<<<<<< HEAD   (80d066 Merge "Merge empty history for sparse-5530831-L2560000030742)
private fun createGenerateLocalApiDiffsArchiveTask(
    project: Project,
    diffTask: TaskProvider<JDiffTask>
): TaskProvider<Zip> = project.tasks.register("generateLocalApiDiffsArchive", Zip::class.java) {
    val docsDir = project.rootProject.docsDir()
    it.from(diffTask.map {
        it.destinationDir
    })
    it.destinationDir = File(docsDir, "online/sdk/support_api_diff/${project.name}")
    it.to("${project.version}.zip")
}

=======
>>>>>>> BRANCH (393684 Merge "Merge cherrypicks of [961903] into sparse-5567208-L67)
private fun sdkApiFile(project: Project) = File(project.docsDir(), "release/sdk_current.txt")

fun <T : Task> TaskContainer.createWithConfig(
    name: String,
    taskClass: Class<T>,
    config: T.() -> Unit
) =
        create(name, taskClass) { task -> task.config() }

fun androidJarFile(project: Project): FileCollection =
        project.files(arrayOf(File(project.sdkPath(),
                "platforms/${SupportConfig.COMPILE_SDK_VERSION}/android.jar")))

private fun androidSrcJarFile(project: Project): File = File(project.sdkPath(),
        "platforms/${SupportConfig.COMPILE_SDK_VERSION}/android-stubs-src.jar")

private fun PublishDocsRules.resolve(extension: SupportLibraryExtension): DocsRule? {
    val mavenGroup = extension.mavenGroup
    return if (mavenGroup == null) null else resolve(mavenGroup, extension.project.name)
}

private fun Prebuilts.dependency(extension: SupportLibraryExtension) =
        "${extension.mavenGroup}:${extension.project.name}:$version"

private fun BaseVariant.rFile() = "${applicationId.replace('.', '/')}/R.java"

// Nasty part. Get rid of that eventually!
fun Project.docsDir(): File = properties["docsDir"] as File

private fun Project.sdkPath(): File {
    val supportRoot = (project.rootProject.property("ext") as ExtraPropertiesExtension)
        .get("supportRootFolder") as File
    return getSdkPath(supportRoot)
}

fun Project.processProperty(name: String) =
        if (hasProperty(name)) {
            properties[name] as String
        } else {
            null
        }
