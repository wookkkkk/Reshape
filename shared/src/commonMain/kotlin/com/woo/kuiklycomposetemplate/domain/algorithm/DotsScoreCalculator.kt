package com.woo.kuiklycomposetemplate.domain.algorithm

import kotlin.math.round

/**
 * DOTS 力量系数与综合评分引擎
 * 国际力量举 (Powerlifting) 权威通用相对力量评分体系
 * 严格按照男女双标准四次多项式公式计算
 */
object DotsScoreCalculator {

    // 男性四次多项式参数 (Male coefficients)
    private val MALE_COEFFS = doubleArrayOf(
        -307.75076,
        24.0900756,
        -0.1918759221,
        0.0007391293,
        -0.000001093
    )

    // 女性四次多项式参数 (Female coefficients)
    private val FEMALE_COEFFS = doubleArrayOf(
        -57.96288,
        13.6175032,
        -0.1126655495,
        0.0005158568,
        -0.0000010706
    )

    /**
     * 计算 DOTS 力量系数
     * Formula: DOTS = Total (kg) * (500 / (a0 + a1*bw + a2*bw^2 + a3*bw^3 + a4*bw^4))
     *
     * @param totalKg 三大项总重量或极限总成绩 (kg)
     * @param bodyWeightKg 真实体重 (kg)
     * @param isMale 生理性别（true=男, false=女）
     * @return DOTS 评分（保留 1 位小数）
     */
    fun calculateDots(totalKg: Float, bodyWeightKg: Float, isMale: Boolean): Float {
        if (totalKg <= 0f || bodyWeightKg <= 20f) return 0f

        val coeffs = if (isMale) MALE_COEFFS else FEMALE_COEFFS
        val bw = bodyWeightKg.toDouble()

        val denom = coeffs[0] +
                coeffs[1] * bw +
                coeffs[2] * bw * bw +
                coeffs[3] * bw * bw * bw +
                coeffs[4] * bw * bw * bw * bw

        if (denom <= 0.0) return 0f

        val score = totalKg.toDouble() * (500.0 / denom)
        return (round(score * 10.0) / 10.0).toFloat()
    }

    /**
     * 根据 DOTS 分数评定力量段位与称号
     */
    fun getStrengthLevel(dotsScore: Float): StrengthLevel {
        return when {
            dotsScore < 200f -> StrengthLevel("初学力量", "筑基起步", "#9BA3AF")
            dotsScore < 300f -> StrengthLevel("入门铁友", "规律做组", "#3B82F6")
            dotsScore < 380f -> StrengthLevel("进阶力量", "核心骨干", "#10B981")
            dotsScore < 440f -> StrengthLevel("资深健力", "高阶老铁", "#F97316")
            else -> StrengthLevel("精英力量", "战神天花板", "#E5A93C")
        }
    }

    fun getStrengthTierTitle(dotsScore: Float, isMale: Boolean = true): String {
        return getStrengthLevel(dotsScore).title
    }

    data class StrengthLevel(
        val title: String,
        val subtitle: String,
        val colorHex: String
    )
}
