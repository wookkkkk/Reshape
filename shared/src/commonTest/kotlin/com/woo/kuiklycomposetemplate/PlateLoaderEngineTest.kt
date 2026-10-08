package com.woo.kuiklycomposetemplate

import com.woo.kuiklycomposetemplate.domain.algorithm.PlateLoaderEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlateLoaderEngineTest {

    @Test
    fun test140KgBarbellLoading_StandardOlympic() {
        // Target 140kg, 20kg bar -> 120kg plates total -> 60kg per side -> 25 + 25 + 10
        val weights = PlateLoaderEngine.computeSingleSideWeights(140f, 20f)
        assertEquals(listOf(25f, 25f, 10f), weights)
    }

    @Test
    fun test140KgBarbellLoading_CommercialGym() {
        // 商业健身房 (最大20kg片)：60kg 每边 -> 20 + 20 + 20
        val weights = PlateLoaderEngine.computeSingleSideWeights(
            targetTotalKg = 140f,
            barWeightKg = 20f,
            availablePlates = PlateLoaderEngine.COMMERCIAL_PLATES
        )
        assertEquals(listOf(20f, 20f, 20f), weights)
    }

    @Test
    fun test102Point5KgLoading() {
        // Target 102.5kg, 20kg bar -> 82.5kg plates total -> 41.25kg per side -> 25 + 15 + 1.25
        val weights = PlateLoaderEngine.computeSingleSideWeights(102.5f, 20f)
        assertEquals(listOf(25f, 15f, 1.25f), weights)
    }

    @Test
    fun testEmptyBar() {
        val weights = PlateLoaderEngine.computeSingleSideWeights(20f, 20f)
        assertTrue(weights.isEmpty())
    }

    @Test
    fun testExactLoadable() {
        assertTrue(PlateLoaderEngine.isExactLoadable(100f, 20f))
        assertTrue(PlateLoaderEngine.isExactLoadable(22.5f, 20f))
        assertTrue(PlateLoaderEngine.isExactLoadable(70f, 20f, PlateLoaderEngine.COMMERCIAL_PLATES))
    }
}
