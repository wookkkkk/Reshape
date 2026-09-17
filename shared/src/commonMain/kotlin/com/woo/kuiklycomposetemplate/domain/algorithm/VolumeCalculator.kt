package com.woo.kuiklycomposetemplate.domain.algorithm

import com.woo.kuiklycomposetemplate.domain.model.SessionExercise
import com.woo.kuiklycomposetemplate.domain.model.SessionSet
import kotlin.math.round

/**
 * 训练容量与有效做组统计引擎 (资深力量特化版)
 *
 * 核心力学计算规则：
 * 1. 热身组 (isWarmup == true) 严格不计入容量 (PRD 规范)；
 * 2. 单侧动作 (isUnilateral == true，如保加利亚单腿蹲、单手下拉) 双侧均完成，有效容量按双侧折算 (x2)；
 * 3. 自重/加重动作 (如双杠臂屈伸、山羊挺身、引体向上)：结合训练者真实体重折算机械做功负荷：
 *    - 双杠臂屈伸 / 引体向上: (体重 x 1.0 + 外挂负重) x 次数
 *    - 山羊挺身 (后链挺身): (体重 x 0.5 + 外挂负重) x 次数
 *    - 彻底消除自重做组为 0kg 导致超量恢复与疲劳评估严重失真。
 */
object VolumeCalculator {

    /**
     * 计算单组有效容量 (kg)
     *
     * @param set 做组明细
     * @param isUnilateral 是否单侧动作 (true 则乘 2)
     * @param userBodyWeightKg 用户体重 (kg)
     * @param exerciseName 动作名称 (用于自重系数推算)
     */
    fun calculateSetVolume(
        set: SessionSet,
        isUnilateral: Boolean = false,
        userBodyWeightKg: Float = 0f,
        exerciseName: String = ""
    ): Float {
        if (set.isWarmup) return 0f

        val bodyWeightContribution = if (userBodyWeightKg > 20f) {
            when {
                exerciseName.contains("双杠") || exerciseName.contains("引体") || (exerciseName.contains("臂屈伸") && !exerciseName.contains("绳索") && !exerciseName.contains("哑铃") && !exerciseName.contains("仰卧")) -> {
                    userBodyWeightKg * 1.0f
                }
                exerciseName.contains("山羊挺身") || exerciseName.contains("挺身") -> {
                    userBodyWeightKg * 0.5f
                }
                else -> 0f
            }
        } else {
            0f
        }

        val effectiveWeight = set.weightKg + bodyWeightContribution
        val multiplier = if (isUnilateral) 2.0f else 1.0f
        return effectiveWeight * set.effectiveReps * multiplier
    }

    /**
     * 计算单动作的总有效容量 (kg)
     */
    fun calculateExerciseVolume(exercise: SessionExercise, userBodyWeightKg: Float = 0f): Float {
        var total = 0f
        for (set in exercise.sets) {
            total += calculateSetVolume(set, exercise.isUnilateral, userBodyWeightKg, exercise.name)
        }
        return total
    }

    /**
     * 计算整场训练总吨位容量 (kg)
     */
    fun calculateTotalVolume(exercises: List<SessionExercise>, userBodyWeightKg: Float = 0f): Float {
        var total = 0f
        for (exercise in exercises) {
            total += calculateExerciseVolume(exercise, userBodyWeightKg)
        }
        return round(total * 10f) / 10f
    }

    /**
     * 统计整场训练的有效正式做组总数 (不含热身组)
     */
    fun countWorkingSets(exercises: List<SessionExercise>): Int {
        var count = 0
        for (exercise in exercises) {
            count += exercise.sets.count { !it.isWarmup }
        }
        return count
    }

    /**
     * 统计整场训练的热身组总数
     */
    fun countWarmupSets(exercises: List<SessionExercise>): Int {
        var count = 0
        for (exercise in exercises) {
            count += exercise.sets.count { it.isWarmup }
        }
        return count
    }

    /**
     * 统计整场训练破 PR 的动作数量与次数
     */
    fun countPrSets(exercises: List<SessionExercise>): Int {
        var count = 0
        for (exercise in exercises) {
            count += exercise.sets.count { it.isPr }
        }
        return count
    }
}
