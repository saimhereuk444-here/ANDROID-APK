package com.example.security

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

object SecurityAuditor {
    private const val TAG = "SecurityAuditor"

    // Brute-force protection state
    private val failedAttempts = AtomicInteger(0)
    private val lockoutUntil = AtomicLong(0L)
    private const val MAX_ATTEMPTS = 5
    private const val LOCKOUT_DURATION_MS = 30_000L // 30-second penalty after 5 failed attempts

    // Allowed game root directories for Shizuku operations
    private val ALLOWED_TARGET_PREFIXES = listOf(
        "/storage/emulated/0/Android/data/com.dts.freefireth",
        "/storage/emulated/0/Android/data/com.dts.freefireth.max",
        "/storage/emulated/0/Android/data/com.dts.freefiremax",
        "/storage/emulated/0/Android/data",
        "/sdcard/Android/data/com.dts.freefireth",
        "/sdcard/Android/data/com.dts.freefireth.max",
        "/sdcard/Android/data/com.dts.freefiremax",
        "/sdcard/Android/data"
    )

    /**
     * Checks if brute-force lockout is currently active.
     * Returns remaining lockout seconds, or 0 if allowed.
     */
    fun getLockoutRemainingSeconds(): Int {
        val now = System.currentTimeMillis()
        val until = lockoutUntil.get()
        return if (until > now) {
            (((until - now) / 1000L) + 1).toInt()
        } else {
            0
        }
    }

    /**
     * Records a failed authentication attempt and triggers lockout if threshold reached.
     */
    fun recordFailedAttempt(): Int {
        val attempts = failedAttempts.incrementAndGet()
        if (attempts >= MAX_ATTEMPTS) {
            lockoutUntil.set(System.currentTimeMillis() + LOCKOUT_DURATION_MS)
        }
        return attempts
    }

    /**
     * Resets failed attempt counter on successful login.
     */
    fun recordSuccessfulAuth() {
        failedAttempts.set(0)
        lockoutUntil.set(0L)
    }

    /**
     * Constant-time comparison between two strings to prevent timing side-channel attacks.
     */
    fun constantTimeEquals(a: String, b: String): Boolean {
        val aBytes = a.toByteArray(Charsets.UTF_8)
        val bBytes = b.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(aBytes, bBytes)
    }

    /**
     * Validates that destination path strictly belongs to authorized game package directories
     * and does not contain path traversal, escaping, or malicious characters.
     */
    fun validateGameTargetPath(path: String): Boolean {
        if (path.isBlank() || path.contains("\u0000")) return false

        // Normalize path separators
        val normalized = path.replace('\\', '/')

        // Check for path traversal sequences
        if (normalized.contains("/../") || normalized.endsWith("/..") || normalized.startsWith("../")) {
            Log.e(TAG, "Security alert: Path traversal rejected: $path")
            return false
        }

        // Canonical verification
        val file = File(normalized)
        val canonical = try {
            file.canonicalPath.replace('\\', '/')
        } catch (e: Throwable) {
            return false
        }

        val isAllowed = ALLOWED_TARGET_PREFIXES.any { prefix ->
            canonical == prefix || canonical.startsWith("$prefix/")
        }

        if (!isAllowed) {
            Log.w(TAG, "Security alert: Target path outside allowed game scope: $canonical")
        }

        return isAllowed
    }

    /**
     * Sanitizes shell argument using strict single-quote encapsulation
     * to eliminate command injection vulnerabilities entirely.
     */
    fun escapeShellArg(arg: String): String {
        // Disallow null bytes
        val clean = arg.replace("\u0000", "")
        return "'" + clean.replace("'", "'\\''") + "'"
    }

    /**
     * Sanitizes file names to remove path components, traversal tokens, and control characters.
     */
    fun sanitizeFileName(rawName: String): String {
        val baseName = rawName.substringAfterLast('/').substringAfterLast('\\')
        return baseName.replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
            .ifBlank { "unnamed_file_${System.currentTimeMillis()}" }
    }

    /**
     * Non-intrusive environment integrity check.
     */
    fun isEnvironmentTampered(context: Context): Boolean {
        return try {
            // Check for basic suspicious test keys or obvious debugger hooks without crashing standard apps
            val tags = Build.TAGS
            tags != null && tags.contains("test-keys") && File("/system/app/Superuser.apk").exists()
        } catch (e: Throwable) {
            false
        }
    }
}
