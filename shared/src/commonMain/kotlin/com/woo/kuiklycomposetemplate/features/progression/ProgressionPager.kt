package com.woo.kuiklycomposetemplate.features.progression

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
import com.tencent.kuikly.compose.foundation.layout.fillMaxHeight
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
import com.woo.kuiklycomposetemplate.core.components.GymNumPadDrawer
import com.woo.kuiklycomposetemplate.core.components.NumPadInputMode
import com.woo.kuiklycomposetemplate.core.components.PainFreeDrawer
import com.woo.kuiklycomposetemplate.core.theme.ReshapeTheme
import com.woo.kuiklycomposetemplate.data.local.PlanRepository
import com.woo.kuiklycomposetemplate.data.local.SessionDraftStore
import com.woo.kuiklycomposetemplate.data.local.TrainingCycleStore
import com.woo.kuiklycomposetemplate.data.local.UserPreferencesStore
import com.woo.kuiklycomposetemplate.data.local.WorkoutHistoryStore
import com.woo.kuiklycomposetemplate.domain.algorithm.BrzyckiCalculator
import com.woo.kuiklycomposetemplate.domain.model.DayType
import com.woo.kuiklycomposetemplate.domain.model.Exercise

enum class CycleBarStatus {
    COMPLETED_SUCCESS, // 达标完成 (绿)
    COMPLETED_BELOW,   // 低于建议/停滞 (橙)
    IN_PROGRESS,       // 今天待练 (主色)
    UPCOMING           // 未来计划 (灰)
}

data class SessionStepDetail(
    val sessionIndex: Int,          // 第几次 (1..8)
    val suggestedWeightKg: Float,   // 建议重量
    val actualWeightKg: Float?,     // 实际打卡重量 (若已练)
    val actualReps: Int?,           // 实际完成次数
    val rpe: Float?,                // 实际 RPE
    val status: CycleBarStatus,
    val note: String = ""
)

@Page("progression", supportInLocal = true)
class ProgressionPager : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ReshapeTheme {
                ProgressionScreen()
            }
        }
    }
}

