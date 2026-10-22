package com.example.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.AppSettingsEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.ContentStatus
import com.example.data.model.FolderEntity
import com.example.data.model.PackageEntity
import com.example.data.model.PackageFileEntity
import com.example.data.model.ScreenEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File

private fun DocumentSnapshot.safeString(key: String, default: String = ""): String =
    get(key)?.toString() ?: default

private fun DocumentSnapshot.safeNullableString(key: String): String? =
    get(key)?.toString()?.ifBlank { null }

private fun DocumentSnapshot.safeLong(key: String, default: Long = 0L): Long =
    when (val v = get(key)) {
        is Number -> v.toLong()
        is String -> v.toLongOrNull() ?: default
        else -> default
    }

private fun DocumentSnapshot.safeInt(key: String, default: Int = 0): Int =
    when (val v = get(key)) {
        is Number -> v.toInt()
        is String -> v.toIntOrNull() ?: default
        else -> default
    }

private fun DocumentSnapshot.safeBoolean(key: String, default: Boolean = false): Boolean =
    when (val v = get(key)) {
        is Boolean -> v
        is String -> v.toBooleanStrictOrNull() ?: default
        is Number -> v.toInt() != 0
        else -> default
    }

class FirebaseService(private val context: Context) {

    private val tag = "FirebaseService"

