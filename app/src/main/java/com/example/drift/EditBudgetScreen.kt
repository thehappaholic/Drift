package com.example.drift

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
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
import kotlin.math.roundToInt

@Composable
fun EditBudgetScreen(
    onBack: () -> Unit,
    initialInstagram: Int,
    initialYoutube: Int,
    initialBrowser: Int,
    initialAdditionalBudgets: Map<String, Int> = emptyMap(),
    onSave: (Int, Int, Int, Map<String, Int>) -> Unit
) {
    val context = LocalContext.current
    val installedApps = remember(context) {
        loadLaunchableAppNames(context.packageManager)
    }
    var instagram by remember {
        mutableFloatStateOf(initialInstagram.toFloat())
    }

    var youtube by remember {
        mutableFloatStateOf(initialYoutube.toFloat())
    }

    var browser by remember {
        mutableFloatStateOf(initialBrowser.toFloat())
    }
    val addedApps = remember(initialAdditionalBudgets) {
        mutableStateListOf<Pair<String, Float>>().apply {
            initialAdditionalBudgets.toSortedMap(String.CASE_INSENSITIVE_ORDER)
                .forEach { (name, minutes) -> add(name to minutes.toFloat()) }
        }
    }
    var showAddApp by remember { mutableStateOf(false) }
    var appName by remember { mutableStateOf("") }
    val whitelist = remember {
        mutableStateListOf(
            "WhatsApp" to true, "Phone" to true,
            "YouTube Music" to true, "Spotify" to true
        )
    }

    val allocated = instagram.roundToInt() +
            youtube.roundToInt() +
            browser.roundToInt() + addedApps.sumOf { it.second.roundToInt() }

    val flexPool = 180 - allocated
    val budgetValid = allocated <= 180

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            DriftBackButton(onClick = onBack)

            Text(
                text = "Edit Budget",
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 18.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Daily discretionary limit: 180 minutes",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Allocated: $allocated min",
            fontSize = 15.sp,
            color = if (budgetValid) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.error
            }
        )

        Text(
            text = if (budgetValid) {
                "Flex pool: $flexPool min"
            } else {
                "Reduce allocations by ${allocated - 180} min"
            },
            fontSize = 15.sp,
            color = if (budgetValid) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.error
            }
        )

        Spacer(modifier = Modifier.height(30.dp))

        BudgetSlider(
            name = "Instagram",
            value = instagram,
            onValueChange = { instagram = it }
        )

        BudgetSlider(
            name = "YouTube",
            value = youtube,
            onValueChange = { youtube = it }
        )

        BudgetSlider(
            name = "Browser",
            value = browser,
            onValueChange = { browser = it }
        )

        addedApps.forEachIndexed { index, app ->
            BudgetSlider(app.first, app.second) { addedApps[index] = app.first to it }
        }

        Row(
            modifier = Modifier.fillMaxWidth().clickable { showAddApp = true }.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("+", fontSize = 25.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            Text("Add an app", Modifier.padding(start = 10.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            Text(
                text = "Whitelisted apps",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            whitelist.forEachIndexed { index, app ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(app.first, fontSize = 14.sp)
                    Switch(checked = app.second, onCheckedChange = { whitelist[index] = app.first to it })
                }
            }

            Text(
                "Whitelisted apps remain available during focus sessions and do not use your 180-minute discretionary budget.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                onSave(
                    instagram.roundToInt(),
                    youtube.roundToInt(),
                    browser.roundToInt(),
                    addedApps.associate { (name, minutes) -> name to minutes.roundToInt() }
                )
            },
            enabled = budgetValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = "Save Budget",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }

    if (showAddApp) {
        AlertDialog(
            onDismissRequest = { showAddApp = false },
            title = { Text("Add an app") },
            text = {
                Column {
                    OutlinedTextField(
                        value = appName,
                        onValueChange = { appName = it },
                        label = { Text("Search installed apps") },
                        placeholder = { Text("Type an app name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    val reservedNames = setOf("Instagram", "YouTube", "Browser")
                    val availableApps = installedApps.filter { installedName ->
                        installedName.contains(appName.trim(), ignoreCase = true) &&
                            reservedNames.none { it.equals(installedName, ignoreCase = true) } &&
                            addedApps.none { it.first.equals(installedName, ignoreCase = true) }
                    }
                    Text(
                        "Apps on this device",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
                        items(availableApps, key = { it }) { installedName ->
                            Text(
                                installedName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        addedApps.add(installedName to 30f)
                                        appName = ""
                                        showAddApp = false
                                    }
                                    .padding(vertical = 11.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    if (availableApps.isEmpty()) {
                        Text(
                            "No matching installed apps. You can add the typed name manually.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = appName.trim().isNotEmpty(),
                    onClick = {
                        val cleanName = appName.trim()
                        val reservedNames = setOf("Instagram", "YouTube", "Browser")
                        if (
                            reservedNames.none { it.equals(cleanName, true) } &&
                            addedApps.none { it.first.equals(cleanName, true) }
                        ) {
                            addedApps.add(cleanName to 30f)
                        }
                        appName = ""
                        showAddApp = false
                    }
                ) { Text("Add manually") }
            },
            dismissButton = { TextButton(onClick = { showAddApp = false }) { Text("Cancel") } }
        )
    }
}

private fun loadLaunchableAppNames(packageManager: PackageManager): List<String> {
    val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return packageManager.queryIntentActivities(launchIntent, PackageManager.MATCH_ALL)
        .map { it.loadLabel(packageManager).toString().trim() }
        .filter(String::isNotEmpty)
        .distinctBy(String::lowercase)
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
}

@Composable
private fun BudgetSlider(
    name: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "${value.roundToInt()} min",
                fontSize = 15.sp
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..120f,
            steps = 23
        )
    }
}
