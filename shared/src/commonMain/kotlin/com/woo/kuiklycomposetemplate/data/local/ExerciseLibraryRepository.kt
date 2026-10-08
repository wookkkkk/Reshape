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
 * 动作无痛变式推荐项 (具备明确关节保护目标与生物力学理论)
 */
data class PainFreeVariantInfo(
    val title: String,                    // 变式标题
    val reason: String,                   // 生物力学康复理由 (例如 "限制肩超伸，保护肩袖")
    val targetJoint: JointType,           // 重点保护/减压的关节
    val exercise: Exercise,               // 对应的变式动作实体
    val weightConversionFactor: Float = 0.8f // 建议重量折算系数
)

/**
 * 全量动作库管理中心 (系统预置 + 用户个性化修改覆盖 + 自定义动作 + 生物力学可替换/无痛变式图谱)
 */
object ExerciseLibraryRepository {

    private const val KEY_CUSTOM_EXERCISES = "reshape_custom_exercises_v3"

    // 内存缓存自定义/覆盖动作列表
    private val customExercises = mutableListOf<Exercise>()
    private var isLoadedFromSp = false

    /**
     * 获取全量动作列表 (系统预置 + 用户自定义覆盖)
     */
    fun getAllExercises(spModule: SharedPreferencesModule? = null): List<Exercise> {
        ensureLoaded(spModule)
        val all = mutableListOf<Exercise>()

        // 1. 遍历预置动作，若存在用户自定义覆盖版本则使用覆盖版本
        for (preset in PresetExercises.ALL_PRESETS) {
            val customOverride = customExercises.find { it.id == preset.id }
            if (customOverride != null) {
                all.add(customOverride)
            } else {
                all.add(preset)
            }
        }

        // 2. 追加纯新增的自定义动作
        val pureCustoms = customExercises.filter { custom -> PresetExercises.ALL_PRESETS.none { it.id == custom.id } }
        all.addAll(pureCustoms)
        return all
    }

    /**
     * 按训练日筛选动作
     */
    fun getExercisesByDay(dayType: DayType, spModule: SharedPreferencesModule? = null): List<Exercise> {
        return getAllExercises(spModule).filter { it.dayType == dayType }
    }

    /**
     * 关键字模糊搜索动作 (名称 / 目标肌群 / 器械)
     */
    fun searchExercises(query: String, spModule: SharedPreferencesModule? = null): List<Exercise> {
        val all = getAllExercises(spModule)
        if (query.isBlank()) return all
        val q = query.trim().lowercase()
        return all.filter {
            it.name.lowercase().contains(q) ||
            it.primaryMuscle.lowercase().contains(q) ||
            it.secondaryMuscles.any { m -> m.lowercase().contains(q) } ||
            it.targetMuscle.lowercase().contains(q) ||
            it.loadType.label.lowercase().contains(q)
        }
    }

    /**
     * 根据 ID 查询动作
     */
    fun findById(id: String, spModule: SharedPreferencesModule? = null): Exercise? {
        return getAllExercises(spModule).find { it.id == id }
    }

    /**
     * 获取持久化层中存储的“自定义动作 + 预置动作覆盖项”
     */
    fun getStoredExercises(spModule: SharedPreferencesModule? = null): List<Exercise> {
        ensureLoaded(spModule)
        return customExercises.toList()
    }

    /**
     * 新增用户自定义动作并持久化
     */
    fun createCustomExercise(
        name: String,
        dayType: DayType,
        primaryMuscle: String,
        secondaryMuscles: List<String> = emptyList(),
        loadType: LoadType,
        primaryJoint: JointType = JointType.NONE,
        isUnilateral: Boolean = false,
        defaultHoleCues: String = "",
        defaultSets: Int = 4,
        targetRepsMin: Int = 8,
        targetRepsMax: Int = 10,
        defaultRestSeconds: Int = 90,
        interchangeableExerciseIds: List<String> = emptyList(),
        spModule: SharedPreferencesModule? = null
    ): Exercise {
        ensureLoaded(spModule)
        val newId = "custom_${name.hashCode().coerceAtLeast(0)}_${customExercises.size + 1}"
        val targetMuscleSummary = if (secondaryMuscles.isEmpty()) primaryMuscle else "$primaryMuscle / ${secondaryMuscles.joinToString(" / ")}"

        val newExercise = Exercise(
            id = newId,
            name = name,
            dayType = dayType,
            primaryMuscle = primaryMuscle,
            secondaryMuscles = secondaryMuscles,
            targetMuscle = targetMuscleSummary,
            loadType = loadType,
            primaryJoint = primaryJoint,
            isCustom = true,
            isUnilateral = isUnilateral,
            defaultHoleCues = defaultHoleCues,
            defaultSets = defaultSets,
            targetRepsMin = targetRepsMin,
            targetRepsMax = targetRepsMax,
            targetRpe = 8.0f,
            defaultRestSeconds = defaultRestSeconds,
            progressionType = if (isUnilateral) ProgressionType.UNILATERAL_BALANCE else ProgressionType.DOUBLE_PROGRESSION,
            interchangeableExerciseIds = interchangeableExerciseIds
        )

        customExercises.add(newExercise)
        persistCustomExercises(spModule)
        return newExercise
    }

