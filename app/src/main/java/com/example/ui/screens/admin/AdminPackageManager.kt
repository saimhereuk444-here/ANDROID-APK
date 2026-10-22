package com.example.ui.screens.admin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ContentStatus
import com.example.data.model.PackageEntity
import com.example.data.model.PackageFileEntity
import com.example.data.repository.SkinToolRepository
import com.example.ui.components.GamingCard
import com.example.ui.components.SkinImage
import com.example.ui.components.SkinImageCache
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPackageManager(
    repository: SkinToolRepository,
    onNavigateBack: () -> Unit
) {
    val packages by repository.getAllPackagesAdmin().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var selectedPackage by remember { mutableStateOf<PackageEntity?>(null) }
    var packageToDelete by remember { mutableStateOf<PackageEntity?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val filteredPackages = remember(packages, searchQuery) {
        if (searchQuery.isBlank()) packages
        else packages.filter { it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("SKIN & DUAL FILE MANAGER", fontWeight = FontWeight.Black, color = TextPrimary, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonGreen)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            selectedPackage = null
                            showDialog = true
                        },
                        modifier = Modifier.testTag("admin_add_package_button")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "New Skin Package", tint = NeonGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberSurface.copy(alpha = 0.85f))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedPackage = null
                    showDialog = true
                },
                containerColor = NeonGreen,
                contentColor = Color(0xFF03200D)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Skin")
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
            // Search & Info Header
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search skins by name...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonGreen) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = CyberBorder,
                        focusedContainerColor = CyberSurface.copy(alpha = 0.85f),
                        unfocusedContainerColor = CyberSurface.copy(alpha = 0.85f)
                    )
                )
            }

            if (filteredPackages.isEmpty()) {
                item {
                    GamingCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Inbox, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                            Text(
                                text = if (searchQuery.isBlank()) "No skins added yet. Tap '+' to create one." else "No skins match '$searchQuery'.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredPackages, key = { it.id }) { pkg ->
                    GamingCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pkg.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                StatusBadge(status = pkg.status)
                            }

                            // 16:9 Thumbnail Preview
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyberSurfaceElevated)
                                    .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                            ) {
                                SkinImage(
                                    model = pkg.coverImageUrl,
                                    contentDescription = pkg.name,
                                    packageId = pkg.id,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            if (pkg.description.isNotBlank()) {
                                Text(text = pkg.description, fontSize = 12.sp, color = TextSecondary)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        selectedPackage = pkg
                                        showDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Skin", tint = NeonGreen)
                                }

                                IconButton(
                                    onClick = { packageToDelete = pkg }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Skin", tint = AccentRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    var isSavingPackage by remember { mutableStateOf(false) }
    var isDeletingPackage by remember { mutableStateOf(false) }

    // Add / Edit Package Dialog
    if (showDialog) {
        PackageEditorDialog(
            initialPackage = selectedPackage,
            repository = repository,
            isParentSaving = isSavingPackage,
            onDismiss = {
                if (!isSavingPackage) {
                    showDialog = false
                }
            },
            onSave = { pkgToSave ->
                if (!isSavingPackage) {
                    isSavingPackage = true
                    scope.launch {
                        try {
                            repository.savePackage(pkgToSave)
                            withContext(Dispatchers.Main) {
                                isSavingPackage = false
                                showDialog = false
                                snackbarHostState.showSnackbar("Skin package saved permanently!")
                            }
                        } catch (e: Throwable) {
                            withContext(Dispatchers.Main) {
                                isSavingPackage = false
                                snackbarHostState.showSnackbar("Save failed: ${e.message}")
                            }
                        }
                    }
                }
            }
        )
    }

    // Delete Confirmation Dialog
    packageToDelete?.let { pkg ->
        AlertDialog(
            onDismissRequest = {
                if (!isDeletingPackage) {
                    packageToDelete = null
                }
            },
            containerColor = CyberSurface,
            title = {
                Text(
                    text = "PERMANENTLY DELETE SKIN?",
                    color = AccentRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${pkg.name}'? This will permanently remove the skin and files from storage and all devices.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    enabled = !isDeletingPackage,
                    onClick = {
                        isDeletingPackage = true
                        scope.launch {
                            try {
                                repository.deletePackage(pkg.id)
                                withContext(Dispatchers.Main) {
                                    isDeletingPackage = false
                                    packageToDelete = null
                                    snackbarHostState.showSnackbar("Skin '${pkg.name}' deleted permanently!")
                                }
                            } catch (e: Throwable) {
                                withContext(Dispatchers.Main) {
                                    isDeletingPackage = false
                                    packageToDelete = null
                                    snackbarHostState.showSnackbar("Delete error: ${e.message}")
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed, contentColor = Color.White)
                ) {
                    if (isDeletingPackage) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("YES, DELETE PERMANENTLY", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDeletingPackage,
                    onClick = { packageToDelete = null }
                ) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun PackageEditorDialog(
    initialPackage: PackageEntity?,
    repository: SkinToolRepository,
    defaultCategoryId: String? = null,
    isParentSaving: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (PackageEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentPkgId = remember(initialPackage?.id) { initialPackage?.id ?: UUID.randomUUID().toString() }

    val categories by repository.getAllCategoriesAdmin().collectAsState(initial = emptyList())
    var selectedCategoryId by remember(initialPackage?.id, defaultCategoryId) { mutableStateOf(initialPackage?.categoryId ?: defaultCategoryId) }

    LaunchedEffect(categories, initialPackage?.id) {
        if (selectedCategoryId == null && categories.isNotEmpty()) {
            selectedCategoryId = defaultCategoryId ?: initialPackage?.categoryId ?: categories.firstOrNull()?.id
        }
    }

    var name by remember(initialPackage?.id) { mutableStateOf(initialPackage?.name ?: "") }
    var description by remember(initialPackage?.id) { mutableStateOf(initialPackage?.description ?: "") }
    var coverUrl by remember(initialPackage?.id) { mutableStateOf(initialPackage?.coverImageUrl ?: "") }
    var ffFileName by remember(initialPackage?.id) { mutableStateOf(initialPackage?.ffFileName ?: "") }
    var ffMaxFileName by remember(initialPackage?.id) { mutableStateOf(initialPackage?.ffMaxFileName ?: "") }
    var version by remember(initialPackage?.id) { mutableStateOf(initialPackage?.version ?: "1.0") }
    var isFeatured by remember(initialPackage?.id) { mutableStateOf(initialPackage?.isFeatured ?: false) }

    var uploadStateText by remember { mutableStateOf("") }
    var uploadProgressPct by remember { mutableStateOf(0) }
    var isUploadingFile by remember { mutableStateOf(false) }

    // Helper to upload cover image with real MB and percentage progress & persistent storage
    fun uploadCoverImageFile(uri: Uri) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    isUploadingFile = true
                    uploadProgressPct = 0
                    uploadStateText = "Uploading Cover Image..."
                }

                val contentResolver = context.contentResolver
                val uploadsDir = File(context.filesDir, "uploads/$currentPkgId")
                uploadsDir.mkdirs()
                val tempRawFile = File(uploadsDir, "temp_raw_cover")

                val descriptor = try {
                    contentResolver.openAssetFileDescriptor(uri, "r")
                } catch (e: Throwable) { null }
                val rawLength = descriptor?.length ?: -1L
                val totalBytes = if (rawLength > 0L) rawLength else 1024000L
                try { descriptor?.close() } catch (t: Throwable) {}

                var readSuccess = false
                try {
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        FileOutputStream(tempRawFile).use { outputStream ->
                            val buffer = ByteArray(32768)
                            var bytesRead = inputStream.read(buffer)
                            var totalRead = 0L
                            var lastProgressUpdate = 0L

                            while (bytesRead != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                totalRead += bytesRead
                                val pct = ((totalRead * 100) / totalBytes).coerceIn(0, 100).toInt()
                                val now = System.currentTimeMillis()

                                // Throttle main-thread UI dispatches to avoid Choreographer / Looper flooding
                                if (now - lastProgressUpdate > 100 || bytesRead == -1) {
                                    lastProgressUpdate = now
                                    val currentMb = String.format(java.util.Locale.US, "%.2f", totalRead / (1024f * 1024f))
                                    val totalMb = String.format(java.util.Locale.US, "%.2f", maxOf(totalRead, totalBytes) / (1024f * 1024f))
                                    withContext(Dispatchers.Main) {
                                        uploadProgressPct = pct
                                        uploadStateText = "Uploading Cover: $currentMb MB / $totalMb MB ($pct%)"
                                    }
                                }
                                bytesRead = inputStream.read(buffer)
                            }
                        }
                    }
                    readSuccess = tempRawFile.exists() && tempRawFile.length() > 0
                } catch (e: Throwable) {
                    Log.e("AdminPackageManager", "Stream cover URI error", e)
                }

                if (!readSuccess) {
                    withContext(Dispatchers.Main) {
                        isUploadingFile = false
                        uploadStateText = "Image file could not be read"
                    }
                    return@launch
                }

                // Compute inSampleSize to safely prevent OutOfMemoryError on large camera images
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(tempRawFile.absolutePath, boundsOpts)
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

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val bitmap = try {
                    BitmapFactory.decodeFile(tempRawFile.absolutePath, decodeOptions)
                } catch (t: Throwable) {
                    Log.e("AdminPackageManager", "Bitmap decode error", t)
                    null
                }

                // Clean up raw temp file
                try { tempRawFile.delete() } catch (t: Throwable) {}

                if (bitmap != null) {
                    val (targetWidth, targetHeight) = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                        if (ratio > 1f) {
                            Pair(maxDimension, (maxDimension / ratio).toInt())
                        } else {
                            Pair((maxDimension * ratio).toInt(), maxDimension)
                        }
                    } else {
                        Pair(bitmap.width, bitmap.height)
                    }

                    val scaled = if (bitmap.width != targetWidth || bitmap.height != targetHeight) {
                        try {
                            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                        } catch (t: Throwable) {
                            bitmap
                        }
                    } else {
                        bitmap
                    }

                    // Save local backup file on disk
                    val localCoverFile = File(uploadsDir, "cover.jpg")
                    try {
                        FileOutputStream(localCoverFile).use { out ->
                            scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }
                    } catch (e: Throwable) {
                        Log.w("AdminPackageManager", "Could not save local cover image", e)
                    }

                    val localFileUri = "file://${localCoverFile.absolutePath}"

                    // Convert scaled image to permanent, self-contained Base64 JPEG Data URL for 100% cross-device sync
                    val baos = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                    val b64Bytes = baos.toByteArray()
                    val b64Encoded = Base64.encodeToString(b64Bytes, Base64.NO_WRAP)
                    val permanentCoverUri = "data:image/jpeg;base64,$b64Encoded"

                    // Cache in memory immediately for 0ms instantaneous display
                    SkinImageCache.put(permanentCoverUri, scaled)
                    SkinImageCache.put(localFileUri, scaled)
                    SkinImageCache.put("pkg_$currentPkgId", scaled)

                    // Set permanent HD cover ready state (NEVER auto-changed or overwritten)
                    withContext(Dispatchers.Main) {
                        coverUrl = permanentCoverUri
                        isUploadingFile = false
                        uploadProgressPct = 100
                        uploadStateText = "Cover Image Ready (HD)"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isUploadingFile = false
                        uploadStateText = "Failed to decode image"
                    }
                }
            } catch (e: Throwable) {
                Log.e("AdminPackageManager", "Image upload failed", e)
                withContext(Dispatchers.Main) {
                    isUploadingFile = false
                    uploadStateText = "Upload Error: ${e.localizedMessage ?: "Unknown Error"}"
                }
            }
        }
    }

    // Helper to stage game files (.zip / .unity3d) into app storage with instant ready state
    fun stageLocalFile(uri: Uri, isMax: Boolean) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    isUploadingFile = true
                    uploadProgressPct = 0
                    uploadStateText = "Staging ${if (isMax) "Free Fire MAX" else "Free Fire"} file..."
                }

                var queryName: String? = null
                if (uri.scheme == "content") {
                    try {
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (idx != -1) queryName = cursor.getString(idx)
                            }
                        }
                    } catch (e: Throwable) {
                        Log.w("AdminPackageManager", "Could not query display name", e)
                    }
                }
                if (queryName.isNullOrBlank()) {
                    queryName = uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null }
                }
                val validName = queryName ?: if (isMax) "ffmax_skin.zip" else "ff_skin.zip"

                val contentResolver = context.contentResolver
                val assetSize = try {
                    contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 5242880L
                } catch (e: Throwable) { 5242880L }

                val uploadDirApp = File(context.filesDir, "uploads/$currentPkgId")
                uploadDirApp.mkdirs()
                val targetFileApp = File(uploadDirApp, validName)

                var bytesCopied = 0L
                val totalBytes = if (assetSize > 0L) assetSize else 5242880L
                val buffer = ByteArray(65536)
                var lastProgressUpdate = 0L

                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFileApp).use { outputApp ->
                        var bytes = input.read(buffer)
                        while (bytes >= 0) {
                            outputApp.write(buffer, 0, bytes)
                            bytesCopied += bytes
                            val pct = ((bytesCopied * 100) / totalBytes).coerceIn(0, 100).toInt()
                            val now = System.currentTimeMillis()

                            // Throttle UI progress dispatches
                            if (now - lastProgressUpdate > 100 || bytes == -1) {
                                lastProgressUpdate = now
                                val currentMb = String.format(java.util.Locale.US, "%.2f", bytesCopied / (1024f * 1024f))
                                val totalMb = String.format(java.util.Locale.US, "%.2f", maxOf(bytesCopied, totalBytes) / (1024f * 1024f))
                                withContext(Dispatchers.Main) {
                                    uploadProgressPct = pct
                                    uploadStateText = "Saving File: $currentMb MB / $totalMb MB ($pct%)"
                                }
                            }
                            bytes = input.read(buffer)
                        }
                    }
                }

                // Also save persistent public backup
                try {
                    val publicDownloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    if (publicDownloads != null) {
                        val pubDir = File(publicDownloads, ".sh_skintool_uploads/$currentPkgId")
                        pubDir.mkdirs()
                        targetFileApp.copyTo(File(pubDir, validName), overwrite = true)
                    }
                } catch (t: Throwable) {
                    Log.w("AdminPackageManager", "Public backup note: ${t.message}")
                }

                // Invalidate any stale cache so newly staged/edited file is applied immediately
                try {
                    val externalBase = context.externalCacheDir ?: File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), ".sh_skintool_cache")
                    val cacheBase = File(externalBase, "pkg_cache/$currentPkgId")
                    if (cacheBase.exists()) cacheBase.deleteRecursively()
                } catch (t: Throwable) {
                    Log.w("AdminPackageManager", "Cache clear note: ${t.message}")
                }

                // File is fully copied to local storage and 100% ready for instant permanent save
                withContext(Dispatchers.Main) {
                    isUploadingFile = false
                    uploadProgressPct = 100
                    uploadStateText = "File Ready: $validName"
                    if (isMax) {
                        ffMaxFileName = validName
                    } else {
                        ffFileName = validName
                    }
                }

                // Save PackageFileEntity in Room and background sync (non-blocking)
                try {
                    val fileEntity = PackageFileEntity(
                        id = "${currentPkgId}_file_${if (isMax) "max" else "ff"}",
                        packageId = currentPkgId,
                        fileName = validName,
                        relativePath = if (isMax) "com.dts.freefireth.max/files/$validName" else "com.dts.freefireth/files/$validName",
                        downloadUrl = "",
                        fileSize = targetFileApp.length(),
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.savePackageFile(fileEntity)
                } catch (t: Throwable) {
                    Log.w("AdminPackageManager", "PackageFile save note: ${t.message}")
                }
            } catch (e: Throwable) {
                Log.e("AdminPackageManager", "File staging error", e)
                withContext(Dispatchers.Main) {
                    isUploadingFile = false
                    uploadStateText = "Upload Error: ${e.localizedMessage ?: "Unknown Error"}"
                }
            }
        }
    }

    // Pickers
    val coverImagePicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadCoverImageFile(it) }
    }
    val ffFilePicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { stageLocalFile(it, isMax = false) }
    }
    val ffMaxFilePicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { stageLocalFile(it, isMax = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Text(
                text = if (initialPackage == null) "ADD NEW SKIN & FILES" else "EDIT SKIN & FILES",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Cover Image Upload Frame (16:9 Rectangular)
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "COVER IMAGE (16:9 RECTANGULAR)",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = NeonGreen, spotColor = NeonGreen)
                                .clip(RoundedCornerShape(14.dp))
                                .background(CyberSurfaceElevated)
                                .border(
                                    width = 1.8.dp,
                                    brush = Brush.horizontalGradient(listOf(NeonGreen, MintGlow, NeonGreen)),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    try {
                                        coverImagePicker.launch("image/*")
                                    } catch (e: Throwable) {
                                        Log.e("AdminPackageManager", "Cannot launch image picker", e)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (coverUrl.isNotBlank()) {
                                SkinImage(
                                    model = coverUrl,
                                    contentDescription = "Cover Image Preview",
                                    packageId = currentPkgId,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(Color(0xCC03180C))
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(12.dp))
                                        Text("CHANGE IMAGE", color = NeonGreen, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(36.dp))
                                    Text(
                                        text = "TAP TO UPLOAD\nCOVER IMAGE",
                                        color = TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = coverUrl,
                            onValueChange = { coverUrl = it },
                            placeholder = { Text("Or paste image URL directly...", color = TextMuted, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = CyberBorder
                            )
                        )
                    }
                }

                // Category Selection Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SELECT CATEGORY",
                                color = NeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            if (selectedCategoryId != null) {
                                val selectedCatName = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Selected"
                                Text(
                                    text = selectedCatName,
                                    color = MintGlow,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (categories.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberSurfaceElevated)
                                    .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "Please create a category first in Categories & Skins Manager before adding skins.",
                                    color = AccentAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                categories.chunked(2).forEach { rowCats ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowCats.forEach { cat ->
                                            FilterChip(
                                                selected = selectedCategoryId == cat.id,
                                                onClick = { selectedCategoryId = cat.id },
                                                label = {
                                                    Text(
                                                        text = "${cat.name} (${if (cat.gameMode == "FREE_FIRE_MAX") "MAX" else if (cat.gameMode == "BOTH") "ALL" else "FF"})",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = if (cat.gameMode == "FREE_FIRE_MAX") MintGlow else NeonGreen,
                                                    selectedLabelColor = Color(0xFF021B0B),
                                                    containerColor = CyberSurfaceElevated,
                                                    labelColor = TextPrimary
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (rowCats.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Skin Name
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Skin Name *") },
                        placeholder = { Text("e.g. Draco AK-47 & Cyber Bundle") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = CyberBorder
                        )
                    )
                }

                // Description
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Skin Description") },
                        placeholder = { Text("e.g. Legendary weapon & suit skin") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = CyberBorder
                        )
                    )
                }

                // Upload Progress Bar
                if (isUploadingFile || uploadStateText.isNotBlank()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0C2415))
                                .border(1.dp, NeonGreen, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = uploadStateText,
                                        color = NeonGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$uploadProgressPct%",
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { uploadProgressPct / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = NeonGreen,
                                    trackColor = CyberSurface
                                )
                            }
                        }
                    }
                }

                // Free Fire File Section
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberSurfaceElevated)
                            .border(1.2.dp, NeonGreen.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("FREE FIRE FILE", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                if (ffFileName.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                                        Text("Ready", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    try {
                                        ffFilePicker.launch("*/*")
                                    } catch (e: Throwable) {
                                        Log.e("AdminPackageManager", "Cannot launch file picker", e)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF072412), contentColor = NeonGreen),
                                border = BorderStroke(1.dp, NeonGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("UPLOAD FREE FIRE FILE (.ZIP / .UNITY3D)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (ffFileName.isNotBlank()) {
                                Text(
                                    text = "File: $ffFileName",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Free Fire MAX File Section
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberSurfaceElevated)
                            .border(1.2.dp, MintGlow.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("FREE FIRE MAX FILE", color = MintGlow, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                if (ffMaxFileName.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MintGlow, modifier = Modifier.size(14.dp))
                                        Text("Ready", color = MintGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    try {
                                        ffMaxFilePicker.launch("*/*")
                                    } catch (e: Throwable) {
                                        Log.e("AdminPackageManager", "Cannot launch MAX file picker", e)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03221B), contentColor = MintGlow),
                                border = BorderStroke(1.dp, MintGlow),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("UPLOAD FREE FIRE MAX FILE (.ZIP / .UNITY3D)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (ffMaxFileName.isNotBlank()) {
                                Text(
                                    text = "File: $ffMaxFileName",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Version & Featured Switch
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Featured on Home Screen", color = TextPrimary, fontSize = 13.sp)
                        Switch(
                            checked = isFeatured,
                            onCheckedChange = { isFeatured = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isParentSaving && !isUploadingFile && (name.isNotBlank() || coverUrl.isNotBlank() || ffFileName.isNotBlank() || ffMaxFileName.isNotBlank()),
                onClick = {
                    if (!isParentSaving && !isUploadingFile) {
                        val finalName = name.trim().ifBlank {
                            if (ffMaxFileName.isNotBlank()) "FF MAX Skin" else "Free Fire Skin"
                        }
                        val uploadDir = File(context.filesDir, "uploads/$currentPkgId")
                        val activeFile = listOfNotNull(
                            if (ffFileName.isNotBlank()) File(uploadDir, ffFileName) else null,
                            if (ffMaxFileName.isNotBlank()) File(uploadDir, ffMaxFileName) else null
                        ).firstOrNull { it.exists() && it.length() > 0 }
                        val computedSize = activeFile?.length() ?: initialPackage?.fileSize ?: 4500000L

                        val pkg = PackageEntity(
                            id = currentPkgId,
                            name = finalName,
                            description = description.trim(),
                            coverImageUrl = coverUrl.trim(),
                            categoryId = selectedCategoryId,
                            ffFileName = ffFileName.trim(),
                            ffMaxFileName = ffMaxFileName.trim(),
                            iconPngUrl = "",
                            ffIconPngUrl = "",
                            ffMaxIconPngUrl = "",
                            version = version.trim().ifBlank { "1.0" },
                            status = ContentStatus.PUBLISHED,
                            isFeatured = isFeatured,
                            fileSize = computedSize,
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(pkg)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF03200D))
            ) {
                if (isParentSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color(0xFF03200D),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("SAVE & STORE PERMANENTLY", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isParentSaving,
                onClick = onDismiss
            ) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}
