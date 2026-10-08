package com.woo.kuiklycomposetemplate.features.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager
import com.woo.kuiklycomposetemplate.core.components.ReshapeBottomBar
import com.woo.kuiklycomposetemplate.core.components.ReshapeNavTab
import com.woo.kuiklycomposetemplate.core.theme.ReshapeTheme
import com.woo.kuiklycomposetemplate.core.theme.ReshapeThemeId
import com.woo.kuiklycomposetemplate.core.theme.ThemeStateHolder
import com.woo.kuiklycomposetemplate.data.local.PlanRepository
import com.woo.kuiklycomposetemplate.data.local.SessionDraftStore
import com.woo.kuiklycomposetemplate.data.local.TrainingCycleStore
import com.woo.kuiklycomposetemplate.data.local.UserPreferencesStore
import com.woo.kuiklycomposetemplate.domain.algorithm.WeightSuggestEngine
import com.woo.kuiklycomposetemplate.domain.model.DayType

@Page("home", supportInLocal = true)
class HomePager : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ReshapeTheme {
                HomeScreen()
            }
        }
    }
}

@Composable
fun HomeScreen() {
    val pager = LocalActivity.current.getPager() as? Pager
    val spModule = pager?.acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)

    var currentDay by remember { mutableStateOf(PlanRepository.getCurrentDay(spModule)) }
    var isFatigued by remember { mutableStateOf(false) }
    val history = remember { com.woo.kuiklycomposetemplate.data.local.WorkoutHistoryStore.getHistory(spModule) }
    var daysSinceLastWorkout by remember { mutableStateOf(com.woo.kuiklycomposetemplate.data.local.WorkoutHistoryStore.getDaysSinceLastWorkout(com.woo.kuiklycomposetemplate.core.utils.platformCurrentTimeMillis())) }
    var isRehabProtected by remember { mutableStateOf(daysSinceLastWorkout > 7) }
    var hasDraft by remember { mutableStateOf(SessionDraftStore.hasDraft(spModule)) }

    val exercises = PlanRepository.getExercises(currentDay, spModule)
    val userProfile = UserPreferencesStore.getProfile(spModule)

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val onboardingDone = UserPreferencesStore.isOnboardingCompleted(spModule)
        if (!onboardingDone) {
            pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("onboarding")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ReshapeTheme.colors.bgCanvas)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
        item {
            // 顶栏紧凑留白 (40dp)
            Spacer(modifier = Modifier.height(40.dp))

            // 头部标题与计划编排入口
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("plan")
                        }
                ) {
                    Text(text = "你的训练计划 ›", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "经典三分化 ⚙️", fontSize = 22.sp, color = ReshapeTheme.colors.textPrimary)
                }

                Box(
                    modifier = Modifier
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(12.dp))
                        .clickable {
                            pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("plan_edit")
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = "⚙️ 编排", fontSize = 13.sp, color = ReshapeTheme.colors.primary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 练中草稿自愈恢复条
            if (hasDraft) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(12.dp))
                        .clickable {
                            val params = JSONObject().apply {
                                put("day", currentDay.name)
                            }
                            pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("record", params)
                        }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔄", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "检测到未完成的训练现场",
                                fontSize = 13.sp,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "点击一键无缝恢复做组数据",
                                fontSize = 11.sp,
                                color = Color(0xFFB45309)
                            )
                        }
                        Text(
                            text = "继续 ➔",
                            fontSize = 12.sp,
                            color = Color(0xFF92400E)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // 循环位置与调度操作
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "当前循环位置",
                            fontSize = 13.sp,
                            color = ReshapeTheme.colors.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        key(currentDay) {
                            Text(
                                text = if (currentDay == DayType.REST) "⚡ 恢复训练" else "🛌 今天休息",
                                fontSize = 12.sp,
                                color = ReshapeTheme.colors.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        if (currentDay == DayType.REST) {
                                            currentDay = DayType.PUSH
                                            PlanRepository.setCurrentDay(DayType.PUSH, spModule)
                                        } else {
                                            currentDay = DayType.REST
                                            PlanRepository.setCurrentDay(DayType.REST, spModule)
                                        }
                                    }
                                    .padding(end = 12.dp)
                            )
                        }
                        Text(
                            text = "⇄ 换部位",
                            fontSize = 12.sp,
                            color = ReshapeTheme.colors.textSecondary,
                            modifier = Modifier.clickable {
                                currentDay = PlanRepository.advanceToNextDay(spModule)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // D1 / D2 / D3 循环条
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val days = listOf(DayType.PUSH, DayType.PULL, DayType.LEGS)
                        days.forEachIndexed { index, d ->
                            if (index > 0) Spacer(modifier = Modifier.width(8.dp))
                            val isToday = d == currentDay
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isToday) ReshapeTheme.colors.primarySoft else ReshapeTheme.colors.card,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        currentDay = d
                                        PlanRepository.setCurrentDay(d, spModule)
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${d.icon} ${d.code}",
                                        fontSize = 14.sp,
                                        color = if (isToday) ReshapeTheme.colors.primary else ReshapeTheme.colors.textPrimary,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isToday) "今天 ›" else "待练",
                                        fontSize = 11.sp,
                                        color = if (isToday) ReshapeTheme.colors.primary else ReshapeTheme.colors.textFaint
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    val trainingCycle = TrainingCycleStore.getCycleForDay(currentDay, spModule)
                    val cycleBannerText = when {
                        trainingCycle.isDeloadActive -> "🛌 周期减载蓄力中 (容量减半) · 点击查看递进 ›"
                        trainingCycle.isCompleted -> "🎉 本周期 ${currentDay.title}日 8 次已达成 · 点击重测 1RM 结项 ›"
                        else -> "本周期${currentDay.title}日 第 ${trainingCycle.currentSessionIndex} / ${trainingCycle.totalSessions} 次 · 距重测 1RM 还剩 ${trainingCycle.remainingSessions} 次 ›"
                    }
                    val cycleBannerColor = when {
                        trainingCycle.isCompleted -> ReshapeTheme.colors.primary
                        trainingCycle.isDeloadActive -> Color(0xFF3B82F6)
                        else -> ReshapeTheme.colors.textFaint
                    }

                    Text(
                        text = cycleBannerText,
                        fontSize = 12.sp,
                        color = cycleBannerColor,
                        fontWeight = if (trainingCycle.isCompleted) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clickable {
                                pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("progression")
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 就绪度切换条 (⚡充沛 ⇋ 🔋疲劳)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (!isFatigued) ReshapeTheme.colors.primarySoft else ReshapeTheme.colors.card,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { isFatigued = false }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "⚡ 状态充沛",
                            fontSize = 14.sp,
                            color = if (!isFatigued) ReshapeTheme.colors.primary else ReshapeTheme.colors.textSecondary,
                            fontWeight = if (!isFatigued) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = "标准负荷",
                            fontSize = 10.sp,
                            color = if (!isFatigued) ReshapeTheme.colors.primary else ReshapeTheme.colors.textFaint
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isFatigued) Color(0xFFFEF3C7) else ReshapeTheme.colors.card,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { isFatigued = true }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🔋 状态疲劳",
                            fontSize = 14.sp,
                            color = if (isFatigued) Color(0xFFD97706) else ReshapeTheme.colors.textSecondary,
                            fontWeight = if (isFatigued) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = "降重 10% 练容量",
                            fontSize = 10.sp,
                            color = if (isFatigued) Color(0xFFB45309) else ReshapeTheme.colors.textFaint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 出差/复训保护横幅 (仅在停训超过 7 天时才显示，正常状态下默认隐藏)
            if (daysSinceLastWorkout > 7) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(
                            if (isRehabProtected) Color(0xFFECFDF5) else Color(0xFFF3F4F6),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "✈️", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isRehabProtected) "复训保护已激活 (80% 唤醒)" else "出差 / 停训回归？",
                                fontSize = 12.sp,
                                color = ReshapeTheme.colors.textPrimary
                            )
                            Text(
                                text = "停训 >7 天建议 80% 唤醒负荷，避免 DOMS 与拉伤",
                                fontSize = 10.sp,
                                color = ReshapeTheme.colors.textSecondary
                            )
                        }
                        key(isRehabProtected) {
                            Box(
                                modifier = Modifier
                                    .background(ReshapeTheme.colors.card, RoundedCornerShape(8.dp))
                                    .clickable { isRehabProtected = !isRehabProtected }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isRehabProtected) "关闭" else "开启 ›",
                                    fontSize = 11.sp,
                                    color = ReshapeTheme.colors.primary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 今日训练核心大卡片
            if (currentDay == DayType.REST) {
                // 休息日主动恢复大卡片
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🛌 今日休息 · 主动超量恢复",
                                fontSize = 16.sp,
                                color = Color(0xFF047857),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFECFDF5), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(text = "休息蓄力中", fontSize = 11.sp, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val recoveryItems = listOf(
                            "🧘‍♂️ 泡沫轴全身筋膜放松 · 10 分钟",
                            "🧘 髋关节与胸椎灵活性伸展 · 8 分钟",
                            "🚶 5000 步低强度轻松散步 · 促进血液循环",
                            "💤 保证充足深度睡眠与优质碳水营养"
                        )
                        recoveryItems.forEachIndexed { idx, itemText ->
                            if (idx > 0) Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .height(6.dp)
                                        .background(Color(0xFF10B981), RoundedCornerShape(3.dp))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = itemText, fontSize = 13.sp, color = ReshapeTheme.colors.textPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "💡 建议：筋膜枪 · 泡沫轴放松 · 充足碳水与电解质",
                                fontSize = 12.sp,
                                color = ReshapeTheme.colors.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 恢复训练切换按钮
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(14.dp))
                                .clickable {
                                    currentDay = DayType.PUSH
                                    PlanRepository.setCurrentDay(DayType.PUSH, spModule)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⇄ 结束休息，开始推日训练",
                                fontSize = 15.sp,
                                color = ReshapeTheme.colors.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // 正常训练日核心大卡片
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${currentDay.code} · ${currentDay.title}",
                                fontSize = 16.sp,
                                color = ReshapeTheme.colors.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(text = "今日训练", fontSize = 11.sp, color = ReshapeTheme.colors.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 动作清单与建议重量展示 (完整响应计划编排定制结果)
                        exercises.forEach { ex ->
                            val displayWeightStr = if (isFatigued) {
                                if (ex.waveWeightsSequence.isNotBlank()) {
                                    val weights = ex.getPerSetWeightsList().map { (it * 0.9f).toInt() }
                                    "${weights.first()}~${weights.last()}kg (疲劳-10%)"
                                } else {
                                    "${(ex.targetWeightKg * 0.9f).toInt()}kg (疲劳-10%)"
                                }
                            } else if (isRehabProtected) {
                                if (ex.waveWeightsSequence.isNotBlank()) {
                                    val weights = ex.getPerSetWeightsList().map { (it * 0.8f).toInt() }
                                    "${weights.first()}~${weights.last()}kg (唤醒80%)"
                                } else {
                                    "${(ex.targetWeightKg * 0.8f).toInt()}kg (唤醒80%)"
                                }
                            } else {
                                ex.displayWeightText
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("plan_edit")
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = ex.name,
                                            fontSize = 14.sp,
                                            color = ReshapeTheme.colors.textPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(text = ex.loadType.label, fontSize = 9.sp, color = ReshapeTheme.colors.primary)
                                        }
                                        if (ex.enableWarmupLadder) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "🪜", fontSize = 10.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${ex.defaultSets} 组 × ${ex.displayRepsText} · 休息 ${ex.defaultRestSeconds}s · RPE ${ex.targetRpe}",
                                        fontSize = 11.sp,
                                        color = ReshapeTheme.colors.textSecondary
                                    )
                                }

                                Text(
                                    text = displayWeightStr,
                                    fontSize = 13.sp,
                                    color = if (isFatigued || isRehabProtected) Color(0xFFD97706) else ReshapeTheme.colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 装备建议清单
                        val gearTip = when (currentDay) {
                            DayType.PUSH -> "💡 装备建议：护腕 · 举重鞋 / 平底鞋"
                            DayType.PULL -> "💡 装备建议：握力带 / 助力带 · 举重腰带"
                            DayType.LEGS -> "💡 装备建议：硬举腰带 · 7mm 护膝 · 平底深蹲鞋"
                            DayType.REST -> "💡 建议：筋膜枪 · 泡沫轴放松 · 充足碳水与电解质"
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text(text = gearTip, fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 开始训练 CTA
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .background(ReshapeTheme.colors.primary, RoundedCornerShape(14.dp))
                                .clickable {
                                    val params = JSONObject().apply {
                                        put("day", currentDay.name)
                                    }
                                    pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("record", params)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "开始 ${currentDay.title} 训练",
                                fontSize = 17.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 肌群超量恢复雷达卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(ReshapeTheme.colors.card, RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⚡ 肌群超量恢复动力学", fontSize = 15.sp, color = ReshapeTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                        Text(text = "生物钟算法评估", fontSize = 10.sp, color = ReshapeTheme.colors.textFaint)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val dynamicMuscleStates = remember(history) {
                        com.woo.kuiklycomposetemplate.domain.algorithm.MuscleRecoveryEngine.evaluateAllMuscleGroups(
                            history,
                            com.woo.kuiklycomposetemplate.core.utils.platformCurrentTimeMillis()
                        )
                    }

                    dynamicMuscleStates.forEach { state ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = state.muscleGroup, fontSize = 13.sp, color = ReshapeTheme.colors.textSecondary, modifier = Modifier.weight(1f))
                            Text(
                                text = state.statusDescription,
                                fontSize = 11.sp,
                                color = if (state.isSupercompensated) ReshapeTheme.colors.green else ReshapeTheme.colors.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(state.recoveryRatio)
                                    .height(6.dp)
                                    .background(
                                        if (state.isSupercompensated) ReshapeTheme.colors.green else ReshapeTheme.colors.primary,
                                        RoundedCornerShape(3.dp)
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(90.dp))
        }
    }

    ReshapeBottomBar(
        currentTab = ReshapeNavTab.TRAIN,
        modifier = Modifier.align(Alignment.BottomCenter)
    )
    }
}
