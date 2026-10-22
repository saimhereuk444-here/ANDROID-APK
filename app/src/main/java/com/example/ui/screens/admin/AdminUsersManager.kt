package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.data.repository.SkinToolRepository
import com.example.ui.components.GamingCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersManager(
    repository: SkinToolRepository,
    onNavigateBack: () -> Unit
) {
    val users by repository.getAllUsers().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = CyberBackground,
        topBar = {
            TopAppBar(
                title = { Text("USER DIRECTORY & ACCESS", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
            item {
                Text(
                    text = "Manage registered gamer accounts and authorization statuses without exposing personal private data.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            if (users.isEmpty()) {
                item {
                    GamingCard(modifier = Modifier.fillMaxWidth()) {
                        Text("No registered users found.", color = TextSecondary, modifier = Modifier.padding(16.dp))
                    }
                }
            } else {
                items(users) { user ->
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
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(36.dp))
                                Column {
                                    Text(user.displayName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("UID: ${user.userId} • Role: ${user.role}", color = TextMuted, fontSize = 11.sp)
                                    val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(user.createdAt))
                                    Text("Joined: $dateStr", color = TextSecondary, fontSize = 11.sp)
                                }
                            }

                            Switch(
                                checked = user.isActive,
                                onCheckedChange = { active ->
                                    scope.launch {
                                        repository.setCurrentUser(user.copy(isActive = active))
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
                            )
                        }
                    }
                }
            }
        }
    }
}
