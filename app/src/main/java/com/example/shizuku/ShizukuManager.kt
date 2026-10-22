package com.example.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.File
import java.lang.reflect.Method

enum class ShizukuStatus {
    NOT_INSTALLED,
    STOPPED,
    RUNNING,
    PERMISSION_REQUIRED,
    CONNECTED
}

class ShizukuManager(private val context: Context) {
    private val tag = "ShizukuManager"

    private val _status = MutableStateFlow(ShizukuStatus.NOT_INSTALLED)
    val status = _status.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _status.value = ShizukuStatus.STOPPED
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                val isGranted = if (Shizuku.isPreV11()) {
                    context.checkCallingOrSelfPermission("moe.shizuku.manager.permission.API_V23") == PackageManager.PERMISSION_GRANTED
                } else {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                }
                if (isGranted) {
                    _status.value = ShizukuStatus.CONNECTED
                } else {
                    _status.value = ShizukuStatus.PERMISSION_REQUIRED
                }
            } else {
                _status.value = ShizukuStatus.PERMISSION_REQUIRED
            }
        }
    }

    init {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (e: Throwable) {
            Log.e(tag, "Failed to register Shizuku listeners", e)
        }
        checkStatus()
    }

    fun destroy() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (e: Throwable) {
            Log.e(tag, "Failed to remove Shizuku listeners", e)
        }
    }

    fun checkStatus() {
        try {
            // First check if Shizuku binder service is active and responsive
            if (Shizuku.pingBinder()) {
                val isGranted = if (Shizuku.isPreV11()) {
                    context.checkCallingOrSelfPermission("moe.shizuku.manager.permission.API_V23") == PackageManager.PERMISSION_GRANTED
                } else {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                }

                if (isGranted) {
                    _status.value = ShizukuStatus.CONNECTED
                } else {
                    _status.value = ShizukuStatus.PERMISSION_REQUIRED
                }
                return
            }

            // If binder is not pingable, check if Shizuku app is installed on the device
            val isInstalled = isShizukuInstalled()
            if (!isInstalled) {
                _status.value = ShizukuStatus.NOT_INSTALLED
            } else {
                _status.value = ShizukuStatus.STOPPED
            }
        } catch (e: Throwable) {
            Log.e(tag, "Error during Shizuku checkStatus", e)
            _status.value = ShizukuStatus.STOPPED
        }
    }

    fun requestPermission() {
        try {
            if (Shizuku.pingBinder()) {
                val isGranted = if (Shizuku.isPreV11()) {
                    context.checkCallingOrSelfPermission("moe.shizuku.manager.permission.API_V23") == PackageManager.PERMISSION_GRANTED
                } else {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                }

                if (isGranted) {
                    _status.value = ShizukuStatus.CONNECTED
                } else {
                    if (Shizuku.isPreV11()) {
                        _status.value = ShizukuStatus.PERMISSION_REQUIRED
                    } else {
                        Shizuku.requestPermission(REQUEST_CODE_PERMISSION)
                    }
                }
            } else {
                checkStatus()
            }
        } catch (e: Throwable) {
            Log.e(tag, "Failed to request Shizuku permission", e)
        }
    }

    private fun isShizukuInstalled(): Boolean {
        val packages = listOf("moe.shizuku.privileged.api", "moe.shizuku.manager", "rikka.sui")
        for (pkg in packages) {
            try {
                context.packageManager.getPackageInfo(pkg, 0)
                return true
            } catch (e: Exception) {
                // Check next
            }
        }
        return false
    }

    suspend fun executePrivilegedCommand(cmd: String): CommandResult = withContext(Dispatchers.IO) {
        if (_status.value != ShizukuStatus.CONNECTED) {
            return@withContext CommandResult(exitCode = -1, output = "", error = "Shizuku authorization not connected")
        }
        try {
            val process: Process = try {
                val method = Shizuku::class.java.getMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                )
                method.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
            } catch (e: Throwable) {
                val method = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                )
                method.isAccessible = true
                method.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
            }

            val output = try { process.inputStream.bufferedReader().readText() } catch (e: Throwable) { "" }
            val error = try { process.errorStream.bufferedReader().readText() } catch (e: Throwable) { "" }
            val code = try { process.waitFor() } catch (e: Throwable) { -1 }

            CommandResult(exitCode = code, output = output, error = error)
        } catch (e: Throwable) {
            Log.e(tag, "Privileged execution error for cmd: $cmd", e)
            CommandResult(exitCode = -1, output = "", error = e.localizedMessage ?: "Unknown privilege execution exception")
        }
    }

    suspend fun testTargetAccess(targetPath: String = DEFAULT_TARGET_DIR): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            checkStatus()
            if (_status.value != ShizukuStatus.CONNECTED) {
                return@withContext Result.failure(Exception("Shizuku service is not connected. Please grant Shizuku permission."))
            }

            val safePath = com.example.security.SecurityAuditor.escapeShellArg(targetPath)
            val result = executePrivilegedCommand("ls -d $safePath && test -r $safePath && echo 'ACCESSIBLE'")
            if (result.exitCode == 0 && result.output.contains("ACCESSIBLE")) {
                Result.success(true)
            } else {
                val errReason = result.error.ifBlank { result.output.ifBlank { "Directory access check failed (code ${result.exitCode})" } }
                Result.failure(Exception("Target directory '$targetPath' access test failed: $errReason"))
            }
        } catch (e: Throwable) {
            Result.failure(Exception("Shizuku capability test error: ${e.localizedMessage}"))
        }
    }

    suspend fun checkDestinationFileExists(destPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!com.example.security.SecurityAuditor.validateGameTargetPath(destPath)) {
                return@withContext false
            }
            if (_status.value == ShizukuStatus.CONNECTED) {
                val safeDest = com.example.security.SecurityAuditor.escapeShellArg(destPath)
                val result = executePrivilegedCommand("test -f $safeDest && echo 'EXISTS'")
                return@withContext result.output.contains("EXISTS")
            } else {
                val file = File(destPath)
                return@withContext file.exists()
            }
        } catch (e: Throwable) {
            return@withContext false
        }
    }

    suspend fun copyFilePrivileged(
        sourceLocalFile: File,
        destPath: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!sourceLocalFile.exists() || sourceLocalFile.length() == 0L) {
                return@withContext Result.failure(Exception("Source file is empty or does not exist: ${sourceLocalFile.name}"))
            }

            // Reject any legacy private path
            if (destPath.contains("/data/user/0/") || destPath.contains("/data/data/")) {
                return@withContext Result.failure(
                    SecurityException("Invalid destination: /data/user/0/ is forbidden. Target must be under /storage/emulated/0/Android/data/")
                )
            }

            // Security Hardening: Validate destination path strictly within allowed game scope
            if (!com.example.security.SecurityAuditor.validateGameTargetPath(destPath)) {
                return@withContext Result.failure(
                    SecurityException("Unauthorized destination path '$destPath': Target must be under /storage/emulated/0/Android/data/")
                )
            }

            if (_status.value != ShizukuStatus.CONNECTED) {
                checkStatus()
                if (_status.value != ShizukuStatus.CONNECTED) {
                    return@withContext Result.failure(
                        Exception("Shizuku authorization required to write to $destPath. Standard app process cannot write to Android/data.")
                    )
                }
            }

            val parentDir = File(destPath).parent ?: ""
            if (parentDir.isNotBlank()) {
                val safeParent = com.example.security.SecurityAuditor.escapeShellArg(parentDir)
                val mkdirRes = executePrivilegedCommand("mkdir -p $safeParent")
                if (mkdirRes.exitCode != 0 && mkdirRes.error.isNotBlank()) {
                    Log.w(tag, "mkdir warning for $parentDir: ${mkdirRes.error}")
                }
            }

            // If source file is in internal app storage (/data/user/0), Shizuku (shell UID 2000)
            // cannot read it due to app sandbox permissions. Stage it to external storage.
            var tempStagedFile: File? = null
            val actualSourceFile = if (sourceLocalFile.absolutePath.startsWith("/data/user/0") || sourceLocalFile.absolutePath.startsWith("/data/data")) {
                try {
                    val extDir = context.externalCacheDir ?: File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), ".sh_staging")
                    extDir.mkdirs()
                    val staged = File(extDir, "staged_${System.currentTimeMillis()}_${sourceLocalFile.name}")
                    sourceLocalFile.copyTo(staged, overwrite = true)
                    staged.setReadable(true, false)
                    tempStagedFile = staged
                    staged
                } catch (e: Throwable) {
                    sourceLocalFile
                }
            } else {
                sourceLocalFile.setReadable(true, false)
                sourceLocalFile
            }

            val safeSource = com.example.security.SecurityAuditor.escapeShellArg(actualSourceFile.absolutePath)
            val safeDest = com.example.security.SecurityAuditor.escapeShellArg(destPath)
            val copyCmd = "cp -f $safeSource $safeDest"
            val result = executePrivilegedCommand(copyCmd)
            
            // Clean up temporary staged file if one was created
            tempStagedFile?.delete()

            if (result.exitCode == 0) {
                val verifyRes = executePrivilegedCommand("test -f $safeDest && echo 'VERIFIED'")
                if (verifyRes.output.contains("VERIFIED")) {
                    Result.success(Unit)
                } else {
                    // Check if file exists via stat
                    val statRes = executePrivilegedCommand("stat -c %s $safeDest")
                    val sizeBytes = statRes.output.trim().toLongOrNull() ?: 0L
                    if (sizeBytes > 0) {
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Verification failed: Target file '$destPath' was not created or is empty."))
                    }
                }
            } else {
                Result.failure(Exception(result.error.ifBlank { "Failed to paste file into $destPath (code ${result.exitCode})" }))
            }
        } catch (e: Throwable) {
            Log.e(tag, "Error in copyFilePrivileged", e)
            Result.failure(Exception("Privileged file copy exception: ${e.localizedMessage}"))
        }
    }

    companion object {
        const val REQUEST_CODE_PERMISSION = 7001
        const val DEFAULT_TARGET_DIR = "/storage/emulated/0/Android/data/"
    }
}

data class CommandResult(
    val exitCode: Int,
    val output: String,
    val error: String
)
