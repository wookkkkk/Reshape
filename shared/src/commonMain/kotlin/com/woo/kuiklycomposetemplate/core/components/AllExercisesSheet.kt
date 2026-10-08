package com.woo.kuiklycomposetemplate.core.components

import androidx.compose.runtime.Composable
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
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.woo.kuiklycomposetemplate.core.theme.ReshapeTheme
import com.woo.kuiklycomposetemplate.domain.model.SessionExercise

/**
 * 练中全部动作概览与跳转半屏浮层 (All Exercises Sheet)
 */
@Composable
fun AllExercisesSheet(
    visible: Boolean,
    exercises: List<SessionExercise>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onSelectIndex: (Int) -> Unit
) {
    if (!visible) return

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
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color(0xFFD1D5DB), RoundedCornerShape(2.dp))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 本次训练动作全览 (${currentIndex + 1}/${exercises.size})",
                    fontSize = 18.sp,
                    color = ReshapeTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "关闭",
                    fontSize = 15.sp,
                    color = ReshapeTheme.colors.textFaint,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            exercises.forEachIndexed { index, ex ->
                val isCurrent = index == currentIndex
                val completedSets = ex.sets.size
                val isDone = completedSets >= 3 // 假设至少做满 3 组为标记完成

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .background(
                            if (isCurrent) ReshapeTheme.colors.primarySoft else ReshapeTheme.colors.bgCanvas,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectIndex(index) }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}.",
                            fontSize = 15.sp,
                            color = if (isCurrent) ReshapeTheme.colors.primary else ReshapeTheme.colors.textSecondary,
                            modifier = Modifier.width(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ex.name,
                                fontSize = 16.sp,
                                color = if (isCurrent) ReshapeTheme.colors.primary else ReshapeTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "已完成 $completedSets 组 · ${ex.category}",
                                fontSize = 12.sp,
                                color = ReshapeTheme.colors.textSecondary
                            )
                        }

                        if (isDone) {
                            Text(text = "✅ 完成", fontSize = 13.sp, color = ReshapeTheme.colors.green)
                        } else if (isCurrent) {
                            Text(text = "🔥 进行中", fontSize = 13.sp, color = ReshapeTheme.colors.primary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
