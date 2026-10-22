package com.example.data.remote

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.data.model.AppSettingsEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.FolderEntity
import com.example.data.model.PackageEntity
import com.example.data.model.PackageFileEntity
import com.example.data.model.ScreenEntity
import com.example.data.model.SupportMessageEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class CloudDatabasePayload(
    val version: Long = 1L,
    val packages: List<PackageEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val folders: List<FolderEntity> = emptyList(),
    val screens: List<ScreenEntity> = emptyList(),
    val packageFiles: List<PackageFileEntity> = emptyList(),
    val appSettings: AppSettingsEntity? = null,
    val supportMessages: List<SupportMessageEntity> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

data class CloudObjectWrapper(
    val id: String? = null,
    val name: String? = null,
    val data: CloudDatabasePayload? = null
)

class CloudSyncEngine(private val context: Context) {

    private val tag = "CloudSyncEngine"

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "application/json")
                .build()
            chain.proceed(req)
        }
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val payloadAdapter = moshi.adapter(CloudDatabasePayload::class.java)
    private val wrapperAdapter = moshi.adapter(CloudObjectWrapper::class.java)

    // Distributed High-Availability Multi-Cloud Endpoints for Authoritative Global Multi-Device Sync
    private val cloudEndpoints = listOf(
        CloudEndpoint("https://filebin.net/sh_skintool_global_2026/db.json", HttpMethod.POST),
        CloudEndpoint("https://filebin.net/sh_skintool_global_backup_2026/db.json", HttpMethod.POST),
        CloudEndpoint("https://ntfy.sh/sh_skintool_sync_2026", HttpMethod.POST)
    )

    private enum class HttpMethod { GET, POST, PUT }
    private data class CloudEndpoint(val url: String, val writeMethod: HttpMethod)

    // Persistent Local File Backup Paths (fallback if device is completely offline)
    private fun getBackupFiles(): List<File> {
        val files = mutableListOf<File>()
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null) {
                files.add(File(downloadsDir, ".sh_skintool_cloud_backup.json"))
            }
        } catch (e: Exception) {
            Log.w(tag, "Could not access downloads directory for backup", e)
        }

        try {
            val extFiles = context.getExternalFilesDir(null)
            if (extFiles != null) {
                files.add(File(extFiles, "sh_skintool_backup.json"))
            }
        } catch (e: Exception) {
            Log.w(tag, "Could not access external app dir", e)
        }

        files.add(File(context.filesDir, "sh_skintool_cloud_backup.json"))
        return files
    }

    /**
     * Uploads skin binary file (.zip / .unity3d / images) to cloud file storage
     * returning a direct HTTPS download link accessible on all Android devices.
     */
    suspend fun uploadBinaryFileToCloud(file: File): String? = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) return@withContext null

        // Service 1: filebin.net
        try {
            val request = Request.Builder()
                .url("https://filebin.net/sh_skintool_assets_2026/${file.name}")
                .addHeader("filename", file.name)
                .addHeader("Content-Type", "application/octet-stream")
                .post(file.asRequestBody("application/octet-stream".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val dlUrl = "https://filebin.net/sh_skintool_assets_2026/${file.name}"
                Log.d(tag, "Uploaded ${file.name} to filebin: $dlUrl")
                return@withContext dlUrl
            }
        } catch (e: Throwable) {
            Log.w(tag, "filebin upload error: ${e.message}")
        }

        // Service 2: paste.rs
        try {
            val request = Request.Builder()
                .url("https://paste.rs")
                .post(file.asRequestBody("application/octet-stream".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val respUrl = response.body?.string()?.trim()
                if (!respUrl.isNullOrBlank() && respUrl.startsWith("http")) {
                    Log.d(tag, "Uploaded ${file.name} to paste.rs: $respUrl")
                    return@withContext respUrl
                }
            }
        } catch (e: Throwable) {
            Log.w(tag, "paste.rs upload error: ${e.message}")
        }

        return@withContext null
    }

    suspend fun fetchCloudPayload(): Result<CloudDatabasePayload> = withContext(Dispatchers.IO) {
        val mergedPackages = mutableMapOf<String, PackageEntity>()
        val mergedCategories = mutableMapOf<String, CategoryEntity>()
        val mergedFolders = mutableMapOf<String, FolderEntity>()
        val mergedScreens = mutableMapOf<String, ScreenEntity>()
        val mergedPackageFiles = mutableMapOf<String, PackageFileEntity>()
        val mergedSupportMessages = mutableMapOf<String, SupportMessageEntity>()
        var latestSettings: AppSettingsEntity? = null
        var highestVersion = 1L
        var hasSuccessfulFetch = false

        // Step 1: Query all high-availability endpoints in parallel for maximum speed
        val results: List<CloudDatabasePayload?> = coroutineScope {
            val fetchJobs = cloudEndpoints.map { endpoint ->
                async {
                    try {
                        val fetchUrl = if (endpoint.url.contains("ntfy.sh")) {
                            "${endpoint.url.trimEnd('/')}/json?poll=1"
                        } else {
                            endpoint.url
                        }

                        val request = Request.Builder()
                            .url(fetchUrl)
                            .addHeader("Accept", "application/json")
                            .get()
                            .build()

                        val response = client.newCall(request).execute()
                        if (response.isSuccessful) {
                            val jsonString = response.body?.string()
                            if (!jsonString.isNullOrBlank()) {
                                var parsedPayload: CloudDatabasePayload? = null

                                if (fetchUrl.contains("ntfy.sh")) {
                                    val lines = jsonString.lines().filter { it.isNotBlank() }
                                    for (line in lines.reversed()) {
                                        try {
                                            val obj = JSONObject(line)
                                            if (obj.optString("event") == "message") {
                                                val msg = obj.optString("message")
                                                if (!msg.isNullOrBlank()) {
                                                    parsedPayload = payloadAdapter.fromJson(msg)
                                                    if (parsedPayload != null) break
                                                }
                                            }
                                        } catch (t: Throwable) {}
                                    }
                                }

                                if (parsedPayload == null) {
                                    // Try direct payload parsing
                                    try {
                                        parsedPayload = payloadAdapter.fromJson(jsonString)
                                    } catch (e: Exception) {
                                        // Try wrapped payload parsing
                                        try {
                                            val wrapper = wrapperAdapter.fromJson(jsonString)
                                            if (wrapper?.data != null) {
                                                parsedPayload = wrapper.data
                                            }
                                        } catch (ex: Exception) {
                                            Log.w(tag, "Parsing error on ${endpoint.url}: ${ex.message}")
                                        }
                                    }
                                }
                                parsedPayload
                            } else null
                        } else null
                    } catch (e: Exception) {
                        Log.w(tag, "Fetch error from ${endpoint.url}: ${e.message}")
                        null
                    }
                }
            }
            fetchJobs.awaitAll()
        }

        val validPayloads = results.filterNotNull()
        if (validPayloads.isNotEmpty()) {
            val authoritativePayload = validPayloads.maxByOrNull { it.version } ?: validPayloads.first()
            saveToLocalBackupFiles(payloadAdapter.toJson(authoritativePayload))
            Log.d(tag, "Authoritative Cloud Sync selected payload ver ${authoritativePayload.version} (${authoritativePayload.packages.size} packages, ${authoritativePayload.categories.size} categories)")
            return@withContext Result.success(authoritativePayload)
        }

        // Step 2: Fallback to local persistent backup files if device is offline
        val localBackupPayload = loadFromLocalBackupFiles()
        if (localBackupPayload != null) {
            Log.d(tag, "Loaded authoritative payload from local persistent storage backup (${localBackupPayload.packages.size} packages, ${localBackupPayload.categories.size} categories)")
            return@withContext Result.success(localBackupPayload)
        }

        Result.failure(Exception("Unable to fetch cloud payload and no local backup available"))
    }

    suspend fun saveCloudPayload(payload: CloudDatabasePayload): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val directJson = payloadAdapter.toJson(payload)

            // 1. Immediately save to local persistent storage backup files
            saveToLocalBackupFiles(directJson)

            // 2. Prepare HTTP Body for Cloud Endpoints and push in parallel
            val body = directJson.toRequestBody("application/json; charset=utf-8".toMediaType())

            coroutineScope {
                val pushJobs = cloudEndpoints.map { endpoint ->
                    async {
                        try {
                            val reqBuilder = Request.Builder()
                                .url(endpoint.url)
                                .addHeader("Content-Type", "application/json")

                            if (endpoint.url.contains("filebin.net")) {
                                reqBuilder.addHeader("filename", "db.json")
                            } else if (endpoint.url.contains("ntfy.sh")) {
                                reqBuilder.addHeader("Title", "sh_skintool_sync")
                            }

                            val request = when (endpoint.writeMethod) {
                                HttpMethod.POST -> reqBuilder.post(body).build()
                                HttpMethod.PUT -> reqBuilder.put(body).build()
                                HttpMethod.GET -> reqBuilder.get().build()
                            }

                            val resp = client.newCall(request).execute()
                            val code = resp.code
                            val isOk = resp.isSuccessful
                            resp.close()
                            Log.d(tag, "Sync push on ${endpoint.url} -> code $code (ok: $isOk)")
                            isOk
                        } catch (netEx: Exception) {
                            Log.w(tag, "Sync push error on ${endpoint.url}: ${netEx.message}")
                            false
                        }
                    }
                }
                pushJobs.awaitAll()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error saving cloud payload", e)
            Result.failure(e)
        }
    }

    private fun saveToLocalBackupFiles(jsonString: String) {
        val encryptedPayload = com.example.security.CryptoManager.encrypt(jsonString)
        for (file in getBackupFiles()) {
            try {
                file.parentFile?.mkdirs()
                file.writeText(encryptedPayload)
                Log.d(tag, "Saved secure persistent backup to ${file.name}")
            } catch (e: Exception) {
                Log.w(tag, "Could not write backup to ${file.name}: ${e.message}")
            }
        }
    }

    private fun loadFromLocalBackupFiles(): CloudDatabasePayload? {
        for (file in getBackupFiles()) {
            try {
                if (file.exists() && file.length() > 0) {
                    val rawContent = file.readText().trim()
                    val jsonString = if (rawContent.startsWith("{")) {
                        rawContent
                    } else {
                        com.example.security.CryptoManager.decrypt(rawContent)
                    }
                    val payload = payloadAdapter.fromJson(jsonString)
                    if (payload != null) {
                        Log.d(tag, "Found valid persistent backup file from ${file.name}")
                        return payload
                    }
                }
            } catch (e: Exception) {
                Log.w(tag, "Error reading backup file ${file.name}: ${e.message}")
            }
        }
        return null
    }
}
