package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.NeonGreen

/**
 * Extracts 11-character YouTube Video ID safely from any standard YouTube URL format:
 * - https://www.youtube.com/watch?v=VIDEO_ID
 * - https://youtu.be/VIDEO_ID
 * - https://www.youtube.com/embed/VIDEO_ID
 * - https://www.youtube.com/shorts/VIDEO_ID
 */
fun extractYouTubeVideoId(url: String): String? {
    if (url.isBlank()) return null
    val regex = "(?:youtube\\.com\\/(?:[^\\/]+\\/.+\\/|(?:v|e(?:mbed)?)\\/|.*[?&]v=)|youtu\\.be\\/|youtube\\.com\\/shorts\\/)([^\"&?\\/\\s]{11})".toRegex(RegexOption.IGNORE_CASE)
    val match = regex.find(url)
    val videoId = match?.groupValues?.getOrNull(1)
    return if (videoId != null && videoId.length == 11) videoId else null
}

/**
 * Interactive YouTube Video Thumbnail Card Component.
 * Displays high-quality YouTube thumbnail with play button overlay.
 * Clicking opens the YouTube video in browser / YouTube app.
 * If URL/ID is missing, hides section cleanly.
 */
@Composable
fun YouTubeVideoThumbnailCard(
    videoUrl: String,
    title: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val videoId = remember(videoUrl) { extractYouTubeVideoId(videoUrl) }

    if (videoId.isNullOrBlank()) return

    val thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                color = NeonGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CyberSurfaceElevated)
                .border(1.5.dp, NeonGreen.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                .clickable {
                    val targetUrl = if (videoUrl.startsWith("http")) videoUrl else "https://www.youtube.com/watch?v=$videoId"
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.w("YouTubeVideoCard", "Could not launch video URL: ${e.message}")
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = title ?: "YouTube Video",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Play Button Overlay
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(2.dp, NeonGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Video",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
