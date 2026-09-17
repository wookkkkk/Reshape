package com.woo.kuiklycomposetemplate.domain.algorithm

import com.woo.kuiklycomposetemplate.domain.model.DayType
import com.woo.kuiklycomposetemplate.domain.model.Exercise
import com.woo.kuiklycomposetemplate.domain.model.UserProfile
import kotlin.math.max

/**
 * 科学热身阶梯结构
 */
data class WarmupStep(
    val stepIndex: Int,       // 1, 2, 3 阶
    val weightKg: Float,      // 建议热身重量 (kg)
    val reps: Int,            // 建议热身次数
    val percentage: Float,    // 占正式组重量百分比
    val restSeconds: Int,     // 建议热身间歇
    val label: String         // 阶段说明 (如 "动态激活", "神经唤醒", "负重试探")
)

/**
 * 逐组波浪负荷计算结果 (用于 15/12/10/8 等波浪推进预览)
 */
data class WaveSetSuggestion(
    val setIndex: Int,
    val targetReps: Int,
    val suggestedWeightKg: Float,
    val rpe: Float = 8.0f
)

/**
 * 智能重量建议与热身阶梯生成引擎
 */
object WeightSuggestEngine {

    /**
     * 根据三大项 1RM 自动计算动作的初始建议做组重量
     * (PRD 4.6 生物力学换算映射公式)
     */
    fun calculateInitialWeightFrom1Rm(exercise: Exercise, userProfile: UserProfile): Float {
        val base1Rm = when (exercise.related1RmKey) {
            "bench" -> if (userProfile.bench1RmKg > 0f) userProfile.bench1RmKg else 70.0f
            "squat" -> if (userProfile.squat1RmKg > 0f) userProfile.squat1RmKg else 90.0f
            "deadlift" -> if (userProfile.deadlift1RmKg > 0f) userProfile.deadlift1RmKg else 120.0f
            else -> when (exercise.dayType) {
                DayType.PUSH -> if (userProfile.bench1RmKg > 0f) userProfile.bench1RmKg else 70.0f
                DayType.LEGS -> if (userProfile.squat1RmKg > 0f) userProfile.squat1RmKg else 90.0f
                DayType.PULL -> if (userProfile.deadlift1RmKg > 0f) userProfile.deadlift1RmKg else 120.0f
                DayType.REST -> 70.0f
            }
        }

        val ratio = exercise.ratioOf1Rm ?: when {
            exercise.name.contains("卧推") && exercise.name.contains("哑铃") -> 0.35f
            exercise.name.contains("卧推") -> 1.0f
            exercise.name.contains("推举") -> 0.55f
            exercise.name.contains("侧平举") -> 0.06f
            exercise.name.contains("下压") || exercise.name.contains("臂屈伸") -> 0.25f
            exercise.name.contains("硬拉") -> 1.0f
            exercise.name.contains("高位下拉") -> 0.55f
            exercise.name.contains("划船") -> 0.50f
            exercise.name.contains("弯举") -> 0.15f
            exercise.name.contains("深蹲") -> 1.0f
            exercise.name.contains("倒蹬") || exercise.name.contains("腿举") -> 1.60f
            exercise.name.contains("分腿蹲") -> 0.25f
            exercise.name.contains("腿屈伸") -> 0.40f
            exercise.name.contains("腿弯举") -> 0.35f
            else -> 0.50f
        }

        val estimated1Rm = base1Rm * ratio
        val reps = if (exercise.targetRepsMin > 0) exercise.targetRepsMin else 8
        val step = if (ratio <= 0.15f) 1.25f else 2.5f

        val calculated = BrzyckiCalculator.calculateWeightForReps(estimated1Rm, reps, roundToStep = step)
        return max(step, calculated)
    }

