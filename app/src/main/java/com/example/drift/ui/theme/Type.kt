package com.example.drift.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.drift.R

private val GoogleSansFlex = FontFamily(
    Font(R.font.google_sans_flex_regular, FontWeight.Normal),
    Font(R.font.google_sans_flex_medium, FontWeight.Medium),
    Font(R.font.google_sans_flex_semibold, FontWeight.SemiBold),
    Font(R.font.google_sans_flex_bold, FontWeight.Bold)
)

val Typography = Typography(
    displaySmall = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 31.sp, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 17.sp, letterSpacing = 0.1.sp),
    labelSmall = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp)
)
