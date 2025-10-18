package com.example.questflow.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.questflow.domain.usecase.LevelCurve
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AnimatedXpLevelBadge(
    level: Int,
    xp: Long,
    previousXp: Long = xp,
    modifier: Modifier = Modifier,
    isCategory: Boolean = false,
    onLevelUp: ((Int) -> Unit)? = null,
    categoryId: Long? = null // Add category ID to track changes
) {
    // Track if this is the first composition or an actual category switch
    var previousCategoryId by remember { mutableStateOf<Long?>(categoryId) }
    var isInitialComposition by remember { mutableStateOf(true) }

    // Only consider it a category switch if it's not the initial composition
    val isCategorySwitch = !isInitialComposition && previousCategoryId != categoryId

    // Reset state when category changes
    var displayLevel by remember(level, categoryId) { mutableStateOf(level) }
    var displayXp by remember(xp, categoryId) { mutableStateOf(xp) }
    var animatingXp by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Update tracking values
    LaunchedEffect(categoryId) {
        if (isCategorySwitch) {
            displayXp = xp
            displayLevel = level
            animatingXp = false
        }
        previousCategoryId = categoryId
        isInitialComposition = false
    }

    // Track actual level for display
    val actualLevel = if (isCategorySwitch) level else displayLevel

    // Animate XP value (skip animation on category switch)
    val animatedXp by animateFloatAsState(
        targetValue = if (animatingXp && !isCategorySwitch) xp.toFloat() else displayXp.toFloat(),
        animationSpec = if (isCategorySwitch) {
            snap() // Instant change on category switch
        } else {
            tween(
                durationMillis = 1500,
                easing = FastOutSlowInEasing
            )
        },
        finishedListener = {
            displayXp = xp
            animatingXp = false
        }
    )

    // Calculate progress using actual level (not displayLevel for accuracy)
    val nextLevelXp = if (isCategory) {
        ((actualLevel + 1) * (actualLevel + 1) * 100).toLong()
    } else {
        LevelCurve.requiredXp(actualLevel + 1)
    }

    val currentLevelXp = if (isCategory) {
        // Category XP: Level 1 starts at 0, Level 2 at 100, Level 3 at 400, etc.
        // Formula: (level-1)² × 100
        // Level 1: (1-1)² × 100 = 0
        // Level 2: (2-1)² × 100 = 100
        // Level 3: (3-1)² × 100 = 400
        ((actualLevel - 1) * (actualLevel - 1) * 100).toLong()
    } else {
        LevelCurve.requiredXp(actualLevel)
    }

    // Use actual XP for progress calculation on category switch
    val xpForProgress = if (isCategorySwitch) xp else animatedXp.toLong()

    // Calculate XP within current level (classic RPG style: each level starts at 0)
    val xpInCurrentLevel = xpForProgress - currentLevelXp
    val xpNeededForLevel = nextLevelXp - currentLevelXp

    // Progress bar shows progress within current level (0% to 100% per level)
    val rawProgress = if (xpNeededForLevel > 0) {
        (xpInCurrentLevel.toFloat() / xpNeededForLevel.toFloat())
    } else {
        0f
    }

    // Debug logging
    if (isCategory) {
        android.util.Log.d("QuestFlow_CategoryUI", "=== RENDERING CATEGORY XP ===")
        android.util.Log.d("QuestFlow_CategoryUI", "Category ID: $categoryId")
        android.util.Log.d("QuestFlow_CategoryUI", "Actual Level: $actualLevel")
        android.util.Log.d("QuestFlow_CategoryUI", "Total XP: $xpForProgress")
        android.util.Log.d("QuestFlow_CategoryUI", "Current Level Base XP: $currentLevelXp (formula: ($actualLevel-1)² × 100)")
        android.util.Log.d("QuestFlow_CategoryUI", "Next Level XP: $nextLevelXp (formula: ($actualLevel+1)² × 100)")
        android.util.Log.d("QuestFlow_CategoryUI", "XP in Current Level: $xpInCurrentLevel")
        android.util.Log.d("QuestFlow_CategoryUI", "XP Needed for Next Level: $xpNeededForLevel")
        android.util.Log.d("QuestFlow_CategoryUI", "Displaying Range: $xpInCurrentLevel/$xpNeededForLevel XP")
        android.util.Log.d("QuestFlow_CategoryUI", "Progress: ${rawProgress * 100}%")
    }

    // Animate progress bar (skip animation on category switch)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress.coerceIn(0f, 1f),
        animationSpec = if (isCategorySwitch) {
            snap() // Instant change on category switch
        } else {
            tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            )
        }
    )

    // Debug animated progress
    if (isCategory) {
        android.util.Log.d("QuestFlow_CategoryUI", "Animated Progress: ${animatedProgress * 100}%")
        android.util.Log.d("QuestFlow_CategoryUI", "Target Progress: ${rawProgress * 100}%")
        android.util.Log.d("QuestFlow_CategoryUI", "Is Category Switch: $isCategorySwitch")
    }

    // Check for level up during animation
    LaunchedEffect(animatedXp) {
        if (animatingXp && animatedXp >= nextLevelXp && displayLevel < level) {
            // Delay to show full bar before level up
            delay(300)
            displayLevel++
            onLevelUp?.invoke(displayLevel)
        }
    }

    // Trigger animation when XP changes (but not on category switch)
    LaunchedEffect(xp, categoryId) {
        if (xp != displayXp && xp > displayXp && !animatingXp) {
            // Only animate if it's an actual XP gain, not a category switch
            animatingXp = true
        } else if (xp != displayXp && (xp < displayXp || categoryId != null)) {
            // Instant update on category switch or XP decrease
            displayXp = xp
            displayLevel = level
        }
    }

    // Pulse effect on level up
    val pulseScale by animateFloatAsState(
        targetValue = if (displayLevel < level) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Lvl $actualLevel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "•",
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                )
                Text(
                    text = "${xpInCurrentLevel}/$xpNeededForLevel XP",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            ) {
                // Background track
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                // Animated progress bar
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (rawProgress >= 0.95f) {
                                // Glow effect when near level up
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                )

                // Shimmer effect during animation
                if (animatingXp) {
                    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
                    val shimmerAlpha by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 0.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "shimmer_alpha"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = shimmerAlpha))
                    )
                }
            }
        }
    }
}