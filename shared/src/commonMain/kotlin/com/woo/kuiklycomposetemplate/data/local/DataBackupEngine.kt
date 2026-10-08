package com.woo.kuiklycomposetemplate.data.local

import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.woo.kuiklycomposetemplate.core.utils.platformCurrentTimeMillis
import com.woo.kuiklycomposetemplate.domain.model.DayType
import com.woo.kuiklycomposetemplate.domain.model.Exercise
import com.woo.kuiklycomposetemplate.domain.model.TrainingCycle
import com.woo.kuiklycomposetemplate.domain.model.UserProfile
import com.woo.kuiklycomposetemplate.domain.model.WorkoutSession

/**
 * 备份校验与数据摘要结果
 */
data class BackupValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val schemaVersion: Int = 2,
    val exportedAt: Long = 0L,
    val nickname: String = "",
    val gender: String = "MALE",
    val bodyWeightKg: Float = 75f,
    val total1RmKg: Float = 0f,
    val historySessionCount: Int = 0,
    val customExerciseCount: Int = 0,
    val plansSummary: String = ""
)

/**
 * 全量数据备份与跨端迁移引擎 (DataBackupEngine)
 * 实现 Local-First 数据一键打包导出为结构化 JSON 文本，以及新设备一键无损导入恢复。
 */
object DataBackupEngine {

    const val CURRENT_SCHEMA_VERSION = 2

