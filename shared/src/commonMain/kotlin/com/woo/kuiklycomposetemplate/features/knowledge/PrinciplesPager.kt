package com.woo.kuiklycomposetemplate.features.knowledge

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ComposeContainer
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
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.pager.Pager
import com.woo.kuiklycomposetemplate.core.theme.ReshapeTheme

@Page("principles", supportInLocal = true)
class PrinciplesPager : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ReshapeTheme {
                PrinciplesScreen()
            }
        }
    }
}

@Composable
fun PrinciplesScreen() {
    val pager = LocalActivity.current.getPager() as? Pager

    val principles = listOf(
        Triple(
            "01",
            "RPE 8 是什么感觉？",
            "做完规定的次数之后，感觉自己还能再做 2 个（2 Reps in Reserve）—— 那就是标准的 RPE 8。\n不是练到力竭动不了，也不是轻轻松松。保留 2 次余力能大幅避免中枢神经 (CNS) 疲劳崩溃，保证后续每组的高质量容量积累。"
        ),
        Triple(
            "02",
            "渐进超负荷与双重递进",
            "每一次训练都要给肌纤维提供超越上次的微小刺激。\n• 主项大重量：每次尝试 +2.5kg 线性递增\n• 辅项哑铃/器械：先提次数（从 8 次练到 12 次），达到上限后再升级重量并重置到 8 次。"
        ),
        Triple(
            "03",
            "单侧平衡与弱侧定组",
            "单腿蹲、单臂哑铃等单侧动作，永远先做弱侧（通常是左侧），且弱侧能做多少次，强侧就严格只做多少次，彻底杜绝左右侧肌力与维度代偿失衡。"
        ),
        Triple(
            "04",
            "8 次周期与减载周 (Deload)",
            "连续高强度训练 6~8 周后，结缔组织与中枢神经会产生深层疲劳。主动降低 40% 容量与 20% 重量进行 1 周减载周，超量恢复启动后力量将迎来飞跃。"
        ),
        Triple(
            "05",
            "为什么选择三分化而不是五分化？",
            "自然训练者的肌肉蛋白合成窗口仅持续 24~48 小时。五分化每周每个部位仅刺激 1 次，大部分时间处于合成停滞；三分化每周刺激 2 次，年刺激频次提升 100%，增肌效率成倍提升。"
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ReshapeTheme.colors.bgCanvas)
    ) {
        item {
            Spacer(modifier = Modifier.height(44.dp))

            // 顶栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "‹",
                    fontSize = 26.sp,
                    color = ReshapeTheme.colors.textSecondary,
                    modifier = Modifier
                        .clickable {
                            pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("knowledge")
                        }
                        .padding(end = 16.dp)
                )
                Text(
                    text = "力量训练核心原则",
                    fontSize = 16.sp,
                    color = ReshapeTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 标题
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(text = "练前必读 · 五大原则", fontSize = 24.sp, color = ReshapeTheme.colors.textPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "整套计划的重量、组数、休息与递进机制全部基于以下五条底层原理推导。",
                    fontSize = 13.sp,
                    color = ReshapeTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 五大原则卡片
            principles.forEach { (no, title, content) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(text = no, fontSize = 12.sp, color = ReshapeTheme.colors.primary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = title, fontSize = 16.sp, color = ReshapeTheme.colors.textPrimary)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF3F4F6)))
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = content,
                            fontSize = 13.sp,
                            color = ReshapeTheme.colors.textSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
