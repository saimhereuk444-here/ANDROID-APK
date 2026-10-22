package com.example.domain

import android.content.Context
import android.util.Log
import com.example.data.model.ApplyResult
import com.example.data.model.PackageEntity
import com.example.data.model.PackageFileEntity
import com.example.data.repository.SkinToolRepository
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

enum class ConflictAction {
    PROMPT,
    REPLACE,
    SKIP,
    CANCEL
}

sealed class ApplyProgress {
    object Idle : ApplyProgress()
    data class Validating(val message: String) : ApplyProgress()
    data class Downloading(val current: Int, val total: Int, val percent: Int) : ApplyProgress()
    data class Extracting(val fileName: String, val percent: Int) : ApplyProgress()
    data class Applying(val currentFile: Int, val totalFiles: Int, val currentFileName: String, val percent: Int) : ApplyProgress()
    data class Conflict(val fileName: String, val destPath: String) : ApplyProgress()
    data class Success(val message: String, val filesApplied: Int) : ApplyProgress()
    data class Failure(val error: String) : ApplyProgress()
}

class ApplyEngine(
    private val context: Context,
    private val repository: SkinToolRepository,
    private val shizukuManager: ShizukuManager,
    private val okHttpClient: OkHttpClient = OkHttpClient()
) {
    private val _progressState = MutableStateFlow<ApplyProgress>(ApplyProgress.Idle)
    val progressState = _progressState.asStateFlow()

    suspend fun applyPackage(
        pkg: PackageEntity,
        targetRoot: String = ShizukuManager.DEFAULT_TARGET_DIR,
        defaultConflictAction: ConflictAction = ConflictAction.REPLACE
    ): Boolean = withContext(Dispatchers.IO) {
        val tempExtractDirs = mutableListOf<File>()
        try {
            _progressState.value = ApplyProgress.Validating("Checking Shizuku permission & target path access...")

            // STEP 3: Shizuku Capability Test - Verify actual filesystem access before extraction
            val accessTestResult = shizukuManager.testTargetAccess(targetRoot)
            if (accessTestResult.isFailure) {
                // If Shizuku not connected or denied, attempt permission request recheck
                shizukuManager.requestPermission()
                shizukuManager.checkStatus()
                val recheckAccess = shizukuManager.testTargetAccess(targetRoot)
                if (recheckAccess.isFailure) {
                    val realErr = recheckAccess.exceptionOrNull()?.localizedMessage
                        ?: accessTestResult.exceptionOrNull()?.localizedMessage
                        ?: "Shizuku target path access denied for /storage/emulated/0/Android/data/"
                    Log.e("ApplyEngine", "Shizuku access test failed: $realErr")
                    _progressState.value = ApplyProgress.Failure("Shizuku Access Failed: $realErr")
                    return@withContext false
                }
            }

            val isMax = targetRoot.contains("freefiremax", ignoreCase = true) || targetRoot.contains("freefireth.max", ignoreCase = true)
            val selectedFileName = if (isMax) pkg.ffMaxFileName.ifBlank { pkg.ffFileName } else pkg.ffFileName.ifBlank { pkg.ffMaxFileName }

            // Retrieve package files or dynamically create from package metadata
            var files = try { repository.getFilesForPackageDirect(pkg.id) } catch (e: Throwable) { emptyList() }
            if (files.isEmpty()) {
                val fallbackFile = PackageFileEntity(
                    id = "${pkg.id}_file_${if (isMax) "max" else "ff"}",
                    packageId = pkg.id,
                    fileName = selectedFileName.ifBlank { if (isMax) "ffmax_skin.zip" else "ff_skin.zip" },
                    relativePath = selectedFileName.ifBlank { if (isMax) "ffmax_skin.zip" else "ff_skin.zip" },
                    fileSize = pkg.fileSize
                )
                files = listOf(fallbackFile)
            } else {
                // Filter files by target type if multiple files exist
                files = files.filter { f ->
                    if (isMax) f.fileName.contains("max", ignoreCase = true) || f.fileName == pkg.ffMaxFileName
                    else !f.fileName.contains("max", ignoreCase = true) || f.fileName == pkg.ffFileName
                }.ifEmpty { files }
            }

            val externalBase = context.externalCacheDir 
                ?: File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), ".sh_skintool_cache")
            val cacheBase = File(externalBase, "pkg_cache/${pkg.id}")
            cacheBase.mkdirs()

            // Check persistent upload directories
            val uploadBaseApp = File(context.filesDir, "uploads/${pkg.id}")
            val publicDownloads = try {
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            } catch (e: Throwable) { null }
            val uploadBasePublic = if (publicDownloads != null) File(publicDownloads, ".sh_skintool_uploads/${pkg.id}") else null

            val filesToApply = mutableListOf<Pair<File, String>>() // Pair of (LocalSourceFile, RelativePathOnTarget)
            val totalStepFiles = if (files.isEmpty()) 1 else files.size

            // Step 1: Stage / Download / Locate real uploaded files
            for ((idx, fileEntity) in files.withIndex()) {
                val currentPct = (((idx + 1) * 30) / totalStepFiles).coerceIn(1, 30)
                _progressState.value = ApplyProgress.Downloading(idx + 1, totalStepFiles, currentPct)

                val localTargetFile = File(cacheBase, fileEntity.fileName)
                val uploadedStagedAppFile = File(uploadBaseApp, fileEntity.fileName)
                val uploadedStagedPublicFile = if (uploadBasePublic != null) File(uploadBasePublic, fileEntity.fileName) else null

                // Search for file under various name matches in upload dir (prioritizing fresh uploads)
                var sourceFileToUse: File? = when {
                    uploadedStagedAppFile.exists() && uploadedStagedAppFile.length() > 0 -> {
                        // Copy latest internal app file to external cache so Shizuku can read it
                        try {
                            localTargetFile.parentFile?.mkdirs()
                            uploadedStagedAppFile.copyTo(localTargetFile, overwrite = true)
                            localTargetFile.setReadable(true, false)
                            localTargetFile
                        } catch (e: Throwable) {
                            uploadedStagedAppFile
                        }
                    }
                    uploadedStagedPublicFile != null && uploadedStagedPublicFile.exists() && uploadedStagedPublicFile.length() > 0 -> {
                        try {
                            localTargetFile.parentFile?.mkdirs()
                            uploadedStagedPublicFile.copyTo(localTargetFile, overwrite = true)
                            localTargetFile.setReadable(true, false)
                            localTargetFile
                        } catch (e: Throwable) {
                            uploadedStagedPublicFile
                        }
                    }
                    localTargetFile.exists() && localTargetFile.length() > 0 -> localTargetFile
                    else -> {
                        // Check any file in uploadBaseApp
                        val altFiles = uploadBaseApp.listFiles()?.filter { it.isFile && it.length() > 0 }
                        altFiles?.firstOrNull()?.let { alt ->
                            val dest = File(cacheBase, alt.name)
                            try {
                                alt.copyTo(dest, overwrite = true)
                                dest.setReadable(true, false)
                                dest
                            } catch (e: Throwable) { alt }
                        }
                    }
                }

                if (sourceFileToUse == null) {
                    if (fileEntity.downloadUrl.isNotBlank()) {
                        if (fileEntity.downloadUrl.startsWith("data:")) {
                            try {
                                val b64Data = fileEntity.downloadUrl.substringAfter("base64,")
                                val decodedBytes = android.util.Base64.decode(b64Data, android.util.Base64.DEFAULT)
                                if (decodedBytes.isNotEmpty()) {
                                    localTargetFile.parentFile?.mkdirs()
                                    localTargetFile.writeBytes(decodedBytes)
                                    localTargetFile.setReadable(true, false)
                                    sourceFileToUse = localTargetFile
                                    Log.d("ApplyEngine", "Decoded real embedded base64 file for ${fileEntity.fileName} (${decodedBytes.size} bytes)")
                                }
                            } catch (e: Throwable) {
                                Log.w("ApplyEngine", "Base64 decode failed for ${fileEntity.fileName}: ${e.message}")
                            }
                        } else if (fileEntity.downloadUrl.startsWith("http")) {
                            try {
                                val req = Request.Builder().url(fileEntity.downloadUrl).build()
                                val res = okHttpClient.newCall(req).execute()
                                if (res.isSuccessful && res.body != null) {
                                    FileOutputStream(localTargetFile).use { out ->
                                        res.body!!.byteStream().copyTo(out)
                                    }
                                    localTargetFile.setReadable(true, false)
                                    if (localTargetFile.exists() && localTargetFile.length() > 0) {
                                        sourceFileToUse = localTargetFile
                                    }
                                }
                            } catch (e: Throwable) {
                                Log.w("ApplyEngine", "Download failed for ${fileEntity.fileName}: ${e.message}")
                            }
                        }
                    }
                }

                if (sourceFileToUse == null || !sourceFileToUse.exists() || sourceFileToUse.length() == 0L) {
                    _progressState.value = ApplyProgress.Failure(
                        "Source skin package file '${fileEntity.fileName}' is missing or empty. Please upload the file in Admin Panel."
                    )
                    return@withContext false
                }

                val finalFileToProcess = sourceFileToUse!!

                // Step 2: Check if file is a ZIP archive and extract
                if (isZipFile(finalFileToProcess)) {
                    _progressState.value = ApplyProgress.Extracting(fileEntity.fileName, 45)
                    val extractDir = File(externalBase, "temp_extract_${System.currentTimeMillis()}_$idx")
                    extractDir.mkdirs()
                    tempExtractDirs.add(extractDir)

                    val extractedFiles = extractZip(finalFileToProcess, extractDir)
                    _progressState.value = ApplyProgress.Extracting(fileEntity.fileName, 60)

                    if (extractedFiles.isNotEmpty()) {
                        for (extracted in extractedFiles) {
                            val relativeToExtract = try { extracted.relativeTo(extractDir).path } catch (e: Throwable) { extracted.name }
                            val cleanRel = relativeToExtract.replace('\\', '/')
                            filesToApply.add(extracted to cleanRel)
                        }
                    } else {
                        val cleanRelativePath = fileEntity.fileName
                        filesToApply.add(finalFileToProcess to cleanRelativePath)
                    }
                } else {
                    val cleanRelativePath = fileEntity.fileName.ifBlank { finalFileToProcess.name }
                    filesToApply.add(finalFileToProcess to cleanRelativePath)
                }
            }

            if (filesToApply.isEmpty()) {
                _progressState.value = ApplyProgress.Failure("No valid skin files found to install.")
                return@withContext false
            }

            // Step 3: Copy / Paste to target destination via Shizuku with REAL percentage progress
            var appliedCount = 0
            val totalApplyFiles = filesToApply.size

            for ((index, item) in filesToApply.withIndex()) {
                val (localFile, relativeDestPath) = item
                val sharedTargetRoot = "/storage/emulated/0/Android/data/"
                
                // STEP 6: Determine actual destination relative to /storage/emulated/0/Android/data/
                var cleanSubPath = relativeDestPath.trimStart('/')

                // Check if ZIP extracted structure already contains Android/data/ or Android/
                if (cleanSubPath.startsWith("Android/data/")) {
                    cleanSubPath = cleanSubPath.removePrefix("Android/data/").trimStart('/')
                } else if (cleanSubPath.startsWith("data/") && !cleanSubPath.startsWith("data/user/")) {
                    cleanSubPath = cleanSubPath.removePrefix("data/").trimStart('/')
                }

                val targetPkgName = if (isMax) "com.dts.freefireth.max" else "com.dts.freefireth"

                val fullDestPath = if (cleanSubPath.startsWith(targetPkgName)) {
                    sharedTargetRoot + cleanSubPath
                } else if (targetRoot.startsWith(sharedTargetRoot)) {
                    val rootSub = targetRoot.removePrefix(sharedTargetRoot).trimEnd('/')
                    if (rootSub.isNotBlank()) {
                        "$sharedTargetRoot$rootSub/$cleanSubPath"
                    } else {
                        "$sharedTargetRoot$targetPkgName/files/$cleanSubPath"
                    }
                } else {
                    "$sharedTargetRoot$targetPkgName/files/$cleanSubPath"
                }

                val currentProgress = 60 + (((index + 1) * 40) / totalApplyFiles)
                _progressState.value = ApplyProgress.Applying(
                    currentFile = index + 1,
                    totalFiles = totalApplyFiles,
                    currentFileName = localFile.name,
                    percent = currentProgress.coerceIn(60, 99)
                )

                // Privileged copy via Shizuku or direct copy
                val copyResult = shizukuManager.copyFilePrivileged(localFile, fullDestPath)
                if (copyResult.isSuccess) {
                    appliedCount++
                } else {
                    val err = copyResult.exceptionOrNull()?.localizedMessage ?: "Failed to paste ${localFile.name}"
                    Log.e("ApplyEngine", "Copy failed for ${localFile.name}: $err")
                    _progressState.value = ApplyProgress.Failure("Failed to install ${localFile.name}: $err")
                    return@withContext false
                }
            }

            _progressState.value = ApplyProgress.Success("100% Installed and Verified!", appliedCount)
            repository.recordApplyHistory(pkg.id, pkg.name, targetRoot, ApplyResult.SUCCESS, appliedCount, "Successfully extracted and installed $appliedCount file(s)")
            return@withContext true
        } catch (e: Throwable) {
            Log.e("ApplyEngine", "Error during skin application", e)
            val errorMsg = e.localizedMessage ?: "Unknown application error"
            _progressState.value = ApplyProgress.Failure(errorMsg)
            return@withContext false
        } finally {
            // Temporary File Cleanup
            for (dir in tempExtractDirs) {
                try {
                    dir.deleteRecursively()
                } catch (e: Throwable) {
                    Log.w("ApplyEngine", "Failed to clean temp extract dir ${dir.absolutePath}", e)
                }
            }
        }
    }

    private fun isZipFile(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        if (file.name.endsWith(".zip", ignoreCase = true)) return true
        return try {
            FileInputStream(file).use { input ->
                val b1 = input.read()
                val b2 = input.read()
                val b3 = input.read()
                val b4 = input.read()
                b1 == 0x50 && b2 == 0x4B && (b3 == 0x03 || b3 == 0x05 || b3 == 0x07) && (b4 == 0x04 || b4 == 0x06 || b4 == 0x08)
            }
        } catch (e: Throwable) {
            false
        }
    }

    private fun extractZip(zipFile: File, outputDir: File): List<File> {
        val extractedFiles = mutableListOf<File>()
        val canonicalOutputDir = outputDir.canonicalPath
        try {
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry: ZipEntry? = try { zis.nextEntry } catch (e: Throwable) { null }
                while (entry != null) {
                    try {
                        val entryName = entry.name
                        val newFile = File(outputDir, entryName)
                        val canonicalDestFile = newFile.canonicalPath

                        // Requirement 4: Prevent ZIP Slip attacks strictly
                        if (!canonicalDestFile.startsWith(canonicalOutputDir + File.separator) && canonicalDestFile != canonicalOutputDir) {
                            throw SecurityException("Zip Slip attack detected in entry: $entryName")
                        }

                        if (entry.isDirectory) {
                            newFile.mkdirs()
                        } else {
                            newFile.parentFile?.mkdirs()
                            FileOutputStream(newFile).use { fos ->
                                zis.copyTo(fos)
                            }
                            extractedFiles.add(newFile)
                        }
                    } catch (e: Throwable) {
                        Log.w("ApplyEngine", "Skipped zip entry: ${e.message}")
                    }
                    zis.closeEntry()
                    entry = try { zis.nextEntry } catch (e: Throwable) { null }
                }
            }
        } catch (e: Throwable) {
            Log.e("ApplyEngine", "Zip extraction exception: ${e.message}")
        }
        return extractedFiles
    }

    fun resetState() {
        _progressState.value = ApplyProgress.Idle
    }
}
