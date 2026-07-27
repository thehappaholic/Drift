package com.example.drift

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import com.example.drift.ui.theme.DriftLilac
import com.example.drift.data.assignment.Assignment
import com.example.drift.data.assignment.AssignmentRepository
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.sqrt

@Composable
fun FocusTimerScreen(
    onBack: () -> Unit,
    sessionStarted: Boolean,
    onStartSession: () -> Unit,
    remainingSeconds: Int,
    onRemainingSecondsChange: (Int) -> Unit,
    onTakeBreak: (Int) -> Unit,
    onSessionComplete: () -> Unit
) {
    val sessionLengthSeconds = 40 * 60
    var paused by remember { mutableStateOf(false) }
    var showEarlyBreakMessage by remember { mutableStateOf(false) }
    var showEndConfirmation by remember { mutableStateOf(false) }
    var showProtectionInfo by remember { mutableStateOf(false) }
    var sessionGoal by remember { mutableStateOf<Assignment?>(null) }
    val timerPaused = !sessionStarted || paused || showEndConfirmation

    BackHandler {
        if (sessionStarted) showEndConfirmation = true else onBack()
    }

    LaunchedEffect(Unit) {
        AssignmentRepository.loadAssignments()
            .onSuccess { assignments ->
                sessionGoal = assignments.firstOrNull { !it.isCompleted }
            }
    }

    LaunchedEffect(timerPaused, remainingSeconds) {
        if (!timerPaused && remainingSeconds > 0) {
            delay(1000)
            onRemainingSecondsChange(remainingSeconds - 1)
        }
    }

    LaunchedEffect(remainingSeconds) {
        if (sessionStarted && remainingSeconds == 0) onSessionComplete()
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val formattedTime = "%02d:%02d".format(minutes, seconds)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (sessionStarted) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            val elapsed = sessionLengthSeconds - remainingSeconds
                            if (elapsed >= 10 * 60) onTakeBreak(elapsed)
                            else showEarlyBreakMessage = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Take a Break",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when {
                            remainingSeconds == 0 -> "Focus session complete!"
                            paused -> "Session paused"
                            else -> "Stay focused. You've got this!"
                        },
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
                DriftBackButton(
                    onClick = {
                        if (sessionStarted) showEndConfirmation = true else onBack()
                    },
                    contentDescription = if (sessionStarted) {
                        "End focus session and go back"
                    } else {
                        "Go back"
                    }
                )

                Text(
                    text = "Focus Timer",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 18.dp)
                )

            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Deep Focus",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                IconButton(
                    onClick = { showProtectionInfo = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Text(
                        text = "!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .size(22.dp)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                    )
                }
            }

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
                    .padding(17.dp)
            ) {
                Text(
                    text = "Session goal",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = sessionGoal?.title ?: "Work on AI Report",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                sessionGoal?.let { goal ->
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = goal.deadlineCountdown(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val palette = riskPalette(goal.priority)
                        Text(
                            text = goal.priority,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.foreground,
                            modifier = Modifier
                                .background(palette.background, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LogicalFocusHourglass(
                    progress = remainingSeconds.toFloat() / sessionLengthSeconds.toFloat(),
                    isRunning = !timerPaused && remainingSeconds > 0,
                    modifier = Modifier.size(width = 190.dp, height = 210.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Focus time left",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = formattedTime,
                    fontSize = 39.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (sessionStarted) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { paused = !paused }
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(
                                if (paused) R.drawable.ic_play else R.drawable.ic_pause
                            ),
                            contentDescription = if (paused) {
                                "Resume focus timer"
                            } else {
                                "Pause focus timer"
                            },
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onStartSession,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Start Focus Session",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showEndConfirmation) {
        AlertDialog(
            onDismissRequest = { showEndConfirmation = false },
            title = { Text("End focus session?") },
            text = {
                Text(
                    "Your current focus session will end and its remaining time will be cleared. Are you sure you want to return home?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEndConfirmation = false
                        onBack()
                    }
                ) {
                    Text("End session")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirmation = false }) {
                    Text("Keep focusing")
                }
            }
        )
    }

    if (showProtectionInfo) {
        AlertDialog(
            onDismissRequest = { showProtectionInfo = false },
            title = {
                Text(
                    if (sessionStarted) {
                        "Focus protection is on"
                    } else {
                        "Focus protection is ready"
                    }
                )
            },
            text = {
                Text(
                    if (sessionStarted) {
                        "Distracting apps pause automatically. Calls, messages, WhatsApp and audio stay available."
                    } else {
                        "Focus protection will turn on when you start the session. Calls, messages, WhatsApp and audio will remain available."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { showProtectionInfo = false }) {
                    Text("Got it")
                }
            }
        )
    }

    if (showEarlyBreakMessage) {
        AlertDialog(
            onDismissRequest = { showEarlyBreakMessage = false },
            title = { Text("Give yourself time to settle") },
            text = {
                Text(
                    "A restorative break becomes available after 10 focused minutes. Keep going—you’re still settling into the task.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showEarlyBreakMessage = false }) {
                    Text("Continue focusing")
                }
            }
        )
    }
}

private fun Assignment.deadlineCountdown(now: LocalDateTime = LocalDateTime.now()): String {
    val deadline = runCatching {
        LocalDateTime.of(
            LocalDate.parse(deadlineDate),
            deadlineTime?.let(LocalTime::parse) ?: LocalTime.of(23, 59)
        )
    }.getOrNull() ?: return "Deadline unavailable"

    val minutesLeft = ChronoUnit.MINUTES.between(now, deadline)
    if (minutesLeft < 0) return "Overdue"

    val hoursLeft = (minutesLeft + 59) / 60
    return if (hoursLeft < 24) {
        "$hoursLeft ${if (hoursLeft == 1L) "hour" else "hours"} left"
    } else {
        val daysLeft = (hoursLeft + 23) / 24
        "$daysLeft ${if (daysLeft == 1L) "day" else "days"} left"
    }
}

@Composable
private fun FocusHourglass(
    progress: Float,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val frameColor = MaterialTheme.colorScheme.onSurfaceVariant
    val glassFill = MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = MaterialTheme.colorScheme.surface
    val sandBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            DriftLilac,
            MaterialTheme.colorScheme.primaryContainer
        )
    )
    val animation = rememberInfiniteTransition(label = "hourglass sand")
    val streamPulse by animation.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sand stream pulse"
    )

    Canvas(modifier = modifier) {
        val remaining = progress.coerceIn(0f, 1f)
        val transferred = 1f - remaining
        val centerX = size.width / 2f
        val glassLeft = size.width * 0.16f
        val glassRight = size.width * 0.84f
        val topY = size.height * 0.09f
        val bottomY = size.height * 0.91f
        val neckTopY = size.height * 0.475f
        val neckBottomY = size.height * 0.525f
        val neckHalfWidth = size.width * 0.025f

        val glassPath = Path().apply {
            moveTo(glassLeft, topY)
            lineTo(glassRight, topY)
            cubicTo(
                glassRight,
                size.height * 0.28f,
                centerX + neckHalfWidth,
                neckTopY,
                centerX + neckHalfWidth,
                neckTopY
            )
            cubicTo(
                centerX + neckHalfWidth,
                neckBottomY,
                glassRight,
                size.height * 0.72f,
                glassRight,
                bottomY
            )
            lineTo(glassLeft, bottomY)
            cubicTo(
                glassLeft,
                size.height * 0.72f,
                centerX - neckHalfWidth,
                neckBottomY,
                centerX - neckHalfWidth,
                neckBottomY
            )
            cubicTo(
                centerX + neckHalfWidth,
                neckTopY,
                glassLeft,
                size.height * 0.28f,
                glassLeft,
                topY
            )
            close()
        }

        drawPath(
            path = glassPath,
            color = glassFill.copy(alpha = 0.22f)
        )

        if (remaining > 0f) {
            val upperSurfaceY = topY + (neckTopY - topY) * (1f - remaining)
            val upperWidthRatio =
                ((neckTopY - upperSurfaceY) / (neckTopY - topY)).coerceIn(0f, 1f)
            val upperHalfWidth =
                neckHalfWidth + (glassRight - glassLeft) / 2f * upperWidthRatio
            val upperSand = Path().apply {
                moveTo(centerX - upperHalfWidth, upperSurfaceY)
                quadraticTo(
                    centerX,
                    upperSurfaceY - size.height * 0.018f,
                    centerX + upperHalfWidth,
                    upperSurfaceY
                )
                lineTo(centerX + neckHalfWidth, neckTopY)
                lineTo(centerX - neckHalfWidth, neckTopY)
                close()
            }
            drawPath(upperSand, sandBrush)

            val upperFacet = Path().apply {
                moveTo(centerX - upperHalfWidth * 0.72f, upperSurfaceY + size.height * 0.012f)
                lineTo(centerX - neckHalfWidth, neckTopY)
                lineTo(centerX - upperHalfWidth * 0.18f, upperSurfaceY + size.height * 0.012f)
                close()
            }
            drawPath(upperFacet, highlightColor.copy(alpha = 0.14f))
        }

        if (transferred > 0f) {
            val lowerSurfaceY = bottomY - (bottomY - neckBottomY) * transferred
            val lowerSand = Path().apply {
                moveTo(glassLeft, bottomY)
                quadraticTo(
                    centerX - size.width * 0.14f,
                    lowerSurfaceY + size.height * 0.045f,
                    centerX,
                    lowerSurfaceY
                )
                quadraticTo(
                    centerX + size.width * 0.14f,
                    lowerSurfaceY + size.height * 0.045f,
                    glassRight,
                    bottomY
                )
                lineTo(glassRight, bottomY)
                close()
            }
            drawPath(lowerSand, sandBrush)

            val leftFacet = Path().apply {
                moveTo(glassLeft, bottomY)
                lineTo(centerX, lowerSurfaceY)
                lineTo(centerX - size.width * 0.06f, bottomY)
                close()
            }
            val rightFacet = Path().apply {
                moveTo(centerX, lowerSurfaceY)
                lineTo(glassRight, bottomY)
                lineTo(centerX + size.width * 0.10f, bottomY)
                close()
            }
            drawPath(leftFacet, highlightColor.copy(alpha = 0.16f))
            drawPath(rightFacet, frameColor.copy(alpha = 0.10f))
        }

        if (isRunning) {
            drawLine(
                brush = sandBrush,
                start = Offset(centerX, neckBottomY),
                end = Offset(
                    centerX,
                    (bottomY - (bottomY - neckBottomY) * transferred - size.height * 0.01f)
                        .coerceAtLeast(neckBottomY)
                ),
                alpha = streamPulse,
                strokeWidth = size.width * 0.022f,
                cap = StrokeCap.Round
            )
        }

        drawPath(
            path = glassPath,
            color = frameColor,
            style = Stroke(
                width = size.width * 0.045f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        val glassGlint = Path().apply {
            moveTo(glassLeft + size.width * 0.055f, topY + size.height * 0.045f)
            cubicTo(
                glassLeft + size.width * 0.055f,
                size.height * 0.25f,
                centerX - size.width * 0.16f,
                size.height * 0.36f,
                centerX - size.width * 0.10f,
                size.height * 0.41f
            )
        }
        drawPath(
            path = glassGlint,
            color = highlightColor.copy(alpha = 0.45f),
            style = Stroke(
                width = size.width * 0.016f,
                cap = StrokeCap.Round
            )
        )
    }
}

@Composable
private fun LogicalFocusHourglass(
    progress: Float,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "timer-linked sand level"
    )
    val frameColor = DriftLilac
    val glassFill = MaterialTheme.colorScheme.surfaceVariant
    val sandBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            DriftLilac,
            MaterialTheme.colorScheme.primaryContainer
        )
    )
    val animation = rememberInfiniteTransition(label = "falling sand")
    val grainPhase by animation.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "individual falling grains"
    )

    Canvas(modifier = modifier) {
        val remaining = animatedProgress.coerceIn(0f, 1f)
        val transferred = 1f - remaining
        val centerX = size.width / 2f
        val railLeft = size.width * 0.08f
        val railRight = size.width * 0.92f
        val glassLeft = size.width * 0.15f
        val glassRight = size.width * 0.85f
        val topY = size.height * 0.10f
        val bottomY = size.height * 0.90f
        val neckY = size.height * 0.50f
        val neckHalfWidth = size.width * 0.035f
        val innerTopY = topY + size.height * 0.045f
        val innerBottomY = bottomY - size.height * 0.045f
        val innerHalfWidth = (glassRight - glassLeft) / 2f - size.width * 0.045f
        val upperHeight = neckY - innerTopY
        val lowerHeight = innerBottomY - neckY

        val glassPath = Path().apply {
            moveTo(glassLeft, topY)
            lineTo(glassRight, topY)
            cubicTo(
                glassRight,
                size.height * 0.29f,
                centerX + neckHalfWidth,
                neckY,
                centerX + neckHalfWidth,
                neckY
            )
            cubicTo(
                centerX + neckHalfWidth,
                neckY,
                glassRight,
                size.height * 0.71f,
                glassRight,
                bottomY
            )
            lineTo(glassLeft, bottomY)
            cubicTo(
                glassLeft,
                size.height * 0.71f,
                centerX - neckHalfWidth,
                neckY,
                centerX - neckHalfWidth,
                neckY
            )
            cubicTo(
                centerX - neckHalfWidth,
                neckY,
                glassLeft,
                size.height * 0.29f,
                glassLeft,
                topY
            )
            close()
        }

        drawPath(glassPath, glassFill.copy(alpha = 0.18f))

        clipPath(glassPath) {
            if (remaining > 0f) {
                // A triangle's area is width × height / 2. Scaling both dimensions
                // by sqrt(remaining) makes visible sand volume match remaining time.
                val upperScale = sqrt(remaining)
                val surfaceY = neckY - upperHeight * upperScale
                val sandNeckHalfWidth = neckHalfWidth * 0.42f
                val safeUpperHalfWidth = innerHalfWidth * 0.84f
                val surfaceHalfWidth =
                    sandNeckHalfWidth +
                        (safeUpperHalfWidth - sandNeckHalfWidth) * upperScale
                val surfaceDip = size.height * 0.018f * upperScale
                val upperSand = Path().apply {
                    moveTo(centerX - surfaceHalfWidth, surfaceY)
                    cubicTo(
                        centerX - surfaceHalfWidth * 0.45f,
                        surfaceY + surfaceDip,
                        centerX + surfaceHalfWidth * 0.45f,
                        surfaceY + surfaceDip,
                        centerX + surfaceHalfWidth,
                        surfaceY
                    )
                    cubicTo(
                        centerX + surfaceHalfWidth * 0.74f,
                        surfaceY + (neckY - surfaceY) * 0.42f,
                        centerX + sandNeckHalfWidth,
                        neckY - size.height * 0.025f,
                        centerX + sandNeckHalfWidth,
                        neckY
                    )
                    lineTo(centerX - sandNeckHalfWidth, neckY)
                    cubicTo(
                        centerX - sandNeckHalfWidth,
                        neckY - size.height * 0.025f,
                        centerX - surfaceHalfWidth * 0.74f,
                        surfaceY + (neckY - surfaceY) * 0.42f,
                        centerX - surfaceHalfWidth,
                        surfaceY
                    )
                    close()
                }
                drawPath(upperSand, sandBrush)
            }

            val lowerScale = sqrt(transferred)
            val moundHeight = lowerHeight * lowerScale * 0.88f
            val pileTopY = innerBottomY - moundHeight
            val pileHalfWidth = innerHalfWidth * 0.84f * lowerScale

            if (transferred > 0f) {
                // Height and base both grow with sqrt(elapsed), so pile area grows
                // linearly and reaches the neck exactly when the timer reaches zero.
                val lowerSand = Path().apply {
                    moveTo(centerX - pileHalfWidth, innerBottomY)
                    cubicTo(
                        centerX - pileHalfWidth * 0.70f,
                        innerBottomY - moundHeight * 0.18f,
                        centerX - pileHalfWidth * 0.34f,
                        pileTopY + moundHeight * 0.10f,
                        centerX,
                        pileTopY
                    )
                    cubicTo(
                        centerX + pileHalfWidth * 0.34f,
                        pileTopY + moundHeight * 0.10f,
                        centerX + pileHalfWidth * 0.70f,
                        innerBottomY - moundHeight * 0.18f,
                        centerX + pileHalfWidth,
                        innerBottomY
                    )
                    close()
                }
                drawPath(lowerSand, sandBrush)
            }

            if (isRunning && remaining > 0f) {
                val streamEndY = (pileTopY - size.height * 0.012f)
                    .coerceAtLeast(neckY + size.height * 0.018f)
                val fallDistance = streamEndY - neckY
                val grainOffsets =
                    listOf(0f, -0.010f, 0.007f, -0.005f, 0.011f, -0.008f)

                grainOffsets.forEachIndexed { index, horizontalOffset ->
                    val position =
                        (grainPhase + index.toFloat() / grainOffsets.size) % 1f
                    val grainRadius = size.width *
                        if (index % 3 == 0) 0.0065f else 0.0052f
                    drawCircle(
                        color = DriftLilac,
                        radius = grainRadius,
                        center = Offset(
                            centerX + size.width * horizontalOffset,
                            neckY + fallDistance * position
                        ),
                        alpha = 0.58f + (index % 3) * 0.12f
                    )
                }
            }
        }

        drawPath(
            path = glassPath,
            color = frameColor,
            style = Stroke(
                width = size.width * 0.030f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        val railHeight = size.height * 0.075f
        val railCorner = railHeight / 2f
        drawRoundRect(
            color = frameColor,
            topLeft = Offset(railLeft, topY - railHeight / 2f),
            size = androidx.compose.ui.geometry.Size(railRight - railLeft, railHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(railCorner)
        )
        drawRoundRect(
            color = frameColor,
            topLeft = Offset(railLeft, bottomY - railHeight / 2f),
            size = androidx.compose.ui.geometry.Size(railRight - railLeft, railHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(railCorner)
        )
    }
}
