package com.woo.kuiklycomposetemplate.domain.algorithm

import com.woo.kuiklycomposetemplate.core.utils.platformCurrentTimeMillis
import com.woo.kuiklycomposetemplate.domain.model.DayType
import com.woo.kuiklycomposetemplate.domain.model.WorkoutSession
import kotlin.math.min
import kotlin.math.round

/**
 * 单个肌群的实时超量恢复评估数据
 */
data class MuscleRecoveryState(
    val muscleGroup: String,           // 肌群名称 (例如 "胸大肌 / 三角肌前中束 / 肱三头肌")
    val dayType: DayType,              // 对应训练日类型 (PUSH, PULL, LEGS)
    val recoveryRatio: Float,          // 恢复进度 (0.0 ~ 1.0)
    val statusDescription: String,     // 状态文案 (例如 "🔥 处于超量恢复黄金期")
    val elapsedHours: Float,           // 距上次训练流逝小时数
    val isSupercompensated: Boolean    // 是否进入超量恢复峰值期
)

/**
 * 肌肉超量恢复与疲劳代谢动力学生物算法引擎
 *
 * 理论基础：运动生理学肌肉蛋白合成 (MPS) 窗口与超量恢复双因子模型 (Fitness-Fatigue Model)
 * - 0 ~ 12h: 急性疲劳期 (Recovery 20% ~ 40%) - 肌纤维微损伤与充血
 * - 12 ~ 36h: 组织修复期 (Recovery 40% ~ 75%) - MPS 峰值，肌原纤维重建
 * - 36 ~ 60h: 基线恢复期 (Recovery 75% ~ 95%) - 神经中枢与肌力恢复
 * - 60 ~ 96h: 超量恢复黄金窗口 (Recovery 100%) - 糖原储备与力量超越基准线 (最佳刺激点)
 * - > 96h: 刺激衰退期 (Recovery 95%) - 力量逐渐回归基准线
 */
object MuscleRecoveryEngine {

    /**
     * 根据训练历史记录与当前时间戳，动态推算三大肌群的恢复与超量恢复百分比
     *
     * @param history 全部历史训练会话
     * @param currentTimestampMillis 当前毫秒时间戳 (默认取系统当前时间)
     * @return 三大肌群的恢复评估列表
     */
    fun evaluateAllMuscleGroups(
        history: List<WorkoutSession>,
        currentTimestampMillis: Long = 0L
    ): List<MuscleRecoveryState> {
        val now = if (currentTimestampMillis > 0L) currentTimestampMillis else platformCurrentTimeMillis()

        return listOf(
            calculateMuscleState(
                muscleName = "胸大肌 / 三角肌前中束 / 肱三头",
                targetDay = DayType.PUSH,
                standardRecoveryHours = 60f, // 推类中型肌群约 60h 达到超量恢复
                history = history,
                nowMillis = now
            ),
            calculateMuscleState(
                muscleName = "背阔肌 / 后链 / 肱二头",
                targetDay = DayType.PULL,
                standardRecoveryHours = 60f, // 拉类中型肌群约 60h 达到超量恢复
                history = history,
                nowMillis = now
            ),
            calculateMuscleState(
                muscleName = "股四头肌 / 臀大肌 / 腘绳肌",
                targetDay = DayType.LEGS,
                standardRecoveryHours = 72f, // 腿部大肌群与神经消耗需 72h 达到超量恢复
                history = history,
                nowMillis = now
            )
        )
    }

    private fun calculateMuscleState(
        muscleName: String,
        targetDay: DayType,
        standardRecoveryHours: Float,
        history: List<WorkoutSession>,
        nowMillis: Long
    ): MuscleRecoveryState {
        // 寻找该肌群最近一次训练记录
        val lastSession = history.firstOrNull { it.planContext.dayCode == targetDay.code }

        if (lastSession == null) {
            // 从未受训过或全新开始，视为 100% 满能量充沛状态
            return MuscleRecoveryState(
                muscleGroup = muscleName,
                dayType = targetDay,
                recoveryRatio = 1.0f,
                statusDescription = "充沛就绪 (100%)",
                elapsedHours = 120f,
                isSupercompensated = true
            )
        }

        val sessionTime = if (lastSession.finishedAt != null && lastSession.finishedAt > 0L) {
            lastSession.finishedAt
        } else if (lastSession.startedAt > 0L) {
            lastSession.startedAt
        } else {
            nowMillis - (48L * 3600L * 1000L) // 兜底 48 小时前
        }

        val diffMillis = (nowMillis - sessionTime).coerceAtLeast(0L)
        val elapsedHours = diffMillis.toFloat() / (1000f * 3600f)

        // 相对负荷加权修正：采用相对体重容量倍数 (V_rel = Tonnage / BodyWeight)
        val bodyWeight = if (lastSession.userProfile.bodyWeightKg > 20f) lastSession.userProfile.bodyWeightKg else 75.0f
        val relativeVolume = lastSession.sessionMetrics.totalTonnageKg / bodyWeight

        val loadFactor = when {
            lastSession.planContext.isDeloadWeek -> 0.75f // 减载周，恢复提速 25%
            relativeVolume > 100f -> 1.25f // 超大容量冲击日 (>100倍体重)，恢复时间 +25%
            relativeVolume > 65f -> 1.05f  // 标准高强度做组 (65~100倍体重)，基准或微调 +5%
            relativeVolume < 40f -> 0.90f  // 低容量恢复性做组 (<40倍体重)，恢复提速 10%
            else -> 1.0f
        }

        val adjustedFullHours = standardRecoveryHours * loadFactor
        val rawProgress = elapsedHours / adjustedFullHours

        val (ratio, status, isSuper) = when {
            elapsedHours < 12f -> {
                val r = 0.20f + (elapsedHours / 12f) * 0.20f
                Triple(r, "急性疲劳充血期 (${(r * 100).toInt()}%)", false)
            }
            elapsedHours < 36f -> {
                val r = 0.40f + ((elapsedHours - 12f) / 24f) * 0.35f
                Triple(r, "蛋白合成修复中 (${(r * 100).toInt()}%)", false)
            }
            elapsedHours < adjustedFullHours -> {
                val span = maxOf(1.0f, adjustedFullHours - 36f)
                val r = 0.75f + ((elapsedHours - 36f).coerceAtLeast(0f) / span) * 0.25f
                Triple(r.coerceIn(0.75f, 1.0f), "良好恢复接近基线 (${(r.coerceIn(0.75f, 1.0f) * 100).toInt()}%)", false)
            }
            elapsedHours <= adjustedFullHours * 1.5f -> {
                // 超量恢复黄金窗口期
                Triple(1.0f, "🔥 超量恢复黄金期 (最佳做组时机)", true)
            }
            elapsedHours < 168f -> {
                // 超过 1.5 倍周期但 < 7 天，处于充沛但超量收益逐渐回归
                Triple(0.95f, "恢复充分，建议及时刺激 (95%)", false)
            }
            else -> {
                // 停训 >= 7 天 (168h)，神经与肌力去适应，启动复训保护
                Triple(0.80f, "🛡️ 复训保护期 (建议 80% 唤醒负荷)", false)
            }
        }

        val finalRatio = min(1.0f, round(ratio * 100f) / 100f)

        return MuscleRecoveryState(
            muscleGroup = muscleName,
            dayType = targetDay,
            recoveryRatio = finalRatio,
            statusDescription = status,
            elapsedHours = elapsedHours,
            isSupercompensated = isSuper
        )
    }
}
