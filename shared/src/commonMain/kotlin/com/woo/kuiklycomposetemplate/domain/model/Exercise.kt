package com.woo.kuiklycomposetemplate.domain.model

/**
 * 训练日类型 (经典三分化)
 */
enum class DayType(val code: String, val title: String, val subtitle: String, val icon: String) {
    PUSH("D1", "推", "胸大肌 / 三角肌前中束 / 肱三头肌", "⚡"),
    PULL("D2", "拉", "背阔肌 / 斜方肌 / 三角肌后束 / 肱二头肌", "⚓"),
    LEGS("D3", "腿", "股四头肌 / 腘绳肌 / 臀大肌 / 核心后链", "🛡️"),
    REST("REST", "休息", "筋膜放松 / 超量恢复 / 碳水补充", "🛌")
}

/**
 * 负重类型
 */
enum class LoadType(val label: String) {
    BARBELL("杠铃"),
    DUMBBELL("哑铃"),
    CABLE("绳索/龙门架"),
    MACHINE("固定器械"),
    BODYWEIGHT("自重/附加重")
}

/**
 * 核心受力/受压主关节类型 (用于关节保护与无痛变式映射)
 */
enum class JointType(val label: String, val icon: String) {
    SHOULDER("肩关节/肩袖", "🦾"),
    LOWER_BACK("腰椎/下背", "🛡️"),
    KNEE("膝关节/髌腱", "🦵"),
    ELBOW("肘关节", "💪"),
    HIP("髋关节", "⚡"),
    WRIST("腕关节", "✋"),
    NONE("无明显关节剪切", "✨")
}

/**
 * 递进策略模型
 */
enum class ProgressionType(val title: String, val desc: String) {
    WAVE_STRENGTH("波浪递进 (3~5次)", "高强度低次数，以神经募集与突破极限为主"),
    DOUBLE_PROGRESSION("双重递进 (6~8次)", "先提次数至上限，再加重量回到下限"),
    METABOLIC_PUMP("代谢递进 (10~15次)", "恒定中等重量，专注肌肉充血与顶峰收缩"),
    UNILATERAL_BALANCE("单侧平衡 (8~10次)", "以弱侧次数为准，彻底解决左右肌力失衡"),
    WEIGHTED_BODY("自重加重递进", "自重达标后挂片加重");

    val label: String get() = title
}

/**
 * 标准骨骼肌解剖学字典
 */
object MuscleTaxonomy {
    val PUSH_PRIMARY_MUSCLES = listOf("胸大肌中下束", "胸大肌上束 (锁骨头)", "三角肌前束", "三角肌中束", "肱三头肌")
    val PULL_PRIMARY_MUSCLES = listOf("背阔肌 (宽度)", "中上背/菱形肌/斜方肌 (厚度)", "三角肌后束", "肩袖外旋肌群", "肱二头肌")
    val LEGS_PRIMARY_MUSCLES = listOf("股四头肌 (前侧)", "腘绳肌 (后侧)", "臀大肌/臀中肌", "竖脊肌/后链", "小腿腓肠肌")

    val ALL_PRIMARY_MUSCLES = listOf(
        "胸大肌中下束", "胸大肌上束", "三角肌前束", "三角肌中束", "三角肌后束",
        "背阔肌", "中上背/菱形肌", "竖脊肌/后链", "肱二头肌", "肱三头肌",
        "股四头肌", "腘绳肌", "臀大肌", "小腿肌群", "腹直肌/核心"
    )

    val ALL_SECONDARY_MUSCLES = listOf(
        "肱三头肌", "肱二头肌", "前臂肌群",
        "三角肌前束", "三角肌后束", "肩袖外旋",
        "臀大肌", "核心竖脊肌", "腹直肌",
        "斜方肌", "小腿肌群"
    )
}

/**
 * 动作元数据模型 (支持主辅肌群与可替换关系图谱)
 */
