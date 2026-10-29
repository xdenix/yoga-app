package com.example.yogaapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Compact − / value / + counter (replaces sliders for duration / pause settings).
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
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        OutlinedIconButton(
            onClick = { onValueChange((value - step).coerceIn(min, max)) },
            enabled = value > min,
            modifier = Modifier.semantics { contentDescription = decreaseContentDescription },
        ) {
            Icon(Icons.Filled.Remove, contentDescription = null)
        }
        Text(
            text = valueText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 72.dp),
        )
        OutlinedIconButton(
            onClick = { onValueChange((value + step).coerceIn(min, max)) },
            enabled = value < max,
            modifier = Modifier.semantics { contentDescription = increaseContentDescription },
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
        }
    }
}
