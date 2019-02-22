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

package com.android.tools.build.jetifier.processor.transform

import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.android.tools.build.jetifier.processor.FileMapping

/**
 * Interface to be implemented by any class that wants process files.
 */
interface Transformer {

    /**
     * Returns whether this instance can process the given archive file.
     */
    fun canTransform(file: ArchiveFile): Boolean

    /**
     * Returns whether this instance can process the given file.
     */
    fun canTransform(fileMapping: FileMapping): Boolean

    /**
     * Runs transformation of the given archive file.
     */
    fun runTransform(file: ArchiveFile)

    /**
     * Runs transformation of the given file.
     */
    fun runTransform(fileMapping: FileMapping)
}