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
package io.github.plume.oss.util

import java.security.Permission

/**
 * Used to stop the JVM from passing System.exit commands.
 */
class CodeControl {
    fun disableSystemExit() {
        val securityManager: SecurityManager = StopExitSecurityManager()
        System.setSecurityManager(securityManager)
    }

    fun enableSystemExit() {
        val mgr = System.getSecurityManager()
        if (mgr != null && mgr is StopExitSecurityManager) System.setSecurityManager(mgr.getPreviousMgr())
        else System.setSecurityManager(null)
    }

    inner class StopExitSecurityManager : SecurityManager() {
        private val _prevMgr = System.getSecurityManager()

        override fun checkPermission(perm: Permission?) {
            if (perm is RuntimePermission) {
                if (perm.getName().startsWith("exitVM")) {
                    throw StopExitException("Exit VM command by external library has been rejected - this is intended.")
                }
            }
            _prevMgr?.checkPermission(perm)
        }

        fun getPreviousMgr(): SecurityManager? = _prevMgr
    }

    inner class StopExitException(msg: String) : RuntimeException(msg)
}