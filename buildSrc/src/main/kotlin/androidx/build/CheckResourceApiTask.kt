package androidx.build

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Task for detecting changes in the public resource surface
 */
open class CheckResourceApiTask : DefaultTask() {

    @InputFile
    @Optional
    var oldApiFile: File? = null

    @InputFile
    @Optional
    var newApiFile: File? = null

    @TaskAction
    fun checkResourceApi() {

        if (oldApiFile == null) {
            throw GradleException("No resource api file for the current version exists, please" +
                    " run updateResourceApi to create one.")
        }
        var oldResourceApi: HashSet<String> = HashSet<String>(oldApiFile?.readLines())
        var newResourceApi: HashSet<String> = HashSet<String>()
        if (newApiFile != null) {
            newResourceApi = HashSet<String>(newApiFile?.readLines())
        }
        if (!oldResourceApi.equals(newResourceApi)) {
            throw GradleException("Public resource surface changes detected, please run" +
                    " updateResourceApi to confirm this change is intentional.")
        }
    }
}
