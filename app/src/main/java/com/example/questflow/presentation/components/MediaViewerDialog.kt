package com.example.questflow.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.rememberAsyncImagePainter
import com.example.questflow.data.database.entity.MediaLibraryEntity
import com.example.questflow.data.database.entity.MediaType
import java.io.File

@Composable
fun MediaViewerDialog(
    media: MediaLibraryEntity,
    onDismiss: () -> Unit,
    onShowDetails: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            when (media.mediaType) {
                MediaType.IMAGE, MediaType.GIF -> {
                    ImageViewer(
                        media = media,
                        onDismiss = onDismiss,
                        onShowDetails = onShowDetails
                    )
                }
                MediaType.AUDIO -> {
                    AudioPlayer(
                        media = media,
                        onDismiss = onDismiss,
                        onShowDetails = onShowDetails
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageViewer(
    media: MediaLibraryEntity,
    onDismiss: () -> Unit,
    onShowDetails: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Image
        Image(
            painter = rememberAsyncImagePainter(model = File(media.filePath)),
            contentDescription = media.fileName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Top bar with close and info buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Schließen",
                    tint = Color.White
                )
            }

            Text(
                text = media.fileName,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )

            IconButton(onClick = onShowDetails) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Details",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun AudioPlayer(
    media: MediaLibraryEntity,
    onDismiss: () -> Unit,
    onShowDetails: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(File(media.filePath).toURI().toString())
            setMediaItem(mediaItem)
            prepare()
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)
            kotlinx.coroutines.delay(100)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Schließen")
                    }

                    IconButton(onClick = onShowDetails) {
                        Icon(Icons.Default.Info, contentDescription = "Details")
                    }
                }

                // Audio icon
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                // File name
                Text(
                    text = media.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Progress bar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Slider(
                        value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                        onValueChange = { value ->
                            exoPlayer.seekTo((value * duration).toLong())
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(currentPosition),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = formatDuration(duration),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Playback controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip backward
                    IconButton(onClick = {
                        exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0))
                    }) {
                        Icon(
                            Icons.Default.KeyboardArrowLeft,
                            contentDescription = "10s zurück",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Play/Pause
                    FloatingActionButton(
                        onClick = {
                            if (isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Clear else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Skip forward
                    IconButton(onClick = {
                        exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(duration))
                    }) {
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = "10s vor",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    if (millis < 0) return "0:00"
    val seconds = (millis / 1000) % 60
    val minutes = (millis / 1000) / 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
