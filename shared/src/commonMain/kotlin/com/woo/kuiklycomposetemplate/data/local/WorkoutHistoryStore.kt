package com.woo.kuiklycomposetemplate.data.local

import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.woo.kuiklycomposetemplate.core.utils.CalendarUtils
import com.woo.kuiklycomposetemplate.domain.model.WorkoutSession
import kotlin.math.round

/**
 * 月度训练出勤汇总
 */
data class MonthWorkoutSummary(
    val yearMonth: String,       // "2026-08"
    val attendanceDays: Int,     // 出勤天数
    val totalTonnageKg: Float,   // 总吨位 (kg)
    val totalDurationMin: Int,   // 总时长 (min)
    val totalPrCount: Int        // 破 PR 总次数
)

/**
 * 训练历史归档与成就统计中心
 */
object WorkoutHistoryStore {

    private const val KEY_WORKOUT_HISTORY = "reshape_workout_history_v1"

    // 内存缓存历史记录列表
    private val historyList = mutableListOf<WorkoutSession>()
    // 动作最高 PR 记录 Map (exerciseId -> maxWeightKg)
    private val prMap = mutableMapOf<String, Float>()

    fun getHistory(spModule: SharedPreferencesModule? = null): List<WorkoutSession> {
        if (spModule != null) {
            try {
                val jsonStr = spModule.getString(KEY_WORKOUT_HISTORY)
                if (!jsonStr.isNullOrEmpty()) {
                    historyList.clear()
                    prMap.clear()
                    val array = JSONArray(jsonStr)
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        val session = JsonUtils.sessionFromJson(obj)
                        historyList.add(session)

                        // 从历史流水中重建各动作的历史最高 PR
                        for (ex in session.exercises) {
                            for (s in ex.sets) {
                                val currentMax = prMap[ex.exerciseId] ?: 0f
                                if (s.weightKg > currentMax) {
                                    prMap[ex.exerciseId] = s.weightKg
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return historyList.toList()
    }

    fun archiveSession(session: WorkoutSession, spModule: SharedPreferencesModule? = null) {
        val completedSession = session.copy(isDraft = false)
        historyList.add(0, completedSession)

        // 更新 PR 记录
        for (ex in completedSession.exercises) {
            for (s in ex.sets) {
                if (s.isPr) {
                    val currentMax = prMap[ex.exerciseId] ?: 0f
                    if (s.weightKg > currentMax) {
                        prMap[ex.exerciseId] = s.weightKg
                    }
                }
            }
        }

        if (spModule != null) {
            try {
                val array = JSONArray()
                for (s in historyList) {
                    array.put(JsonUtils.sessionToJson(s))
                }
                spModule.setString(KEY_WORKOUT_HISTORY, array.toString())
            } catch (_: Exception) {}
        }
    }

    /**
     * 计算月度统计数据
     */
    fun getMonthSummary(yearMonth: String = "2026-08", spModule: SharedPreferencesModule? = null): MonthWorkoutSummary {
        val list = getHistory(spModule).filter { session ->
            val timestamp = session.finishedAt ?: session.startedAt
            val (year, month, _) = CalendarUtils.parseTimestamp(timestamp)
            val sessionYearMonth = year.toString().padStart(4, '0') + "-" + month.toString().padStart(2, '0')
            sessionYearMonth == yearMonth
        }
        var daysCount = 0
        var totalTonnage = 0f
        var totalDuration = 0
        var totalPr = 0

        for (session in list) {
            daysCount++
            totalTonnage += session.sessionMetrics.totalTonnageKg
            totalDuration += session.sessionMetrics.durationMinutes
            totalPr += session.sessionMetrics.prBreakCount
        }

        return MonthWorkoutSummary(
            yearMonth = yearMonth,
            attendanceDays = daysCount,
            totalTonnageKg = round(totalTonnage * 10f) / 10f,
            totalDurationMin = totalDuration,
            totalPrCount = totalPr
        )
    }

    fun getExercisePr(exerciseId: String): Float {
        return prMap[exerciseId] ?: 0f
    }

    /**
     * 计算距离上次训练过去的天数
     */
    fun getDaysSinceLastWorkout(nowTimestampMillis: Long = 0L): Int {
        val list = historyList
        if (list.isEmpty()) return 1 // 新用户或首测默认为正常 1 天
        val lastSession = list.firstOrNull() ?: return 1
        val lastTime = if (lastSession.startedAt > 0L) lastSession.startedAt else (lastSession.finishedAt ?: 0L)
        if (lastTime == 0L || nowTimestampMillis == 0L) return 1
        val diffMillis = nowTimestampMillis - lastTime
        val days = (diffMillis / (1000L * 60 * 60 * 24)).toInt()
        return days.coerceAtLeast(0)
    }
}
