package com.woo.kuiklycomposetemplate

import com.woo.kuiklycomposetemplate.domain.algorithm.MuscleRecoveryEngine
import com.woo.kuiklycomposetemplate.domain.model.DayType
import com.woo.kuiklycomposetemplate.domain.model.SessionMetrics
import com.woo.kuiklycomposetemplate.domain.model.SessionPlanContext
import com.woo.kuiklycomposetemplate.domain.model.SessionUserProfile
import com.woo.kuiklycomposetemplate.domain.model.WorkoutSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MuscleRecoveryEngineTest {

    private val baseTime = 1756400000000L // 当前基准时间戳

    private fun createSession(dayCode: String, hoursAgo: Float, tonnage: Float = 5500f, bodyWeight: Float = 75f): WorkoutSession {
        val sessionTime = baseTime - (hoursAgo * 3600f * 1000f).toLong()
        return WorkoutSession(
            sessionId = "test_sess_${dayCode}",
            userProfile = SessionUserProfile(bodyWeightKg = bodyWeight, gender = "male"),
            planContext = SessionPlanContext(dayCode = dayCode, dayTitle = dayCode),
            sessionMetrics = SessionMetrics(totalTonnageKg = tonnage),
            exercises = emptyList(),
            finishedAt = sessionTime
        )
    }

    @Test
    fun testNeverTrained_ReturnsFull100Percent() {
        val states = MuscleRecoveryEngine.evaluateAllMuscleGroups(emptyList(), currentTimestampMillis = baseTime)
        assertEquals(3, states.size)
        assertTrue(states.all { it.recoveryRatio == 1.0f })
        assertTrue(states.all { it.isSupercompensated })
    }

    @Test
    fun testAcuteFatiguePhase_Under12Hours() {
        val history = listOf(createSession("D1", hoursAgo = 6f)) // 6 小时前练了推
        val states = MuscleRecoveryEngine.evaluateAllMuscleGroups(history, currentTimestampMillis = baseTime)
        val pushState = states.first { it.dayType == DayType.PUSH }

        // 6h: 0.20 + (6/12) * 0.20 = 0.30 (30%)
        assertEquals(0.30f, pushState.recoveryRatio)
        assertFalse(pushState.isSupercompensated)
        assertTrue(pushState.statusDescription.contains("急性疲劳"))
    }

    @Test
    fun testProteinSynthesisRepairPhase_Between12And36Hours() {
        val history = listOf(createSession("D1", hoursAgo = 24f)) // 24 小时前练了推
        val states = MuscleRecoveryEngine.evaluateAllMuscleGroups(history, currentTimestampMillis = baseTime)
        val pushState = states.first { it.dayType == DayType.PUSH }

        // 24h: 0.40 + (12/24) * 0.35 = 0.575 -> ~0.58 (58%)
        assertEquals(0.58f, pushState.recoveryRatio)
        assertFalse(pushState.isSupercompensated)
        assertTrue(pushState.statusDescription.contains("蛋白合成"))
    }

    @Test
    fun testSupercompensationGoldenWindow_Around60To90Hours() {
        val history = listOf(createSession("D1", hoursAgo = 65f)) // 65 小时前练了推 (标准 60h)
        val states = MuscleRecoveryEngine.evaluateAllMuscleGroups(history, currentTimestampMillis = baseTime)
        val pushState = states.first { it.dayType == DayType.PUSH }

        assertEquals(1.0f, pushState.recoveryRatio)
        assertTrue(pushState.isSupercompensated)
        assertTrue(pushState.statusDescription.contains("超量恢复黄金期"))
    }

    @Test
    fun testDeconditioningProtection_Over7Days() {
        val history = listOf(createSession("D3", hoursAgo = 200f)) // 停训 200 小时 (>7天)
        val states = MuscleRecoveryEngine.evaluateAllMuscleGroups(history, currentTimestampMillis = baseTime)
        val legsState = states.first { it.dayType == DayType.LEGS }

        assertEquals(0.80f, legsState.recoveryRatio)
        assertFalse(legsState.isSupercompensated)
        assertTrue(legsState.statusDescription.contains("复训保护期"))
    }
}
