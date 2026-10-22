package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.ContentStatus
import com.example.data.model.ScreenEntity
import com.example.data.repository.SkinToolRepository
import com.example.ui.components.GamingButton
import com.example.ui.components.GamingCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import android.util.Log
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreensManager(
    repository: SkinToolRepository,
    onNavigateBack: () -> Unit
) {
    val screens by repository.getAllScreensAdmin().collectAsState(initial = emptyList())
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedScreenToEdit by remember { mutableStateOf<ScreenEntity?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = CyberBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("DYNAMIC SCREEN BUILDER", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonGreen)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            selectedScreenToEdit = null
                            showEditDialog = true
                        },
                        modifier = Modifier.testTag("admin_add_screen_button")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Create Screen", tint = NeonGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedScreenToEdit = null
                    showEditDialog = true
                },
                containerColor = NeonGreen,
                contentColor = CyberBackground
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Screen")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Screens configured here render automatically on all user devices after publishing.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            if (screens.isEmpty()) {
                item {
                    GamingCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No screens created yet. Tap '+' to create your first dynamic user screen.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(screens) { screen ->
                    GamingCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = screen.title,
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    StatusBadge(status = screen.status)
                                }
                                Text(
                                    text = screen.description.ifBlank { "No description" },
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Order: ${screen.displayOrder} • Version: ${screen.version}",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        selectedScreenToEdit = screen
                                        showEditDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = NeonGreen)
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                repository.deleteScreen(screen.id)
                                                snackbarHostState.showSnackbar("Screen deleted")
                                            } catch (t: Throwable) {
                                                snackbarHostState.showSnackbar("Error deleting: ${t.localizedMessage}")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        ScreenEditorDialog(
            initialScreen = selectedScreenToEdit,
            repository = repository,
            onDismiss = { showEditDialog = false },
            onSave = { screenToSave ->
                scope.launch {
                    try {
                        repository.saveScreen(screenToSave)
                        showEditDialog = false
                        snackbarHostState.showSnackbar("Screen saved successfully")
                    } catch (t: Throwable) {
                        snackbarHostState.showSnackbar("Error saving screen: ${t.localizedMessage}")
                    }
                }
            }
        )
    }
}

