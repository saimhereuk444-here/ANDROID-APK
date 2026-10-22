package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.SkinToolRepository
import com.example.ui.components.GamingButton
import com.example.ui.components.GamingCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSecuritySettingsScreen(
    repository: SkinToolRepository,
    onNavigateBack: () -> Unit
) {
    val appSettings by repository.appSettings.collectAsState()
    var userPassInput by remember { mutableStateOf(appSettings.userPassword) }
    var adminPassInput by remember { mutableStateOf(appSettings.adminPassword) }
    var getPassUrlInput by remember { mutableStateOf(appSettings.getPasswordUrl) }
    var wrongPassVideoUrlInput by remember { mutableStateOf(appSettings.wrongPasswordVideoUrl) }
    var shizukuSetupVideoUrlInput by remember { mutableStateOf(appSettings.shizukuSetupVideoUrl) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(appSettings) {
        userPassInput = appSettings.userPassword
        adminPassInput = appSettings.adminPassword
        getPassUrlInput = appSettings.getPasswordUrl
        wrongPassVideoUrlInput = appSettings.wrongPasswordVideoUrl
        shizukuSetupVideoUrlInput = appSettings.shizukuSetupVideoUrl
    }

    Scaffold(
        containerColor = CyberBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("PASSWORDS & YOUTUBE SETTINGS", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
                    text = "Update access passwords, GET KEY link, and YouTube tutorial videos. Changing user password immediately forces logout on all connected user devices.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }

            // User Password Configuration
            item {
                GamingCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = NeonGreen)
                            Text("USER PANEL ACCESS PASSWORD", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(
                            text = "Users must enter this password on login screen to open User Panel. Changing this forces logout for all logged-in users.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        OutlinedTextField(
                            value = userPassInput,
                            onValueChange = { userPassInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_user_pass_input"),
                            label = { Text("User Password") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )
                    }
                }
            }

            // Admin Password Configuration
            item {
                GamingCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldGreen)
                            Text("ADMIN PANEL MASTER PASSWORD", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(
                            text = "Master key that grants full access to Admin Command Center.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        OutlinedTextField(
                            value = adminPassInput,
                            onValueChange = { adminPassInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_master_pass_input"),
                            label = { Text("Admin Master Password") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )
                    }
                }
            }

            // Get Password URL Configuration
            item {
                GamingCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = MintGlow)
                            Text("GET PASSWORD BUTTON LINK (URL)", color = MintGlow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(
                            text = "The web address opened when users tap 'GET PASSWORD' on the login screen.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        OutlinedTextField(
                            value = getPassUrlInput,
                            onValueChange = { getPassUrlInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_get_pass_url_input"),
                            label = { Text("Destination URL") },
                            placeholder = { Text("https://t.me/saimteam") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MintGlow,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )
                    }
                }
            }

            // Wrong Password Popup YouTube Video
            item {
                GamingCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = AccentRed)
                            Text("WRONG PASSWORD POPUP YOUTUBE VIDEO", color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(
                            text = "YouTube video shown at the top of the wrong-password popup when user enters incorrect password.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        OutlinedTextField(
                            value = wrongPassVideoUrlInput,
                            onValueChange = { wrongPassVideoUrlInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_wrong_pass_video_input"),
                            label = { Text("YouTube Video URL") },
                            placeholder = { Text("https://www.youtube.com/watch?v=...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentRed,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )
                        if (wrongPassVideoUrlInput.isNotBlank()) {
                            com.example.ui.components.YouTubeVideoThumbnailCard(
                                videoUrl = wrongPassVideoUrlInput,
                                title = "PREVIEW WRONG PASS VIDEO"
                            )
                        }
                    }
                }
            }

            // Shizuku Setup YouTube Video
            item {
                GamingCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = NeonGreen)
                            Text("SHIZUKU SETUP YOUTUBE VIDEO", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(
                            text = "YouTube tutorial video displayed on the 'SHIZUKU NOT CONNECTED' screen.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        OutlinedTextField(
                            value = shizukuSetupVideoUrlInput,
                            onValueChange = { shizukuSetupVideoUrlInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_shizuku_setup_video_input"),
                            label = { Text("Shizuku Setup Video URL") },
                            placeholder = { Text("https://www.youtube.com/watch?v=...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )
                        if (shizukuSetupVideoUrlInput.isNotBlank()) {
                            com.example.ui.components.YouTubeVideoThumbnailCard(
                                videoUrl = shizukuSetupVideoUrlInput,
                                title = "PREVIEW SHIZUKU SETUP VIDEO"
                            )
                        }
                    }
                }
            }

            // Save Changes Action
            item {
                GamingButton(
                    text = if (isSaving) "SAVING CHANGES..." else "SAVE ALL SETTINGS & PUBLISH",
                    onClick = {
                        isSaving = true
                        scope.launch {
                            repository.updateSecuritySettings(
                                newUserPass = userPassInput,
                                newAdminPass = adminPassInput,
                                newGetPassUrl = getPassUrlInput,
                                newWrongPasswordVideoUrl = wrongPassVideoUrlInput,
                                newShizukuSetupVideoUrl = shizukuSetupVideoUrlInput
                            )
                            isSaving = false
                            snackbarHostState.showSnackbar("✓ Security settings updated! Old passwords invalidated across all devices.")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_security_settings_button"),
                    enabled = !isSaving,
                    icon = Icons.Default.Check
                )
            }
        }
    }
}
