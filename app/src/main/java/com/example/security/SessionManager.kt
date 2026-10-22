package com.example.security

import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicReference

object SessionManager {
    private const val SESSION_TTL_MS = 2 * 60 * 60 * 1000L // 2 hours validity

    data class SessionToken(
        val token: String,
        val createdAt: Long = System.currentTimeMillis(),
        val expiresAt: Long = System.currentTimeMillis() + SESSION_TTL_MS
    ) {
        val isValid: Boolean get() = System.currentTimeMillis() < expiresAt
    }

    private val activeAdminSession = AtomicReference<SessionToken?>(null)
    private val activeUserSessionPassword = AtomicReference<String?>(null)
    private val activeUserSessionAuthVersion = java.util.concurrent.atomic.AtomicLong(0L)
    private val secureRandom = SecureRandom()

    /**
     * Generates a new cryptographically random 256-bit Admin session token.
     */
    fun createAdminSession(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        val tokenHex = bytes.joinToString("") { "%02x".format(it) }
        val session = SessionToken(token = tokenHex)
        activeAdminSession.set(session)
        return tokenHex
    }

    /**
     * Verifies if current session possesses valid, unexpired Admin authorization.
     */
    fun isAdminSessionValid(): Boolean {
        val session = activeAdminSession.get()
        return session != null && session.isValid
    }

    /**
     * Invalidates current Admin session immediately on logout or password change.
     */
    fun invalidateAdminSession() {
        activeAdminSession.set(null)
    }

    /**
     * Retrieves current active admin token if valid.
     */
    fun getActiveAdminToken(): String? {
        val session = activeAdminSession.get()
        return if (session != null && session.isValid) session.token else null
    }

    /**
     * Registers active user session with the exact password and authVersion used to authenticate.
     */
    fun createUserSession(password: String, authVersion: Long = 1L) {
        activeUserSessionPassword.set(password)
        activeUserSessionAuthVersion.set(authVersion)
    }

    /**
     * Returns true if there is an active authenticated user session in memory.
     */
    fun hasActiveUserSession(): Boolean {
        return activeUserSessionPassword.get() != null
    }

    /**
     * Checks if active user session matches current required system password and authVersion.
     */
    fun isUserSessionValid(currentPassword: String, currentAuthVersion: Long = 1L): Boolean {
        val session = activeUserSessionPassword.get()
        val sessionVer = activeUserSessionAuthVersion.get()
        return session != null && session == currentPassword && sessionVer >= currentAuthVersion
    }

    /**
     * Returns true ONLY if an active user session exists in memory AND its password or authVersion is no longer valid.
     * Prevents false session invalidations when opening the app or loading splash screen.
     */
    fun isUserSessionExpired(currentPassword: String, currentAuthVersion: Long = 1L): Boolean {
        val session = activeUserSessionPassword.get() ?: return false
        val sessionVer = activeUserSessionAuthVersion.get()
        return session != currentPassword || sessionVer < currentAuthVersion
    }

    /**
     * Invalidates user session so users must re-authenticate with new password.
     */
    fun invalidateUserSession() {
        activeUserSessionPassword.set(null)
        activeUserSessionAuthVersion.set(-1L)
    }
}
