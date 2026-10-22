package com.example.ui.screens.admin

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SupportMessageEntity
import com.example.data.model.UserRole
import com.example.data.repository.SkinToolRepository
import com.example.ui.components.GamingCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCustomerServiceScreen(
    repository: SkinToolRepository,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val allMessages by repository.getAllSupportMessagesAdmin().collectAsState(initial = emptyList())
    val unreadCount by repository.getUnreadCountAdmin().collectAsState(initial = 0)

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "UNREAD", "READ"

    // Active conversation opened for viewing/replying (group by conversationId or userId)
    var selectedConversationId by remember { mutableStateOf<String?>(null) }
    var replyTextInput by remember { mutableStateOf("") }
    var isSendingReply by remember { mutableStateOf(false) }
    var conversationToDelete by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }

    BackHandler {
        if (selectedConversationId != null) {
            selectedConversationId = null
        } else {
            onNavigateBack()
        }
    }

    // Group messages into conversations
    val conversations = remember(allMessages) {
        allMessages.groupBy { it.conversationId.ifBlank { "conv_${it.userId}" } }
            .map { (convId, msgs) ->
                val sorted = msgs.sortedBy { it.timestamp }
                val lastMsg = sorted.last()
                val userMsgs = sorted.filter { it.senderRole == UserRole.USER }
                val hasUnread = userMsgs.any { it.status == "UNREAD" }
                val customerName = userMsgs.firstOrNull()?.senderName?.ifBlank { "User ${lastMsg.userId.take(8)}" }
                    ?: (lastMsg.senderName.ifBlank { "User ${lastMsg.userId.take(8)}" })
                val userId = lastMsg.userId

                AdminConversationSummary(
                    conversationId = convId,
                    userId = userId,
                    customerName = customerName,
                    lastMessage = lastMsg.message,
                    lastTimestamp = lastMsg.timestamp,
                    hasUnread = hasUnread,
                    unreadCount = userMsgs.count { it.status == "UNREAD" },
                    totalMessages = sorted.size,
                    messages = sorted
                )
            }.sortedByDescending { it.lastTimestamp }
    }

    // Filter conversations
    val filteredConversations = remember(conversations, searchQuery, selectedFilter) {
        conversations.filter { conv ->
            val matchesSearch = searchQuery.isBlank() ||
                    conv.customerName.contains(searchQuery, ignoreCase = true) ||
                    conv.userId.contains(searchQuery, ignoreCase = true) ||
                    conv.lastMessage.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "UNREAD" -> conv.hasUnread
                "READ" -> !conv.hasUnread
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (selectedConversationId == null) "CUSTOMER SERVICE INBOX" else "CONVERSATION",
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                letterSpacing = 1.sp
                            )
                            if (selectedConversationId == null && unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AccentRed)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$unreadCount NEW",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (selectedConversationId == null) "LIVE USER MESSAGES & ADMIN REPLIES" else "THREAD DETAILS & TWO-WAY CHAT",
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedConversationId != null) {
                                selectedConversationId = null
                            } else {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.testTag("admin_support_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeonGreen
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                repository.syncWithCloud()
                            }
                        },
                        modifier = Modifier.testTag("admin_support_sync_button")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = MintGlow
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberSurface.copy(alpha = 0.9f))
            )
        }
    ) { innerPadding ->
        if (selectedConversationId == null) {
            // === CONVERSATIONS LIST VIEW ===
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Search Input
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by customer name, message or ID...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = NeonGreen)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_support_search_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Filter Chips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("ALL (${conversations.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonGreen,
                                selectedLabelColor = Color(0xFF02190A),
                                containerColor = CyberSurfaceElevated,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == "ALL",
                                borderColor = CyberBorder,
                                selectedBorderColor = NeonGreen
                            ),
                            modifier = Modifier.testTag("admin_filter_all")
                        )

                        FilterChip(
                            selected = selectedFilter == "UNREAD",
                            onClick = { selectedFilter = "UNREAD" },
                            label = {
                                val unreadThreads = conversations.count { it.hasUnread }
                                Text("UNREAD ($unreadThreads)")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentRed,
                                selectedLabelColor = Color.White,
                                containerColor = CyberSurfaceElevated,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == "UNREAD",
                                borderColor = CyberBorder,
                                selectedBorderColor = AccentRed
                            ),
                            modifier = Modifier.testTag("admin_filter_unread")
                        )

                        FilterChip(
                            selected = selectedFilter == "READ",
                            onClick = { selectedFilter = "READ" },
                            label = {
                                val readThreads = conversations.count { !it.hasUnread }
                                Text("RESOLVED ($readThreads)")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MintGlow,
                                selectedLabelColor = Color(0xFF02190A),
                                containerColor = CyberSurfaceElevated,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == "READ",
                                borderColor = CyberBorder,
                                selectedBorderColor = MintGlow
                            ),
                            modifier = Modifier.testTag("admin_filter_read")
                        )
                    }
                }

                // Empty State
                if (filteredConversations.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(CyberSurfaceElevated)
                                        .border(1.dp, CyberBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.SupportAgent,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Text(
                                    text = "NO CONVERSATIONS FOUND",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No messages match your search filter." else "No customer inquiries have been submitted yet.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(filteredConversations, key = { it.conversationId }) { conv ->
                        GamingCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedConversationId = conv.conversationId
                                    // Mark conversation read for user messages
                                    if (conv.hasUnread) {
                                        scope.launch {
                                            repository.markConversationAsRead(conv.conversationId, UserRole.USER)
                                        }
                                    }
                                }
                                .testTag("admin_conv_${conv.conversationId}"),
                            borderGlow = conv.hasUnread
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (conv.hasUnread) AccentRed.copy(alpha = 0.2f) else NeonGreen.copy(alpha = 0.15f))
                                                .border(1.dp, if (conv.hasUnread) AccentRed else NeonGreen, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = if (conv.hasUnread) AccentRed else NeonGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = conv.customerName,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "ID: ${conv.userId.take(12)} • ${conv.totalMessages} msg",
                                                fontSize = 10.sp,
                                                color = TextMuted
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (conv.hasUnread) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(AccentRed)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "${conv.unreadCount} NEW",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { conversationToDelete = conv.conversationId },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = conv.lastMessage,
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 17.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dateFormat.format(Date(conv.lastTimestamp)),
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "OPEN CONVERSATION →",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (conv.hasUnread) NeonGreen else MintGlow
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // === CONVERSATION DETAIL & REPLY VIEW ===
            val currentConv = conversations.firstOrNull { it.conversationId == selectedConversationId }
            val convMessages = currentConv?.messages ?: emptyList()
            val listState = rememberLazyListState()

            // Scroll to bottom when conversation messages change
            LaunchedEffect(convMessages.size) {
                if (convMessages.isNotEmpty()) {
                    listState.animateScrollToItem(convMessages.size - 1)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Header Bar for Active Conversation
                Surface(
                    color = CyberSurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = currentConv?.customerName ?: "Customer",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "User UID: ${currentConv?.userId ?: ""}",
                                fontSize = 11.sp,
                                color = NeonGreen
                            )
                        }

                        Button(
                            onClick = { selectedConversationId = null },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = BorderStroke(1.dp, CyberBorder),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("BACK TO LIST", fontSize = 11.sp)
                        }
                    }
                }

                // Messages Chat Thread
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(convMessages, key = { it.id }) { msg ->
                        val isAdmin = msg.senderRole == UserRole.ADMIN

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isAdmin) Alignment.End else Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 14.dp,
                                            topEnd = 14.dp,
                                            bottomStart = if (isAdmin) 14.dp else 2.dp,
                                            bottomEnd = if (isAdmin) 2.dp else 14.dp
                                        )
                                    )
                                    .background(
                                        if (isAdmin) {
                                            Brush.verticalGradient(listOf(Color(0xFF0C2B17), Color(0xFF071B0E)))
                                        } else {
                                            Brush.verticalGradient(listOf(CyberSurfaceElevated, CyberSurface))
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (isAdmin) NeonGreen.copy(alpha = 0.8f) else CyberBorder,
                                        RoundedCornerShape(
                                            topStart = 14.dp,
                                            topEnd = 14.dp,
                                            bottomStart = if (isAdmin) 14.dp else 2.dp,
                                            bottomEnd = if (isAdmin) 2.dp else 14.dp
                                        )
                                    )
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isAdmin) "SH ADMIN REPLY" else msg.senderName.ifBlank { "User" },
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = if (isAdmin) NeonGreen else MintGlow
                                        )

                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    repository.deleteSupportMessage(msg.id)
                                                }
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Delete",
                                                tint = TextMuted,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = msg.message,
                                        fontSize = 13.sp,
                                        color = TextPrimary,
                                        lineHeight = 18.sp
                                    )

                                    Text(
                                        text = dateFormat.format(Date(msg.timestamp)),
                                        fontSize = 9.sp,
                                        color = TextMuted,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Reply Bar
                Surface(
                    color = CyberSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = replyTextInput,
                            onValueChange = { replyTextInput = it },
                            placeholder = { Text("Write admin reply...") },
                            maxLines = 4,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_reply_text_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = NeonGreen
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                val replyText = replyTextInput.trim()
                                if (replyText.isBlank() || currentConv == null || isSendingReply) return@Button

                                isSendingReply = true
                                scope.launch {
                                    val result = repository.sendAdminReply(
                                        conversationId = currentConv.conversationId,
                                        targetUserId = currentConv.userId,
                                        replyText = replyText,
                                        adminName = "SH Admin Support"
                                    )
                                    isSendingReply = false
                                    if (result.isSuccess) {
                                        replyTextInput = ""
                                    }
                                }
                            },
                            enabled = !isSendingReply && replyTextInput.trim().isNotBlank(),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("admin_send_reply_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = Color(0xFF021B0B),
                                disabledContainerColor = CyberSurfaceElevated,
                                disabledContentColor = TextMuted
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSendingReply) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color(0xFF021B0B),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("REPLY", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Conversation Confirmation Dialog
    conversationToDelete?.let { convId ->
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            containerColor = CyberSurface,
            title = {
                Text("DELETE CONVERSATION", fontWeight = FontWeight.Black, color = TextPrimary)
            },
            text = {
                Text("Are you sure you want to permanently delete this conversation and all its messages?", color = TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.deleteSupportConversation(convId)
                            if (selectedConversationId == convId) {
                                selectedConversationId = null
                            }
                            conversationToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("DELETE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { conversationToDelete = null },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("CANCEL")
                }
            }
        )
    }
}

data class AdminConversationSummary(
    val conversationId: String,
    val userId: String,
    val customerName: String,
    val lastMessage: String,
    val lastTimestamp: Long,
    val hasUnread: Boolean,
    val unreadCount: Int,
    val totalMessages: Int,
    val messages: List<SupportMessageEntity>
)
