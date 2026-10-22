package com.example.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.SkinToolRepository
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    repository: SkinToolRepository? = null,
    onSplashComplete: () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0.1f) }
    var statusText by remember { mutableStateOf("INITIALIZING SECURE ENGINE...") }

    // Infinite rotation for outer cyber ring
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulse animation for crest
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        if (repository != null) {
            repository.preloadAllAppAssets { currentProgress, text ->
                progress = currentProgress
                statusText = text
            }
            delay(400)
            onSplashComplete()
        } else {
            // Step 1
            progress = 0.25f
            statusText = "CONNECTING CYBER STORAGE..."
            delay(400)

            // Step 2
            progress = 0.65f
            statusText = "PRELOADING HD ASSETS & PNGs..."
            delay(400)

            // Step 3
            progress = 1.0f
            statusText = "SYSTEM READY • WELCOME"
            delay(300)

            onSplashComplete()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Cyber Grid Background Lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val step = 40.dp.toPx()

            for (x in 0..width.toInt() step step.toInt()) {
                drawLine(
                    color = Color(0x0A00FF66),
                    start = Offset(x.toFloat(), 0f),
                    end = Offset(x.toFloat(), height),
                    strokeWidth = 1f
                )
            }
            for (y in 0..height.toInt() step step.toInt()) {
                drawLine(
                    color = Color(0x0A00FF66),
                    start = Offset(0f, y.toFloat()),
                    end = Offset(width, y.toFloat()),
                    strokeWidth = 1f
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Glowing Animated Logo Area
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(170.dp)
                    .scale(pulseScale)
            ) {
                // Outer Cyber Tech Ring
                Canvas(
                    modifier = Modifier
                        .size(170.dp)
                        .rotate(rotation)
                ) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(
                                NeonGreen,
                                Color.Transparent,
                                MintGlow,
                                Color.Transparent,
                                NeonGreen
                            )
                        ),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Inner Glow Background Circle
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .shadow(24.dp, CircleShape, spotColor = NeonGreen, ambientColor = NeonGreen)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF164024),
                                    Color(0xFF08180E),
                                    CyberBackground
                                )
                            )
                        )
                        .border(1.5.dp, CyberBorderActive, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = "SH Skin Tool Logo",
                        modifier = Modifier
                            .size(95.dp)
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Brand Typography
            Text(
                text = "SH SKIN TOOL",
                color = TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "SAIM TEAM OFFICIAL",
                color = NeonGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Pro Style Cyber Loading Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF0B1F12))
                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
            ) {
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(400, easing = LinearOutSlowInEasing),
                    label = "bar"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF059669),
                                    NeonGreen,
                                    MintGlow
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Status Loading Text
            Text(
                text = statusText,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Bottom Watermark
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "ULTRA HD DUAL STORAGE • ALL DEVICES READY",
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }
    }
}