    /**
     * 导出全量数据为格式化 JSON 字符串
     */
    fun exportFullBackupJson(spModule: SharedPreferencesModule? = null): String {
        val root = JSONObject()
        val now = platformCurrentTimeMillis()

        // 1. 元信息
        root.put("schemaVersion", CURRENT_SCHEMA_VERSION)
        root.put("app", "Reshape")
        root.put("exportedAt", now)
        root.put("deviceInfo", "Kuikly-CrossPlatform")

        // 2. 用户档案 UserProfile
        val profile = UserPreferencesStore.getProfile(spModule)
        root.put("userProfile", JsonUtils.profileToJson(profile))

        // 3. 1RM 反推历史记录
        val estHistory = UserPreferencesStore.getEstimateHistory(spModule)
        val estObj = JSONObject().apply {
            put("squatWeight", estHistory.squatWeight)
            put("squatReps", estHistory.squatReps)
            put("benchWeight", estHistory.benchWeight)
            put("benchReps", estHistory.benchReps)
            put("deadliftWeight", estHistory.deadliftWeight)
            put("deadliftReps", estHistory.deadliftReps)
        }
        root.put("estimateHistory", estObj)

        // 4. 当前训练循环日
        val currentDay = PlanRepository.getCurrentDay(spModule)
        root.put("currentDay", currentDay.name)

        // 5. 三分化计划编排 (PUSH / PULL / LEGS)
        val plansObj = JSONObject()
        DayType.entries.filter { it != DayType.REST }.forEach { day ->
            val exercises = PlanRepository.getExercises(day, spModule)
            val arr = JSONArray()
            exercises.forEach { ex ->
                val exObj = JSONObject().apply {
                    put("id", ex.id)
                    put("name", ex.name)
                    put("dayType", ex.dayType.name)
                    put("primaryMuscle", ex.primaryMuscle)
                    val secArr = JSONArray()
                    ex.secondaryMuscles.forEach { secArr.put(it) }
                    put("secondaryMuscles", secArr)
                    put("targetMuscle", ex.displayMuscleSummary)
                    put("loadType", ex.loadType.name)
                    put("primaryJoint", ex.primaryJoint.name)
                    put("isCustom", ex.isCustom)
                    put("isUnilateral", ex.isUnilateral)
                    put("defaultHoleCues", ex.defaultHoleCues)
                    put("defaultSets", ex.defaultSets)
                    put("targetRepsMin", ex.targetRepsMin)
                    put("targetRepsMax", ex.targetRepsMax)
                    put("targetRepsSequence", ex.targetRepsSequence)
                    put("targetWeightKg", ex.targetWeightKg.toDouble())
                    put("waveWeightsSequence", ex.waveWeightsSequence)
                    put("targetRpe", ex.targetRpe.toDouble())
                    put("defaultRestSeconds", ex.defaultRestSeconds)
                    put("progressionType", ex.progressionType.name)
                    if (!ex.related1RmKey.isNullOrBlank()) {
                        put("related1RmKey", ex.related1RmKey)
                    }
                    if (ex.ratioOf1Rm != null) {
                        put("ratioOf1Rm", ex.ratioOf1Rm.toDouble())
                    }
                    val painFreeArr = JSONArray()
                    ex.painFreeVariantIds.forEach { painFreeArr.put(it) }
                    put("painFreeVariantIds", painFreeArr)
                    val interArr = JSONArray()
                    ex.interchangeableExerciseIds.forEach { interArr.put(it) }
                    put("interchangeableExerciseIds", interArr)
                    put("enableWarmupLadder", ex.enableWarmupLadder)
                }
                arr.put(exObj)
            }
            plansObj.put(day.name, arr)
        }
        root.put("plans", plansObj)

        // 6. 8 次训练中周期状态
        val cycle = TrainingCycleStore.getCycle(spModule)
        val cycleObj = JSONObject().apply {
            put("cycleId", cycle.cycleId)
            put("cycleNumber", cycle.cycleNumber)
            put("totalSessions", cycle.totalSessions)
            put("currentSessionIndex", cycle.currentSessionIndex)
            put("isDeloadActive", cycle.isDeloadActive)
            put("isReturnShieldActive", cycle.isReturnShieldActive)
            put("startTimestamp", cycle.startTimestamp)
            put("initialBench1RmKg", cycle.initialBench1RmKg.toDouble())
            put("initialSquat1RmKg", cycle.initialSquat1RmKg.toDouble())
            put("initialDeadlift1RmKg", cycle.initialDeadlift1RmKg.toDouble())
        }
        root.put("trainingCycle", cycleObj)

        // 7. 用户自定义动作库
        val customOnly = ExerciseLibraryRepository.getStoredExercises(spModule)
        val customArr = JSONArray()
        customOnly.forEach { ex ->
            val obj = JSONObject().apply {
                put("id", ex.id)
                put("name", ex.name)
                put("dayType", ex.dayType.name)
                put("primaryMuscle", ex.primaryMuscle)
                val secArr = JSONArray()
                ex.secondaryMuscles.forEach { secArr.put(it) }
                put("secondaryMuscles", secArr)
                put("loadType", ex.loadType.name)
                put("primaryJoint", ex.primaryJoint.name)
                put("isCustom", ex.isCustom)
                put("isUnilateral", ex.isUnilateral)
                put("defaultHoleCues", ex.defaultHoleCues)
                put("defaultSets", ex.defaultSets)
                put("targetRepsMin", ex.targetRepsMin)
                put("targetRepsMax", ex.targetRepsMax)
                put("targetRepsSequence", ex.targetRepsSequence)
                put("targetWeightKg", ex.targetWeightKg.toDouble())
                put("waveWeightsSequence", ex.waveWeightsSequence)
                put("targetRpe", ex.targetRpe.toDouble())
                put("defaultRestSeconds", ex.defaultRestSeconds)
                put("progressionType", ex.progressionType.name)
                if (!ex.related1RmKey.isNullOrBlank()) {
                    put("related1RmKey", ex.related1RmKey)
                }
                if (ex.ratioOf1Rm != null) {
                    put("ratioOf1Rm", ex.ratioOf1Rm.toDouble())
                }
                val painFreeArr = JSONArray()
                ex.painFreeVariantIds.forEach { painFreeArr.put(it) }
                put("painFreeVariantIds", painFreeArr)
                val interArr = JSONArray()
                ex.interchangeableExerciseIds.forEach { interArr.put(it) }
                put("interchangeableExerciseIds", interArr)
                put("enableWarmupLadder", ex.enableWarmupLadder)
            }
            customArr.put(obj)
        }
        root.put("customExercises", customArr)

        // 8. 训练历史打卡流水
        val historyList = WorkoutHistoryStore.getHistory(spModule)
        val historyArr = JSONArray()
        historyList.forEach { session ->
            historyArr.put(JsonUtils.sessionToJson(session))
        }
        root.put("workoutHistory", historyArr)

        return root.toString()
    }

    /**
     * 生成人类友好型【Reshape 训练档案迁移口令】
     * 适合微信/备忘录直接分享，降低非专业用户对纯 JSON 乱码与符号的恐惧感
     */
    fun exportShareablePasscode(spModule: SharedPreferencesModule? = null): String {
        val profile = UserPreferencesStore.getProfile(spModule)
        val historyList = WorkoutHistoryStore.getHistory(spModule)
        val total1Rm = profile.bench1RmKg + profile.squat1RmKg + profile.deadlift1RmKg
        val json = exportFullBackupJson(spModule)

        return buildString {
            appendLine("📦【Reshape 智能力量训练档案口令】")
            appendLine("👤 老铁：${profile.nickname} · 体重 ${profile.bodyWeightKg.toInt()}kg")
            appendLine("💪 三大项总和：${total1Rm.toInt()}kg (卧推${profile.bench1RmKg.toInt()}/深蹲${profile.squat1RmKg.toInt()}/硬拉${profile.deadlift1RmKg.toInt()})")
            appendLine("📊 包含：${historyList.size} 场历史训练打卡流水 · 完整周期与自定义计划")
            appendLine("─────── 复制整段口令导入 ───────")
            appendLine("\$RESHAPE_BACKUP_V2\$")
            appendLine(json)
            appendLine("\$END_RESHAPE\$")
            appendLine("────────────────────────────────")
            append("💡 换新手机/新设备后复制整段内容，打开 Reshape 即可 1 秒自动识别还原！")
        }
    }