data class Exercise(
    val id: String,
    val name: String,
    val dayType: DayType,
    val primaryMuscle: String = "胸大肌",              // 🎯 主发力肌群 (单选)
    val secondaryMuscles: List<String> = emptyList(), // 🧬 协同/辅助肌群 (多选)
    val targetMuscle: String = "",                    // 兼容旧字段
    val loadType: LoadType,
    val primaryJoint: JointType = JointType.NONE,
    val isCustom: Boolean = false,
    val isUnilateral: Boolean = false,
    val defaultHoleCues: String = "",        // 默认孔位口诀
    val defaultSets: Int = 4,                // 计划正式组数 (Working Sets)
    val targetRepsMin: Int = 8,              // 目标次数基准
    val targetRepsMax: Int = 8,              // 目标次数基准
    val targetRepsSequence: String = "",     // 波浪/金字塔序列 (如 "15/12/10/8" 或 "5/5/5/5/5")
    val targetWeightKg: Float = 40.0f,       // 目标做组重量 (kg，恒定做组使用或波浪第一组基准)
    val waveWeightsSequence: String = "",    // 逐组波浪重量序列 (如 "45/50/52.5/57.5")
    val targetRpe: Float = 8.0f,             // 建议 RPE (默认 8.0，严控力竭)
    val defaultRestSeconds: Int = 120,       // 默认组间休息 (秒)
    val progressionType: ProgressionType = ProgressionType.DOUBLE_PROGRESSION,
    val related1RmKey: String? = null,       // 关联三大项 1RM 字段 (bench / squat / deadlift)
    val ratioOf1Rm: Float? = null,           // 换算系数 (如上斜哑铃 = 0.35 * 平板卧推 1RM)
    val painFreeVariantIds: List<String> = emptyList(), // 关联的无痛变式动作 ID 列表
    val interchangeableExerciseIds: List<String> = emptyList(), // 关联的可替换动作 ID 列表
    val enableWarmupLadder: Boolean = true   // 是否开启科学热身阶梯隔离
) {
    val displayMuscleSummary: String
        get() = if (targetMuscle.isNotBlank()) targetMuscle else if (secondaryMuscles.isEmpty()) primaryMuscle else "$primaryMuscle / ${secondaryMuscles.joinToString(" / ")}"

    /**
     * 解析逐组目标次数列表 (支持恒定做组与 15/12/10/8 波浪序列)
     */
    fun getPerSetRepsList(): List<Int> {
        if (targetRepsSequence.isNotBlank()) {
            val list = targetRepsSequence.split("/", "-", "–", ",").mapNotNull { it.trim().toIntOrNull() }
            if (list.isNotEmpty()) return list
        }
        val rep = if (targetRepsMin > 0) targetRepsMin else 8
        return List(defaultSets.coerceAtLeast(1)) { rep }
    }

    /**
     * 解析逐组目标重量列表 (支持恒定做组与波浪重量序列)
     */
    fun getPerSetWeightsList(): List<Float> {
        if (waveWeightsSequence.isNotBlank()) {
            val list = waveWeightsSequence.split("/", "-", "–", ",").mapNotNull { it.trim().toFloatOrNull() }
            if (list.isNotEmpty()) return list
        }
        return List(defaultSets.coerceAtLeast(1)) { targetWeightKg }
    }

    /**
     * 界面展示的次数标签文案 (如 "15/12/10/8" 或 "8 次")
     */
    val displayRepsText: String
        get() {
            if (targetRepsSequence.isNotBlank()) {
                return targetRepsSequence
            }
            return if (targetRepsMin == targetRepsMax) "${targetRepsMin} 次" else "${targetRepsMin}~${targetRepsMax} 次"
        }

    /**
     * 界面展示的重量标签文案 (如 "45~57.5 kg" 或 "47.5 kg")
     */
    val displayWeightText: String
        get() {
            if (waveWeightsSequence.isNotBlank()) {
                val weights = getPerSetWeightsList()
                if (weights.isNotEmpty() && weights.first() != weights.last()) {
                    return "${weights.first()}~${weights.last()} kg"
                }
            }
            return "${targetWeightKg} kg"
        }
}

/**
 * 官方预置黄金动作库 (系统基准动作)
 */
object PresetExercises {

