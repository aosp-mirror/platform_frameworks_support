package com.android.tools.build.jetifier.processor.com.android.tools.build.jetifier.processor.transform.java

import com.android.tools.build.jetifier.processor.FileMapping
import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.android.tools.build.jetifier.processor.transform.SourceJetifier
import com.android.tools.build.jetifier.processor.transform.TransformationContext
import com.android.tools.build.jetifier.processor.transform.Transformer

class JavaTransformer internal constructor(private val context: TransformationContext)
    : Transformer {

    // Does not yet support transforming .java files within archives.
    override fun canTransform(file: ArchiveFile) = false
    override fun canTransform(fileMapping: FileMapping) = fileMapping.from.path.endsWith(".java")

    override fun runTransform(file: ArchiveFile) {
        throw IllegalAccessError("Java source file transformation within archives is " +
                "not supported!")
    }
    override fun runTransform(fileMapping: FileMapping) {
        SourceJetifier.jetifySourceFile(
            context.config, fileMapping.from, fileMapping.to,
            context.isInReversedMode
        )
    }
}