package com.example.drift

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

private data class AppBudget(
    val name: String,
    val used: Int,
    val limit: Int,
    val status: String? = null
)

@Composable
fun UsageBudgetScreen(
    onBack: () -> Unit,
    onEditClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onFocusClick: () -> Unit = {},
    onTasksClick: () -> Unit = {},
    onInsightsClick: () -> Unit = {},
    instagramLimit: Int,
    youtubeLimit: Int,
    browserLimit: Int,
    additionalBudgets: Map<String, Int> = emptyMap()
) {
    val context = LocalContext.current
    var todayUsage by remember {
        mutableStateOf(DailyUsageHistory(LocalDate.now(), emptyList(), availability = UsageDataAvailability.Partial))
    }
    LaunchedEffect(Unit) {
        if (UsageHistoryRepository.hasUsageAccess(context)) {
            todayUsage = withContext(Dispatchers.IO) {
                UsageHistoryRepository.loadLastSevenDays(context).lastOrNull()
                    ?: DailyUsageHistory(LocalDate.now(), emptyList(), availability = UsageDataAvailability.Partial)
            }
        }
    }
    val dailyUsageMinutes = todayUsage.totalMinutes
    val dailyUsagePalette = screenTimePalette(dailyUsageMinutes)
    fun usedMinutes(vararg names: String): Int = todayUsage.apps
        .firstOrNull { app -> names.any { name -> name.equals(app.appName, ignoreCase = true) } }
        ?.foregroundMinutes ?: 0
    val apps = listOf(
        AppBudget("Instagram", usedMinutes("Instagram"), instagramLimit),
        AppBudget("YouTube", usedMinutes("YouTube"), youtubeLimit),
        AppBudget("Chrome", usedMinutes("Chrome", "Browser"), browserLimit)
    ) + additionalBudgets.toSortedMap(String.CASE_INSENSITIVE_ORDER).map { (name, limit) ->
        AppBudget(name, usedMinutes(name), limit)
    } + listOf(
        AppBudget("WhatsApp", usedMinutes("WhatsApp"), 0, "Whitelisted")
    )

    val allocatedMinutes =
        instagramLimit + youtubeLimit + browserLimit + additionalBudgets.values.sum()

    val flexPoolMinutes =
        (180 - allocatedMinutes).coerceAtLeast(0)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            DriftBottomNavigation(DriftDestination.Budget, onHomeClick, {}, onFocusClick, onTasksClick, onInsightsClick)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DriftBackButton(onClick = onBack)

                Text(
                    text = "Usage Budget",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 18.dp)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Daily Limit",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "180 min",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { dailyUsageMinutes / 180f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = dailyUsagePalette.background,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                drawStopIndicator = {}
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "$dailyUsageMinutes min used · ${usageStatusLabel(dailyUsageMinutes)}",
                    fontSize = 13.sp,
                    color = riskAccentColor(dailyUsagePalette)
                )
                Text("${(180 - dailyUsageMinutes).coerceAtLeast(0)} min left", fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "App Budgets",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 16.dp)
            ) {
                apps.forEachIndexed { index, app ->
                    AppBudgetRow(app)

                    if (index < apps.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Flex Pool",
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(text = "$flexPoolMinutes min left")
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = {
                        flexPoolMinutes.toFloat() / 180f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    drawStopIndicator = {}
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onEditClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Edit Budget",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun AppBudgetRow(
    app: AppBudget
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = app.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (app.status != null) {
                Text(
                    text = if (app.used > 0) "${app.status} · ${app.used} min" else app.status,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "${app.limit} min",
                    fontSize = 13.sp
                )
            }
        }

        if (app.status == null) {
            val palette = budgetUsagePalette(app.used, app.limit)
            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = {
                    if (app.limit == 0) 0f
                    else app.used.toFloat() / app.limit.toFloat()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = palette.background,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                drawStopIndicator = {}
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${app.used} / ${app.limit} min",
                fontSize = 11.sp,
                color = riskAccentColor(palette),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

private fun usageStatusLabel(minutes: Int): String = when (screenTimeBand(minutes)) {
    ScreenTimeBand.Low1, ScreenTimeBand.Low2, ScreenTimeBand.Low3 -> "Low"
    ScreenTimeBand.Medium1 -> "Moderate"
    ScreenTimeBand.Medium2, ScreenTimeBand.Medium3 -> "Elevated"
    ScreenTimeBand.High -> "High"
}