    // ==========================================
    // ---------- D1 推 (Push) 动作库 -----------
    // ==========================================
    val BARBELL_BENCH_PRESS = Exercise(
        id = "push_1_bench_press",
        name = "杠铃平板卧推",
        dayType = DayType.PUSH,
        primaryMuscle = "胸大肌中下束",
        secondaryMuscles = listOf("三角肌前束", "肱三头肌"),
        targetMuscle = "胸大肌 / 肱三头肌",
        loadType = LoadType.BARBELL,
        primaryJoint = JointType.SHOULDER,
        defaultHoleCues = "平凳距架 3 孔 · 肩胛骨后缩下沉 · 落点乳头连线",
        defaultSets = 4,
        targetRepsMin = 3,
        targetRepsMax = 5,
        targetRpe = 8.5f,
        defaultRestSeconds = 180,
        progressionType = ProgressionType.WAVE_STRENGTH,
        related1RmKey = "bench",
        ratioOf1Rm = 1.0f,
        painFreeVariantIds = listOf("push_var_floor_press", "push_3_dips", "push_var_chest_press"),
        interchangeableExerciseIds = listOf("push_2_incline_db_press", "push_var_floor_press", "push_var_chest_press")
    )

    val INCLINE_DUMBBELL_PRESS = Exercise(
        id = "push_2_incline_db_press",
        name = "上斜哑铃卧推",
        dayType = DayType.PUSH,
        primaryMuscle = "胸大肌上束",
        secondaryMuscles = listOf("三角肌前束", "肱三头肌"),
        targetMuscle = "胸大肌上束",
        loadType = LoadType.DUMBBELL,
        primaryJoint = JointType.SHOULDER,
        defaultHoleCues = "斜板 30 度(第2孔) · 手肘内收 45 度 · 底部充分拉伸",
        defaultSets = 4,
        targetRepsMin = 6,
        targetRepsMax = 8,
        defaultRestSeconds = 120,
        progressionType = ProgressionType.DOUBLE_PROGRESSION,
        related1RmKey = "bench",
        ratioOf1Rm = 0.36f,
        painFreeVariantIds = listOf("push_var_floor_press"),
        interchangeableExerciseIds = listOf("push_1_bench_press", "push_var_chest_press")
    )

    val DIPS = Exercise(
        id = "push_3_dips",
        name = "双杠臂屈伸",
        dayType = DayType.PUSH,
        primaryMuscle = "胸大肌中下束",
        secondaryMuscles = listOf("肱三头肌", "三角肌前束"),
        targetMuscle = "胸大肌下缘 / 肱三头肌",
        loadType = LoadType.BODYWEIGHT,
        primaryJoint = JointType.SHOULDER,
        defaultHoleCues = "躯干前倾 15 度 · 下沉至大臂平行 · 顶峰不超伸锁死",
        defaultSets = 3,
        targetRepsMin = 8,
        targetRepsMax = 10,
        defaultRestSeconds = 90,
        progressionType = ProgressionType.WEIGHTED_BODY,
        painFreeVariantIds = listOf("push_var_chest_press", "push_var_floor_press"),
        interchangeableExerciseIds = listOf("push_var_chest_press", "push_var_floor_press")
    )

    val DUMBBELL_LATERAL_RAISE = Exercise(
        id = "push_4_lateral_raise",
        name = "哑铃侧平举",
        dayType = DayType.PUSH,
        primaryMuscle = "三角肌中束",
        secondaryMuscles = listOf("斜方肌上束"),
        targetMuscle = "三角肌中束",
        loadType = LoadType.DUMBBELL,
        primaryJoint = JointType.SHOULDER,
        isUnilateral = false,
        defaultHoleCues = "手肘微屈引领先起 · 沉肩避免斜方肌代偿 · 顶点倒水意念",
        defaultSets = 4,
        targetRepsMin = 12,
        targetRepsMax = 15,
        defaultRestSeconds = 60,
        progressionType = ProgressionType.METABOLIC_PUMP,
        painFreeVariantIds = listOf("push_var_chest_supported_lateral_raise", "push_var_cable_lateral_raise"),
        interchangeableExerciseIds = listOf("push_var_chest_supported_lateral_raise", "push_var_cable_lateral_raise")
    )

    val CABLE_PUSHDOWN = Exercise(
        id = "push_5_cable_pushdown",
        name = "绳索下压",
        dayType = DayType.PUSH,
        primaryMuscle = "肱三头肌",
        secondaryMuscles = listOf("前臂腕屈肌"),
        targetMuscle = "肱三头肌外侧头",
        loadType = LoadType.CABLE,
        primaryJoint = JointType.ELBOW,
        defaultHoleCues = "滑轮置于最高孔 · 大臂贴紧肋骨 · 底部外旋挤压",
        defaultSets = 3,
        targetRepsMin = 10,
        targetRepsMax = 12,
        defaultRestSeconds = 60,
        progressionType = ProgressionType.METABOLIC_PUMP
    )

