package com.woo.kuiklycomposetemplate

import com.woo.kuiklycomposetemplate.data.local.JsonUtils
import com.woo.kuiklycomposetemplate.domain.model.Gender
import com.woo.kuiklycomposetemplate.domain.model.SessionExercise
import com.woo.kuiklycomposetemplate.domain.model.SessionMetrics
import com.woo.kuiklycomposetemplate.domain.model.SessionPlanContext
import com.woo.kuiklycomposetemplate.domain.model.SessionSet
import com.woo.kuiklycomposetemplate.domain.model.SessionUserProfile
import com.woo.kuiklycomposetemplate.domain.model.UserProfile
import com.woo.kuiklycomposetemplate.domain.model.WorkoutSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionSerializationTest {

    @Test
    fun testWorkoutSessionSerializationRoundTrip() {
        val originalSession = WorkoutSession(
            sessionId = "sess_20260828_d1",
            userId = "user_001",
            userProfile = SessionUserProfile(
                bodyWeightKg = 76.5f,
                gender = "male",
                trainingExpYears = 2.5f,
                bench1RmKg = 90.0f,
                squat1RmKg = 120.0f,
                deadlift1RmKg = 140.0f
            ),
            planContext = SessionPlanContext(
                planId = "classic_ppl",
                dayCode = "D1",
                dayTitle = "D1 · 力量推",
                readinessState = "full",
                isDeloadWeek = false,
                isRehabProtected = false
            ),
            sessionMetrics = SessionMetrics(
                durationMinutes = 58,
                totalTonnageKg = 6420.0f,
                workingSetsCount = 16,
                warmupSetsCount = 4,
                prBreakCount = 1,
                avgRestSeconds = 110
            ),
            exercises = listOf(
                SessionExercise(
                    exerciseId = "push_1_bench_press",
                    name = "杠铃平板卧推",
                    category = "chest",
                    isUnilateral = false,
                    memo = "落点稳定",
                    sets = listOf(
                        SessionSet(setNo = 1, isWarmup = true, weightKg = 40f, effectiveReps = 10),
                        SessionSet(setNo = 2, isWarmup = false, weightKg = 80f, effectiveReps = 5, isPr = true, rpe = 8.5f)
                    )
                )
            ),
            startedAt = 1756350000000L,
            finishedAt = 1756353480000L,
            isDraft = false
        )

        val json = JsonUtils.sessionToJson(originalSession)
        val restoredSession = JsonUtils.sessionFromJson(json)

        assertEquals(originalSession.sessionId, restoredSession.sessionId)
        assertEquals(originalSession.userId, restoredSession.userId)
        assertEquals(originalSession.planContext.dayCode, restoredSession.planContext.dayCode)
        assertEquals(originalSession.sessionMetrics.totalTonnageKg, restoredSession.sessionMetrics.totalTonnageKg)
        assertEquals(1, restoredSession.exercises.size)
        assertEquals("push_1_bench_press", restoredSession.exercises[0].exerciseId)
        assertEquals(2, restoredSession.exercises[0].sets.size)
        assertEquals(80f, restoredSession.exercises[0].sets[1].weightKg)
        assertTrue(restoredSession.exercises[0].sets[1].isPr)
    }

    @Test
    fun testUserProfileSerializationRoundTrip() {
        val originalProfile = UserProfile(
            userId = "user_888",
            nickname = "卧推老铁",
            gender = Gender.MALE,
            heightCm = 178f,
            bodyWeightKg = 77.5f,
            bench1RmKg = 95f,
            squat1RmKg = 130f,
            deadlift1RmKg = 160f,
            preferredThemeCode = "BLUE"
        )

        val json = JsonUtils.profileToJson(originalProfile)
        val restoredProfile = JsonUtils.profileFromJson(json)

        assertEquals(originalProfile.userId, restoredProfile.userId)
        assertEquals(originalProfile.nickname, restoredProfile.nickname)
        assertEquals(originalProfile.gender, restoredProfile.gender)
        assertEquals(originalProfile.bench1RmKg, restoredProfile.bench1RmKg)
        assertEquals(originalProfile.preferredThemeCode, restoredProfile.preferredThemeCode)
    }
}