    /**
     * 保存或更新动作 (支持预置动作个性化编辑与自定义动作更新并持久化)
     */
    fun saveOrUpdateExercise(exercise: Exercise, spModule: SharedPreferencesModule? = null) {
        ensureLoaded(spModule)
        val idx = customExercises.indexOfFirst { it.id == exercise.id }
        if (idx >= 0) {
            customExercises[idx] = exercise
        } else {
            customExercises.add(exercise)
        }
        persistCustomExercises(spModule)
    }

    /**
     * 恢复预置动作至官方初始状态
     */
    fun resetExerciseToPreset(exerciseId: String, spModule: SharedPreferencesModule? = null) {
        ensureLoaded(spModule)
        customExercises.removeAll { it.id == exerciseId }
        persistCustomExercises(spModule)
    }

    /**
     * 删除自定义动作
     */
    fun deleteCustomExercise(exerciseId: String, spModule: SharedPreferencesModule? = null): Boolean {
        ensureLoaded(spModule)
        val removed = customExercises.removeAll { it.id == exerciseId }
        if (removed) {
            persistCustomExercises(spModule)
        }
        return removed
    }

    fun reloadFromStorage(spModule: SharedPreferencesModule?) {
        customExercises.clear()
        isLoadedFromSp = false
        ensureLoaded(spModule)
    }

    /**
     * 获取当前动作的全部可替换动作 (Explicit Mapping + Automatic Biomechanical Inference)
     */
    fun getInterchangeableExercises(exerciseId: String, spModule: SharedPreferencesModule? = null): List<Exercise> {
        val current = findById(exerciseId, spModule) ?: return emptyList()
        val all = getAllExercises(spModule)

        // 1. 显式关联的 ID (包括互换动作与无痛变式)
        val explicitIds = (current.interchangeableExerciseIds + current.painFreeVariantIds).distinct()
        val explicitList = explicitIds.mapNotNull { id -> all.find { it.id == id } }.filter { it.id != current.id }

        // 2. 算法自适应推导：同训练日 + 相同主发力肌群的动作
        val inferredList = all.filter {
            it.id != current.id &&
            it.dayType == current.dayType &&
            (it.primaryMuscle == current.primaryMuscle || it.targetMuscle.contains(current.primaryMuscle.take(3))) &&
            explicitList.none { exp -> exp.id == it.id }
        }

        return (explicitList + inferredList).distinctBy { it.id }
    }

    /**
     * 基于生物力学图谱，动态推导当前动作的「无痛变式」
     */
    fun getPainFreeVariants(exerciseId: String, spModule: SharedPreferencesModule? = null): List<PainFreeVariantInfo> {
        val currentEx = findById(exerciseId, spModule) ?: return emptyList()
        val results = mutableListOf<PainFreeVariantInfo>()

        // 1. 优先从动作自身的 painFreeVariantIds 映射图谱中解析
        for (variantId in currentEx.painFreeVariantIds) {
            val variantEx = findById(variantId, spModule) ?: continue
            val reason = generateBiomechanicalReason(currentEx, variantEx)
            results.add(
                PainFreeVariantInfo(
                    title = variantEx.name,
                    reason = reason,
                    targetJoint = currentEx.primaryJoint,
                    exercise = variantEx,
                    weightConversionFactor = variantEx.ratioOf1Rm ?: 0.8f
                )
            )
        }

        // 2. 若映射为空或不足，根据同训练日 + 关节友好度自适应匹配保底变式
        if (results.isEmpty()) {
            val sameDayVariants = getExercisesByDay(currentEx.dayType, spModule)
                .filter { it.id != currentEx.id && (it.loadType == LoadType.DUMBBELL || it.loadType == LoadType.MACHINE || it.loadType == LoadType.CABLE) }
                .take(2)

            for (variantEx in sameDayVariants) {
                results.add(
                    PainFreeVariantInfo(
                        title = variantEx.name,
                        reason = "采用轨迹更自由/支撑更稳定的器械，减轻关节剪切负荷",
                        targetJoint = currentEx.primaryJoint,
                        exercise = variantEx,
                        weightConversionFactor = 0.8f
                    )
                )
            }
        }

        return results
    }