    val DB_FLOOR_PRESS = Exercise(
        id = "push_var_floor_press",
        name = "哑铃对握地板卧推",
        dayType = DayType.PUSH,
        primaryMuscle = "胸大肌中下束",
        secondaryMuscles = listOf("肱三头肌"),
        targetMuscle = "胸大肌 / 肱三头肌 (肩袖友好)",
        loadType = LoadType.DUMBBELL,
        primaryJoint = JointType.SHOULDER,
        defaultHoleCues = "平躺地面 · 大臂触地即推起 · 彻底杜绝肩关节超伸",
        defaultSets = 4,
        targetRepsMin = 8,
        targetRepsMax = 10,
        defaultRestSeconds = 90,
        progressionType = ProgressionType.DOUBLE_PROGRESSION,
        interchangeableExerciseIds = listOf("push_1_bench_press", "push_2_incline_db_press")
    )

    val CHEST_SUPPORTED_LATERAL_RAISE = Exercise(
        id = "push_var_chest_supported_lateral_raise",
        name = "上斜胸托哑铃侧平举",
        dayType = DayType.PUSH,
        primaryMuscle = "三角肌中束",
        secondaryMuscles = emptyList(),
        targetMuscle = "三角肌中束 (零肩峰撞击)",
        loadType = LoadType.DUMBBELL,
        primaryJoint = JointType.SHOULDER,
        isUnilateral = false,
        defaultHoleCues = "胸贴上斜板 30 度 · 肘部微曲侧上方推起 · 彻底卸除肩峰挤压",
        defaultSets = 4,
        targetRepsMin = 12,
        targetRepsMax = 15,
        defaultRestSeconds = 60,
        progressionType = ProgressionType.METABOLIC_PUMP,
        interchangeableExerciseIds = listOf("push_4_lateral_raise")
    )

    val CABLE_LATERAL_RAISE = Exercise(
        id = "push_var_cable_lateral_raise",
        name = "低位绳索侧平举",
        dayType = DayType.PUSH,
        primaryMuscle = "三角肌中束",
        secondaryMuscles = emptyList(),
        targetMuscle = "三角肌中束 (全程恒定张力)",
        loadType = LoadType.CABLE,
        primaryJoint = JointType.SHOULDER,
        isUnilateral = true,
        defaultHoleCues = "滑轮最低孔 · 手腕穿过身体内侧拉起 · 顶峰停顿 1 秒",
        defaultSets = 4,
        targetRepsMin = 12,
        targetRepsMax = 15,
        defaultRestSeconds = 60,
        progressionType = ProgressionType.METABOLIC_PUMP,
        interchangeableExerciseIds = listOf("push_4_lateral_raise")
    )

    val BARBELL_OHP = Exercise(
        id = "push_var_ohp",
        name = "站姿杠铃推举 (OHP)",
        dayType = DayType.PUSH,
        primaryMuscle = "三角肌前束",
        secondaryMuscles = listOf("三角肌中束", "肱三头肌", "腹直肌/核心"),
        targetMuscle = "三角肌前中束 / 核心稳定",
        loadType = LoadType.BARBELL,
        primaryJoint = JointType.SHOULDER,
        defaultHoleCues = "锁骨起杠 · 臀腹夹紧 · 头部让道后顶峰穿头锁定",
        defaultSets = 4,
        targetRepsMin = 5,
        targetRepsMax = 6,
        defaultRestSeconds = 150,
        progressionType = ProgressionType.DOUBLE_PROGRESSION
    )

    val MACHINE_CHEST_PRESS = Exercise(
        id = "push_var_chest_press",
        name = "坐姿器械推胸",
        dayType = DayType.PUSH,
        primaryMuscle = "胸大肌中下束",
        secondaryMuscles = listOf("肱三头肌"),
        targetMuscle = "胸大肌孤立",
        loadType = LoadType.MACHINE,
        primaryJoint = JointType.SHOULDER,
        defaultHoleCues = "握把对准中胸高度 · 轨迹固定无摇晃 · 离心平稳还原",
        defaultSets = 4,
        targetRepsMin = 8,
        targetRepsMax = 10,
        defaultRestSeconds = 90,
        progressionType = ProgressionType.DOUBLE_PROGRESSION,
        interchangeableExerciseIds = listOf("push_1_bench_press", "push_var_floor_press")
    )

