package com.woo.kuiklycomposetemplate.features.plan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
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
import com.tencent.kuikly.compose.foundation.text.BasicTextField
import com.tencent.kuikly.compose.foundation.text.KeyboardOptions
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager
import com.woo.kuiklycomposetemplate.core.components.GymNumPadDrawer
import com.woo.kuiklycomposetemplate.core.components.NumPadInputMode
import com.woo.kuiklycomposetemplate.core.theme.ReshapeTheme
import com.woo.kuiklycomposetemplate.data.local.PlanRepository
import com.woo.kuiklycomposetemplate.data.local.UserPreferencesStore
import com.woo.kuiklycomposetemplate.domain.algorithm.WeightSuggestEngine
import com.woo.kuiklycomposetemplate.domain.model.DayType
import com.woo.kuiklycomposetemplate.domain.model.Exercise

/**
 * 训练日动作编排与参数微调 (清晰无截断、可直接点击调起键盘、支持重量设定与波浪/热身自动计算)
 */
@Page("plan_edit", supportInLocal = true)
class PlanEditPager : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ReshapeTheme {
                PlanEditScreen()
            }
        }
    }
}

@Composable
fun PlanEditScreen() {
    val pager = LocalActivity.current.getPager() as? Pager
    val spModule = pager?.acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)
    val userProfile = remember { UserPreferencesStore.getProfile(spModule) }

    var selectedDay by remember { mutableStateOf(PlanRepository.getCurrentDay(spModule)) }
    val exercises = remember(selectedDay) {
        mutableStateListOf<Exercise>().apply {
            addAll(PlanRepository.getExercises(selectedDay, spModule))
        }
    }

    // 计划项微调弹窗状态
    var editingExerciseIndex by remember { mutableStateOf<Int?>(null) }
    var isWaveMode by remember { mutableStateOf(false) }
    var tuningSets by remember { mutableStateOf("4") }
    var tuningTargetReps by remember { mutableStateOf("8") }
    var tuningWaveSequence by remember { mutableStateOf("15/12/10/8") }
    var tuningWeightKg by remember { mutableStateOf("45.0") }
    var tuningRestSeconds by remember { mutableStateOf("120") }
    var tuningRpe by remember { mutableStateOf("8.0") }
    var tuningCues by remember { mutableStateOf("") }
    var tuningWarmupLadder by remember { mutableStateOf(true) }

    // 9 宫格大按键数字键盘抽屉状态
    var numPadVisible by remember { mutableStateOf(false) }
    var numPadMode by remember { mutableStateOf(NumPadInputMode.WEIGHT) }
    var numPadInitial by remember { mutableStateOf("45") }
    var numPadTarget by remember { mutableStateOf("TUNING_WEIGHT") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ReshapeTheme.colors.bgCanvas)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(44.dp))

                // 1. 顶栏 (返回 / 标题 / 保存完成)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(ReshapeTheme.colors.card, RoundedCornerShape(10.dp))
                            .clickable {
                                pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("home")
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = "← 首页", fontSize = 14.sp, color = ReshapeTheme.colors.primary)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "训练计划编排",
                        fontSize = 16.sp,
                        color = ReshapeTheme.colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .background(ReshapeTheme.colors.primary, RoundedCornerShape(10.dp))
                            .clickable {
                                PlanRepository.saveCustomPlan(selectedDay, exercises.toList(), spModule)
                                pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("home")
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(text = "完成保存", fontSize = 13.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. 官方大纲提示与分化日切换 Tab
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "以官方经典模板为基础编排",
                        fontSize = 12.sp,
                        color = ReshapeTheme.colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "📖 查看 ${selectedDay.code} 设计逻辑 ›",
                        fontSize = 12.sp,
                        color = ReshapeTheme.colors.primary,
                        modifier = Modifier.clickable {
                            val data = JSONObject().apply {
                                put("day", when (selectedDay) {
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

                // D1 / D2 / D3 Tab
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf(DayType.PUSH, DayType.PULL, DayType.LEGS).forEachIndexed { index, d ->
                        if (index > 0) Spacer(modifier = Modifier.width(8.dp))
                        val isSelected = d == selectedDay
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .background(
                                    if (isSelected) ReshapeTheme.colors.primary else ReshapeTheme.colors.card,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    PlanRepository.saveCustomPlan(selectedDay, exercises.toList(), spModule)
                                    selectedDay = d
                                    exercises.clear()
                                    exercises.addAll(PlanRepository.getExercises(d, spModule))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${d.code} ${d.title}",
                                fontSize = 13.sp,
                                color = if (isSelected) Color.White else ReshapeTheme.colors.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. 动作列表卡片 (包含组数、次数、重量、间歇与 RPE 规格)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedDay.code} 执行动作 (${exercises.size})",
                                fontSize = 14.sp,
                                color = ReshapeTheme.colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "点击动作可调重量与参数",
                                fontSize = 11.sp,
                                color = ReshapeTheme.colors.textFaint
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (exercises.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "暂无动作，请从下方动作库添加", fontSize = 12.sp, color = ReshapeTheme.colors.textFaint)
                            }
                        }

                        exercises.forEachIndexed { index, ex ->
                            if (index > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF3F4F6)))
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 序号与上下微调
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(24.dp)
                                ) {
                                    if (index > 0) {
                                        Text(
                                            text = "▲",
                                            fontSize = 9.sp,
                                            color = ReshapeTheme.colors.textFaint,
                                            modifier = Modifier.clickable {
                                                val item = exercises.removeAt(index)
                                                exercises.add(index - 1, item)
                                                PlanRepository.saveCustomPlan(selectedDay, exercises.toList(), spModule)
                                            }
                                        )
                                    }
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 13.sp,
                                        color = ReshapeTheme.colors.textSecondary
                                    )
                                    if (index < exercises.size - 1) {
                                        Text(
                                            text = "▼",
                                            fontSize = 9.sp,
                                            color = ReshapeTheme.colors.textFaint,
                                            modifier = Modifier.clickable {
                                                val item = exercises.removeAt(index)
                                                exercises.add(index + 1, item)
                                                PlanRepository.saveCustomPlan(selectedDay, exercises.toList(), spModule)
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // 中间：动作名称 + 规格 (组数 × 次数 · 重量 · 间歇 · RPE)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            editingExerciseIndex = index
                                            isWaveMode = ex.targetRepsSequence.isNotBlank()
                                            tuningSets = ex.defaultSets.toString()
                                            tuningTargetReps = ex.targetRepsMin.toString()
                                            tuningWaveSequence = if (ex.targetRepsSequence.isNotBlank()) ex.targetRepsSequence else "15/12/10/8"
                                            val initWeight = if (ex.targetWeightKg > 0f) ex.targetWeightKg else WeightSuggestEngine.calculateInitialWeightFrom1Rm(ex, userProfile)
                                            tuningWeightKg = initWeight.toString()
                                            tuningRestSeconds = ex.defaultRestSeconds.toString()
                                            tuningRpe = ex.targetRpe.toString()
                                            tuningCues = ex.defaultHoleCues
                                            tuningWarmupLadder = ex.enableWarmupLadder
                                        }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = ex.name,
                                            fontSize = 14.sp,
                                            color = ReshapeTheme.colors.textPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = ex.loadType.label,
                                                fontSize = 10.sp,
                                                color = ReshapeTheme.colors.primary
                                            )
                                        }
                                        if (ex.enableWarmupLadder) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "🪜热身", fontSize = 9.sp, color = Color(0xFFD97706))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))

                                    // 单行全景规格信息：组数 × 次数 · 重量 · 休息 · RPE
                                    Text(
                                        text = "${ex.defaultSets} 组 × ${ex.displayRepsText} · ${ex.displayWeightText} · 休息 ${ex.defaultRestSeconds}s · RPE ${ex.targetRpe}",
                                        fontSize = 11.sp,
                                        color = ReshapeTheme.colors.textSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // 右侧：配置按钮 + 删除按钮
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(6.dp))
                                            .clickable {
                                                editingExerciseIndex = index
                                                isWaveMode = ex.targetRepsSequence.isNotBlank()
                                                tuningSets = ex.defaultSets.toString()
                                                tuningTargetReps = ex.targetRepsMin.toString()
                                                tuningWaveSequence = if (ex.targetRepsSequence.isNotBlank()) ex.targetRepsSequence else "15/12/10/8"
                                                val initWeight = if (ex.targetWeightKg > 0f) ex.targetWeightKg else WeightSuggestEngine.calculateInitialWeightFrom1Rm(ex, userProfile)
                                                tuningWeightKg = initWeight.toString()
                                                tuningRestSeconds = ex.defaultRestSeconds.toString()
                                                tuningRpe = ex.targetRpe.toString()
                                                tuningCues = ex.defaultHoleCues
                                                tuningWarmupLadder = ex.enableWarmupLadder
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(text = "配置", fontSize = 11.sp, color = ReshapeTheme.colors.primary)
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Text(
                                        text = "✕",
                                        fontSize = 13.sp,
                                        color = Color(0xFFEF4444),
                                        modifier = Modifier
                                            .clickable {
                                                exercises.removeAt(index)
                                                PlanRepository.saveCustomPlan(selectedDay, exercises.toList(), spModule)
                                            }
                                            .padding(horizontal = 4.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. 底部动作库挑选与录入
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(ReshapeTheme.colors.primary, RoundedCornerShape(10.dp))
                            .clickable {
                                val data = JSONObject().apply {
                                    put("dayCode", selectedDay.name)
                                }
                                pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("exercise_library", data)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📚 从动作库挑选 (${selectedDay.code})",
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(ReshapeTheme.colors.card, RoundedCornerShape(10.dp))
                            .clickable {
                                val data = JSONObject().apply {
                                    put("dayCode", selectedDay.name)
                                    put("autoCreate", true)
                                }
                                pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("exercise_library", data)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "➕ 录入新动作至动作库",
                            fontSize = 13.sp,
                            color = ReshapeTheme.colors.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. 恢复模板按钮
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            PlanRepository.resetToDefaultPlan(selectedDay, spModule)
                            exercises.clear()
                            exercises.addAll(PlanRepository.getExercises(selectedDay, spModule))
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "↺ 恢复官方经典 5 动作模板",
                        fontSize = 12.sp,
                        color = ReshapeTheme.colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // =========================================================================
        // 5. 计划项参数微调 Modal (做组重量设定、波浪负荷自动推导与热身重量计算)
        // =========================================================================
        editingExerciseIndex?.let { index ->
            val currentEx = exercises.getOrNull(index)
            if (currentEx != null) {
                val base1Rm = when (selectedDay) {
                    DayType.PUSH -> if (userProfile.bench1RmKg > 0f) userProfile.bench1RmKg else 70.0f
                    DayType.LEGS -> if (userProfile.squat1RmKg > 0f) userProfile.squat1RmKg else 90.0f
                    DayType.PULL -> if (userProfile.deadlift1RmKg > 0f) userProfile.deadlift1RmKg else 120.0f
                    DayType.REST -> 70.0f
                }

                val currentWorkingWeight = tuningWeightKg.toFloatOrNull() ?: 45.0f

                val currentRepsList = if (isWaveMode) {
                    val parsed = tuningWaveSequence.split("/", "-", "–", ",").mapNotNull { it.trim().toIntOrNull() }
                    if (parsed.isNotEmpty()) parsed else listOf(15, 12, 10, 8)
                } else {
                    val rep = tuningTargetReps.toIntOrNull() ?: 8
                    val sets = tuningSets.toIntOrNull() ?: 4
                    List(sets.coerceAtLeast(1)) { rep }
                }

                // 实时推算波浪逐组建议重量 (以当前设定的首组重量/1RM推导)
                val waveSuggestions = WeightSuggestEngine.calculateWaveLoads(
                    oneRmKg = base1Rm,
                    repsSequence = currentRepsList,
                    targetRpe = tuningRpe.toFloatOrNull() ?: 8.0f,
                    baseFirstSetWeightKg = currentWorkingWeight
                )

                // 实时计算 3 阶科学热身重量
                val warmupSteps = WeightSuggestEngine.generateWarmupLadder(currentWorkingWeight)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x88000000))
                        .clickable { editingExerciseIndex = null },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .fillMaxHeight(0.90f)
                            .background(Color.White, RoundedCornerShape(18.dp))
                            .clickable(enabled = false) {}
                            .padding(16.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                // ① 弹窗顶栏
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentEx.name,
                                            fontSize = 16.sp,
                                            color = ReshapeTheme.colors.textPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${selectedDay.code} ${selectedDay.title}日执行配置",
                                            fontSize = 11.sp,
                                            color = ReshapeTheme.colors.textFaint
                                        )
                                    }
                                    Text(
                                        text = "✕",
                                        fontSize = 18.sp,
                                        color = ReshapeTheme.colors.textFaint,
                                        modifier = Modifier
                                            .clickable { editingExerciseIndex = null }
                                            .padding(8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // ② 做组模式选择 (恒定 vs 波浪)
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .background(
                                                if (!isWaveMode) ReshapeTheme.colors.primary else Color(0xFFF3F4F6),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { isWaveMode = false },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "恒定做组 (如 4×8)",
                                            fontSize = 12.sp,
                                            color = if (!isWaveMode) Color.White else ReshapeTheme.colors.textSecondary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .background(
                                                if (isWaveMode) ReshapeTheme.colors.primary else Color(0xFFF3F4F6),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                isWaveMode = true
                                                val parsed = tuningWaveSequence.split("/", "-", "–", ",").mapNotNull { it.trim().toIntOrNull() }
                                                if (parsed.isNotEmpty()) tuningSets = parsed.size.toString()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "🌊 波浪递进 (15/12/10/8)",
                                            fontSize = 12.sp,
                                            color = if (isWaveMode) Color.White else ReshapeTheme.colors.textSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // ③ 计划做组重量输入 (清晰无截断 BasicTextField + 步进器 ±2.5kg)
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isWaveMode) "1. 首组/基准做组重量 (kg)" else "1. 计划做组重量 (kg)",
                                            fontSize = 12.sp,
                                            color = ReshapeTheme.colors.textSecondary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "1RM参考: ${base1Rm}kg",
                                            fontSize = 11.sp,
                                            color = ReshapeTheme.colors.textFaint
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                                            .clickable {
                                                numPadMode = NumPadInputMode.WEIGHT
                                                numPadInitial = tuningWeightKg
                                                numPadTarget = "TUNING_WEIGHT"
                                                numPadVisible = true
                                            }
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        key(tuningWeightKg) {
                                            Text(
                                                text = "${tuningWeightKg} kg ⌨",
                                                fontSize = 15.sp,
                                                color = ReshapeTheme.colors.primary,
                                                textAlign = TextAlign.Center,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // ④ 组数与次数配置
                                if (!isWaveMode) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = "2. 正式组数", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                                            Spacer(modifier = Modifier.height(5.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(38.dp)
                                                    .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        numPadMode = NumPadInputMode.SETS
                                                        numPadInitial = tuningSets
                                                        numPadTarget = "TUNING_SETS"
                                                        numPadVisible = true
                                                    }
                                                    .padding(horizontal = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                key(tuningSets) {
                                                    Text(
                                                        text = "${tuningSets} 组 ⌨",
                                                        fontSize = 15.sp,
                                                        color = ReshapeTheme.colors.primary,
                                                        textAlign = TextAlign.Center,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = "目标每组次数", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                                            Spacer(modifier = Modifier.height(5.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(38.dp)
                                                    .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        numPadMode = NumPadInputMode.REPS
                                                        numPadInitial = tuningTargetReps
                                                        numPadTarget = "TUNING_REPS"
                                                        numPadVisible = true
                                                    }
                                                    .padding(horizontal = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                key(tuningTargetReps) {
                                                    Text(
                                                        text = "${tuningTargetReps} 次 ⌨",
                                                        fontSize = 15.sp,
                                                        color = ReshapeTheme.colors.primary,
                                                        textAlign = TextAlign.Center,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        listOf("5" to "5次(力量)", "8" to "8次(增肌)", "10" to "10次(标准)", "12" to "12次(肥大)", "15" to "15次(耐力)").forEachIndexed { idx, (rVal, label) ->
                                            if (idx > 0) Spacer(modifier = Modifier.width(3.dp))
                                            val isSelected = tuningTargetReps == rVal
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(28.dp)
                                                    .background(
                                                        if (isSelected) ReshapeTheme.colors.primarySoft else Color(0xFFF3F4F6),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .clickable { tuningTargetReps = rVal },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 10.sp,
                                                    color = if (isSelected) ReshapeTheme.colors.primary else ReshapeTheme.colors.textSecondary
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(text = "2. 波浪次数序列 (以 / 分隔)", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        BasicTextField(
                                            value = tuningWaveSequence,
                                            onValueChange = {
                                                tuningWaveSequence = it
                                                val parsed = it.split("/", "-", "–", ",").mapNotNull { s -> s.trim().toIntOrNull() }
                                                if (parsed.isNotEmpty()) tuningSets = parsed.size.toString()
                                            },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = TextStyle(
                                                fontSize = 14.sp,
                                                color = ReshapeTheme.colors.textPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        listOf("15/12/10/8" to "15/12/10/8", "12/10/8/6" to "12/10/8/6", "5/5/5/5/5" to "5×5", "10/8/6" to "10/8/6").forEachIndexed { idx, (seq, label) ->
                                            if (idx > 0) Spacer(modifier = Modifier.width(3.dp))
                                            val isSelected = tuningWaveSequence == seq
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(28.dp)
                                                    .background(
                                                        if (isSelected) ReshapeTheme.colors.primarySoft else Color(0xFFF3F4F6),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .clickable {
                                                        tuningWaveSequence = seq
                                                        val parsed = seq.split("/").mapNotNull { s -> s.toIntOrNull() }
                                                        tuningSets = parsed.size.toString()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 10.sp,
                                                    color = if (isSelected) ReshapeTheme.colors.primary else ReshapeTheme.colors.textSecondary
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // ⑤ 逐组波浪负荷自动推导卡片
                                if (isWaveMode) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF0FDF4), RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = "🌊 波浪负荷推导 (RPE $tuningRpe)", fontSize = 11.sp, color = Color(0xFF15803D))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = "基于首组 ${currentWorkingWeight}kg 递进", fontSize = 10.sp, color = ReshapeTheme.colors.textFaint)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            val waveText = waveSuggestions.mapIndexed { idx, item ->
                                                if (idx == 0) "${item.setIndex}组: ${item.targetReps}次/${item.suggestedWeightKg}kg"
                                                else {
                                                    val diff = item.suggestedWeightKg - waveSuggestions[idx - 1].suggestedWeightKg
                                                    val diffStr = if (diff > 0f) " (+${diff}kg)" else ""
                                                    "${item.setIndex}组: ${item.targetReps}次/${item.suggestedWeightKg}kg$diffStr"
                                                }
                                            }.joinToString(" ➔ ")

                                            Text(
                                                text = waveText,
                                                fontSize = 11.sp,
                                                color = Color(0xFF166534),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                // ⑥ 3 阶科学热身阶梯
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (tuningWarmupLadder) Color(0xFFFFFBEB) else Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        key(tuningWarmupLadder) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { tuningWarmupLadder = !tuningWarmupLadder },
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (tuningWarmupLadder) "☑ 开启 3 阶热身阶梯" else "☐ 开启 3 阶热身阶梯",
                                                    fontSize = 12.sp,
                                                    color = if (tuningWarmupLadder) Color(0xFFB45309) else ReshapeTheme.colors.textSecondary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(text = "零疲劳隔离", fontSize = 10.sp, color = Color(0xFFD97706))
                                            }
                                        }

                                        if (tuningWarmupLadder && warmupSteps.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                warmupSteps.forEachIndexed { sIdx, step ->
                                                    if (sIdx > 0) Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp))
                                                            .padding(vertical = 4.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Text(text = "阶梯 ${step.stepIndex} · ${step.label}", fontSize = 9.sp, color = Color(0xFF92400E))
                                                            Text(
                                                                text = "${step.weightKg}kg × ${step.reps}次",
                                                                fontSize = 11.sp,
                                                                color = Color(0xFF78350F),
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // ⑦ 组间休息与 RPE 强度
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "3. 组间休息 (秒)", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                                        Spacer(modifier = Modifier.height(5.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(38.dp)
                                                .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    numPadMode = NumPadInputMode.REST_SECONDS
                                                    numPadInitial = tuningRestSeconds
                                                    numPadTarget = "TUNING_REST"
                                                    numPadVisible = true
                                                }
                                                .padding(horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            key(tuningRestSeconds) {
                                                Text(
                                                    text = "${tuningRestSeconds} s ⌨",
                                                    fontSize = 15.sp,
                                                    color = ReshapeTheme.colors.primary,
                                                    textAlign = TextAlign.Center,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1.2f)) {
                                        Text(text = "4. 目标 RPE", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                                        Spacer(modifier = Modifier.height(5.dp))
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            listOf("6.0" to "6", "7.0" to "7", "8.0" to "8★", "9.0" to "9", "10.0" to "10").forEachIndexed { idx, (rVal, label) ->
                                                if (idx > 0) Spacer(modifier = Modifier.width(3.dp))
                                                val isSelected = tuningRpe == rVal || (tuningRpe == "8" && rVal == "8.0")
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(38.dp)
                                                        .background(
                                                            if (isSelected) ReshapeTheme.colors.primary else Color(0xFFF3F4F6),
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable { tuningRpe = rVal },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = label,
                                                        fontSize = 12.sp,
                                                        color = if (isSelected) Color.White else ReshapeTheme.colors.textSecondary,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // ⑧ 动作口诀备忘
                                Text(text = "5. 动作孔位与口诀备忘", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                                Spacer(modifier = Modifier.height(5.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                        .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    BasicTextField(
                                        value = tuningCues,
                                        onValueChange = { tuningCues = it },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = TextStyle(
                                            fontSize = 13.sp,
                                            color = ReshapeTheme.colors.textPrimary
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // ⑨ 底部主保存按钮
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .background(ReshapeTheme.colors.primary, RoundedCornerShape(10.dp))
                                        .clickable {
                                            val reps = tuningTargetReps.toIntOrNull() ?: 8
                                            val waveSeq = if (isWaveMode) tuningWaveSequence.trim() else ""
                                            val totalSets = if (isWaveMode) currentRepsList.size else (tuningSets.toIntOrNull() ?: 4)
                                            val weight = tuningWeightKg.toFloatOrNull() ?: 45.0f
                                            val waveWeights = if (isWaveMode) {
                                                waveSuggestions.joinToString("/") { it.suggestedWeightKg.toString() }
                                            } else ""

                                            val updated = currentEx.copy(
                                                defaultSets = totalSets,
                                                targetRepsMin = reps,
                                                targetRepsMax = reps,
                                                targetRepsSequence = waveSeq,
                                                targetWeightKg = weight,
                                                waveWeightsSequence = waveWeights,
                                                defaultRestSeconds = tuningRestSeconds.toIntOrNull() ?: 120,
                                                targetRpe = tuningRpe.toFloatOrNull() ?: 8.0f,
                                                defaultHoleCues = tuningCues.trim(),
                                                enableWarmupLadder = tuningWarmupLadder
                                            )
                                            exercises[index] = updated
                                            PlanRepository.saveCustomPlan(selectedDay, exercises.toList(), spModule)
                                            editingExerciseIndex = null
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "确认并应用至 ${selectedDay.code} 计划",
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 健身房数字键盘抽屉
                GymNumPadDrawer(
                    visible = numPadVisible,
                    mode = numPadMode,
                    initialValue = numPadInitial,
                    onDismiss = { numPadVisible = false },
                    onConfirm = { newVal ->
                        numPadVisible = false
                        val formatted = if (newVal % 1.0f == 0f) newVal.toInt().toString() else newVal.toString()
                        when (numPadTarget) {
                            "TUNING_WEIGHT" -> tuningWeightKg = formatted
                            "TUNING_SETS" -> tuningSets = newVal.toInt().toString()
                            "TUNING_REPS" -> tuningTargetReps = newVal.toInt().toString()
                            "TUNING_REST" -> tuningRestSeconds = newVal.toInt().toString()
                        }
                    }
                )
            }
        }
    }
}
