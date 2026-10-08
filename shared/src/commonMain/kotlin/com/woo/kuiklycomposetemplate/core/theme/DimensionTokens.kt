package com.woo.kuiklycomposetemplate.core.theme

import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.TextUnit
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp

/**
 * 全局尺寸与间距令牌定义（对齐 design/tokens.css 与 screens.css 规范）
 */
object DimensionTokens {
    // 间距 Tokens (对应 Spacer / padding)
    val space2: Dp = 2.dp
    val space3: Dp = 3.dp
    val space4: Dp = 4.dp
    val space6: Dp = 6.dp
    val space8: Dp = 8.dp
    val space10: Dp = 10.dp
    val space12: Dp = 12.dp
    val space14: Dp = 14.dp
    val space16: Dp = 16.dp
    val space18: Dp = 18.dp
    val space20: Dp = 20.dp
    val space24: Dp = 24.dp
    val space32: Dp = 32.dp
    val space48: Dp = 48.dp

    // 圆角 Tokens
    val radius4: Dp = 4.dp
    val radius7: Dp = 7.dp
    val radius8: Dp = 8.dp
    val radius10: Dp = 10.dp
    val radius12: Dp = 12.dp
    val radius14: Dp = 14.dp
    val radius15: Dp = 15.dp
    val radius16: Dp = 16.dp
    val radius20: Dp = 20.dp
    val radius24: Dp = 24.dp
    val radiusPill: Dp = 999.dp

    // 控件标准尺寸
    val statusBarHeight: Dp = 44.dp
    val navBarHeight: Dp = 46.dp
    val bottomCtaHeight: Dp = 54.dp
    val numPadKeyHeight: Dp = 58.dp
    val floatingDockHeight: Dp = 64.dp
    val bottomScrollPadding: Dp = 140.dp  // 列表底部垫高，避开吸底悬浮仓

    // 字体字号 Tokens
    val font10: TextUnit = 10.sp
    val font11: TextUnit = 11.sp
    val font12: TextUnit = 12.sp
    val font13: TextUnit = 13.sp
    val font14: TextUnit = 14.sp
    val font15: TextUnit = 15.sp
    val font16: TextUnit = 16.sp
    val font17: TextUnit = 17.sp
    val font18: TextUnit = 18.sp
    val font20: TextUnit = 20.sp
    val font24: TextUnit = 24.sp
    val font28: TextUnit = 28.sp
    val font32: TextUnit = 32.sp
    val font38: TextUnit = 38.sp
    val font48: TextUnit = 48.sp
}
