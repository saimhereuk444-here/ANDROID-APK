package com.example.ui.screens.admin

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.data.model.FolderEntity
import com.example.data.model.PackageEntity
import com.example.data.repository.SkinToolRepository
import com.example.ui.components.GamingCard
import com.example.ui.components.SkinImage
import com.example.ui.components.SkinImageCache
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
fun AdminCategoryFolderManager(
    repository: SkinToolRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val categories by repository.getAllCategoriesAdmin().collectAsState(initial = emptyList())
    val folders by repository.getAllFoldersAdmin().collectAsState(initial = emptyList())
    val allPackages by repository.getAllPackagesAdmin().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) } // 0: CATEGORIES, 1: FOLDERS
    var selectedGameFilter by remember { mutableStateOf("ALL") } // "ALL", "FREE_FIRE", "FREE_FIRE_MAX"

    // Active Category Detail View (null = showing list of categories, non-null = viewing skins inside that category)
    var selectedCategoryForDetail by remember { mutableStateOf<CategoryEntity?>(null) }

    BackHandler {
        if (selectedCategoryForDetail != null) {
            selectedCategoryForDetail = null
        } else {
            onNavigateBack()
        }
    }

    // Dialogs
    var showCategoryEditorDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    var showSkinEditorDialog by remember { mutableStateOf(false) }
    var skinToEdit by remember { mutableStateOf<PackageEntity?>(null) }
    var skinToDelete by remember { mutableStateOf<PackageEntity?>(null) }

    var showFolderDialog by remember { mutableStateOf(false) }

    val filteredCategories = remember(categories, selectedGameFilter) {
        when (selectedGameFilter) {
            "FREE_FIRE" -> categories.filter { it.gameMode == "FREE_FIRE" || it.gameMode == "BOTH" || it.gameMode.isBlank() }
            "FREE_FIRE_MAX" -> categories.filter { it.gameMode == "FREE_FIRE_MAX" || it.gameMode == "BOTH" || it.gameMode.isBlank() }
            else -> categories
        }
    }

    Scaffold(
        containerColor = CyberBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedCategoryForDetail != null) {
                            "CATEGORY: ${selectedCategoryForDetail!!.name.uppercase()}"
                        } else {
                            "CATEGORIES & GAME SELECTION"
                        },
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedCategoryForDetail != null) {
                                selectedCategoryForDetail = null
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonGreen)
                    }
                },
                actions = {
                    if (selectedCategoryForDetail != null) {
                        IconButton(
                            onClick = {
                                skinToEdit = null
                                showSkinEditorDialog = true
                            }
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Add Skin to Category", tint = NeonGreen)
                        }
                    } else if (selectedTab == 0) {
                        IconButton(
                            onClick = {
                                categoryToEdit = null
                                showCategoryEditorDialog = true
                            }
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Create Category", tint = NeonGreen)
                        }
                    } else {
                        IconButton(onClick = { showFolderDialog = true }) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Add Folder", tint = AccentAmber)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedCategoryForDetail == null) {
                // Tab Selection (CATEGORIES vs FOLDERS)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = CyberSurfaceElevated,
                    contentColor = NeonGreen
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("CATEGORIES (${categories.size})", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("FOLDERS (${folders.size})", fontWeight = FontWeight.Bold) }
                    )
                }

                if (selectedTab == 0) {
                    // Game Filter Row for Categories
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedGameFilter == "ALL",
                            onClick = { selectedGameFilter = "ALL" },
                            label = { Text("ALL (${categories.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonGreen, selectedLabelColor = Color(0xFF021B0B))
                        )
                        FilterChip(
                            selected = selectedGameFilter == "FREE_FIRE",
                            onClick = { selectedGameFilter = "FREE_FIRE" },
                            label = { Text("FREE FIRE", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonGreen, selectedLabelColor = Color(0xFF021B0B))
                        )
                        FilterChip(
                            selected = selectedGameFilter == "FREE_FIRE_MAX",
                            onClick = { selectedGameFilter = "FREE_FIRE_MAX" },
                            label = { Text("FREE FIRE MAX", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MintGlow, selectedLabelColor = Color(0xFF021B0B))
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (filteredCategories.isEmpty()) {
                            item {
                                GamingCard(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "No categories created for this game yet. Tap '+' at top right to create a new category.",
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            items(filteredCategories, key = { it.id }) { cat ->
                                val catSkinsCount = remember(allPackages, cat.id) {
                                    allPackages.count { it.categoryId == cat.id }
                                }

                                GamingCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedCategoryForDetail = cat }
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                val modeColor = if (cat.gameMode == "FREE_FIRE_MAX") MintGlow else NeonGreen
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(modeColor.copy(alpha = 0.15f))
                                                        .border(1.dp, modeColor, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (cat.gameMode == "FREE_FIRE_MAX") Icons.Default.FlashOn else Icons.Default.SportsEsports,
                                                        contentDescription = null,
                                                        tint = modeColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                Column {
                                                    Text(cat.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                    Text(
                                                        text = when (cat.gameMode) {
                                                            "FREE_FIRE_MAX" -> "FREE FIRE MAX • $catSkinsCount SKINS"
                                                            "BOTH" -> "FREE FIRE & MAX • $catSkinsCount SKINS"
                                                            else -> "FREE FIRE • $catSkinsCount SKINS"
                                                        },
                                                        color = modeColor,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Row {
                                                IconButton(
                                                    onClick = {
                                                        categoryToEdit = cat
                                                        showCategoryEditorDialog = true
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Category", tint = NeonGreen)
                                                }
                                                IconButton(onClick = { categoryToDelete = cat }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed)
                                                }
                                            }
                                        }

                                        if (cat.coverImageUrl.isNotBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(110.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(CyberSurfaceElevated)
                                            ) {
                                                SkinImage(
                                                    model = cat.coverImageUrl,
                                                    contentDescription = cat.name,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }

                                        if (cat.description.isNotBlank()) {
                                            Text(text = cat.description, color = TextMuted, fontSize = 12.sp)
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Tap card to manage skins inside", color = TextSecondary, fontSize = 11.sp)
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NeonGreen)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Folders Tab
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(folders) { folder ->
                            GamingCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = AccentAmber)
                                        Column {
                                            Text(folder.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(folder.description, color = TextMuted, fontSize = 11.sp)
                                        }
                                    }
                                    IconButton(onClick = { scope.launch { repository.deleteFolder(folder.id) } }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // CATEGORY SKINS VIEW (Admin opened a specific category)
                val currentCategory = selectedCategoryForDetail!!
                val skinsInCategory = remember(allPackages, currentCategory.id) {
                    allPackages.filter { it.categoryId == currentCategory.id }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        GamingCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "SKINS INSIDE '${currentCategory.name.uppercase()}'",
                                    color = NeonGreen,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Game Mode: ${currentCategory.gameMode} • Total Skins: ${skinsInCategory.size}",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )

                                Button(
                                    onClick = {
                                        skinToEdit = null
                                        showSkinEditorDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF021B0B)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ADD NEW SKIN IN THIS CATEGORY", fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }

                    if (skinsInCategory.isEmpty()) {
                        item {
                            GamingCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(Icons.Default.Inbox, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                                    Text("No skins inside this category yet.", color = TextSecondary, fontSize = 13.sp)
                                    Text("Tap button above to add a skin file to this category.", color = TextMuted, fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
                        items(skinsInCategory, key = { it.id }) { pkg ->
                            GamingCard(modifier = Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(pkg.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    skinToEdit = pkg
                                                    showSkinEditorDialog = true
                                                }
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit Skin", tint = NeonGreen)
                                            }
                                            IconButton(onClick = { skinToDelete = pkg }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete Skin", tint = AccentRed)
                                            }
                                        }
                                    }

                                    if (pkg.coverImageUrl.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(130.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(CyberSurfaceElevated)
                                        ) {
                                            SkinImage(
                                                model = pkg.coverImageUrl,
                                                contentDescription = pkg.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }

                                    if (pkg.description.isNotBlank()) {
                                        Text(pkg.description, color = TextSecondary, fontSize = 12.sp)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val ffText = if (pkg.ffFileName.isNotBlank()) "FF: ${pkg.ffFileName}" else ""
                                        val ffMaxText = if (pkg.ffMaxFileName.isNotBlank()) "MAX: ${pkg.ffMaxFileName}" else ""
                                        Text(
                                            text = listOf(ffText, ffMaxText).filter { it.isNotBlank() }.joinToString(" • "),
                                            color = MintGlow,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
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

    // Category Create/Edit Dialog
    if (showCategoryEditorDialog) {
        CategoryEditorDialog(
            initialCategory = categoryToEdit,
            repository = repository,
            onDismiss = { showCategoryEditorDialog = false },
            onSave = { category ->
                showCategoryEditorDialog = false
                scope.launch {
                    try {
                        repository.saveCategory(category)
                        snackbarHostState.showSnackbar("Category '${category.name}' saved and published!")
                    } catch (t: Throwable) {
                        Log.e("AdminCategoryFolderManager", "Save category error", t)
                        snackbarHostState.showSnackbar("Error saving category: ${t.localizedMessage}")
                    }
                }
            }
        )
    }

    // Category Delete Confirmation Dialog
    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            containerColor = CyberSurface,
            title = { Text("DELETE CATEGORY?", color = AccentRed, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${cat.name}'? Skins inside this category will remain available.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        val catId = cat.id
                        categoryToDelete = null
                        if (selectedCategoryForDetail?.id == catId) {
                            selectedCategoryForDetail = null
                        }
                        scope.launch {
                            repository.deleteCategory(catId)
                            snackbarHostState.showSnackbar("Category deleted!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("DELETE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }

    // Skin Editor Dialog for adding/editing skins inside Category
    if (showSkinEditorDialog && selectedCategoryForDetail != null) {
        PackageEditorDialog(
            initialPackage = skinToEdit,
            repository = repository,
            defaultCategoryId = selectedCategoryForDetail!!.id,
            onDismiss = {
                showSkinEditorDialog = false
                skinToEdit = null
            },
            onSave = { pkg ->
                val categoryName = selectedCategoryForDetail!!.name
                showSkinEditorDialog = false
                val pkgToSave = skinToEdit
                skinToEdit = null
                scope.launch {
                    try {
                        val linkedPkg = pkg.copy(categoryId = selectedCategoryForDetail!!.id)
                        repository.savePackage(linkedPkg)
                        snackbarHostState.showSnackbar("Skin '${pkg.name}' saved inside '$categoryName'!")
                    } catch (t: Throwable) {
                        Log.e("AdminCategoryFolderManager", "Save skin error", t)
                        snackbarHostState.showSnackbar("Error saving skin: ${t.localizedMessage}")
                    }
                }
            }
        )
    }

    // Skin Delete Confirmation
    skinToDelete?.let { pkg ->
        AlertDialog(
            onDismissRequest = { skinToDelete = null },
            containerColor = CyberSurface,
            title = { Text("DELETE SKIN?", color = AccentRed, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${pkg.name}'?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        val pkgId = pkg.id
                        skinToDelete = null
                        scope.launch {
                            repository.deletePackage(pkgId)
                            snackbarHostState.showSnackbar("Skin deleted!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("DELETE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { skinToDelete = null }) { Text("CANCEL", color = TextSecondary) }
            }
        )
    }

    // Create Folder Dialog
    if (showFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        var folderDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            containerColor = CyberSurface,
            title = { Text("NEW FOLDER", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = folderName, onValueChange = { folderName = it }, label = { Text("Folder Name") })
                    OutlinedTextField(value = folderDesc, onValueChange = { folderDesc = it }, label = { Text("Description") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            val fName = folderName.trim()
                            val fDesc = folderDesc.trim()
                            showFolderDialog = false
                            scope.launch {
                                repository.saveFolder(
                                    FolderEntity(
                                        id = "folder_${UUID.randomUUID().toString().take(6)}",
                                        name = fName,
                                        description = fDesc
                                    )
                                )
                                snackbarHostState.showSnackbar("Folder created!")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = CyberBackground)
                ) {
                    Text("SAVE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFolderDialog = false }) { Text("CANCEL", color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun CategoryEditorDialog(
    initialCategory: CategoryEntity?,
    repository: SkinToolRepository,
    onDismiss: () -> Unit,
    onSave: (CategoryEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val catId = remember { initialCategory?.id ?: "cat_${UUID.randomUUID().toString().take(8)}" }

    var name by remember { mutableStateOf(initialCategory?.name ?: "") }
    var description by remember { mutableStateOf(initialCategory?.description ?: "") }
    var gameMode by remember { mutableStateOf(initialCategory?.gameMode ?: "FREE_FIRE") } // "FREE_FIRE", "FREE_FIRE_MAX", "BOTH"
    var coverUrl by remember { mutableStateOf(initialCategory?.coverImageUrl ?: "") }

    var isUploadingImage by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    withContext(Dispatchers.Main) { isUploadingImage = true }
                    val contentResolver = context.contentResolver

                    // 1. Stream URI directly to a safe temporary file to avoid huge memory allocations
                    val catDir = File(context.filesDir, "categories/$catId")
                    catDir.mkdirs()
                    val tempRawFile = File(catDir, "temp_raw_image")
                    
                    var readSuccess = false
                    try {
                        contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(tempRawFile).use { output ->
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
                        Log.e("AdminCategoryFolderManager", "Failed to stream image URI", e)
                    }

                    if (!readSuccess) {
                        withContext(Dispatchers.Main) { isUploadingImage = false }
                        return@launch
                    }

                    // 2. Safely decode bounds to compute inSampleSize and prevent OutOfMemoryError
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
                        Log.e("AdminCategoryFolderManager", "Bitmap decode error", t)
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
                                android.graphics.Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                            } catch (t: Throwable) {
                                bitmap
                            }
                        } else {
                            bitmap
                        }

                        // Save permanent optimized HD cover on local disk
                        val localCover = File(catDir, "cover.jpg")
                        try {
                            FileOutputStream(localCover).use { out ->
                                scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                            }
                        } catch (e: Throwable) {
                            Log.e("AdminCategoryFolderManager", "Error saving local cover", e)
                        }

                        val localFileUri = "file://${localCover.absolutePath}"

                        // Convert to permanent Base64 JPEG Data URL for 100% reliable cross-device sync
                        val baos = ByteArrayOutputStream()
                        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, baos)
                        val b64Bytes = baos.toByteArray()
                        val b64Encoded = Base64.encodeToString(b64Bytes, Base64.NO_WRAP)
                        val permanentCoverUri = "data:image/jpeg;base64,$b64Encoded"

                        // Cache in memory immediately for 0ms lookup
                        SkinImageCache.put(permanentCoverUri, scaled)
                        SkinImageCache.put(localFileUri, scaled)
                        SkinImageCache.put("cat_$catId", scaled)

                        withContext(Dispatchers.Main) {
                            coverUrl = permanentCoverUri
                            isUploadingImage = false
                        }
                    } else {
                        withContext(Dispatchers.Main) { isUploadingImage = false }
                    }
                } catch (t: Throwable) {
                    Log.e("AdminCategoryFolderManager", "Image picking error", t)
                    withContext(Dispatchers.Main) { isUploadingImage = false }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Text(
                text = if (initialCategory == null) "CREATE NEW CATEGORY" else "EDIT CATEGORY",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Game Selection Row
                Text("SELECT GAME", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = gameMode == "FREE_FIRE",
                        onClick = { gameMode = "FREE_FIRE" },
                        label = { Text("FREE FIRE", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonGreen, selectedLabelColor = Color(0xFF021B0B))
                    )
                    FilterChip(
                        selected = gameMode == "FREE_FIRE_MAX",
                        onClick = { gameMode = "FREE_FIRE_MAX" },
                        label = { Text("FREE FIRE MAX", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MintGlow, selectedLabelColor = Color(0xFF021B0B))
                    )
                }

                // Category Cover Image Section (REQUIRED)
                Text("CATEGORY COVER IMAGE * (REQUIRED)", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurfaceElevated)
                        .border(1.5.dp, if (coverUrl.isNotBlank()) NeonGreen else CyberBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            try {
                                imagePicker.launch("image/*")
                            } catch (e: Throwable) {
                                Log.e("AdminCategoryFolderManager", "Cannot launch image picker", e)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (coverUrl.isNotBlank()) {
                        SkinImage(model = coverUrl, contentDescription = "Preview", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(32.dp))
                            Text(
                                text = if (isUploadingImage) "UPLOADING HD IMAGE..." else "TAP TO UPLOAD CATEGORY IMAGE",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Optional Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name (Optional)") },
                    placeholder = { Text("e.g. Weapon Skins, Costumes, Bundles") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Optional category details") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = coverUrl.isNotBlank() && !isUploadingImage,
                onClick = {
                    val finalName = name.trim().ifBlank {
                        if (gameMode == "FREE_FIRE_MAX") "FF MAX Category" else "Free Fire Category"
                    }
                    val cat = CategoryEntity(
                        id = catId,
                        name = finalName,
                        description = description.trim(),
                        coverImageUrl = coverUrl.trim(),
                        gameMode = gameMode,
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(cat)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color(0xFF021B0B))
            ) {
                Text("SAVE CATEGORY", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = TextSecondary) }
        }
    )
}
