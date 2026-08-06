package com.happaholic.drift

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AppHourlyUsageChart(
    app: AppUsageEntry,
    barColor: Color,
    highlightedHourlyMillis: List<Long> = emptyList(),
    highlightColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val hours = app.hourlyMillis.takeIf { it.size == 24 }
    Column(modifier.fillMaxWidth().padding(top = 10.dp)) {
        if (hours == null) {
            Text(
                "Hourly pattern unavailable for this date.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }
        val maximum = hours.maxOrNull()?.coerceAtLeast(1L) ?: 1L
        Row(Modifier.fillMaxWidth().height(70.dp), Arrangement.spacedBy(2.dp), Alignment.Bottom) {
            hours.forEachIndexed { index, millis ->
                val highlighted = highlightedHourlyMillis.getOrNull(index)?.coerceAtMost(millis) ?: 0L
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier.fillMaxWidth()
                            .height(if (millis == 0L) 2.dp else (5f + millis / maximum.toFloat() * 60f).dp)
                            .clip(RoundedCornerShape(3.dp, 3.dp, 1.dp, 1.dp))
                            .background(if (millis == 0L) MaterialTheme.colorScheme.outlineVariant else barColor)
                    )
                    if (highlighted > 0L && highlightColor != Color.Unspecified) {
                        Box(
                            Modifier.fillMaxWidth()
                                .height((5f + highlighted / maximum.toFloat() * 60f).dp)
                                .clip(RoundedCornerShape(3.dp, 3.dp, 1.dp, 1.dp))
                                .background(highlightColor)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            listOf("12am", "6am", "12pm", "6pm", "11pm").forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val peakHour = hours.indices.maxByOrNull { hours[it] } ?: 0
        if (hours[peakHour] > 0L) {
            Spacer(Modifier.height(5.dp))
            Text(
                "Peak use: ${hourLabel(peakHour)}–${hourLabel((peakHour + 1) % 24)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun hourLabel(hour: Int): String = when {
    hour == 0 -> "12am"
    hour < 12 -> "${hour}am"
    hour == 12 -> "12pm"
    else -> "${hour - 12}pm"
}
