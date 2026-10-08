package com.woo.kuiklycomposetemplate.data.local

import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.woo.kuiklycomposetemplate.domain.model.DayType
import com.woo.kuiklycomposetemplate.domain.model.Exercise
import com.woo.kuiklycomposetemplate.domain.model.JointType
import com.woo.kuiklycomposetemplate.domain.model.LoadType
import com.woo.kuiklycomposetemplate.domain.model.PresetExercises
import com.woo.kuiklycomposetemplate.domain.model.ProgressionType

/**
 * 训练计划与动作编排仓库 (支持本地持久化、个性化组次数/间歇独立配置与官方经典模板重置)
 */
object PlanRepository {

    private const val KEY_PLAN_PREFIX = "reshape_plan_day_"
    private const val KEY_CURRENT_DAY = "reshape_current_day"
    private val dayExercisesMap = mutableMapOf<DayType, MutableList<Exercise>>()
    private var currentDay: DayType = DayType.PUSH
    private var isLoadedFromSp = false

    init {
        resetToDefault()
    }

    /**
     * 重置为官方经典 5 动作模板
     */
    fun resetToDefault(dayType: DayType? = null, spModule: SharedPreferencesModule? = null) {
        val profile = UserPreferencesStore.getProfile(spModule)
        if (dayType != null) {
            val list = PresetExercises.getExercisesForDay(dayType).map { ex ->
                val initWeight = if (ex.targetWeightKg <= 0f || ex.targetWeightKg == 40.0f) {
                    com.woo.kuiklycomposetemplate.domain.algorithm.WeightSuggestEngine.calculateInitialWeightFrom1Rm(ex, profile)
                } else ex.targetWeightKg

                val waveWeights = if (ex.targetRepsSequence.isNotBlank()) {
                    val reps = ex.getPerSetRepsList()
                    val waveSuggestions = com.woo.kuiklycomposetemplate.domain.algorithm.WeightSuggestEngine.calculateWaveLoads(
                        oneRmKg = when (ex.related1RmKey) {
                            "bench" -> profile.bench1RmKg
                            "squat" -> profile.squat1RmKg
                            "deadlift" -> profile.deadlift1RmKg
                            else -> 70.0f
                        },
                        repsSequence = reps,
                        targetRpe = ex.targetRpe,
                        baseFirstSetWeightKg = initWeight
                    )
                    waveSuggestions.joinToString("/") { it.suggestedWeightKg.toString() }
                } else ""

                ex.copy(targetWeightKg = initWeight, waveWeightsSequence = waveWeights)
            }.toMutableList()
            dayExercisesMap[dayType] = list
            if (spModule != null) {
                persistDayPlan(dayType, spModule)
            }
        } else {
            DayType.entries.filter { it != DayType.REST }.forEach { d ->
                val list = PresetExercises.getExercisesForDay(d).map { ex ->
                    val initWeight = if (ex.targetWeightKg <= 0f || ex.targetWeightKg == 40.0f) {
                        com.woo.kuiklycomposetemplate.domain.algorithm.WeightSuggestEngine.calculateInitialWeightFrom1Rm(ex, profile)
                    } else ex.targetWeightKg

                    val waveWeights = if (ex.targetRepsSequence.isNotBlank()) {
                        val reps = ex.getPerSetRepsList()
                        val waveSuggestions = com.woo.kuiklycomposetemplate.domain.algorithm.WeightSuggestEngine.calculateWaveLoads(
                            oneRmKg = when (ex.related1RmKey) {
                                "bench" -> profile.bench1RmKg
                                "squat" -> profile.squat1RmKg
                                "deadlift" -> profile.deadlift1RmKg
                                else -> 70.0f
                            },
                            repsSequence = reps,
                            targetRpe = ex.targetRpe,
                            baseFirstSetWeightKg = initWeight
                        )
                        waveSuggestions.joinToString("/") { it.suggestedWeightKg.toString() }
                    } else ""

                    ex.copy(targetWeightKg = initWeight, waveWeightsSequence = waveWeights)
                }.toMutableList()
                dayExercisesMap[d] = list
                if (spModule != null) {
                    persistDayPlan(d, spModule)
                }
            }
        }
    }

