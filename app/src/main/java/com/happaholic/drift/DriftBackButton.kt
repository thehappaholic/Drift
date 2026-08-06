package com.happaholic.drift

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource

@Composable
fun DriftBackButton(
    onClick: () -> Unit,
    contentDescription: String = "Go back"
) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
