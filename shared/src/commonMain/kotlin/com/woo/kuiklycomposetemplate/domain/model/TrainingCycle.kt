package com.woo.kuiklycomposetemplate.domain.model

enum class CycleStatus {
    IN_PROGRESS,               // 进行中 (1..7 次)
    COMPLETED_AWAITING_RETEST, // 8/8 次达成，待重测/结项
    DELOAD_ACTIVE              // 减载蓄力中
}

/**
 * 训练中周期实体 (Mesocycle)
 * 管理 8 次训练推进状态、重测 1RM 倒计时、减载周与复训保护
 */
data class TrainingCycle(
    val cycleId: String = "cycle_1",
    val cycleNumber: Int = 1,
    val totalSessions: Int = 8,
    val currentSessionIndex: Int = 3, // 当前处于第几次 (1..8)
    val isDeloadActive: Boolean = false,
    val isReturnShieldActive: Boolean = false,
    val startTimestamp: Long = 0L,
    val initialBench1RmKg: Float = 75f,
    val initialSquat1RmKg: Float = 100f,
    val initialDeadlift1RmKg: Float = 130f
) {
    val remainingSessions: Int
        get() = maxOf(0, totalSessions - currentSessionIndex)

    val isCompleted: Boolean
        get() = currentSessionIndex >= totalSessions

    val status: CycleStatus
        get() = when {
            isDeloadActive -> CycleStatus.DELOAD_ACTIVE
            isCompleted -> CycleStatus.COMPLETED_AWAITING_RETEST
            else -> CycleStatus.IN_PROGRESS
        }
}
