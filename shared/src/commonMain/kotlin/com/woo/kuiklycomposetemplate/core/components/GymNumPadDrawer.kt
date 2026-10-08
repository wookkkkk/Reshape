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
import com.tencent.kuikly.compose.foundation.layout.fillMaxHeight
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
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.ui.zIndex
import com.woo.kuiklycomposetemplate.core.theme.ReshapeTheme

enum class NumPadInputMode(
    val title: String,
    val unit: String,
    val isDecimalAllowed: Boolean = true,
    val confirmText: String = "确 认"
) {
    WEIGHT("做组重量", "kg", isDecimalAllowed = true, confirmText = "确 认 输 入"),
    REPS("每组次数", "次", isDecimalAllowed = false, confirmText = "确 认 次 数"),
    SETS("计划组数", "组", isDecimalAllowed = false, confirmText = "确 认 组 数"),
    REST_SECONDS("组间休息时长", "秒", isDecimalAllowed = false, confirmText = "确 认 休 息 秒 数"),
    BENCH_1RM("卧推 1RM 基准极限", "kg", isDecimalAllowed = true, confirmText = "确 认 卧 推 1RM"),
    SQUAT_1RM("深蹲 1RM 基准极限", "kg", isDecimalAllowed = true, confirmText = "确 认 深 蹲 1RM"),
    DEADLIFT_1RM("硬拉 1RM 基准极限", "kg", isDecimalAllowed = true, confirmText = "确 认 硬 拉 1RM"),
    BODY_WEIGHT("当前体重", "kg", isDecimalAllowed = true, confirmText = "确 认 体 重"),
    TARGET_WEIGHT("目标体重", "kg", isDecimalAllowed = true, confirmText = "确 认 目 标 体 重"),
    HEIGHT("身高", "cm", isDecimalAllowed = false, confirmText = "确 认 身 高"),
    TRAINING_EXP("力量训练经验年限", "年", isDecimalAllowed = true, confirmText = "确 认 训 练 年 限"),
    BODY_FAT("体脂率", "%", isDecimalAllowed = true, confirmText = "确 认 体 脂 率"),
    SKELETAL_MUSCLE("骨骼肌重", "kg", isDecimalAllowed = true, confirmText = "确 认 骨 骼 肌 重")
}

/**
 * 健身房实战 56dp 沉浸式 9 宫格全宽大按键抽屉
 * 彻底杜绝系统软键盘在 H5 / 跨端被拦截、遮挡或挤压视口的问题，提供超大热区与快捷加减微调键
 */