    val isFirebaseAvailable: Boolean
        get() = try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }

    private val auth: FirebaseAuth?
        get() = if (isFirebaseAvailable) try { FirebaseAuth.getInstance() } catch (e: Exception) { null } else null

    private val firestore: FirebaseFirestore?
        get() = if (isFirebaseAvailable) try { FirebaseFirestore.getInstance() } catch (e: Exception) { null } else null

    private val storage: FirebaseStorage?
        get() = if (isFirebaseAvailable) try { FirebaseStorage.getInstance() } catch (e: Exception) { null } else null

    fun getCurrentUserId(): String? = auth?.currentUser?.uid

    suspend fun signInAnonymously(): Result<String> {
        val a = auth ?: return Result.failure(Exception("Firebase Auth not initialized"))
        return try {
            val result = a.signInAnonymously().await()
            val uid = result.user?.uid ?: return Result.failure(Exception("No user UID returned"))
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<String> {
        val a = auth ?: return Result.failure(Exception("Firebase Auth not initialized"))
        return try {
            val result = a.signInWithEmailAndPassword(email, pass).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("No user UID returned"))
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerWithEmail(email: String, pass: String): Result<String> {
        val a = auth ?: return Result.failure(Exception("Firebase Auth not initialized"))
        return try {
            val result = a.createUserWithEmailAndPassword(email, pass).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("No user UID returned"))
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserRole(userId: String): String {
        val db = firestore ?: return UserRole.USER
        return try {
            val doc = db.collection("users").document(userId).get().await()
            doc.getString("role") ?: UserRole.USER
        } catch (e: Exception) {
            UserRole.USER
        }
    }

    suspend fun saveUserProfile(user: UserEntity): Result<Unit> {
        val db = firestore ?: return Result.success(Unit)
        return try {
            val data = mapOf(
                "userId" to user.userId,
                "email" to user.email,
                "displayName" to user.displayName,
                "role" to user.role,
                "isActive" to user.isActive,
                "createdAt" to user.createdAt,
                "lastActive" to System.currentTimeMillis()
            )
            db.collection("users").document(user.userId).set(data, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Dynamic Content Publishing & Synchronization ---

    suspend fun publishScreen(screen: ScreenEntity): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val map = mapOf(
                "id" to screen.id,
                "title" to screen.title,
                "description" to screen.description,
                "coverImageUrl" to screen.coverImageUrl,
                "categoryId" to screen.categoryId,
                "folderId" to screen.folderId,
                "displayOrder" to screen.displayOrder,
                "status" to screen.status,
                "updatedAt" to System.currentTimeMillis(),
                "version" to screen.version + 1
            )
            db.collection("screens").document(screen.id).set(map, SetOptions.merge()).await()
            incrementGlobalVersion()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteScreenFromCloud(screenId: String): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection("screens").document(screenId).delete().await()
            incrementGlobalVersion()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun publishCategory(category: CategoryEntity): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val map = mapOf(
                "id" to category.id,
                "name" to category.name,
                "description" to category.description,
                "coverImageUrl" to category.coverImageUrl,
                "gameMode" to category.gameMode,
                "displayOrder" to category.displayOrder,
                "status" to category.status,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection("categories").document(category.id).set(map, SetOptions.merge()).await()
            incrementGlobalVersion()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCategoryFromCloud(categoryId: String): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection("categories").document(categoryId).delete().await()
            incrementGlobalVersion()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun publishFolder(folder: FolderEntity): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val map = mapOf(
                "id" to folder.id,
                "name" to folder.name,
                "description" to folder.description,
                "coverImageUrl" to folder.coverImageUrl,
                "parentFolderId" to folder.parentFolderId,
                "displayOrder" to folder.displayOrder,
                "status" to folder.status,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection("folders").document(folder.id).set(map, SetOptions.merge()).await()
            incrementGlobalVersion()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFolderFromCloud(folderId: String): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection("folders").document(folderId).delete().await()
            incrementGlobalVersion()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun publishPackage(pkg: PackageEntity): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val map = mutableMapOf<String, Any>(
                "id" to pkg.id,
                "name" to pkg.name,
                "description" to pkg.description,
                "coverImageUrl" to pkg.coverImageUrl,
                "version" to pkg.version,
                "fileSize" to pkg.fileSize,
                "displayOrder" to pkg.displayOrder,
                "status" to pkg.status,
                "isFeatured" to pkg.isFeatured,
                "ffFileName" to pkg.ffFileName,
                "ffMaxFileName" to pkg.ffMaxFileName,
                "iconPngUrl" to pkg.iconPngUrl,
                "ffIconPngUrl" to pkg.ffIconPngUrl,
                "ffMaxIconPngUrl" to pkg.ffMaxIconPngUrl,
                "createdAt" to pkg.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )
            pkg.categoryId?.let { if (it.isNotBlank()) map["categoryId"] = it }
            pkg.screenId?.let { if (it.isNotBlank()) map["screenId"] = it }
            pkg.folderId?.let { if (it.isNotBlank()) map["folderId"] = it }

            db.collection("packages").document(pkg.id).set(map, SetOptions.merge()).await()
            incrementGlobalVersion()
            Log.d(tag, "Successfully published package ${pkg.id} to Firestore (category: ${pkg.categoryId})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error publishing package ${pkg.id} to Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun deletePackageFromCloud(packageId: String): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection("packages").document(packageId).delete().await()
            // delete files metadata under this package
            val filesSnapshot = db.collection("package_files").whereEqualTo("packageId", packageId).get().await()
            for (doc in filesSnapshot.documents) {
                doc.reference.delete().await()
            }
            incrementGlobalVersion()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePackageFileMeta(fileEntity: PackageFileEntity): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val map = mapOf(
                "id" to fileEntity.id,
                "packageId" to fileEntity.packageId,
                "fileName" to fileEntity.fileName,
                "relativePath" to fileEntity.relativePath,
                "downloadUrl" to fileEntity.downloadUrl,
                "fileSize" to fileEntity.fileSize,
                "checksum" to fileEntity.checksum,
                "updatedAt" to fileEntity.updatedAt
            )
            db.collection("package_files").document(fileEntity.id).set(map, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePackageFileFromCloud(fileId: String): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            db.collection("package_files").document(fileId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Firebase Storage Uploads ---

    suspend fun uploadFileToStorage(
        localUri: Uri,
        storagePath: String,
        onProgress: (Int) -> Unit
    ): Result<String> {
        val st = storage ?: return Result.failure(Exception("Firebase Storage not initialized"))
        return try {
            val ref = st.reference.child(storagePath)
            val uploadTask = ref.putFile(localUri)
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = if (taskSnapshot.totalByteCount > 0) {
                    ((100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount).toInt()
                } else {
                    0
                }
                onProgress(progress)
            }
            uploadTask.await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun publishAppSettings(settings: AppSettingsEntity): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firestore not initialized"))
        return try {
            val map = mapOf(
                "id" to settings.id,
                "userPassword" to settings.userPassword,
                "adminPassword" to settings.adminPassword,
                "getPasswordUrl" to settings.getPasswordUrl,
                "targetDirectory" to settings.targetDirectory,
                "ffDefaultIconUrl" to settings.ffDefaultIconUrl,
                "ffMaxDefaultIconUrl" to settings.ffMaxDefaultIconUrl,
                "wrongPasswordVideoUrl" to settings.wrongPasswordVideoUrl,
                "shizukuSetupVideoUrl" to settings.shizukuSetupVideoUrl,
                "authVersion" to settings.authVersion,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection("app_settings").document(settings.id).set(map, SetOptions.merge()).await()
            incrementGlobalVersion()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Real-time snapshot listeners for User Panel Synchronization ---

    fun observeCloudScreens(): Flow<List<ScreenEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }
        val listener: ListenerRegistration = db.collection("screens")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to screens", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val id = doc.safeString("id", doc.id)
                        if (id.isBlank()) return@mapNotNull null
                        ScreenEntity(
                            id = id,
                            title = doc.safeString("title"),
                            description = doc.safeString("description"),
                            coverImageUrl = doc.safeString("coverImageUrl"),
                            categoryId = doc.safeNullableString("categoryId"),
                            folderId = doc.safeNullableString("folderId"),
                            displayOrder = doc.safeInt("displayOrder", 0),
                            status = doc.safeString("status", ContentStatus.PUBLISHED),
                            updatedAt = doc.safeLong("updatedAt", System.currentTimeMillis()),
                            version = doc.safeLong("version", 1L)
                        )
                    } catch (e: Exception) {
                        Log.e(tag, "Error parsing screen doc ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun observeCloudPackages(): Flow<List<PackageEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }
        val listener: ListenerRegistration = db.collection("packages")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to packages", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val id = doc.safeString("id", doc.id)
                        if (id.isBlank()) return@mapNotNull null
                        PackageEntity(
                            id = id,
                            name = doc.safeString("name", "Skin Package"),
                            description = doc.safeString("description"),
                            coverImageUrl = doc.safeString("coverImageUrl"),
                            screenId = doc.safeNullableString("screenId"),
                            categoryId = doc.safeNullableString("categoryId"),
                            folderId = doc.safeNullableString("folderId"),
                            version = doc.safeString("version", "1.0"),
                            fileSize = doc.safeLong("fileSize", 0L),
                            displayOrder = doc.safeInt("displayOrder", 0),
                            status = doc.safeString("status", ContentStatus.PUBLISHED),
                            isFeatured = doc.safeBoolean("isFeatured", false),
                            ffFileName = doc.safeString("ffFileName"),
                            ffMaxFileName = doc.safeString("ffMaxFileName"),
                            iconPngUrl = doc.safeString("iconPngUrl"),
                            ffIconPngUrl = doc.safeString("ffIconPngUrl"),
                            ffMaxIconPngUrl = doc.safeString("ffMaxIconPngUrl"),
                            createdAt = doc.safeLong("createdAt", System.currentTimeMillis()),
                            updatedAt = doc.safeLong("updatedAt", System.currentTimeMillis())
                        )
                    } catch (e: Exception) {
                        Log.e(tag, "Error parsing package doc ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                Log.d(tag, "Received ${list.size} packages from Firestore snapshot")
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    fun observeCloudCategories(): Flow<List<CategoryEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }
        val listener: ListenerRegistration = db.collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to categories", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val id = doc.safeString("id", doc.id)
                        if (id.isBlank()) return@mapNotNull null
                        CategoryEntity(
                            id = id,
                            name = doc.safeString("name", "Category"),
                            description = doc.safeString("description"),
                            coverImageUrl = doc.safeString("coverImageUrl"),
                            gameMode = doc.safeString("gameMode", "FREE_FIRE"),
                            displayOrder = doc.safeInt("displayOrder", 0),
                            status = doc.safeString("status", ContentStatus.PUBLISHED),
                            updatedAt = doc.safeLong("updatedAt", System.currentTimeMillis())
                        )
                    } catch (e: Exception) {
                        Log.e(tag, "Error parsing category doc ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                Log.d(tag, "Received ${list.size} categories from Firestore snapshot")
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    private suspend fun incrementGlobalVersion() {
        val db = firestore ?: return
        try {
            val metaRef = db.collection("system_meta").document("global_sync")
            db.runTransaction { transaction ->
                val snapshot = transaction.get(metaRef)
                val currentVer = snapshot.getLong("contentVersion") ?: 1L
                transaction.set(
                    metaRef,
                    mapOf(
                        "contentVersion" to currentVer + 1,
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
            }.await()
        } catch (e: Exception) {
            Log.e(tag, "Error incrementing global version", e)
        }
    }

    suspend fun publishSupportMessage(message: com.example.data.model.SupportMessageEntity): Result<Unit> {
        val db = firestore ?: return Result.success(Unit)
        return try {
            val map = mapOf(
                "id" to message.id,
                "conversationId" to message.conversationId,
                "userId" to message.userId,
                "senderRole" to message.senderRole,
                "senderName" to message.senderName,
                "message" to message.message,
                "status" to message.status,
                "timestamp" to message.timestamp,
                "adminReplyToId" to message.adminReplyToId,
                "updatedAt" to message.updatedAt
            )
            db.collection("support_messages").document(message.id).set(map, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeCloudSupportMessages(): Flow<List<com.example.data.model.SupportMessageEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }
        val listener: ListenerRegistration = db.collection("support_messages")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error listening to support messages", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        com.example.data.model.SupportMessageEntity(
                            id = doc.getString("id") ?: doc.id,
                            conversationId = doc.getString("conversationId") ?: "",
                            userId = doc.getString("userId") ?: "",
                            senderRole = doc.getString("senderRole") ?: UserRole.USER,
                            senderName = doc.getString("senderName") ?: "",
                            message = doc.getString("message") ?: "",
                            status = doc.getString("status") ?: "UNREAD",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            adminReplyToId = doc.getString("adminReplyToId"),
                            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun deleteSupportMessageFromCloud(messageId: String): Result<Unit> {
        val db = firestore ?: return Result.success(Unit)
        return try {
            db.collection("support_messages").document(messageId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
