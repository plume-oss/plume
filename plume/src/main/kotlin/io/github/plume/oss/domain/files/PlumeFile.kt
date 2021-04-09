/*
 * Copyright 2021 Plume Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.plume.oss.domain.files

import io.github.plume.oss.util.HashUtil
import java.io.File

/**
 * Generic wrapper for supported Plume source/class files.
 */
abstract class PlumeFile(pathname: String, val fileType: PlumeFileType) : File(pathname) {

    override fun hashCode() = if (this.exists())
        HashUtil.getFileHash(this)
    else super.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlumeFile) return false
        if (!super.equals(other)) return false
        return true
    }

}