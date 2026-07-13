package com.example.drift

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.drift.ui.theme.*

private val InsightBlue = Color(0xFFDCEAF4)
private val InsightBlueInk = Color(0xFF315E78)
private val InsightLilac = Color(0xFFE9E0F3)
private val InsightLilacInk = Color(0xFF624A78)
private val InsightMint = Color(0xFFD5F1EA)
private val InsightMintInk = Color(0xFF23675D)
private val InsightCard = RoundedCornerShape(14.dp)

@Composable
fun InsightsScreen(
    onHomeClick: () -> Unit,
    onBudgetClick: () -> Unit = {},
    onFocusClick: () -> Unit = {},
    onTasksClick: () -> Unit = {},
) {
    var range by remember { mutableStateOf("7 days") }
    val scores = if (range == "7 days") listOf(62, 70, 68, 78, 73, 81, 78) else listOf(66, 69, 72, 76)
    val days = if (range == "7 days") listOf("M", "T", "W", "T", "F", "S", "S") else listOf("W1", "W2", "W3", "W4")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            DriftBottomNavigation(DriftDestination.Insights, onHomeClick, onBudgetClick, onFocusClick, onTasksClick, {})
        }
    ) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text("Insights", style = MaterialTheme.typography.headlineSmall)
                    Text("Your week, without the guilt.", style = MaterialTheme.typography.bodyMedium, color = Muted)
                }
                Row(Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(9.dp)).padding(3.dp)) {
                    listOf("7 days", "4 weeks").forEach { option ->
                        Text(
                            option,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (range == option) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clip(RoundedCornerShape(7.dp)).clickable { range = option }
                                .background(if (range == option) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .padding(horizontal = 9.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            WeeklyWinCard()
            Spacer(Modifier.height(26.dp))
            SectionHeading("FOCUS RHYTHM", "Daily score")
            Spacer(Modifier.height(10.dp))
            FocusChart(scores, days)
            Spacer(Modifier.height(26.dp))
            SectionHeading("WHAT CHANGED", "Compared with last week")
            Spacer(Modifier.height(10.dp))
            ChangeCards()
            Spacer(Modifier.height(26.dp))
            SectionHeading("RISK PATTERN", "Last four days")
            Spacer(Modifier.height(10.dp))
            RiskHistoryCard()
            Spacer(Modifier.height(26.dp))
            PatternCard(onHomeClick)
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun WeeklyWinCard() {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer, InsightCard)
            .border(1.dp, MaterialTheme.colorScheme.outline, InsightCard).padding(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("THIS WEEK", style = MaterialTheme.typography.labelSmall, color = InsightLilacInk)
            Text("↑ 12%", style = MaterialTheme.typography.labelMedium, color = InsightLilacInk,
                modifier = Modifier.background(Color(0xFFF4EDF9), RoundedCornerShape(7.dp)).padding(horizontal = 8.dp, vertical = 5.dp))
        }
        Spacer(Modifier.height(15.dp))
        Text("7h 24m", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text("intentional time reclaimed", style = MaterialTheme.typography.bodyLarge, color = InsightLilacInk)
        Spacer(Modifier.height(12.dp))
        Text("That’s almost one extra evening back.", style = MaterialTheme.typography.bodyMedium, color = Muted)
    }
}

@Composable
private fun FocusChart(scores: List<Int>, days: List<String>) {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, InsightCard).border(1.dp, MaterialTheme.colorScheme.outline, InsightCard).padding(18.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
            Column {
                Text("73", style = MaterialTheme.typography.headlineSmall)
                Text("weekly average", style = MaterialTheme.typography.labelSmall, color = Muted)
            }
            Text("+5 points", style = MaterialTheme.typography.labelMedium, color = Sage,
                modifier = Modifier.background(SageSoft, RoundedCornerShape(7.dp)).padding(horizontal = 8.dp, vertical = 5.dp))
        }
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth().height(150.dp), Arrangement.spacedBy(8.dp), Alignment.Bottom) {
            scores.forEachIndexed { index, score ->
                val barColor = when {
                    score >= 78 -> InsightMint
                    score >= 70 -> InsightBlue
                    else -> Color(0xFFF4DDCF)
                }
                val ink = when {
                    score >= 78 -> InsightMintInk
                    score >= 70 -> InsightBlueInk
                    else -> Color(0xFF8A583C)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(score.toString(), style = MaterialTheme.typography.labelSmall, color = ink)
                    Spacer(Modifier.height(5.dp))
                    Box(
                        Modifier.fillMaxWidth().height((score * 1.05).dp).clip(RoundedCornerShape(7.dp, 7.dp, 3.dp, 3.dp))
                            .background(barColor)
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(days[index], style = MaterialTheme.typography.labelSmall, color = Muted)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Your strongest focus was over the weekend.", style = MaterialTheme.typography.bodyMedium, color = Muted)
    }
}

@Composable
private fun ChangeCards() {
    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
        ChangeCard("Screen time", "−42m", "per day", InsightMint, InsightMintInk, Modifier.weight(1f))
        ChangeCard("Phone checks", "−11", "per day", InsightBlue, InsightBlueInk, Modifier.weight(1f))
    }
}

@Composable
private fun ChangeCard(title: String, value: String, note: String, bg: Color, ink: Color, modifier: Modifier) {
    Column(modifier.background(bg, InsightCard).padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = ink)
        Spacer(Modifier.height(15.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(note, style = MaterialTheme.typography.labelSmall, color = Muted)
    }
}

@Composable
private fun RiskHistoryCard() {
    val history = listOf("Today" to "Medium", "Yesterday" to "Low", "Friday" to "Medium", "Thursday" to "High")
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, InsightCard).border(1.dp, MaterialTheme.colorScheme.outline, InsightCard).padding(16.dp)) {
        history.forEachIndexed { index, (day, risk) ->
            val palette = riskPalette(risk)
            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(day, style = MaterialTheme.typography.bodyMedium)
                Text(risk, style = MaterialTheme.typography.labelMedium, color = palette.foreground,
                    modifier = Modifier.background(palette.background, RoundedCornerShape(7.dp)).padding(horizontal = 10.dp, vertical = 5.dp))
            }
            if (index < history.lastIndex) HorizontalDivider(color = Border)
        }
    }
}

@Composable
private fun PatternCard(onAdjustWindDown: () -> Unit) {
    val palette = riskPalette("Medium")
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onAdjustWindDown)
            .background(palette.background, InsightCard).padding(18.dp)
    ) {
        Text("A pattern worth noticing", style = MaterialTheme.typography.labelSmall, color = palette.foreground)
        Spacer(Modifier.height(8.dp))
        Text("Late-night phone checks", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text("On nights with checks after 11 PM, your next-day focus score is about 9 points lower.",
            style = MaterialTheme.typography.bodyMedium, color = Color(0xFF6F5434))
        Spacer(Modifier.height(15.dp))
        Text("Adjust wind-down  →", style = MaterialTheme.typography.labelLarge, color = palette.foreground)
    }
}

@Composable
private fun SectionHeading(title: String, trailing: String) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = Muted)
        Text(trailing, style = MaterialTheme.typography.labelSmall, color = Muted)
    }
}