    private fun generateBiomechanicalReason(base: Exercise, variant: Exercise): String {
        return when {
            base.id.contains("bench") && variant.id.contains("floor") ->
                "限制肩关节过度超伸，彻底保护肩峰间隙与肩袖肌群"
            base.id.contains("bench") && variant.id.contains("dips") ->
                "自由活动腕关节，由胸下缘与肱三头肌主导发力"
            base.id.contains("bench") && variant.id.contains("chest_press") ->
                "固定导轨提供骨盆与背部支撑，消除摇晃与肩部代偿"
            base.id.contains("squat") && variant.id.contains("split") ->
                "腰椎零轴向压迫，弱化膝前侧剪切力，强化臀中肌稳定"
            base.id.contains("squat") && variant.id.contains("leg_press") ->
                "后背完全贴紧靠垫，完全卸除脊柱垂直重压"
            base.id.contains("squat") && variant.id.contains("extension") ->
                "单关节开链孤立做组，避免髋腰深层疲劳"
            base.id.contains("deadlift") && variant.id.contains("rdl") ->
                "专注纯髋铰链折叠，大幅降低下背部启动拉扯压力"
            base.id.contains("deadlift") && variant.id.contains("back_extension") ->
                "固定骨盆，低负荷安全强化竖脊肌与后链"
            base.id.contains("row") && variant.id.contains("chest") ->
                "胸口紧贴斜板，彻底消除下背部剪切力"
            base.id.contains("pulldown") || base.id.contains("pullup") ->
                "采用对握中立位，消除前臂旋前扭矩，保护肘腕关节"
            else ->
                "优化发力力臂，降低主受力关节 (${base.primaryJoint.label}) 剪切压迫"
        }
    }

    private fun ensureLoaded(spModule: SharedPreferencesModule?) {
        if (isLoadedFromSp || spModule == null) return
        try {
            val jsonStr = spModule.getString(KEY_CUSTOM_EXERCISES)
            if (!jsonStr.isNullOrEmpty()) {
                val arr = JSONArray(jsonStr)
                customExercises.clear()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    customExercises.add(exerciseFromJson(obj))
                }
            }
            isLoadedFromSp = true
        } catch (_: Exception) {}
    }

    private fun persistCustomExercises(spModule: SharedPreferencesModule?) {
        if (spModule == null) return
        try {
            val arr = JSONArray()
            for (ex in customExercises) {
                arr.put(exerciseToJson(ex))
            }
            spModule.setString(KEY_CUSTOM_EXERCISES, arr.toString())
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
        obj.put("enableWarmupLadder", ex.enableWarmupLadder)

        val painFreeArr = JSONArray()
        for (iid in ex.painFreeVariantIds) painFreeArr.put(iid)
        obj.put("painFreeVariantIds", painFreeArr)

        val interArr = JSONArray()
        for (iid in ex.interchangeableExerciseIds) interArr.put(iid)
        obj.put("interchangeableExerciseIds", interArr)
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
                if (!str.isNullOrBlank()) {
                    secList.add(str)
                }
            }
        }

        val interList = mutableListOf<String>()
        val interArr = obj.optJSONArray("interchangeableExerciseIds")
        if (interArr != null) {
            for (i in 0 until interArr.length()) {
                val str = interArr.optString(i)
                if (!str.isNullOrBlank()) {
                    interList.add(str)
                }
            }
        }

        val painFreeList = mutableListOf<String>()
        val painFreeArr = obj.optJSONArray("painFreeVariantIds")
        if (painFreeArr != null) {
            for (i in 0 until painFreeArr.length()) {
                val str = painFreeArr.optString(i)
                if (!str.isNullOrBlank()) {
                    painFreeList.add(str)
                }
            }
        }

        val priMuscle = obj.optString("primaryMuscle", "目标肌群")
        val tgtMuscle = obj.optString("targetMuscle", priMuscle)

        return Exercise(
            id = obj.optString("id", "custom_${obj.optString("name").hashCode()}"),
            name = obj.optString("name", "自定义动作"),
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
            defaultRestSeconds = obj.optInt("defaultRestSeconds", 90),
            progressionType = prog,
            related1RmKey = obj.optString("related1RmKey", "").ifBlank { null },
            ratioOf1Rm = if (obj.has("ratioOf1Rm")) obj.optDouble("ratioOf1Rm", 0.0).toFloat() else null,
            painFreeVariantIds = painFreeList,
            interchangeableExerciseIds = interList,
            enableWarmupLadder = obj.optBoolean("enableWarmupLadder", true)
        )
    }
}
