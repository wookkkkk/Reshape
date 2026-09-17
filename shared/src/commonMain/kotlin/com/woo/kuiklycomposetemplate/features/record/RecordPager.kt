package com.woo.kuiklycomposetemplate.features.record

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
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
import com.woo.kuiklycomposetemplate.core.components.AllExercisesSheet
import com.woo.kuiklycomposetemplate.core.components.FloatingRestDock
import com.woo.kuiklycomposetemplate.core.components.GymNumPadDrawer
import com.woo.kuiklycomposetemplate.core.components.NumPadInputMode
import com.woo.kuiklycomposetemplate.core.components.PainFreeDrawer
import com.woo.kuiklycomposetemplate.core.components.PlateVisualizer
import com.woo.kuiklycomposetemplate.core.components.PrGlowCard
import com.woo.kuiklycomposetemplate.core.components.RestDurationDrawer
import com.woo.kuiklycomposetemplate.core.theme.ReshapeTheme
import com.woo.kuiklycomposetemplate.data.local.PlanRepository
import com.woo.kuiklycomposetemplate.data.local.SessionDraftStore
import com.woo.kuiklycomposetemplate.data.local.TrainingCycleStore
import com.woo.kuiklycomposetemplate.data.local.UserPreferencesStore
import com.woo.kuiklycomposetemplate.data.local.WorkoutHistoryStore
import com.woo.kuiklycomposetemplate.domain.algorithm.VolumeCalculator
import com.woo.kuiklycomposetemplate.domain.model.DayType
import com.woo.kuiklycomposetemplate.domain.model.Exercise
import com.woo.kuiklycomposetemplate.domain.model.PresetExercises
import com.woo.kuiklycomposetemplate.domain.model.SessionExercise
import com.woo.kuiklycomposetemplate.domain.model.SessionMetrics
import com.woo.kuiklycomposetemplate.domain.model.SessionPlanContext
import com.woo.kuiklycomposetemplate.domain.model.SessionSet
import com.woo.kuiklycomposetemplate.domain.model.SessionUserProfile
import com.woo.kuiklycomposetemplate.domain.model.UserProfile
import com.woo.kuiklycomposetemplate.domain.model.WorkoutSession
import kotlinx.coroutines.delay

/**
 * 极简练中记录页 (完整联动计划编排、波浪递进重量、组数、次数、间歇与热身阶梯)
 */
@Page("record", supportInLocal = true)
class RecordPager : ComposeContainer() {

    private var initialDay: DayType? = null

    override fun willInit() {
        super.willInit()
        try {
            val dayStr = pageData.params.optString("day", "")
            if (dayStr.isNotBlank()) {
                initialDay = when (dayStr.uppercase()) {
                    "PUSH", "D1" -> DayType.PUSH
                    "PULL", "D2" -> DayType.PULL
                    "LEGS", "D3", "SQUAT" -> DayType.LEGS
                    else -> null
                }
            }
        } catch (_: Exception) {}

        setContent {
            ReshapeTheme {
                RecordScreen(initialDay)
            }
        }
    }
}

// 练中可变做组状态 (支持热身组 W1/W2 独立编号与差异化专属间歇)
class EditableSetState(
    val setNo: Int,             // 全局做组顺序 (1-based)
    val displayNo: String = "", // 显示组号 ("W1", "W2", "1", "2", "3", "4")
    isWarmup: Boolean = false,
    weightKg: Float = 0f,
    leftReps: Int? = null,
    rightReps: Int? = null,
    reps: Int = 8,
    rpe: Float = 8.0f,
    isCompleted: Boolean = false,
    isPr: Boolean = false,
    val customRestSeconds: Int = 90
) {
    var isWarmup by mutableStateOf(isWarmup)
    var weightKg by mutableStateOf(weightKg)
    var leftReps by mutableStateOf(leftReps)
    var rightReps by mutableStateOf(rightReps)
    var reps by mutableStateOf(reps)
    var rpe by mutableStateOf(rpe)
    var isCompleted by mutableStateOf(isCompleted)
    var isPr by mutableStateOf(isPr)
}

/**
 * 依据动作的编排设置生成初始做组清单 (支持热身阶梯、波浪递进、独立次数与 RPE)
 */
