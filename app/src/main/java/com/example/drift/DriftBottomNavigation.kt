package com.example.drift

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class DriftDestination(val label: String) {
    Home("Home"), Budget("Budget"), Focus("Focus"), Tasks("Tasks"), Insights("Insights")
}

@Composable
fun DriftBottomNavigation(
    selected: DriftDestination?,
    onHome: () -> Unit,
    onBudget: () -> Unit,
    onFocus: () -> Unit,
    onTasks: () -> Unit,
    onInsights: () -> Unit
) {
    val destinations = listOf(
        DriftDestination.Budget to onBudget,
        DriftDestination.Tasks to onTasks,
        DriftDestination.Home to onHome,
        DriftDestination.Focus to onFocus,
        DriftDestination.Insights to onInsights
    )
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant).navigationBarsPadding()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        destinations.forEach { (destination, action) ->
            val isSelected = destination == selected
            val ink = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            val iconColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.primary
            }
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable(onClick = action)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DestinationIcon(
                    destination,
                    iconColor
                )
                Spacer(Modifier.height(3.dp))
                Text(destination.label, style = MaterialTheme.typography.labelSmall, color = ink)
            }
        }
    }
}

@Composable
private fun DestinationIcon(destination: DriftDestination, color: Color) {
    Canvas(Modifier.size(19.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.7.dp.toPx())
        when (destination) {
            DriftDestination.Home -> {
                val roof = Path().apply { moveTo(w * .14f, h * .48f); lineTo(w * .5f, h * .18f); lineTo(w * .86f, h * .48f) }
                drawPath(roof, color, style = stroke)
                drawRect(color, topLeft = androidx.compose.ui.geometry.Offset(w * .24f, h * .45f), size = androidx.compose.ui.geometry.Size(w * .52f, h * .4f), style = stroke)
            }
            DriftDestination.Budget -> {
                drawCircle(color, w * .35f, center, style = stroke)
                drawLine(color, center, androidx.compose.ui.geometry.Offset(w * .5f, h * .29f), stroke.width)
                drawLine(color, center, androidx.compose.ui.geometry.Offset(w * .68f, h * .55f), stroke.width)
            }
            DriftDestination.Focus -> {
                // A compact, filled hourglass matching Drift's friendly icon language.
                drawRoundRect(
                    color,
                    topLeft = androidx.compose.ui.geometry.Offset(w * .17f, h * .10f),
                    size = androidx.compose.ui.geometry.Size(w * .66f, h * .12f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .06f)
                )
                val glass = Path().apply {
                    moveTo(w * .25f, h * .22f)
                    cubicTo(w * .27f, h * .39f, w * .41f, h * .44f, w * .50f, h * .50f)
                    cubicTo(w * .41f, h * .56f, w * .27f, h * .61f, w * .25f, h * .78f)
                    lineTo(w * .75f, h * .78f)
                    cubicTo(w * .73f, h * .61f, w * .59f, h * .56f, w * .50f, h * .50f)
                    cubicTo(w * .59f, h * .44f, w * .73f, h * .39f, w * .75f, h * .22f)
                    close()
                }
                drawPath(glass, color)
                drawRoundRect(
                    color,
                    topLeft = androidx.compose.ui.geometry.Offset(w * .17f, h * .78f),
                    size = androidx.compose.ui.geometry.Size(w * .66f, h * .12f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .06f)
                )
            }
            DriftDestination.Tasks -> {
                drawRoundRect(color, topLeft = androidx.compose.ui.geometry.Offset(w * .18f, h * .16f), size = androidx.compose.ui.geometry.Size(w * .64f, h * .68f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .08f), style = stroke)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .3f, h * .52f), androidx.compose.ui.geometry.Offset(w * .43f, h * .65f), stroke.width)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .43f, h * .65f), androidx.compose.ui.geometry.Offset(w * .7f, h * .36f), stroke.width)
            }
            DriftDestination.Insights -> {
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .2f, h * .82f), androidx.compose.ui.geometry.Offset(w * .2f, h * .57f), stroke.width * 2)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .5f, h * .82f), androidx.compose.ui.geometry.Offset(w * .5f, h * .34f), stroke.width * 2)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .8f, h * .82f), androidx.compose.ui.geometry.Offset(w * .8f, h * .18f), stroke.width * 2)
            }
        }
    }
}
