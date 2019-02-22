package com.android.tools.build.jetifier.processor.com.android.tools.build.jetifier.processor.transform.java

import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.android.tools.build.jetifier.processor.transform.TransformationContext
import com.android.tools.build.jetifier.processor.transform.Transformer

class JavaTransformer internal constructor(private val context: TransformationContext)
    : Transformer {

    // Support only single files for now.
    override fun canTransform(file: ArchiveFile) = file.isJavaFile() && file.isSingleFile

    override fun runTransform(file: ArchiveFile) {
        transformSource(file, context)
    }
}