    // ==========================================
    // ---------- D2 拉 (Pull) 动作库 -----------
    // ==========================================
    val BARBELL_DEADLIFT = Exercise(
        id = "pull_1_deadlift",
        name = "屈腿杠铃硬拉",
        dayType = DayType.PULL,
        primaryMuscle = "竖脊肌/后链",
        secondaryMuscles = listOf("臀大肌", "腘绳肌", "前臂腕屈肌"),
        targetMuscle = "后链肌群 / 竖脊肌 / 臀大肌",
        loadType = LoadType.BARBELL,
        primaryJoint = JointType.LOWER_BACK,
        defaultHoleCues = "杠铃贴紧小腿骨 · 腹压饱满锁定 · 脚跟蹬地推地启动",
        defaultSets = 4,
        targetRepsMin = 3,
        targetRepsMax = 5,
        targetRpe = 8.5f,
        defaultRestSeconds = 180,
        progressionType = ProgressionType.WAVE_STRENGTH,
        related1RmKey = "deadlift",
        ratioOf1Rm = 1.0f,
        painFreeVariantIds = listOf("legs_2_rdl", "legs_5_back_extension"),
        interchangeableExerciseIds = listOf("legs_2_rdl", "pull_3_barbell_row")
    )

    val LAT_PULLDOWN = Exercise(
        id = "pull_2_lat_pulldown",
        name = "高位下拉",
        dayType = DayType.PULL,
        primaryMuscle = "背阔肌",
        secondaryMuscles = listOf("肱二头肌", "前臂腕屈肌"),
        targetMuscle = "背阔肌",
        loadType = LoadType.CABLE,
        primaryJoint = JointType.ELBOW,
        defaultHoleCues = "大腿挡板卡紧第 3 孔 · 锁骨引向横杠 · 手肘向后下插",
        defaultSets = 4,
        targetRepsMin = 6,
        targetRepsMax = 8,
        defaultRestSeconds = 120,
        progressionType = ProgressionType.DOUBLE_PROGRESSION,
        interchangeableExerciseIds = listOf("pull_3_barbell_row", "pull_var_chest_row")
    )

    val BARBELL_ROW = Exercise(
        id = "pull_3_barbell_row",
        name = "俯身杠铃划船",
        dayType = DayType.PULL,
        primaryMuscle = "中上背/菱形肌",
        secondaryMuscles = listOf("背阔肌", "肱二头肌", "竖脊肌/后链"),
        targetMuscle = "中背部 / 菱形肌 / 背阔肌",
        loadType = LoadType.BARBELL,
        primaryJoint = JointType.LOWER_BACK,
        defaultHoleCues = "俯身 45 度保持脊柱中立 · 沿大腿拉向肚脐眼",
        defaultSets = 4,
        targetRepsMin = 6,
        targetRepsMax = 8,
        defaultRestSeconds = 120,
        progressionType = ProgressionType.DOUBLE_PROGRESSION,
        related1RmKey = "deadlift",
        ratioOf1Rm = 0.55f,
        painFreeVariantIds = listOf("pull_var_chest_row"),
        interchangeableExerciseIds = listOf("pull_var_chest_row", "pull_2_lat_pulldown")
    )

    val FACE_PULL = Exercise(
        id = "pull_4_face_pull",
        name = "坐姿绳索面拉",
        dayType = DayType.PULL,
        primaryMuscle = "三角肌后束",
        secondaryMuscles = listOf("肩袖外旋肌群", "斜方肌"),
        targetMuscle = "三角肌后束 / 肩袖外旋肌",
        loadType = LoadType.CABLE,
        primaryJoint = JointType.SHOULDER,
        defaultHoleCues = "滑轮对准眼眉高度 · 拇指向后拉 · 双峰展翅顶峰停顿",
        defaultSets = 4,
        targetRepsMin = 12,
        targetRepsMax = 15,
        defaultRestSeconds = 60,
        progressionType = ProgressionType.METABOLIC_PUMP
    )

    val INCLINE_DUMBBELL_CURL = Exercise(
        id = "pull_5_db_curl",
        name = "哑铃斜板弯举",
        dayType = DayType.PULL,
        primaryMuscle = "肱二头肌",
        secondaryMuscles = listOf("前臂腕屈肌"),
        targetMuscle = "肱二头肌长头",
        loadType = LoadType.DUMBBELL,
        primaryJoint = JointType.ELBOW,
        isUnilateral = false,
        defaultHoleCues = "斜板 45 度后仰 · 充分拉伸长头 · 顶部外旋小拇指",
        defaultSets = 3,
        targetRepsMin = 8,
        targetRepsMax = 12,
        defaultRestSeconds = 75,
        progressionType = ProgressionType.DOUBLE_PROGRESSION
    )

