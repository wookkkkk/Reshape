package com.woo.kuiklycomposetemplate.core.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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

/**
 * 组间休息时长修改抽屉 (支持数字微调、肌力学科学预设与持久化计划保存)
 */
@Composable
fun RestDurationDrawer(
    visible: Boolean,
    exerciseName: String,
    currentSeconds: Int,
    onDismiss: () -> Unit,
    onSelectDuration: (selectedSeconds: Int, saveToPlan: Boolean) -> Unit
) {
    if (!visible) return

    var selectedSeconds by remember(currentSeconds, visible) { mutableStateOf(currentSeconds) }
    var saveToPlan by remember(visible) { mutableStateOf(true) }

    val presets = listOf(
        30 to "30 秒 · 超级组/极速充血",
        45 to "45 秒 · 孤立小肌群",
        60 to "60 秒 · 代谢力竭",
        90 to "90 秒 · 双重递进 (推荐)",
        120 to "2 分钟 · 经典复合项",
        180 to "3 分钟 · 三大项波浪",
        240 to "4 分钟 · 极限大负荷",
        300 to "5 分钟 · 绝对神经恢复"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = ReshapeTheme.colors.card,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            // 顶部拖拽指示器
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color(0xFFD1D5DB), RoundedCornerShape(2.dp))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(14.dp))

            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "⏱️ 设置组间休息时长",
                        fontSize = 18.sp,
                        color = ReshapeTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "当前动作：$exerciseName",
                        fontSize = 12.sp,
                        color = ReshapeTheme.colors.textSecondary
                    )
                }
                Text(
                    text = "取消",
                    fontSize = 15.sp,
                    color = ReshapeTheme.colors.textFaint,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 大数字步进微调器
            val minPart = selectedSeconds / 60
            val secPart = selectedSeconds % 60
            val formattedTime = if (minPart > 0 && secPart > 0) {
                "${minPart}分 ${secPart}秒"
            } else if (minPart > 0) {
                "${minPart} 分钟"
            } else {
                "${secPart} 秒"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // -30s
                    Box(
                        modifier = Modifier
                            .background(ReshapeTheme.colors.card, RoundedCornerShape(8.dp))
                            .clickable {
                                selectedSeconds = (selectedSeconds - 30).coerceAtLeast(15)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(text = "-30s", fontSize = 12.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    // -15s
                    Box(
                        modifier = Modifier
                            .background(ReshapeTheme.colors.card, RoundedCornerShape(8.dp))
                            .clickable {
                                selectedSeconds = (selectedSeconds - 15).coerceAtLeast(15)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(text = "-15s", fontSize = 12.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.Bold)
                    }

                    // 中间大时间展示
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = formattedTime,
                            fontSize = 22.sp,
                            color = ReshapeTheme.colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "(${selectedSeconds} 秒)",
                            fontSize = 11.sp,
                            color = ReshapeTheme.colors.textFaint
                        )
                    }

                    // +15s
                    Box(
                        modifier = Modifier
                            .background(ReshapeTheme.colors.card, RoundedCornerShape(8.dp))
                            .clickable {
                                selectedSeconds = (selectedSeconds + 15).coerceAtMost(600)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(text = "+15s", fontSize = 12.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    // +30s
                    Box(
                        modifier = Modifier
                            .background(ReshapeTheme.colors.card, RoundedCornerShape(8.dp))
                            .clickable {
                                selectedSeconds = (selectedSeconds + 30).coerceAtMost(600)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(text = "+30s", fontSize = 12.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 科学预设快捷卡片
            Text(
                text = "科学预设推荐",
                fontSize = 13.sp,
                color = ReshapeTheme.colors.textSecondary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            presets.chunked(2).forEach { rowPresets ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    rowPresets.forEachIndexed { idx, (sec, label) ->
                        if (idx > 0) Spacer(modifier = Modifier.width(8.dp))
                        val isSelected = sec == selectedSeconds
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) ReshapeTheme.colors.primarySoft else ReshapeTheme.colors.bgCanvas,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedSeconds = sec }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    color = if (isSelected) ReshapeTheme.colors.primary else ReshapeTheme.colors.textPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Text(
                                        text = "✓",
                                        fontSize = 13.sp,
                                        color = ReshapeTheme.colors.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 同步保存为计划默认设置勾选项
            key(saveToPlan) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(10.dp))
                        .clickable { saveToPlan = !saveToPlan }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (saveToPlan) "☑️" else "⬜",
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "同步保存为「${exerciseName}」计划默认休息时长",
                            fontSize = 13.sp,
                            color = ReshapeTheme.colors.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (saveToPlan) "已启用：未来该动作将默认使用 ${selectedSeconds} 秒" else "未勾选：仅对本次训练生效",
                            fontSize = 11.sp,
                            color = if (saveToPlan) ReshapeTheme.colors.primary else ReshapeTheme.colors.textFaint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 确定应用主 CTA 按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(ReshapeTheme.colors.primary, RoundedCornerShape(14.dp))
                    .clickable {
                        onSelectDuration(selectedSeconds, saveToPlan)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "应用此休息时长 ($formattedTime)",
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
