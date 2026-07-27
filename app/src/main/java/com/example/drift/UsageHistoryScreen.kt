package com.example.drift

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val HistoryCardShape = RoundedCornerShape(16.dp)

@Composable
fun UsageHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    var hasAccess by remember { mutableStateOf(UsageHistoryRepository.hasUsageAccess(context)) }
    var history by remember { mutableStateOf<List<DailyUsageHistory>>(emptyList()) }
    var loading by remember { mutableStateOf(hasAccess) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshKey++
    }

    LaunchedEffect(refreshKey) {
        hasAccess = UsageHistoryRepository.hasUsageAccess(context)
        if (hasAccess) {
            loading = true
            history = withContext(Dispatchers.IO) {
                UsageHistoryRepository.loadLastSevenDays(context)
            }
            loading = false
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DriftBackButton(onClick = onBack)
                Column(Modifier.padding(start = 18.dp)) {
                    Text("Screen Time History", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Every app, day by day", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(22.dp))

            if (!hasAccess) {
                Column(
                    Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer, HistoryCardShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, HistoryCardShape)
                        .padding(20.dp)
                ) {
                    Text("Allow Usage Access", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "Drift needs Android Usage Access to calculate screen time for every app. This data stays on your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            settingsLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Open Usage Access settings")
                    }
                }
            } else if (loading) {
                Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val selected = history.firstOrNull { it.date == selectedDate }
                    ?: history.lastOrNull()
                    ?: DailyUsageHistory(LocalDate.now(), emptyList())
                val palette = screenTimePalette(selected.totalMinutes)

                Column(
                    Modifier.fillMaxWidth()
                        .background(palette.background, HistoryCardShape)
                        .padding(18.dp)
                ) {
                    Text(
                        selected.date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.foreground
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        formatUsageMinutes(selected.totalMinutes),
                        style = MaterialTheme.typography.displaySmall,
                        color = palette.foreground
                    )
                    Text(
                        "${selected.apps.size} apps used",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.foreground
                    )
                }

                Spacer(Modifier.height(18.dp))
                Text("Last 7 days", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    history.forEach { day ->
                        val selectedDay = day.date == selected.date
                        Column(
                            Modifier.clickable { selectedDate = day.date },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(5.dp))
                            Box(
                                Modifier.size(37.dp)
                                    .background(
                                        if (selectedDay) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    day.date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selectedDay) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))
                Text("All apps", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Column(
                    Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, HistoryCardShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, HistoryCardShape)
                        .padding(horizontal = 16.dp)
                ) {
                    if (selected.apps.isEmpty()) {
                        Text(
                            "No app usage recorded for this day.",
                            modifier = Modifier.padding(vertical = 18.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val longest = selected.apps.maxOf(AppUsageEntry::foregroundMinutes).coerceAtLeast(1)
                        selected.apps.forEachIndexed { index, app ->
                            Column(Modifier.padding(vertical = 12.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Text(app.appName, style = MaterialTheme.typography.bodyMedium)
                                    Text(formatUsageMinutes(app.foregroundMinutes), style = MaterialTheme.typography.labelLarge)
                                }
                                Spacer(Modifier.height(7.dp))
                                LinearProgressIndicator(
                                    progress = { app.foregroundMinutes / longest.toFloat() },
                                    modifier = Modifier.fillMaxWidth().height(5.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    drawStopIndicator = {}
                                )
                            }
                            if (index < selected.apps.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

private fun formatUsageMinutes(minutes: Int): String {
    val hours = minutes / 60
    val remainder = minutes % 60
    return when {
        hours == 0 -> "${remainder}m"
        remainder == 0 -> "${hours}h"
        else -> "${hours}h ${remainder}m"
    }
}
