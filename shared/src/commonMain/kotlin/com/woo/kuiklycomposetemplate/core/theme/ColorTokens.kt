package com.woo.kuiklycomposetemplate.core.theme

import com.tencent.kuikly.compose.ui.graphics.Color

/**
 * Reshape 主题标识枚举（支持 6 种专业运动主题）
 */
enum class ReshapeThemeId(val label: String, val desc: String) {
    ORANGE("活力力量橙", "泵感与力量 · 默认"),
    BLUE("深海曜蓝", "严谨硬核 · 专注"),
    CYAN("极光电青", "运动科技 · 破风"),
    EMERALD("翡翠高能绿", "爆发力与健康"),
    GOLD("黑曜琥珀金", "黑金低调 · 冠军"),
    PURPLE("经典紫罗兰", "原版经典 · 质感")
}

/**
 * 主题配色结构体
 */
data class ReshapeThemePalette(
    val id: ReshapeThemeId,
    val primary: Color,
    val primaryDeep: Color,
    val primarySoft: Color,
    val primaryTint: Color
)

/**
 * 全局颜色令牌定义（严格对齐 design/tokens.css 与 PRD 规范）
 */
object ColorTokens {
    // 基础背景色
    val bgCanvas = Color(0xFFF0F1F7)          // 页面全局主底色
    val card = Color(0xFFFFFFFF)              // 卡片纯白背景
    val divider = Color(0xFFEDEEF4)           // 浅色分割线与边框
    val dividerSubtle = Color(0xFFE2E4ED)     // 次级分割线

    // 暗色底/战报卡/暗黑模式专用
    val darkCanvas = Color(0xFF0D0F12)        // 纯黑背景
    val darkCard = Color(0xFF14171D)          // 极深暗黑卡片
    val darkElevated = Color(0xFF1A1E26)      // 悬浮暗黑卡片
    val darkBorder = Color(0xFF252A36)        // 暗黑边框线
    val darkTextPrimary = Color(0xFFF0F2F5)   // 暗黑高亮字

    // 基础文本色
    val textPrimary = Color(0xFF1E2033)       // 90% 深度主文本
    val textSecondary = Color(0xFF6A6D82)     // 60% 深度次级说明文本
    val textFaint = Color(0xFF9A9CAE)         // 40% 深度弱提示文本
    val textMuted = Color(0xFFB4B7C9)         // 占位/禁用文本
    val textWhite = Color(0xFFFFFFFF)         // 纯白文字

    // 状态语义色
    val green = Color(0xFF3EA97A)             // 恢复/达标/成功绿
    val greenSoft = Color(0xFFD1FAE5)         // 浅绿背景
    val red = Color(0xFFEF4444)               // 危险/力竭/删除红
    val redSoft = Color(0xFFFEE2E2)           // 浅红背景
    val prGold = Color(0xFFE5A93C)            // NEW PR 突破暗金
    val prGoldSoft = Color(0xFFFEF3C7)        // PR 金浅底
    val blueInfo = Color(0xFF3B82F6)          // 信息/提示蓝
    val blueInfoSoft = Color(0xFFEFF6FF)      // 信息蓝浅底

    // 控件状态
    val ctaDisabled = Color(0xFFC7C9DE)       // 禁用状态按键色
    val ctaDisabledText = Color(0xFF8E92A8)   // 禁用文本色

    // 6 大专业主题预置
    val ThemeOrange = ReshapeThemePalette(
        id = ReshapeThemeId.ORANGE,
        primary = Color(0xFFFF5722),
        primaryDeep = Color(0xFFE64A19),
        primarySoft = Color(0xFFFFEBE5),
        primaryTint = Color(0xFFFFF6F3)
    )

    val ThemeBlue = ReshapeThemePalette(
        id = ReshapeThemeId.BLUE,
        primary = Color(0xFF2563EB),
        primaryDeep = Color(0xFF1D4ED8),
        primarySoft = Color(0xFFEFF6FF),
        primaryTint = Color(0xFFF8FAFF)
    )

    val ThemeCyan = ReshapeThemePalette(
        id = ReshapeThemeId.CYAN,
        primary = Color(0xFF0284C7),
        primaryDeep = Color(0xFF0369A1),
        primarySoft = Color(0xFFE0F2FE),
        primaryTint = Color(0xFFF0F9FF)
    )

    val ThemeEmerald = ReshapeThemePalette(
        id = ReshapeThemeId.EMERALD,
        primary = Color(0xFF059669),
        primaryDeep = Color(0xFF047857),
        primarySoft = Color(0xFFD1FAE5),
        primaryTint = Color(0xFFECFDF5)
    )

    val ThemeGold = ReshapeThemePalette(
        id = ReshapeThemeId.GOLD,
        primary = Color(0xFFD97706),
        primaryDeep = Color(0xFFB45309),
        primarySoft = Color(0xFFFEF3C7),
        primaryTint = Color(0xFFFFFBEB)
    )

    val ThemePurple = ReshapeThemePalette(
        id = ReshapeThemeId.PURPLE,
        primary = Color(0xFF5B5FC7),
        primaryDeep = Color(0xFF3F43A8),
        primarySoft = Color(0xFFE9EAF7),
        primaryTint = Color(0xFFF5F5FC)
    )

    fun getThemePalette(themeId: ReshapeThemeId): ReshapeThemePalette {
        return when (themeId) {
            ReshapeThemeId.ORANGE -> ThemeOrange
            ReshapeThemeId.BLUE -> ThemeBlue
            ReshapeThemeId.CYAN -> ThemeCyan
            ReshapeThemeId.EMERALD -> ThemeEmerald
            ReshapeThemeId.GOLD -> ThemeGold
            ReshapeThemeId.PURPLE -> ThemePurple
        }
    }
}
