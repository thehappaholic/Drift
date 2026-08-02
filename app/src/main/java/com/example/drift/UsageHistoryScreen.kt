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
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
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
    var previousWeekCount by remember { mutableIntStateOf(0) }
    var expandedPackage by remember(selectedDate) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshKey++
    }

    LaunchedEffect(refreshKey, previousWeekCount) {
        hasAccess = UsageHistoryRepository.hasUsageAccess(context)
        if (hasAccess) {
            loading = true
            history = withContext(Dispatchers.IO) {
                UsageHistoryRepository.loadSevenDaysEnding(
                    context,
                    LocalDate.now().minusDays(previousWeekCount * 7L)
                )
            }
            history.lastOrNull()?.let { selectedDate = it.date }
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
                val unavailable = selected.availability == UsageDataAvailability.Unavailable
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
                        if (unavailable) "Not collected" else formatUsageMinutes(selected.totalMinutes),
                        style = MaterialTheme.typography.displaySmall,
                        color = palette.foreground
                    )
                    Text(
                        if (unavailable) "Drift had not started mobile-only tracking" else if (selected.availability == UsageDataAvailability.Partial) "Today · still collecting" else "${selected.apps.size} apps used · finalized",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.foreground
                    )
                }

                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    IconButton(onClick = { previousWeekCount++ }, modifier = Modifier.size(34.dp)) {
                        Text("‹", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (previousWeekCount == 0) "Last 7 days" else "Previous week",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (history.isNotEmpty()) {
                            Text(
                                "${history.first().date.format(DateTimeFormatter.ofPattern("d MMM"))} – ${history.last().date.format(DateTimeFormatter.ofPattern("d MMM"))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = { if (previousWeekCount > 0) previousWeekCount-- },
                        enabled = previousWeekCount > 0,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Text(
                            "›",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (previousWeekCount > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
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
                    if (unavailable) {
                        Text(
                            "Exact Android events are no longer available for this date. Drift will not estimate or invent a total.",
                            modifier = Modifier.padding(vertical = 18.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (selected.apps.isEmpty()) {
                        Text(
                            "No app usage recorded for this day.",
                            modifier = Modifier.padding(vertical = 18.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val longest = selected.apps.maxOf(AppUsageEntry::foregroundMinutes).coerceAtLeast(1)
                        selected.apps.forEachIndexed { index, app ->
                            val appPalette = screenTimePalette(app.foregroundMinutes)
                            Column(Modifier.padding(vertical = 12.dp)) {
                                Row(
                                    Modifier.fillMaxWidth().clickable {
                                        val opening = expandedPackage != app.packageName
                                        expandedPackage = if (opening) app.packageName else null
                                        if (opening && app.hourlyMillis.size != 24) {
                                            scope.launch {
                                                val refreshed = withContext(Dispatchers.IO) {
                                                    UsageHistoryRepository.loadDay(context, selected.date)
                                                }
                                                if (refreshed != null) {
                                                    history = history.map { if (it.date == refreshed.date) refreshed else it }
                                                }
                                            }
                                        }
                                    },
                                    Arrangement.SpaceBetween,
                                    Alignment.CenterVertically
                                ) {
                                    Text(app.appName, style = MaterialTheme.typography.bodyMedium)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                        Text(formatUsageMinutes(app.foregroundMinutes), style = MaterialTheme.typography.labelLarge,
                                            color = riskAccentColor(appPalette))
                                        Icon(
                                            painter = painterResource(R.drawable.ic_app_expand),
                                            contentDescription = if (expandedPackage == app.packageName) "Hide hourly pattern" else "Show hourly pattern",
                                            tint = riskAccentColor(appPalette),
                                            modifier = Modifier.size(18.dp).rotate(if (expandedPackage == app.packageName) 180f else 0f)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(7.dp))
                                LinearProgressIndicator(
                                    progress = { app.foregroundMinutes / longest.toFloat() },
                                    modifier = Modifier.fillMaxWidth().height(5.dp),
                                    color = appPalette.background,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    drawStopIndicator = {}
                                )
                                if (expandedPackage == app.packageName) {
                                    AppHourlyUsageChart(app, appPalette.background)
                                }
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
