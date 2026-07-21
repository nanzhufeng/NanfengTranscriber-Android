package com.nanzhufeng.transcriber.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ActionOrange = Color(0xFFFF5A1F)
val TranscriptPreviewOrange = Color(0xFF9B6049)
val ParameterPurple = Color(0xFF7552B8)
val AttentionOchre = Color(0xFFB96812)
val WorkbenchBorder = Color(0xFFD7DED9)
val SecondaryText = Color(0xFF5F6C64)
val FailureRed = Color(0xFFC8161D)

private data class NanfengSkinPalette(
    val background: Color,
    val surface: Color,
    val deepSurface: Color,
    val primary: Color,
    val secondary: Color,
    val onBackground: Color,
    val onSurfaceVariant: Color,
    val primaryContainer: Color,
)

data class NanfengSkinOption(val id: String, val displayName: String)

object NanfengSkinCatalog {
    val options = listOf(
        NanfengSkinOption("charcoal_relief", "玄岩浮影"),
        NanfengSkinOption("forest_maple", "青林木语"),
        NanfengSkinOption("burgundy_frost", "枫绯凝霜"),
        NanfengSkinOption("burgundy_ribbon", "绛绡叠韵"),
        NanfengSkinOption("brown_contour", "铜棕弦影"),
        NanfengSkinOption("jade_satin", "青玉流岚"),
    )

    fun nameOf(id: String): String = options.firstOrNull { it.id == id }?.displayName
        ?: options.first { it.id == "forest_maple" }.displayName
}

private val SkinPalettes = mapOf(
    "charcoal_relief" to NanfengSkinPalette(
        Color(0xFFF5F4F1), Color(0xFFFFFEFC), Color(0xFF2B2522), Color(0xFF6ECC54),
        Color(0xFFEB5C20), Color(0xFF261F1C), Color(0xFF6F625B), Color(0xFFE8F4E3),
    ),
    "forest_maple" to NanfengSkinPalette(
        Color(0xFFEBF3E9), Color(0xFFFFFEFB), Color(0xFF1F4D3B), Color(0xFF2F6A4A),
        Color(0xFFD87832), Color(0xFF203329), Color(0xFF607068), Color(0xFFE2F0E5),
    ),
    "burgundy_frost" to NanfengSkinPalette(
        Color(0xFFF7F2F3), Color(0xFFFFFEFC), Color(0xFF470125), Color(0xFF982A54),
        Color(0xFFEB5C20), Color(0xFF351520), Color(0xFF755865), Color(0xFFF5E4EA),
    ),
    "burgundy_ribbon" to NanfengSkinPalette(
        Color(0xFFF7F2F3), Color(0xFFFFFEFC), Color(0xFF35001C), Color(0xFF7D153E),
        Color(0xFFD6A42A), Color(0xFF32131E), Color(0xFF70525F), Color(0xFFF3E2E9),
    ),
    "brown_contour" to NanfengSkinPalette(
        Color(0xFFF5F2EF), Color(0xFFFFFEFC), Color(0xFF4A2D22), Color(0xFF018B8D),
        Color(0xFFC77A3F), Color(0xFF30221D), Color(0xFF6D5A52), Color(0xFFDDF2F1),
    ),
    "jade_satin" to NanfengSkinPalette(
        Color(0xFFEAF3F0), Color(0xFFFEFFFE), Color(0xFF234B47), Color(0xFF018B8D),
        Color(0xFFEB5C20), Color(0xFF203330), Color(0xFF5D6F6A), Color(0xFFDDF2EE),
    ),
)

private fun workbenchColors(skinId: String) = (SkinPalettes[skinId] ?: SkinPalettes.getValue("forest_maple")).let { palette ->
    lightColorScheme(
    primary = palette.primary,
    onPrimary = Color.White,
    primaryContainer = palette.primaryContainer,
    onPrimaryContainer = palette.onBackground,
    secondary = palette.secondary,
    onSecondary = Color.White,
    tertiary = ParameterPurple,
    onTertiary = Color.White,
    background = palette.background,
    onBackground = palette.onBackground,
    surface = palette.surface,
    surfaceTint = palette.surface,
    onSurface = palette.onBackground,
    surfaceVariant = palette.primaryContainer,
    onSurfaceVariant = palette.onSurfaceVariant,
    outline = palette.onSurfaceVariant.copy(alpha = 0.24f).compositeOver(palette.surface),
    error = FailureRed,
)
}

private val WorkbenchTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp),
)

@Composable
fun NanfengTranscriberTheme(
    skinId: String = "forest_maple",
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = workbenchColors(skinId),
        typography = WorkbenchTypography,
        shapes = MaterialTheme.shapes.copy(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(20.dp),
            extraLarge = RoundedCornerShape(20.dp),
        ),
        content = content,
    )
}
