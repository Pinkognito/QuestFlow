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

@Composable
fun XpLevelBadge(
    level: Int,
    xp: Long = 0,
    currentXp: Long = 0,
    modifier: Modifier = Modifier,
    isCategory: Boolean = false
) {
    val actualXp = if (xp > 0) xp else currentXp
    val nextLevelXp = if (isCategory) {
        ((level + 1) * (level + 1) * 100).toLong()
    } else {
        LevelCurve.requiredXp(level + 1)
    }
    val progress = if (isCategory) {
        val currentLevelXp = (level * level * 100).toLong()
        val xpInCurrentLevel = actualXp - currentLevelXp
        val xpNeededForLevel = nextLevelXp - currentLevelXp
        (xpInCurrentLevel.toFloat() / xpNeededForLevel.toFloat()).coerceIn(0f, 1f)
    } else {
        LevelCurve.getProgressToNextLevel(actualXp, level)
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
                    text = "$actualXp/${nextLevelXp} XP",
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