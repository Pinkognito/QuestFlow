package com.example.questflow.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun XpClaimAnimation(
    xpAmount: Int,
    onAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }

    // Animation values
    val infiniteTransition = rememberInfiniteTransition(label = "xp_animation")

    // Scale animation: starts small, scales up, then down
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1.2f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    // Fade animation
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isVisible) 300 else 800,
            easing = if (isVisible) FastOutSlowInEasing else LinearEasing
        ),
        label = "alpha"
    )

    // Y offset animation - float upward
    val offsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else -100f,
        animationSpec = tween(
            durationMillis = 1500,
            easing = FastOutSlowInEasing
        ),
        label = "offsetY"
    )

    // Trigger hide after delay
    LaunchedEffect(key1 = true) {
        delay(300) // Initial delay for impact
        isVisible = true
        delay(1200) // Show duration
        isVisible = false
        delay(800) // Wait for fade out
        onAnimationEnd()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset(y = offsetY.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
        ) {
            // Main XP text
            Text(
                text = "+$xpAmount",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black
                ),
                color = MaterialTheme.colorScheme.primary
            )

            // XP label
            Text(
                text = "XP",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun XpBurstAnimation(
    xpAmount: Int,
    leveledUp: Boolean = false,
    newLevel: Int = 0,
    unlockedMemes: List<String> = emptyList(),
    onAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    var showLevelUp by remember { mutableStateOf(false) }

    // Main XP animation
    val scale by animateFloatAsState(
        targetValue = when {
            !isVisible -> 0f
            showLevelUp -> 1.5f
            else -> 1.3f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isVisible) 200 else 600,
            easing = FastOutSlowInEasing
        ),
        label = "alpha"
    )

    val rotation by animateFloatAsState(
        targetValue = if (showLevelUp) 360f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing
        ),
        label = "rotation"
    )

    LaunchedEffect(key1 = true) {
        delay(100)
        isVisible = true

        if (leveledUp) {
            delay(800)
            showLevelUp = true
            delay(1500)
        } else {
            delay(1200)
        }

        isVisible = false
        delay(600)
        onAnimationEnd()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
                .graphicsLayer(
                    rotationZ = if (leveledUp) rotation else 0f
                )
        ) {
            // XP Amount
            Text(
                text = "+$xpAmount",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Black
                ),
                color = if (leveledUp) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )

            Text(
                text = "XP ERHALTEN!",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )

            // Level up text if applicable
            if (showLevelUp && leveledUp) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "LEVEL UP!",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "Level $newLevel",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.tertiary
                )

                // Show unlocked memes
                if (unlockedMemes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Neue Memes freigeschaltet!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    unlockedMemes.forEach { memeName ->
                        Text(
                            text = "🎉 $memeName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}