    /**
     * 智能提取清洗器：无论用户粘贴的是整段微信口令、带前后说明的杂乱文本、还是纯 JSON，均能智能提取核心数据
     */
    fun extractCleanJson(rawText: String): String {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) return ""

        // 1. 如果包含 $RESHAPE_BACKUP_V2$ 标记，截取标记内部
        val tagStart = trimmed.indexOf("\$RESHAPE_BACKUP_V2\$")
        val tagEnd = trimmed.lastIndexOf("\$END_RESHAPE\$")
        if (tagStart >= 0 && tagEnd > tagStart) {
            val inner = trimmed.substring(tagStart + "\$RESHAPE_BACKUP_V2\$".length, tagEnd).trim()
            if (inner.isNotBlank()) return inner
        }

        // 2. 否则贪婪匹配首个 '{' 与 最后一个 '}'
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1).trim()
        }

        return trimmed
    }

    /**
     * 校验待导入备份文本并提取数据摘要 (支持口令或纯 JSON)
     */
    fun validateBackupJson(rawInput: String): BackupValidationResult {
        val jsonStr = extractCleanJson(rawInput)
        if (jsonStr.isBlank()) {
            return BackupValidationResult(isValid = false, errorMessage = "未检测到有效的档案数据口令")
        }
        try {
            val root = JSONObject(jsonStr)
            val app = root.optString("app", "")
            if (app != "Reshape" && !root.has("userProfile")) {
                return BackupValidationResult(isValid = false, errorMessage = "非 Reshape 训练档案口令")
            }

            val schemaVersion = root.optInt("schemaVersion", 1)
            val exportedAt = root.optLong("exportedAt", 0L)

            val profileObj = root.optJSONObject("userProfile")
            val nickname = profileObj?.optString("nickname", "Reshape 老铁") ?: "Reshape 老铁"
            val gender = profileObj?.optString("gender", "MALE") ?: "MALE"
            val bodyWeight = profileObj?.optDouble("bodyWeightKg", 75.0)?.toFloat() ?: 75f
            val bench = profileObj?.optDouble("bench1RmKg", 0.0)?.toFloat() ?: 0f
            val squat = profileObj?.optDouble("squat1RmKg", 0.0)?.toFloat() ?: 0f
            val deadlift = profileObj?.optDouble("deadlift1RmKg", 0.0)?.toFloat() ?: 0f
            val total1Rm = bench + squat + deadlift

            val historyArr = root.optJSONArray("workoutHistory")
            val historyCount = historyArr?.length() ?: 0

            val customArr = root.optJSONArray("customExercises")
            val customCount = customArr?.length() ?: 0

            val plansObj = root.optJSONObject("plans")
            val planKeys = mutableListOf<String>()
            if (plansObj != null) {
                if (plansObj.has("PUSH")) planKeys.add("D1推")
                if (plansObj.has("PULL")) planKeys.add("D2拉")
                if (plansObj.has("LEGS")) planKeys.add("D3腿")
            }
            val plansSummary = if (planKeys.isNotEmpty()) planKeys.joinToString(" · ") else "默认计划"

            return BackupValidationResult(
                isValid = true,
                schemaVersion = schemaVersion,
                exportedAt = exportedAt,
                nickname = nickname,
                gender = gender,
                bodyWeightKg = bodyWeight,
                total1RmKg = total1Rm,
                historySessionCount = historyCount,
                customExerciseCount = customCount,
                plansSummary = plansSummary
            )
        } catch (e: Exception) {
            return BackupValidationResult(isValid = false, errorMessage = "JSON 格式解析错误：${e.message}")
        }
    }

    /**
     * 从备份 JSON 恢复全量数据
     */
    fun restoreFromBackupJson(
        rawInput: String,
        spModule: SharedPreferencesModule? = null
    ): Boolean {
        if (spModule == null) return false
        val validation = validateBackupJson(rawInput)
        if (!validation.isValid) return false

        try {
            val cleanJson = extractCleanJson(rawInput)
            val root = JSONObject(cleanJson)

            // 1. 恢复 UserProfile
            val profileObj = root.optJSONObject("userProfile")
            if (profileObj != null) {
                val profile = JsonUtils.profileFromJson(profileObj)
                UserPreferencesStore.saveProfile(profile, spModule)
            }

            // 2. 恢复 1RM 推算记录
            val estObj = root.optJSONObject("estimateHistory")
            if (estObj != null) {
                val estHistory = LiftEstimateHistory(
                    squatWeight = estObj.optString("squatWeight", "80.0"),
                    squatReps = estObj.optString("squatReps", "5"),
                    benchWeight = estObj.optString("benchWeight", "60.0"),
                    benchReps = estObj.optString("benchReps", "5"),
                    deadliftWeight = estObj.optString("deadliftWeight", "105.0"),
                    deadliftReps = estObj.optString("deadliftReps", "5")
                )
                UserPreferencesStore.saveEstimateHistory(estHistory, spModule)
            }

            // 3. 恢复当前循环日
            val currentDayStr = root.optString("currentDay", "PUSH")
            try {
                val day = DayType.valueOf(currentDayStr)
                PlanRepository.setCurrentDay(day, spModule)
            } catch (_: Exception) {}

            // 4. 恢复计划编排
            val plansObj = root.optJSONObject("plans")
            if (plansObj != null) {
                DayType.entries.filter { it != DayType.REST }.forEach { day ->
                    val arr = plansObj.optJSONArray(day.name)
                    if (arr != null) {
                        spModule.setString("reshape_plan_day_" + day.name, arr.toString())
                    }
                }
            }

            // 5. 恢复 8 次中周期
            val cycleObj = root.optJSONObject("trainingCycle")
            if (cycleObj != null) {
                spModule.setString("reshape_current_training_cycle_v2", cycleObj.toString())
            }

            // 6. 恢复自定义动作
            val customArr = root.optJSONArray("customExercises")
            if (customArr != null) {
                spModule.setString("reshape_custom_exercises_v3", customArr.toString())
            }

            // 7. 恢复训练历史流水
            val historyArr = root.optJSONArray("workoutHistory")
            if (historyArr != null) {
                spModule.setString("reshape_workout_history_v1", historyArr.toString())
            }

            // 8. 标记新手引导已完成
            UserPreferencesStore.setOnboardingCompleted(true, spModule)

            // 9. 强制全量重新加载所有内存 Store 缓存
            reloadAllMemoryStores(spModule)

            return true
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 重新加载各个 Store 的内存缓存
     */
    fun reloadAllMemoryStores(spModule: SharedPreferencesModule?) {
        if (spModule == null) return
        try {
            UserPreferencesStore.getProfile(spModule)
            WorkoutHistoryStore.getHistory(spModule)
            TrainingCycleStore.getCycle(spModule)
            ExerciseLibraryRepository.reloadFromStorage(spModule)
            PlanRepository.reloadFromStorage(spModule)
        } catch (_: Exception) {}
    }

    /**
     * 导出全量训练历史流水为 CSV 格式 (带 UTF-8 BOM，支持 Excel 直接双击打开)
     */
    fun exportTrainingHistoryCsv(spModule: SharedPreferencesModule? = null): String {
        val historyList = WorkoutHistoryStore.getHistory(spModule)
        val sb = StringBuilder()

        // 写入 UTF-8 BOM 标头，防止 Windows Excel 中文乱码
        sb.append("\uFEFF")

        // 表头
        sb.append("训练会话ID,日期时间戳,训练分化,动作名称,主要肌群,组号,组类型,负重(kg),有效次数,左侧次数,右侧次数,RPE力竭度,是否突破PR,组间休息(s),当日总吨位(kg)\n")

        for (session in historyList) {
            val dateStr = session.finishedAt ?: session.startedAt
            val dayCode = session.planContext.dayCode + " · " + session.planContext.dayTitle
            val totalTonnage = session.sessionMetrics.totalTonnageKg

            for (ex in session.exercises) {
                val exName = ex.name.replace(",", " ")
                val category = ex.category.replace(",", " ")

                for (set in ex.sets) {
                    val setType = if (set.isWarmup) "科学热身组" else "正式工作组"
                    val left = set.leftReps?.toString() ?: "-"
                    val right = set.rightReps?.toString() ?: "-"
                    val isPrStr = if (set.isPr) "突破PR ★" else "-"

                    sb.append("${session.sessionId},")
                    sb.append("${dateStr},")
                    sb.append("\"${dayCode}\",")
                    sb.append("\"${exName}\",")
                    sb.append("\"${category}\",")
                    sb.append("${set.setNo},")
                    sb.append("${setType},")
                    sb.append("${set.weightKg},")
                    sb.append("${set.effectiveReps},")
                    sb.append("${left},")
                    sb.append("${right},")
                    sb.append("${set.rpe},")
                    sb.append("${isPrStr},")
                    sb.append("${set.restSeconds},")
                    sb.append("${totalTonnage}\n")
                }
            }
        }

        return sb.toString()
    }
}