fun generateSetsForExercise(exercise: Exercise, userProfile: UserProfile): List<EditableSetState> {
    val result = mutableListOf<EditableSetState>()
    var globalCounter = 1

    // 1. 如果启用了热身阶梯，且目标重量 >= 40kg，自动生成 1~2 组渐进热身组
    if (exercise.enableWarmupLadder && exercise.targetWeightKg >= 40f) {
        val barWeight = userProfile.barbellWeightKg
        val needTwoWarmups = exercise.targetWeightKg >= 70f

        // 热身 1：空杆或 50% 负荷 x 10 次 (组间歇 30 秒换片加重)
        val w1 = if (exercise.targetWeightKg > 60f) {
            maxOf(barWeight, ((exercise.targetWeightKg * 0.5f) / 2.5f).toInt() * 2.5f)
        } else {
            barWeight
        }
        result.add(
            EditableSetState(
                setNo = globalCounter++,
                displayNo = "W1",
                isWarmup = true,
                weightKg = w1,
                reps = 10,
                rpe = 6.0f,
                customRestSeconds = if (needTwoWarmups) 30 else 60
            )
        )

        // 热身 2：75% 负荷 x 5 次 (若主项重量较高，作为进入正式组前的过渡神经激活组，组间歇 60 秒)
        if (needTwoWarmups) {
            val w2 = ((exercise.targetWeightKg * 0.75f) / 2.5f).toInt() * 2.5f
            result.add(
                EditableSetState(
                    setNo = globalCounter++,
                    displayNo = "W2",
                    isWarmup = true,
                    weightKg = w2,
                    reps = 5,
                    rpe = 7.0f,
                    customRestSeconds = 60
                )
            )
        }
    }

    // 2. 正式组 (精确联动计划编排中的组数、波浪重量、次数与 RPE，序号从 1 开始独立编号)
    val totalWorkingSets = exercise.defaultSets.coerceIn(1, 10)
    val waveWeights = exercise.getPerSetWeightsList()
    val repsList = exercise.getPerSetRepsList()

    for (i in 0 until totalWorkingSets) {
        val setWeight = if (waveWeights.isNotEmpty()) {
            waveWeights.getOrElse(i) { waveWeights.last() }
        } else {
            exercise.targetWeightKg
        }

        val setReps = if (repsList.isNotEmpty()) {
            repsList.getOrElse(i) { repsList.last() }
        } else {
            exercise.targetRepsMin
        }

        result.add(
            EditableSetState(
                setNo = globalCounter++,
                displayNo = "${i + 1}",
                isWarmup = false,
                weightKg = setWeight,
                reps = setReps,
                rpe = exercise.targetRpe,
                customRestSeconds = exercise.defaultRestSeconds
            )
        )
    }

    return result
}

