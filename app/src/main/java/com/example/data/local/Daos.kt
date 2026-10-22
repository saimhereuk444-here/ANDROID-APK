package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ApplyHistoryEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.ContentStatus
import com.example.data.model.FavoriteEntity
import com.example.data.model.FolderEntity
import com.example.data.model.PackageEntity
import com.example.data.model.PackageFileEntity
import com.example.data.model.ScreenEntity
import com.example.data.model.SyncMetadataEntity
import com.example.data.model.UserEntity
import com.example.data.model.SupportMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenDao {
    @Query("SELECT * FROM screens WHERE status = '${ContentStatus.PUBLISHED}' ORDER BY displayOrder ASC, updatedAt DESC")
    fun getPublishedScreens(): Flow<List<ScreenEntity>>

    @Query("SELECT * FROM screens ORDER BY displayOrder ASC, updatedAt DESC")
    fun getAllScreensAdmin(): Flow<List<ScreenEntity>>

    @Query("SELECT * FROM screens WHERE id = :id LIMIT 1")
    suspend fun getScreenById(id: String): ScreenEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreen(screen: ScreenEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreens(screens: List<ScreenEntity>)

    @Query("SELECT * FROM screens ORDER BY displayOrder ASC, updatedAt DESC")
    suspend fun getAllScreensAdminDirect(): List<ScreenEntity>

    @Query("DELETE FROM screens WHERE id = :id")
    suspend fun deleteScreen(id: String)

    @Query("DELETE FROM screens")
    suspend fun clearScreens()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE (status != '${ContentStatus.DISABLED}' OR status IS NULL) ORDER BY displayOrder ASC, updatedAt DESC")
    fun getPublishedCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE (status != '${ContentStatus.DISABLED}' OR status IS NULL) AND (gameMode = :gameMode OR gameMode = 'BOTH' OR gameMode = '' OR gameMode IS NULL) ORDER BY displayOrder ASC, updatedAt DESC")
    fun getCategoriesForGame(gameMode: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY displayOrder ASC, updatedAt DESC")
    fun getAllCategoriesAdmin(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY displayOrder ASC, updatedAt DESC")
    suspend fun getAllCategoriesAdminDirect(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: String)

    @Query("DELETE FROM categories")
    suspend fun clearCategories()
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE (status != '${ContentStatus.DISABLED}' OR status IS NULL) ORDER BY displayOrder ASC, updatedAt DESC")
    fun getPublishedFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders ORDER BY displayOrder ASC, updatedAt DESC")
    fun getAllFoldersAdmin(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders ORDER BY displayOrder ASC, updatedAt DESC")
    suspend fun getAllFoldersAdminDirect(): List<FolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<FolderEntity>)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolder(id: String)

    @Query("DELETE FROM folders")
    suspend fun clearFolders()
}

@Dao
interface PackageDao {
    @Query("SELECT * FROM packages WHERE (status != '${ContentStatus.DISABLED}' OR status IS NULL) ORDER BY displayOrder ASC, updatedAt DESC")
    fun getPublishedPackages(): Flow<List<PackageEntity>>

    @Query("SELECT * FROM packages ORDER BY displayOrder ASC, updatedAt DESC")
    fun getAllPackagesAdmin(): Flow<List<PackageEntity>>

    @Query("SELECT * FROM packages ORDER BY displayOrder ASC, updatedAt DESC")
    suspend fun getAllPackagesAdminDirect(): List<PackageEntity>

    @Query("SELECT * FROM packages WHERE categoryId = :categoryId AND (status != '${ContentStatus.DISABLED}' OR status IS NULL) ORDER BY displayOrder ASC, updatedAt DESC")
    fun getPackagesForCategory(categoryId: String): Flow<List<PackageEntity>>

    @Query("SELECT * FROM packages WHERE categoryId = :categoryId ORDER BY displayOrder ASC, updatedAt DESC")
    fun getAllPackagesForCategoryAdmin(categoryId: String): Flow<List<PackageEntity>>

    @Query("SELECT * FROM packages WHERE categoryId = :categoryId AND (status != '${ContentStatus.DISABLED}' OR status IS NULL) ORDER BY displayOrder ASC, updatedAt DESC")
    suspend fun getPackagesForCategoryDirect(categoryId: String): List<PackageEntity>

    @Query("SELECT * FROM packages WHERE screenId = :screenId AND (status != '${ContentStatus.DISABLED}' OR status IS NULL) ORDER BY displayOrder ASC")
    fun getPackagesForScreen(screenId: String): Flow<List<PackageEntity>>

    @Query("SELECT * FROM packages WHERE folderId = :folderId AND (status != '${ContentStatus.DISABLED}' OR status IS NULL) ORDER BY displayOrder ASC")
    fun getPackagesForFolder(folderId: String): Flow<List<PackageEntity>>

    @Query("SELECT * FROM packages WHERE id = :id LIMIT 1")
    fun getPackageFlow(id: String): Flow<PackageEntity?>

    @Query("SELECT * FROM packages WHERE id = :id LIMIT 1")
    suspend fun getPackageDirect(id: String): PackageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackage(pkg: PackageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackages(pkgs: List<PackageEntity>)

    @Query("DELETE FROM packages WHERE id = :id")
    suspend fun deletePackage(id: String)

    @Query("DELETE FROM packages")
    suspend fun clearPackages()
}

@Dao
interface PackageFileDao {
    @Query("SELECT * FROM package_files WHERE packageId = :packageId")
    fun getFilesForPackage(packageId: String): Flow<List<PackageFileEntity>>

    @Query("SELECT * FROM package_files WHERE packageId = :packageId")
    suspend fun getFilesForPackageDirect(packageId: String): List<PackageFileEntity>

    @Query("SELECT * FROM package_files")
    suspend fun getAllFilesDirect(): List<PackageFileEntity>

    @Query("SELECT COUNT(*) FROM package_files")
    fun getTotalFileCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: PackageFileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<PackageFileEntity>)

    @Query("DELETE FROM package_files WHERE id = :id")
    suspend fun deleteFile(id: String)

    @Query("DELETE FROM package_files WHERE packageId = :packageId")
    suspend fun deleteFilesForPackage(packageId: String)

    @Query("DELETE FROM package_files")
    suspend fun clearFiles()
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE userId = :userId")
    fun getFavoritesForUser(userId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE packageId = :packageId AND userId = :userId)")
    fun isFavorite(packageId: String, userId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE packageId = :packageId AND userId = :userId")
    suspend fun deleteFavorite(packageId: String, userId: String)
}

@Dao
interface ApplyHistoryDao {
    @Query("SELECT * FROM apply_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ApplyHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ApplyHistoryEntity): Long

    @Query("DELETE FROM apply_history")
    suspend fun clearHistory()
}

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE key = :key LIMIT 1")
    fun getMetadata(key: String = "global_sync"): Flow<SyncMetadataEntity?>

    @Query("SELECT * FROM sync_metadata WHERE key = :key LIMIT 1")
    suspend fun getMetadataDirect(key: String = "global_sync"): SyncMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(metadata: SyncMetadataEntity)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM app_users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM app_users WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("UPDATE app_users SET isActive = :isActive WHERE userId = :userId")
    suspend fun updateUserStatus(userId: String, isActive: Boolean)
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = :id LIMIT 1")
    fun getSettings(id: String = "global_settings"): Flow<com.example.data.model.AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = :id LIMIT 1")
    suspend fun getSettingsDirect(id: String = "global_settings"): com.example.data.model.AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: com.example.data.model.AppSettingsEntity)
}

@Dao
interface SupportMessageDao {
    @Query("SELECT * FROM support_messages WHERE userId = :userId ORDER BY timestamp ASC")
    fun getMessagesForUser(userId: String): Flow<List<SupportMessageEntity>>

    @Query("SELECT * FROM support_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<SupportMessageEntity>>

    @Query("SELECT * FROM support_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<SupportMessageEntity>>

    @Query("SELECT * FROM support_messages ORDER BY timestamp DESC")
    suspend fun getAllMessagesDirect(): List<SupportMessageEntity>

    @Query("SELECT COUNT(*) FROM support_messages WHERE userId = :userId AND senderRole = 'ADMIN' AND status = 'UNREAD'")
    fun getUnreadCountForUser(userId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM support_messages WHERE senderRole = 'USER' AND status = 'UNREAD'")
    fun getUnreadCountAdmin(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: SupportMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<SupportMessageEntity>)

    @Query("UPDATE support_messages SET status = 'READ', updatedAt = :updatedAt WHERE id = :messageId")
    suspend fun markMessageRead(messageId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE support_messages SET status = 'READ', updatedAt = :updatedAt WHERE conversationId = :conversationId AND senderRole = :role")
    suspend fun markConversationRead(conversationId: String, role: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM support_messages WHERE id = :id")
    suspend fun deleteMessage(id: String)

    @Query("DELETE FROM support_messages WHERE conversationId = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    @Query("DELETE FROM support_messages")
    suspend fun clearMessages()
}

