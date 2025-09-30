package com.example.questflow.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.questflow.domain.usecase.LevelCurve

/**
 * @deprecated Replaced by AnimatedXpLevelBadge which provides XP animations and level-up effects.
 * This static version is no longer used in the app. Will be removed in future version.
 */
@Deprecated(
    message = "Use AnimatedXpLevelBadge instead for animated XP updates",
    replaceWith = ReplaceWith("AnimatedXpLevelBadge(level, xp, modifier = modifier, isCategory = isCategory)")
)
@Composable
fun XpLevelBadge(
    level: Int,
    xp: Long = 0,
    currentXp: Long = 0,
    modifier: Modifier = Modifier,
    isCategory: Boolean = false
) {
    val actualXp = if (xp > 0) xp else currentXp

    // Calculate XP thresholds for current and next level
    val currentLevelXp = if (isCategory) {
        (level * level * 100).toLong()
    } else {
        LevelCurve.requiredXp(level)
    }

    val nextLevelXp = if (isCategory) {
        ((level + 1) * (level + 1) * 100).toLong()
    } else {
        LevelCurve.requiredXp(level + 1)
    }

    // Calculate XP within current level (classic RPG style)
    val xpInCurrentLevel = actualXp - currentLevelXp
    val xpNeededForLevel = nextLevelXp - currentLevelXp

    // Progress within current level
    val progress = if (xpNeededForLevel > 0) {
        (xpInCurrentLevel.toFloat() / xpNeededForLevel.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

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
                    text = "Lvl $level",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "•",
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                )
                Text(
                    text = "${xpInCurrentLevel}/${xpNeededForLevel} XP",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}