@Composable
fun RecordScreen(initDay: DayType? = null) {
    val pager = LocalActivity.current.getPager() as? Pager
    val spModule = pager?.acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)

    val currentDay = (initDay ?: PlanRepository.getCurrentDay(spModule)).let {
        if (it == DayType.REST) DayType.PUSH else it
    }
    var rawExercises by remember(currentDay) {
        mutableStateOf(
            PlanRepository.getExercises(currentDay, spModule).ifEmpty {
                PresetExercises.getExercisesForDay(currentDay)
            }
        )
    }
    var currentExerciseIndex by remember { mutableStateOf(0) }
    var currentExercise by remember(currentExerciseIndex, currentDay, rawExercises) {
        mutableStateOf(rawExercises.getOrElse(currentExerciseIndex) { rawExercises.first() })
    }

    val userProfile = UserPreferencesStore.getProfile(spModule)

    // 全局多动作组状态维护 (避免切换动作时数据被重新初始化丢失)
    val exerciseSetsMap = remember(currentDay.code) {
        val map = mutableMapOf<String, MutableList<EditableSetState>>()
        rawExercises.forEach { ex ->
            map[ex.id] = generateSetsForExercise(ex, userProfile).toMutableList()
        }
        map
    }
    var stateRefreshKey by remember { mutableStateOf(0) }

    val currentSets = exerciseSetsMap.getOrPut(currentExercise.id) {
        generateSetsForExercise(currentExercise, userProfile).toMutableList()
    }

    // 抽屉与弹窗显隐控制
    var numPadVisible by remember { mutableStateOf(false) }
    var numPadMode by remember { mutableStateOf(NumPadInputMode.WEIGHT) }
    var editingSetIndex by remember { mutableStateOf(0) }
    var editingIsLeftReps by remember { mutableStateOf(false) }

    var plateVisualizerVisible by remember { mutableStateOf(false) }
    var painFreeVisible by remember { mutableStateOf(false) }
    var restDurationVisible by remember { mutableStateOf(false) }
    var allExercisesSheetVisible by remember { mutableStateOf(false) }
    var showPrCard by remember { mutableStateOf(false) }
    var prWeight by remember { mutableStateOf(0f) }
    var isRibbonExpanded by remember { mutableStateOf(false) }

    // 休息倒计时状态 (自动同步当前动作编排的组间休息时长与热身短间歇)
    var restActive by remember { mutableStateOf(false) }
    var restTitle by remember { mutableStateOf("⏳ 组间休息") }
    var remainingSeconds by remember { mutableStateOf(currentExercise.defaultRestSeconds) }
    var totalRestSeconds by remember { mutableStateOf(currentExercise.defaultRestSeconds) }

    // 秒表计时 (从 00:00 开始真实秒级累加)
    var elapsedSeconds by remember { mutableStateOf(0) }

    // 聚合全场所有动作做组数据为 SessionExercise 列表
    fun buildAllSessionExercises(): List<SessionExercise> {
        val completedList = rawExercises.mapNotNull { ex ->
            val sets = exerciseSetsMap[ex.id] ?: emptyList()
            val hasAnyCompleted = sets.any { it.isCompleted }
            if (hasAnyCompleted) {
                SessionExercise(
                    exerciseId = ex.id,
                    name = ex.name,
                    category = ex.targetMuscle.ifBlank { ex.primaryMuscle },
                    isUnilateral = ex.isUnilateral,
                    sets = sets.map { s ->
                        SessionSet(
                            setNo = s.setNo,
                            isWarmup = s.isWarmup,
                            weightKg = s.weightKg,
                            leftReps = s.leftReps,
                            rightReps = s.rightReps,
                            effectiveReps = s.reps,
                            rpe = s.rpe,
                            isPr = s.isPr
                        )
                    }
                )
            } else null
        }

        return if (completedList.isNotEmpty()) {
            completedList
        } else {
            // 兜底：若尚未点击完成任何组，取当前屏幕动作
            listOf(
                SessionExercise(
                    exerciseId = currentExercise.id,
                    name = currentExercise.name,
                    category = currentExercise.targetMuscle.ifBlank { currentExercise.primaryMuscle },
                    isUnilateral = currentExercise.isUnilateral,
                    sets = currentSets.map { s ->
                        SessionSet(
                            setNo = s.setNo,
                            isWarmup = s.isWarmup,
                            weightKg = s.weightKg,
                            effectiveReps = s.reps,
                            rpe = s.rpe,
                            isPr = s.isPr
                        )
                    }
                )
            )
        }
    }

    // 同步练中草稿至 SP (断电与闪退防丢自愈)
    fun syncDraft() {
        val allSessionExercises = buildAllSessionExercises()
        val now = com.woo.kuiklycomposetemplate.core.utils.platformCurrentTimeMillis()
        val session = WorkoutSession(
            sessionId = "draft_${now}",
            userProfile = SessionUserProfile(
                bodyWeightKg = userProfile.bodyWeightKg,
                gender = userProfile.gender.name
            ),
            planContext = SessionPlanContext(
                dayCode = currentDay.code,
                dayTitle = currentDay.title
            ),
            sessionMetrics = SessionMetrics(durationMinutes = maxOf(1, elapsedSeconds / 60)),
            exercises = allSessionExercises,
            startedAt = now - elapsedSeconds * 1000L,
            isDraft = true
        )
        SessionDraftStore.saveDraft(session, spModule)
    }

    // 完成训练结算落盘
    fun finishAndArchiveSession() {
        val allSessionExercises = buildAllSessionExercises()
        val totalTonnage = VolumeCalculator.calculateTotalVolume(allSessionExercises, userProfile.bodyWeightKg)
        val metrics = SessionMetrics(
            durationMinutes = maxOf(1, elapsedSeconds / 60),
            totalTonnageKg = totalTonnage,
            workingSetsCount = VolumeCalculator.countWorkingSets(allSessionExercises),
            warmupSetsCount = VolumeCalculator.countWarmupSets(allSessionExercises),
            prBreakCount = VolumeCalculator.countPrSets(allSessionExercises)
        )
        val now = com.woo.kuiklycomposetemplate.core.utils.platformCurrentTimeMillis()
        val session = WorkoutSession(
            sessionId = "session_${now}",
            userProfile = SessionUserProfile(
                bodyWeightKg = userProfile.bodyWeightKg,
                gender = userProfile.gender.name
            ),
            planContext = SessionPlanContext(
                dayCode = currentDay.code,
                dayTitle = currentDay.title
            ),
            sessionMetrics = metrics,
            exercises = allSessionExercises,
            startedAt = now - elapsedSeconds * 1000L,
            finishedAt = now,
            isDraft = false
        )
        WorkoutHistoryStore.archiveSession(session, spModule)
        SessionDraftStore.clearDraft(spModule)
        // 自动推进 8 次中周期进度
        TrainingCycleStore.advanceCycleSession(spModule)
        // 自动将训练循环推进到下一天
        PlanRepository.advanceToNextDay(spModule)
        val params = JSONObject().apply {
            put("sessionId", session.sessionId)
        }
        pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("summary", params)
    }

    // 切换到下一个动作或自动结算
    fun advanceToNextExerciseOrFinish() {
        restActive = false
        remainingSeconds = 0
        if (currentExerciseIndex < rawExercises.size - 1) {
            currentExerciseIndex++
        } else {
            finishAndArchiveSession()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSeconds++
            if (restActive && remainingSeconds > 0) {
                remainingSeconds--
                if (remainingSeconds <= 0) {
                    restActive = false
                    // 若当前动作全部做组均已打卡完成，倒计时结束后自动流转到下一个动作或结算
                    val allCompleted = currentSets.all { it.isCompleted }
                    if (allCompleted) {
                        advanceToNextExerciseOrFinish()
                    }
                }
            }
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
                Spacer(modifier = Modifier.height(44.dp))

                // 顶栏 (退出 / 动作清单 / 结束并记录)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "‹ 退出",
                        fontSize = 15.sp,
                        color = ReshapeTheme.colors.textSecondary,
                        modifier = Modifier
                            .clickable {
                                syncDraft()
                                pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("home")
                            }
                            .padding(end = 12.dp)
                    )
                    Text(
                        text = "${currentDay.icon} ${currentDay.code} · ${currentDay.title} (${currentExerciseIndex + 1}/${rawExercises.size})",
                        fontSize = 14.sp,
                        color = ReshapeTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { allExercisesSheetVisible = true }
                            .weight(1f)
                    )
                    Text(
                        text = "完成训练 ➔",
                        fontSize = 15.sp,
                        color = ReshapeTheme.colors.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                finishAndArchiveSession()
                            }
                    )
                }

                // 1. 顶部动作导航条（默认折叠，极简省空间，可展开横向秒切）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    if (!isRibbonExpanded) {
                        // 【默认折叠态】：极简紧凑条，显示当前动作序号与总进度，右侧提供展开按钮
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ReshapeTheme.colors.card, RoundedCornerShape(10.dp))
                                .clickable { isRibbonExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "动作 ${currentExerciseIndex + 1}/${rawExercises.size}",
                                    fontSize = 11.sp,
                                    color = ReshapeTheme.colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentExercise.name,
                                fontSize = 13.sp,
                                color = ReshapeTheme.colors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                modifier = Modifier
                                    .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "切换动作",
                                    fontSize = 11.sp,
                                    color = ReshapeTheme.colors.textSecondary
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "▾",
                                    fontSize = 12.sp,
                                    color = ReshapeTheme.colors.textSecondary
                                )
                            }
                        }
                    } else {
                        // 【展开态】：2 列全景网格面板 (Panoramic Grid Matrix)，一屏全览零横滑，直觉点选秒切即收起
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ReshapeTheme.colors.card, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📋 今日动作全览 (${rawExercises.size}) · 点选直达",
                                    fontSize = 12.sp,
                                    color = ReshapeTheme.colors.textSecondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "收起 ▴",
                                    fontSize = 12.sp,
                                    color = ReshapeTheme.colors.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { isRibbonExpanded = false }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            // 2 列全景平铺，4~6 个动作全屏尽收眼底
                            val exercisePairs = rawExercises.chunked(2)
                            exercisePairs.forEachIndexed { pairIndex, pair ->
                                if (pairIndex > 0) Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    pair.forEachIndexed { inRowIndex, ex ->
                                        val realIdx = pairIndex * 2 + inRowIndex
                                        val sets = exerciseSetsMap[ex.id] ?: emptyList()
                                        val completedWorking = sets.count { !it.isWarmup && it.isCompleted }
                                        val totalWorking = sets.count { !it.isWarmup }
                                        val isSelected = realIdx == currentExerciseIndex
                                        val isDone = completedWorking == totalWorking && totalWorking > 0

                                        val cardBg = when {
                                            isSelected -> ReshapeTheme.colors.primary
                                            isDone -> Color(0xFFECFDF5)
                                            else -> ReshapeTheme.colors.bgCanvas
                                        }
                                        val titleColor = when {
                                            isSelected -> Color.White
                                            isDone -> Color(0xFF065F46)
                                            else -> ReshapeTheme.colors.textPrimary
                                        }
                                        val badgeBg = when {
                                            isSelected -> Color(0x33FFFFFF)
                                            isDone -> Color(0xFF10B981)
                                            else -> ReshapeTheme.colors.card
                                        }
                                        val badgeTextColor = when {
                                            isSelected || isDone -> Color.White
                                            else -> ReshapeTheme.colors.textSecondary
                                        }
                                        val statusText = if (isDone) "✓" else "$completedWorking/$totalWorking"

                                        if (inRowIndex > 0) Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(cardBg, RoundedCornerShape(10.dp))
                                                .clickable {
                                                    currentExerciseIndex = realIdx
                                                    isRibbonExpanded = false
                                                }
                                                .padding(horizontal = 10.dp, vertical = 9.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${realIdx + 1}. ${ex.name}",
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = titleColor,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(badgeBg, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = statusText,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = badgeTextColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(modifier = Modifier.weight(1f))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 晚高峰器械调换入口
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(8.dp))
                                    .clickable {
                                        isRibbonExpanded = false
                                        allExercisesSheetVisible = true
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⇄ 晚高峰器械被占？点击调换动作顺序 ➔",
                                    fontSize = 11.sp,
                                    color = ReshapeTheme.colors.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // 进度与时钟条
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val completedWorkingCount = currentSets.count { !it.isWarmup && it.isCompleted }
                    val totalWorkingCount = currentSets.count { !it.isWarmup }
                    val completedWarmupCount = currentSets.count { it.isWarmup && it.isCompleted }
                    val totalWarmupCount = currentSets.count { it.isWarmup }
                    val progressText = if (totalWarmupCount > 0) {
                        "已完成 $completedWorkingCount/$totalWorkingCount 组正式组 (热身 $completedWarmupCount/$totalWarmupCount)"
                    } else {
                        "已完成 $completedWorkingCount/$totalWorkingCount 组正式做组"
                    }

                    Text(
                        text = "$progressText · RPE ${currentExercise.targetRpe}",
                        fontSize = 12.sp,
                        color = ReshapeTheme.colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    WorkoutStopwatchDisplay(elapsedSecondsProvider = { elapsedSeconds })
                }

                Spacer(modifier = Modifier.height(6.dp))

                // NEW PR 激励高光卡
                if (showPrCard) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        PrGlowCard(
                            exerciseName = currentExercise.name,
                            weightKg = prWeight,
                            reps = 8,
                            previousPrKg = prWeight - 2.5f,
                            onDismiss = { showPrCard = false }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 动作主卡片
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${currentDay.title} · 第 ${currentExerciseIndex + 1}/${rawExercises.size} 动作",
                                fontSize = 13.sp,
                                color = ReshapeTheme.colors.primary,
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

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentExercise.name,
                                fontSize = 20.sp,
                                color = ReshapeTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "🧮 装片指南",
                                fontSize = 12.sp,
                                color = ReshapeTheme.colors.textSecondary,
                                modifier = Modifier
                                    .clickable { plateVisualizerVisible = true }
                                    .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // 孔位备忘条
                        if (currentExercise.defaultHoleCues.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "📌 ${currentExercise.defaultHoleCues}",
                                    fontSize = 12.sp,
                                    color = ReshapeTheme.colors.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // 做组记录列表 (区分渐进热身阶梯与正式做组，支持 Ghost Set 幽灵组同屏对标)
                        currentSets.forEachIndexed { index, s ->
                            val isCurrentSet = !s.isCompleted && (index == 0 || currentSets[index - 1].isCompleted)

                            // 1. 若当前是第一组热身组，渲染「🔥 渐进热身阶梯」分栏标题
                            if (index == 0 && s.isWarmup) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🔥 渐进热身阶梯",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "换片极速间歇 30s~60s",
                                        fontSize = 11.sp,
                                        color = ReshapeTheme.colors.textSecondary
                                    )
                                }
                            }

                            // 2. 若从热身组过渡到第一组正式组，渲染「⚡ 正式做组」分栏标题
                            if (index > 0 && currentSets[index - 1].isWarmup && !s.isWarmup) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚡ 正式做组 (Working Sets)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ReshapeTheme.colors.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "标准间歇 ${currentExercise.defaultRestSeconds}s",
                                        fontSize = 11.sp,
                                        color = ReshapeTheme.colors.textSecondary
                                    )
                                }
                            }

                            // 幽灵对标数据：上次该组真实记录
                            val ghostWeight = if (s.isWarmup) s.weightKg else (s.weightKg - 2.5f).coerceAtLeast(s.weightKg * 0.9f)
                            val ghostReps = s.reps

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(
                                        when {
                                            s.isCompleted -> Color(0xFFF3F4F6)
                                            isCurrentSet -> if (s.isWarmup) Color(0xFFFFFBEB) else ReshapeTheme.colors.primarySoft
                                            else -> ReshapeTheme.colors.bgCanvas
                                        },
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(10.dp)
                            ) {
                                // 同屏常驻 Ghost Set 提示行 (点击即可一秒无脑快填)
                                if (!s.isWarmup && !s.isCompleted) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp)
                                            .clickable {
                                                s.weightKg = ghostWeight
                                                s.reps = ghostReps
                                                stateRefreshKey++
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "👻 上次: ${ghostWeight}kg × ${ghostReps}次",
                                            fontSize = 11.sp,
                                            color = ReshapeTheme.colors.textFaint,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "🎯 目标 +1次或+2.5kg (点此快填)",
                                            fontSize = 10.sp,
                                            color = ReshapeTheme.colors.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 组序号与热身标识 (独立编号 W1/W2 与 1/2/3/4)
                                    Column(modifier = Modifier.width(52.dp)) {
                                        if (s.isWarmup) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = s.displayNo,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFD97706),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "第 ${s.displayNo} 组",
                                                fontSize = 13.sp,
                                                color = if (isCurrentSet) ReshapeTheme.colors.primary else ReshapeTheme.colors.textPrimary,
                                                fontWeight = if (isCurrentSet) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }

                                    // 重量大按键 (点击调起 9 宫格大按键抽屉，禁用原生软键盘)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(ReshapeTheme.colors.card, RoundedCornerShape(8.dp))
                                            .clickable {
                                                editingSetIndex = index
                                                numPadMode = NumPadInputMode.WEIGHT
                                                numPadVisible = true
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${s.weightKg} kg",
                                            fontSize = 16.sp,
                                            color = ReshapeTheme.colors.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // 次数按钮 (单侧动作支持左/右，双侧直接输入次数)
                                    if (currentExercise.isUnilateral) {
                                        Box(
                                            modifier = Modifier
                                                .width(42.dp)
                                                .background(ReshapeTheme.colors.card, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    editingSetIndex = index
                                                    editingIsLeftReps = true
                                                    numPadMode = NumPadInputMode.REPS
                                                    numPadVisible = true
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "左:${s.leftReps ?: s.reps}", fontSize = 11.sp, color = ReshapeTheme.colors.textPrimary)
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Box(
                                            modifier = Modifier
                                                .width(42.dp)
                                                .background(ReshapeTheme.colors.card, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    editingSetIndex = index
                                                    editingIsLeftReps = false
                                                    numPadMode = NumPadInputMode.REPS
                                                    numPadVisible = true
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "右:${s.rightReps ?: s.reps}", fontSize = 11.sp, color = ReshapeTheme.colors.textPrimary)
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        // 单侧弱侧强侧对齐锁
                                        Box(
                                            modifier = Modifier
                                                .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(6.dp))
                                                .clickable {
                                                    val weakReps = minOf(s.leftReps ?: s.reps, s.rightReps ?: s.reps)
                                                    s.leftReps = weakReps
                                                    s.rightReps = weakReps
                                                    s.reps = weakReps
                                                    stateRefreshKey++
                                                }
                                                .padding(horizontal = 4.dp, vertical = 6.dp)
                                        ) {
                                            Text(text = "🔒对齐", fontSize = 10.sp, color = ReshapeTheme.colors.primary)
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(ReshapeTheme.colors.card, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    editingSetIndex = index
                                                    numPadMode = NumPadInputMode.REPS
                                                    numPadVisible = true
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${s.reps} 次",
                                                fontSize = 16.sp,
                                                color = ReshapeTheme.colors.textPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // 做组完成勾选按键 (大触控面积，手汗防误触，零触控打卡闭环)
                                    val checkBgColor = when {
                                        s.isCompleted -> Color(0xFF10B981) // 翡翠绿 (已完成)
                                        isCurrentSet -> if (s.isWarmup) Color(0xFFF59E0B) else ReshapeTheme.colors.primary // 琥珀橙(热身) / 活力主色(正式)
                                        else -> Color(0xFFE5E7EB) // 浅灰 (后续待练)
                                    }
                                    val checkTextColor = when {
                                        s.isCompleted -> Color.White
                                        isCurrentSet -> Color.White
                                        else -> Color(0xFF9CA3AF)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(48.dp)
                                            .height(38.dp)
                                            .background(checkBgColor, RoundedCornerShape(10.dp))
                                            .clickable {
                                                s.isCompleted = !s.isCompleted
                                                stateRefreshKey++
                                                if (s.isCompleted) {
                                                    // 触发 PR 检测 (正式组且重量达标)
                                                    if (s.weightKg >= 80f && !s.isWarmup) {
                                                        s.isPr = true
                                                        prWeight = s.weightKg
                                                        showPrCard = true
                                                    }
                                                    // 启动专属倒计时 (精准区分热身组 30s/60s 换片与正式组间歇)
                                                    totalRestSeconds = s.customRestSeconds
                                                    remainingSeconds = totalRestSeconds
                                                    restTitle = when {
                                                        s.isWarmup && s.customRestSeconds <= 35 -> "🔥 极速换片加重 (30s)"
                                                        s.isWarmup -> "🔥 神经激活准备 (60s)"
                                                        else -> "⏳ 正式组间休息 (${currentExercise.defaultRestSeconds}s)"
                                                    }
                                                    restActive = true

                                                    // 全会话秒级落盘草稿
                                                    val allExercisesDraft = buildAllSessionExercises()
                                                    val currentSession = WorkoutSession(
                                                        sessionId = "draft_session",
                                                        userProfile = SessionUserProfile(bodyWeightKg = userProfile.bodyWeightKg, gender = userProfile.gender.name),
                                                        planContext = SessionPlanContext(dayCode = currentDay.code, dayTitle = currentDay.title),
                                                        sessionMetrics = SessionMetrics(
                                                            durationMinutes = maxOf(1, elapsedSeconds / 60),
                                                            totalTonnageKg = VolumeCalculator.calculateTotalVolume(allExercisesDraft, userProfile.bodyWeightKg),
                                                            workingSetsCount = VolumeCalculator.countWorkingSets(allExercisesDraft),
                                                            warmupSetsCount = VolumeCalculator.countWarmupSets(allExercisesDraft),
                                                            prBreakCount = VolumeCalculator.countPrSets(allExercisesDraft)
                                                        ),
                                                        exercises = allExercisesDraft,
                                                        isDraft = true
                                                    )
                                                    SessionDraftStore.saveDraft(currentSession, spModule)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "✓",
                                            fontSize = if (s.isCompleted) 18.sp else 15.sp,
                                            color = checkTextColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 加组 / 切换休息时长
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "➕ 添加一组",
                                fontSize = 13.sp,
                                color = ReshapeTheme.colors.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable {
                                        val nextWorkingNo = currentSets.count { !it.isWarmup } + 1
                                        val nextGlobalNo = currentSets.size + 1
                                        val lastWeight = currentSets.lastOrNull()?.weightKg ?: currentExercise.targetWeightKg
                                        val lastReps = currentSets.lastOrNull()?.reps ?: currentExercise.targetRepsMin
                                        currentSets.add(
                                            EditableSetState(
                                                setNo = nextGlobalNo,
                                                displayNo = "$nextWorkingNo",
                                                isWarmup = false,
                                                weightKg = lastWeight,
                                                reps = lastReps,
                                                customRestSeconds = currentExercise.defaultRestSeconds
                                            )
                                        )
                                        stateRefreshKey++
                                    }
                                    .weight(1f)
                            )
                            Text(
                                text = "⏱️ 正式间歇 ${currentExercise.defaultRestSeconds}s ›",
                                fontSize = 13.sp,
                                color = ReshapeTheme.colors.textSecondary,
                                modifier = Modifier.clickable { restDurationVisible = true }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 下一个动作切换栏 / 全场结算栏
                if (currentExerciseIndex < rawExercises.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(48.dp)
                            .background(ReshapeTheme.colors.card, RoundedCornerShape(12.dp))
                            .clickable {
                                advanceToNextExerciseOrFinish()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "进入下一个动作：${rawExercises[currentExerciseIndex + 1].name} ➔",
                            fontSize = 15.sp,
                            color = ReshapeTheme.colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(48.dp)
                            .background(ReshapeTheme.colors.primary, RoundedCornerShape(12.dp))
                            .clickable {
                                finishAndArchiveSession()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎉 最后一个动作已完成 · 点击结算战报 ➔",
                            fontSize = 15.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 底部垫高 140dp 避让吸底悬浮休息仓
                Spacer(modifier = Modifier.height(140.dp))
            }
        }

        // 吸底常驻悬浮休息仓
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            FloatingRestDock(
                isActive = restActive,
                remainingSeconds = remainingSeconds,
                totalSeconds = totalRestSeconds,
                restTitle = restTitle,
                onAdjustTime = { delta ->
                    remainingSeconds = (remainingSeconds + delta).coerceAtLeast(0)
                },
                onSkip = {
                    restActive = false
                    remainingSeconds = 0
                    val allCompleted = currentSets.all { it.isCompleted }
                    if (allCompleted) {
                        advanceToNextExerciseOrFinish()
                    }
                }
            )
        }

        // 各抽屉与弹层浮层组件 (接入历史上次数据带入)
        val editingSet = currentSets.getOrNull(editingSetIndex)
        val editingLastHistory = if (numPadMode == NumPadInputMode.WEIGHT) editingSet?.weightKg else editingSet?.reps?.toFloat()

        GymNumPadDrawer(
            visible = numPadVisible,
            mode = numPadMode,
            initialValue = if (numPadMode == NumPadInputMode.WEIGHT) editingSet?.weightKg?.toString() ?: "60" else editingSet?.reps?.toString() ?: "8",
            lastHistoryValue = editingLastHistory,
            onDismiss = { numPadVisible = false },
            onConfirm = { valNum ->
                if (editingSetIndex in currentSets.indices) {
                    val s = currentSets[editingSetIndex]
                    if (numPadMode == NumPadInputMode.WEIGHT) {
                        s.weightKg = valNum
                    } else {
                        if (editingIsLeftReps) {
                            s.leftReps = valNum.toInt()
                            s.reps = minOf(s.leftReps ?: valNum.toInt(), s.rightReps ?: valNum.toInt())
                        } else {
                            s.rightReps = valNum.toInt()
                            s.reps = minOf(s.leftReps ?: valNum.toInt(), s.rightReps ?: valNum.toInt())
                        }
                    }
                    stateRefreshKey++
                }
                numPadVisible = false
            }
        )

        // 装片算盘：智能抓取当前待完成的下一组目标重量与用户自定义杆重
        val activeUncompletedSet = currentSets.firstOrNull { !it.isCompleted } ?: currentSets.lastOrNull()
        val plateTargetWeight = activeUncompletedSet?.weightKg ?: currentExercise.targetWeightKg
        val plateTargetName = if (activeUncompletedSet != null) {
            if (activeUncompletedSet.isWarmup) "热身组" else "第 ${activeUncompletedSet.setNo} 组"
        } else "下一组"

        PlateVisualizer(
            visible = plateVisualizerVisible,
            totalWeightKg = plateTargetWeight,
            barWeightKg = userProfile.barbellWeightKg,
            targetSetName = plateTargetName,
            onDismiss = { plateVisualizerVisible = false }
        )

        PainFreeDrawer(
            visible = painFreeVisible,
            currentExerciseId = currentExercise.id,
            onDismiss = { painFreeVisible = false },
            onSelectVariant = { newEx ->
                currentExercise = newEx
                painFreeVisible = false
            }
        )

        RestDurationDrawer(
            visible = restDurationVisible,
            exerciseName = currentExercise.name,
            currentSeconds = currentExercise.defaultRestSeconds,
            onDismiss = { restDurationVisible = false },
            onSelectDuration = { sec, saveToPlan ->
                val updatedEx = currentExercise.copy(defaultRestSeconds = sec)
                currentExercise = updatedEx
                totalRestSeconds = sec
                if (restActive) {
                    remainingSeconds = sec
                }
                val updatedList = rawExercises.toMutableList()
                if (currentExerciseIndex in updatedList.indices) {
                    updatedList[currentExerciseIndex] = updatedEx
                    rawExercises = updatedList
                }
                if (saveToPlan) {
                    PlanRepository.updateExercise(currentDay, updatedEx, spModule)
                }
                stateRefreshKey++
                restDurationVisible = false
            }
        )

        AllExercisesSheet(
            visible = allExercisesSheetVisible,
            exercises = rawExercises.map { ex -> SessionExercise(exerciseId = ex.id, name = ex.name, category = ex.targetMuscle) },
            currentIndex = currentExerciseIndex,
            onDismiss = { allExercisesSheetVisible = false },
            onSelectIndex = { idx ->
                currentExerciseIndex = idx
                allExercisesSheetVisible = false
            }
        )
    }
}

@Composable
private fun WorkoutStopwatchDisplay(elapsedSecondsProvider: () -> Int) {
    val seconds = elapsedSecondsProvider()
    val clockMin = seconds / 60
    val clockSec = seconds % 60
    val clockStr = "${clockMin.toString().padStart(2, '0')}:${clockSec.toString().padStart(2, '0')}"
    Text(
        text = "⏱️ $clockStr",
        fontSize = 14.sp,
        color = ReshapeTheme.colors.textPrimary,
        fontWeight = FontWeight.Bold
    )
}
