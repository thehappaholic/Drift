package com.happaholic.drift

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
    Home("Home"), Budget("Budget"), Focus("Focus"), Tasks("To-do"), Insights("Insights")
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
            val labelColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            val iconColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.primary
            }
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = action)
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .then(
                            if (destination == DriftDestination.Home) {
                                Modifier.size(36.dp)
                            } else {
                                Modifier.size(width = 48.dp, height = 32.dp)
                            }
                        )
                        .clip(
                            if (destination == DriftDestination.Home) {
                                CircleShape
                            } else {
                                RoundedCornerShape(50)
                            }
                        )
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    DestinationIcon(destination, iconColor)
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    destination.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor
                )
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
                val home = Path().apply {
                    moveTo(w * .10f, h * .50f)
                    lineTo(w * .50f, h * .14f)
                    lineTo(w * .90f, h * .50f)
                    moveTo(w * .22f, h * .43f)
                    lineTo(w * .22f, h * .88f)
                    lineTo(w * .42f, h * .88f)
                    lineTo(w * .42f, h * .62f)
                    lineTo(w * .58f, h * .62f)
                    lineTo(w * .58f, h * .88f)
                    lineTo(w * .78f, h * .88f)
                    lineTo(w * .78f, h * .43f)
                }
                drawPath(home, color, style = Stroke(width = stroke.width, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            DriftDestination.Budget -> {
                val timerCenter = androidx.compose.ui.geometry.Offset(w * .66f, h * .65f)
                drawCircle(color, w * .28f, timerCenter, style = stroke)
                drawLine(
                    color,
                    timerCenter,
                    androidx.compose.ui.geometry.Offset(w * .66f, h * .48f),
                    stroke.width,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                drawLine(
                    color,
                    timerCenter,
                    androidx.compose.ui.geometry.Offset(w * .55f, h * .72f),
                    stroke.width,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                val miniHourglass = Path().apply {
                    moveTo(w * .10f, h * .12f)
                    lineTo(w * .58f, h * .12f)
                    cubicTo(w * .56f, h * .29f, w * .45f, h * .34f, w * .34f, h * .42f)
                    cubicTo(w * .27f, h * .49f, w * .20f, h * .56f, w * .18f, h * .70f)
                    moveTo(w * .10f, h * .12f)
                    cubicTo(w * .12f, h * .29f, w * .23f, h * .34f, w * .34f, h * .42f)
                }
                drawPath(miniHourglass, color, style = Stroke(width = stroke.width, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            DriftDestination.Focus -> {
                val glass = Path().apply {
                    moveTo(w * .22f, h * .16f)
                    lineTo(w * .78f, h * .16f)
                    cubicTo(w * .76f, h * .36f, w * .62f, h * .44f, w * .50f, h * .50f)
                    cubicTo(w * .62f, h * .56f, w * .76f, h * .64f, w * .78f, h * .84f)
                    lineTo(w * .22f, h * .84f)
                    cubicTo(w * .24f, h * .64f, w * .38f, h * .56f, w * .50f, h * .50f)
                    cubicTo(w * .38f, h * .44f, w * .24f, h * .36f, w * .22f, h * .16f)
                }
                drawPath(glass, color, style = Stroke(width = stroke.width, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .18f, h * .14f), androidx.compose.ui.geometry.Offset(w * .82f, h * .14f), stroke.width, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .18f, h * .86f), androidx.compose.ui.geometry.Offset(w * .82f, h * .86f), stroke.width, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                val sand = Path().apply {
                    moveTo(w * .30f, h * .24f)
                    lineTo(w * .70f, h * .24f)
                    lineTo(w * .50f, h * .45f)
                    close()
                    moveTo(w * .34f, h * .78f)
                    lineTo(w * .50f, h * .61f)
                    lineTo(w * .66f, h * .78f)
                    close()
                }
                drawPath(sand, color)
            }
            DriftDestination.Tasks -> {
                drawRoundRect(
                    color,
                    topLeft = androidx.compose.ui.geometry.Offset(w * .13f, h * .20f),
                    size = androidx.compose.ui.geometry.Size(w * .68f, h * .68f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .08f),
                    style = stroke
                )
                val check = Path().apply {
                    moveTo(w * .34f, h * .50f)
                    lineTo(w * .48f, h * .64f)
                    lineTo(w * .90f, h * .18f)
                }
                drawPath(check, color, style = Stroke(width = stroke.width * 1.25f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            DriftDestination.Insights -> {
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .2f, h * .82f), androidx.compose.ui.geometry.Offset(w * .2f, h * .57f), stroke.width * 2)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .5f, h * .82f), androidx.compose.ui.geometry.Offset(w * .5f, h * .34f), stroke.width * 2)
                drawLine(color, androidx.compose.ui.geometry.Offset(w * .8f, h * .82f), androidx.compose.ui.geometry.Offset(w * .8f, h * .18f), stroke.width * 2)
            }
        }
    }
}
