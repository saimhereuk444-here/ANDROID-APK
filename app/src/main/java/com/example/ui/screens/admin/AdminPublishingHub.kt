package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.PublishedWithChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContentStatus
import com.example.data.repository.SkinToolRepository
import com.example.ui.components.GamingButton
import com.example.ui.components.GamingCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPublishingHub(
    repository: SkinToolRepository,
    onNavigateBack: () -> Unit
) {
    val screens by repository.getAllScreensAdmin().collectAsState(initial = emptyList())
    val packages by repository.getAllPackagesAdmin().collectAsState(initial = emptyList())
    var isPublishing by remember { mutableStateOf(false) }
    var publishSuccessMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val draftScreens = remember(screens) { screens.filter { it.status == ContentStatus.DRAFT } }
    val draftPackages = remember(packages) { packages.filter { it.status == ContentStatus.DRAFT } }

    Scaffold(
        containerColor = CyberBackground,
        topBar = {
            TopAppBar(
                title = { Text("PUBLISHING HUB", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberSurface)
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
            // Main Action Card
            item {
                GamingCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderGlow = true
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(28.dp))
                            Column {
                                Text("SYNCHRONIZE & PUBLISH", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Push all changes to Firebase Cloud backend", color = TextSecondary, fontSize = 12.sp)
                            }
                        }

                        Text(
                            text = "Draft content remains private on this admin device until you publish. When you press publish, all draft screens, packages, and folders become instantly visible on user devices without rebuilding the APK.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        GamingButton(
                            text = if (isPublishing) "PUBLISHING TO CLOUD..." else "PUBLISH TO USER PANEL",
                            onClick = {
                                isPublishing = true
                                publishSuccessMessage = null
                                scope.launch {
                                    // Publish all draft screens
                                    draftScreens.forEach { screen ->
                                        repository.saveScreen(screen.copy(status = ContentStatus.PUBLISHED, updatedAt = System.currentTimeMillis()))
                                    }
                                    // Publish all draft packages
                                    draftPackages.forEach { pkg ->
                                        repository.savePackage(pkg.copy(status = ContentStatus.PUBLISHED, updatedAt = System.currentTimeMillis()))
                                    }
                                    repository.syncWithCloud()
                                    isPublishing = false
                                    publishSuccessMessage = "PUBLISHED SUCCESSFULLY"
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("publish_all_button"),
                            enabled = !isPublishing,
                            icon = Icons.Default.PublishedWithChanges
                        )

                        if (publishSuccessMessage != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = publishSuccessMessage!!,
                                    color = AccentGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Summary of Drafts
            item {
                Text(
                    text = "PENDING DRAFTS (${draftScreens.size + draftPackages.size})",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            if (draftScreens.isEmpty() && draftPackages.isEmpty()) {
                item {
                    GamingCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "All screens and packages are currently published and in sync with the cloud.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(draftScreens) { screen ->
                    GamingCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("SCREEN: ${screen.title}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(screen.description, color = TextMuted, fontSize = 11.sp)
                            }
                            StatusBadge(status = screen.status)
                        }
                    }
                }

                items(draftPackages) { pkg ->
                    GamingCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("PACKAGE: ${pkg.name}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Version ${pkg.version}", color = TextMuted, fontSize = 11.sp)
                            }
                            StatusBadge(status = pkg.status)
                        }
                    }
                }
            }
        }
    }
}
