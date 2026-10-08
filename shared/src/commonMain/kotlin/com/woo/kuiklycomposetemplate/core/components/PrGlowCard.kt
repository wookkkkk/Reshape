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
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.woo.kuiklycomposetemplate.core.theme.ReshapeTheme

/**
 * NEW PR 暗金呼吸高光激励卡
 */
@Composable
fun PrGlowCard(
    exerciseName: String,
    weightKg: Float,
    reps: Int,
    previousPrKg: Float,
    onDismiss: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(
                color = Color(0xFF1E1A11),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onDismiss() }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆 NEW PR 极限突破！",
                    fontSize = 16.sp,
                    color = ReshapeTheme.colors.prGold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "✕",
                    fontSize = 14.sp,
                    color = Color(0xFF9E8B62),
                    modifier = Modifier.clickable { onDismiss() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$exerciseName：${weightKg}kg × ${reps}次",
                fontSize = 18.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            val gain = weightKg - previousPrKg
            val gainText = if (gain > 0f) "+${gain}kg 突破历史极限" else "刷新同重量做组次数"
            Text(
                text = "超越历史基准 $gainText",
                fontSize = 13.sp,
                color = Color(0xFFD4AF37)
            )
        }
    }
}
