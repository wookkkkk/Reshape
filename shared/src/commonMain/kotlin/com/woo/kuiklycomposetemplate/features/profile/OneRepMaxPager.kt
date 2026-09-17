package com.woo.kuiklycomposetemplate.features.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
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
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.pager.Pager
import com.woo.kuiklycomposetemplate.core.components.GymNumPadDrawer
import com.woo.kuiklycomposetemplate.core.components.NumPadInputMode
import com.woo.kuiklycomposetemplate.core.theme.ReshapeTheme
import com.woo.kuiklycomposetemplate.data.local.LiftEstimateHistory
import com.woo.kuiklycomposetemplate.data.local.PlanRepository
import com.woo.kuiklycomposetemplate.data.local.UserPreferencesStore
import com.woo.kuiklycomposetemplate.domain.algorithm.BrzyckiCalculator
import com.woo.kuiklycomposetemplate.domain.algorithm.DotsScoreCalculator
import com.woo.kuiklycomposetemplate.domain.model.Gender

/**
 * 三大项 1RM 档案与建议重量推算依据 (对齐 one-rep-max.html 与 PRD 4.1/4.6 规范，支持秒级落盘与反推历史自动恢复)
 */
@Page("one_rep_max", supportInLocal = true)
class OneRepMaxPager : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ReshapeTheme {
                OneRepMaxScreen()
            }
        }
    }
}

