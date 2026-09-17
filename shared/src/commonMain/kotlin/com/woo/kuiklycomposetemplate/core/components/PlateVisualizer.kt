package com.woo.kuiklycomposetemplate.core.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.woo.kuiklycomposetemplate.core.theme.ReshapeTheme
import com.woo.kuiklycomposetemplate.domain.algorithm.PlateLoaderEngine

/**
 * 彩色奥赛杠铃片装配示意弹窗 (Plate Visualizer)
 */
@Composable
fun PlateVisualizer(
    visible: Boolean,
    totalWeightKg: Float,
    barWeightKg: Float = 20.0f,
    targetSetName: String = "下一组",
    onDismiss: () -> Unit
) {
    if (!visible) return

    // 支持在弹窗内动态切换杠铃杆重量与商业铁馆片库
    var currentBarWeight by remember(barWeightKg) { mutableStateOf(barWeightKg) }
    var useCommercialOnly by remember { mutableStateOf(false) }

    val activePlates = if (useCommercialOnly) PlateLoaderEngine.COMMERCIAL_PLATES else PlateLoaderEngine.STANDARD_PLATES
    val singleSidePlates = PlateLoaderEngine.computeSingleSidePlates(totalWeightKg, currentBarWeight, activePlates)
    val singleSideTotal = if (totalWeightKg > currentBarWeight) (totalWeightKg - currentBarWeight) / 2.0f else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .background(ReshapeTheme.colors.card, RoundedCornerShape(20.dp))
                .clickable(enabled = false) {}
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🧮 杠铃挂片免口算指南",
                        fontSize = 17.sp,
                        color = ReshapeTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "「$targetSetName」做组目标 $totalWeightKg kg",
                        fontSize = 12.sp,
                        color = ReshapeTheme.colors.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "✕",
                    fontSize = 18.sp,
                    color = ReshapeTheme.colors.textFaint,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 杠铃杆重切换胶囊
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "杆重:",
                    fontSize = 12.sp,
                    color = ReshapeTheme.colors.textSecondary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                val bars = listOf(
                    20.0f to "20kg (标准杆)",
                    15.0f to "15kg (女/短杆)",
                    0.0f to "0kg (史密斯)"
                )
                bars.forEach { (w, label) ->
                    val isSelected = currentBarWeight == w
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) ReshapeTheme.colors.primary else ReshapeTheme.colors.bgCanvas,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { currentBarWeight = w }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            color = if (isSelected) Color.White else ReshapeTheme.colors.textSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 重量总览看板
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "总目标", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "${totalWeightKg}kg", fontSize = 18.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "底杆", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "${currentBarWeight.toInt()}kg", fontSize = 16.sp, color = ReshapeTheme.colors.textSecondary, fontWeight = FontWeight.SemiBold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "单侧各挂", fontSize = 11.sp, color = ReshapeTheme.colors.primary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "${singleSideTotal}kg", fontSize = 18.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 杠铃单侧片数示意条 (由内到外)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "单侧装配 (内 ➔ 外):",
                    fontSize = 12.sp,
                    color = ReshapeTheme.colors.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (useCommercialOnly) "🏢 商业铁馆模式" else "🏆 标准奥赛模式",
                    fontSize = 11.sp,
                    color = ReshapeTheme.colors.primary,
                    modifier = Modifier
                        .clickable { useCommercialOnly = !useCommercialOnly }
                        .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (singleSidePlates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (totalWeightKg <= currentBarWeight) "空杆即可，无需额外挂片" else "重量不足或无法精确配平",
                        fontSize = 13.sp,
                        color = ReshapeTheme.colors.textSecondary
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(Color(0xFF1E2033), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 杠铃杆头 (左侧)
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(10.dp)
                            .background(Color(0xFF9E9E9E), RoundedCornerShape(2.dp))
                    )
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(44.dp)
                            .background(Color(0xFF616161), RoundedCornerShape(2.dp))
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // 挂片队列
                    singleSidePlates.forEach { plate ->
                        val plateHeight = (72f * plate.diameterRatio).dp
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .height(plateHeight)
                                    .background(plate.color, RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = plate.label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 底部关闭按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(ReshapeTheme.colors.primary, RoundedCornerShape(12.dp))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "我知道了，去装片", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
