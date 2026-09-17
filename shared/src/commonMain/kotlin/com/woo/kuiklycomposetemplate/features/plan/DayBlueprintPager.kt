package com.woo.kuiklycomposetemplate.features.plan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.pager.Pager
import com.woo.kuiklycomposetemplate.core.theme.ReshapeTheme
import com.woo.kuiklycomposetemplate.data.local.PlanRepository
import com.woo.kuiklycomposetemplate.domain.model.DayType

@Page("day_blueprint", supportInLocal = true)
class DayBlueprintPager : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ReshapeTheme {
                DayBlueprintScreen()
            }
        }
    }
}

@Composable
fun DayBlueprintScreen() {
    val pager = LocalActivity.current.getPager() as? Pager
    val spModule = pager?.acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)

    var currentDay by remember { mutableStateOf(PlanRepository.getCurrentDay(spModule)) }
    val exercises = PlanRepository.getExercises(currentDay, spModule)

    val blueprintIntro = when (currentDay) {
        DayType.PUSH -> "【推日底层设计逻辑】\n从多关节大重量主项（卧推）建立 CNS 神经募集，中段引入上斜与双杠针对上胸及三头，末段通过绳索夹胸+侧平举超级组在极低关节压力下耗尽肌纤维。"
        DayType.PULL -> "【拉日底层设计逻辑】\n垂直拉（高位下拉）与水平拉（划船）交替构建背阔肌宽度与上背厚度，辅以反向飞鸟强化后束代偿防护，末段弯举超级组直接拉满二头肌峰充血。"
        DayType.LEGS -> "【腿日底层设计逻辑】\n以低杠/高杠深蹲建立下肢最大动力链，辅项结合保加利亚单腿蹲消除左右侧力量失衡，最后用倒蹬与腿屈伸/腿弯举超级组榨干股四与腘绳肌。"
        DayType.REST -> "【主动恢复逻辑】\n神经系统与糖原再生日，配合筋膜放松与低强度拉伸促进代谢废物排出。"
    }

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
                            pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("plan")
                        }
                        .padding(end = 16.dp)
                )
                Text(
                    text = "${currentDay.code} · ${currentDay.title} 蓝图详解",
                    fontSize = 16.sp,
                    color = ReshapeTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "编排 ➔",
                    fontSize = 14.sp,
                    color = ReshapeTheme.colors.primary,
                    modifier = Modifier.clickable {
                        pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("plan_edit")
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Day Tab 切换
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                listOf(DayType.PUSH, DayType.PULL, DayType.LEGS).forEachIndexed { index, d ->
                    if (index > 0) Spacer(modifier = Modifier.width(8.dp))
                    val isSelected = d == currentDay
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) ReshapeTheme.colors.primarySoft else ReshapeTheme.colors.card,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { currentDay = d }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${d.icon} ${d.code}",
                            fontSize = 14.sp,
                            color = if (isSelected) ReshapeTheme.colors.primary else ReshapeTheme.colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 蓝图核心设计思想卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "💡 动作编排科学原理", fontSize = 15.sp, color = ReshapeTheme.colors.textPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = blueprintIntro,
                        fontSize = 13.sp,
                        color = ReshapeTheme.colors.textSecondary,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5 大动作逐一拆解
            exercises.forEachIndexed { index, ex ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "动作 0${index + 1}",
                                fontSize = 12.sp,
                                color = ReshapeTheme.colors.primary,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = ex.progressionType.label, fontSize = 10.sp, color = ReshapeTheme.colors.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = ex.name, fontSize = 18.sp, color = ReshapeTheme.colors.textPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🎯 目标部位：${ex.targetMuscle} · ${ex.defaultSets} 组 × ${ex.targetRepsMin}-${ex.targetRepsMax} 次 · RPE ${ex.targetRpe}",
                            fontSize = 12.sp,
                            color = ReshapeTheme.colors.textSecondary
                        )

                        if (ex.defaultHoleCues.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "📌 调机备忘：${ex.defaultHoleCues}",
                                    fontSize = 11.sp,
                                    color = ReshapeTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 底部 CTA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(50.dp)
                    .background(ReshapeTheme.colors.primary, RoundedCornerShape(14.dp))
                    .clickable {
                        pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("plan_edit")
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "编排此训练日动作 ➔", fontSize = 16.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
