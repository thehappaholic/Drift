package com.example.drift

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.drift.data.profile.OnboardingDraft

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onEditAbout: () -> Unit,
    onEditRole: () -> Unit,
    onEditManaging: () -> Unit,
    onEditSchedule: () -> Unit,
    onEditWindDown: () -> Unit,
    onLogout: () -> Unit,
    name: String,
    email: String,
    onboarding: OnboardingDraft
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .height(52.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text("Log Out", fontWeight = FontWeight.SemiBold) }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DriftBackButton(onClick = onBack)
                Text(
                    "Profile",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 18.dp)
                )
            }
            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .padding(13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.ic_profile),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(name.ifBlank { "Drift user" }, style = MaterialTheme.typography.titleMedium)
                    Text(
                        email.ifBlank { "Email unavailable" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("Your profile", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                        RoundedCornerShape(20.dp)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 18.dp)
            ) {
                ProfileSection(
                    "About you",
                    listOf(onboarding.ageRange, onboarding.gender)
                        .filter { it.isNotBlank() }.joinToString(" · ")
                        .ifBlank { "Optional details not added" },
                    onboarding.ageRange.isNotBlank() || onboarding.gender.isNotBlank(),
                    onEditAbout
                )
                ProfileSection(
                    "What describes you",
                    if (onboarding.role == "Other") onboarding.otherRole.ifBlank { "Other" }
                    else onboarding.role.ifBlank { "Not set" },
                    onboarding.role.isNotBlank(),
                    onEditRole
                )
                ProfileSection(
                    "What you're managing",
                    onboarding.managing.joinToString().ifBlank { "Not set" },
                    onboarding.managing.isNotEmpty(),
                    onEditManaging
                )
                ProfileScheduleSection(onboarding, onEditSchedule)
                ProfileWindDownSection(onboarding, onEditWindDown)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ProfileScheduleSection(
    onboarding: OnboardingDraft,
    onClick: () -> Unit
) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompletionCircle(onboarding.studyDays.isNotEmpty())
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Work / study schedule",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${onboarding.studyStart} – ${onboarding.studyEnd}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                days.forEach { day ->
                    val selected = day in onboarding.studyDays
                    Box(
                        modifier = Modifier.size(25.dp).background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface,
                            CircleShape
                        ).border(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                            CircleShape
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            day.first().toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        DriftNextIcon(Modifier.size(18.dp), "Edit work / study schedule")
    }
}

@Composable
private fun ProfileWindDownSection(
    onboarding: OnboardingDraft,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompletionCircle(true)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Wind-down",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (onboarding.windDownEnabled) {
                    "${onboarding.windDownStart} – ${onboarding.windDownEnd}"
                } else "Disabled",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Switch(
            checked = onboarding.windDownEnabled,
            onCheckedChange = null
        )
        Spacer(Modifier.width(6.dp))
        DriftNextIcon(Modifier.size(18.dp), "Edit wind-down")
    }
}

@Composable
private fun CompletionCircle(completed: Boolean) {
    Box(
        modifier = Modifier.size(26.dp).background(
            if (completed) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surface,
            CircleShape
        ).border(
            1.dp,
            if (completed) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline,
            CircleShape
        ),
        contentAlignment = Alignment.Center
    ) {
        if (completed) {
            Icon(
                painter = painterResource(R.drawable.ic_completed_check),
                contentDescription = "Completed",
                tint = androidx.compose.ui.graphics.Color.Unspecified,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ProfileSection(
    title: String,
    value: String,
    completed: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompletionCircle(completed)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        DriftNextIcon(
            modifier = Modifier.size(18.dp),
            contentDescription = "Edit $title"
        )
    }
}
