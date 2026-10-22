package com.example.ui.screens.user

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuStatus
import com.example.ui.components.GamingButton
import com.example.ui.components.GamingCard
import com.example.ui.theme.*

@Composable
fun UserShizukuGatekeeperScreen(
    shizukuManager: ShizukuManager,
    onShizukuGranted: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    BackHandler {
        activity?.moveTaskToBack(true)
    }

    val status by shizukuManager.status.collectAsState()

    LaunchedEffect(Unit) {
        shizukuManager.checkStatus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // App Logo
            Box(
                modifier = Modifier
                    .size(85.dp)
                    .clip(CircleShape)
                    .border(2.dp, Brush.linearGradient(listOf(NeonGreen, MintGlow)), CircleShape)
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "SH Skin Tool Logo",
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "SHIZUKU ACCESS GATE",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "REQUIRED AUTHORIZATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen,
                    letterSpacing = 1.5.sp
                )
            }

            GamingCard(
                modifier = Modifier.fillMaxWidth(),
                borderGlow = status == ShizukuStatus.CONNECTED
            ) {
                when (status) {
                    ShizukuStatus.CONNECTED -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(54.dp)
                            )
                            Text(
                                text = "✓ SHIZUKU GRANTED & CONNECTED",
                                color = NeonGreen,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Storage access is authorized. You can now explore and apply all skins seamlessly.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            GamingButton(
                                text = "CONTINUE TO SKINS",
                                onClick = onShizukuGranted,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("enter_user_panel_button"),
                                icon = Icons.Default.SportsEsports
                            )
                        }
                    }

                    ShizukuStatus.PERMISSION_REQUIRED, ShizukuStatus.RUNNING -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = AccentAmber,
                                modifier = Modifier.size(54.dp)
                            )
                            Text(
                                text = "SHIZUKU PERMISSION REQUIRED",
                                color = AccentAmber,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Shizuku is running on your phone, but permission has not been granted yet. Tap below to allow Shizuku access.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            GamingButton(
                                text = "ALLOW SHIZUKU PERMISSION",
                                onClick = { shizukuManager.requestPermission() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("allow_shizuku_button"),
                                icon = Icons.Default.Key
                            )
                            GamingButton(
                                text = "CHECK AGAIN",
                                onClick = { shizukuManager.checkStatus() },
                                modifier = Modifier.fillMaxWidth(),
                                isSecondary = true,
                                icon = Icons.Default.Refresh
                            )
                        }
                    }

                    ShizukuStatus.STOPPED -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AccentRed,
                                modifier = Modifier.size(54.dp)
                            )
                            Text(
                                text = "SHIZUKU IS NOT RUNNING",
                                color = AccentRed,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Shizuku app is installed on your device, but the Shizuku service is not active. Please start Shizuku via Wireless Debugging or ADB to proceed.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            GamingButton(
                                text = "OPEN SHIZUKU APP",
                                onClick = {
                                    val launchIntent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                    if (launchIntent != null) {
                                        context.startActivity(launchIntent)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Default.Launch
                            )
                            GamingButton(
                                text = "I HAVE STARTED IT (REFRESH)",
                                onClick = { shizukuManager.checkStatus() },
                                modifier = Modifier.fillMaxWidth(),
                                isSecondary = true,
                                icon = Icons.Default.Refresh
                            )
                        }
                    }

                    ShizukuStatus.NOT_INSTALLED -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DownloadForOffline,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(54.dp)
                            )
                            Text(
                                text = "SHIZUKU NOT INSTALLED",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Shizuku is required to apply skins safely on modern Android devices. Please install and connect Shizuku to proceed.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            GamingButton(
                                text = "GET SHIZUKU (DOWNLOAD)",
                                onClick = {
                                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try { context.startActivity(webIntent) } catch (e: Exception) {}
                                },
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Default.OpenInBrowser
                            )
                            GamingButton(
                                text = "CONTINUE",
                                onClick = onShizukuGranted,
                                modifier = Modifier.fillMaxWidth(),
                                isSecondary = true,
                                icon = Icons.Default.SportsEsports
                            )
                        }
                    }
                }
            }

            // Clean subtle return icon
            IconButton(
                onClick = onLogout,
                modifier = Modifier.testTag("gatekeeper_logout_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
