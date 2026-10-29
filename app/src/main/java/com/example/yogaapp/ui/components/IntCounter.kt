package com.example.yogaapp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Framed numeric counter (`[ − | value | + ]`) with rolling digit animation.
 */
@Composable
fun IntCounter(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    modifier: Modifier = Modifier,
    step: Int = 5,
    valueText: String = value.toString(),
    decreaseContentDescription: String = "−",
    increaseContentDescription: String = "+",
) {
    val min = valueRange.first
    val max = valueRange.last
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { onValueChange((value - step).coerceIn(min, max)) },
            enabled = value > min,
            modifier = Modifier
                .size(40.dp)
                .semantics { contentDescription = decreaseContentDescription },
        ) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
        Box(
            modifier = Modifier
                .widthIn(min = 64.dp)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = value to valueText,
                transitionSpec = {
                    val up = targetState.first >= initialState.first
                    val enter = slideInVertically { h -> if (up) h else -h } + fadeIn()
                    val exit = slideOutVertically { h -> if (up) -h else h } + fadeOut()
                    enter togetherWith exit
                },
                label = "counterRoll",
            ) { (_, label) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
        IconButton(
            onClick = { onValueChange((value + step).coerceIn(min, max)) },
            enabled = value < max,
            modifier = Modifier
                .size(40.dp)
                .semantics { contentDescription = increaseContentDescription },
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * Settings row with title (+ optional subtitle) on the left and [IntCounter] on the right.
 */
@Composable
fun CounterSettingRow(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    step: Int = 5,
    valueText: String = value.toString(),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IntCounter(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            step = step,
            valueText = valueText,
        )
    }
}
