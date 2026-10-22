package com.example.ui.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.data.repository.SkinToolRepository
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuStatus
import com.example.ui.components.GamingButton
import com.example.ui.components.GamingCard
import com.example.ui.components.ShizukuStatusPill
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: SkinToolRepository,
    shizukuManager: ShizukuManager,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val currentUser by repository.currentUser.collectAsState()
    val shizukuStatus by shizukuManager.status.collectAsState()
    val syncState by repository.syncState.collectAsState()
    var targetPath by remember { mutableStateOf(ShizukuManager.DEFAULT_TARGET_DIR) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("SETTINGS", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberSurface.copy(alpha = 0.85f))
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
            // Account Info Card
            item {
                GamingCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = currentUser?.displayName ?: "Elite Player",
                                color = TextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (currentUser?.role == UserRole.ADMIN) "Administrator • VIP Access" else "VIP Member • Verified",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = onLogout,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated, contentColor = AccentRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("LOGOUT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Shizuku Storage Service
            item {
                GamingCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderGlow = shizukuStatus == ShizukuStatus.CONNECTED
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SHIZUKU SERVICE",
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp
                            )
                            IconButton(onClick = { shizukuManager.checkStatus() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NeonGreen)
                            }
                        }

                        ShizukuStatusPill(
                            status = shizukuStatus,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = when (shizukuStatus) {
                                ShizukuStatus.CONNECTED -> "✓ Connected and authorized. Ready to apply skins."
                                ShizukuStatus.PERMISSION_REQUIRED -> "Shizuku is running. Please grant permission to continue."
                                ShizukuStatus.RUNNING -> "Shizuku service is running. Waiting for authorization check."
                                ShizukuStatus.STOPPED -> "Shizuku service is stopped. Please start Shizuku to proceed."
                                ShizukuStatus.NOT_INSTALLED -> "Shizuku is not installed. Shizuku is required to apply skins on Android 11+."
                            },
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        if (shizukuStatus == ShizukuStatus.PERMISSION_REQUIRED) {
                            GamingButton(
                                text = "GRANT PERMISSION",
                                onClick = { shizukuManager.requestPermission() },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "req_shizuku_perm_button"
                            )
                        }
                    }
                }
            }

            // Target Storage Directory Card (Admin Only)
            if (currentUser?.role == UserRole.ADMIN) {
                item {
                    GamingCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "DEFAULT TARGET ROOT DIRECTORY",
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp
                            )
                            OutlinedTextField(
                                value = targetPath,
                                onValueChange = { targetPath = it },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonGreen,
                                    unfocusedBorderColor = CyberBorder
                                ),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // Cloud Synchronization Card
            item {
                GamingCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "CLOUD SYNCHRONIZATION",
                            color = NeonGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Status:", color = TextSecondary, fontSize = 12.sp)
                            Text(
                                text = if (syncState == "SYNCED") "Online & Updated" else "Connecting...",
                                color = if (syncState == "SYNCED") AccentGreen else AccentAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        GamingButton(
                            text = "CHECK FOR UPDATES",
                            onClick = {
                                scope.launch {
                                    repository.syncWithCloud()
                                    snackbarHostState.showSnackbar("Skins updated from cloud")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            isSecondary = true,
                            icon = Icons.Default.Sync
                        )
                    }
                }
            }

            // Brand & Version Card
            item {
                GamingCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("SH SKIN TOOL", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Text("OFFICIAL RELEASE BY SAIM TEAM", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                        Text("Version 1.0.0 (Build 2026.1)", color = TextMuted, fontSize = 11.sp)
                        Text("Safe, user-authorized utility for Android gaming devices.", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
