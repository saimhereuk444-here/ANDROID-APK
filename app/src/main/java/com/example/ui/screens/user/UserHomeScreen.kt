package com.example.ui.screens.user

import android.app.Activity
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CategoryEntity
import com.example.data.model.PackageEntity
import com.example.data.model.UserRole
import com.example.data.repository.SkinToolRepository
import com.example.domain.ApplyEngine
import com.example.domain.ApplyProgress
import com.example.domain.ConflictAction
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuStatus
import com.example.ui.components.GamingCard
import com.example.ui.components.GamingLogoutDialog
import com.example.ui.components.ShizukuStatusPill
import com.example.ui.components.SimpleProgressBar
import com.example.ui.components.SkinImage
import com.example.ui.components.VipGamerHeader
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHomeScreen(
    repository: SkinToolRepository,
    shizukuManager: ShizukuManager,
    applyEngine: ApplyEngine? = null,
    onNavigateToDynamicScreen: (String) -> Unit = {},
    onNavigateToPackageDetails: (String) -> Unit = {},
    onNavigateToFolder: (String) -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onNavigateToMailbox: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val scope = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }

    val packages by repository.getPublishedPackages().collectAsState(initial = emptyList())
    val categories by repository.getPublishedCategories().collectAsState(initial = emptyList())
    val syncState by repository.syncState.collectAsState()
    val shizukuStatus by shizukuManager.status.collectAsState()
    val appSettings by repository.appSettings.collectAsState()
    val currentUser by repository.currentUser.collectAsState()
    val activeUserId = currentUser?.userId ?: repository.getOrCreatePersistentUserId()
    val unreadMailCount by repository.getUnreadCountForUser(activeUserId).collectAsState(initial = 0)

    // Game Category State ("FREE_FIRE" or "FREE_FIRE_MAX", null initially to show Choose Game)
    var selectedGameMode by remember { mutableStateOf<String?>(null) }
    // Active Category Selection State (null = showing Category Grid/Cards, non-null = showing skins in that category)
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    BackHandler {
        if (selectedCategoryId != null) {
            selectedCategoryId = null
        } else if (selectedGameMode != null) {
            selectedGameMode = null
        } else {
            activity?.moveTaskToBack(true)
        }
    }

    // Auto-Logout if Admin changes user password or authVersion
    LaunchedEffect(appSettings.userPassword, appSettings.authVersion) {
        val currentUser = repository.currentUser.value
        if (currentUser?.role != com.example.data.model.UserRole.ADMIN) {
            if (com.example.security.SessionManager.isUserSessionExpired(appSettings.userPassword, appSettings.authVersion)) {
                repository.logout()
                onLogout()
            }
        }
    }

    // Filter packages strictly based on selected game category and uploaded files
    val displayedPackages = remember(packages, selectedGameMode) {
        when (selectedGameMode) {
            "FREE_FIRE" -> packages.filter { it.ffFileName.isNotBlank() || it.ffMaxFileName.isBlank() }
            "FREE_FIRE_MAX" -> packages.filter { it.ffMaxFileName.isNotBlank() || it.ffFileName.isBlank() }
            else -> packages
        }
    }

    // Apply Flow States
    var selectedPackageForApply by remember { mutableStateOf<PackageEntity?>(null) }
    var selectedGamePlatform by remember { mutableStateOf<String?>(null) } // "Free Fire" or "Free Fire MAX"
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showApplyProgressDialog by remember { mutableStateOf(false) }
    var targetPathForApply by remember { mutableStateOf(ShizukuManager.DEFAULT_TARGET_DIR) }
    val applyProgress = applyEngine?.progressState?.collectAsState()?.value ?: ApplyProgress.Idle

    // All Files / Manage Storage Permission State
    var hasManageFilesPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                android.os.Environment.isExternalStorageManager()
            } else {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasManageFilesPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    android.os.Environment.isExternalStorageManager()
                } else {
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        shizukuManager.checkStatus()
        repository.triggerSync()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            val activeGameIcon = if (selectedGameMode == "FREE_FIRE_MAX") appSettings.ffMaxDefaultIconUrl else if (selectedGameMode == "FREE_FIRE") appSettings.ffDefaultIconUrl else ""
            val isLoggedIn = currentUser != null && currentUser?.role == UserRole.USER && com.example.security.SessionManager.isUserSessionValid(appSettings.userPassword, appSettings.authVersion)
            val allPermissionsGranted = (shizukuStatus == ShizukuStatus.CONNECTED) && hasManageFilesPermission
            val showSupportIcons = isLoggedIn && allPermissionsGranted

            VipGamerHeader(
                syncState = syncState,
                onSettingsClick = onNavigateToSettings,
                selectedGame = selectedGameMode,
                gameIconUrl = activeGameIcon,
                showSupportActions = showSupportIcons,
                unreadMailCount = unreadMailCount,
                onCustomerServiceClick = onNavigateToSupport,
                onMailboxClick = onNavigateToMailbox,
                onSelectGame = { selectedGameMode = it },
                onOpenGameSelector = { selectedGameMode = null },
                onLogoutClick = { showLogoutDialog = true }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (shizukuStatus != ShizukuStatus.CONNECTED) {
                val isConnected = shizukuStatus == ShizukuStatus.PERMISSION_REQUIRED || shizukuStatus == ShizukuStatus.RUNNING

                // Gatekeeper Screen: Conditionally show Grant Permission only when Shizuku is connected
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GamingCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .testTag("shizuku_gatekeeper_card"),
                        borderGlow = isConnected
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = if (isConnected) Icons.Default.Shield else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isConnected) AccentAmber else AccentRed,
                                modifier = Modifier.size(56.dp)
                            )

                            Text(
                                text = if (isConnected) "SHIZUKU PERMISSION REQUIRED" else "SHIZUKU NOT CONNECTED",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = if (isConnected) {
                                    "Shizuku is running and connected on your phone. Please grant permission to continue and apply skins directly."
                                } else {
                                    "Please connect Shizuku and try again. Start the Shizuku service via Wireless Debugging or ADB, then tap Try Again."
                                },
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            ShizukuStatusPill(
                                status = shizukuStatus,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (isConnected) {
                                Button(
                                    onClick = { shizukuManager.requestPermission() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("grant_shizuku_permission_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonGreen,
                                        contentColor = Color(0xFF021B0B)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "GRANT PERMISSION",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = { shizukuManager.checkStatus() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("check_again_button"),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "CHECK AGAIN",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { shizukuManager.checkStatus() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("try_again_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonGreen,
                                        contentColor = Color(0xFF021B0B)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "TRY AGAIN",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        val launchIntent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                            ?: context.packageManager.getLaunchIntentForPackage("moe.shizuku.manager")
                                        if (launchIntent != null) {
                                            context.startActivity(launchIntent)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("open_shizuku_button"),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MintGlow),
                                    border = BorderStroke(1.dp, MintGlow.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "OPEN SHIZUKU APP",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (!hasManageFilesPermission) {
                // Gatekeeper Screen 2: ALLOW MANAGE FILES Permission Card
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GamingCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .testTag("manage_files_gatekeeper_card"),
                        borderGlow = true
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderCopy,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(56.dp)
                            )

                            Text(
                                text = "ALLOW MANAGE FILES",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                textAlign = TextAlign.Center,
                                letterSpacing = 1.sp
                            )

                            Text(
                                text = "Please allow file manage access so skin files can be extracted and pasted directly into your Free Fire target path without errors.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Button(
                                onClick = {
                                    try {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                                data = android.net.Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } else {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = android.net.Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        }
                                    } catch (e: Exception) {
                                        try {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                            context.startActivity(intent)
                                        } catch (ex: Exception) {
                                            // Fallback
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("grant_manage_files_permission_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonGreen,
                                    contentColor = Color(0xFF021B0B)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.FolderCopy, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ALLOW FILE ACCESS",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            } else if (selectedGameMode == null) {
                // CHOOSE YOUR GAME SCREEN (Free Fire vs Free Fire MAX)
                val ffCount = packages.count { it.ffFileName.isNotBlank() && it.ffFileName != "" }
                val ffMaxCount = packages.count { it.ffMaxFileName.isNotBlank() && it.ffMaxFileName != "" }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "CHOOSE YOUR GAME",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = "Select your edition to view compatible skins",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Card 1: FREE FIRE (Normal)
                    item {
                        GamingCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("choose_free_fire_card"),
                            borderGlow = true,
                            onClick = { selectedGameMode = "FREE_FIRE" }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF0C2B16))
                                                .border(1.5.dp, NeonGreen, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (appSettings.ffDefaultIconUrl.isNotBlank()) {
                                                SkinImage(
                                                    model = appSettings.ffDefaultIconUrl,
                                                    contentDescription = "Free Fire Icon",
                                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                    contentScale = ContentScale.Crop,
                                                    fallbackIcon = Icons.Default.SportsEsports
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.SportsEsports,
                                                    contentDescription = null,
                                                    tint = NeonGreen,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = "FREE FIRE",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Black,
                                                color = TextPrimary,
                                                letterSpacing = 1.sp
                                            )
                                            Text(
                                                text = "Standard Free Fire Edition",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF082613))
                                            .border(1.dp, NeonGreen.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "$ffCount SKINS",
                                            color = NeonGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = "Access all exclusive skins with Free Fire files uploaded by Admin.",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )

                                Button(
                                    onClick = { selectedGameMode = "FREE_FIRE" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonGreen,
                                        contentColor = Color(0xFF021B0B)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "SELECT FREE FIRE",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // Card 2: FREE FIRE MAX
                    item {
                        GamingCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("choose_free_fire_max_card"),
                            borderGlow = true,
                            onClick = { selectedGameMode = "FREE_FIRE_MAX" }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF052A22))
                                                .border(1.5.dp, MintGlow, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (appSettings.ffMaxDefaultIconUrl.isNotBlank()) {
                                                SkinImage(
                                                    model = appSettings.ffMaxDefaultIconUrl,
                                                    contentDescription = "Free Fire MAX Icon",
                                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                    contentScale = ContentScale.Crop,
                                                    fallbackIcon = Icons.Default.FlashOn
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.FlashOn,
                                                    contentDescription = null,
                                                    tint = MintGlow,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = "FREE FIRE MAX",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Black,
                                                color = TextPrimary,
                                                letterSpacing = 1.sp
                                            )
                                            Text(
                                                text = "MAX Edition Ultra HD",
                                                fontSize = 12.sp,
                                                color = MintGlow
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF03221B))
                                            .border(1.dp, MintGlow.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "$ffMaxCount SKINS",
                                            color = MintGlow,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = "Access all Ultra HD MAX skins with Free Fire MAX files uploaded by Admin.",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )

                                Button(
                                    onClick = { selectedGameMode = "FREE_FIRE_MAX" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MintGlow,
                                        contentColor = Color(0xFF021B0B)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "SELECT FREE FIRE MAX",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (selectedCategoryId == null) {
                // Shizuku Granted & Game Selected -> STEP 1: CATEGORY SELECTION VIEW
                val gameCategories = remember(categories, selectedGameMode) {
                    categories.filter { cat ->
                        cat.gameMode == selectedGameMode || cat.gameMode == "BOTH" || cat.gameMode.isBlank()
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Banner with Game Switch Option
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val activePng = if (selectedGameMode == "FREE_FIRE_MAX") appSettings.ffMaxDefaultIconUrl else appSettings.ffDefaultIconUrl
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (selectedGameMode == "FREE_FIRE_MAX") Color(0xFF0C3D32) else Color(0xFF1D5A30))
                                        .border(1.dp, if (selectedGameMode == "FREE_FIRE_MAX") MintGlow else NeonGreen, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (activePng.isNotBlank()) {
                                        SkinImage(
                                            model = activePng,
                                            contentDescription = "Game Logo",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (selectedGameMode == "FREE_FIRE_MAX") Icons.Default.FlashOn else Icons.Default.SportsEsports,
                                            contentDescription = null,
                                            tint = if (selectedGameMode == "FREE_FIRE_MAX") MintGlow else NeonGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = if (selectedGameMode == "FREE_FIRE_MAX") "FREE FIRE MAX" else "FREE FIRE",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "SELECT CATEGORY",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedGameMode == "FREE_FIRE_MAX") MintGlow else NeonGreen,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { selectedGameMode = null },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CHANGE GAME", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Category Cards List
                    if (gameCategories.isEmpty()) {
                        item {
                            GamingCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Category,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Text(
                                        text = "No Categories Found",
                                        color = TextSecondary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Add categories and skins for ${if (selectedGameMode == "FREE_FIRE_MAX") "Free Fire MAX" else "Free Fire"} from the Admin Panel.",
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        // Render Admin-Added Categories
                        items(gameCategories, key = { it.id }) { cat ->
                            val catSkinsCount = remember(packages, cat.id) {
                                packages.count { pkg -> pkg.categoryId == cat.id }
                            }

                            GamingCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("category_card_${cat.id}"),
                                borderGlow = true,
                                onClick = { selectedCategoryId = cat.id }
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // [ CATEGORY COVER IMAGE UPLOADED BY ADMIN ]
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(CyberSurfaceElevated)
                                            .border(
                                                width = 1.5.dp,
                                                brush = Brush.horizontalGradient(
                                                    if (selectedGameMode == "FREE_FIRE_MAX") listOf(MintGlow, EmeraldGreen, MintGlow)
                                                    else listOf(NeonGreen, MintGlow, NeonGreen)
                                                ),
                                                shape = RoundedCornerShape(14.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (cat.coverImageUrl.isNotBlank()) {
                                            SkinImage(
                                                model = cat.coverImageUrl,
                                                contentDescription = cat.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (selectedGameMode == "FREE_FIRE_MAX") Icons.Default.FlashOn else Icons.Default.SportsEsports,
                                                    contentDescription = null,
                                                    tint = if (selectedGameMode == "FREE_FIRE_MAX") MintGlow else NeonGreen,
                                                    modifier = Modifier.size(48.dp)
                                                )
                                                Text(
                                                    text = cat.name.uppercase(),
                                                    color = TextPrimary,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }

                                        // Overlay Badge for Skins Count
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(10.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xDD031A0D))
                                                .border(1.dp, (if (selectedGameMode == "FREE_FIRE_MAX") MintGlow else NeonGreen).copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "$catSkinsCount ${if (catSkinsCount == 1) "SKIN" else "SKINS"}",
                                                color = if (selectedGameMode == "FREE_FIRE_MAX") MintGlow else NeonGreen,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }

                                    // Category Details
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = cat.name,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black,
                                                color = TextPrimary
                                            )
                                            if (cat.description.isNotBlank()) {
                                                Text(
                                                    text = cat.description,
                                                    fontSize = 12.sp,
                                                    color = TextSecondary,
                                                    maxLines = 2
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = { selectedCategoryId = cat.id },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (selectedGameMode == "FREE_FIRE_MAX") MintGlow else NeonGreen,
                                                contentColor = Color(0xFF021B0B)
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("OPEN", fontWeight = FontWeight.Black, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Shizuku Granted & Category Selected -> STEP 2: SKINS INSIDE CATEGORY VIEW
                val activeCategory = remember(categories, selectedCategoryId) {
                    categories.firstOrNull { it.id == selectedCategoryId }
                }

                val categorySkins = remember(packages, selectedCategoryId) {
                    packages.filter { pkg ->
                        pkg.categoryId == selectedCategoryId
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Category Header & Back Button
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { selectedCategoryId = null },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ALL CATEGORIES", fontSize = 12.sp, fontWeight = FontWeight.Black)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyberSurfaceElevated)
                                    .border(1.dp, (if (selectedGameMode == "FREE_FIRE_MAX") MintGlow else NeonGreen).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${categorySkins.size} ${if (categorySkins.size == 1) "SKIN" else "SKINS"}",
                                    color = if (selectedGameMode == "FREE_FIRE_MAX") MintGlow else NeonGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    // Active Category Name Banner
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = activeCategory?.name?.uppercase() ?: "CATEGORY SKINS",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = 1.sp
                            )
                            if (!activeCategory?.description.isNullOrBlank()) {
                                Text(
                                    text = activeCategory!!.description,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Skin List inside the Selected Category
                    if (categorySkins.isEmpty()) {
                        item {
                            GamingCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.SportsEsports,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Text(
                                        text = "No skins found in this category",
                                        color = TextSecondary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Upload ${if (selectedGameMode == "FREE_FIRE_MAX") "Free Fire MAX" else "Free Fire"} skin files under '${activeCategory?.name ?: "this category"}' from Admin Panel.",
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(categorySkins, key = { it.id }) { pkg ->
                            GamingCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("skin_card_${pkg.id}"),
                                borderGlow = pkg.isFeatured
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // [ LARGE ADMIN-UPLOADED THUMBNAIL ]
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(CyberSurfaceElevated)
                                            .border(
                                                width = 1.5.dp,
                                                brush = Brush.horizontalGradient(
                                                    if (pkg.isFeatured) listOf(NeonGreen, MintGlow, NeonGreen)
                                                    else listOf(CyberBorder, CyberBorder)
                                                ),
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                    ) {
                                        SkinImage(
                                            model = pkg.coverImageUrl,
                                            contentDescription = pkg.name,
                                            packageId = pkg.id,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    // Skin Name
                                    Text(
                                        text = pkg.name,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )

                                    // Category / Version
                                    Text(
                                        text = if (pkg.description.isNotBlank()) pkg.description else "Exclusive Esports Skin",
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        maxLines = 2
                                    )

                                    Divider(color = CyberBorder, thickness = 0.8.dp)

                                    // Centered & Prominent APPLY NOW Button
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val isApplying = applyProgress is ApplyProgress.Validating || applyProgress is ApplyProgress.Downloading || applyProgress is ApplyProgress.Extracting || applyProgress is ApplyProgress.Applying
                                        Button(
                                            enabled = !isApplying,
                                            onClick = {
                                                if (isApplying) return@Button
                                                val isMax = selectedGameMode == "FREE_FIRE_MAX"
                                                val targetPath = if (isMax) {
                                                    "/storage/emulated/0/Android/data/com.dts.freefireth.max/files/"
                                                } else {
                                                    "/storage/emulated/0/Android/data/com.dts.freefireth/files/"
                                                }
                                                targetPathForApply = targetPath
                                                selectedGamePlatform = if (isMax) "Free Fire MAX" else "Free Fire"
                                                selectedPackageForApply = null
                                                showApplyProgressDialog = true
                                                applyEngine?.let { engine ->
                                                    engine.resetState()
                                                    scope.launch {
                                                        engine.applyPackage(
                                                            pkg = pkg,
                                                            targetRoot = targetPath,
                                                            defaultConflictAction = ConflictAction.REPLACE
                                                        )
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = NeonGreen,
                                                contentColor = Color(0xFF021B0B)
                                            ),
                                            shape = RoundedCornerShape(14.dp),
                                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .testTag("apply_now_button_${pkg.id}")
                                        ) {
                                            Icon(
                                                Icons.Default.FlashOn,
                                                contentDescription = null,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "APPLY NOW",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 16.sp,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Step 3: Real Asset Preparation & Apply Pipeline Progress Dialog
    if (showApplyProgressDialog) {
        AlertDialog(
            onDismissRequest = {
                if (applyProgress is ApplyProgress.Success || applyProgress is ApplyProgress.Failure) {
                    showApplyProgressDialog = false
                }
            },
            containerColor = CyberSurface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (applyProgress) {
                            is ApplyProgress.Success -> Icons.Default.CheckCircle
                            is ApplyProgress.Failure -> Icons.Default.Error
                            else -> Icons.Default.HourglassTop
                        },
                        contentDescription = null,
                        tint = when (applyProgress) {
                            is ApplyProgress.Success -> AccentGreen
                            is ApplyProgress.Failure -> AccentRed
                            else -> NeonGreen
                        }
                    )
                    Text(
                        text = when (applyProgress) {
                            is ApplyProgress.Success -> "100% Completed!"
                            is ApplyProgress.Failure -> "Installation Failed"
                            else -> "Installing Skin"
                        },
                        fontWeight = FontWeight.Black,
                        color = when (applyProgress) {
                            is ApplyProgress.Success -> AccentGreen
                            is ApplyProgress.Failure -> AccentRed
                            else -> NeonGreen
                        },
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (val state = applyProgress) {
                        is ApplyProgress.Idle -> {
                            Text("Initializing...", color = TextSecondary, fontSize = 13.sp)
                        }
                        is ApplyProgress.Validating -> {
                            Text(state.message, color = TextSecondary, fontSize = 13.sp)
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NeonGreen)
                        }
                        is ApplyProgress.Downloading -> {
                            SimpleProgressBar(
                                progressPercent = state.percent,
                                label = "Loading skin files (${state.percent}%)..."
                            )
                        }
                        is ApplyProgress.Extracting -> {
                            SimpleProgressBar(
                                progressPercent = state.percent,
                                label = "Extracting skin package (${state.percent}%)..."
                            )
                        }
                        is ApplyProgress.Applying -> {
                            SimpleProgressBar(
                                progressPercent = state.percent,
                                label = "Installing file ${state.currentFile}/${state.totalFiles} (${state.percent}%)..."
                            )
                        }
                        is ApplyProgress.Conflict -> {
                            Text("Resolving file conflicts...", color = AccentAmber, fontSize = 13.sp)
                        }
                        is ApplyProgress.Success -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "✓ 100% Completed!",
                                    color = AccentGreen,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Skin installed successfully.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        is ApplyProgress.Failure -> {
                            Text(
                                text = state.error,
                                color = AccentRed,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (applyProgress is ApplyProgress.Success || applyProgress is ApplyProgress.Failure) {
                    Button(
                        onClick = { showApplyProgressDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF021B0B)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("DONE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }

    GamingLogoutDialog(
        showDialog = showLogoutDialog,
        onDismiss = { showLogoutDialog = false },
        onConfirm = {
            showLogoutDialog = false
            onLogout()
        }
    )
}
