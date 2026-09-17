package com.woo.kuiklycomposetemplate.domain.model

import com.woo.kuiklycomposetemplate.domain.algorithm.DotsScoreCalculator

/**
 * 性别枚举
 */
enum class Gender(val label: String) {
    MALE("男 ♂"),
    FEMALE("女 ♀")
}

/**
 * 用户身体档案与力量基准
 */
data class UserProfile(
    val userId: String = "local_user",
    val nickname: String = "Reshape 老铁",
    val gender: Gender = Gender.MALE,
    val heightCm: Float = 175.0f,
    val bodyWeightKg: Float = 75.0f,
    val targetWeightKg: Float = 72.0f,
    val bodyFatPercentage: Float = 15.0f,
    val skeletalMuscleKg: Float = 36.0f,
    val trainingExpYears: Float = 2.0f,
    val barbellWeightKg: Float = 20.0f,
    // 三大项 1RM (kg)
    val bench1RmKg: Float = 80.0f,
    val squat1RmKg: Float = 110.0f,
    val deadlift1RmKg: Float = 130.0f,
    val preferredThemeCode: String = "ORANGE",
    val soundEnabled: Boolean = true,
    val screenKeepOn: Boolean = true
) {
    val bodyFatPercent: Float get() = bodyFatPercentage
    /**
     * 三大项总和
     */
    val totalBigThreeKg: Float
        get() = bench1RmKg + squat1RmKg + deadlift1RmKg

    /**
     * DOTS 综合得分
     */
    val dotsScore: Float
        get() = DotsScoreCalculator.calculateDots(
            totalKg = totalBigThreeKg,
            bodyWeightKg = bodyWeightKg,
            isMale = (gender == Gender.MALE)
        )

    /**
     * 力量段位
     */
    val strengthLevel: DotsScoreCalculator.StrengthLevel
        get() = DotsScoreCalculator.getStrengthLevel(dotsScore)
}