    fun getExercises(dayType: DayType, spModule: SharedPreferencesModule? = null): List<Exercise> {
        ensureLoaded(spModule)
        val list = dayExercisesMap[dayType] ?: PresetExercises.getExercisesForDay(dayType).toMutableList()
        dayExercisesMap[dayType] = list
        return list.toList()
    }

    fun saveCustomPlan(dayType: DayType, exercises: List<Exercise>, spModule: SharedPreferencesModule? = null) {
        dayExercisesMap[dayType] = exercises.toMutableList()
        persistDayPlan(dayType, spModule)
    }

    fun resetToDefaultPlan(dayType: DayType, spModule: SharedPreferencesModule? = null) {
        resetToDefault(dayType, spModule)
    }

    /**
     * 1RM 基准更新后，批量刷新与 1RM 挂钩动作的建议起始重量与波浪序列
     */
    fun refreshSuggestedWeightsWithProfile(profile: com.woo.kuiklycomposetemplate.domain.model.UserProfile, spModule: SharedPreferencesModule? = null) {
        ensureLoaded(spModule)
        DayType.entries.filter { it != DayType.REST }.forEach { d ->
            val list = dayExercisesMap[d] ?: return@forEach
            val updatedList = list.map { ex ->
                if (!ex.related1RmKey.isNullOrBlank()) {
                    val newWeight = com.woo.kuiklycomposetemplate.domain.algorithm.WeightSuggestEngine.calculateInitialWeightFrom1Rm(ex, profile)
                    val waveWeights = if (ex.targetRepsSequence.isNotBlank()) {
                        val reps = ex.getPerSetRepsList()
                        val waveSuggestions = com.woo.kuiklycomposetemplate.domain.algorithm.WeightSuggestEngine.calculateWaveLoads(
                            oneRmKg = when (ex.related1RmKey) {
                                "bench" -> profile.bench1RmKg
                                "squat" -> profile.squat1RmKg
                                "deadlift" -> profile.deadlift1RmKg
                                else -> 70.0f
                            },
                            repsSequence = reps,
                            targetRpe = ex.targetRpe,
                            baseFirstSetWeightKg = newWeight
                        )
                        waveSuggestions.joinToString("/") { it.suggestedWeightKg.toString() }
                    } else ex.waveWeightsSequence
                    ex.copy(targetWeightKg = newWeight, waveWeightsSequence = waveWeights)
                } else ex
            }.toMutableList()
            dayExercisesMap[d] = updatedList
            persistDayPlan(d, spModule)
        }
    }

    fun getCurrentDay(spModule: SharedPreferencesModule? = null): DayType {
        if (spModule != null) {
            try {
                val saved = spModule.getString(KEY_CURRENT_DAY)
                if (!saved.isNullOrEmpty()) {
                    currentDay = DayType.valueOf(saved)
                }
            } catch (_: Exception) {}
        }
        return currentDay
    }

    fun setCurrentDay(dayType: DayType, spModule: SharedPreferencesModule? = null) {
        currentDay = dayType
        if (spModule != null) {
            try {
                spModule.setString(KEY_CURRENT_DAY, dayType.name)
            } catch (_: Exception) {}
        }
    }

    fun advanceToNextDay(spModule: SharedPreferencesModule? = null): DayType {
        currentDay = when (currentDay) {
            DayType.PUSH -> DayType.PULL
            DayType.PULL -> DayType.LEGS
            DayType.LEGS -> DayType.PUSH
            DayType.REST -> DayType.PUSH
        }
        if (spModule != null) {
            try {
                spModule.setString(KEY_CURRENT_DAY, currentDay.name)
            } catch (_: Exception) {}
        }
        return currentDay
    }

