package com.woo.kuiklycomposetemplate

import com.woo.kuiklycomposetemplate.domain.algorithm.VolumeCalculator
import com.woo.kuiklycomposetemplate.domain.model.SessionExercise
import com.woo.kuiklycomposetemplate.domain.model.SessionSet
import kotlin.test.Test
import kotlin.test.assertEquals

class VolumeCalculatorTest {

    @Test
    fun testVolumeCalculation_ExcludesWarmup() {
        val exercise = SessionExercise(
            exerciseId = "bench_press",
            name = "杠铃平板卧推",
            category = "chest",
            isUnilateral = false,
            sets = listOf(
                SessionSet(setNo = 1, isWarmup = true, weightKg = 40f, effectiveReps = 10),  // Warmup (0 vol)
                SessionSet(setNo = 2, isWarmup = false, weightKg = 80f, effectiveReps = 6), // 480 kg
                SessionSet(setNo = 3, isWarmup = false, weightKg = 80f, effectiveReps = 6), // 480 kg
                SessionSet(setNo = 4, isWarmup = false, weightKg = 80f, effectiveReps = 5)  // 400 kg
            )
        )

        val volume = VolumeCalculator.calculateExerciseVolume(exercise, 75f)
        assertEquals(1360.0f, volume)

        val workingSets = VolumeCalculator.countWorkingSets(listOf(exercise))
        assertEquals(3, workingSets)

        val warmupSets = VolumeCalculator.countWarmupSets(listOf(exercise))
        assertEquals(1, warmupSets)
    }

    @Test
    fun testUnilateralVolume_DoubledForBothSides() {
        val exercise = SessionExercise(
            exerciseId = "db_curl",
            name = "单臂哑铃弯举",
            category = "arms",
            isUnilateral = true,
            sets = listOf(
                SessionSet(setNo = 1, isWarmup = false, weightKg = 15f, effectiveReps = 10) // 15 * 10 * 2 = 300kg
            )
        )

        val volume = VolumeCalculator.calculateExerciseVolume(exercise, 75f)
        assertEquals(300.0f, volume)
    }

    @Test
    fun testBodyweightVolume_CalculatesProperWorkTonnage() {
        // 自重双杠臂屈伸：75kg 体重，自重做 10 次 -> 75 * 10 = 750kg
        val dips = SessionExercise(
            exerciseId = "push_3_dips",
            name = "双杠臂屈伸",
            category = "chest",
            isUnilateral = false,
            sets = listOf(
                SessionSet(setNo = 1, isWarmup = false, weightKg = 0f, effectiveReps = 10), // (75 + 0) * 10 = 750kg
                SessionSet(setNo = 2, isWarmup = false, weightKg = 10f, effectiveReps = 8)  // (75 + 10) * 8 = 680kg
            )
        )

        val dipsVol = VolumeCalculator.calculateExerciseVolume(dips, userBodyWeightKg = 75f)
        assertEquals(1430.0f, dipsVol)

        // 自重山羊挺身：75kg 体重，后链挺身折算 0.5 系数 -> (75 * 0.5) * 12 = 450kg
        val backExt = SessionExercise(
            exerciseId = "legs_5_back_extension",
            name = "山羊挺身 (后链强化)",
            category = "posterior_chain",
            isUnilateral = false,
            sets = listOf(
                SessionSet(setNo = 1, isWarmup = false, weightKg = 0f, effectiveReps = 12) // 37.5 * 12 = 450kg
            )
        )

        val backExtVol = VolumeCalculator.calculateExerciseVolume(backExt, userBodyWeightKg = 75f)
        assertEquals(450.0f, backExtVol)
    }
}