@Composable
fun OneRepMaxScreen() {
    val pager = LocalActivity.current.getPager() as? Pager
    val spModule = pager?.acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)

    var currentProfile by remember { mutableStateOf(UserPreferencesStore.getProfile(spModule)) }
    var bench1Rm by remember { mutableStateOf(currentProfile.bench1RmKg) }
    var squat1Rm by remember { mutableStateOf(currentProfile.squat1RmKg) }
    var deadlift1Rm by remember { mutableStateOf(currentProfile.deadlift1RmKg) }

    // 保存反馈提示
    var saveTip by remember { mutableStateOf<String?>(null) }

    // 反推计算器展开状态 (默认展开深蹲)
    var expandedLiftKey by remember { mutableStateOf<String?>("squat") }

    // 从本地存储读取历史做组重量与次数记录
    val initialHistory = remember { UserPreferencesStore.getEstimateHistory(spModule) }

    // 深蹲反推临时输入（历史记忆）
    var squatEstWeight by remember { mutableStateOf(initialHistory.squatWeight) }
    var squatEstReps by remember { mutableStateOf(initialHistory.squatReps) }

    // 卧推反推临时输入（历史记忆）
    var benchEstWeight by remember { mutableStateOf(initialHistory.benchWeight) }
    var benchEstReps by remember { mutableStateOf(initialHistory.benchReps) }

    // 硬拉反推临时输入（历史记忆）
    var deadliftEstWeight by remember { mutableStateOf(initialHistory.deadliftWeight) }
    var deadliftEstReps by remember { mutableStateOf(initialHistory.deadliftReps) }

    // 9 宫格大按键键盘抽屉状态
    var numPadVisible by remember { mutableStateOf(false) }
    var numPadMode by remember { mutableStateOf(NumPadInputMode.WEIGHT) }
    var numPadInitial by remember { mutableStateOf("60") }
    var currentEditingTarget by remember { mutableStateOf<String?>("SQUAT_WEIGHT") }

    val total1Rm = bench1Rm + squat1Rm + deadlift1Rm
    val dotsScore = DotsScoreCalculator.calculateDots(total1Rm, currentProfile.bodyWeightKg, currentProfile.gender == Gender.MALE)
    val strengthTitle = DotsScoreCalculator.getStrengthTierTitle(dotsScore, currentProfile.gender == Gender.MALE)

    fun persistAllChanges() {
        val updated = currentProfile.copy(
            bench1RmKg = bench1Rm,
            squat1RmKg = squat1Rm,
            deadlift1RmKg = deadlift1Rm
        )
        currentProfile = updated
        UserPreferencesStore.saveProfile(updated, spModule)
        UserPreferencesStore.saveEstimateHistory(
            LiftEstimateHistory(
                squatWeight = squatEstWeight,
                squatReps = squatEstReps,
                benchWeight = benchEstWeight,
                benchReps = benchEstReps,
                deadliftWeight = deadliftEstWeight,
                deadliftReps = deadliftEstReps
            ),
            spModule
        )
        PlanRepository.resetToDefault(spModule = spModule)
    }

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

                // 1. 顶栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(ReshapeTheme.colors.card, RoundedCornerShape(10.dp))
                            .clickable {
                                persistAllChanges()
                                pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("profile")
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = "← 返回", fontSize = 13.sp, color = ReshapeTheme.colors.textSecondary)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "三大项数据 · 推算依据",
                        fontSize = 15.sp,
                        color = ReshapeTheme.colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .background(ReshapeTheme.colors.primary, RoundedCornerShape(10.dp))
                            .clickable {
                                persistAllChanges()
                                pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("profile")
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(text = "保存并返回", fontSize = 13.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. 页面定位与说明
                Text(text = "建议重量的推算依据", fontSize = 13.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = "你的三大项基准", fontSize = 22.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "填一次就够。之后每个动作的建议重量都从这三个数算出来，练完 8 次一个周期后再回来重测。",
                    fontSize = 12.sp,
                    color = ReshapeTheme.colors.textSecondary,
                    lineHeight = 17.sp
                )

                if (saveTip != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFECFDF5), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = saveTip ?: "", fontSize = 12.sp, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. 三大项数据卡片 (深蹲 / 卧推 / 硬拉，支持直接填 1RM 或用 Brzycki 公式反推)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // ① 深蹲 (SQUAT)
                        LiftEstimateCard(
                            liftName = "杠铃后深蹲",
                            current1RmKg = squat1Rm,
                            isExpanded = expandedLiftKey == "squat",
                            onToggleExpand = {
                                expandedLiftKey = if (expandedLiftKey == "squat") null else "squat"
                            },
                            weightInput = squatEstWeight,
                            repsInput = squatEstReps,
                            onClickDirect1Rm = {
                                numPadMode = NumPadInputMode.SQUAT_1RM
                                numPadInitial = if (squat1Rm % 1.0f == 0f) squat1Rm.toInt().toString() else squat1Rm.toString()
                                currentEditingTarget = "SQUAT_1RM"
                                numPadVisible = true
                            },
                            onClickWeightInput = {
                                numPadMode = NumPadInputMode.WEIGHT
                                numPadInitial = squatEstWeight
                                currentEditingTarget = "SQUAT_WEIGHT"
                                numPadVisible = true
                            },
                            onClickRepsInput = {
                                numPadMode = NumPadInputMode.REPS
                                numPadInitial = squatEstReps
                                currentEditingTarget = "SQUAT_REPS"
                                numPadVisible = true
                            },
                            onDirectChange1Rm = { delta ->
                                squat1Rm = (squat1Rm + delta).coerceAtLeast(20f)
                                persistAllChanges()
                                saveTip = "深蹲 1RM 已更新为 ${squat1Rm.toInt()}kg 并保存 ✓"
                            },
                            onApplyEstimated = { est ->
                                squat1Rm = est
                                persistAllChanges()
                                saveTip = "深蹲 1RM 已成功应用为 ${est}kg 并实时落盘 ✓"
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF3F4F6)))
                        Spacer(modifier = Modifier.height(10.dp))

                        // ② 卧推 (BENCH)
                        LiftEstimateCard(
                            liftName = "杠铃平板卧推",
                            current1RmKg = bench1Rm,
                            isExpanded = expandedLiftKey == "bench",
                            onToggleExpand = {
                                expandedLiftKey = if (expandedLiftKey == "bench") null else "bench"
                            },
                            weightInput = benchEstWeight,
                            repsInput = benchEstReps,
                            onClickDirect1Rm = {
                                numPadMode = NumPadInputMode.BENCH_1RM
                                numPadInitial = if (bench1Rm % 1.0f == 0f) bench1Rm.toInt().toString() else bench1Rm.toString()
                                currentEditingTarget = "BENCH_1RM"
                                numPadVisible = true
                            },
                            onClickWeightInput = {
                                numPadMode = NumPadInputMode.WEIGHT
                                numPadInitial = benchEstWeight
                                currentEditingTarget = "BENCH_WEIGHT"
                                numPadVisible = true
                            },
                            onClickRepsInput = {
                                numPadMode = NumPadInputMode.REPS
                                numPadInitial = benchEstReps
                                currentEditingTarget = "BENCH_REPS"
                                numPadVisible = true
                            },
                            onDirectChange1Rm = { delta ->
                                bench1Rm = (bench1Rm + delta).coerceAtLeast(20f)
                                persistAllChanges()
                                saveTip = "卧推 1RM 已更新为 ${bench1Rm.toInt()}kg 并保存 ✓"
                            },
                            onApplyEstimated = { est ->
                                bench1Rm = est
                                persistAllChanges()
                                saveTip = "卧推 1RM 已成功应用为 ${est}kg 并实时落盘 ✓"
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF3F4F6)))
                        Spacer(modifier = Modifier.height(10.dp))

                        // ③ 硬拉 (DEADLIFT)
                        LiftEstimateCard(
                            liftName = "传统/罗马尼亚硬拉",
                            current1RmKg = deadlift1Rm,
                            isExpanded = expandedLiftKey == "deadlift",
                            onToggleExpand = {
                                expandedLiftKey = if (expandedLiftKey == "deadlift") null else "deadlift"
                            },
                            weightInput = deadliftEstWeight,
                            repsInput = deadliftEstReps,
                            onClickDirect1Rm = {
                                numPadMode = NumPadInputMode.DEADLIFT_1RM
                                numPadInitial = if (deadlift1Rm % 1.0f == 0f) deadlift1Rm.toInt().toString() else deadlift1Rm.toString()
                                currentEditingTarget = "DEADLIFT_1RM"
                                numPadVisible = true
                            },
                            onClickWeightInput = {
                                numPadMode = NumPadInputMode.WEIGHT
                                numPadInitial = deadliftEstWeight
                                currentEditingTarget = "DEADLIFT_WEIGHT"
                                numPadVisible = true
                            },
                            onClickRepsInput = {
                                numPadMode = NumPadInputMode.REPS
                                numPadInitial = deadliftEstReps
                                currentEditingTarget = "DEADLIFT_REPS"
                                numPadVisible = true
                            },
                            onDirectChange1Rm = { delta ->
                                deadlift1Rm = (deadlift1Rm + delta).coerceAtLeast(20f)
                                persistAllChanges()
                                saveTip = "硬拉 1RM 已更新为 ${deadlift1Rm.toInt()}kg 并保存 ✓"
                            },
                            onApplyEstimated = { est ->
                                deadlift1Rm = est
                                persistAllChanges()
                                saveTip = "硬拉 1RM 已成功应用为 ${est}kg 并实时落盘 ✓"
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. 建议重量是怎么来的
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💡 建议重量是怎么来的", fontSize = 14.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val rules = listOf(
                            "① 每个动作有专属生物力学换算系数，先按主项 1RM 折算，再按目标次数取 RPE 8 对应百分比。",
                            "② 所有输出结果一律按 2.5 kg 步长取整。",
                            "③ 哑铃动作的换算系数已包含单手折算。",
                            "④ 主项每次训练 +2.5 kg，8 次一个周期。",
                            "⑤ 自建动作严格按你在计划中指定的起始重量执行。"
                        )

                        rules.forEachIndexed { rIdx, rule ->
                            if (rIdx > 0) Spacer(modifier = Modifier.height(6.dp))
                            Text(text = rule, fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary, lineHeight = 17.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. 力量水平评级
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📊 力量水平与 DOTS 评级", fontSize = 14.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = "男女多项式矩阵", fontSize = 10.sp, color = ReshapeTheme.colors.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val currentKg = currentProfile.bodyWeightKg
                        val isMale = currentProfile.gender == Gender.MALE

                        val tiers = listOf(
                            TierInfo("王者级 (Elite)", "> 450 分", "总计约 ${(currentKg * if (isMale) 6.0f else 4.5f).toInt()} kg+ (${if (isMale) "6.0x" else "4.5x"}体重)", dotsScore >= 450f),
                            TierInfo("大师级 (Master)", "380 ~ 450 分", "总计约 ${(currentKg * if (isMale) 5.0f else 3.8f).toInt()} ~ ${(currentKg * if (isMale) 6.0f else 4.5f).toInt()} kg (${if (isMale) "5.0x" else "3.8x"}体重)", dotsScore in 380f..449.99f),
                            TierInfo("进阶级 (Advanced)", "300 ~ 380 分", "总计约 ${(currentKg * if (isMale) 4.0f else 3.0f).toInt()} ~ ${(currentKg * if (isMale) 5.0f else 3.8f).toInt()} kg (${if (isMale) "4.0x" else "3.0x"}体重)", dotsScore in 300f..379.99f),
                            TierInfo("中级 (Intermediate)", "220 ~ 300 分", "总计约 ${(currentKg * if (isMale) 3.0f else 2.2f).toInt()} ~ ${(currentKg * if (isMale) 4.0f else 3.0f).toInt()} kg (${if (isMale) "3.0x" else "2.2x"}体重)", dotsScore in 220f..299.99f),
                            TierInfo("初学级 (Novice)", "< 220 分", "总计 < ${(currentKg * if (isMale) 3.0f else 2.2f).toInt()} kg (初学力量起点)", dotsScore < 220f)
                        )

                        tiers.forEachIndexed { tIdx, tier ->
                            if (tIdx > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF3F4F6)))
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (tier.isCurrent) ReshapeTheme.colors.primarySoft else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = if (tier.isCurrent) 8.dp else 0.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = tier.title,
                                            fontSize = 13.sp,
                                            fontWeight = if (tier.isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            color = if (tier.isCurrent) ReshapeTheme.colors.primary else ReshapeTheme.colors.textPrimary
                                        )
                                        if (tier.isCurrent) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(ReshapeTheme.colors.primary, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(text = "当前水平", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = tier.multiplier, fontSize = 11.sp, color = ReshapeTheme.colors.textFaint)
                                }
                                Text(
                                    text = tier.scoreRange,
                                    fontSize = 12.sp,
                                    color = if (tier.isCurrent) ReshapeTheme.colors.primary else ReshapeTheme.colors.textSecondary,
                                    fontWeight = if (tier.isCurrent) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 6. 本地数据隐私安全保障提示卡
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFECFDF5), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🛡️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "三大项数据仅保存在本机 LocalStorage，不上传任何云端服务器",
                            fontSize = 11.sp,
                            color = Color(0xFF047857)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // 健身房 56dp 9 宫格大按键数字键盘抽屉 (全面替换软键盘输入)
        GymNumPadDrawer(
            visible = numPadVisible,
            mode = numPadMode,
            initialValue = numPadInitial,
            onDismiss = { numPadVisible = false },
            onConfirm = { newVal ->
                numPadVisible = false
                val formatted = if (newVal % 1.0f == 0f) newVal.toInt().toString() else newVal.toString()
                when (currentEditingTarget) {
                    "SQUAT_1RM" -> {
                        squat1Rm = newVal
                        persistAllChanges()
                        saveTip = "深蹲 1RM 已更新为 ${squat1Rm.toInt()}kg 并保存 ✓"
                    }
                    "BENCH_1RM" -> {
                        bench1Rm = newVal
                        persistAllChanges()
                        saveTip = "卧推 1RM 已更新为 ${bench1Rm.toInt()}kg 并保存 ✓"
                    }
                    "DEADLIFT_1RM" -> {
                        deadlift1Rm = newVal
                        persistAllChanges()
                        saveTip = "硬拉 1RM 已更新为 ${deadlift1Rm.toInt()}kg 并保存 ✓"
                    }
                    "SQUAT_WEIGHT" -> squatEstWeight = formatted
                    "SQUAT_REPS" -> squatEstReps = newVal.toInt().toString()
                    "BENCH_WEIGHT" -> benchEstWeight = formatted
                    "BENCH_REPS" -> benchEstReps = newVal.toInt().toString()
                    "DEADLIFT_WEIGHT" -> deadliftEstWeight = formatted
                    "DEADLIFT_REPS" -> deadliftEstReps = newVal.toInt().toString()
                }
            }
        )
    }
}

