# Project specific ProGuard rules for SH Skin Tool Hardening

# Strip debugging symbols and line numbers in release builds
-repackageclasses 'com.example.sec'
-allowaccessmodification
-renamesourcefileattribute SourceFile
-keepattributes !SourceFile,!LineNumberTable

# Strip debug and verbose logs
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# Preserve Room entities and DAOs
-keep class com.example.data.model.** { *; }
-keep interface com.example.data.local.** { *; }
-keep class com.example.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }

# Preserve Moshi models and adapters
-keep class com.example.data.remote.CloudDatabasePayload { *; }
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
}

# Preserve Shizuku IPC binder interfaces
-keep class rikka.shizuku.** { *; }
-keep interface rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-keep interface moe.shizuku.** { *; }

# Preserve Compose runtime
-keep class androidx.compose.** { *; }