    /**
     * 生成 3 阶科学热身阶梯 (科学梯度，激活 CNS 神经中枢，避免提前堆积乳酸，热身组严格隔离于总容量)
     *
     * @param workingWeightKg 目标正式组第一组重量 (kg)
     * @param barWeightKg 空杆重量 (默认 20kg)
     */
    fun generateWarmupLadder(workingWeightKg: Float, barWeightKg: Float = 20.0f): List<WarmupStep> {
        val minBase = if (workingWeightKg > 0f) workingWeightKg else 40.0f
        if (minBase <= 10f) {
            return emptyList()
        }

        val step1Min = if (minBase >= 30f) barWeightKg else 5.0f
        // 阶梯 1: 50% 负荷或空杆 (10 次，关节滑液分泌与动作轨迹锁定)
        val step1Weight = BrzyckiCalculator.roundTo(max(step1Min, minBase * 0.5f), 2.5f)
        // 阶梯 2: 70% 负荷 (5 次，募集快肌纤维)
        val step2Weight = BrzyckiCalculator.roundTo(max(step1Weight + 2.5f, minBase * 0.7f), 2.5f)
        // 阶梯 3: 85% 负荷 (2~3 次，仅适应负重压迫感，零疲劳)
        val step3Weight = BrzyckiCalculator.roundTo(max(step2Weight + 2.5f, minBase * 0.85f), 2.5f)

        return listOf(
            WarmupStep(
                stepIndex = 1,
                weightKg = step1Weight,
                reps = 10,
                percentage = 0.50f,
                restSeconds = 45,
                label = "动态激活"
            ),
            WarmupStep(
                stepIndex = 2,
                weightKg = step2Weight,
                reps = 5,
                percentage = 0.70f,
                restSeconds = 60,
                label = "神经唤醒"
            ),
            WarmupStep(
                stepIndex = 3,
                weightKg = step3Weight,
                reps = 2,
                percentage = 0.85f,
                restSeconds = 90,
                label = "负重试探"
            )
        )
    }

    /**
     * 逐组波浪/金字塔负荷推算 (PRD 4.6 规范：逐组严格单调递增，防平梯保障)
     * 针对 15/12/10/8 或每组次数不同的波浪推进，按每组目标次数分别计算 RPE 8 建议重量
     * 若用户指定了首组重量 (baseFirstSetWeightKg)，则以首组重量反推 1RM 后平滑推导后续各组重量
     */
    fun calculateWaveLoads(
        oneRmKg: Float,
        repsSequence: List<Int>,
        targetRpe: Float = 8.0f,
        roundToStep: Float = 2.5f,
        baseFirstSetWeightKg: Float? = null
    ): List<WaveSetSuggestion> {
        if (repsSequence.isEmpty()) return emptyList()
        val firstReps = repsSequence.first()
        val effective1Rm = if (baseFirstSetWeightKg != null && baseFirstSetWeightKg > 0f) {
            BrzyckiCalculator.calculate1RM(baseFirstSetWeightKg, firstReps)
        } else if (oneRmKg > 0f) {
            oneRmKg
        } else {
            70.0f
        }

        val results = mutableListOf<WaveSetSuggestion>()
        var previousWeight = 0.0f

        repsSequence.forEachIndexed { idx, reps ->
            val rawCalc = if (idx == 0 && baseFirstSetWeightKg != null && baseFirstSetWeightKg > 0f) {
                baseFirstSetWeightKg
            } else {
                BrzyckiCalculator.calculateWeightForReps(effective1Rm, reps, roundToStep)
            }

            // 防平梯保障：当次数递减时 (如 15 -> 12 -> 10 -> 8)，重量必须单调严格递增
            val finalWeight = if (idx > 0 && reps < repsSequence[idx - 1]) {
                max(previousWeight + roundToStep, rawCalc)
            } else {
                rawCalc
            }

            previousWeight = finalWeight
            results.add(WaveSetSuggestion(idx + 1, reps, finalWeight, targetRpe))
        }

        return results
    }

    /**
     * 根据 1RM 与目标做组次数区间计算建议起始重量
     */
    fun suggestWorkingWeight(
        oneRmKg: Float,
        targetRepsMin: Int = 6,
        targetRepsMax: Int = 8,
        isFatigued: Boolean = false
    ): Float {
        if (oneRmKg <= 0f) return 20.0f

        val avgReps = (targetRepsMin + targetRepsMax) / 2
        var suggested = BrzyckiCalculator.calculateWeightForReps(oneRmKg, avgReps, roundToStep = 2.5f)

        if (isFatigued) {
            suggested = BrzyckiCalculator.roundTo(suggested * 0.9f, 2.5f)
        }

        return max(2.5f, suggested)
    }
}
