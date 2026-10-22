package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AppSettingsEntity
import com.example.data.model.ApplyHistoryEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.FavoriteEntity
import com.example.data.model.FolderEntity
import com.example.data.model.PackageEntity
import com.example.data.model.PackageFileEntity
import com.example.data.model.ScreenEntity
import com.example.data.model.SupportMessageEntity
import com.example.data.model.SyncMetadataEntity
import com.example.data.model.UserEntity

@Database(
    entities = [
        ScreenEntity::class,
        CategoryEntity::class,
        FolderEntity::class,
        PackageEntity::class,
        PackageFileEntity::class,
        FavoriteEntity::class,
        ApplyHistoryEntity::class,
        SyncMetadataEntity::class,
        UserEntity::class,
        AppSettingsEntity::class,
        SupportMessageEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun screenDao(): ScreenDao
    abstract fun categoryDao(): CategoryDao
    abstract fun folderDao(): FolderDao
    abstract fun packageDao(): PackageDao
    abstract fun packageFileDao(): PackageFileDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun applyHistoryDao(): ApplyHistoryDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun userDao(): UserDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun supportMessageDao(): SupportMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sh_skin_tool_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
