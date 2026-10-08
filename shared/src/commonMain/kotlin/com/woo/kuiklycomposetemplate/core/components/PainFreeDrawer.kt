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
import com.woo.kuiklycomposetemplate.data.local.ExerciseLibraryRepository
import com.woo.kuiklycomposetemplate.data.local.PainFreeVariantInfo
import com.woo.kuiklycomposetemplate.domain.model.Exercise

/**
 * 关节友好无痛变式抽屉 (Pain-free Joint Friendly Variant Drawer)
 */
@Composable
fun PainFreeDrawer(
    visible: Boolean,
    currentExerciseId: String,
    onDismiss: () -> Unit,
    onSelectVariant: (Exercise) -> Unit
) {
    if (!visible) return

    val variants: List<PainFreeVariantInfo> = ExerciseLibraryRepository.getPainFreeVariants(currentExerciseId)

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
                    text = "🩹 关节友好 · 无痛变式替换",
                    fontSize = 18.sp,
                    color = ReshapeTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "取消",
                    fontSize = 15.sp,
                    color = ReshapeTheme.colors.textFaint,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            variants.forEach { v ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(14.dp))
                        .clickable { onSelectVariant(v.exercise) }
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = v.title,
                                fontSize = 16.sp,
                                color = ReshapeTheme.colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "替换 ➔",
                                fontSize = 13.sp,
                                color = ReshapeTheme.colors.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = v.reason,
                            fontSize = 12.sp,
                            color = ReshapeTheme.colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