@Composable
fun ProgressionScreen() {
    val pager = LocalActivity.current.getPager() as? Pager
    val spModule = pager?.acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)

    val currentTodayPlan = PlanRepository.getCurrentDay(spModule)
    var selectedDayType by remember { mutableStateOf(currentTodayPlan) }
    var selectedExerciseIndex by remember { mutableStateOf(0) }
    var refreshTrigger by remember { mutableStateOf(0) }

    val dayExercises = PlanRepository.getExercises(selectedDayType, spModule)
    val currentExercise: Exercise = dayExercises.getOrNull(selectedExerciseIndex)
        ?: dayExercises.firstOrNull()
        ?: Exercise(
            id = "fallback",
            name = "罗马尼亚硬拉",
            dayType = DayType.LEGS,
            loadType = com.woo.kuiklycomposetemplate.domain.model.LoadType.BARBELL,
            targetMuscle = "腘绳肌/臀大肌",
            targetWeightKg = 72.5f
        )

    val profile = UserPreferencesStore.getProfile(spModule)
    val (trainingCycle, historyRecords) = TrainingCycleStore.getCycleForExercise(currentExercise, selectedDayType, spModule)
    val hasActiveDraft = SessionDraftStore.hasDraft(spModule)

    var painFreeVisible by remember { mutableStateOf(false) }
    var retestSheetVisible by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    // 停滞状态判定：检查历史记录中是否存在连续力竭或高 RPE 记录
    val isStalled = (historyRecords.size >= 2 && historyRecords.takeLast(2).all { it.rpe >= 9.0f }) ||
                    (trainingCycle.currentSessionIndex >= 4 && historyRecords.any { it.rpe >= 9.0f })

    // 构造 8 次推进时间线数据 (真实历史打卡 + 无痛变式映射 + 今天 + 未来预测)
    val baseWeight = if (currentExercise.targetWeightKg > 0f) currentExercise.targetWeightKg else 60f
    val cycleIndex = trainingCycle.currentSessionIndex.coerceIn(1, 8)

    val stepDetails = (1..8).map { stepNo ->
        val stepTarget = baseWeight - (cycleIndex - stepNo) * 2.5f
        when {
            stepNo < cycleIndex -> {
                // 已完成的历次
                val record = historyRecords.getOrNull(stepNo - 1)
                if (record != null) {
                    val isSuccess = record.weightKg >= stepTarget && record.rpe <= 8.5f
                    SessionStepDetail(
                        sessionIndex = stepNo,
                        suggestedWeightKg = stepTarget,
                        actualWeightKg = record.weightKg,
                        actualReps = record.reps,
                        rpe = record.rpe,
                        status = if (isSuccess) CycleBarStatus.COMPLETED_SUCCESS else CycleBarStatus.COMPLETED_BELOW,
                        note = if (record.isVariant) "实际：${record.exerciseName} [无痛变式]" else if (isSuccess) "达标完成" else "低于建议"
                    )
                } else {
                    SessionStepDetail(
                        sessionIndex = stepNo,
                        suggestedWeightKg = stepTarget,
                        actualWeightKg = stepTarget,
                        actualReps = currentExercise.targetRepsMin,
                        rpe = 8.0f,
                        status = CycleBarStatus.COMPLETED_SUCCESS,
                        note = "历史达标"
                    )
                }
            }
            stepNo == cycleIndex -> {
                // 今天待练
                SessionStepDetail(
                    sessionIndex = stepNo,
                    suggestedWeightKg = baseWeight,
                    actualWeightKg = null,
                    actualReps = null,
                    rpe = null,
                    status = CycleBarStatus.IN_PROGRESS,
                    note = "今天 · 建议目标"
                )
            }
            else -> {
                // 未来规划
                val futureTarget = baseWeight + (stepNo - cycleIndex) * 2.5f
                SessionStepDetail(
                    sessionIndex = stepNo,
                    suggestedWeightKg = futureTarget,
                    actualWeightKg = null,
                    actualReps = null,
                    rpe = null,
                    status = CycleBarStatus.UPCOMING,
                    note = if (stepNo == 8) "周期收官日" else "未开始"
                )
            }
        }
    }

    val orangeColor = Color(0xFFF97316)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ReshapeTheme.colors.bgCanvas)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
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
                        fontSize = 28.sp,
                        color = ReshapeTheme.colors.textSecondary,
                        modifier = Modifier
                            .clickable {
                                pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("home")
                            }
                            .padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "周期递进分析",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = ReshapeTheme.colors.textPrimary
                        )
                        Text(
                            text = "第 ${trainingCycle.cycleNumber} 周期 · 8 次中周期管理",
                            fontSize = 11.sp,
                            color = ReshapeTheme.colors.textSecondary
                        )
                    }

                    // 1RM 重测入口
                    Box(
                        modifier = Modifier
                            .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(8.dp))
                            .clickable { retestSheetVisible = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "🏆 1RM 校准",
                            fontSize = 12.sp,
                            color = ReshapeTheme.colors.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // 提示横幅 (Toast)
                toastMessage?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .background(Color(0xFFECFDF5), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(text = "✓ $msg", fontSize = 12.sp, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 分化日切换胶囊 (D1 推 / D2 拉 / D3 腿)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    listOf(DayType.PUSH, DayType.PULL, DayType.LEGS).forEach { day ->
                        val isSelected = selectedDayType == day
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 3.dp)
                                .background(
                                    if (isSelected) ReshapeTheme.colors.primary else ReshapeTheme.colors.card,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedDayType = day
                                    selectedExerciseIndex = 0
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${day.code} ${day.title}",
                                fontSize = 12.sp,
                                color = if (isSelected) Color.White else ReshapeTheme.colors.textSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 动作多行优雅胶囊切换器 (3列等宽网格，杜绝挤压变形)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    dayExercises.chunked(3).forEachIndexed { rowIdx, rowExercises ->
                        if (rowIdx > 0) Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowExercises.forEachIndexed { colIdx, ex ->
                                val realIdx = rowIdx * 3 + colIdx
                                val isAct = selectedExerciseIndex == realIdx
                                if (colIdx > 0) Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isAct) ReshapeTheme.colors.darkElevated else ReshapeTheme.colors.card,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedExerciseIndex = realIdx }
                                        .padding(vertical = 7.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ex.name,
                                        fontSize = 11.sp,
                                        color = if (isAct) Color.White else ReshapeTheme.colors.textPrimary,
                                        fontWeight = if (isAct) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                            // 补齐空白占位防止宽度不均
                            repeat(3 - rowExercises.size) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 当前选中动作概览卡片
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "${selectedDayType.title} · 动作 ${selectedExerciseIndex + 1}/${dayExercises.size} · ${currentExercise.targetMuscle}",
                        fontSize = 12.sp,
                        color = ReshapeTheme.colors.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentExercise.name,
                            fontSize = 22.sp,
                            color = ReshapeTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "🩹 无痛变式 ›",
                            fontSize = 12.sp,
                            color = ReshapeTheme.colors.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { painFreeVisible = true }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    val progressionDesc = when (currentExercise.progressionType) {
                        com.woo.kuiklycomposetemplate.domain.model.ProgressionType.DOUBLE_PROGRESSION -> "双重递进 (${currentExercise.targetRepsMin}~${currentExercise.targetRepsMax}次 · 满组进阶)"
                        com.woo.kuiklycomposetemplate.domain.model.ProgressionType.METABOLIC_PUMP -> "代谢充血 (${currentExercise.targetRepsMin}~${currentExercise.targetRepsMax}次 · 恒定质量)"
                        com.woo.kuiklycomposetemplate.domain.model.ProgressionType.UNILATERAL_BALANCE -> "单侧平衡 (${currentExercise.targetRepsMin}~${currentExercise.targetRepsMax}次 · 弱侧对齐)"
                        com.woo.kuiklycomposetemplate.domain.model.ProgressionType.WEIGHTED_BODY -> "自重挂重递进"
                        com.woo.kuiklycomposetemplate.domain.model.ProgressionType.WAVE_STRENGTH -> "波浪力量阶梯 (+2.5kg · 3~5次)"
                    }
                    Text(
                        text = "${currentExercise.defaultSets} 组 × ${currentExercise.targetRepsMin} 次 · RPE ${currentExercise.targetRpe} · $progressionDesc",
                        fontSize = 12.sp,
                        color = ReshapeTheme.colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 8 次柱状趋势带卡片 (8-Session Ribbon)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "本周期 8 次训练趋势",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ReshapeTheme.colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "当前第 ${trainingCycle.currentSessionIndex} 次 · 距重测 1RM 还剩 ${trainingCycle.remainingSessions} 次",
                                    fontSize = 12.sp,
                                    color = ReshapeTheme.colors.textSecondary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(
                                        when {
                                            trainingCycle.isDeloadActive -> Color(0xFFEFF6FF)
                                            trainingCycle.isCompleted -> Color(0xFFFEF3C7)
                                            else -> ReshapeTheme.colors.primarySoft
                                        },
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = when {
                                        trainingCycle.isDeloadActive -> "减载中"
                                        trainingCycle.isCompleted -> "待结项"
                                        else -> "进行中"
                                    },
                                    fontSize = 11.sp,
                                    color = when {
                                        trainingCycle.isDeloadActive -> Color(0xFF2563EB)
                                        trainingCycle.isCompleted -> Color(0xFFD97706)
                                        else -> ReshapeTheme.colors.primary
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 8 根趋势柱条 (高度 120dp 归一化映射，叠加 RPE 疲劳标注)
                        val minWeight = stepDetails.map { it.suggestedWeightKg }.minOrNull() ?: 50f
                        val maxWeight = stepDetails.map { it.suggestedWeightKg }.maxOrNull() ?: 100f

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            stepDetails.forEachIndexed { index, step ->
                                if (index > 0) Spacer(modifier = Modifier.width(6.dp))

                                val weightVal = step.actualWeightKg ?: step.suggestedWeightKg
                                val ratio = if (maxWeight > minWeight) {
                                    0.45f + 0.55f * ((weightVal - minWeight) / (maxWeight - minWeight)).coerceIn(0f, 1f)
                                } else 0.7f

                                val barColor = when (step.status) {
                                    CycleBarStatus.COMPLETED_SUCCESS -> Color(0xFF10B981) // 翡翠绿 (按建议完成)
                                    CycleBarStatus.COMPLETED_BELOW -> orangeColor         // 琥珀橙 (低于建议)
                                    CycleBarStatus.IN_PROGRESS -> ReshapeTheme.colors.primary // 活力蓝 (今天)
                                    CycleBarStatus.UPCOMING -> Color(0xFFE5E7EB)           // 浅灰 (未开始)
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    verticalArrangement = com.tencent.kuikly.compose.foundation.layout.Arrangement.Bottom,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (step.rpe != null && step.rpe >= 9.0f) {
                                        Text(text = "R${step.rpe.toInt()}⚠️", fontSize = 9.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                    } else if (step.rpe != null) {
                                        Text(text = "R${step.rpe.toInt()}", fontSize = 9.sp, color = ReshapeTheme.colors.textFaint)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(ratio)
                                            .background(barColor, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // X 轴标签
                        Row(modifier = Modifier.fillMaxWidth()) {
                            stepDetails.forEachIndexed { index, step ->
                                if (index > 0) Spacer(modifier = Modifier.width(6.dp))
                                val labelText = if (step.status == CycleBarStatus.IN_PROGRESS) "今天" else "${step.suggestedWeightKg.toInt()}"
                                Text(
                                    text = labelText,
                                    fontSize = 10.sp,
                                    color = if (step.status == CycleBarStatus.IN_PROGRESS) ReshapeTheme.colors.primary else ReshapeTheme.colors.textFaint,
                                    fontWeight = if (step.status == CycleBarStatus.IN_PROGRESS) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF3F4F6)))
                        Spacer(modifier = Modifier.height(10.dp))

                        // 图例
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.width(8.dp).height(8.dp).background(Color(0xFF10B981), RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "按建议完成", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.width(8.dp).height(8.dp).background(orangeColor, RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "低于建议", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.width(8.dp).height(8.dp).background(ReshapeTheme.colors.primary, RoundedCornerShape(2.dp)))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "今天", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 停滞诊断与 4 大破局出口 (Stall Diagnosis & 4 Exits)
                if (isStalled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(Color(0xFFFFFBEB), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "⚠️", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "连续两次没有进步 (达到神经疲劳阈值)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "同一动作连续两次未能递增重量或次数，RPE 持续达到 9.0。盲目硬加大概率导致动作代偿与损伤，请选择科学处理：",
                                fontSize = 12.sp,
                                color = Color(0xFFB45309),
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // 4 大破局出口按钮
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .clickable {
                                            val newTarget = maxOf(20f, currentExercise.targetWeightKg - 5f)
                                            val updated = currentExercise.copy(targetWeightKg = newTarget)
                                            PlanRepository.updateExercise(selectedDayType, updated, spModule)
                                            toastMessage = "已将该动作建议重量降至 ${newTarget}kg 重新积累容量"
                                            refreshTrigger++
                                        }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "减到 ${(currentExercise.targetWeightKg - 5f).coerceAtLeast(20f)}kg\n重做两次",
                                        fontSize = 11.sp,
                                        color = Color(0xFF92400E),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .clickable {
                                            val updated = currentExercise.copy(defaultSets = maxOf(2, currentExercise.defaultSets - 1))
                                            PlanRepository.updateExercise(selectedDayType, updated, spModule)
                                            toastMessage = "已将该动作调整为 ${updated.defaultSets} 组以降低局部疲劳"
                                            refreshTrigger++
                                        }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "降到 3 组\n维持强度",
                                        fontSize = 11.sp,
                                        color = Color(0xFF92400E),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Box(
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(8.dp))
                                        .clickable {
                                            TrainingCycleStore.setDeloadActive(true, spModule)
                                            toastMessage = "已开启 1 周主动减载周 (Deload)：容量减半蓄力"
                                            refreshTrigger++
                                        }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "开启 1 周减载\n(容量降半·蓄力)",
                                        fontSize = 11.sp,
                                        color = ReshapeTheme.colors.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .clickable { painFreeVisible = true }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "换动作\n换新刺激",
                                        fontSize = 11.sp,
                                        color = Color(0xFF92400E),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 逐次明细表 (8-Step Timeline Audit)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "逐次明细",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ReshapeTheme.colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(text = "建议 / 实际", fontSize = 12.sp, color = ReshapeTheme.colors.textFaint)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF3F4F6)))
                        Spacer(modifier = Modifier.height(8.dp))

                        stepDetails.forEach { step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "第 ${step.sessionIndex} 次",
                                    fontSize = 13.sp,
                                    color = if (step.status == CycleBarStatus.IN_PROGRESS) ReshapeTheme.colors.primary else ReshapeTheme.colors.textPrimary,
                                    fontWeight = if (step.status == CycleBarStatus.IN_PROGRESS) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.width(60.dp)
                                )

                                Text(
                                    text = "${step.suggestedWeightKg.toInt()} kg",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (step.status == CycleBarStatus.COMPLETED_BELOW) orangeColor else ReshapeTheme.colors.textPrimary,
                                    modifier = Modifier.width(65.dp)
                                )

                                Text(
                                    text = when {
                                        step.actualWeightKg != null -> "实际 ${step.actualWeightKg.toInt()} × ${step.actualReps}"
                                        step.status == CycleBarStatus.IN_PROGRESS -> "今天 · 已智能校准"
                                        else -> step.note
                                    },
                                    fontSize = 12.sp,
                                    color = when (step.status) {
                                        CycleBarStatus.COMPLETED_BELOW -> orangeColor
                                        CycleBarStatus.IN_PROGRESS -> ReshapeTheme.colors.primary
                                        else -> ReshapeTheme.colors.textSecondary
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                if (step.rpe != null) {
                                    Box(
                                        modifier = Modifier
                                            .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = "RPE ${step.rpe.toInt()}", fontSize = 10.sp, color = ReshapeTheme.colors.textSecondary)
                                    }
                                } else if (step.status == CycleBarStatus.IN_PROGRESS) {
                                    Box(
                                        modifier = Modifier
                                            .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = "今日建议", fontSize = 10.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF9FAFB)))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 现实自适应控制器 (出差/停训复训保护 & 减载周)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "✈️ 现实中断与复训保护",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ReshapeTheme.colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(text = "自适应调节", fontSize = 11.sp, color = ReshapeTheme.colors.primary)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "• 停训 1~2 周：开启「复训保护」，按原重量 80% 唤醒肌肉记忆与神经连接。\n• 主动减载 (Deload)：关节不适或中枢神经疲劳时，一键开启 1 周容量减半。",
                            fontSize = 12.sp,
                            color = ReshapeTheme.colors.textSecondary,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (trainingCycle.isReturnShieldActive) Color(0xFFEFF6FF) else ReshapeTheme.colors.bgCanvas,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        val newState = !trainingCycle.isReturnShieldActive
                                        TrainingCycleStore.setReturnShieldActive(newState, spModule)
                                        toastMessage = if (newState) "已开启复训保护 (负荷自动调整为 80%)" else "已关闭复训保护"
                                        refreshTrigger++
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (trainingCycle.isReturnShieldActive) "✈️ 复训保护中 (80%)" else "✈️ 开启复训保护",
                                    fontSize = 12.sp,
                                    color = if (trainingCycle.isReturnShieldActive) Color(0xFF2563EB) else ReshapeTheme.colors.textPrimary,
                                    fontWeight = if (trainingCycle.isReturnShieldActive) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (trainingCycle.isDeloadActive) Color(0xFFECFDF5) else ReshapeTheme.colors.primarySoft,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        val newState = !trainingCycle.isDeloadActive
                                        TrainingCycleStore.setDeloadActive(newState, spModule)
                                        toastMessage = if (newState) "已开启 1 周主动减载 (容量减半·蓄力)" else "已恢复标准训练容量"
                                        refreshTrigger++
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (trainingCycle.isDeloadActive) "🛌 减载周进行中" else "🛌 开启主动减载周",
                                    fontSize = 12.sp,
                                    color = if (trainingCycle.isDeloadActive) Color(0xFF047857) else ReshapeTheme.colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 什么时候该重测 1RM 指南
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "什么时候该重测 1RM",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ReshapeTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val rules = listOf(
                            "1" to "周期走完 8 次：主项力量理论上已提升 10~20kg，旧 1RM 基准已偏低失效。",
                            "2" to "连续两次触发停滞提示：可能并非身体疲劳，而是基准力量系数计算偏差。",
                            "3" to "停训超过 3 周：重新通过 3~5 次安全做组反推新 1RM，重新标定起跑线。"
                        )
                        rules.forEach { (no, txt) ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .height(20.dp)
                                        .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = no, fontSize = 11.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = txt, fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary, lineHeight = 16.sp, modifier = Modifier.weight(1f))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "💡 重测不用真正做 1RM 极限。采用 3~5 次做组重量通过 Brzycki 公式反推，安全且无受伤风险。",
                            fontSize = 11.sp,
                            color = ReshapeTheme.colors.textFaint
                        )
                    }
                }

                // 底部垫高避让吸底栏
                Spacer(modifier = Modifier.height(110.dp))
            }
        }

        // 智能上下文吸底栏 (Smart Context Sticky Action)
        val isExerciseBelongsToToday = selectedDayType == currentTodayPlan
        val actionWeight = currentExercise.targetWeightKg

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(ReshapeTheme.colors.card)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            key(selectedDayType.name, selectedExerciseIndex, hasActiveDraft, actionWeight) {
                val buttonBg = if (isExerciseBelongsToToday || hasActiveDraft) ReshapeTheme.colors.primary else ReshapeTheme.colors.darkElevated
                val buttonText = when {
                    hasActiveDraft -> "✓ 应用此建议 (${actionWeight.toInt()}kg) 并返回练中 ➔"
                    isExerciseBelongsToToday -> "⚡ 采用建议 (${actionWeight.toInt()}kg) 开始今日${currentTodayPlan.title} ➔"
                    else -> "📌 已保存为 ${selectedDayType.title} 下次建议重量 (${actionWeight.toInt()}kg)"
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(buttonBg, RoundedCornerShape(12.dp))
                        .clickable {
                            when {
                                hasActiveDraft -> {
                                    pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("record")
                                }
                                isExerciseBelongsToToday -> {
                                    val params = JSONObject().apply {
                                        put("day", currentTodayPlan.name)
                                    }
                                    pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("record", params)
                                }
                                else -> {
                                    toastMessage = "已保存为 ${currentExercise.name} 下次建议重量 (${actionWeight.toInt()}kg)"
                                    refreshTrigger++
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = buttonText,
                        fontSize = if (isExerciseBelongsToToday || hasActiveDraft) 15.sp else 14.sp,
                        color = Color.White,
                        fontWeight = if (isExerciseBelongsToToday || hasActiveDraft) FontWeight.Bold else FontWeight.SemiBold
                    )
                }
            }
        }

        // 无痛变式抽屉
        PainFreeDrawer(
            visible = painFreeVisible,
            currentExerciseId = currentExercise.id,
            onDismiss = { painFreeVisible = false },
            onSelectVariant = { newEx ->
                PlanRepository.updateExercise(selectedDayType, newEx, spModule)
                toastMessage = "已成功替换为无痛变式：${newEx.name}"
                painFreeVisible = false
                refreshTrigger++
            }
        )

        // 1RM 重测与开启新周期抽屉
        if (retestSheetVisible) {
            RetestOneRmSheet(
                currentCycle = trainingCycle,
                userProfile = profile,
                onDismiss = { retestSheetVisible = false },
                onConfirmRetest = { bench1Rm, squat1Rm, deadlift1Rm ->
                    val updatedProfile = profile.copy(
                        bench1RmKg = bench1Rm,
                        squat1RmKg = squat1Rm,
                        deadlift1RmKg = deadlift1Rm
                    )
                    UserPreferencesStore.saveProfile(updatedProfile, spModule)
                    TrainingCycleStore.startNewCycle(updatedProfile, spModule)
                    toastMessage = "🎉 1RM 已成功校准！已正式开启第 ${trainingCycle.cycleNumber + 1} 周期 (1/8 次)"
                    retestSheetVisible = false
                    refreshTrigger++
                }
            )
        }
    }
}

/**
 * 1RM 极限力量重测与新周期开启抽屉
 */
@Composable
fun RetestOneRmSheet(
    currentCycle: com.woo.kuiklycomposetemplate.domain.model.TrainingCycle,
    userProfile: com.woo.kuiklycomposetemplate.domain.model.UserProfile,
    onDismiss: () -> Unit,
    onConfirmRetest: (bench1Rm: Float, squat1Rm: Float, deadlift1Rm: Float) -> Unit
) {
    var benchWeightStr by remember { mutableStateOf(userProfile.bench1RmKg.toInt().toString()) }
    var benchRepsStr by remember { mutableStateOf("5") }
    var benchRpe by remember { mutableStateOf(8.0f) }

    var squatWeightStr by remember { mutableStateOf(userProfile.squat1RmKg.toInt().toString()) }
    var squatRepsStr by remember { mutableStateOf("5") }
    var squatRpe by remember { mutableStateOf(8.0f) }

    var deadliftWeightStr by remember { mutableStateOf(userProfile.deadlift1RmKg.toInt().toString()) }
    var deadliftRepsStr by remember { mutableStateOf("5") }
    var deadliftRpe by remember { mutableStateOf(8.0f) }

    // 9 宫格键盘状态
    var numPadVisible by remember { mutableStateOf(false) }
    var numPadMode by remember { mutableStateOf(NumPadInputMode.WEIGHT) }
    var numPadInitial by remember { mutableStateOf("60") }
    var currentEditingField by remember { mutableStateOf("BENCH_WEIGHT") }

    val calculatedBench1Rm = BrzyckiCalculator.calculateRpeAdjusted1RM(
        benchWeightStr.toFloatOrNull() ?: 60f,
        benchRepsStr.toIntOrNull() ?: 5,
        benchRpe
    )
    val calculatedSquat1Rm = BrzyckiCalculator.calculateRpeAdjusted1RM(
        squatWeightStr.toFloatOrNull() ?: 90f,
        squatRepsStr.toIntOrNull() ?: 5,
        squatRpe
    )
    val calculatedDeadlift1Rm = BrzyckiCalculator.calculateRpeAdjusted1RM(
        deadliftWeightStr.toFloatOrNull() ?: 110f,
        deadliftRepsStr.toIntOrNull() ?: 5,
        deadliftRpe
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ReshapeTheme.colors.card, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .clickable { /* 阻止冒泡 */ }
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🏆 1RM 力量极限校准与新周期结项",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ReshapeTheme.colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = "✕", fontSize = 18.sp, color = ReshapeTheme.colors.textFaint, modifier = Modifier.clickable { onDismiss() })
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "点击下方做组数值唤起 9 宫格大键盘，输入近期做组与 RPE 储备，系统自动精准反推新 1RM：",
                    fontSize = 12.sp,
                    color = ReshapeTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 卧推
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "卧推", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ReshapeTheme.colors.textPrimary, modifier = Modifier.width(44.dp))
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        key(benchWeightStr) {
                            Text(
                                text = "${benchWeightStr}kg ⌨",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ReshapeTheme.colors.primary,
                                modifier = Modifier.clickable {
                                    numPadMode = NumPadInputMode.WEIGHT
                                    numPadInitial = benchWeightStr
                                    currentEditingField = "BENCH_WEIGHT"
                                    numPadVisible = true
                                }
                            )
                        }
                        Text(text = " × ", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                        key(benchRepsStr) {
                            Text(
                                text = "${benchRepsStr}次 ⌨",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ReshapeTheme.colors.primary,
                                modifier = Modifier.clickable {
                                    numPadMode = NumPadInputMode.REPS
                                    numPadInitial = benchRepsStr
                                    currentEditingField = "BENCH_REPS"
                                    numPadVisible = true
                                }
                            )
                        }
                        Text(text = " @R${benchRpe.toInt()}", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                    }
                    key(calculatedBench1Rm) {
                        Text(text = "➔ 1RM: ${calculatedBench1Rm}kg", fontSize = 13.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 深蹲
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "深蹲", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ReshapeTheme.colors.textPrimary, modifier = Modifier.width(44.dp))
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        key(squatWeightStr) {
                            Text(
                                text = "${squatWeightStr}kg ⌨",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ReshapeTheme.colors.primary,
                                modifier = Modifier.clickable {
                                    numPadMode = NumPadInputMode.WEIGHT
                                    numPadInitial = squatWeightStr
                                    currentEditingField = "SQUAT_WEIGHT"
                                    numPadVisible = true
                                }
                            )
                        }
                        Text(text = " × ", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                        key(squatRepsStr) {
                            Text(
                                text = "${squatRepsStr}次 ⌨",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ReshapeTheme.colors.primary,
                                modifier = Modifier.clickable {
                                    numPadMode = NumPadInputMode.REPS
                                    numPadInitial = squatRepsStr
                                    currentEditingField = "SQUAT_REPS"
                                    numPadVisible = true
                                }
                            )
                        }
                        Text(text = " @R${squatRpe.toInt()}", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                    }
                    key(calculatedSquat1Rm) {
                        Text(text = "➔ 1RM: ${calculatedSquat1Rm}kg", fontSize = 13.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 硬拉
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "硬拉", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ReshapeTheme.colors.textPrimary, modifier = Modifier.width(44.dp))
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        key(deadliftWeightStr) {
                            Text(
                                text = "${deadliftWeightStr}kg ⌨",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ReshapeTheme.colors.primary,
                                modifier = Modifier.clickable {
                                    numPadMode = NumPadInputMode.WEIGHT
                                    numPadInitial = deadliftWeightStr
                                    currentEditingField = "DEADLIFT_WEIGHT"
                                    numPadVisible = true
                                }
                            )
                        }
                        Text(text = " × ", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                        key(deadliftRepsStr) {
                            Text(
                                text = "${deadliftRepsStr}次 ⌨",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ReshapeTheme.colors.primary,
                                modifier = Modifier.clickable {
                                    numPadMode = NumPadInputMode.REPS
                                    numPadInitial = deadliftRepsStr
                                    currentEditingField = "DEADLIFT_REPS"
                                    numPadVisible = true
                                }
                            )
                        }
                        Text(text = " @R${deadliftRpe.toInt()}", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                    }
                    key(calculatedDeadlift1Rm) {
                        Text(text = "➔ 1RM: ${calculatedDeadlift1Rm}kg", fontSize = 13.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(ReshapeTheme.colors.primary, RoundedCornerShape(12.dp))
                        .clickable {
                            onConfirmRetest(calculatedBench1Rm, calculatedSquat1Rm, calculatedDeadlift1Rm)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎉 保存新 1RM 并开启第 ${currentCycle.cycleNumber + 1} 周期 ➔",
                        fontSize = 15.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 挂载 9 宫格数字键盘抽屉
        GymNumPadDrawer(
            visible = numPadVisible,
            mode = numPadMode,
            initialValue = numPadInitial,
            onDismiss = { numPadVisible = false },
            onConfirm = { newVal ->
                when (currentEditingField) {
                    "BENCH_WEIGHT" -> benchWeightStr = if (newVal % 1.0f == 0f) newVal.toInt().toString() else newVal.toString()
                    "BENCH_REPS" -> benchRepsStr = newVal.toInt().toString()
                    "SQUAT_WEIGHT" -> squatWeightStr = if (newVal % 1.0f == 0f) newVal.toInt().toString() else newVal.toString()
                    "SQUAT_REPS" -> squatRepsStr = newVal.toInt().toString()
                    "DEADLIFT_WEIGHT" -> deadliftWeightStr = if (newVal % 1.0f == 0f) newVal.toInt().toString() else newVal.toString()
                    "DEADLIFT_REPS" -> deadliftRepsStr = newVal.toInt().toString()
                }
                numPadVisible = false
            }
        )
    }
}
