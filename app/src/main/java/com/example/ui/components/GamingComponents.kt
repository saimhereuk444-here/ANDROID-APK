package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.model.ContentStatus
import com.example.shizuku.ShizukuStatus
import com.example.ui.theme.*

/**
 * High-Definition Looping Video Background Player.
 * Renders the provided anime/cyberpunk video continuously in an infinite loop
 * with hardware acceleration and a sleek contrast overlay.
 */
@Composable
fun LoopingVideoBackground(
    modifier: Modifier = Modifier,
    rawResId: Int = R.raw.bg_video,
    overlayAlpha: Float = 0.55f,
    overlayColor: Color = CyberBackground
) {
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                VideoView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    val uri = Uri.parse("android.resource://${context.packageName}/$rawResId")
                    setVideoURI(uri)
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        mp.setVolume(0f, 0f)
                        mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                        start()
                    }
                    setOnCompletionListener {
                        start()
                    }
                    setOnErrorListener { _, _, _ ->
                        true // Suppress error popup and keep app intact
                    }
                }
            },
            onRelease = { videoView ->
                try {
                    videoView.stopPlayback()
                } catch (e: Throwable) {
                    // Ignored on teardown
                }
            }
        )

        // Luxury Cyberpunk Vignette & Glow Overlay for crystal clear UI readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            overlayColor.copy(alpha = overlayAlpha * 0.85f),
                            overlayColor.copy(alpha = overlayAlpha * 0.45f),
                            overlayColor.copy(alpha = overlayAlpha * 0.95f)
                        )
                    )
                )
        )
    }
}

/**
 * Ultra VIP Gaming Card with Cyberpunk Glassmorphic finish,
 * Neon glow outline and tactile elevation.
 */
@Composable
fun GamingCard(
    modifier: Modifier = Modifier,
    borderGlow: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    
    val glowAlpha = if (borderGlow) {
        val infiniteTransition = rememberInfiniteTransition(label = "card_glow")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow"
        )
        alpha
    } else {
        1.0f
    }

    val borderBrush = if (borderGlow) {
        Brush.horizontalGradient(
            listOf(
                NeonGreen.copy(alpha = glowAlpha),
                MintGlow.copy(alpha = 0.9f),
                NeonGreen.copy(alpha = glowAlpha)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                CyberBorder.copy(alpha = 0.9f),
                Color(0xFF0E2214)
            )
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (borderGlow) 12.dp else 5.dp,
                shape = shape,
                spotColor = if (borderGlow) NeonGreen else Color.Black,
                ambientColor = if (borderGlow) NeonGreen else Color.Black
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F2316),
                        Color(0xFF0A160E),
                        Color(0xFF060F09)
                    )
                )
            )
            .border(if (borderGlow) 1.5.dp else 1.dp, borderBrush, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(18.dp)
    ) {
        Column(content = content)
    }
}

/**
 * VIP Gamer Header with Rank Crest & Live Cloud Sync Pill
 */