    val CHEST_SUPPORTED_ROW = Exercise(
        id = "pull_var_chest_row",
        name = "上斜胸部支撑哑铃划船",
        dayType = DayType.PULL,
        primaryMuscle = "中上背/菱形肌",
        secondaryMuscles = listOf("背阔肌", "肱二头肌"),
        targetMuscle = "中上背 / 菱形肌 (下背零压迫)",
        loadType = LoadType.DUMBBELL,
        primaryJoint = JointType.LOWER_BACK,
        defaultHoleCues = "胸口紧贴斜板 30 度 · 彻底卸除下背负荷 · 手肘向后拉",
        defaultSets = 4,
        targetRepsMin = 8,
        targetRepsMax = 10,
        defaultRestSeconds = 90,
        progressionType = ProgressionType.DOUBLE_PROGRESSION,
        interchangeableExerciseIds = listOf("pull_3_barbell_row")
    )

    // ==========================================
    // ---------- D3 腿 (Legs) 动作库 -----------
    // ==========================================
    val BARBELL_SQUAT = Exercise(
        id = "legs_1_squat",
        name = "杠铃后深蹲",
        dayType = DayType.LEGS,
        primaryMuscle = "股四头肌",
        secondaryMuscles = listOf("臀大肌", "核心竖脊肌"),
        targetMuscle = "股四头肌 / 臀大肌",
        loadType = LoadType.BARBELL,
        primaryJoint = JointType.KNEE,
        defaultHoleCues = "保护杆第 6 孔 · 脚尖外展 30 度 · 膝盖轨迹对准脚尖",
        defaultSets = 4,
        targetRepsMin = 3,
        targetRepsMax = 5,
        targetRpe = 8.5f,
        defaultRestSeconds = 180,
        progressionType = ProgressionType.WAVE_STRENGTH,
        related1RmKey = "squat",
        ratioOf1Rm = 1.0f,
        painFreeVariantIds = listOf("legs_3_split_squat", "legs_var_leg_press", "legs_4_leg_extension"),
        interchangeableExerciseIds = listOf("legs_var_leg_press", "legs_3_split_squat")
    )

    val ROMANIAN_DEADLIFT = Exercise(
        id = "legs_2_rdl",
        name = "罗马尼亚硬拉 (RDL)",
        dayType = DayType.LEGS,
        primaryMuscle = "腘绳肌",
        secondaryMuscles = listOf("臀大肌", "竖脊肌/后链"),
        targetMuscle = "腘绳肌 / 臀大肌",
        loadType = LoadType.BARBELL,
        primaryJoint = JointType.HIP,
        defaultHoleCues = "臀部向后折叠 · 膝盖微屈固定角度 · 感受腘绳肌强烈牵拉",
        defaultSets = 4,
        targetRepsMin = 6,
        targetRepsMax = 8,
        defaultRestSeconds = 120,
        progressionType = ProgressionType.DOUBLE_PROGRESSION,
        related1RmKey = "squat",
        ratioOf1Rm = 0.65f,
        painFreeVariantIds = listOf("legs_5_back_extension"),
        interchangeableExerciseIds = listOf("legs_5_back_extension")
    )

    val BULGARIAN_SPLIT_SQUAT = Exercise(
        id = "legs_3_split_squat",
        name = "保加利亚单腿蹲",
        dayType = DayType.LEGS,
        primaryMuscle = "股四头肌",
        secondaryMuscles = listOf("臀大肌"),
        targetMuscle = "臀中肌 / 股四头肌 (单侧平衡)",
        loadType = LoadType.DUMBBELL,
        primaryJoint = JointType.KNEE,
        isUnilateral = true,
        defaultHoleCues = "后脚搭平凳第 2 孔 · 躯干微前倾 · 垂直下蹲后膝近地",
        defaultSets = 3,
        targetRepsMin = 8,
        targetRepsMax = 10,
        defaultRestSeconds = 90,
        progressionType = ProgressionType.UNILATERAL_BALANCE,
        interchangeableExerciseIds = listOf("legs_1_squat", "legs_var_leg_press")
    )

