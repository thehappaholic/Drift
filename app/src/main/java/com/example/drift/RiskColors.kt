package com.example.drift

import androidx.compose.ui.graphics.Color

data class RiskPalette(val background: Color, val foreground: Color)

fun riskPalette(level: String): RiskPalette = when (level.lowercase()) {
    "low" -> RiskPalette(Color(0xFFDDEEDF), Color(0xFF315E3A))
    "medium", "moderate" -> RiskPalette(Color(0xFFFFE7C2), Color(0xFF80510A))
    "high" -> RiskPalette(Color(0xFFF7D6D3), Color(0xFF8A302B))
    else -> RiskPalette(Color(0xFFE8E7E2), Color(0xFF55544F))
}
