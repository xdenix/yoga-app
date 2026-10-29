package com.example.yogaapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.yogaapp.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val WEEKDAYS_RU = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

/**
 * Compact monthly calendar card highlighting days with completed workouts.
 */
@Composable
fun WorkoutCalendarCard(
    trainedDates: Set<LocalDate>,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    val monthTrainedCount = remember(trainedDates, month) {
        trainedDates.count { YearMonth.from(it) == month }
    }
    val ruLocale = remember { Locale.forLanguageTag("ru-RU") }
    val monthTitle = remember(month) {
        val raw = month.month.getDisplayName(TextStyle.FULL_STANDALONE, ruLocale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(ruLocale) else it.toString() }
        "$raw ${month.year}"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = { month = month.minusMonths(1) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(
                            R.string.calendar_trained_summary,
                            monthTrainedCount,
                            trainedDates.size,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(
                    onClick = { month = month.plusMonths(1) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                WEEKDAYS_RU.forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            val firstOfMonth = month.atDay(1)
            // Monday = 0 .. Sunday = 6
            val leadingBlanks = (firstOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
            val daysInMonth = month.lengthOfMonth()
            val totalCells = ((leadingBlanks + daysInMonth + 6) / 7) * 7

            for (row in 0 until totalCells / 7) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNum = cellIndex - leadingBlanks + 1
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (dayNum in 1..daysInMonth) {
                                val date = month.atDay(dayNum)
                                val isTrained = date in trainedDates
                                val isToday = date == today
                                CalendarDayCell(
                                    day = dayNum,
                                    isTrained = isTrained,
                                    isToday = isToday,
                                )
                            } else {
                                Box(modifier = Modifier.size(30.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    isTrained: Boolean,
    isToday: Boolean,
) {
    val bg = if (isTrained) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = when {
        isTrained -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderMod = if (isToday && !isTrained) {
        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(bg)
            .then(borderMod),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isTrained || isToday) FontWeight.Bold else FontWeight.Normal,
            color = fg,
        )
    }
}
