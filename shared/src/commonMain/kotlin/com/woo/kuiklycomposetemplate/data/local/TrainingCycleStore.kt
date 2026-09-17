package com.woo.kuiklycomposetemplate.data.local

import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.woo.kuiklycomposetemplate.core.utils.platformCurrentTimeMillis
import com.woo.kuiklycomposetemplate.domain.model.DayType
import com.woo.kuiklycomposetemplate.domain.model.Exercise
import com.woo.kuiklycomposetemplate.domain.model.TrainingCycle
import com.woo.kuiklycomposetemplate.domain.model.UserProfile
import com.woo.kuiklycomposetemplate.domain.model.WorkoutSession

/**
 * 匹配到的历史单次做组记录 (支持主项与无痛变式映射)
 */
data class MatchedStepRecord(
    val sessionIndex: Int,
    val exerciseName: String,
    val isVariant: Boolean,
    val weightKg: Float,
    val reps: Int,
    val rpe: Float,
    val timestamp: Long
)

/**
 * 8 次训练中周期管理存储器 (TrainingCycleStore)
 * 100% 绑定真实历史流水，支持分化日独立核算、动作无痛变式映射与周期结项
 */
object TrainingCycleStore {

    private const val KEY_TRAINING_CYCLE = "reshape_current_training_cycle_v2"

    // 内存缓存层
    private var cachedCycle: TrainingCycle = TrainingCycle(
        cycleId = "cycle_1",
        cycleNumber = 1,
        totalSessions = 8,
        currentSessionIndex = 1,
        isDeloadActive = false,
        isReturnShieldActive = false,
        startTimestamp = 0L,
        initialBench1RmKg = 75f,
        initialSquat1RmKg = 100f,
        initialDeadlift1RmKg = 130f
    )

    fun getCycle(spModule: SharedPreferencesModule? = null): TrainingCycle {
        if (spModule != null) {
            try {
                val jsonStr = spModule.getString(KEY_TRAINING_CYCLE)
                if (!jsonStr.isNullOrEmpty()) {
                    val obj = JSONObject(jsonStr)
                    cachedCycle = cycleFromJson(obj)
                }
            } catch (_: Exception) {}
        }
        return cachedCycle
    }

    fun saveCycle(cycle: TrainingCycle, spModule: SharedPreferencesModule? = null) {
        cachedCycle = cycle
        if (spModule != null) {
            try {
                val obj = cycleToJson(cycle)
                spModule.setString(KEY_TRAINING_CYCLE, obj.toString())
            } catch (_: Exception) {}
        }
    }

    /**
     * 完成一次训练，触发周期状态刷新并自增推进场次
     */
    fun advanceCycleSession(spModule: SharedPreferencesModule? = null): TrainingCycle {
        val current = getCycle(spModule)
        val nextIndex = (current.currentSessionIndex + 1).coerceAtMost(current.totalSessions)
        val updated = current.copy(currentSessionIndex = nextIndex)
        saveCycle(updated, spModule)
        return updated
    }

    /**
     * 根据真实训练历史，动态推算指定分化日 (PUSH / PULL / LEGS) 的当前周期进度
     */
    fun getCycleForDay(dayType: DayType, spModule: SharedPreferencesModule? = null): TrainingCycle {
        val baseCycle = getCycle(spModule)
        val history = WorkoutHistoryStore.getHistory(spModule)

        // 过滤当前周期起始时间之后、且属于该分化日的历史打卡记录
        val validDaySessions = history.filter { session ->
            val sessionTime = session.finishedAt ?: session.startedAt ?: 0L
            sessionTime >= baseCycle.startTimestamp && (
                session.planContext.dayCode.equals(dayType.name, ignoreCase = true) ||
                session.planContext.dayCode.equals(dayType.code, ignoreCase = true) ||
                session.planContext.dayTitle.contains(dayType.title)
            )
        }

        val completedCount = validDaySessions.size
        // startTimestamp 会在开启新周期时重置，因此这里只需要基于当前周期的真实完成场次推导进度
        val currentSessionIndex = if (completedCount >= baseCycle.totalSessions) {
            baseCycle.totalSessions
        } else {
            completedCount + 1
        }

        return baseCycle.copy(
            currentSessionIndex = currentSessionIndex
        )
    }

