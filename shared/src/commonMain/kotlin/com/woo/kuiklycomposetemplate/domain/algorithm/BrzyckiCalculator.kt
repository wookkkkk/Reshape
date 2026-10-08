package com.woo.kuiklycomposetemplate.domain.algorithm

import kotlin.math.max
import kotlin.math.round

/**
 * Brzycki 极限力量 (1RM) 推算与反推引擎
 * 严格对齐 PRD 4.1 规范，所有输出默认以 2.5kg 步长四舍五入
 */
object BrzyckiCalculator {

    /**
     * Brzycki 1RM 推算：
     * 1RM = w / (1.0278 - 0.0278 * r)
     * 四舍五入至 2.5kg 步长
     *
     * @param weightKg 实际做组重量 (kg)
     * @param reps 实际完成次数 (推荐 1~12 次区间最为精准)
     * @param roundToStep 步长四舍五入，默认 2.5kg，若设为 0f 则返回精确浮点数
     */
    fun calculate1RM(weightKg: Float, reps: Int, roundToStep: Float = 2.5f): Float {
        if (weightKg <= 0f) return 0f
        if (reps <= 1) return if (roundToStep > 0f) roundTo(weightKg, roundToStep) else weightKg
        
        // 超过 12 次时递减精准度，但仍保证基础推算
        val safeReps = reps.coerceIn(1, 15)
        val raw = weightKg / (1.0278f - 0.0278f * safeReps)
        
        return if (roundToStep > 0f) roundTo(raw, roundToStep) else raw
    }

    /**
     * 根据 1RM 反推目标次数对应的做组重量：
     * weight = 1RM * (1.0278 - 0.0278 * targetReps)
     */
    fun calculateWeightForReps(oneRmKg: Float, targetReps: Int, roundToStep: Float = 2.5f): Float {
        if (oneRmKg <= 0f) return 0f
        if (targetReps <= 1) return if (roundToStep > 0f) roundTo(oneRmKg, roundToStep) else oneRmKg
        
        val safeReps = targetReps.coerceIn(1, 15)
        val raw = oneRmKg * (1.0278f - 0.0278f * safeReps)
        val bounded = max(0f, raw)
        
        return if (roundToStep > 0f) roundTo(bounded, roundToStep) else bounded
    }

    /**
     * 结合主观力竭度 (RPE) 换算等效 1RM：
     * RIR (Reps In Reserve 剩余储备次数) = 10.0 - RPE
     * 等效总次数 = actualReps + RIR
     */
    fun calculateRpeAdjusted1RM(weightKg: Float, actualReps: Int, rpe: Float, roundToStep: Float = 2.5f): Float {
        if (weightKg <= 0f || actualReps <= 0) return 0f
        val safeRpe = rpe.coerceIn(6.0f, 10.0f)
        val rir = max(0f, 10.0f - safeRpe)
        val estimatedTotalReps = round(actualReps + rir).toInt()
        return calculate1RM(weightKg, estimatedTotalReps, roundToStep)
    }

    /**
     * 辅助函数：四舍五入至指定步长 (如 2.5kg)
     */
    fun roundTo(value: Float, step: Float = 2.5f): Float {
        if (step <= 0f) return value
        return round(value / step) * step
    }
}
