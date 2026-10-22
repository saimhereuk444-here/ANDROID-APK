package com.example.ui.screens.user

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.SkinToolRepository
import com.example.ui.components.GamingCard
import com.example.ui.components.SkinImage
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicScreenView(
    screenId: String,
    repository: SkinToolRepository,
    onNavigateBack: () -> Unit,
    onNavigateToPackageDetails: (String) -> Unit
) {
    val screens by repository.getPublishedScreens().collectAsState(initial = emptyList())
    val screen = remember(screens, screenId) { screens.find { it.id == screenId } }
    val packages by repository.getPackagesForScreen(screenId).collectAsState(initial = emptyList())

    Scaffold(
        containerColor = CyberBackground,
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(
                        text = screen?.title ?: "COLLECTION",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeonGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberSurface
                )
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
            // Screen Hero Header
            if (screen != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CyberSurfaceElevated)
                            .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
                    ) {
                        SkinImage(
                            model = screen.coverImageUrl,
                            contentDescription = screen.title,
                            packageId = screen.id,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color(0x99080B11), Color(0xF5080B11))
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = screen.title,
                                color = TextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (screen.description.isNotBlank()) {
                                Text(
                                    text = screen.description,
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "AVAILABLE SKINS (${packages.size})",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            if (packages.isEmpty()) {
                item {
                    GamingCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No skins available in this collection yet. Check back soon for updates.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(packages) { pkg ->
                    GamingCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dynamic_package_item_${pkg.id}"),
                        borderGlow = pkg.isFeatured,
                        onClick = { onNavigateToPackageDetails(pkg.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
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

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = pkg.name,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = pkg.description,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "v${pkg.version}", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "•", color = TextMuted, fontSize = 11.sp)
                                    val sizeMb = String.format("%.1f MB", pkg.fileSize / (1024f * 1024f))
                                    Text(text = sizeMb, color = TextMuted, fontSize = 11.sp)
                                }
                            }

                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Open",
                                tint = NeonGreen
                            )
                        }
                    }
                }
            }
        }
    }
}