    /**
     * 根据真实训练历史与变式图谱，动态提取某动作在当前 8 次周期内的历史打卡明细与当前进度
     * 解决“中间某次换了无痛变式/替代动作”导致漏算或重量错乱的问题
     */
    fun getCycleForExercise(
        exercise: Exercise,
        dayType: DayType,
        spModule: SharedPreferencesModule? = null
    ): Pair<TrainingCycle, List<MatchedStepRecord>> {
        val dayCycle = getCycleForDay(dayType, spModule)
        val baseCycle = getCycle(spModule)
        val history = WorkoutHistoryStore.getHistory(spModule)

        // 过滤当前周期属于该分化日的所有历史场次 (按时间正序排列)
        val validDaySessions = history.filter { session ->
            val sessionTime = session.finishedAt ?: session.startedAt ?: 0L
            sessionTime >= baseCycle.startTimestamp && (
                session.planContext.dayCode.equals(dayType.name, ignoreCase = true) ||
                session.planContext.dayCode.equals(dayType.code, ignoreCase = true) ||
                session.planContext.dayTitle.contains(dayType.title)
            )
        }.sortedBy { it.finishedAt ?: it.startedAt ?: 0L }

        val records = mutableListOf<MatchedStepRecord>()
        val currentCycleSessions = validDaySessions.takeLast(baseCycle.totalSessions)

        currentCycleSessions.forEachIndexed { index, session ->
            val sessionTime = session.finishedAt ?: session.startedAt ?: 0L
            // 1. 优先精确匹配同名或同 ID 动作
            val exactMatch = session.exercises.firstOrNull {
                it.exerciseId == exercise.id || it.name == exercise.name
            }

            if (exactMatch != null && exactMatch.sets.isNotEmpty()) {
                val workingSets = exactMatch.sets.filter { !it.isWarmup }
                val maxSet = workingSets.maxByOrNull { it.weightKg } ?: exactMatch.sets.first()
                records.add(
                    MatchedStepRecord(
                        sessionIndex = index + 1,
                        exerciseName = exactMatch.name,
                        isVariant = false,
                        weightKg = maxSet.weightKg,
                        reps = maxSet.effectiveReps,
                        rpe = maxSet.rpe,
                        timestamp = sessionTime
                    )
                )
            } else {
                // 2. 智能识别无痛变式或替代动作 (根据变式 ID 列表或相同主发力肌群)
                val variantMatch = session.exercises.firstOrNull { ex ->
                    exercise.painFreeVariantIds.contains(ex.exerciseId) ||
                    exercise.interchangeableExerciseIds.contains(ex.exerciseId) ||
                    (ex.category.isNotBlank() && ex.category == exercise.primaryMuscle)
                }

                if (variantMatch != null && variantMatch.sets.isNotEmpty()) {
                    val workingSets = variantMatch.sets.filter { !it.isWarmup }
                    val maxSet = workingSets.maxByOrNull { it.weightKg } ?: variantMatch.sets.first()
                    records.add(
                        MatchedStepRecord(
                            sessionIndex = index + 1,
                            exerciseName = variantMatch.name,
                            isVariant = true, // 标记为无痛变式
                            weightKg = maxSet.weightKg,
                            reps = maxSet.effectiveReps,
                            rpe = maxSet.rpe,
                            timestamp = sessionTime
                        )
                    )
                }
            }
        }

        return Pair(dayCycle, records)
    }

    /**
     * 开启全新周期 (如第 8 次收官后重测 1RM 开启第 2 周期)
     */
    fun startNewCycle(
        profile: UserProfile,
        spModule: SharedPreferencesModule? = null
    ): TrainingCycle {
        val current = getCycle(spModule)
        val newCycleNum = current.cycleNumber + 1
        val newCycle = TrainingCycle(
            cycleId = "cycle_$newCycleNum",
            cycleNumber = newCycleNum,
            totalSessions = 8,
            currentSessionIndex = 1,
            isDeloadActive = false,
            isReturnShieldActive = false,
            startTimestamp = platformCurrentTimeMillis(), // 重置周期起始时间戳
            initialBench1RmKg = profile.bench1RmKg,
            initialSquat1RmKg = profile.squat1RmKg,
            initialDeadlift1RmKg = profile.deadlift1RmKg
        )
        saveCycle(newCycle, spModule)
        return newCycle
    }

    /**
     * 切换主动减载周 (Deload) 状态
     */
    fun setDeloadActive(active: Boolean, spModule: SharedPreferencesModule? = null): TrainingCycle {
        val current = getCycle(spModule)
        val updated = current.copy(isDeloadActive = active)
        saveCycle(updated, spModule)
        return updated
    }

    /**
     * 切换复训保护 (Return Shield 80%) 状态
     */
    fun setReturnShieldActive(active: Boolean, spModule: SharedPreferencesModule? = null): TrainingCycle {
        val current = getCycle(spModule)
        val updated = current.copy(isReturnShieldActive = active)
        saveCycle(updated, spModule)
        return updated
    }

    private fun cycleToJson(c: TrainingCycle): JSONObject {
        val obj = JSONObject()
        obj.put("cycleId", c.cycleId)
        obj.put("cycleNumber", c.cycleNumber)
        obj.put("totalSessions", c.totalSessions)
        obj.put("currentSessionIndex", c.currentSessionIndex)
        obj.put("isDeloadActive", c.isDeloadActive)
        obj.put("isReturnShieldActive", c.isReturnShieldActive)
        obj.put("startTimestamp", c.startTimestamp)
        obj.put("initialBench1RmKg", c.initialBench1RmKg.toDouble())
        obj.put("initialSquat1RmKg", c.initialSquat1RmKg.toDouble())
        obj.put("initialDeadlift1RmKg", c.initialDeadlift1RmKg.toDouble())
        return obj
    }

    private fun cycleFromJson(obj: JSONObject): TrainingCycle {
        return TrainingCycle(
            cycleId = obj.optString("cycleId", "cycle_1"),
            cycleNumber = obj.optInt("cycleNumber", 1),
            totalSessions = obj.optInt("totalSessions", 8),
            currentSessionIndex = obj.optInt("currentSessionIndex", 1),
            isDeloadActive = obj.optBoolean("isDeloadActive", false),
            isReturnShieldActive = obj.optBoolean("isReturnShieldActive", false),
            startTimestamp = obj.optLong("startTimestamp", 0L),
            initialBench1RmKg = obj.optDouble("initialBench1RmKg", 75.0).toFloat(),
            initialSquat1RmKg = obj.optDouble("initialSquat1RmKg", 100.0).toFloat(),
            initialDeadlift1RmKg = obj.optDouble("initialDeadlift1RmKg", 130.0).toFloat()
        )
    }
}