    val LEG_EXTENSION = Exercise(
        id = "legs_4_leg_extension",
        name = "坐姿腿屈伸",
        dayType = DayType.LEGS,
        primaryMuscle = "股四头肌",
        secondaryMuscles = emptyList(),
        targetMuscle = "股四头肌孤立充血",
        loadType = LoadType.MACHINE,
        primaryJoint = JointType.KNEE,
        defaultHoleCues = "靠背贴紧 · 轴心对准膝关节 · 顶峰挤压 1 秒",
        defaultSets = 3,
        targetRepsMin = 10,
        targetRepsMax = 12,
        defaultRestSeconds = 60,
        progressionType = ProgressionType.METABOLIC_PUMP
    )

    val BACK_EXTENSION = Exercise(
        id = "legs_5_back_extension",
        name = "山羊挺身 (后链强化)",
        dayType = DayType.LEGS,
        primaryMuscle = "竖脊肌/后链",
        secondaryMuscles = listOf("臀大肌", "腘绳肌"),
        targetMuscle = "竖脊肌 / 臀大肌",
        loadType = LoadType.BODYWEIGHT,
        primaryJoint = JointType.LOWER_BACK,
        defaultHoleCues = "挡垫调节至髂前上棘下方 · 骨盆固定 · 后链发力顶峰收缩",
        defaultSets = 3,
        targetRepsMin = 12,
        targetRepsMax = 15,
        defaultRestSeconds = 60,
        progressionType = ProgressionType.WEIGHTED_BODY,
        interchangeableExerciseIds = listOf("legs_2_rdl")
    )

    val LEG_PRESS = Exercise(
        id = "legs_var_leg_press",
        name = "45度倒蹬机 (腿举)",
        dayType = DayType.LEGS,
        primaryMuscle = "股四头肌",
        secondaryMuscles = listOf("臀大肌"),
        targetMuscle = "股四头肌 / 臀大肌 (腰椎零压力)",
        loadType = LoadType.MACHINE,
        primaryJoint = JointType.LOWER_BACK,
        defaultHoleCues = "臀部紧贴靠垫勿悬空 · 双脚与肩同宽 · 下降至膝盖 90 度",
        defaultSets = 4,
        targetRepsMin = 8,
        targetRepsMax = 10,
        defaultRestSeconds = 120,
        progressionType = ProgressionType.DOUBLE_PROGRESSION,
        interchangeableExerciseIds = listOf("legs_1_squat", "legs_3_split_squat")
    )

    // 全量黄金预置集合
    val ALL_PRESETS = listOf(
        // 推
        BARBELL_BENCH_PRESS, INCLINE_DUMBBELL_PRESS, DIPS, DUMBBELL_LATERAL_RAISE, CABLE_PUSHDOWN,
        DB_FLOOR_PRESS, CHEST_SUPPORTED_LATERAL_RAISE, CABLE_LATERAL_RAISE, BARBELL_OHP, MACHINE_CHEST_PRESS,
        // 拉
        BARBELL_DEADLIFT, LAT_PULLDOWN, BARBELL_ROW, FACE_PULL, INCLINE_DUMBBELL_CURL,
        CHEST_SUPPORTED_ROW,
        // 腿
        BARBELL_SQUAT, ROMANIAN_DEADLIFT, BULGARIAN_SPLIT_SQUAT, LEG_EXTENSION, BACK_EXTENSION,
        LEG_PRESS
    )

    fun getExercisesForDay(dayType: DayType): List<Exercise> {
        return when (dayType) {
            DayType.PUSH -> listOf(BARBELL_BENCH_PRESS, INCLINE_DUMBBELL_PRESS, DIPS, DUMBBELL_LATERAL_RAISE, CABLE_PUSHDOWN)
            DayType.PULL -> listOf(BARBELL_DEADLIFT, LAT_PULLDOWN, BARBELL_ROW, FACE_PULL, INCLINE_DUMBBELL_CURL)
            DayType.LEGS -> listOf(BARBELL_SQUAT, ROMANIAN_DEADLIFT, BULGARIAN_SPLIT_SQUAT, LEG_EXTENSION, BACK_EXTENSION)
            DayType.REST -> emptyList()
        }
    }

    fun findById(id: String): Exercise? {
        return ALL_PRESETS.find { it.id == id }
    }
}
