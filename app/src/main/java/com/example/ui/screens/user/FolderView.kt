package com.example.ui.screens.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.SkinToolRepository
import com.example.ui.components.GamingCard
import com.example.ui.components.SkinImage
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderView(
    folderId: String,
    repository: SkinToolRepository,
    onNavigateBack: () -> Unit,
    onNavigateToPackageDetails: (String) -> Unit
) {
    val folders by repository.getPublishedFolders().collectAsState(initial = emptyList())
    val currentFolder = remember(folders, folderId) { folders.find { it.id == folderId } }
    val packages by repository.getPackagesForFolder(folderId).collectAsState(initial = emptyList())

    Scaffold(
        containerColor = CyberBackground,
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = AccentAmber)
                        Text(
                            text = currentFolder?.name ?: "Folder",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                },
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
            item {
                Text(
                    text = currentFolder?.description ?: "Skins in this collection",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            if (packages.isEmpty()) {
                item {
                    GamingCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No skins inside this collection yet.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(packages) { pkg ->
                    GamingCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigateToPackageDetails(pkg.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(CyberSurfaceElevated)
                            ) {
                                SkinImage(
                                    model = pkg.coverImageUrl,
                                    contentDescription = pkg.name,
                                    packageId = pkg.id,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pkg.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("v${pkg.version}", color = NeonGreen, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NeonGreen)
                        }
                    }
                }
            }
        }
    }
}
