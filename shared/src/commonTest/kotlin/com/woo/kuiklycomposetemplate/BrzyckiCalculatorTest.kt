package com.woo.kuiklycomposetemplate

import com.woo.kuiklycomposetemplate.domain.algorithm.BrzyckiCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BrzyckiCalculatorTest {

    @Test
    fun testCalculate1RM_SingleRep() {
        val result = BrzyckiCalculator.calculate1RM(100f, 1)
        assertEquals(100f, result)
    }

    @Test
    fun testCalculate1RM_FiveReps() {
        // 100 / (1.0278 - 0.0278 * 5) = 100 / 0.8888 = 112.511 -> rounds to 112.5kg
        val result = BrzyckiCalculator.calculate1RM(100f, 5)
        assertEquals(112.5f, result)
    }

    @Test
    fun testCalculateWeightForReps() {
        // 112.5kg 1RM -> for 5 reps: 112.5 * (1.0278 - 0.0278 * 5) = 112.5 * 0.8888 = 99.99 -> rounds to 100.0kg
        val result = BrzyckiCalculator.calculateWeightForReps(112.5f, 5)
        assertEquals(100.0f, result)
    }

    @Test
    fun testRpeAdjusted1RM() {
        // 100kg for 5 reps @ RPE 8.0 (2 RIR) -> effective 7 reps -> 100 / (1.0278 - 0.0278 * 7) = 100 / 0.8332 = 120.01kg -> 120.0kg
        val result = BrzyckiCalculator.calculateRpeAdjusted1RM(100f, 5, 8.0f)
        assertEquals(120.0f, result)
    }

    @Test
    fun testEdgeCases() {
        assertEquals(0f, BrzyckiCalculator.calculate1RM(0f, 5))
        assertEquals(0f, BrzyckiCalculator.calculateWeightForReps(0f, 5))
        assertTrue(BrzyckiCalculator.calculate1RM(50f, 20) > 50f)
    }
}