@Composable
fun VipGamerHeader(
    syncState: String,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedGame: String? = null,
    gameIconUrl: String = "",
    showSupportActions: Boolean = false,
    unreadMailCount: Int = 0,
    onCustomerServiceClick: (() -> Unit)? = null,
    onMailboxClick: (() -> Unit)? = null,
    onSelectGame: ((String) -> Unit)? = null,
    onOpenGameSelector: (() -> Unit)? = null,
    onLogoutClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D2214),
                        CyberSurface,
                        Color(0xFF08120A)
                    )
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color(0xFF1E492B), CyberBorder)),
                RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Switch Button on Left Side for Toggling Between Free Fire and Free Fire MAX (Only when a game is active)
                if (selectedGame != null) {
                    IconButton(
                        onClick = {
                            val nextGame = if (selectedGame == "FREE_FIRE") "FREE_FIRE_MAX" else "FREE_FIRE"
                            onSelectGame?.invoke(nextGame)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberSurfaceElevated)
                            .border(
                                1.dp,
                                if (selectedGame == "FREE_FIRE_MAX") MintGlow.copy(alpha = 0.8f) else NeonGreen.copy(alpha = 0.8f),
                                RoundedCornerShape(10.dp)
                            )
                            .testTag("switch_game_category_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Switch Game Edition",
                            tint = if (selectedGame == "FREE_FIRE_MAX") MintGlow else NeonGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Game PNG Logo / VIP Crest Badge on Left
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    if (selectedGame == "FREE_FIRE_MAX") Color(0xFF0C3D32) else Color(0xFF1D5A30),
                                    Color(0xFF0C2413)
                                )
                            )
                        )
                        .border(
                            1.2.dp,
                            if (selectedGame == "FREE_FIRE_MAX") MintGlow else NeonGreen,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (gameIconUrl.isNotBlank()) {
                        SkinImage(
                            model = gameIconUrl,
                            contentDescription = "Game Logo",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = if (selectedGame == "FREE_FIRE_MAX") Icons.Default.FlashOn else if (selectedGame == "FREE_FIRE") Icons.Default.SportsEsports else Icons.Default.WorkspacePremium,
                            contentDescription = "Game Icon",
                            tint = if (selectedGame == "FREE_FIRE_MAX") MintGlow else NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (selectedGame == "FREE_FIRE_MAX") "FREE FIRE MAX" else if (selectedGame == "FREE_FIRE") "FREE FIRE" else "SH SKIN TOOL",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            letterSpacing = 1.2.sp
                        )
                        if (selectedGame != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (selectedGame == "FREE_FIRE_MAX") MintGlow else NeonGreen)
                                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    text = if (selectedGame == "FREE_FIRE_MAX") "MAX" else "STD",
                                    color = Color(0xFF02170A),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                    Text(
                        text = if (selectedGame == "FREE_FIRE_MAX") "FREE FIRE MAX SKINS" else if (selectedGame == "FREE_FIRE") "FREE FIRE SKINS" else "SAIM TEAM • OFFICIAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedGame == "FREE_FIRE_MAX") MintGlow else NeonGreen,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showSupportActions) {
                    if (onCustomerServiceClick != null) {
                        IconButton(
                            onClick = onCustomerServiceClick,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberSurfaceElevated)
                                .border(1.dp, NeonGreen.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                .testTag("topbar_customer_service_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HeadsetMic,
                                contentDescription = "Customer Service",
                                tint = NeonGreen,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    if (onMailboxClick != null) {
                        BadgedBox(
                            badge = {
                                if (unreadMailCount > 0) {
                                    Badge(
                                        containerColor = AccentRed,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = if (unreadMailCount > 9) "9+" else unreadMailCount.toString(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        ) {
                            IconButton(
                                onClick = onMailboxClick,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberSurfaceElevated)
                                    .border(1.dp, if (unreadMailCount > 0) AccentRed else MintGlow.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .testTag("topbar_mailbox_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mail,
                                    contentDescription = "User Mailbox",
                                    tint = if (unreadMailCount > 0) NeonGreen else MintGlow,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }

                SyncStatusPill(syncState = syncState)
                if (onLogoutClick != null) {
                    GamingLogoutButton(
                        onClick = onLogoutClick,
                        testTag = "user_logout_button"
                    )
                }
            }
        }
    }
}

/**
 * Gaming Logout Action Button
 */
@Composable
fun GamingLogoutButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "logout_button"
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CyberSurfaceElevated)
            .border(1.dp, AccentRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .testTag(testTag)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = "Logout",
            tint = AccentRed,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Cyberpunk styled Logout Confirmation Dialog
 */
@Composable
fun GamingLogoutDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = CyberSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = AccentRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "LOGOUT",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp,
                        color = TextPrimary
                    )
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to log out of your session?",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_logout_dialog_button")
                ) {
                    Text("LOGOUT", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    ),
                    border = BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("CANCEL", fontSize = 13.sp)
                }
            }
        )
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (color, bgColor, text) = when (status) {
        ContentStatus.PUBLISHED, "SUCCESS" -> Triple(NeonGreen, Color(0xFF072B15), if (status == "SUCCESS") "SUCCESS" else "ACTIVE")
        ContentStatus.DRAFT -> Triple(AccentAmber, Color(0xFF2E2207), "PREVIEW")
        ContentStatus.DISABLED, "FAILED" -> Triple(AccentRed, Color(0xFF380E0E), if (status == "FAILED") "FAILED" else "UNAVAILABLE")
        "CANCELLED" -> Triple(TextMuted, Color(0xFF14241A), "CANCELLED")
        else -> Triple(NeonGreen, Color(0xFF072B15), status)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
fun ShizukuStatusPill(
    status: ShizukuStatus,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val (text, icon, color) = when (status) {
        ShizukuStatus.CONNECTED -> Triple("✓ SHIZUKU CONNECTED & ACTIVE", Icons.Default.CheckCircle, NeonGreen)
        ShizukuStatus.PERMISSION_REQUIRED -> Triple("TAP TO GRANT SHIZUKU PERMISSION", Icons.Default.Warning, AccentAmber)
        ShizukuStatus.RUNNING -> Triple("SHIZUKU RUNNING • AUTH REQUIRED", Icons.Default.Shield, MintGlow)
        ShizukuStatus.STOPPED -> Triple("SHIZUKU STOPPED • PLEASE START SERVICE", Icons.Default.Warning, AccentRed)
        ShizukuStatus.NOT_INSTALLED -> Triple("SHIZUKU NOT FOUND ON DEVICE", Icons.Default.Warning, TextMuted)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF0D2214),
                        Color(0xFF08170D)
                    )
                )
            )
            .border(1.2.dp, color.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun SyncStatusPill(
    syncState: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0B1B10))
            .border(1.dp, NeonGreen.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(NeonGreen)
        )
        Text(
            text = "ONLINE",
            color = NeonGreen,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun SimpleProgressBar(
    progressPercent: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent / 100f,
        label = "progress"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$progressPercent%",
                color = NeonGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }
        LinearProgressIndicator(
            progress = { animatedProgress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = NeonGreen,
            trackColor = Color(0xFF0F2618)
        )
    }
}

/**
 * Ultra VIP Pro Gaming Button with high-energy Neon Glow,
 * Bold Typography and tactile interaction.
 */
@Composable
fun GamingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "gaming_button",
    enabled: Boolean = true,
    isSecondary: Boolean = false,
    icon: ImageVector? = null
) {
    val shape = RoundedCornerShape(14.dp)
    
    val backgroundBrush = if (!enabled) {
        Brush.horizontalGradient(listOf(Color(0xFF14241A), Color(0xFF14241A)))
    } else if (isSecondary) {
        Brush.horizontalGradient(listOf(Color(0xFF0D2215), Color(0xFF08160E)))
    } else {
        Brush.horizontalGradient(
            listOf(
                Color(0xFF00FF66),
                Color(0xFF10B981),
                Color(0xFF00FF66)
            )
        )
    }

    val borderBrush = if (!enabled) {
        Brush.horizontalGradient(listOf(Color(0xFF1C2E22), Color(0xFF1C2E22)))
    } else if (isSecondary) {
        Brush.horizontalGradient(listOf(NeonGreen, MintGlow))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFFB4FFCE), Color(0xFF00FF66)))
    }

    val contentColor = if (!enabled) {
        TextMuted
    } else if (isSecondary) {
        NeonGreen
    } else {
        Color(0xFF021B0B)
    }

    Box(
        modifier = modifier
            .testTag(testTag)
            .height(54.dp)
            .shadow(
                elevation = if (enabled && !isSecondary) 10.dp else 2.dp,
                shape = shape,
                spotColor = if (enabled && !isSecondary) NeonGreen else Color.Black,
                ambientColor = if (enabled && !isSecondary) NeonGreen else Color.Black
            )
            .clip(shape)
            .background(backgroundBrush)
            .border(if (isSecondary) 1.5.dp else 1.2.dp, borderBrush, shape)
            .clickable(
                enabled = enabled,
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = if (isSecondary) NeonGreen else Color.White)
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = text,
                color = contentColor,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                letterSpacing = 1.2.sp
            )
        }
    }
}

/**
 * Luxury VIP Access Key button
 */
@Composable
fun LuxuryGetPasswordButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "get_password_button"
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .testTag(testTag)
            .height(54.dp)
            .shadow(8.dp, shape, ambientColor = NeonGreen, spotColor = NeonGreen)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF0E2918),
                        Color(0xFF081A0F),
                        Color(0xFF0E2918)
                    )
                )
            )
            .border(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF00FF66),
                        Color(0xFF34D399),
                        Color(0xFF00FF66)
                    )
                ),
                shape
            )
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = NeonGreen)
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF144023))
                        .border(1.2.dp, NeonGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Key Icon",
                        tint = NeonGreen,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = "GET ACCESS KEY",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Tap to obtain password instantly",
                        color = MintGlow,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open Link",
                tint = NeonGreen,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
