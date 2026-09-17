package com.woo.kuiklycomposetemplate

import com.woo.kuiklycomposetemplate.domain.algorithm.DotsScoreCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DotsScoreCalculatorTest {

    @Test
    fun testMaleDotsCalculation() {
        // Male 80kg, 500kg total -> DOTS should be approx 330 ~ 340
        val dots = DotsScoreCalculator.calculateDots(totalKg = 500f, bodyWeightKg = 80f, isMale = true)
        assertTrue(dots > 300f && dots < 360f, "Male DOTS should be in standard powerlifting range, got $dots")
        
        val level = DotsScoreCalculator.getStrengthLevel(dots)
        assertEquals("进阶力量", level.title)
    }

    @Test
    fun testFemaleDotsCalculation() {
        // Female 60kg, 350kg total -> DOTS should be approx 370 ~ 400
        val dots = DotsScoreCalculator.calculateDots(totalKg = 350f, bodyWeightKg = 60f, isMale = false)
        assertTrue(dots > 350f && dots < 420f, "Female DOTS should be in standard powerlifting range, got $dots")
        
        val level = DotsScoreCalculator.getStrengthLevel(dots)
        assertEquals("进阶力量", level.title)
    }

    @Test
    fun testDotsEdgeCases() {
        assertEquals(0f, DotsScoreCalculator.calculateDots(0f, 75f, true))
        assertEquals(0f, DotsScoreCalculator.calculateDots(400f, 10f, true))
    }
}
