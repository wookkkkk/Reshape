package com.woo.kuiklycomposetemplate

import com.woo.kuiklycomposetemplate.data.local.TrainingCycleStore
import com.woo.kuiklycomposetemplate.domain.model.CycleStatus
import com.woo.kuiklycomposetemplate.domain.model.TrainingCycle
import com.woo.kuiklycomposetemplate.domain.model.UserProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrainingCycleStoreTest {

    @Test
    fun testTrainingCycleProgression_advanceCycleSessionIncrementsIndex() {
        val initial = TrainingCycle(
            cycleId = "test_cycle",
            cycleNumber = 1,
            totalSessions = 8,
            currentSessionIndex = 1
        )
        TrainingCycleStore.saveCycle(initial)

        val updated = TrainingCycleStore.advanceCycleSession()
        assertEquals(2, updated.currentSessionIndex)
        assertEquals(1, updated.cycleNumber)
        assertEquals(CycleStatus.IN_PROGRESS, updated.status)
        assertEquals(6, updated.remainingSessions)
        assertFalse(updated.isCompleted)

        // Advance to 8 sessions (completed)
        var cycle = updated
        repeat(7) {
            cycle = TrainingCycleStore.advanceCycleSession()
        }
        assertEquals(8, cycle.currentSessionIndex)
        assertTrue(cycle.isCompleted)
        assertEquals(CycleStatus.COMPLETED_AWAITING_RETEST, cycle.status)
        assertEquals(0, cycle.remainingSessions)
    }

    @Test
    fun testTrainingCycle_startNewCycle() {
        val profile = UserProfile(
            bench1RmKg = 85f,
            squat1RmKg = 120f,
            deadlift1RmKg = 150f
        )
        val initial = TrainingCycle(cycleId = "c1", cycleNumber = 1, currentSessionIndex = 8)
        TrainingCycleStore.saveCycle(initial)

        val newCycle = TrainingCycleStore.startNewCycle(profile)
        assertEquals(2, newCycle.cycleNumber)
        assertEquals(1, newCycle.currentSessionIndex)
        assertEquals(85f, newCycle.initialBench1RmKg)
        assertEquals(120f, newCycle.initialSquat1RmKg)
        assertEquals(150f, newCycle.initialDeadlift1RmKg)
        assertEquals(CycleStatus.IN_PROGRESS, newCycle.status)
    }

    @Test
    fun testTrainingCycle_deloadAndReturnShieldToggle() {
        val cycle = TrainingCycle(cycleId = "c1", cycleNumber = 1, currentSessionIndex = 3)
        TrainingCycleStore.saveCycle(cycle)

        val deloadCycle = TrainingCycleStore.setDeloadActive(true)
        assertTrue(deloadCycle.isDeloadActive)
        assertEquals(CycleStatus.DELOAD_ACTIVE, deloadCycle.status)

        val resetDeload = TrainingCycleStore.setDeloadActive(false)
        assertFalse(resetDeload.isDeloadActive)
        assertEquals(CycleStatus.IN_PROGRESS, resetDeload.status)

        val shieldCycle = TrainingCycleStore.setReturnShieldActive(true)
        assertTrue(shieldCycle.isReturnShieldActive)
    }
}
