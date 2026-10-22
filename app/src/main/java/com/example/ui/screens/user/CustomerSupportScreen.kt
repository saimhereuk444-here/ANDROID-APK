package com.example.ui.screens.user

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun CustomerSupportScreen(
    repository: SkinToolRepository,
    onNavigateBack: () -> Unit,
    onNavigateToMailbox: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUser by repository.currentUser.collectAsState()
    val activeUserId = currentUser?.userId ?: repository.getOrCreatePersistentUserId()

    val userMessages by repository.getMessagesForUser(activeUserId).collectAsState(initial = emptyList())
    val unreadMailCount by repository.getUnreadCountForUser(activeUserId).collectAsState(initial = 0)

    var nameInput by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var messageInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successNotification by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Column {
                        Text(
                            text = "CUSTOMER SERVICE",
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            fontSize = 17.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "24/7 SUPPORT & HELP CENTER",
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("support_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeonGreen
                        )
                    }
                },
                actions = {
                    // Quick Mailbox shortcut with unread badge
                    BadgedBox(
                        badge = {
                            if (unreadMailCount > 0) {
                                Badge(
                                    containerColor = AccentRed,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = if (unreadMailCount > 9) "9+" else unreadMailCount.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateToMailbox,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberSurfaceElevated)
                                .border(1.dp, MintGlow.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .testTag("support_open_mailbox_button")
                        ) {
                            Icon(
                                Icons.Default.Mail,
                                contentDescription = "Mailbox",
                                tint = MintGlow,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberSurface.copy(alpha = 0.9f))
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            item {
                GamingCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderGlow = true
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(NeonGreen.copy(alpha = 0.15f))
                                .border(1.dp, NeonGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.HeadsetMic,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "HOW CAN WE HELP YOU?",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = TextPrimary,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Have an issue with skin files, Shizuku, or requests? Send a message and our admin team will reply directly to your Mailbox.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Success feedback banner
            item {
                AnimatedVisibility(
                    visible = successNotification,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0B2915))
                            .border(1.dp, NeonGreen, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "MESSAGE SENT SUCCESSFULLY!",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = NeonGreen
                                )
                                Text(
                                    text = "Our admin has received your inquiry. Check your Mailbox for replies.",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            TextButton(
                                onClick = onNavigateToMailbox,
                                modifier = Modifier.testTag("banner_view_mailbox_button")
                            ) {
                                Text("VIEW MAILBOX", color = MintGlow, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Error Message Banner
            if (errorMessage != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2E0D0D))
                            .border(1.dp, AccentRed, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AccentRed)
                            Text(
                                text = errorMessage ?: "",
                                color = AccentRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Message Form Card
            item {
                GamingCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "SEND SUPPORT MESSAGE",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = NeonGreen,
                            letterSpacing = 1.sp
                        )

                        // Name Input
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = {
                                if (it.length <= 40) {
                                    nameInput = it
                                    errorMessage = null
                                }
                            },
                            label = { Text("Your Name or Gamertag") },
                            placeholder = { Text("e.g. Saim, EliteSniper") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = NeonGreen)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("support_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = CyberBorder,
                                focusedLabelColor = NeonGreen,
                                unfocusedLabelColor = TextSecondary,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = NeonGreen
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Message Input
                        OutlinedTextField(
                            value = messageInput,
                            onValueChange = {
                                if (it.length <= 1000) {
                                    messageInput = it
                                    errorMessage = null
                                }
                            },
                            label = { Text("Describe Your Question / Issue") },
                            placeholder = { Text("Explain what you need help with in detail...") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = MintGlow,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            },
                            minLines = 4,
                            maxLines = 6,
                            supportingText = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (messageInput.isBlank()) "Required" else "",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "${messageInput.length} / 1000",
                                        color = if (messageInput.length > 900) AccentAmber else TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("support_message_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MintGlow,
                                unfocusedBorderColor = CyberBorder,
                                focusedLabelColor = MintGlow,
                                unfocusedLabelColor = TextSecondary,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = MintGlow
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Submit Button
                        Button(
                            onClick = {
                                val trimmedName = nameInput.trim()
                                val trimmedMsg = messageInput.trim()

                                if (trimmedName.isBlank()) {
                                    errorMessage = "Please enter your name."
                                    return@Button
                                }
                                if (trimmedMsg.isBlank()) {
                                    errorMessage = "Please type a message before sending."
                                    return@Button
                                }

                                if (isSubmitting) return@Button

                                isSubmitting = true
                                errorMessage = null
                                successNotification = false

                                scope.launch {
                                    val result = repository.sendCustomerMessage(
                                        senderName = trimmedName,
                                        messageText = trimmedMsg,
                                        userId = activeUserId
                                    )
                                    isSubmitting = false
                                    if (result.isSuccess) {
                                        messageInput = ""
                                        successNotification = true
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to send message. Please try again."
                                    }
                                }
                            },
                            enabled = !isSubmitting && nameInput.trim().isNotBlank() && messageInput.trim().isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("send_support_message_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = Color(0xFF021B0B),
                                disabledContainerColor = CyberSurfaceElevated,
                                disabledContentColor = TextMuted
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color(0xFF021B0B),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("SENDING MESSAGE...", fontWeight = FontWeight.Black, fontSize = 14.sp)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SUBMIT MESSAGE", fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // Quick Link to Mailbox
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberSurfaceElevated)
                        .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
                        .clickable { onNavigateToMailbox() }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Inbox, contentDescription = null, tint = MintGlow)
                        Column {
                            Text(
                                text = "CHECK USER MAILBOX",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = if (unreadMailCount > 0) "$unreadMailCount new unread admin reply" else "View all admin replies and message logs",
                                fontSize = 11.sp,
                                color = if (unreadMailCount > 0) NeonGreen else TextSecondary
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                }
            }

            // Conversation History Header
            if (userMessages.isNotEmpty()) {
                item {
                    Text(
                        text = "CONVERSATION HISTORY (${userMessages.size})",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                }

                items(userMessages, key = { it.id }) { msg ->
                    SupportMessageBubble(
                        message = msg,
                        dateFormat = dateFormat
                    )
                }
            }
        }
    }
}

@Composable
fun SupportMessageBubble(
    message: SupportMessageEntity,
    dateFormat: SimpleDateFormat
) {
    val isAdmin = message.senderRole == UserRole.ADMIN

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isAdmin) Alignment.Start else Alignment.End
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isAdmin) 2.dp else 14.dp,
                        bottomEnd = if (isAdmin) 14.dp else 2.dp
                    )
                )
                .background(
                    if (isAdmin) {
                        Brush.verticalGradient(listOf(Color(0xFF0F2B19), Color(0xFF0A1B10)))
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
                        bottomStart = if (isAdmin) 2.dp else 14.dp,
                        bottomEnd = if (isAdmin) 14.dp else 2.dp
                    )
                )
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isAdmin) Icons.Default.Shield else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (isAdmin) NeonGreen else MintGlow,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isAdmin) "SH ADMIN REPLY" else message.senderName.ifBlank { "You" },
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = if (isAdmin) NeonGreen else MintGlow,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (isAdmin && message.status == "UNREAD") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AccentRed)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "NEW",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Text(
                    text = message.message,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )

                Text(
                    text = dateFormat.format(Date(message.timestamp)),
                    fontSize = 10.sp,
                    color = TextMuted,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