    /**
     * 重新排序动作
     */
    fun reorderExercises(dayType: DayType, fromIndex: Int, toIndex: Int, spModule: SharedPreferencesModule? = null) {
        val list = dayExercisesMap[dayType] ?: return
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            persistDayPlan(dayType, spModule)
        }
    }

    /**
     * 添加动作到当前计划
     */
    fun addExercise(dayType: DayType, exercise: Exercise, spModule: SharedPreferencesModule? = null) {
        val list = dayExercisesMap.getOrPut(dayType) { mutableListOf() }
        list.add(exercise)
        persistDayPlan(dayType, spModule)
    }

    /**
     * 更新指定动作的配置 (如组间休息时长、目标次数、RPE 等)
     */
    fun updateExercise(dayType: DayType, updatedExercise: Exercise, spModule: SharedPreferencesModule? = null) {
        val list = dayExercisesMap[dayType] ?: return
        val index = list.indexOfFirst { it.id == updatedExercise.id }
        if (index >= 0) {
            list[index] = updatedExercise
            persistDayPlan(dayType, spModule)
        }
    }

    /**
     * 移除动作
     */
    fun removeExercise(dayType: DayType, exerciseId: String, spModule: SharedPreferencesModule? = null) {
        val list = dayExercisesMap[dayType] ?: return
        list.removeAll { it.id == exerciseId }
        persistDayPlan(dayType, spModule)
    }

    fun reloadFromStorage(spModule: SharedPreferencesModule?) {
        dayExercisesMap.clear()
        isLoadedFromSp = false
        ensureLoaded(spModule)
    }

    private fun ensureLoaded(spModule: SharedPreferencesModule?) {
        if (isLoadedFromSp || spModule == null) return
        try {
            DayType.entries.filter { it != DayType.REST }.forEach { d ->
                val jsonStr = spModule.getString(KEY_PLAN_PREFIX + d.name)
                if (!jsonStr.isNullOrEmpty()) {
                    val arr = JSONArray(jsonStr)
                    val list = mutableListOf<Exercise>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        list.add(exerciseFromJson(obj))
                    }
                    if (list.isNotEmpty()) {
                        dayExercisesMap[d] = list
                    }
                }
            }
            isLoadedFromSp = true
        } catch (_: Exception) {}
    }

    private fun persistDayPlan(dayType: DayType, spModule: SharedPreferencesModule?) {
        if (spModule == null) return
        try {
            val list = dayExercisesMap[dayType] ?: return
            val arr = JSONArray()
            for (ex in list) {
                arr.put(exerciseToJson(ex))
            }
            spModule.setString(KEY_PLAN_PREFIX + dayType.name, arr.toString())
        } catch (_: Exception) {}
    }

    private fun exerciseToJson(ex: Exercise): JSONObject {
        val obj = JSONObject()
        obj.put("id", ex.id)
        obj.put("name", ex.name)
        obj.put("dayType", ex.dayType.name)
        obj.put("primaryMuscle", ex.primaryMuscle)
        val secArr = JSONArray()
        for (m in ex.secondaryMuscles) secArr.put(m)
        obj.put("secondaryMuscles", secArr)
        obj.put("targetMuscle", ex.displayMuscleSummary)
        obj.put("loadType", ex.loadType.name)
        obj.put("primaryJoint", ex.primaryJoint.name)
        obj.put("isCustom", ex.isCustom)
        obj.put("isUnilateral", ex.isUnilateral)
        obj.put("defaultHoleCues", ex.defaultHoleCues)
        obj.put("defaultSets", ex.defaultSets)
        obj.put("targetRepsMin", ex.targetRepsMin)
        obj.put("targetRepsMax", ex.targetRepsMax)
        obj.put("targetRepsSequence", ex.targetRepsSequence)
        obj.put("targetWeightKg", ex.targetWeightKg.toDouble())
        obj.put("waveWeightsSequence", ex.waveWeightsSequence)
        obj.put("targetRpe", ex.targetRpe.toDouble())
        obj.put("defaultRestSeconds", ex.defaultRestSeconds)
        obj.put("progressionType", ex.progressionType.name)
        if (!ex.related1RmKey.isNullOrBlank()) {
            obj.put("related1RmKey", ex.related1RmKey)
        }
        if (ex.ratioOf1Rm != null) {
            obj.put("ratioOf1Rm", ex.ratioOf1Rm.toDouble())
        }

        val painFreeArr = JSONArray()
        for (iid in ex.painFreeVariantIds) painFreeArr.put(iid)
        obj.put("painFreeVariantIds", painFreeArr)

        val interArr = JSONArray()
        for (iid in ex.interchangeableExerciseIds) interArr.put(iid)
        obj.put("interchangeableExerciseIds", interArr)

        obj.put("enableWarmupLadder", ex.enableWarmupLadder)
        return obj
    }

    private fun exerciseFromJson(obj: JSONObject): Exercise {
        val dayName = obj.optString("dayType", "PUSH")
        val day = try { DayType.valueOf(dayName) } catch (_: Exception) { DayType.PUSH }
        val loadName = obj.optString("loadType", "BARBELL")
        val load = try { LoadType.valueOf(loadName) } catch (_: Exception) { LoadType.BARBELL }
        val jointName = obj.optString("primaryJoint", "NONE")
        val joint = try { JointType.valueOf(jointName) } catch (_: Exception) { JointType.NONE }
        val progName = obj.optString("progressionType", "DOUBLE_PROGRESSION")
        val prog = try { ProgressionType.valueOf(progName) } catch (_: Exception) { ProgressionType.DOUBLE_PROGRESSION }

        val secList = mutableListOf<String>()
        val secArr = obj.optJSONArray("secondaryMuscles")
        if (secArr != null) {
            for (i in 0 until secArr.length()) {
                val str = secArr.optString(i)
                if (!str.isNullOrBlank()) secList.add(str)
            }
        }

        val painFreeList = mutableListOf<String>()
        val painFreeArr = obj.optJSONArray("painFreeVariantIds")
        if (painFreeArr != null) {
            for (i in 0 until painFreeArr.length()) {
                val str = painFreeArr.optString(i)
                if (!str.isNullOrBlank()) painFreeList.add(str)
            }
        }

        val interList = mutableListOf<String>()
        val interArr = obj.optJSONArray("interchangeableExerciseIds")
        if (interArr != null) {
            for (i in 0 until interArr.length()) {
                val str = interArr.optString(i)
                if (!str.isNullOrBlank()) interList.add(str)
            }
        }

        val priMuscle = obj.optString("primaryMuscle", "目标肌群")
        val tgtMuscle = obj.optString("targetMuscle", priMuscle)

        return Exercise(
            id = obj.optString("id", "ex_${obj.optString("name").hashCode()}"),
            name = obj.optString("name", "训练动作"),
            dayType = day,
            primaryMuscle = priMuscle,
            secondaryMuscles = secList,
            targetMuscle = tgtMuscle,
            loadType = load,
            primaryJoint = joint,
            isCustom = obj.optBoolean("isCustom", false),
            isUnilateral = obj.optBoolean("isUnilateral", false),
            defaultHoleCues = obj.optString("defaultHoleCues", ""),
            defaultSets = obj.optInt("defaultSets", 4),
            targetRepsMin = obj.optInt("targetRepsMin", 8),
            targetRepsMax = obj.optInt("targetRepsMax", 8),
            targetRepsSequence = obj.optString("targetRepsSequence", ""),
            targetWeightKg = obj.optDouble("targetWeightKg", 40.0).toFloat(),
            waveWeightsSequence = obj.optString("waveWeightsSequence", ""),
            targetRpe = obj.optDouble("targetRpe", 8.0).toFloat(),
            defaultRestSeconds = obj.optInt("defaultRestSeconds", 120),
            progressionType = prog,
            related1RmKey = obj.optString("related1RmKey", "").ifBlank { null },
            ratioOf1Rm = if (obj.has("ratioOf1Rm")) obj.optDouble("ratioOf1Rm", 0.0).toFloat() else null,
            painFreeVariantIds = painFreeList,
            interchangeableExerciseIds = interList,
            enableWarmupLadder = obj.optBoolean("enableWarmupLadder", true)
        )
    }
}
