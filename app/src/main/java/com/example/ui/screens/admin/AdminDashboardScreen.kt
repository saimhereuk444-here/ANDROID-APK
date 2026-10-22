package com.example.ui.screens.admin

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.SkinToolRepository
import com.example.ui.components.GamingCard
import com.example.ui.components.GamingLogoutButton
import com.example.ui.components.GamingLogoutDialog
import com.example.ui.components.SkinImage
import com.example.ui.components.SkinImageCache
import com.example.ui.components.SyncStatusPill
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    repository: SkinToolRepository,
    onNavigateToCategories: () -> Unit = {},
    onNavigateToPackages: () -> Unit = {},
    onNavigateToSecurity: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSupport: () -> Unit = {},
    onSwitchToUserPanel: () -> Unit,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    BackHandler {
        activity?.moveTaskToBack(true)
    }

    var showLogoutDialog by remember { mutableStateOf(false) }

    val packages by repository.getAllPackagesAdmin().collectAsState(initial = emptyList())
    val syncState by repository.syncState.collectAsState()
    val appSettings by repository.appSettings.collectAsState()
    val unreadSupportCount by repository.getUnreadCountAdmin().collectAsState(initial = 0)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showIconsDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Column {
                        Text(
                            text = "ADMIN COMMAND CENTER",
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            fontSize = 17.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "SAIM TEAM CLOUD CONTROL",
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen,
                            fontSize = 11.sp,
                            letterSpacing = 1.5.sp
                        )
                    }
                },
                actions = {
                    SyncStatusPill(syncState = syncState)
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onSwitchToUserPanel,
                        modifier = Modifier.testTag("admin_switch_to_user_button")
                    ) {
                        Icon(
                            Icons.Default.SportsEsports,
                            contentDescription = "User Panel",
                            tint = NeonGreen
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    GamingLogoutButton(
                        onClick = { showLogoutDialog = true },
                        testTag = "admin_logout_button"
                    )
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
            // Main Hero: Categories & Skins Manager
            item {
                GamingCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderGlow = true,
                    onClick = onNavigateToCategories
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NeonGreen.copy(alpha = 0.15f))
                                        .border(1.dp, NeonGreen, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Category,
                                        contentDescription = null,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "CATEGORIES & SKINS",
                                        color = NeonGreen,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "${packages.size} Active Packages in Storage",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Button(
                                onClick = onNavigateToCategories,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonGreen,
                                    contentColor = Color(0xFF03200D)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("MANAGE", fontWeight = FontWeight.Black)
                            }
                        }

                        Text(
                            text = "Add and manage categories (with category cover images & Free Fire/Max mode) and add skins inside each category with dual Free Fire & Max files. All changes persist permanently on all devices.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // Quick Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "CATEGORIES & SKINS",
                        count = packages.size.toString(),
                        icon = Icons.Default.Category,
                        color = NeonGreen,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCategories
                    )
                    StatCard(
                        title = "CLOUD STORAGE",
                        count = "ACTIVE",
                        icon = Icons.Default.CloudDone,
                        color = MintGlow,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            scope.launch {
                                repository.syncWithCloud()
                                snackbarHostState.showSnackbar("Storage synced with cloud successfully!")
                            }
                        }
                    )
                }
            }

            // Core Focused Modules Section
            item {
                Text(
                    text = "CORE CONTROL MODULES",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 1. Categories & Skins Manager
                    AdminActionCard(
                        title = "Categories & Skins Manager",
                        subtitle = "Add categories with images (Free Fire / Max) and upload skins inside categories",
                        icon = Icons.Default.Category,
                        accentColor = NeonGreen,
                        onClick = onNavigateToCategories
                    )

                    // 2. Customer Service & User Mailbox
                    AdminActionCard(
                        title = if (unreadSupportCount > 0) "Customer Service Inbox ($unreadSupportCount NEW)" else "Customer Service & User Mailbox",
                        subtitle = "View customer support inquiries, reply to user mailboxes, and manage two-way threads",
                        icon = Icons.Default.SupportAgent,
                        accentColor = if (unreadSupportCount > 0) AccentRed else MintGlow,
                        onClick = onNavigateToSupport
                    )

                    // 2. Global Game PNG Badges
                    AdminActionCard(
                        title = "Game PNG Badges (FF & FF MAX)",
                        subtitle = "Configure Free Fire and Free Fire Max PNG icons shown during apply",
                        icon = Icons.Default.Image,
                        accentColor = MintGlow,
                        onClick = { showIconsDialog = true }
                    )

                    // 3. Security Passwords & Get Key Link
                    AdminActionCard(
                        title = "Access Passwords & Key Link",
                        subtitle = "Update user/admin access keys and 'GET ACCESS KEY' Telegram link",
                        icon = Icons.Default.Key,
                        accentColor = AccentAmber,
                        onClick = onNavigateToSecurity
                    )

                    // 4. Target Paths & System Config
                    AdminActionCard(
                        title = "Target Directory & System Config",
                        subtitle = "Configure Free Fire & Max Android/data/ paths and Shizuku status",
                        icon = Icons.Default.FolderSpecial,
                        accentColor = EmeraldGreen,
                        onClick = onNavigateToSettings
                    )

                    // 5. Direct User Panel Launch
                    AdminActionCard(
                        title = "Launch User Panel",
                        subtitle = "Switch directly to user interface to preview live skins and apply workflow",
                        icon = Icons.Default.SportsEsports,
                        accentColor = NeonGreen,
                        onClick = onSwitchToUserPanel
                    )
                }
            }
        }
    }

    // Game Icons / Badges Dialog
    if (showIconsDialog) {
        var ffIconInput by remember { mutableStateOf(appSettings.ffDefaultIconUrl) }
        var ffMaxIconInput by remember { mutableStateOf(appSettings.ffMaxDefaultIconUrl) }
        var uploadStateText by remember { mutableStateOf("") }
        var uploadProgressPct by remember { mutableStateOf(0) }
        var isUploading by remember { mutableStateOf(false) }

        // Helper to import image from Gallery URI into persistent Base64 Data URL and local file
        fun importGalleryIcon(uri: Uri, isMax: Boolean) {
            scope.launch(Dispatchers.IO) {
                try {
                    withContext(Dispatchers.Main) {
                        isUploading = true
                        uploadProgressPct = 0
                        uploadStateText = "Importing ${if (isMax) "Free Fire Max PNG" else "Free Fire PNG"}..."
                    }
                    val contentResolver = context.contentResolver
                    val inputStream = contentResolver.openInputStream(uri) ?: throw Exception("Could not open Gallery image")
                    val assetSize = try {
                        contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 1048576L
                    } catch (e: Throwable) { 1048576L }
                    val totalBytes = if (assetSize > 0) assetSize else 1048576L

                    val buffer = ByteArray(16384)
                    val outputStream = ByteArrayOutputStream()
                    var bytesRead = inputStream.read(buffer)
                    var totalRead = 0L

                    while (bytesRead != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        val pct = ((totalRead * 100) / totalBytes).coerceIn(0, 100).toInt()
                        val currentMb = String.format("%.2f", totalRead / (1024f * 1024f))
                        val totalMb = String.format("%.2f", totalBytes / (1024f * 1024f))

                        withContext(Dispatchers.Main) {
                            uploadProgressPct = pct
                            uploadStateText = "Uploading: $currentMb MB / $totalMb MB ($pct%)"
                        }
                        bytesRead = inputStream.read(buffer)
                    }
                    inputStream.close()

                    val rawBytes = outputStream.toByteArray()
                    val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                    val (dataUrl, targetBitmap, pngBytes) = if (bitmap != null) {
                        val maxDim = 192
                        val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                            val ratio = Math.min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
                            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
                        } else bitmap

                        val compressedStream = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.PNG, 100, compressedStream)
                        val bytes = compressedStream.toByteArray()
                        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        Triple("data:image/png;base64,$b64", scaled, bytes)
                    } else {
                        val b64 = Base64.encodeToString(rawBytes, Base64.NO_WRAP)
                        Triple("data:image/png;base64,$b64", null, rawBytes)
                    }

                    // Save local backup file in app files dir so it survives Gallery deletion and app restarts
                    try {
                        val iconsDir = File(context.filesDir, "icons")
                        iconsDir.mkdirs()
                        val localFile = File(iconsDir, if (isMax) "ffmax_icon.png" else "ff_icon.png")
                        FileOutputStream(localFile).use { it.write(pngBytes) }
                    } catch (e: Throwable) {
                        Log.w("AdminDashboard", "Could not save local icon file", e)
                    }

                    if (targetBitmap != null) {
                        SkinImageCache.put(dataUrl, targetBitmap)
                    }

                    withContext(Dispatchers.Main) {
                        isUploading = false
                        uploadStateText = "${if (isMax) "Free Fire Max PNG" else "Free Fire PNG"} Uploaded Successfully!"
                        if (isMax) {
                            ffMaxIconInput = dataUrl
                        } else {
                            ffIconInput = dataUrl
                        }
                    }
                } catch (e: Throwable) {
                    Log.e("AdminDashboard", "Failed to import icon from Gallery", e)
                    withContext(Dispatchers.Main) {
                        isUploading = false
                        uploadStateText = "Import Failed: ${e.localizedMessage}"
                    }
                }
            }
        }

        val ffGalleryPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { importGalleryIcon(it, isMax = false) }
        }

        val ffMaxGalleryPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { importGalleryIcon(it, isMax = true) }
        }

        AlertDialog(
            onDismissRequest = { showIconsDialog = false },
            containerColor = CyberSurface,
            title = {
                Text(
                    text = "CONFIGURE GAME PNG ICONS",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Upload the PNG icons directly from Gallery for Free Fire and Free Fire Max:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    // 1. Free Fire PNG Section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberSurfaceElevated)
                            .border(1.2.dp, NeonGreen.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Free Fire PNG", color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Black)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF092E17))
                                        .border(1.5.dp, NeonGreen, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SkinImage(
                                        model = ffIconInput,
                                        contentDescription = "Free Fire PNG",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop,
                                        fallbackIcon = Icons.Default.SportsEsports
                                    )
                                }

                                Button(
                                    onClick = { ffGalleryPicker.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF072412), contentColor = NeonGreen),
                                    border = BorderStroke(1.dp, NeonGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (ffIconInput.isNotBlank()) "CHANGE IMAGE" else "Upload from Gallery", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 2. Free Fire Max PNG Section
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberSurfaceElevated)
                            .border(1.2.dp, MintGlow.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Free Fire Max PNG", color = MintGlow, fontSize = 13.sp, fontWeight = FontWeight.Black)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF052B24))
                                        .border(1.5.dp, MintGlow, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SkinImage(
                                        model = ffMaxIconInput,
                                        contentDescription = "Free Fire Max PNG",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop,
                                        fallbackIcon = Icons.Default.Whatshot
                                    )
                                }

                                Button(
                                    onClick = { ffMaxGalleryPicker.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03221B), contentColor = MintGlow),
                                    border = BorderStroke(1.dp, MintGlow),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (ffMaxIconInput.isNotBlank()) "CHANGE IMAGE" else "Upload from Gallery", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Upload Progress Feedback
                    if (isUploading || uploadStateText.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0C2415))
                                .border(1.dp, NeonGreen, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = uploadStateText, color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    if (isUploading) {
                                        Text(text = "$uploadProgressPct%", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                if (isUploading) {
                                    LinearProgressIndicator(
                                        progress = { uploadProgressPct / 100f },
                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                        color = NeonGreen,
                                        trackColor = CyberSurface
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.updateGameIcons(ffIconInput, ffMaxIconInput)
                            showIconsDialog = false
                            snackbarHostState.showSnackbar("Game PNG Icons updated and stored permanently!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF03200D))
                ) {
                    Text("SAVE ICONS", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showIconsDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
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

@Composable
private fun StatCard(
    title: String,
    count: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurface)
            .border(1.2.dp, CyberBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = count,
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun AdminActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    GamingCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted
            )
        }
    }
}
