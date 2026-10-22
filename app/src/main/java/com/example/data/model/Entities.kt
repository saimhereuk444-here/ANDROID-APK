package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

object ContentStatus {
    const val DRAFT = "DRAFT"
    const val PUBLISHED = "PUBLISHED"
    const val DISABLED = "DISABLED"
}

object UserRole {
    const val ADMIN = "ADMIN"
    const val USER = "USER"
}

object ApplyResult {
    const val SUCCESS = "SUCCESS"
    const val FAILED = "FAILED"
    const val CANCELLED = "CANCELLED"
}

@Entity(tableName = "screens")
data class ScreenEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String = "",
    val coverImageUrl: String = "",
    val categoryId: String? = null,
    val folderId: String? = null,
    val displayOrder: Int = 0,
    val status: String = ContentStatus.PUBLISHED, // DRAFT, PUBLISHED, DISABLED
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val coverImageUrl: String = "",
    val gameMode: String = "FREE_FIRE", // "FREE_FIRE", "FREE_FIRE_MAX", or "BOTH"
    val displayOrder: Int = 0,
    val status: String = ContentStatus.PUBLISHED,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val coverImageUrl: String = "",
    val parentFolderId: String? = null,
    val displayOrder: Int = 0,
    val status: String = ContentStatus.PUBLISHED,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "packages")
data class PackageEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val coverImageUrl: String = "",
    val screenId: String? = null,
    val categoryId: String? = null,
    val folderId: String? = null,
    val version: String = "1.0",
    val fileSize: Long = 0L,
    val displayOrder: Int = 0,
    val status: String = ContentStatus.PUBLISHED,
    val isFeatured: Boolean = false,
    val ffFileName: String = "",
    val ffMaxFileName: String = "",
    val iconPngUrl: String = "",
    val ffIconPngUrl: String = "",
    val ffMaxIconPngUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "package_files")
data class PackageFileEntity(
    @PrimaryKey val id: String,
    val packageId: String,
    val fileName: String,
    val relativePath: String, // Relative path under target directory
    val downloadUrl: String = "",
    val fileSize: Long = 0L,
    val checksum: String = "",
    val localCachedPath: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val packageId: String,
    val userId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "apply_history")
data class ApplyHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageId: String,
    val packageName: String,
    val targetPath: String,
    val result: String, // SUCCESS, FAILED, CANCELLED
    val filesCount: Int,
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val key: String = "global_sync",
    val contentVersion: Long = 1L,
    val lastSyncTimestamp: Long = 0L,
    val status: String = "SYNCED"
)

@Entity(tableName = "app_users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val email: String,
    val displayName: String,
    val role: String = UserRole.USER,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActive: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: String = "global_settings",
    val userPassword: String = "SAIMHERE444",
    val adminPassword: String = "SAIMHEREFORAREASON",
    val getPasswordUrl: String = "https://t.me/saimteam",
    val targetDirectory: String = "/storage/emulated/0/Android/data/",
    val ffDefaultIconUrl: String = "",
    val ffMaxDefaultIconUrl: String = "",
    val wrongPasswordVideoUrl: String = "",
    val shizukuSetupVideoUrl: String = "",
    val authVersion: Long = 1L,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "support_messages")
data class SupportMessageEntity(
    @PrimaryKey val id: String = "",
    val conversationId: String = "",
    val userId: String = "",
    val senderRole: String = UserRole.USER, // "USER" or "ADMIN"
    val senderName: String = "",
    val message: String = "",
    val status: String = "UNREAD", // "UNREAD" or "READ"
    val timestamp: Long = System.currentTimeMillis(),
    val adminReplyToId: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)


