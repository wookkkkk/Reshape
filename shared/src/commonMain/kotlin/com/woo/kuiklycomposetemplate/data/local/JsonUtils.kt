package com.woo.kuiklycomposetemplate.data.local

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.woo.kuiklycomposetemplate.domain.model.Gender
import com.woo.kuiklycomposetemplate.domain.model.SessionExercise
import com.woo.kuiklycomposetemplate.domain.model.SessionMetrics
import com.woo.kuiklycomposetemplate.domain.model.SessionPlanContext
import com.woo.kuiklycomposetemplate.domain.model.SessionSet
import com.woo.kuiklycomposetemplate.domain.model.SessionUserProfile
import com.woo.kuiklycomposetemplate.domain.model.UserProfile
import com.woo.kuiklycomposetemplate.domain.model.WorkoutSession

/**
 * 跨端轻量级 JSON 编解码工具
 * 基于 Kuikly 原生 JSONObject / JSONArray，0 外部依赖，5 端通用
 */
object JsonUtils {

    // ==================== WorkoutSession 编解码 ====================

    fun sessionToJson(session: WorkoutSession): JSONObject {
        val obj = JSONObject()
        obj.put("sessionId", session.sessionId)
        obj.put("userId", session.userId)
        obj.put("startedAt", session.startedAt)
        obj.put("isDraft", session.isDraft)
        if (session.finishedAt != null) {
            obj.put("finishedAt", session.finishedAt)
        }

        // userProfile
        val profileObj = JSONObject().apply {
            put("bodyWeightKg", session.userProfile.bodyWeightKg.toDouble())
            put("gender", session.userProfile.gender)
            put("trainingExpYears", session.userProfile.trainingExpYears.toDouble())
            put("bench1RmKg", session.userProfile.bench1RmKg.toDouble())
            put("squat1RmKg", session.userProfile.squat1RmKg.toDouble())
            put("deadlift1RmKg", session.userProfile.deadlift1RmKg.toDouble())
        }
        obj.put("userProfile", profileObj)

        // planContext
        val planObj = JSONObject().apply {
            put("planId", session.planContext.planId)
            put("dayCode", session.planContext.dayCode)
            put("dayTitle", session.planContext.dayTitle)
            put("readinessState", session.planContext.readinessState)
            put("isDeloadWeek", session.planContext.isDeloadWeek)
            put("isRehabProtected", session.planContext.isRehabProtected)
        }
        obj.put("planContext", planObj)

        // sessionMetrics
        val metricsObj = JSONObject().apply {
            put("durationMinutes", session.sessionMetrics.durationMinutes)
            put("totalTonnageKg", session.sessionMetrics.totalTonnageKg.toDouble())
            put("workingSetsCount", session.sessionMetrics.workingSetsCount)
            put("warmupSetsCount", session.sessionMetrics.warmupSetsCount)
            put("prBreakCount", session.sessionMetrics.prBreakCount)
            put("avgRestSeconds", session.sessionMetrics.avgRestSeconds)
        }
        obj.put("sessionMetrics", metricsObj)

        // exercises
        val exercisesArray = JSONArray()
        for (exercise in session.exercises) {
            val exObj = JSONObject().apply {
                put("exerciseId", exercise.exerciseId)
                put("name", exercise.name)
                put("category", exercise.category)
                put("isUnilateral", exercise.isUnilateral)
                put("memo", exercise.memo)

                val setsArray = JSONArray()
                for (set in exercise.sets) {
                    val setObj = JSONObject().apply {
                        put("setNo", set.setNo)
                        put("isWarmup", set.isWarmup)
                        put("weightKg", set.weightKg.toDouble())
                        if (set.leftReps != null) put("leftReps", set.leftReps)
                        if (set.rightReps != null) put("rightReps", set.rightReps)
                        put("effectiveReps", set.effectiveReps)
                        put("rpe", set.rpe.toDouble())
                        put("isPr", set.isPr)
                        put("restSeconds", set.restSeconds)
                        put("completedAt", set.completedAt)
                    }
                    setsArray.put(setObj)
                }
                put("sets", setsArray)
            }
            exercisesArray.put(exObj)
        }
        obj.put("exercises", exercisesArray)

        return obj
    }

