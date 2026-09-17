package com.woo.kuiklycomposetemplate.domain.model

/**
 * 单次递进记录点
 */
data class ProgressionPoint(
    val sessionIndex: Int,       // 第几次训练 (1~8)
    val date: String,            // "08-15"
    val targetWeightKg: Float,   // 目标重量
    val actualWeightKg: Float,   // 实际重量
    val reps: Int,               // 完成次数
    val targetReps: Int,         // 目标次数
    val isAchieved: Boolean,     // 是否达标
    val isDeload: Boolean = false// 是否减载组
)

/**
 * 停滞诊断状态
 */
enum class StagnationStatus(val label: String, val solution: String) {
    NORMAL("正常递进", "保持节奏，继续向目标次数冲击"),
    WARNING("1次未达标", "注意睡眠与营养，下次再尝试同等负荷"),
    STALLED("连续2次停滞", "触发周期停滞诊断，建议启动 4 大出口策略"),
    DELOAD_RECOMMENDED("建议减载周", "已连续高负荷 6~8 周，需降重 40% 促进神经系统超量恢复")
}

/**
 * 8 次周期递进状态模型
 */
data class ExerciseProgression(
    val exerciseId: String,
    val exerciseName: String,
    val currentCycle: Int = 1,
    val points: List<ProgressionPoint> = emptyList(),
    val status: StagnationStatus = StagnationStatus.NORMAL
)
