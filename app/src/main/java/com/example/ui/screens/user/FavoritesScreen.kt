package com.example.ui.screens.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.SkinToolRepository
import com.example.ui.components.GamingCard
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    repository: SkinToolRepository,
    onNavigateBack: () -> Unit,
    onNavigateToPackageDetails: (String) -> Unit
) {
    val currentUser by repository.currentUser.collectAsState()
    val favorites by repository.getFavoritesForUser(currentUser?.userId ?: "default_user").collectAsState(initial = emptyList())
    val allPackages by repository.getPublishedPackages().collectAsState(initial = emptyList())

    val favPackages = remember(favorites, allPackages) {
        val favIds = favorites.map { it.packageId }.toSet()
        allPackages.filter { it.id in favIds }
    }

    Scaffold(
        containerColor = CyberBackground,
        topBar = {
            TopAppBar(
                title = { Text("FAVORITE SKINS", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (favPackages.isEmpty()) {
                item {
                    GamingCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = TextMuted, modifier = Modifier.size(44.dp))
                            Text("No favorites saved yet", color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text("Tap the heart icon on any skin to save it here for fast access.", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(favPackages) { pkg ->
                    GamingCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigateToPackageDetails(pkg.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pkg.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(pkg.description, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                                Text("Version ${pkg.version}", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NeonGreen)
                        }
                    }
                }
            }
        }
    }
}