    fun sessionFromJson(obj: JSONObject): WorkoutSession {
        val sessionId = obj.optString("sessionId", "")
        val userId = obj.optString("userId", "local_user")
        val startedAt = obj.optLong("startedAt", 0L)
        val finishedAt = if (obj.has("finishedAt")) obj.optLong("finishedAt", 0L) else null
        val isDraft = obj.optBoolean("isDraft", false)

        val pObj = obj.optJSONObject("userProfile") ?: JSONObject()
        val userProfile = SessionUserProfile(
            bodyWeightKg = pObj.optDouble("bodyWeightKg", 75.0).toFloat(),
            gender = pObj.optString("gender", "male"),
            trainingExpYears = pObj.optDouble("trainingExpYears", 1.0).toFloat(),
            bench1RmKg = pObj.optDouble("bench1RmKg", 60.0).toFloat(),
            squat1RmKg = pObj.optDouble("squat1RmKg", 80.0).toFloat(),
            deadlift1RmKg = pObj.optDouble("deadlift1RmKg", 100.0).toFloat()
        )

        val planObj = obj.optJSONObject("planContext") ?: JSONObject()
        val planContext = SessionPlanContext(
            planId = planObj.optString("planId", "classic_ppl"),
            dayCode = planObj.optString("dayCode", "D1"),
            dayTitle = planObj.optString("dayTitle", "D1 · 推"),
            readinessState = planObj.optString("readinessState", "full"),
            isDeloadWeek = planObj.optBoolean("isDeloadWeek", false),
            isRehabProtected = planObj.optBoolean("isRehabProtected", false)
        )

        val mObj = obj.optJSONObject("sessionMetrics") ?: JSONObject()
        val sessionMetrics = SessionMetrics(
            durationMinutes = mObj.optInt("durationMinutes", 0),
            totalTonnageKg = mObj.optDouble("totalTonnageKg", 0.0).toFloat(),
            workingSetsCount = mObj.optInt("workingSetsCount", 0),
            warmupSetsCount = mObj.optInt("warmupSetsCount", 0),
            prBreakCount = mObj.optInt("prBreakCount", 0),
            avgRestSeconds = mObj.optInt("avgRestSeconds", 90)
        )

        val exercisesList = mutableListOf<SessionExercise>()
        val exArray = obj.optJSONArray("exercises")
        if (exArray != null) {
            for (i in 0 until exArray.length()) {
                val exObj = exArray.optJSONObject(i) ?: continue
                val setsList = mutableListOf<SessionSet>()
                val sArray = exObj.optJSONArray("sets")
                if (sArray != null) {
                    for (j in 0 until sArray.length()) {
                        val sObj = sArray.optJSONObject(j) ?: continue
                        setsList.add(
                            SessionSet(
                                setNo = sObj.optInt("setNo", j + 1),
                                isWarmup = sObj.optBoolean("isWarmup", false),
                                weightKg = sObj.optDouble("weightKg", 0.0).toFloat(),
                                leftReps = if (sObj.has("leftReps")) sObj.optInt("leftReps") else null,
                                rightReps = if (sObj.has("rightReps")) sObj.optInt("rightReps") else null,
                                effectiveReps = sObj.optInt("effectiveReps", 0),
                                rpe = sObj.optDouble("rpe", 8.0).toFloat(),
                                isPr = sObj.optBoolean("isPr", false),
                                restSeconds = sObj.optInt("restSeconds", 120),
                                completedAt = sObj.optLong("completedAt", 0L)
                            )
                        )
                    }
                }

                exercisesList.add(
                    SessionExercise(
                        exerciseId = exObj.optString("exerciseId", ""),
                        name = exObj.optString("name", ""),
                        category = exObj.optString("category", ""),
                        isUnilateral = exObj.optBoolean("isUnilateral", false),
                        memo = exObj.optString("memo", ""),
                        sets = setsList
                    )
                )
            }
        }

        return WorkoutSession(
            sessionId = sessionId,
            userId = userId,
            userProfile = userProfile,
            planContext = planContext,
            sessionMetrics = sessionMetrics,
            exercises = exercisesList,
            startedAt = startedAt,
            finishedAt = finishedAt,
            isDraft = isDraft
        )
    }

    // ==================== UserProfile 编解码 ====================

    fun profileToJson(profile: UserProfile): JSONObject {
        val obj = JSONObject()
        obj.put("userId", profile.userId)
        obj.put("nickname", profile.nickname)
        obj.put("gender", profile.gender.name)
        obj.put("heightCm", profile.heightCm.toDouble())
        obj.put("bodyWeightKg", profile.bodyWeightKg.toDouble())
        obj.put("targetWeightKg", profile.targetWeightKg.toDouble())
        obj.put("bodyFatPercentage", profile.bodyFatPercentage.toDouble())
        obj.put("skeletalMuscleKg", profile.skeletalMuscleKg.toDouble())
        obj.put("trainingExpYears", profile.trainingExpYears.toDouble())
        obj.put("barbellWeightKg", profile.barbellWeightKg.toDouble())
        obj.put("bench1RmKg", profile.bench1RmKg.toDouble())
        obj.put("squat1RmKg", profile.squat1RmKg.toDouble())
        obj.put("deadlift1RmKg", profile.deadlift1RmKg.toDouble())
        obj.put("preferredThemeCode", profile.preferredThemeCode)
        obj.put("soundEnabled", profile.soundEnabled)
        obj.put("screenKeepOn", profile.screenKeepOn)
        return obj
    }

    fun profileFromJson(obj: JSONObject): UserProfile {
        val genderStr = obj.optString("gender", "MALE")
        val gender = try {
            Gender.valueOf(genderStr)
        } catch (_: Exception) {
            Gender.MALE
        }

        return UserProfile(
            userId = obj.optString("userId", "local_user"),
            nickname = obj.optString("nickname", "Reshape 老铁"),
            gender = gender,
            heightCm = obj.optDouble("heightCm", 175.0).toFloat(),
            bodyWeightKg = obj.optDouble("bodyWeightKg", 75.0).toFloat(),
            targetWeightKg = obj.optDouble("targetWeightKg", 72.0).toFloat(),
            bodyFatPercentage = obj.optDouble("bodyFatPercentage", 15.0).toFloat(),
            skeletalMuscleKg = obj.optDouble("skeletalMuscleKg", 36.0).toFloat(),
            trainingExpYears = obj.optDouble("trainingExpYears", 2.0).toFloat(),
            barbellWeightKg = obj.optDouble("barbellWeightKg", 20.0).toFloat(),
            bench1RmKg = obj.optDouble("bench1RmKg", 80.0).toFloat(),
            squat1RmKg = obj.optDouble("squat1RmKg", 110.0).toFloat(),
            deadlift1RmKg = obj.optDouble("deadlift1RmKg", 130.0).toFloat(),
            preferredThemeCode = obj.optString("preferredThemeCode", "ORANGE"),
            soundEnabled = obj.optBoolean("soundEnabled", true),
            screenKeepOn = obj.optBoolean("screenKeepOn", true)
        )
    }
}
