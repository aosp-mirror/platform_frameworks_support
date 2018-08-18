import org.gradle.api.internal.artifacts.result.DefaultResolvedArtifactResult

// The output folder inside prebuilts
val prebuiltsLocation = file("../../../../prebuilts/androidx")
val internalFolder = "internal"
val externalFolder = "external"

// Passed in as a project property
val artifactName: String by project

val internalArtifacts = listOf(
        "android.arch(.*)?".toRegex(),
        "com.android.support(.*)?".toRegex()
)

val potentialInternalArtifacts = listOf(
        "androidx(.*)?".toRegex()
)

// Need to exclude androidx.databinding
val forceExternal = setOf(
        ".databinding"
)

buildscript {
    repositories {
        mavenCentral()
        google()
    }
}

plugins {
    java
}

repositories {
    jcenter()
    mavenCentral()
    google()
}

dependencies {
    compile(artifactName)
}

/**
 * Returns the POM file path for a given artifact.
 */
fun pomFile(artifact: ResolvedArtifact): DefaultResolvedArtifactResult? {
    val pomQuery = project.dependencies.createArtifactResolutionQuery()
    val queryResult = pomQuery.forComponents(artifact.id.componentIdentifier)
            .withArtifacts(MavenModule::class.java, MavenPomArtifact::class.java)
            .execute()
    val artifactResult = queryResult.resolvedComponents.firstOrNull()
    // DefaultResolvedArtifactResult is an internal Gradle class.
    // However, it's being widely used anyway.
    return artifactResult?.getArtifacts(MavenPomArtifact::class.java)
            ?.firstOrNull()
            as? DefaultResolvedArtifactResult
}

/**
 * Copies artifacts to the right locations.
 */
fun copyLibrary(artifact: ResolvedArtifact, internal: Boolean = false) {
    val folder = if (internal) internalFolder else externalFolder
    val moduleVersionId = artifact.moduleVersion.id
    val group = moduleVersionId.group
    val groupPath = group.split(".").joinToString("/")
    val pathComponents = listOf(prebuiltsLocation,
            folder,
            groupPath,
            moduleVersionId.name,
            moduleVersionId.version)
    val location = pathComponents.joinToString("/")
    println("Copying $artifact to $location")
    val pomFile = pomFile(artifact)
    copy {
        from(artifact.file)
        into(location)
    }
    if (pomFile != null) {
        println("Copying $pomFile to $location")
        copy {
            from(pomFile.file)
            into(location)
        }
    }
}

tasks {
    val fetchArtifacts by creating {
        doLast {
            // Collect all the internal and external dependencies.
            // Copy the jar/aar's and their respective POM files.
            val internalLibraries =
                    configurations.compile.resolvedConfiguration
                            .resolvedArtifacts.filter {
                        val moduleVersionId = it.moduleVersion.id
                        val group = moduleVersionId.group

                        for (regex in internalArtifacts) {
                            val match = regex.matches(group)
                            if (match) {
                                return@filter regex.matches(group)
                            }
                        }

                        for (regex in potentialInternalArtifacts) {
                            val matchResult = regex.matchEntire(group)
                            val match = regex.matches(group) &&
                                    matchResult?.destructured?.let { (sub) ->
                                        !forceExternal.contains(sub)
                                    } ?: true
                            if (match) {
                                return@filter true
                            }
                        }
                        false
                    }

            val externalLibraries =
                    configurations.compile.resolvedConfiguration
                            .resolvedArtifacts.filter {
                        val isInternal = internalLibraries.contains(it)
                        !isInternal
                    }

            println("\r\nInternal Libraries")
            internalLibraries.forEach { library ->
                copyLibrary(library, internal = true)
            }

            println("\r\nExternal Libraries")
            externalLibraries.forEach { library ->
                copyLibrary(library, internal = false)
            }
            println("\r\nResolved artifacts for $artifactName.")
        }
    }
}

defaultTasks("fetchArtifacts")