private data class TierInfo(
    val title: String,
    val scoreRange: String,
    val multiplier: String,
    val isCurrent: Boolean
)

@Composable
private fun LiftEstimateCard(
    liftName: String,
    current1RmKg: Float,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    weightInput: String,
    repsInput: String,
    onClickDirect1Rm: () -> Unit,
    onClickWeightInput: () -> Unit,
    onClickRepsInput: () -> Unit,
    onDirectChange1Rm: (Float) -> Unit,
    onApplyEstimated: (Float) -> Unit
) {
    val weightVal = weightInput.toFloatOrNull() ?: 0f
    val repsVal = repsInput.toIntOrNull() ?: 1

    // Brzycki 实时估算: 1RM = weight * 36 / (37 - reps)
    val estimated1Rm = if (weightVal > 0f && repsVal in 1..15) {
        BrzyckiCalculator.calculate1RM(weightVal, repsVal, roundToStep = 2.5f)
    } else 0f

    val safeReps = repsVal.coerceIn(1, 15)
    val formulaText = "1RM = 重量 × 36 ÷ (37 − 次数)\n    = ${weightVal.toInt()} × 36 ÷ ${37 - safeReps} ≈ ${estimated1Rm} kg"

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = liftName, fontSize = 14.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isExpanded) "用一组做组重量反推 1RM" else "当前 1RM 基准 (点击数值直接输入)",
                    fontSize = 11.sp,
                    color = ReshapeTheme.colors.textFaint
                )
            }

            key(current1RmKg) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                        .clickable { onClickDirect1Rm() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${current1RmKg.toInt()} kg ⌨",
                        fontSize = 15.sp,
                        color = ReshapeTheme.colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            key(isExpanded) {
                Box(
                    modifier = Modifier
                        .background(if (isExpanded) ReshapeTheme.colors.primarySoft else Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                        .clickable { onToggleExpand() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isExpanded) "收起" else "反推/修改",
                        fontSize = 11.sp,
                        color = if (isExpanded) ReshapeTheme.colors.primary else ReshapeTheme.colors.textSecondary
                    )
                }
            }
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(10.dp))

            // 反推输入行：重量 (kg) + 完成次数 (全面支持点击唤起 9 宫格键盘)
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "做组重量 (kg)", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                            .clickable { onClickWeightInput() }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        key(weightInput) {
                            Text(
                                text = if (weightInput.isBlank()) "点击输入 kg ⌨" else "${weightInput} kg ⌨",
                                fontSize = 14.sp,
                                color = ReshapeTheme.colors.textPrimary,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "完成次数 (推荐3~5次)", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                            .clickable { onClickRepsInput() }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        key(repsInput) {
                            Text(
                                text = if (repsInput.isBlank()) "点击输入次 ⌨" else "${repsInput} 次 ⌨",
                                fontSize = 14.sp,
                                color = ReshapeTheme.colors.textPrimary,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Brzycki 公式推导展示卡片
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                key(formulaText) {
                    Text(
                        text = formulaText,
                        fontSize = 11.sp,
                        color = ReshapeTheme.colors.textSecondary,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                key(estimated1Rm) {
                    Text(
                        text = "估算 1RM ≈ ${estimated1Rm} kg",
                        fontSize = 12.sp,
                        color = ReshapeTheme.colors.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (estimated1Rm > 0f) {
                    Box(
                        modifier = Modifier
                            .background(ReshapeTheme.colors.primary, RoundedCornerShape(6.dp))
                            .clickable { onApplyEstimated(estimated1Rm) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = "应用此 1RM", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        } else {
            // 收起状态下的微调步进器
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                        .clickable { onDirectChange1Rm(-2.5f) }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(text = "−2.5kg", fontSize = 10.sp, color = ReshapeTheme.colors.textSecondary)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                        .clickable { onDirectChange1Rm(2.5f) }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(text = "+2.5kg", fontSize = 10.sp, color = ReshapeTheme.colors.textSecondary)
                }
            }
        }
    }
}
