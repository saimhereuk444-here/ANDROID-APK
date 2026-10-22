package com.example.ui.screens.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.R
import com.example.data.repository.SkinToolRepository
import com.example.ui.components.GamingButton
import com.example.ui.components.GamingCard
import com.example.ui.components.LuxuryGetPasswordButton
import com.example.ui.components.LoopingVideoBackground
import com.example.ui.components.rememberIsConnected
import com.example.ui.theme.*

@Composable
fun AuthScreen(
    repository: SkinToolRepository,
    onLoginSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val appSettings by repository.appSettings.collectAsState()
    var passwordInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showPasswordTutorialDialog by remember { mutableStateOf(false) }

    val handleLogin = {
        if (passwordInput.isBlank()) {
            errorMessage = "Please enter your access password."
        } else {
            isLoading = true
            errorMessage = null
            try {
                val role = repository.verifyPassword(passwordInput.trim())
                isLoading = false
                if (role != null) {
                    onLoginSuccess(role)
                } else {
                    errorMessage = "Incorrect or expired password! Key has been updated."
                    showPasswordTutorialDialog = true
                }
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.message ?: "Authentication temporarily restricted."
                showPasswordTutorialDialog = true
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Subtle cyber green grid overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val step = 36.dp.toPx()

            for (x in 0..width.toInt() step step.toInt()) {
                drawLine(
                    color = Color(0x0D00FF66),
                    start = Offset(x.toFloat(), 0f),
                    end = Offset(x.toFloat(), height),
                    strokeWidth = 1f
                )
            }
            for (y in 0..height.toInt() step step.toInt()) {
                drawLine(
                    color = Color(0x0D00FF66),
                    start = Offset(0f, y.toFloat()),
                    end = Offset(width, y.toFloat()),
                    strokeWidth = 1f
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Ultra-HD Logo with Triple Glowing Emerald Ring
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .shadow(24.dp, CircleShape, ambientColor = NeonGreen, spotColor = NeonGreen)
                    .clip(CircleShape)
                    .background(Color(0xFF092915))
                    .border(
                        2.5.dp,
                        Brush.sweepGradient(
                            listOf(
                                NeonGreen,
                                MintGlow,
                                EmeraldGreen,
                                NeonGreen
                            )
                        ),
                        CircleShape
                    )
                    .padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "SH Skin Tool Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            // High-Impact Brand Identity
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "SH SKIN TOOL",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(NeonGreen)
                    )
                    Text(
                        text = "BY SAIM TEAM",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonGreen,
                        letterSpacing = 2.5.sp
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(NeonGreen)
                    )
                }
            }

            // High-Security Auth Portal Card
            GamingCard(
                modifier = Modifier.fillMaxWidth(),
                borderGlow = true
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NeonGreen)
                    )
                    Text(
                        text = "AUTHENTICATION ACCESS",
                        color = NeonGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enter your VIP access password to unlock",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(18.dp))

                // ONLY Password input field - Clean, Green, and Focused
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = {
                        passwordInput = it
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input_field"),
                    label = { Text("Enter Password", fontWeight = FontWeight.SemiBold) },
                    placeholder = { Text("VIP Access Key / Password") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (passwordInput.isNotEmpty()) {
                            IconButton(onClick = {
                                passwordInput = ""
                                errorMessage = null
                            }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { handleLogin() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = Color(0xFF1E522C),
                        focusedContainerColor = Color(0xFF082613),
                        unfocusedContainerColor = Color(0xFF061F0F),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = NeonGreen,
                        focusedLabelColor = NeonGreen,
                        unfocusedLabelColor = TextSecondary
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Clean, elegant Error Display
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF280A0E))
                            .border(1.dp, AccentRed.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = AccentRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFFFB4AB),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Login Button
                GamingButton(
                    text = if (isLoading) "VERIFYING..." else "LOGIN",
                    onClick = handleLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_button"),
                    enabled = !isLoading,
                    icon = Icons.Default.Key
                )

                Spacer(modifier = Modifier.height(14.dp))

                // High-End Luxury "GET KEY / PASSWORD" Button
                LuxuryGetPasswordButton(
                    onClick = {
                        val url = appSettings.getPasswordUrl.ifBlank { "https://t.me/saimteam" }
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback if browser is not configured
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // High-Tech Green Footer
            Text(
                text = "Secure Shizuku Integration • Android 11 to 16 Supported",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }

        // Popup Dialog for Incorrect / Expired Password directing to YouTube tutorial video
        if (showPasswordTutorialDialog) {
            val wrongPassVideoUrl = appSettings.wrongPasswordVideoUrl.ifBlank { appSettings.getPasswordUrl }

            AlertDialog(
                onDismissRequest = { showPasswordTutorialDialog = false },
                containerColor = CyberSurface,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Video Tutorial",
                        tint = AccentRed,
                        modifier = Modifier.size(44.dp)
                    )
                },
                title = {
                    Text(
                        text = "INCORRECT PASSWORD",
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Clickable YouTube Video Thumbnail at TOP of popup
                        com.example.ui.components.YouTubeVideoThumbnailCard(
                            videoUrl = wrongPassVideoUrl
                        )

                        Text(
                            text = "The VIP password entered is incorrect or has been updated by Admin.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Text(
                            text = "Watch our video or click below to get the working password key!",
                            color = MintGlow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val url = appSettings.getPasswordUrl.ifBlank { wrongPassVideoUrl.ifBlank { "https://t.me/saimteam" } }
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback
                            }
                            showPasswordTutorialDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "WATCH VIDEO & GET NEW KEY",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showPasswordTutorialDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("TRY AGAIN", color = TextSecondary, fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
        
        // Connectivity Warning Popup
        val isConnected by rememberIsConnected()
        if (!isConnected) {
            AlertDialog(
                onDismissRequest = { /* Cannot dismiss */ },
                containerColor = CyberSurface,
                icon = {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "No Internet",
                        tint = AccentRed,
                        modifier = Modifier.size(44.dp)
                    )
                },
                title = {
                    Text(
                        text = "No Internet Connection",
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Text(
                        text = "No internet connection detected. Please turn on your internet connection to continue.",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {},
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
