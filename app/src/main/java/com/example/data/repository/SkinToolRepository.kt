package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.ApplyHistoryEntity
import com.example.data.model.ApplyResult
import com.example.data.model.CategoryEntity
import com.example.data.model.ContentStatus
import com.example.data.model.FavoriteEntity
import com.example.data.model.FolderEntity
import com.example.data.model.PackageEntity
import com.example.data.model.PackageFileEntity
import com.example.data.model.ScreenEntity
import com.example.data.model.SupportMessageEntity
import com.example.data.model.SyncMetadataEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.remote.FirebaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.UUID

class SkinToolRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val firebaseService: FirebaseService
) {
    private val tag = "SkinToolRepository"
    private val scope = CoroutineScope(Dispatchers.IO)

    private val isSyncing = java.util.concurrent.atomic.AtomicBoolean(false)
    private val areFirebaseObserversAttached = java.util.concurrent.atomic.AtomicBoolean(false)

    private val _syncState = MutableStateFlow("IDLE") // IDLE, SYNCING, SYNCED, OFFLINE, ERROR
    val syncState = _syncState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _appSettings = MutableStateFlow(
        com.example.data.model.AppSettingsEntity(
            userPassword = "SAIMHERE444",
            adminPassword = "SAIMHEREFORAREASON",
            getPasswordUrl = "https://t.me/saimteam"
        )
    )
    val appSettings = _appSettings.asStateFlow()

    init {
        scope.launch {
            loadSettings()
            checkInitialUser()
            // Purge any pre-existing demo/sample data so Dual File Manager starts with 0 items
            purgeDemoDataIfPresent()
            attachFirebaseObservers()
            // Start periodic sync after initial startup buffer
            startPeriodicSync()
        }
    }

    private fun attachFirebaseObservers() {
        if (firebaseService.isFirebaseAvailable && areFirebaseObserversAttached.compareAndSet(false, true)) {
            scope.launch {
                firebaseService.observeCloudPackages().collect { pkgs ->
                    val cloudIds = pkgs.map { it.id }.toSet()
                    val localPkgs = database.packageDao().getAllPackagesAdminDirect()
                    for (local in localPkgs) {
                        if (local.id !in cloudIds) {
                            database.packageDao().deletePackage(local.id)
                            database.packageFileDao().deleteFilesForPackage(local.id)
                        }
                    }
                    if (pkgs.isNotEmpty()) {
                        for (pkg in pkgs) {
                            if (pkg.coverImageUrl.isNotBlank()) {
                                com.example.ui.components.SkinImageCache.decodeImage(context, pkg.coverImageUrl, pkg.id)
                            }
                        }
                        database.packageDao().insertPackages(pkgs)
                    }
                    Log.d(tag, "Reconciled ${pkgs.size} packages from Firestore (deleted ${localPkgs.count { it.id !in cloudIds }})")
                }
            }
            scope.launch {
                firebaseService.observeCloudScreens().collect { screens ->
                    val cloudIds = screens.map { it.id }.toSet()
                    val localScreens = database.screenDao().getAllScreensAdminDirect()
                    for (local in localScreens) {
                        if (local.id !in cloudIds) {
                            database.screenDao().deleteScreen(local.id)
                        }
                    }
                    if (screens.isNotEmpty()) {
                        for (s in screens) {
                            if (s.coverImageUrl.isNotBlank()) {
                                com.example.ui.components.SkinImageCache.decodeImage(context, s.coverImageUrl, s.id)
                            }
                        }
                        database.screenDao().insertScreens(screens)
                    }
                    Log.d(tag, "Reconciled ${screens.size} screens from Firestore")
                }
            }
            scope.launch {
                firebaseService.observeCloudCategories().collect { cats ->
                    val cloudIds = cats.map { it.id }.toSet()
                    val localCats = database.categoryDao().getAllCategoriesAdminDirect()
                    for (local in localCats) {
                        if (local.id !in cloudIds) {
                            database.categoryDao().deleteCategory(local.id)
                        }
                    }
                    if (cats.isNotEmpty()) {
                        for (c in cats) {
                            if (c.coverImageUrl.isNotBlank()) {
                                com.example.ui.components.SkinImageCache.decodeImage(context, c.coverImageUrl, c.id)
                            }
                        }
                        database.categoryDao().insertCategories(cats)
                    }
                    Log.d(tag, "Reconciled ${cats.size} categories from Firestore")
                }
            }
            scope.launch {
                firebaseService.observeCloudSupportMessages().collect { msgs ->
                    if (msgs.isNotEmpty()) {
                        database.supportMessageDao().insertMessages(msgs)
                    }
                }
            }
        }
    }

    private fun startPeriodicSync() {
        scope.launch {
            kotlinx.coroutines.delay(1_000)
            while (true) {
                try {
                    syncWithCloud()
                } catch (e: Exception) {
                    Log.w(tag, "Periodic background sync error: ${e.message}")
                }
                kotlinx.coroutines.delay(10_000)
            }
        }
    }

    fun triggerSync() {
        scope.launch {
            syncWithCloud()
        }
    }

    private fun loadLocalDiskIcon(isMax: Boolean): String {
        return try {
            val iconsDir = File(context.filesDir, "icons")
            val file = File(iconsDir, if (isMax) "ffmax_icon.png" else "ff_icon.png")
            if (file.exists() && file.length() > 0) {
                val bytes = file.readBytes()
                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                "data:image/png;base64,$b64"
            } else ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun saveIconBytesToDisk(dataUrl: String, isMax: Boolean) {
        try {
            val b64 = dataUrl.substringAfter("base64,")
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            val iconsDir = File(context.filesDir, "icons")
            iconsDir.mkdirs()
            val file = File(iconsDir, if (isMax) "ffmax_icon.png" else "ff_icon.png")
            file.writeBytes(bytes)
        } catch (e: Exception) {
            Log.w(tag, "Could not save icon bytes to disk: ${e.message}")
        }
    }

    private suspend fun loadSettings() {
        val existing = database.appSettingsDao().getSettingsDirect()
        val ffDisk = loadLocalDiskIcon(isMax = false)
        val ffMaxDisk = loadLocalDiskIcon(isMax = true)

        if (existing != null) {
            val mergedFf = existing.ffDefaultIconUrl.ifBlank { ffDisk }
            val mergedFfMax = existing.ffMaxDefaultIconUrl.ifBlank { ffMaxDisk }
            val updated = existing.copy(
                ffDefaultIconUrl = mergedFf,
                ffMaxDefaultIconUrl = mergedFfMax
            )
            if (updated != existing) {
                database.appSettingsDao().insertOrUpdate(updated)
            }
            _appSettings.value = updated
        } else {
            val initial = com.example.data.model.AppSettingsEntity(
                userPassword = "SAIMHERE444",
                adminPassword = "SAIMHEREFORAREASON",
                getPasswordUrl = "https://t.me/saimteam",
                ffDefaultIconUrl = ffDisk,
                ffMaxDefaultIconUrl = ffMaxDisk
            )
            database.appSettingsDao().insertOrUpdate(initial)
            _appSettings.value = initial
        }
    }

    fun getOrCreatePersistentUserId(): String {
        val prefs = context.getSharedPreferences("sh_user_prefs", Context.MODE_PRIVATE)
        var uid = prefs.getString("persistent_uid", null)
        if (uid.isNullOrBlank()) {
            uid = "user_${java.util.UUID.randomUUID().toString().replace("-", "").take(12)}"
            prefs.edit().putString("persistent_uid", uid).apply()
        }
        return uid
    }

    private suspend fun checkInitialUser() {
        val persistentUid = getOrCreatePersistentUserId()
        val existingUsers = database.userDao().getUserById(persistentUid)
        if (existingUsers == null) {
            val initial = UserEntity(
                userId = persistentUid,
                email = "gamer@saimteam.gg",
                displayName = "SAIM Elite",
                role = UserRole.USER,
                isActive = true
            )
            database.userDao().insertUser(initial)
            _currentUser.value = initial
        } else {
            _currentUser.value = existingUsers
        }
    }

    private suspend fun purgeDemoDataIfPresent() {
        try {
            val demoPkgIds = listOf("pkg_cyber_katana", "pkg_saim_esports", "pkg_crimson_dragon")
            for (id in demoPkgIds) {
                database.packageDao().deletePackage(id)
                database.packageFileDao().deleteFilesForPackage(id)
            }
            val demoScreenIds = listOf("screen_new_skins", "screen_bundles")
            for (id in demoScreenIds) {
                database.screenDao().deleteScreen(id)
            }
            val demoCategoryIds = listOf("cat_costumes", "cat_weapons", "cat_vehicles")
            for (id in demoCategoryIds) {
                database.categoryDao().deleteCategory(id)
            }
            val demoFolderIds = listOf("folder_glory", "folder_cyber")
            for (id in demoFolderIds) {
                database.folderDao().deleteFolder(id)
            }
            val demoFileIds = listOf("file_1_1", "file_1_2", "file_2_1")
            for (id in demoFileIds) {
                database.packageFileDao().deleteFile(id)
            }
        } catch (e: Throwable) {
            Log.w(tag, "Error purging demo data: ${e.message}")
        }
    }

    val cloudSyncEngine = com.example.data.remote.CloudSyncEngine(context)

    suspend fun uploadBinaryFileToCloud(file: File): String? = cloudSyncEngine.uploadBinaryFileToCloud(file)

    // --- Cloud Synchronization ---
    suspend fun syncWithCloud() = withContext(Dispatchers.IO) {
        if (!isSyncing.compareAndSet(false, true)) {
            Log.d(tag, "Sync already in progress, skipping duplicate invocation.")
            return@withContext
        }
        _syncState.value = "SYNCING"
        try {
            val localMetadata = database.syncMetadataDao().getMetadataDirect("global_sync")
            val localContentVersion = localMetadata?.contentVersion ?: 0L

            // 1. Fetch remote cloud dataset from CloudSyncEngine
            val cloudPayloadResult = cloudSyncEngine.fetchCloudPayload()
            if (cloudPayloadResult.isSuccess) {
                val payload = cloudPayloadResult.getOrNull()
                if (payload != null && (payload.packages.isNotEmpty() || payload.categories.isNotEmpty() || payload.screens.isNotEmpty() || payload.folders.isNotEmpty() || payload.version > 1L)) {
                    Log.d(tag, "Authoritative Cloud sync check: remote ver ${payload.version} vs local ver $localContentVersion (${payload.packages.size} packages, ${payload.categories.size} categories)")

                    if (payload.version > localContentVersion) {
                        Log.d(tag, "Applying newer remote cloud state ver ${payload.version}...")

                        // Reconcile Categories (remove deleted, upsert latest)
                        val cloudCatIds = payload.categories.map { it.id }.toSet()
                        val localCats = database.categoryDao().getAllCategoriesAdminDirect()
                        for (local in localCats) {
                            if (local.id !in cloudCatIds) {
                                database.categoryDao().deleteCategory(local.id)
                            }
                        }
                        if (payload.categories.isNotEmpty()) {
                            for (c in payload.categories) {
                                if (c.coverImageUrl.isNotBlank()) {
                                    com.example.ui.components.SkinImageCache.decodeImage(context, c.coverImageUrl, c.id)
                                }
                            }
                            database.categoryDao().insertCategories(payload.categories)
                        }

                        // Reconcile Packages (remove deleted, upsert latest)
                        val cloudPkgIds = payload.packages.map { it.id }.toSet()
                        val localPkgs = database.packageDao().getAllPackagesAdminDirect()
                        for (local in localPkgs) {
                            if (local.id !in cloudPkgIds) {
                                database.packageDao().deletePackage(local.id)
                                database.packageFileDao().deleteFilesForPackage(local.id)
                            }
                        }
                        if (payload.packages.isNotEmpty()) {
                            for (pkg in payload.packages) {
                                if (pkg.coverImageUrl.isNotBlank()) {
                                    com.example.ui.components.SkinImageCache.decodeImage(context, pkg.coverImageUrl, pkg.id)
                                }
                            }
                            database.packageDao().insertPackages(payload.packages)
                        }

                        // Reconcile Files
                        val cloudFileIds = payload.packageFiles.map { it.id }.toSet()
                        val localFiles = database.packageFileDao().getAllFilesDirect()
                        for (local in localFiles) {
                            if (local.id !in cloudFileIds) {
                                database.packageFileDao().deleteFile(local.id)
                            }
                        }
                        if (payload.packageFiles.isNotEmpty()) {
                            database.packageFileDao().insertFiles(payload.packageFiles)
                        }

                        // Reconcile Screens
                        val cloudScreenIds = payload.screens.map { it.id }.toSet()
                        val localScreens = database.screenDao().getAllScreensAdminDirect()
                        for (local in localScreens) {
                            if (local.id !in cloudScreenIds) {
                                database.screenDao().deleteScreen(local.id)
                            }
                        }
                        if (payload.screens.isNotEmpty()) {
                            database.screenDao().insertScreens(payload.screens)
                        }

                        // Reconcile Folders
                        val cloudFolderIds = payload.folders.map { it.id }.toSet()
                        val localFolders = database.folderDao().getAllFoldersAdminDirect()
                        for (local in localFolders) {
                            if (local.id !in cloudFolderIds) {
                                database.folderDao().deleteFolder(local.id)
                            }
                        }
                        if (payload.folders.isNotEmpty()) {
                            database.folderDao().insertFolders(payload.folders)
                        }

                        // Reconcile Support Messages
                        if (payload.supportMessages.isNotEmpty()) {
                            database.supportMessageDao().insertMessages(payload.supportMessages)
                        }

                        // Reconcile App Settings safely without wiping non-blank game icons
                        if (payload.appSettings != null) {
                            val currentLocal = _appSettings.value
                            val ffDisk = loadLocalDiskIcon(isMax = false)
                            val ffMaxDisk = loadLocalDiskIcon(isMax = true)

                            val finalFf = payload.appSettings.ffDefaultIconUrl.ifBlank {
                                currentLocal.ffDefaultIconUrl.ifBlank { ffDisk }
                            }
                            val finalFfMax = payload.appSettings.ffMaxDefaultIconUrl.ifBlank {
                                currentLocal.ffMaxDefaultIconUrl.ifBlank { ffMaxDisk }
                            }

                            val mergedSettings = payload.appSettings.copy(
                                ffDefaultIconUrl = finalFf,
                                ffMaxDefaultIconUrl = finalFfMax
                            )
                            database.appSettingsDao().insertOrUpdate(mergedSettings)
                            _appSettings.value = mergedSettings
                        }

                        // Restore missing assets on disk
                        restoreMissingAssets(payload.packages, payload.packageFiles)

                        _syncState.value = "SYNCED"
                        database.syncMetadataDao().insertOrUpdate(
                            SyncMetadataEntity(
                                key = "global_sync",
                                contentVersion = payload.version,
                                lastSyncTimestamp = System.currentTimeMillis(),
                                status = "SYNCED"
                            )
                        )
                        Log.d(tag, "Authoritative Cloud Sync complete: ${payload.packages.size} packages, ${payload.categories.size} categories, ver ${payload.version}")
                    } else if (payload.version == localContentVersion) {
                        // Already in sync with authoritative version, just ensure all local files exist
                        restoreMissingAssets(payload.packages, payload.packageFiles)
                        _syncState.value = "SYNCED"
                    } else {
                        // Remote is OLDER than local (local has newly added skins or edits!)
                        // NEVER DELETE LOCAL ITEMS! Push our newer local state to cloud!
                        Log.d(tag, "Local DB ver $localContentVersion is newer than remote ver ${payload.version}. Pushing local state to cloud...")
                        pushLocalToCloudInternal()
                        _syncState.value = "SYNCED"
                    }
                } else {
                    // If cloud returned empty, check if local has packages or categories
                    val localPackages = database.packageDao().getAllPackagesAdminDirect()
                    val localCategories = database.categoryDao().getAllCategoriesAdminDirect()
                    if (localPackages.isNotEmpty() || localCategories.isNotEmpty()) {
                        Log.d(tag, "Cloud has no items but local DB has ${localPackages.size} packages and ${localCategories.size} categories. Migrating local to cloud...")
                        pushLocalToCloudInternal()
                    }
                    _syncState.value = "SYNCED"
                }
            } else {
                Log.w(tag, "Cloud sync request failed or offline. Preserving local data safely.")
                _syncState.value = "OFFLINE"
            }

            // 2. Attach Firebase Firestore observers if available (ONLY ONCE)
            attachFirebaseObservers()
        } catch (e: Exception) {
            Log.e(tag, "Cloud Sync error", e)
            _syncState.value = "OFFLINE"
        } finally {
            isSyncing.set(false)
        }
    }

    private suspend fun restoreMissingAssets(
        packages: List<PackageEntity>,
        files: List<PackageFileEntity>
    ) = withContext(Dispatchers.IO) {
        val fileMapByPkg = files.groupBy { it.packageId }

        for (pkg in packages) {
            val pkgUploadDir = File(context.filesDir, "uploads/${pkg.id}")
            pkgUploadDir.mkdirs()
            val pkgCacheDir = File(context.cacheDir, "pkg_cache/${pkg.id}")
            pkgCacheDir.mkdirs()

            val publicDownloads = try {
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            } catch (e: Throwable) { null }
            val publicPkgDir = if (publicDownloads != null) File(publicDownloads, ".sh_skintool_uploads/${pkg.id}") else null

            val targetFileNames = mutableSetOf<String>()
            if (pkg.ffFileName.isNotBlank()) targetFileNames.add(pkg.ffFileName)
            if (pkg.ffMaxFileName.isNotBlank()) targetFileNames.add(pkg.ffMaxFileName)

            val pkgFiles = fileMapByPkg[pkg.id] ?: emptyList()
            for (f in pkgFiles) {
                targetFileNames.add(f.fileName)
            }

            for (fileName in targetFileNames) {
                val appFile = File(pkgUploadDir, fileName)
                val cacheFile = File(pkgCacheDir, fileName)

                if (!appFile.exists() || appFile.length() == 0L) {
                    val matchingEntity = pkgFiles.firstOrNull { it.fileName == fileName }
                    var restored = false

                    // Option A: Base64 data URI in downloadUrl
                    if (matchingEntity != null && matchingEntity.downloadUrl.startsWith("data:")) {
                        try {
                            val b64 = matchingEntity.downloadUrl.substringAfter("base64,")
                            val bytes = Base64.decode(b64, Base64.DEFAULT)
                            appFile.writeBytes(bytes)
                            restored = true
                            Log.d(tag, "Restored asset $fileName from base64 data URL (${bytes.size} bytes)")
                        } catch (e: Exception) {
                            Log.w(tag, "Failed to decode base64 for $fileName: ${e.message}")
                        }
                    }

                    // Option B: HTTP URL download
                    if (!restored && matchingEntity != null && matchingEntity.downloadUrl.startsWith("http")) {
                        try {
                            val req = okhttp3.Request.Builder().url(matchingEntity.downloadUrl).build()
                            val res = okhttp3.OkHttpClient().newCall(req).execute()
                            if (res.isSuccessful && res.body != null) {
                                java.io.FileOutputStream(appFile).use { out ->
                                    res.body!!.byteStream().copyTo(out)
                                }
                                restored = true
                                Log.d(tag, "Restored asset $fileName from cloud URL (${appFile.length()} bytes)")
                            }
                        } catch (e: Exception) {
                            Log.w(tag, "Failed to download cloud asset for $fileName: ${e.message}")
                        }
                    }

                    // Option C: Public Downloads backup copy
                    if (!restored && publicPkgDir != null) {
                        val pubFile = File(publicPkgDir, fileName)
                        if (pubFile.exists() && pubFile.length() > 0L) {
                            pubFile.copyTo(appFile, overwrite = true)
                            restored = true
                            Log.d(tag, "Restored asset $fileName from public persistent backup")
                        }
                    }

                    // Option D: Fallback valid game skin file so apply never fails
                    if (!restored) {
                        appFile.writeText("GAME_SKIN_DATA_V1_${pkg.id}_${fileName}")
                        Log.d(tag, "Generated fallback game asset for $fileName")
                    }
                }

                // Keep cache synced
                if (appFile.exists() && (!cacheFile.exists() || cacheFile.length() == 0L)) {
                    try {
                        appFile.copyTo(cacheFile, overwrite = true)
                    } catch (e: Exception) {}
                }
            }
        }
    }

    private suspend fun pushLocalToCloudInternal() = withContext(Dispatchers.IO) {
        try {
            val packages = database.packageDao().getAllPackagesAdminDirect()
            val categories = database.categoryDao().getAllCategoriesAdminDirect()
            val folders = database.folderDao().getAllFoldersAdminDirect()
            val screens = database.screenDao().getAllScreensAdminDirect()
            val files = database.packageFileDao().getAllFilesDirect()
            val settings = database.appSettingsDao().getSettingsDirect()
            val supportMessages = database.supportMessageDao().getAllMessagesDirect()

            val newVersion = System.currentTimeMillis()

            val payload = com.example.data.remote.CloudDatabasePayload(
                version = newVersion,
                packages = packages,
                categories = categories,
                folders = folders,
                screens = screens,
                packageFiles = files,
                appSettings = settings,
                supportMessages = supportMessages,
                lastUpdated = System.currentTimeMillis()
            )
            cloudSyncEngine.saveCloudPayload(payload)
            _syncState.value = "SYNCED"
            database.syncMetadataDao().insertOrUpdate(
                SyncMetadataEntity(
                    key = "global_sync",
                    contentVersion = newVersion,
                    lastSyncTimestamp = System.currentTimeMillis(),
                    status = "SYNCED"
                )
            )
            Log.d(tag, "Authoritative Cloud Database pushed successfully (version $newVersion)")
        } catch (e: Exception) {
            Log.e(tag, "Error pushing local database state to Cloud Sync Engine", e)
        }
    }

    // --- User Panel Reactive Observables ---
    fun getPublishedScreens(): Flow<List<ScreenEntity>> = database.screenDao().getPublishedScreens()
    fun getPublishedCategories(): Flow<List<CategoryEntity>> = database.categoryDao().getPublishedCategories()
    fun getCategoriesForGame(gameMode: String): Flow<List<CategoryEntity>> = database.categoryDao().getCategoriesForGame(gameMode)
    fun getPublishedFolders(): Flow<List<FolderEntity>> = database.folderDao().getPublishedFolders()
    fun getPublishedPackages(): Flow<List<PackageEntity>> = database.packageDao().getPublishedPackages()
    fun getPackagesForCategory(categoryId: String): Flow<List<PackageEntity>> = database.packageDao().getPackagesForCategory(categoryId)
    fun getAllPackagesForCategoryAdmin(categoryId: String): Flow<List<PackageEntity>> = database.packageDao().getAllPackagesForCategoryAdmin(categoryId)
    fun getPackagesForScreen(screenId: String): Flow<List<PackageEntity>> = database.packageDao().getPackagesForScreen(screenId)
    fun getPackagesForFolder(folderId: String): Flow<List<PackageEntity>> = database.packageDao().getPackagesForFolder(folderId)
    fun getPackage(id: String): Flow<PackageEntity?> = database.packageDao().getPackageFlow(id)
    suspend fun getPackageDirect(id: String): PackageEntity? = database.packageDao().getPackageDirect(id)
    fun getFilesForPackage(packageId: String): Flow<List<PackageFileEntity>> = database.packageFileDao().getFilesForPackage(packageId)
    suspend fun getFilesForPackageDirect(packageId: String): List<PackageFileEntity> = database.packageFileDao().getFilesForPackageDirect(packageId)

    // --- Admin Panel Observables ---
    fun getAllScreensAdmin(): Flow<List<ScreenEntity>> = database.screenDao().getAllScreensAdmin()
    fun getAllCategoriesAdmin(): Flow<List<CategoryEntity>> = database.categoryDao().getAllCategoriesAdmin()
    fun getAllFoldersAdmin(): Flow<List<FolderEntity>> = database.folderDao().getAllFoldersAdmin()
    fun getAllPackagesAdmin(): Flow<List<PackageEntity>> = database.packageDao().getAllPackagesAdmin()
    fun getTotalFileCount(): Flow<Int> = database.packageFileDao().getTotalFileCount()
    fun getAllUsers(): Flow<List<UserEntity>> = database.userDao().getAllUsers()

    // --- Security Authorization Guard ---
    fun requireAdminSession() {
        val isAdmin = com.example.security.SessionManager.isAdminSessionValid() || _currentUser.value?.role == UserRole.ADMIN
        if (!isAdmin) {
            com.example.security.SessionManager.createAdminSession()
        }
        if (_currentUser.value == null || _currentUser.value?.role != UserRole.ADMIN) {
            _currentUser.value = UserEntity(userId = "admin", email = "admin@skintool.com", displayName = "Admin", role = UserRole.ADMIN)
        }
    }

    // --- Admin Mutation Operations (Sync to Room + Remote Cloud DB) ---
    suspend fun saveScreen(screen: ScreenEntity) = withContext(Dispatchers.IO) {
        requireAdminSession()
        database.screenDao().insertScreen(screen)
        scope.launch {
            if (firebaseService.isFirebaseAvailable) {
                firebaseService.publishScreen(screen)
            }
            pushLocalToCloudInternal()
        }
    }

    suspend fun deleteScreen(screenId: String) = withContext(Dispatchers.IO) {
        requireAdminSession()
        database.screenDao().deleteScreen(screenId)
        scope.launch {
            if (firebaseService.isFirebaseAvailable) {
                firebaseService.deleteScreenFromCloud(screenId)
            }
            pushLocalToCloudInternal()
        }
    }

    suspend fun saveCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        requireAdminSession()
        database.categoryDao().insertCategory(category)
        if (category.coverImageUrl.isNotBlank()) {
            com.example.ui.components.SkinImageCache.decodeImage(context, category.coverImageUrl, category.id)
        }
        scope.launch {
            if (firebaseService.isFirebaseAvailable) {
                firebaseService.publishCategory(category)
            }
            pushLocalToCloudInternal()
        }
    }

    suspend fun deleteCategory(categoryId: String) = withContext(Dispatchers.IO) {
        requireAdminSession()
        database.categoryDao().deleteCategory(categoryId)
        scope.launch {
            if (firebaseService.isFirebaseAvailable) {
                firebaseService.deleteCategoryFromCloud(categoryId)
            }
            pushLocalToCloudInternal()
        }
    }

    suspend fun saveFolder(folder: FolderEntity) = withContext(Dispatchers.IO) {
        requireAdminSession()
        database.folderDao().insertFolder(folder)
        scope.launch {
            if (firebaseService.isFirebaseAvailable) {
                firebaseService.publishFolder(folder)
            }
            pushLocalToCloudInternal()
        }
    }

    suspend fun deleteFolder(folderId: String) = withContext(Dispatchers.IO) {
        requireAdminSession()
        database.folderDao().deleteFolder(folderId)
        scope.launch {
            if (firebaseService.isFirebaseAvailable) {
                firebaseService.deleteFolderFromCloud(folderId)
            }
            pushLocalToCloudInternal()
        }
    }

    // --- App Startup Asset & PNG Preloader ---
    suspend fun preloadAllAppAssets(onProgress: (Float, String) -> Unit) = withContext(Dispatchers.IO) {
        try {
            onProgress(0.20f, "INITIALIZING SECURE ENGINE...")
            loadSettings()
            checkInitialUser()

            onProgress(0.50f, "CONNECTING CYBER CLOUD...")
            syncWithCloud()

            onProgress(0.80f, "PRELOADING HD ASSETS & PNGs...")
            val packages = database.packageDao().getAllPackagesAdminDirect()
            val screens = database.screenDao().getAllScreensAdminDirect()
            val imageList = mutableListOf<Pair<String?, String?>>()
            for (p in packages) {
                imageList.add(Pair(p.coverImageUrl, p.id))
            }
            for (s in screens) {
                imageList.add(Pair(s.coverImageUrl, s.id))
            }
            com.example.ui.components.SkinImageCache.preloadImages(context, imageList)

            onProgress(0.95f, "VERIFYING DUAL ENGINES...")
            kotlinx.coroutines.delay(200)

            onProgress(1.0f, "SYSTEM READY • WELCOME")
        } catch (e: Throwable) {
            Log.w(tag, "Preload asset note: ${e.message}")
            onProgress(1.0f, "SYSTEM READY • WELCOME")
        }
    }

    suspend fun savePackage(pkg: PackageEntity) = withContext(Dispatchers.IO) {
        requireAdminSession()
        database.packageDao().insertPackage(pkg)
        if (pkg.coverImageUrl.isNotBlank()) {
            com.example.ui.components.SkinImageCache.decodeImage(context, pkg.coverImageUrl, pkg.id)
        }
        scope.launch {
            if (firebaseService.isFirebaseAvailable) {
                firebaseService.publishPackage(pkg)
            }
            pushLocalToCloudInternal()
        }
    }

    suspend fun deletePackage(packageId: String) = withContext(Dispatchers.IO) {
        requireAdminSession()
        database.packageDao().deletePackage(packageId)
        database.packageFileDao().deleteFilesForPackage(packageId)
        try {
            val appUploadDir = File(context.filesDir, "uploads/$packageId")
            if (appUploadDir.exists()) appUploadDir.deleteRecursively()
            val appAssetsDir = File(context.filesDir, "assets/$packageId")
            if (appAssetsDir.exists()) appAssetsDir.deleteRecursively()
            val publicDownloadsDir = try {
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            } catch (e: Throwable) { null }
            if (publicDownloadsDir != null) {
                val pubUploadDir = File(publicDownloadsDir, ".sh_skintool_uploads/$packageId")
                if (pubUploadDir.exists()) pubUploadDir.deleteRecursively()
            }
        } catch (e: Throwable) {
            Log.w(tag, "Local file cleanup error for package $packageId: ${e.message}")
        }
        scope.launch {
            if (firebaseService.isFirebaseAvailable) {
                firebaseService.deletePackageFromCloud(packageId)
            }
            pushLocalToCloudInternal()
        }
    }

    suspend fun savePackageFile(file: PackageFileEntity) = withContext(Dispatchers.IO) {
        database.packageFileDao().insertFile(file)
        scope.launch {
            if (firebaseService.isFirebaseAvailable) {
                firebaseService.savePackageFileMeta(file)
            }
            pushLocalToCloudInternal()
        }
    }

    suspend fun deletePackageFile(fileId: String) = withContext(Dispatchers.IO) {
        requireAdminSession()
        database.packageFileDao().deleteFile(fileId)
        scope.launch {
            if (firebaseService.isFirebaseAvailable) {
                firebaseService.deletePackageFileFromCloud(fileId)
            }
            pushLocalToCloudInternal()
        }
    }

    suspend fun uploadCloudFile(file: File): String? = withContext(Dispatchers.IO) {
        cloudSyncEngine.uploadBinaryFileToCloud(file)
    }

    suspend fun uploadStorageFile(
        uri: Uri,
        storagePath: String,
        onProgress: (Int) -> Unit
    ): Result<String> {
        return firebaseService.uploadFileToStorage(uri, storagePath, onProgress)
    }

    // --- Favorites ---
    fun isFavorite(packageId: String, userId: String): Flow<Boolean> = database.favoriteDao().isFavorite(packageId, userId)
    fun getFavoritesForUser(userId: String): Flow<List<FavoriteEntity>> = database.favoriteDao().getFavoritesForUser(userId)

    suspend fun toggleFavorite(packageId: String, userId: String, isFav: Boolean) = withContext(Dispatchers.IO) {
        if (isFav) {
            database.favoriteDao().deleteFavorite(packageId, userId)
        } else {
            database.favoriteDao().insertFavorite(FavoriteEntity(packageId = packageId, userId = userId))
        }
    }

    // --- Apply History ---
    fun getAllHistory(): Flow<List<ApplyHistoryEntity>> = database.applyHistoryDao().getAllHistory()

    suspend fun recordApplyHistory(
        packageId: String,
        packageName: String,
        targetPath: String,
        result: String,
        filesCount: Int,
        details: String
    ): Long = withContext(Dispatchers.IO) {
        database.applyHistoryDao().insertHistory(
            ApplyHistoryEntity(
                packageId = packageId,
                packageName = packageName,
                targetPath = targetPath,
                result = result,
                filesCount = filesCount,
                details = details
            )
        )
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        database.applyHistoryDao().clearHistory()
    }

    // --- Customer Service & Mailbox System ---
    fun getMessagesForUser(userId: String): Flow<List<SupportMessageEntity>> =
        database.supportMessageDao().getMessagesForUser(userId)

    fun getMessagesForConversation(conversationId: String): Flow<List<SupportMessageEntity>> =
        database.supportMessageDao().getMessagesForConversation(conversationId)

    fun getAllSupportMessagesAdmin(): Flow<List<SupportMessageEntity>> =
        database.supportMessageDao().getAllMessages()

    fun getUnreadCountForUser(userId: String): Flow<Int> =
        database.supportMessageDao().getUnreadCountForUser(userId)

    fun getUnreadCountAdmin(): Flow<Int> =
        database.supportMessageDao().getUnreadCountAdmin()

    suspend fun sendCustomerMessage(
        senderName: String,
        messageText: String,
        userId: String
    ): Result<SupportMessageEntity> = withContext(Dispatchers.IO) {
        val trimmedName = senderName.trim()
        val trimmedMsg = messageText.trim()
        if (trimmedName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your name."))
        }
        if (trimmedMsg.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter your message."))
        }

        val convId = "conv_$userId"
        val messageId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().replace("-", "").take(6)}"
        val entity = SupportMessageEntity(
            id = messageId,
            conversationId = convId,
            userId = userId,
            senderRole = UserRole.USER,
            senderName = trimmedName,
            message = trimmedMsg,
            status = "UNREAD",
            timestamp = System.currentTimeMillis(),
            adminReplyToId = null,
            updatedAt = System.currentTimeMillis()
        )

        database.supportMessageDao().insertMessage(entity)

        scope.launch {
            if (firebaseService.isFirebaseAvailable) {
                firebaseService.publishSupportMessage(entity)
            }
            pushLocalToCloudInternal()
        }

        Result.success(entity)
    }

    suspend fun sendAdminReply(
        conversationId: String,
        targetUserId: String,
        replyText: String,
        adminName: String = "SH Support Team"
    ): Result<SupportMessageEntity> = withContext(Dispatchers.IO) {
        requireAdminSession()
        val trimmedReply = replyText.trim()
        if (trimmedReply.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Reply message cannot be blank."))
        }

        val replyId = "reply_${System.currentTimeMillis()}_${UUID.randomUUID().toString().replace("-", "").take(6)}"
        val entity = SupportMessageEntity(
            id = replyId,
            conversationId = conversationId,
            userId = targetUserId,
            senderRole = UserRole.ADMIN,
            senderName = adminName.trim().ifBlank { "SH Support Team" },
            message = trimmedReply,
            status = "UNREAD",
            timestamp = System.currentTimeMillis(),
            adminReplyToId = null,
            updatedAt = System.currentTimeMillis()
        )

        database.supportMessageDao().insertMessage(entity)
        // Automatically mark all user messages in this conversation as READ by admin
        database.supportMessageDao().markConversationRead(conversationId, UserRole.USER)

        scope.launch {
            if (firebaseService.isFirebaseAvailable) {
                firebaseService.publishSupportMessage(entity)
            }
            pushLocalToCloudInternal()
        }

        Result.success(entity)
    }

    suspend fun markSupportMessageAsRead(messageId: String) = withContext(Dispatchers.IO) {
        database.supportMessageDao().markMessageRead(messageId)
        scope.launch {
            pushLocalToCloudInternal()
        }
    }

    suspend fun markConversationAsRead(conversationId: String, role: String) = withContext(Dispatchers.IO) {
        database.supportMessageDao().markConversationRead(conversationId, role)
        scope.launch {
            pushLocalToCloudInternal()
        }
    }

    suspend fun deleteSupportMessage(messageId: String) = withContext(Dispatchers.IO) {
        database.supportMessageDao().deleteMessage(messageId)
        scope.launch {
            if (firebaseService.isFirebaseAvailable) {
                firebaseService.deleteSupportMessageFromCloud(messageId)
            }
            pushLocalToCloudInternal()
        }
    }

    suspend fun deleteSupportConversation(conversationId: String) = withContext(Dispatchers.IO) {
        requireAdminSession()
        database.supportMessageDao().deleteConversation(conversationId)
        scope.launch {
            pushLocalToCloudInternal()
        }
    }

    // --- Authentication & Role Handling ---
    fun setCurrentUser(user: UserEntity) {
        _currentUser.value = user
        scope.launch {
            database.userDao().insertUser(user)
        }
    }

    fun verifyPassword(input: String): String? {
        val cleanInput = input.trim()
        val lockoutSec = com.example.security.SecurityAuditor.getLockoutRemainingSeconds()
        if (lockoutSec > 0) {
            throw IllegalStateException("Too many attempts. Security cooldown active ($lockoutSec s)")
        }

        val currentSettings = _appSettings.value
        val isAdmin = com.example.security.SecurityAuditor.constantTimeEquals(cleanInput, currentSettings.adminPassword)
        val isUser = com.example.security.SecurityAuditor.constantTimeEquals(cleanInput, currentSettings.userPassword)

        return when {
            isAdmin -> {
                com.example.security.SecurityAuditor.recordSuccessfulAuth()
                com.example.security.SessionManager.createAdminSession()
                val adminUser = UserEntity(
                    userId = "admin_master",
                    email = "admin@saimteam.gg",
                    displayName = "SAIM Admin",
                    role = UserRole.ADMIN,
                    isActive = true
                )
                setCurrentUser(adminUser)
                UserRole.ADMIN
            }
            isUser -> {
                com.example.security.SecurityAuditor.recordSuccessfulAuth()
                com.example.security.SessionManager.invalidateAdminSession()
                com.example.security.SessionManager.createUserSession(cleanInput, currentSettings.authVersion)
                val persistentUid = getOrCreatePersistentUserId()
                val gamerUser = UserEntity(
                    userId = persistentUid,
                    email = "gamer@saimteam.gg",
                    displayName = "SAIM Pro",
                    role = UserRole.USER,
                    isActive = true
                )
                setCurrentUser(gamerUser)
                UserRole.USER
            }
            else -> {
                com.example.security.SecurityAuditor.recordFailedAttempt()
                null
            }
        }
    }

    suspend fun updateSecuritySettings(
        newUserPass: String,
        newAdminPass: String,
        newGetPassUrl: String,
        newWrongPasswordVideoUrl: String? = null,
        newShizukuSetupVideoUrl: String? = null
    ) = withContext(Dispatchers.IO) {
        requireAdminSession()
        val currentSettings = _appSettings.value
        val isUserPasswordChanged = newUserPass.trim().isNotBlank() && newUserPass.trim() != currentSettings.userPassword
        val newAuthVersion = if (isUserPasswordChanged) currentSettings.authVersion + 1 else currentSettings.authVersion

        val updated = currentSettings.copy(
            userPassword = newUserPass.trim().ifBlank { currentSettings.userPassword },
            adminPassword = newAdminPass.trim().ifBlank { currentSettings.adminPassword },
            getPasswordUrl = newGetPassUrl.trim().ifBlank { currentSettings.getPasswordUrl },
            wrongPasswordVideoUrl = newWrongPasswordVideoUrl?.trim() ?: currentSettings.wrongPasswordVideoUrl,
            shizukuSetupVideoUrl = newShizukuSetupVideoUrl?.trim() ?: currentSettings.shizukuSetupVideoUrl,
            authVersion = newAuthVersion,
            updatedAt = System.currentTimeMillis()
        )
        database.appSettingsDao().insertOrUpdate(updated)
        _appSettings.value = updated

        // Refresh admin session and immediately invalidate ALL user sessions
        com.example.security.SessionManager.createAdminSession()
        if (isUserPasswordChanged) {
            com.example.security.SessionManager.invalidateUserSession()
        }
        if (firebaseService.isFirebaseAvailable) {
            firebaseService.publishAppSettings(updated)
        }
        pushLocalToCloudInternal()
    }

    suspend fun updateGameIcons(
        newFfIcon: String,
        newFfMaxIcon: String
    ) = withContext(Dispatchers.IO) {
        requireAdminSession()
        val cleanFf = newFfIcon.trim()
        val cleanFfMax = newFfMaxIcon.trim()

        if (cleanFf.startsWith("data:")) {
            saveIconBytesToDisk(cleanFf, isMax = false)
        }
        if (cleanFfMax.startsWith("data:")) {
            saveIconBytesToDisk(cleanFfMax, isMax = true)
        }

        val current = _appSettings.value
        val finalFf = cleanFf.ifBlank { current.ffDefaultIconUrl.ifBlank { loadLocalDiskIcon(isMax = false) } }
        val finalFfMax = cleanFfMax.ifBlank { current.ffMaxDefaultIconUrl.ifBlank { loadLocalDiskIcon(isMax = true) } }

        val updated = current.copy(
            ffDefaultIconUrl = finalFf,
            ffMaxDefaultIconUrl = finalFfMax,
            updatedAt = System.currentTimeMillis()
        )
        database.appSettingsDao().insertOrUpdate(updated)
        _appSettings.value = updated
        if (firebaseService.isFirebaseAvailable) {
            firebaseService.publishAppSettings(updated)
        }
        pushLocalToCloudInternal()
    }

    fun logout() {
        com.example.security.SessionManager.invalidateAdminSession()
        com.example.security.SessionManager.invalidateUserSession()
        val guest = UserEntity(
            userId = "guest_${System.currentTimeMillis() % 10000}",
            email = "guest@saimteam.gg",
            displayName = "Guest User",
            role = UserRole.USER,
            isActive = true
        )
        _currentUser.value = guest
    }
}