@Composable
fun GymNumPadDrawer(
    visible: Boolean,
    mode: NumPadInputMode,
    initialValue: String,
    lastHistoryValue: Float? = null,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    if (!visible) return

    val cleanInitial = initialValue.filter { it.isDigit() || it == '.' }
    var currentValueStr by remember(initialValue, visible) { mutableStateOf(cleanInitial) }

    // 背景半透明遮罩 + 底部弹层 (zIndex 999f 强制提升为整个页面最高层叠上下文，100% 杜绝底层穿透与重影)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(999f)
            .background(Color(0x88000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1000f)
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .clickable { /* 阻止点击穿透到遮罩与底层 */ }
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶栏拖拽指示条与标题
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color(0xFFD1D5DB), RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mode.title,
                    fontSize = 17.sp,
                    fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.Bold,
                    color = ReshapeTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (lastHistoryValue != null && lastHistoryValue > 0f) {
                    val histText = if (mode.isDecimalAllowed) "${lastHistoryValue}${mode.unit}" else "${lastHistoryValue.toInt()}${mode.unit}"
                    Text(
                        text = "上次: $histText",
                        fontSize = 13.sp,
                        color = ReshapeTheme.colors.textSecondary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
                Text(
                    text = "取消",
                    fontSize = 15.sp,
                    color = ReshapeTheme.colors.textFaint,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 当前输入大字展示区域 + 快速清空按键 (实心背景 + key 彻底根除重影)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1001f)
                    .background(Color(0xFFF3F4F6), RoundedCornerShape(14.dp))
                    .padding(vertical = 12.dp, horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                key(currentValueStr, mode) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val displayDigits = currentValueStr.filter { it.isDigit() || it == '.' }
                        Text(
                            text = if (displayDigits.isEmpty()) "0" else displayDigits,
                            fontSize = 38.sp,
                            fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.Bold,
                            color = Color(0xFF1E2033)
                        )
                        Text(
                            text = " " + mode.unit,
                            fontSize = 17.sp,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                // 快速清空按钮 (一键清空三位数，无需连续点退格)
                if (currentValueStr.isNotEmpty() && currentValueStr != "0") {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFEE2E2), RoundedCornerShape(8.dp))
                            .clickable { currentValueStr = "" }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕ 清空",
                            fontSize = 13.sp,
                            fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 快捷增量与快填操作条 (根据模式智能适配)
            when (mode) {
                NumPadInputMode.WEIGHT, NumPadInputMode.BENCH_1RM, NumPadInputMode.SQUAT_1RM, NumPadInputMode.DEADLIFT_1RM -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val quickButtons = mutableListOf<String>()
                        if (lastHistoryValue != null && lastHistoryValue > 0f) {
                            quickButtons.add("上次")
                        }
                        quickButtons.addAll(listOf("+2.5", "-2.5", "+5", "自重"))

                        quickButtons.forEachIndexed { index, label ->
                            if (index > 0) Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .background(
                                        if (label == "上次") ReshapeTheme.colors.primary else ReshapeTheme.colors.primarySoft,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        when (label) {
                                            "上次" -> {
                                                if (lastHistoryValue != null) {
                                                    currentValueStr = if (lastHistoryValue % 1.0f == 0f) lastHistoryValue.toInt().toString() else lastHistoryValue.toString()
                                                }
                                            }
                                            "自重" -> {
                                                currentValueStr = "0"
                                            }
                                            else -> {
                                                val delta = label.toFloatOrNull() ?: 0f
                                                val cur = currentValueStr.toFloatOrNull() ?: 0f
                                                val nextVal = (cur + delta).coerceAtLeast(0f)
                                                currentValueStr = if (nextVal % 1.0f == 0f) nextVal.toInt().toString() else nextVal.toString()
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.SemiBold,
                                    color = if (label == "上次") Color.White else ReshapeTheme.colors.primary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                NumPadInputMode.BODY_WEIGHT, NumPadInputMode.TARGET_WEIGHT, NumPadInputMode.BODY_FAT, NumPadInputMode.SKELETAL_MUSCLE -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val quickButtons = listOf("+0.5", "-0.5", "+1.0", "-1.0")
                        quickButtons.forEachIndexed { index, label ->
                            if (index > 0) Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(8.dp))
                                    .clickable {
                                        val delta = label.toFloatOrNull() ?: 0f
                                        val cur = currentValueStr.toFloatOrNull() ?: 0f
                                        val nextVal = (cur + delta).coerceAtLeast(0f)
                                        currentValueStr = if (nextVal % 1.0f == 0f) nextVal.toInt().toString() else (kotlin.math.round(nextVal * 10f) / 10f).toString()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.SemiBold,
                                    color = ReshapeTheme.colors.primary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                NumPadInputMode.HEIGHT -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val quickButtons = listOf("+1cm", "-1cm", "+5cm", "-5cm")
                        quickButtons.forEachIndexed { index, label ->
                            if (index > 0) Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(8.dp))
                                    .clickable {
                                        val delta = label.replace("cm", "").toIntOrNull() ?: 0
                                        val cur = currentValueStr.toIntOrNull() ?: 170
                                        val nextVal = (cur + delta).coerceIn(100, 250)
                                        currentValueStr = nextVal.toString()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.SemiBold,
                                    color = ReshapeTheme.colors.primary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                NumPadInputMode.TRAINING_EXP -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val quickButtons = listOf("+0.5年", "-0.5年", "+1年", "3年", "5年")
                        quickButtons.forEachIndexed { index, label ->
                            if (index > 0) Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(8.dp))
                                    .clickable {
                                        when (label) {
                                            "+0.5年" -> {
                                                val cur = currentValueStr.toFloatOrNull() ?: 0f
                                                currentValueStr = (cur + 0.5f).toString()
                                            }
                                            "-0.5年" -> {
                                                val cur = currentValueStr.toFloatOrNull() ?: 0f
                                                currentValueStr = maxOf(0f, cur - 0.5f).toString()
                                            }
                                            "+1年" -> {
                                                val cur = currentValueStr.toFloatOrNull() ?: 0f
                                                currentValueStr = (cur + 1f).toString()
                                            }
                                            else -> {
                                                val target = label.replace("年", "").toFloatOrNull() ?: 1f
                                                currentValueStr = target.toString()
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.SemiBold,
                                    color = ReshapeTheme.colors.primary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                NumPadInputMode.REPS -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val repQuickButtons = mutableListOf<String>()
                        if (lastHistoryValue != null && lastHistoryValue > 0f) {
                            repQuickButtons.add("上次")
                        }
                        repQuickButtons.addAll(listOf("+1次", "-1次", "8次", "10次", "12次"))

                        repQuickButtons.forEachIndexed { index, label ->
                            if (index > 0) Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .background(
                                        if (label == "上次") ReshapeTheme.colors.primary else ReshapeTheme.colors.primarySoft,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        when (label) {
                                            "上次" -> {
                                                if (lastHistoryValue != null) {
                                                    currentValueStr = lastHistoryValue.toInt().toString()
                                                }
                                            }
                                            "+1次" -> {
                                                val cur = currentValueStr.toIntOrNull() ?: 0
                                                currentValueStr = (cur + 1).toString()
                                            }
                                            "-1次" -> {
                                                val cur = currentValueStr.toIntOrNull() ?: 0
                                                currentValueStr = maxOf(1, cur - 1).toString()
                                            }
                                            else -> {
                                                val target = label.replace("次", "").toIntOrNull() ?: 8
                                                currentValueStr = target.toString()
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.SemiBold,
                                    color = if (label == "上次") Color.White else ReshapeTheme.colors.primary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                NumPadInputMode.SETS -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val setButtons = listOf("+1组", "-1组", "3组", "4组", "5组")
                        setButtons.forEachIndexed { index, label ->
                            if (index > 0) Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(8.dp))
                                    .clickable {
                                        when (label) {
                                            "+1组" -> {
                                                val cur = currentValueStr.toIntOrNull() ?: 3
                                                currentValueStr = minOf(30, cur + 1).toString()
                                            }
                                            "-1组" -> {
                                                val cur = currentValueStr.toIntOrNull() ?: 3
                                                currentValueStr = maxOf(1, cur - 1).toString()
                                            }
                                            else -> {
                                                val target = label.replace("组", "").toIntOrNull() ?: 4
                                                currentValueStr = target.toString()
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.SemiBold,
                                    color = ReshapeTheme.colors.primary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                NumPadInputMode.REST_SECONDS -> {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val restButtons = listOf("+15s", "-15s", "60s", "90s", "120s", "180s")
                        restButtons.forEachIndexed { index, label ->
                            if (index > 0) Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(8.dp))
                                    .clickable {
                                        when (label) {
                                            "+15s" -> {
                                                val cur = currentValueStr.toIntOrNull() ?: 120
                                                currentValueStr = minOf(600, cur + 15).toString()
                                            }
                                            "-15s" -> {
                                                val cur = currentValueStr.toIntOrNull() ?: 120
                                                currentValueStr = maxOf(15, cur - 15).toString()
                                            }
                                            else -> {
                                                val target = label.replace("s", "").toIntOrNull() ?: 120
                                                currentValueStr = target.toString()
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.SemiBold,
                                    color = ReshapeTheme.colors.primary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // 9 宫格数字全宽 56dp 大按键矩阵 (1~9, ., 0, ⌫ / C)
            val keyRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(if (mode.isDecimalAllowed) "." else "C", "0", "⌫")
            )

            keyRows.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowKeys.forEachIndexed { index, key ->
                        if (index > 0) Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .background(
                                    when {
                                        key.isEmpty() -> Color.Transparent
                                        key == "C" -> Color(0xFFFEE2E2)
                                        else -> ReshapeTheme.colors.bgCanvas
                                    },
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = key.isNotEmpty()) {
                                    when (key) {
                                        "C" -> {
                                            currentValueStr = ""
                                        }
                                        "⌫" -> {
                                            if (currentValueStr.isNotEmpty()) {
                                                currentValueStr = currentValueStr.dropLast(1)
                                            }
                                        }
                                        "." -> {
                                            if (mode.isDecimalAllowed && !currentValueStr.contains(".")) {
                                                currentValueStr = if (currentValueStr.isEmpty()) "0." else "$currentValueStr."
                                            }
                                        }
                                        else -> {
                                            if (currentValueStr == "0") {
                                                currentValueStr = key
                                            } else {
                                                currentValueStr += key
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                fontSize = 24.sp,
                                fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.Bold,
                                color = if (key == "C") Color(0xFFDC2626) else ReshapeTheme.colors.textPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 确认按键
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(ReshapeTheme.colors.primary, RoundedCornerShape(14.dp))
                    .clickable {
                        val num = currentValueStr.toFloatOrNull() ?: 0f
                        onConfirm(num)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.confirmText,
                    fontSize = 18.sp,
                    fontWeight = com.tencent.kuikly.compose.ui.text.font.FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
