package com.woo.kuiklycomposetemplate

import com.woo.kuiklycomposetemplate.domain.algorithm.VolumeCalculator
import com.woo.kuiklycomposetemplate.domain.model.PresetExercises
import com.woo.kuiklycomposetemplate.domain.model.SessionExercise
import com.woo.kuiklycomposetemplate.domain.model.SessionMetrics
import com.woo.kuiklycomposetemplate.domain.model.SessionPlanContext
import com.woo.kuiklycomposetemplate.domain.model.SessionSet
import com.woo.kuiklycomposetemplate.domain.model.SessionUserProfile
import com.woo.kuiklycomposetemplate.domain.model.WorkoutSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecordSessionAggregationTest {

    @Test
    fun testMultiExerciseSessionAggregation_RetainsAllCompletedExercises() {
        val userWeight = 75.0f

        // 模拟推日完成 5 个动作全量数据
        val benchPress = SessionExercise(
            exerciseId = PresetExercises.BARBELL_BENCH_PRESS.id,
            name = PresetExercises.BARBELL_BENCH_PRESS.name,
            category = "胸大肌",
            isUnilateral = false,
            sets = listOf(
                SessionSet(setNo = 1, isWarmup = true, weightKg = 40f, effectiveReps = 10),
                SessionSet(setNo = 2, isWarmup = false, weightKg = 70f, effectiveReps = 6, isPr = true),
                SessionSet(setNo = 3, isWarmup = false, weightKg = 70f, effectiveReps = 5)
            )
        )

        val dips = SessionExercise(
            exerciseId = PresetExercises.DIPS.id,
            name = PresetExercises.DIPS.name,
            category = "胸大肌 / 肱三头肌",
            isUnilateral = false,
            sets = listOf(
                SessionSet(setNo = 4, isWarmup = false, weightKg = 10f, effectiveReps = 8) // 自重 75 + 10 = 85kg * 8 = 680kg
            )
        )

        val lateralRaise = SessionExercise(
            exerciseId = PresetExercises.DUMBBELL_LATERAL_RAISE.id,
            name = PresetExercises.DUMBBELL_LATERAL_RAISE.name,
            category = "三角肌中束",
            isUnilateral = false, // 已修复为双手双侧
            sets = listOf(
                SessionSet(setNo = 5, isWarmup = false, weightKg = 7.5f, effectiveReps = 15) // 7.5 * 15 = 112.5kg
            )
        )

        val sessionExercises = listOf(benchPress, dips, lateralRaise)

        // 1. 验证总容量计算 (包含自重做功)
        val totalVolume = VolumeCalculator.calculateTotalVolume(sessionExercises, userBodyWeightKg = userWeight)
        // bench: 70*6 + 70*5 = 770kg
        // dips: (75+10)*8 = 680kg
        // lateral: 7.5*15 = 112.5kg
        // total: 770 + 680 + 112.5 = 1562.5kg
        assertEquals(1562.5f, totalVolume)

        // 2. 验证有效正式组与热身组
        val workingSets = VolumeCalculator.countWorkingSets(sessionExercises)
        assertEquals(4, workingSets)

        val warmupSets = VolumeCalculator.countWarmupSets(sessionExercises)
        assertEquals(1, warmupSets)

        // 3. 验证 PR 计数
        val prCount = VolumeCalculator.countPrSets(sessionExercises)
        assertEquals(1, prCount)

        // 4. 验证 WorkoutSession 实体完整封装
        val session = WorkoutSession(
            sessionId = "sess_integration_001",
            userProfile = SessionUserProfile(bodyWeightKg = userWeight, gender = "male"),
            planContext = SessionPlanContext(dayCode = "D1", dayTitle = "D1 · 推"),
            sessionMetrics = SessionMetrics(
                durationMinutes = 45,
                totalTonnageKg = totalVolume,
                workingSetsCount = workingSets,
                warmupSetsCount = warmupSets,
                prBreakCount = prCount
            ),
            exercises = sessionExercises
        )

        assertEquals(3, session.exercises.size)
        assertEquals("push_1_bench_press", session.exercises[0].exerciseId)
        assertEquals("push_3_dips", session.exercises[1].exerciseId)
        assertEquals("push_4_lateral_raise", session.exercises[2].exerciseId)
        assertTrue(session.sessionMetrics.totalTonnageKg > 1500f)
    }
}
