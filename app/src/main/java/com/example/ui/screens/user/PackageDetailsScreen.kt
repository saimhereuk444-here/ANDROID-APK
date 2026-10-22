package com.example.ui.screens.user

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.repository.SkinToolRepository
import com.example.domain.ApplyEngine
import com.example.domain.ApplyProgress
import com.example.domain.ConflictAction
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuStatus
import com.example.ui.components.GamingButton
import com.example.ui.components.GamingCard
import com.example.ui.components.SimpleProgressBar
import com.example.ui.components.SkinImage
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageDetailsScreen(
    packageId: String,
    repository: SkinToolRepository,
    applyEngine: ApplyEngine,
    shizukuManager: ShizukuManager,
    onNavigateBack: () -> Unit
) {
    val pkg by repository.getPackage(packageId).collectAsState(initial = null)
    val currentUser by repository.currentUser.collectAsState()
    val isFavorite by repository.isFavorite(packageId, currentUser?.userId ?: "default_user").collectAsState(initial = false)
    val applyProgress by applyEngine.progressState.collectAsState()
    val appSettings by repository.appSettings.collectAsState()
    val shizukuStatus by shizukuManager.status.collectAsState()

    var showGameChoiceDialog by remember { mutableStateOf(false) }
    var showApplyDialog by remember { mutableStateOf(false) }
    var targetPath by remember { mutableStateOf(ShizukuManager.DEFAULT_TARGET_DIR) }
    var selectedGameName by remember { mutableStateOf("Free Fire") }
    var conflictResolution by remember { mutableStateOf(ConflictAction.REPLACE) }

    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(
                        text = pkg?.name ?: "Skin Details",
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        maxLines = 1,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonGreen)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            pkg?.let { packageItem ->
                                scope.launch {
                                    repository.toggleFavorite(
                                        packageItem.id,
                                        currentUser?.userId ?: "default_user",
                                        isFavorite
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) AccentRed else TextMuted
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberSurface.copy(alpha = 0.85f))
            )
        }
    ) { innerPadding ->
        pkg?.let { packageItem ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Ultra HD 16:9 Hero Image Banner
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = NeonGreen, spotColor = NeonGreen)
                                .clip(RoundedCornerShape(18.dp))
                                .background(CyberSurfaceElevated)
                                .border(1.5.dp, Brush.horizontalGradient(listOf(NeonGreen, MintGlow, NeonGreen)), RoundedCornerShape(18.dp))
                        ) {
                            if (packageItem.coverImageUrl.isNotBlank()) {
                                SkinImage(
                                    model = packageItem.coverImageUrl,
                                    contentDescription = packageItem.name,
                                    packageId = packageItem.id,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    Color(0x77040F08),
                                                    Color(0xF0020804)
                                                )
                                            )
                                        )
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.SportsEsports,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .align(Alignment.Center),
                                    tint = NeonGreen
                                )
                            }

                            // VIP Badge on Hero Image
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xE6021A0C))
                                    .border(1.dp, NeonGreen, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(13.dp))
                                    Text("VIP EXCLUSIVE", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }

                    // Package Title & Meta Header
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = packageItem.name,
                                    color = TextPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black
                                )
                                StatusBadge(status = packageItem.status)
                            }

                            if (packageItem.description.isNotBlank()) {
                                Text(
                                    text = packageItem.description,
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    // Game Selection / Quick Spec Card
                    item {
                        GamingCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "GAME COMPATIBILITY",
                                        color = NeonGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Free Fire Chip
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(CyberSurfaceElevated)
                                            .border(1.2.dp, NeonGreen.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("Free Fire", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text("Supported", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }

                                    // Free Fire Max Chip
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(CyberSurfaceElevated)
                                            .border(1.2.dp, MintGlow.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("Free Fire Max", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            Text("Supported", color = MintGlow, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Sticky Bottom Bar with Apply Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberSurface)
                        .border(
                            width = 1.dp,
                            color = CyberBorder,
                            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                        )
                        .padding(16.dp)
                ) {
                    val isApplying = applyProgress is ApplyProgress.Validating || applyProgress is ApplyProgress.Downloading || applyProgress is ApplyProgress.Extracting || applyProgress is ApplyProgress.Applying
                    GamingButton(
                        text = if (isApplying) "INSTALLING..." else "APPLY NOW",
                        icon = Icons.Default.FlashOn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apply_package_button"),
                        enabled = !isApplying,
                        onClick = {
                            if (isApplying) return@GamingButton
                            packageItem.let { item ->
                                if (shizukuStatus != ShizukuStatus.CONNECTED) {
                                    shizukuManager.requestPermission()
                                    applyEngine.resetState()
                                    showApplyDialog = true
                                    scope.launch {
                                        applyEngine.applyPackage(item, targetPath)
                                    }
                                } else {
                                    val hasFF = item.ffFileName.isNotBlank()
                                    val hasMax = item.ffMaxFileName.isNotBlank()

                                    if (hasFF && hasMax) {
                                        showGameChoiceDialog = true
                                    } else if (hasMax && !hasFF) {
                                        selectedGameName = "Free Fire Max"
                                        targetPath = "/storage/emulated/0/Android/data/com.dts.freefireth.max/files/"
                                        showApplyDialog = true
                                        applyEngine.resetState()
                                        scope.launch {
                                            applyEngine.applyPackage(item, targetPath, conflictResolution)
                                        }
                                    } else {
                                        selectedGameName = "Free Fire"
                                        targetPath = "/storage/emulated/0/Android/data/com.dts.freefireth/files/"
                                        showApplyDialog = true
                                        applyEngine.resetState()
                                        scope.launch {
                                            applyEngine.applyPackage(item, targetPath, conflictResolution)
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        } ?: run {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NeonGreen)
            }
        }
    }

    var showConfirmationDialog by remember { mutableStateOf(false) }

    // Game Choice Selection Dialog (EXACTLY TWO OPTIONS: Left = Free Fire, Right = Free Fire MAX)
    if (showGameChoiceDialog && pkg != null) {
        val packageItem = pkg!!
        val ffIcon = appSettings.ffDefaultIconUrl.ifBlank { packageItem.ffIconPngUrl.ifBlank { packageItem.iconPngUrl } }
        val ffMaxIcon = appSettings.ffMaxDefaultIconUrl.ifBlank { packageItem.ffMaxIconPngUrl.ifBlank { packageItem.iconPngUrl } }

        AlertDialog(
            onDismissRequest = { showGameChoiceDialog = false },
            containerColor = CyberSurface,
            title = {
                Text(
                    text = "Select Game Platform",
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // LEFT SIDE: Free Fire Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = NeonGreen, spotColor = NeonGreen)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CyberSurfaceElevated)
                            .border(1.8.dp, NeonGreen, RoundedCornerShape(16.dp))
                            .clickable {
                                selectedGameName = "Free Fire"
                                targetPath = "/storage/emulated/0/Android/data/com.dts.freefireth/files/"
                                showGameChoiceDialog = false
                                showConfirmationDialog = true
                            }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Free Fire Logo Frame
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF092E17))
                                    .border(1.5.dp, NeonGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                SkinImage(
                                    model = ffIcon,
                                    contentDescription = "Free Fire Logo",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                    fallbackIcon = Icons.Default.SportsEsports
                                )
                            }

                            // Free Fire Text
                            Text(
                                text = "Free Fire",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // RIGHT SIDE: Free Fire Max Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = MintGlow, spotColor = MintGlow)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CyberSurfaceElevated)
                            .border(1.8.dp, MintGlow, RoundedCornerShape(16.dp))
                            .clickable {
                                selectedGameName = "Free Fire Max"
                                targetPath = "/storage/emulated/0/Android/data/com.dts.freefireth.max/files/"
                                showGameChoiceDialog = false
                                showConfirmationDialog = true
                            }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Free Fire Max Logo Frame
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF052B24))
                                    .border(1.5.dp, MintGlow, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                SkinImage(
                                    model = ffMaxIcon,
                                    contentDescription = "Free Fire Max Logo",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                    fallbackIcon = Icons.Default.Whatshot
                                )
                            }

                            // Free Fire Max Text
                            Text(
                                text = "Free Fire Max",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showGameChoiceDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }

    // Clean Confirmation Dialog
    if (showConfirmationDialog && pkg != null) {
        val packageItem = pkg!!
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            containerColor = CyberSurface,
            title = {
                Text(
                    text = "Confirm Installation",
                    color = TextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "Skin", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = packageItem.name, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Divider(color = CyberBorder, thickness = 0.8.dp)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "Platform", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = selectedGameName, color = NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmationDialog = false
                        showApplyDialog = true
                        applyEngine.resetState()
                        scope.launch {
                            applyEngine.applyPackage(
                                pkg = packageItem,
                                targetRoot = targetPath,
                                defaultConflictAction = conflictResolution
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF021B0B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("CONTINUE", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmationDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }

    // Apply Real Extraction & Pasting Progress Dialog
    if (showApplyDialog) {
        AlertDialog(
            onDismissRequest = {
                if (applyProgress is ApplyProgress.Success || applyProgress is ApplyProgress.Failure) {
                    showApplyDialog = false
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
                            is ApplyProgress.Failure -> "Apply Failed"
                            else -> "Applying to $selectedGameName"
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
                                    text = "Skin installed successfully into $selectedGameName.",
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
                        onClick = { showApplyDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF021B0B))
                    ) {
                        Text("DONE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }
}
