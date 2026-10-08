package com.woo.kuiklycomposetemplate.features.plan

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
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager
import com.woo.kuiklycomposetemplate.core.theme.ReshapeTheme
import com.woo.kuiklycomposetemplate.data.local.PlanRepository
import com.woo.kuiklycomposetemplate.data.local.UserPreferencesStore
import com.woo.kuiklycomposetemplate.domain.algorithm.WeightSuggestEngine
import com.woo.kuiklycomposetemplate.domain.model.DayType

@Page("plan", supportInLocal = true)
class PlanOutlinePager : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ReshapeTheme {
                PlanOutlineScreen()
            }
        }
    }
}

@Composable
fun PlanOutlineScreen() {
    val pager = LocalActivity.current.getPager() as? Pager
    val spModule = pager?.acquireModule<com.tencent.kuikly.core.module.SharedPreferencesModule>(com.tencent.kuikly.core.module.SharedPreferencesModule.MODULE_NAME)

    val profile = UserPreferencesStore.getProfile(spModule)
    val days = listOf(DayType.PUSH, DayType.PULL, DayType.LEGS)

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
                            pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("home")
                        }
                        .padding(end = 16.dp)
                )
                Text(
                    text = "完整计划大纲",
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

            Spacer(modifier = Modifier.height(10.dp))

            // 标题与说明
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(text = "经典三分化训练计划", fontSize = 24.sp, color = ReshapeTheme.colors.textPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "推 / 拉 / 腿三天一轮，每个部位每周刺激 2 次。主项线性递进，辅项稳定动作轨迹与做组容量。",
                    fontSize = 13.sp,
                    color = ReshapeTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 三大项 1RM 基准卡
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${profile.squat1RmKg.toInt()} kg", fontSize = 20.sp, color = ReshapeTheme.colors.textPrimary)
                            Text(text = "深蹲 1RM", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                        }
                        Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color(0xFFE5E7EB)))
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${profile.bench1RmKg.toInt()} kg", fontSize = 20.sp, color = ReshapeTheme.colors.textPrimary)
                            Text(text = "卧推 1RM", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                        }
                        Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color(0xFFE5E7EB)))
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${profile.deadlift1RmKg.toInt()} kg", fontSize = 20.sp, color = ReshapeTheme.colors.textPrimary)
                            Text(text = "硬拉 1RM", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF3F4F6)))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 下方所有建议重量均由三大项 1RM 科学折算",
                            fontSize = 11.sp,
                            color = ReshapeTheme.colors.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "修改基准 ›",
                            fontSize = 12.sp,
                            color = ReshapeTheme.colors.primary,
                            modifier = Modifier.clickable {
                                pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("profile")
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3 个训练日动作卡片
            days.forEach { day ->
                val exercises = PlanRepository.getExercises(day, spModule)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "${day.icon} ${day.code} · ${day.title}", fontSize = 16.sp, color = ReshapeTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                            Text(
                                text = "蓝图设计逻辑 ›",
                                fontSize = 12.sp,
                                color = ReshapeTheme.colors.primary,
                                modifier = Modifier.clickable {
                                    val data = JSONObject().apply {
                                        put("day", when (day) {
                                            DayType.PUSH -> "push"
                                            DayType.PULL -> "pull"
                                            DayType.LEGS -> "legs"
                                            DayType.REST -> "push"
                                        })
                                    }
                                    pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("day_blueprint", data)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        exercises.forEachIndexed { index, ex ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 12.sp,
                                    color = ReshapeTheme.colors.textFaint,
                                    modifier = Modifier.width(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = ex.name, fontSize = 14.sp, color = ReshapeTheme.colors.textPrimary)
                                    Text(
                                        text = "${ex.defaultSets} 组 × ${ex.displayRepsText} · 休息 ${ex.defaultRestSeconds}s · RPE ${ex.targetRpe}",
                                        fontSize = 11.sp,
                                        color = ReshapeTheme.colors.textSecondary
                                    )
                                }
                                Text(
                                    text = ex.displayWeightText,
                                    fontSize = 14.sp,
                                    color = ReshapeTheme.colors.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
