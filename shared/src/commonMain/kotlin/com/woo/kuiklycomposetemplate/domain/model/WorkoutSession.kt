package com.woo.kuiklycomposetemplate.domain.model

/**
 * 完整训练会话数据实体 (严格对齐 PRD 5.2 JSON 数据契约)
 */
data class WorkoutSession(
    val sessionId: String,
    val userId: String = "local_user",
    val userProfile: SessionUserProfile,
    val planContext: SessionPlanContext,
    val sessionMetrics: SessionMetrics,
    val exercises: List<SessionExercise>,
    val startedAt: Long = 0L,
    val finishedAt: Long? = null,
    val isDraft: Boolean = false
)

data class SessionUserProfile(
    val bodyWeightKg: Float,
    val gender: String,              // "male" | "female"
    val trainingExpYears: Float = 1.0f,
    val bench1RmKg: Float = 60.0f,
    val squat1RmKg: Float = 80.0f,
    val deadlift1RmKg: Float = 100.0f
)

data class SessionPlanContext(
    val planId: String = "classic_ppl",
    val dayCode: String,             // "D1", "D2", "D3"
    val dayTitle: String,            // "D1 · 推"
    val readinessState: String = "full", // "full" (充沛) | "fatigued" (疲劳)
    val isDeloadWeek: Boolean = false,
    val isRehabProtected: Boolean = false
)

data class SessionMetrics(
    val durationMinutes: Int = 0,
    val totalTonnageKg: Float = 0f,
    val workingSetsCount: Int = 0,
    val warmupSetsCount: Int = 0,
    val prBreakCount: Int = 0,
    val avgRestSeconds: Int = 90
)

data class SessionExercise(
    val exerciseId: String,
    val name: String,
    val category: String,
    val isUnilateral: Boolean = false,
    val memo: String = "",
    val sets: List<SessionSet> = emptyList()
)

data class SessionSet(
    val setNo: Int,               // 全局做组顺序号 (1-based)
    val isWarmup: Boolean = false,// 是否热身组 (true 则不计入总容量)
    val weightKg: Float,          // 做组负重 (kg)
    val leftReps: Int? = null,    // 单侧左侧次数
    val rightReps: Int? = null,   // 单侧右侧次数
    val effectiveReps: Int,       // 有效次数 (单侧以弱侧为准)
    val rpe: Float = 8.0f,        // 主观力竭度 (6.0 - 10.0)
    val isPr: Boolean = false,    // 是否突破历史 PR
    val restSeconds: Int = 120,   // 实际做完该组后的休息秒数
    val completedAt: Long = 0L    // 打卡时间戳
)
