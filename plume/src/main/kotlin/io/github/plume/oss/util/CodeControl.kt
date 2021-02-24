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