package com.happaholic.drift

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

@Composable
fun DriftNextIcon(
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    Icon(
        painter = painterResource(R.drawable.ic_chevron_right),
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}
