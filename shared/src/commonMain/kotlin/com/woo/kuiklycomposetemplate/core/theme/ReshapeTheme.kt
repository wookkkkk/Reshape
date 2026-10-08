package com.woo.kuiklycomposetemplate.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.tencent.kuikly.compose.ui.graphics.Color

/**
 * Reshape 颜色系统对象，随主题动态注入
 */
data class ReshapeColors(
    val themeId: ReshapeThemeId,
    val primary: Color,
    val primaryDeep: Color,
    val primarySoft: Color,
    val primaryTint: Color,
    val bgCanvas: Color = ColorTokens.bgCanvas,
    val card: Color = ColorTokens.card,
    val divider: Color = ColorTokens.divider,
    val dividerSubtle: Color = ColorTokens.dividerSubtle,
    val textPrimary: Color = ColorTokens.textPrimary,
    val textSecondary: Color = ColorTokens.textSecondary,
    val textFaint: Color = ColorTokens.textFaint,
    val textMuted: Color = ColorTokens.textMuted,
    val textWhite: Color = ColorTokens.textWhite,
    val green: Color = ColorTokens.green,
    val greenSoft: Color = ColorTokens.greenSoft,
    val red: Color = ColorTokens.red,
    val redSoft: Color = ColorTokens.redSoft,
    val prGold: Color = ColorTokens.prGold,
    val prGoldSoft: Color = ColorTokens.prGoldSoft,
    val blueInfo: Color = ColorTokens.blueInfo,
    val blueInfoSoft: Color = ColorTokens.blueInfoSoft,
    val ctaDisabled: Color = ColorTokens.ctaDisabled,
    val ctaDisabledText: Color = ColorTokens.ctaDisabledText,
    // 暗色底专用
    val darkCanvas: Color = ColorTokens.darkCanvas,
    val darkCard: Color = ColorTokens.darkCard,
    val darkElevated: Color = ColorTokens.darkElevated,
    val darkBorder: Color = ColorTokens.darkBorder,
    val darkTextPrimary: Color = ColorTokens.darkTextPrimary
)

fun createReshapeColors(themeId: ReshapeThemeId): ReshapeColors {
    val palette = ColorTokens.getThemePalette(themeId)
    return ReshapeColors(
        themeId = themeId,
        primary = palette.primary,
        primaryDeep = palette.primaryDeep,
        primarySoft = palette.primarySoft,
        primaryTint = palette.primaryTint
    )
}

/**
 * 全局主题状态持有者（单例，支持跨页面即时切换响应）
 */
object ThemeStateHolder {
    var currentThemeId: ReshapeThemeId by mutableStateOf(ReshapeThemeId.ORANGE)

    fun switchTheme(themeId: ReshapeThemeId) {
        currentThemeId = themeId
    }
}

val LocalReshapeColors = staticCompositionLocalOf {
    createReshapeColors(ReshapeThemeId.ORANGE)
}

val LocalReshapeDimensions = staticCompositionLocalOf {
    DimensionTokens
}

/**
 * Reshape 主题访问入口
 */
object ReshapeTheme {
    val colors: ReshapeColors
        @Composable
        get() = LocalReshapeColors.current

    val dimensions: DimensionTokens
        @Composable
        get() = LocalReshapeDimensions.current
}

/**
 * Reshape 主题包装容器
 */
@Composable
fun ReshapeTheme(
    themeId: ReshapeThemeId = ThemeStateHolder.currentThemeId,
    content: @Composable () -> Unit
) {
    val reshapeColors = createReshapeColors(themeId)
    CompositionLocalProvider(
        LocalReshapeColors provides reshapeColors,
        LocalReshapeDimensions provides DimensionTokens
    ) {
        content()
    }
}
