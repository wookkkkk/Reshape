package com.woo.kuiklycomposetemplate.core.components

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.woo.kuiklycomposetemplate.core.theme.ReshapeTheme

/**
 * 常驻悬浮休息仓 (Floating Rest Dock)
 * 吸底常驻，支持 ±15s 微调与立即跳过，伴随进度条指示
 */
@Composable
fun FloatingRestDock(
    isActive: Boolean,
    remainingSeconds: Int,
    totalSeconds: Int,
    restTitle: String = "⏳ 组间休息",
    onAdjustTime: (deltaSeconds: Int) -> Unit,
    onSkip: () -> Unit,
    onClickDetail: () -> Unit = {}
) {
    if (!isActive && remainingSeconds <= 0) return

    val progress = if (totalSeconds > 0) {
        (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeStr = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(
                color = ReshapeTheme.colors.darkElevated,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClickDetail() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 顶部进度条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color(0xFF2D3748), RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .background(ReshapeTheme.colors.primary, RoundedCornerShape(2.dp))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧倒计时大字
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = restTitle,
                            fontSize = 12.sp,
                            color = ReshapeTheme.colors.textFaint
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = timeStr,
                        fontSize = 26.sp,
                        color = Color.White
                    )
                }

                // 右侧微调按钮群 (-15s, +30s, 跳过)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2A2E3D), RoundedCornerShape(8.dp))
                            .clickable { onAdjustTime(-15) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "-15s", fontSize = 12.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .background(Color(0xFF374151), RoundedCornerShape(8.dp))
                            .clickable { onAdjustTime(30) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+30s",
                            fontSize = 13.sp,
                            fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.Bold,
                            color = Color(0xFFFBBF24)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(
                        modifier = Modifier
                            .background(ReshapeTheme.colors.primary, RoundedCornerShape(8.dp))
                            .clickable { onSkip() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "跳过",
                            fontSize = 13.sp,
                            fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
