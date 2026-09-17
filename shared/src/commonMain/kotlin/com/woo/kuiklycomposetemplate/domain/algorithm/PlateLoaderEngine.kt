package com.woo.kuiklycomposetemplate.domain.algorithm

import com.tencent.kuikly.compose.ui.graphics.Color

/**
 * 彩色奥赛标准杠铃片规格
 */
data class PlateSpec(
    val weightKg: Float,
    val color: Color,
    val colorHex: String,
    val label: String,
    val diameterRatio: Float // 相对视觉尺寸比例 (0.5 ~ 1.0)
)

/**
 * 常见杠铃杆与器械基重预设
 */
enum class BarbellBarType(val label: String, val weightKg: Float) {
    MALE_OLYMPIC("20kg (标准奥杆)", 20.0f),
    FEMALE_SHORT("15kg (女子/短杆)", 15.0f),
    SMITH_OR_BODY("0kg (史密斯/自重)", 0.0f)
}

/**
 * 奥赛杠铃片装载计算引擎（最少片数贪心算法）
 */
object PlateLoaderEngine {

    val STANDARD_PLATES = listOf(
        PlateSpec(25.0f, Color(0xFFEF4444), "#EF4444", "25", 1.0f),
        PlateSpec(20.0f, Color(0xFF2563EB), "#2563EB", "20", 0.95f),
        PlateSpec(15.0f, Color(0xFFEAB308), "#EAB308", "15", 0.85f),
        PlateSpec(10.0f, Color(0xFF10B981), "#10B981", "10", 0.75f),
        PlateSpec(5.0f, Color(0xFFF3F4F6), "#F3F4F6", "5", 0.65f),
        PlateSpec(2.5f, Color(0xFF1F2937), "#1F2937", "2.5", 0.55f),
        PlateSpec(1.25f, Color(0xFF9CA3AF), "#9CA3AF", "1.25", 0.48f),
        PlateSpec(0.5f, Color(0xFFD1D5DB), "#D1D5DB", "0.5", 0.42f)
    )

    // 国内商业健身房最普及的标准片集合 (20kg, 10kg, 5kg, 2.5kg)
    val COMMERCIAL_PLATES = listOf(
        PlateSpec(20.0f, Color(0xFF2563EB), "#2563EB", "20", 0.95f),
        PlateSpec(10.0f, Color(0xFF10B981), "#10B981", "10", 0.75f),
        PlateSpec(5.0f, Color(0xFFF3F4F6), "#F3F4F6", "5", 0.65f),
        PlateSpec(2.5f, Color(0xFF1F2937), "#1F2937", "2.5", 0.55f)
    )

    private val PLATE_WEIGHTS = STANDARD_PLATES.map { it.weightKg }

    /**
     * 计算单侧装片序列（贪心扣减最少片数）
     *
     * @param targetTotalKg 杠铃总目标重量 (含杆重)
     * @param barWeightKg 杠铃空杆重量 (标准奥林匹克大杆 20kg, 短杆/特种杆 15kg, 史密斯/固定器械 0kg)
     * @param availablePlates 可选片集合（默认使用 STANDARD_PLATES）
     * @param disabledWeights 用户关闭禁用的片重集合 (如禁用 15kg 或 1.25kg)
     * @return 单侧所需片数列表（由内向外排序，大片在内小片在外）
     */
    fun computeSingleSidePlates(
        targetTotalKg: Float,
        barWeightKg: Float = 20.0f,
        availablePlates: List<PlateSpec> = STANDARD_PLATES,
        disabledWeights: Set<Float> = emptySet()
    ): List<PlateSpec> {
        var remain = (targetTotalKg - barWeightKg) / 2.0f
        if (remain <= 0f) return emptyList()

        val activePlates = availablePlates.filter { it.weightKg !in disabledWeights }
        val result = mutableListOf<PlateSpec>()
        for (spec in activePlates) {
            while (remain >= spec.weightKg - 0.01f) {
                result.add(spec)
                remain -= spec.weightKg
            }
        }
        return result
    }

    /**
     * 获取单侧装片重量数字列表
     */
    fun computeSingleSideWeights(
        targetTotalKg: Float,
        barWeightKg: Float = 20.0f,
        availablePlates: List<PlateSpec> = STANDARD_PLATES,
        disabledWeights: Set<Float> = emptySet()
    ): List<Float> {
        return computeSingleSidePlates(targetTotalKg, barWeightKg, availablePlates, disabledWeights).map { it.weightKg }
    }

    /**
     * 校验目标重量是否可以通过标准片与杆重精确拼出（误差 < 0.1kg）
     */
    fun isExactLoadable(
        targetTotalKg: Float,
        barWeightKg: Float = 20.0f,
        availablePlates: List<PlateSpec> = STANDARD_PLATES,
        disabledWeights: Set<Float> = emptySet()
    ): Boolean {
        if (targetTotalKg < barWeightKg) return false
        val remain = (targetTotalKg - barWeightKg) / 2.0f
        var current = remain
        val activePlates = availablePlates.filter { it.weightKg !in disabledWeights }
        for (spec in activePlates) {
            while (current >= spec.weightKg - 0.01f) {
                current -= spec.weightKg
            }
        }
        return current < 0.01f
    }
}