@Composable
private fun ScreenEditorDialog(
    initialScreen: ScreenEntity?,
    repository: SkinToolRepository,
    onDismiss: () -> Unit,
    onSave: (ScreenEntity) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val screenId = remember { initialScreen?.id ?: UUID.randomUUID().toString() }

    var title by remember { mutableStateOf(initialScreen?.title ?: "") }
    var description by remember { mutableStateOf(initialScreen?.description ?: "") }
    var coverUrl by remember { mutableStateOf(initialScreen?.coverImageUrl ?: "") }
    var order by remember { mutableStateOf(initialScreen?.displayOrder?.toString() ?: "1") }
    var status by remember { mutableStateOf(initialScreen?.status ?: ContentStatus.PUBLISHED) }
    var isUploadingImage by remember { mutableStateOf(false) }

    val imagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { isUploadingImage = true }
                    val contentResolver = context.contentResolver
                    val screenDir = java.io.File(context.filesDir, "screens/$screenId")
                    screenDir.mkdirs()
                    val tempRawFile = java.io.File(screenDir, "temp_raw_image")

                    var readSuccess = false
                    try {
                        contentResolver.openInputStream(uri)?.use { input ->
                            java.io.FileOutputStream(tempRawFile).use { output ->
                                val buffer = ByteArray(32768)
                                var bytes = input.read(buffer)
                                while (bytes >= 0) {
                                    output.write(buffer, 0, bytes)
                                    bytes = input.read(buffer)
                                }
                            }
                        }
                        readSuccess = tempRawFile.exists() && tempRawFile.length() > 0
                    } catch (e: Throwable) {
                        Log.e("AdminScreensManager", "Failed to stream screen image", e)
                    }

                    if (!readSuccess) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { isUploadingImage = false }
                        return@launch
                    }

                    val boundsOpts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    android.graphics.BitmapFactory.decodeFile(tempRawFile.absolutePath, boundsOpts)
                    val origW = boundsOpts.outWidth
                    val origH = boundsOpts.outHeight
                    var sampleSize = 1
                    val maxDimension = 1080
                    if (origW > maxDimension || origH > maxDimension) {
                        val halfW = origW / 2
                        val halfH = origH / 2
                        while ((halfW / sampleSize) >= maxDimension && (halfH / sampleSize) >= maxDimension) {
                            sampleSize *= 2
                        }
                    }

                    val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                    }
                    val bitmap = try {
                        android.graphics.BitmapFactory.decodeFile(tempRawFile.absolutePath, decodeOptions)
                    } catch (t: Throwable) {
                        Log.e("AdminScreensManager", "Bitmap decode error", t)
                        null
                    }

                    try { tempRawFile.delete() } catch (t: Throwable) {}

                    if (bitmap != null) {
                        val localCover = java.io.File(screenDir, "cover.jpg")
                        try {
                            java.io.FileOutputStream(localCover).use { out ->
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                            }
                        } catch (e: Throwable) {
                            Log.e("AdminScreensManager", "Error saving local screen cover", e)
                        }

                        val localFileUri = "file://${localCover.absolutePath}"
                        com.example.ui.components.SkinImageCache.put(localFileUri, bitmap)
                        com.example.ui.components.SkinImageCache.put("screen_$screenId", bitmap)

                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            coverUrl = localFileUri
                            isUploadingImage = false
                        }

                        if (localCover.exists() && localCover.length() > 0) {
                            try {
                                val cloudUrl = repository.cloudSyncEngine.uploadBinaryFileToCloud(localCover)
                                if (!cloudUrl.isNullOrBlank()) {
                                    com.example.ui.components.SkinImageCache.put(cloudUrl, bitmap)
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        coverUrl = cloudUrl
                                    }
                                }
                            } catch (t: Throwable) {
                                Log.w("AdminScreensManager", "Cloud image upload note: ${t.message}")
                            }
                        }
                    } else {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { isUploadingImage = false }
                    }
                } catch (t: Throwable) {
                    Log.e("AdminScreensManager", "Image picking error", t)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { isUploadingImage = false }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Text(
                text = if (initialScreen == null) "CREATE DYNAMIC SCREEN" else "EDIT SCREEN",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Screen Title *") },
                    placeholder = { Text("e.g. VIP SKINS") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = CyberBorder
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Optional description") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = CyberBorder
                    )
                )

                // Image Upload Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = coverUrl,
                        onValueChange = { coverUrl = it },
                        label = { Text("Cover Image URL / Base64") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = CyberBorder
                        )
                    )
                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = CyberBackground),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isUploadingImage) "..." else "PICK", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = order,
                    onValueChange = { order = it },
                    label = { Text("Display Order") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = CyberBorder
                    )
                )

                // Status Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(ContentStatus.PUBLISHED, ContentStatus.DRAFT, ContentStatus.DISABLED).forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (s == ContentStatus.PUBLISHED) AccentGreen else if (s == ContentStatus.DRAFT) AccentAmber else AccentRed,
                                selectedLabelColor = CyberBackground
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isUploadingImage && (title.isNotBlank() || coverUrl.isNotBlank()),
                onClick = {
                    val finalTitle = title.trim().ifBlank { "Dynamic Screen" }
                    val screen = ScreenEntity(
                        id = initialScreen?.id ?: UUID.randomUUID().toString(),
                        title = finalTitle,
                        description = description.trim(),
                        coverImageUrl = coverUrl.trim(),
                        displayOrder = order.toIntOrNull() ?: 1,
                        status = status,
                        version = (initialScreen?.version ?: 0L) + 1L,
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(screen)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = CyberBackground)
            ) {
                Text("SAVE & SYNC", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}
