package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.SupportMessageEntity
import com.example.data.model.UserRole
import com.example.data.remote.FirebaseService
import com.example.data.repository.SkinToolRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CustomerServiceTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: SkinToolRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val firebaseService = FirebaseService(context)
        repository = SkinToolRepository(context, database, firebaseService)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testUserCanSendMessageAndItSavesCorrectly() = runBlocking {
        val userId = "user_alpha_123"
        val senderName = "ProPlayer"
        val messageText = "Free Fire katana skin is not appearing in game."

        val result = repository.sendCustomerMessage(senderName, messageText, userId)
        assertTrue("Message should be sent successfully", result.isSuccess)

        val entity = result.getOrNull()
        assertNotNull(entity)
        assertEquals("user_alpha_123", entity?.userId)
        assertEquals("conv_user_alpha_123", entity?.conversationId)
        assertEquals(UserRole.USER, entity?.senderRole)
        assertEquals("ProPlayer", entity?.senderName)
        assertEquals(messageText, entity?.message)
        assertEquals("UNREAD", entity?.status)

        // Check user inbox messages
        val userMessages = repository.getMessagesForUser(userId).first()
        assertEquals(1, userMessages.size)
        assertEquals(messageText, userMessages[0].message)

        // Check admin sees the message as unread
        val adminUnreadCount = repository.getUnreadCountAdmin().first()
        assertEquals(1, adminUnreadCount)
    }

    @Test
    fun testEmptyInputValidationFailsGracefully() = runBlocking {
        val emptyNameResult = repository.sendCustomerMessage("   ", "Valid message", "user_1")
        assertTrue("Empty name should fail", emptyNameResult.isFailure)

        val emptyMsgResult = repository.sendCustomerMessage("Valid Name", "   ", "user_1")
        assertTrue("Empty message should fail", emptyMsgResult.isFailure)
    }

    @Test
    fun testAdminCanReplyAndUserSeesInMailbox() = runBlocking {
        val userId = "user_beta_456"
        val convId = "conv_$userId"

        // 1. User sends message
        repository.sendCustomerMessage("GamerBeta", "How do I setup Shizuku?", userId)

        // 2. Authorize admin session and send reply
        com.example.security.SessionManager.createAdminSession()
        val replyText = "Hello GamerBeta, please check the Shizuku video tutorial in settings."
        val replyResult = repository.sendAdminReply(convId, userId, replyText, "SH Admin Support")
        assertTrue("Admin reply should succeed", replyResult.isSuccess)

        // 3. User checks mailbox: user should have 1 unread admin reply
        val userUnreadCount = repository.getUnreadCountForUser(userId).first()
        assertEquals(1, userUnreadCount)

        // 4. Check conversation order (user message first, admin reply second)
        val conversationMessages = repository.getMessagesForConversation(convId).first()
        assertEquals(2, conversationMessages.size)
        assertEquals(UserRole.USER, conversationMessages[0].senderRole)
        assertEquals(UserRole.ADMIN, conversationMessages[1].senderRole)
        assertEquals(replyText, conversationMessages[1].message)

        // 5. Admin marked preceding user message as READ
        assertEquals("READ", conversationMessages[0].status)
    }

    @Test
    fun testMarkMessageAsReadUpdatesUserUnreadCount() = runBlocking {
        val userId = "user_gamma_789"
        val convId = "conv_$userId"

        repository.sendCustomerMessage("UserGamma", "Question", userId)
        com.example.security.SessionManager.createAdminSession()
        val reply = repository.sendAdminReply(convId, userId, "Here is the answer", "Admin").getOrThrow()

        // Unread count should be 1
        assertEquals(1, repository.getUnreadCountForUser(userId).first())

        // User opens message
        repository.markSupportMessageAsRead(reply.id)

        // Unread count should now be 0
        assertEquals(0, repository.getUnreadCountForUser(userId).first())
    }

    @Test
    fun testUserIsolationBetweenDifferentUsers() = runBlocking {
        val user1 = "user_one"
        val user2 = "user_two"

        repository.sendCustomerMessage("User One", "Secret question 1", user1)
        repository.sendCustomerMessage("User Two", "Secret question 2", user2)

        val msgs1 = repository.getMessagesForUser(user1).first()
        val msgs2 = repository.getMessagesForUser(user2).first()

        assertEquals(1, msgs1.size)
        assertEquals("Secret question 1", msgs1[0].message)

        assertEquals(1, msgs2.size)
        assertEquals("Secret question 2", msgs2[0].message)

        // Admin should see both in all messages
        val allMsgs = repository.getAllSupportMessagesAdmin().first()
        assertEquals(2, allMsgs.size)
    }
}
