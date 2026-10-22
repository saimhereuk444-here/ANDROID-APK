package com.example.ui.screens.user

import androidx.activity.compose.BackHandler
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
fun UserMailboxScreen(
    repository: SkinToolRepository,
    onNavigateBack: () -> Unit,
    onNavigateToSupport: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUser by repository.currentUser.collectAsState()
    val activeUserId = currentUser?.userId ?: repository.getOrCreatePersistentUserId()

    val messages by repository.getMessagesForUser(activeUserId).collectAsState(initial = emptyList())
    val unreadCount by repository.getUnreadCountForUser(activeUserId).collectAsState(initial = 0)

    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "ADMIN_REPLIES", "UNREAD"
    var selectedMessageDetail by remember { mutableStateOf<SupportMessageEntity?>(null) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }

    BackHandler {
        onNavigateBack()
    }

    // Filter messages
    val filteredMessages = remember(messages, selectedFilter) {
        when (selectedFilter) {
            "ADMIN_REPLIES" -> messages.filter { it.senderRole == UserRole.ADMIN }
            "UNREAD" -> messages.filter { it.senderRole == UserRole.ADMIN && it.status == "UNREAD" }
            else -> messages
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
                                text = "USER MAILBOX",
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                letterSpacing = 1.sp
                            )
                            if (unreadCount > 0) {
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
                            text = "OFFICIAL ADMIN REPLIES & NOTIFICATIONS",
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("mailbox_back_button")
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
                        onClick = onNavigateToSupport,
                        modifier = Modifier.testTag("mailbox_new_message_button")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "New Message",
                            tint = MintGlow
                        )
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Filter Pills
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("ALL (${messages.size})") },
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
                        modifier = Modifier.testTag("mailbox_filter_all")
                    )

                    FilterChip(
                        selected = selectedFilter == "ADMIN_REPLIES",
                        onClick = { selectedFilter = "ADMIN_REPLIES" },
                        label = {
                            val replyCount = messages.count { it.senderRole == UserRole.ADMIN }
                            Text("ADMIN REPLIES ($replyCount)")
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MintGlow,
                            selectedLabelColor = Color(0xFF02190A),
                            containerColor = CyberSurfaceElevated,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedFilter == "ADMIN_REPLIES",
                            borderColor = CyberBorder,
                            selectedBorderColor = MintGlow
                        ),
                        modifier = Modifier.testTag("mailbox_filter_replies")
                    )

                    FilterChip(
                        selected = selectedFilter == "UNREAD",
                        onClick = { selectedFilter = "UNREAD" },
                        label = { Text("UNREAD ($unreadCount)") },
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
                        modifier = Modifier.testTag("mailbox_filter_unread")
                    )
                }
            }

            // Empty State
            if (filteredMessages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(CyberSurfaceElevated)
                                    .border(1.dp, CyberBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.MailOutline,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Text(
                                text = "NO MESSAGES IN MAILBOX",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = if (selectedFilter == "UNREAD") "You have no unread messages." else "When our admin team replies to your inquiries, they will appear here.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = onNavigateToSupport,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonGreen,
                                    contentColor = Color(0xFF021B0B)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("mailbox_empty_contact_button")
                            ) {
                                Icon(Icons.Default.HeadsetMic, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CONTACT CUSTOMER SERVICE", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            } else {
                items(filteredMessages, key = { it.id }) { msg ->
                    val isAdmin = msg.senderRole == UserRole.ADMIN
                    val isUnread = isAdmin && msg.status == "UNREAD"

                    GamingCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMessageDetail = msg
                                if (isUnread) {
                                    scope.launch {
                                        repository.markSupportMessageAsRead(msg.id)
                                    }
                                }
                            }
                            .testTag("mailbox_item_${msg.id}"),
                        borderGlow = isUnread
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
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(if (isAdmin) NeonGreen.copy(alpha = 0.15f) else CyberSurfaceElevated)
                                            .border(1.dp, if (isAdmin) NeonGreen else CyberBorder, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isAdmin) Icons.Default.Shield else Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isAdmin) NeonGreen else MintGlow,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = if (isAdmin) "SH ADMIN TEAM" else "Your Inquiry",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = if (isAdmin) NeonGreen else TextPrimary
                                        )
                                        Text(
                                            text = dateFormat.format(Date(msg.timestamp)),
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                    }
                                }

                                if (isUnread) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(AccentRed)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "UNREAD",
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }

                            Text(
                                text = msg.message,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 17.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "TAP TO VIEW DETAILS →",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MintGlow
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail / Thread Dialog
    selectedMessageDetail?.let { msg ->
        val isAdmin = msg.senderRole == UserRole.ADMIN
        AlertDialog(
            onDismissRequest = { selectedMessageDetail = null },
            containerColor = CyberSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isAdmin) NeonGreen.copy(alpha = 0.2f) else CyberSurfaceElevated)
                            .border(1.dp, if (isAdmin) NeonGreen else CyberBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAdmin) Icons.Default.Shield else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (isAdmin) NeonGreen else MintGlow,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isAdmin) "SH ADMIN TEAM" else msg.senderName,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = if (isAdmin) NeonGreen else TextPrimary
                        )
                        Text(
                            text = dateFormat.format(Date(msg.timestamp)),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberSurfaceElevated)
                            .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = msg.message,
                            fontSize = 14.sp,
                            color = TextPrimary,
                            lineHeight = 20.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedMessageDetail = null
                        onNavigateToSupport()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreen,
                        contentColor = Color(0xFF021B0B)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("mailbox_dialog_reply_button")
                ) {
                    Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("REPLY IN SUPPORT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { selectedMessageDetail = null },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("CLOSE")
                }
            }
        )
    }
}
