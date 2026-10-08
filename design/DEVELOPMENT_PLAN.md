# Reshape 智能力量训练产品 Kuikly 跨端开发计划 (v1.0)

> **版本**：v1.0 · Kuikly Compose 跨端特化版  
> **关联 PRD**：[PRD.md (v2.0 资深特化版)](file:///d:/workspace/Reshape/design/PRD.md)  
> **设计稿源**：[全站设计稿画廊 (index.html)](file:///d:/workspace/Reshape/design/index.html)  
> **适用平台**：Android / iOS / HarmonyOS (鸿蒙) / H5 / 微信小程序 (5 端同构)  
> **架构技术**：Kotlin Multiplatform (KMM) + Kuikly Compose (Compose DSL)

---

## 1. 项目概述与技术栈架构

### 1.1 核心架构模式
基于 **Kuikly Compose (Jetpack Compose DSL on KMM)** 构建单工程跨端架构，业务逻辑与 UI 声明式代码 100% 收敛于 `shared/src/commonMain/kotlin`，通过统一编译体系分发至 5 大宿主端：

```
                               ┌────────────────────────────────────────┐
                               │  Kuikly Compose Shared Module (shared) │
                               │  - 声明式 UI (Compose DSL)              │
                               │  - 核心算法引擎 (Brzycki/DOTS/装片)     │
                               │  - 本地存储与秒级自愈草稿 (SQLite/KV)   │
                               └──────────────────┬─────────────────────┘
                                                  │
                ┌──────────────────┬──────────────┼──────────────┬──────────────────┐
                ▼                  ▼              ▼              ▼                  ▼
        【Android 宿主】      【iOS 宿主】   【鸿蒙宿主】   【H5 Web 宿主】    【微信小程序】
        (:androidApp)        (:iosApp)      (:ohosApp)     (:h5App)          (:miniApp)
```

### 1.2 核心技术栈选型

| 维度 | 技术选型 | 版本/规范说明 |
| :--- | :--- | :--- |
| **语言与运行时** | Kotlin Multiplatform | 默认配置: `2.1.21` / 鸿蒙配置: `2.0.21-KBA-010` |
| **UI 渲染引擎** | Kuikly Compose | `Version.getKuiklyVersion()` (`2.7.0-2.1.21`) |
| **包名引用规则** | 运行时 / Kuikly 专有包 | 仅 `androidx.compose.runtime.*` 使用官方包，其余全部使用 `com.tencent.kuikly.compose.*` |
| **状态架构** | MVI / StateFlow | 响应式状态流驱动，单向数据流，页面容器继承 `ComposeContainer` |
| **数据持久化** | 本地 SQLite / KV 存储 | 会话秒级落盘、训练日历归档、用户身体档案与离线缓存 |
| **原生能力桥接** | `BridgeModule` (HRBridge) | 耳机穿透轻音播放、PR 突破三道强振动、战报卡长图导出 |

### 1.3 状态管理架构 (MVI + SharedStateFlow)

采用单向数据流 MVI 架构，所有页面容器继承 `ComposeContainer`，内部 ViewModel 通过 `StateFlow<UiState>` 驱动 Compose UI 声明式刷新：

```
                    ┌──────────────┐
  UI Event ───────► │  ViewModel   │ ───► StateFlow<UiState> ───► Compose UI
                    │  (Reducer)   │
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │  Repository  │ ◄──► SQLite / KV / Network
                    └──────────────┘
```

**跨页面状态同步机制**：

| 触发场景 | 数据流 | 刷新范围 |
| :--- | :--- | :--- |
| 1RM 修改 | `UserProfileRepository.oneRepMaxFlow` | 所有订阅了建议重量的 ViewModel 自动重算 |
| 主题切换 | `ThemeStateHolder.currentTheme` (全局 StateFlow) | 全部页面实时响应 |
| 训练完成写入 | `WorkoutHistoryStore.latestSessionFlow` | 首页恢复雷达 + 日历页 + 递进页 |
| 计划编排变更 | `PlanRepository.currentPlanFlow` | 首页动作预览 + 记录页动作列表 |
| 性别切换 | `UserProfileRepository.genderFlow` | DOTS 评分 + 标准杆重 + 力量评级 |

---

## 2. 全站信息架构与 @Page 路由映射表

根据 [PRD.md#2](file:///d:/workspace/Reshape/design/PRD.md#L22)，全站 12 个核心页面在 Kuikly 中的容器定义与路由映射如下：

| 页面名称 | 对应设计稿 | Kuikly 页面注解 (`@Page`) | 页面容器类路径 | 功能职责说明 |
| :--- | :--- | :--- | :--- | :--- |
| **今日训练首页** | [home-v2.html](file:///d:/workspace/Reshape/design/home-v2.html) | `@Page("home")` | `features.home.HomePager` | Tab 1 首页，循环进度、就绪度切换、装备清单、恢复雷达 |
| **逐组实战记录** | [record-v2.html](file:///d:/workspace/Reshape/design/record-v2.html) | `@Page("record")` | `features.record.RecordPager` | 健身房肉体实战核心页，9 宫格键盘、悬浮休息仓、无痛变式 |
| **训练完成战报** | [summary.html](file:///d:/workspace/Reshape/design/summary.html) | `@Page("summary")` | `features.summary.SummaryPager` | 训练结算、今日 PR 高光、老铁纯黑战报长图生成与导出 |
| **计划大纲全览** | [plan.html](file:///d:/workspace/Reshape/design/plan.html) | `@Page("plan_outline")` | `features.plan.PlanOutlinePager` | 凯谭经典三分化总纲与 15 个动作全览 |
| **训练计划编排** | [plan-edit.html](file:///d:/workspace/Reshape/design/plan-edit.html) | `@Page("plan_edit")` | `features.plan.PlanEditPager` | D1/D2/D3 动作增删、拖拽排序、超级组绑定与模板重置 |
| **全量动作库与变式图谱** | [exercise-library.html](file:///d:/workspace/Reshape/design/exercise-library.html) | `@Page("exercise_library")` | `features.plan.ExerciseLibraryPager` | 全 App 动作资产中枢，主辅肌群字典、5大器械、无痛变式图谱、参数编辑与本地持久化 |
| **单日设计蓝图** | `day-*.html` | `@Page("day_blueprint")` | `features.plan.DayBlueprintPager` | D1推 / D2拉 / D3腿 5 动作设计理念与建议重量跑数 |
| **周期递进减载** | [progression-v2.html](file:///d:/workspace/Reshape/design/progression-v2.html) | `@Page("progression")` | `features.progression.ProgressionPager` | 8 次柱状趋势、连续停滞诊断与出口、出差复训保护 |
| **三大项与 1RM** | [one-rep-max.html](file:///d:/workspace/Reshape/design/one-rep-max.html) | `@Page("one_rep_max")` | `features.profile.OneRepMaxPager` | 深蹲/卧推/硬拉 1RM 档案、Brzycki 反推与 DOTS 标准 |
| **进阶训练原则** | [principles.html](file:///d:/workspace/Reshape/design/principles.html) | `@Page("principles")` | `features.knowledge.PrinciplesPager` | RPE 8、力竭时机、渐进超负荷、CNS 中枢神经疲劳管理 |
| **训练日历成就** | [history-calendar.html](file:///d:/workspace/Reshape/design/history-calendar.html) | `@Page("history")` | `features.history.HistoryCalendarPager` | Tab 3 记录页，7×5 热力打卡日历、月总吨位、单日简报 |
| **个人资料设置** | [profile.html](file:///d:/workspace/Reshape/design/profile.html) | `@Page("profile")` | `features.profile.ProfilePager` | Tab 4 我的页，身体档案、男女 DOTS 联动、6 色主题与 CSV 导出 |
| **训练知识库** | [knowledge.html](file:///d:/workspace/Reshape/design/knowledge.html) | `@Page("knowledge")` | `features.knowledge.KnowledgePager` | Tab 2 知识页，3 级离线缓存、同步状态条与云端拓展库 |

**辅助浮层与抽屉组件** (非独立页面，隶属 `features.record` 模块)：

| 组件名称 | 对应设计稿 | 组件类型 | 组件类路径 | 功能职责说明 |
| :--- | :--- | :--- | :--- | :--- |
| **休息时长修改** | [rest-duration.html](file:///d:/workspace/Reshape/design/rest-duration.html) | 底部抽屉 (Drawer) | `features.record.RestDurationDrawer` | 练中手动修改当前组的休息时长 |
| **全动作概览** | [sheet.html](file:///d:/workspace/Reshape/design/sheet.html) | 底部半屏 (Sheet) | `features.record.AllExercisesSheet` | 练中查看/跳转本次训练全部动作列表 |

---

## 3. 模块包结构设计 (Package Topology)

所有源代码组织在 `shared/src/commonMain/kotlin/com/woo/kuiklycomposetemplate/` 目录下：

```
com.woo.kuiklycomposetemplate/
├── core/                                   # 核心设计系统与通用组件
│   ├── theme/                              # 设计令牌与主题引擎
│   │   ├── ReshapeTheme.kt                 # Kuikly ThemeProvider (支持 6 色主题动态切换)
│   │   ├── ColorTokens.kt                  # 颜色与渐变定义 (对齐 tokens.css)
│   │   └── DimensionTokens.kt              # 间距与圆角 (8dp, 12dp, 16dp 等)
│   ├── components/                         # 通用原子级 UI 组件
│   │   ├── StepperWidget.kt                # ±2.5kg 重量步进器
│   │   ├── GymNumPadDrawer.kt              # 9 宫格大按键数字键盘抽屉 (+2.5/+5/+10/自重)
│   │   ├── FloatingRestDock.kt             # 吸底常驻悬浮休息仓 (±15s, 进度条, 跳过)
│   │   ├── PlateVisualizer.kt              # 彩色奥赛杠铃片装配示意图
│   │   ├── WarmupLadderCard.kt             # 3 阶科学热身阶梯卡片
│   │   └── PrGlowCard.kt                   # NEW PR 暗金呼吸流光激励卡
│   └── bridge/                             # 原生端能力桥接模块
│       ├── AudioBridge.kt                  # 耳机穿透低调轻音 (Duck 模式)
│       ├── HapticBridge.kt                 # PR 突破三道强振动 / 倒计时结束震动
│       └── ShareImageBridge.kt             # 战报图 Canvas 绘制与系统相册保存
│
├── domain/                                 # 纯 Kotlin 领域模型与核心算法 (100% 独立可测)
│   ├── algorithm/
│   │   ├── BrzyckiCalculator.kt            # 1RM 公式推算与 3~5 次无损反推算法
│   │   ├── WeightSuggestEngine.kt          # 建议起始重量矩阵 (含自重/附加重折算)
│   │   ├── DotsScoreCalculator.kt          # 男女双标准四次多项式 DOTS 评分引擎
│   │   ├── PlateLoaderEngine.kt            # 最少片数贪心装载算法 (20kg/15kg 杆 + 微加重片)
│   │   └── VolumeCalculator.kt             # 有效工作容量计算 (严格隔离热身组)
│   └── model/                              # 领域数据实体
│       ├── Exercise.kt                     # 动作元数据 (名称/部位/孔位/口诀/递进方式)
│       ├── WorkoutSession.kt               # 完整训练会话数据 (对齐 PRD 5.2 JSON 契约)
│       ├── ProgressionCycle.kt             # 8 次周期递进状态实体
│       └── UserProfile.kt                  # 身体维度、男女生理性别与三大项基准
│
├── data/                                   # 数据持久化与网络缓存层
│   ├── local/
│   │   ├── SessionDraftStore.kt            # 练中做组秒级持久化 (崩溃/退出 100% 现场恢复)
│   │   ├── WorkoutHistoryStore.kt          # 历史出勤记录与日历月度聚合
│   │   └── UserPreferencesStore.kt         # 偏好设置 (主题/性别/杆重/提示音)
│   └── remote/
│       ├── KnowledgeManifestApi.kt         # 远端 knowledge_manifest.json 静默拉取
│       └── KnowledgeCacheManager.kt        # 知识库三级缓存 (内存 ➔ SQLite ➔ CDN)
│
└── features/                               # 各功能业务模块与页面容器 (@Page)
    ├── home/                               # Tab 1: 首页 (Home-v2)
    ├── record/                             # 逐组实战记录 (Record-v2)
    ├── summary/                            # 训练结算与分享 (Summary)
    ├── plan/                               # 计划大纲、编排与统一动作配置
    ├── progression/                        # 周期递进与减载 (Progression-v2)
    ├── history/                            # Tab 3: 日历与成就 (History-Calendar)
    ├── profile/                            # Tab 4: 档案与设置 (Profile / OneRepMax)
    └── knowledge/                          # Tab 2: 知识库 (Knowledge / Principles)
```

---

## 4. 核心计算算法与数据模型契约

### 4.1 核心算法实现规范 (100% 对齐 PRD 第 4 章)

```kotlin
object BrzyckiCalculator {
    /** Brzycki 1RM 推算：1RM = w / (1.0278 - 0.0278 * r)，四舍五入至 2.5kg 步长 */
    fun calculate1RM(weightKg: Float, reps: Int): Float {
        if (reps <= 1) return weightKg
        val raw = weightKg / (1.0278f - 0.0278f * reps)
        return (kotlin.math.round(raw / 2.5f) * 2.5f)
    }
}

object DotsScoreCalculator {
    /** 男女双标准四次多项式 DOTS 力量系数计算 */
    fun calculateDots(totalKg: Float, bodyWeightKg: Float, isMale: Boolean): Float {
        val (a0, a1, a2, a3, a4) = if (isMale) {
            doubleArrayOf(-307.75076, 24.0900756, -0.1918759221, 0.0007391293, -0.000001093)
        } else {
            doubleArrayOf(-57.96288, 13.6175032, -0.1126655495, 0.0005158568, -0.0000010706)
        }
        val bw = bodyWeightKg.toDouble()
        val denom = a0 + a1 * bw + a2 * bw * bw + a3 * bw * bw * bw + a4 * bw * bw * bw * bw
        return ((totalKg.toDouble() * (500.0 / denom))).toFloat()
    }
}

object PlateLoaderEngine {
    val PLATES = listOf(25.0f, 20.0f, 15.0f, 10.0f, 5.0f, 2.5f, 1.25f, 0.5f)
    
    /** 贪心扣减最少片数装载序列 */
    fun computeSingleSidePlates(targetTotalKg: Float, barWeightKg: Float = 20.0f): List<Float> {
        var remain = (targetTotalKg - barWeightKg) / 2.0f
        if (remain <= 0f) return emptyList()
        val result = mutableListOf<Float>()
        for (plate in PLATES) {
            while (remain >= plate - 0.01f) {
                result.add(plate)
                remain -= plate
            }
        }
        return result
    }
}
```

### 4.2 会话数据标准契约 (对齐 Phase 2 AI 兼容)

在 `domain/model/WorkoutSession.kt` 中严格落盘该 JSON Schema：

```kotlin
@Serializable
data class WorkoutSession(
    val sessionId: String,
    val userId: String,
    val userProfile: SessionUserProfile,
    val planContext: SessionPlanContext,
    val sessionMetrics: SessionMetrics,
    val exercises: List<SessionExercise>
)

@Serializable
data class SessionExercise(
    val exerciseId: String,
    val name: String,
    val category: String,
    val isUnilateral: Boolean,
    val memo: String = "",
    val sets: List<SessionSet>
)

@Serializable
data class SessionSet(
    val setNo: Int,               // 全局做组顺序号 (1-based)
    val isWarmup: Boolean,        // 是否热身组 (true 则不计入总容量)
    val weightKg: Float,          // 单手或杠铃总重
    val leftReps: Int? = null,    // 单侧左侧次数
    val rightReps: Int? = null,   // 单侧右侧次数
    val effectiveReps: Int,       // 有效次数 (单侧以弱侧为准)
    val rpe: Float,               // RPE (6.0 - 10.0)
    val isPr: Boolean,            // 是否突破 PR
    val restSeconds: Int          // 实际休息秒数
)
```

### 4.3 数据持久化规范

#### SQLite 核心表设计

| 表名 | 用途 | 主要字段 |
| :--- | :--- | :--- |
| `session_drafts` | 练中草稿秒级落盘 | `draft_id, session_json, updated_at, is_recovered` |
| `workout_history` | 已完成训练归档 | `session_id, day_type, date, metrics_json, exercises_json` |
| `exercise_pr_records` | 动作维度 PR 历史天花板 | `exercise_id, max_weight_kg, max_reps, best_volume, achieved_date` |
| `progression_cycles` | 8 次周期递进快照 | `exercise_id, cycle_index, suggested_kg, actual_kg, reps, is_stalled` |
| `knowledge_cache` | 知识库 L2 缓存 | `article_id, markdown_text, manifest_version, cached_at` |
| `user_preferences` | 偏好设置 KV | `key, value_json, updated_at` |

#### 草稿落盘策略

- **触发时机**：每完成一组打卡后立即持久化 + 重量/次数修改后 500ms 防抖合并写入；
- **写入方式**：SQLite 单事务，整个 `WorkoutSession` JSON 序列化后覆盖写入 `session_drafts` 表（简单可靠，避免增量 diff 复杂度）；
- **恢复逻辑**：App 冷启动时查询 `is_recovered = false` 的草稿记录，若存在则在首页顶部展示恢复横幅；
- **清除时机**：用户点击「完成并返回首页」或主动「放弃草稿」时删除对应记录。

#### 数据迁移策略
- 使用版本号管理 Schema 变更，每次升级执行增量迁移脚本 `migration_vN_to_vN+1`；
- 迁移失败不阻塞启动，降级为清除本地缓存并重新初始化（训练历史为核心资产不可丢失，需额外备份机制）。

### 4.4 训练会话生命周期状态机

```kotlin
/**
 * 训练会话生命周期状态机
 *
 *  ┌─────┐                                 ┌──────────────┐
 *  │ Idle │─── 检测到未完成草稿 ──────────► │ DraftDetected │
 *  └──┬──┘                                 └──────┬───────┘
 *     │ 点击"开始训练"                             │
 *     ▼                                    ┌───────▼───────┐
 *  ┌─────────┐                             │ 继续  │ 放弃  │
 *  │Planning │                             └──┬──────┬────┘
 *  └────┬────┘                                │      │
 *       │                                     │      └──► Idle
 *       ▼                                     ▼
 *  ┌───────────┐     意外杀进程/崩溃     ┌─────────────┐
 *  │ Recording │ ◄──────────────────── │ Interrupted  │
 *  │ (秒级落盘) │ ─────────────────────► │ (重启恢复)   │
 *  └─────┬─────┘                       └─────────────┘
 *        │ 点击"结束并记录"
 *        ▼
 *  ┌───────────┐    返回首页 (清除草稿)
 *  │ Summary   │ ──────────────────────► Idle
 *  └───────────┘
 */
enum class SessionState {
    IDLE,              // 无活跃训练
    DRAFT_DETECTED,    // 检测到未完成草稿，等待用户决策
    RECORDING,         // 训练进行中 (每组完成后秒级落盘)
    INTERRUPTED,       // 意外中断 (杀进程/崩溃/来电)
    COMPLETING         // 结算中 (跳转战报页)
}
```

---

## 5. 分阶段开发里程碑与任务清单 (Milestones & Sprint Plan)

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                               开发排期甘特概览                                    │
│                                                                                  │
│  [M1: 底座与算法] ──► [M2: 首页与训练闭环] ──┬──► [M3: 计划编排] ──┐              │
│                                               │                     ├──► [M5: 联调]│
│                                               └──► [M4: 知识/递进] ─┘              │
└──────────────────────────────────────────────────────────────────────────────────┘
```

#### 任务依赖关系与并行度

| 任务 | 前置依赖 | 可并行任务 |
| :--- | :--- | :--- |
| M1.1 设计系统 | 无 (首日启动) | M1.2 算法引擎 |
| M1.2 算法引擎 | 无 | M1.1 设计系统 |
| M1.3 数据存储 | M1.2 (需数据模型定义) | — |
| M2.1 首页 | M1.1 + M1.3 | M2.2 / M2.3 (可同步) |
| M2.2 记录页 | M1.1 + M1.2 + M1.3 | M2.1 / M2.3 |
| M2.3 交互组件 | M1.1 | M2.1 / M2.2 (可同步) |
| M2.4 战报页 | M2.2 (需会话数据) | — |
| M3 计划编排 | M1.1 + M1.3 | **M4 (可完全并行)** |
| M4 知识/递进/日历 | M1.1 + M1.3 | **M3 (可完全并行)** |
| M5 多端联调 | M1~M4 全部完成 | — |

> **关键路径**：M1 → M2.2 (记录页) → M2.4 (战报页) → M5。M3 与 M4 不在关键路径上，可与 M2 后段并行推进。

### Milestone 1：架构底座、设计系统与核心算法引擎 (预计 3-4 天)
- [ ] **M1.1 设计系统与主题引擎**：
  - 编写 `ColorTokens.kt`、`DimensionTokens.kt`，映射 `tokens.css` 变量；
  - 实现 `ReshapeTheme.kt`，支持 6 种专业运动主题实时切换（橙、蓝、青、绿、金、紫）。
- [ ] **M1.2 核心算法引擎与单元测试**：
  - 实现 `BrzyckiCalculator`（1RM 推算与反推）；
  - 实现 `WeightSuggestEngine`：
    - 主辅动作 1RM 生物力学换算系数模型（D1/D2/D3 全量动作初始重量自动生成）；
    - 逐组单调递增波浪推进算法（Ascending Wave Model，防平梯保障 $\text{Weight}_1 < \text{Weight}_2 < \text{Weight}_3 < \text{Weight}_4$）；
    - 3 阶科学热身阶梯重量生成引擎（空杆/50%/70%/85% 零疲劳隔离）；
  - 实现 `DotsScoreCalculator`（男女双标准多项式评分）；
  - 实现 `PlateLoaderEngine`（最少片数装片算法，支持 20kg/15kg 杆与微调片）；
  - 编写 100% 覆盖率的算法单元测试用例。
- [ ] **M1.3 本地数据存储基础**：
  - 封装 Session 契约 JSON 序列化器与本地持久化接口。

### Milestone 2：核心训练闭环 — 首页、实战记录与战报结算 (预计 5-6 天)
- [ ] **M2.1 「训练」首页 (`HomePager.kt` · `home-v2.html`)**：
  - 实现 D1/D2/D3 循环位置条，支持 `🛌 今天休息` 顺延与 `⇄ 换部位` 调度；
  - 实现状态就绪度切换（⚡充沛 ⇋ 🔋疲劳降重 10%）；
  - 实现出差/停训复训保护横幅（停训 >7 天 80% 唤醒负荷）；
  - 实现出门装备清单卡片（腰带/护膝/鞋自动提示）；
  - 实现肌群超量恢复雷达卡片（胸肩/背/腿恢复进度条）；
  - 实现**练中草稿异常中断自愈恢复条**（未完成训练一键恢复现场）。
- [ ] **M2.2 逐组记录核心页面 (`RecordPager.kt` · `record-v2.html`)**：
  - 搭建实战顶栏（退出草稿确认、调换动作入口、练中秒表时钟）；
  - 实现 `PrGlowCard`（NEW PR 呼吸流光与振动触发）；
  - 实现无痛变式抽屉 `PainFreeDrawer`（一键替换关节友好动作）；
  - 实现孔位口诀备忘条与 3 阶科学热身阶梯展示；
  - 实现单侧弱侧平衡输入（左/右分别记录，弱侧定有效组）与 Ghost Set 上次同组镜像；
  - 实现 `RestDurationDrawer`（练中手动修改当前组休息时长抽屉）；
  - 实现 `AllExercisesSheet`（练中查看/跳转本次训练全部动作半屏浮层）。
- [ ] **M2.3 实战交互特化组件**：
  - 实现 `GymNumPadDrawer`（9 宫格数字大键盘抽屉，规避系统键盘弹起问题）；
  - 实现 `FloatingRestDock`（常驻悬浮休息仓，支持 `±15s` 微调与立即跳过）；
  - 实现 `PlateVisualizer`（彩色奥赛挂片弹窗）。
- [ ] **M2.4 训练完成战报页 (`SummaryPager.kt` · `summary.html`)**：
  - 实现 3 项汇总指标（有效正式组、总容量 kg、训练时长 min）；
  - 实现今日突破 PR 高光卡片；
  - 实现老铁极简纯黑战报卡 `StoryCard`（支持 Canvas 导出长图与复制文本）；
  - 预留 Phase 2 AI 力量教练展示卡位（展示"即将上线"静态态）。

### Milestone 3：训练计划编排与动作配置系统 (预计 4-5 天)
- [ ] **M3.1 训练计划编排页 (`PlanEditPager.kt` · `plan-edit.html`)**：
  - 实现 D1/D2/D3 Tab 联动切换；
  - 实现动作长按拖拽排序与左滑删除；
  - 实现超级组绑定高亮呈现与一键解绑；
  - 实现一键重置为官方经典 5 动作模板。
- [ ] **M3.2 统一动作配置页 (`ExerciseEditPager.kt` · `exercise-edit.html`)**：
  - 统一支持"计划编排态"与"练中临时加动作"；
  - 负重形式、波浪次数区间、目标 RPE、递进策略与器械孔位配置；
  - 智能关联三大项 1RM 与折算系数；
  - 保存生效范围（仅本次有效 vs 永久写入计划）。
- [ ] **M3.3 官方计划大纲与单日蓝图**：
  - 实现 `PlanOutlinePager.kt` 与 `DayBlueprintPager.kt`（推/拉/腿 5 动作详解）。
- [ ] **M3.4 全量动作库、自定义动作与无痛变式图谱体系 (`ExerciseLibraryPager.kt` · `sheet.html`)**：
  - 实现全量动作库管理器 `ExerciseLibraryRepository`（内置黄金动作 + 用户自定义动作持久化）；
  - 实现动作库页面与计划编排动作选择器（支持按部位/器械筛选、搜索与一键添加到计划）；
  - 实现生物力学动作间无痛变式图谱与关节保护数据映射（`JointType`、`PainFreeVariantRule`），打通 `PainFreeDrawer` 动态数据源。

### Milestone 4：知识库热更新、周期递进与日历档案 (预计 4-5 天)
- [ ] **M4.1 力量训练知识库 (`KnowledgePager.kt` · `knowledge.html`)**：
  - 实现本地预置大纲与核心训练原则（RPE 8、力竭法则、超量恢复与 CNS 疲劳）；
  - 实现 3 级离线缓存机制（内存 ➔ SQLite ➔ CDN）；
  - 启动静默拉取 `knowledge_manifest.json`，支持弱网无感降级与离线状态条展示。
- [ ] **M4.2 周期递进与减载页 (`ProgressionPager.kt` · `progression-v2.html`)**：
  - 实现 8 次训练柱状趋势图（按重量归一化高度，绿/橙/主色/灰四态表达）；
  - 实现连续 2 次无法达标的停滞诊断卡与 4 大出口（减重/减组/1周减载/换动作）；
  - 实现重测 1RM 触发条件说明与主动开启减载周。
- [ ] **M4.3 训练日历与成就统计 (`HistoryCalendarPager.kt` · `history-calendar.html`)**：
  - 实现月度 4 项核心成果卡（出勤天数、总吨位、总时长、PR 次数）；
  - 实现 7×5 互动训练日历打卡矩阵（点选任意日期动态切换下方动作明细与战报入口）；
  - 呈现连续出勤勋章。
- [ ] **M4.4 个人资料与设置 (`ProfilePager.kt` · `profile.html`)**：
  - 个人身份、铁龄与身体指标矩阵（身高、当前体重、目标体重、体脂率、骨骼肌）；
  - 生理性别切换（`♂ 男` vs `♀ 女`），联动切换 DOTS 多项式与标准杆重；
  - 集中管理三大项 1RM 档案（[one-rep-max.html](file:///d:/workspace/Reshape/design/one-rep-max.html)）；
  - 系统设置：主题配色、耳机穿透提示音、屏幕防误触常亮与训练数据 CSV 导出。

### Milestone 5：5 端编译联调、性能调优与验收 (预计 3-4 天)

#### 5 端平台适配矩阵

| 平台 | 最低版本 | 编译命令 | 特殊处理 | 已知风险 |
| :--- | :--- | :--- | :--- | :--- |
| **Android** | API 24 (7.0) | `./gradlew :androidApp:assembleDebug` | 前台 Service 保活草稿落盘 | 厂商杀后台行为差异 |
| **iOS** | iOS 15.0 | `:shared:podPublishXCFramework` + Xcode | 背景音频需 `AVAudioSession` 配置 | App Store 审核周期 |
| **HarmonyOS** | API 12 | `settings.ohos.gradle.kts` + DevEco Studio | 需 DevEco Studio 4.x 适配 | Kuikly 鸿蒙端成熟度 |
| **H5 Web** | Chrome 90+ | `:h5App:publishLocalJSBundle` | Service Worker 离线缓存 | IndexedDB 容量限制 |
| **微信小程序** | 基础库 3.0 | `:miniApp:jsMiniAppProductionWebpack` | 包体积限制 (2MB 主包) | Canvas 导出长图兼容性 |

- [ ] **M5.1 5 端宿主编译走查**：
  - 按上表逐平台编译安装，真机/模拟器走查全部 12 页面 + 辅助浮层。
- [ ] **M5.2 跨端兼容性与体验优化**：
  - 消除各平台手势冲突与页面切换卡顿；
  - 验证地下室 100% 离线脱机运行（0 白屏、0 报错）；
  - 验证练中异常杀进程后 100% 恢复草稿数据。

---

## 6. Kuikly Compose 关键技术攻坚点与避坑规范

根据 [CLAUDE.md](file:///d:/workspace/Reshape/CLAUDE.md) 约束，开发时必须严格遵守以下准则：

### 6.1 布局与滚动约束
- 🚫 **绝对禁止**使用 `Modifier.verticalScroll(state)` 或 `Modifier.horizontalScroll(state)`（Kuikly 会编译报错或失效）；
- ✅ **唯一正确方案**：单屏滚动一律使用 `LazyColumn { item { ... } }` 或 `LazyRow { item { ... } }`；
- ✅ **吸底操作区**：悬浮休息仓与底部 CTA 按钮使用父级 `Box` + `Modifier.align(Alignment.BottomCenter)`，并在滚动列表底部加入 `Spacer(Modifier.height(140.dp))` 垫高。

### 6.2 表单输入与数字键盘
- 🚫 **禁用原生 API**：Kuikly 不支持 `OutlinedTextField`、`BasicTextField2`；
- ✅ **重量与组数**：使用自建的 `GymNumPadDrawer`（9 宫格 Compose 大键盘），彻底避免系统软键盘弹起挤压视口；
- ✅ **文字输入**：使用 `TextField` 配合自定义 `shape = RoundedCornerShape(...)` 与 `colors = TextFieldDefaults.colors(...)`。

### 6.3 图标与视觉素材
- 🚫 **禁用原生 API**：Kuikly 不支持 Material `Icon` / `IconImage`；
- ✅ **正确方案**：矢量图标与状态展示一律使用 `Image` 配合资源图片，或使用高保真 Unicode 符号（如 `👑`、`⚡`、`🛌`、`🩹`、`🧮`、`🏆`）。

### 6.4 跨端振动与音频穿透
- 通过 `BridgeModule`（`HRBridgeModule`）下发平台指令：
  - **PR 突破**：`bridgeModule.vibrate("heavy_triple")`（调用宿主触觉反馈引擎）；
  - **休息结束**：`bridgeModule.playDuckAudio("chime_double")`（以 Duck 模式播放短促轻音，不暂停/不打断音乐播放）。

---

## 7. 质量保障与验收标准 (QA Criteria)

| 验证维度 | 验收标准 | 验证方式 |
| :--- | :--- | :--- |
| **算法准确性** | Brzycki 1RM、DOTS 男女评分、建议重量折算、彩色装片算法 100% 符合 PRD 精度 | 自动化单元测试 (`commonTest`) |
| **地下室离线度** | 在飞行模式/无网环境下，打开 App、查看大纲原则、记录训练、生成战报全流程 0 报错、0 阻塞 | 5 端脱机真机测试 |
| **草稿自愈性** | 逐组打卡期间强杀 App 或关机，重新启动后首页顶部弹出恢复横幅，点击 100% 还原现场 | 异常中断恢复测试 |
| **5 端视觉一致性** | Android / iOS / Ohos / H5 / 小程序界面与设计稿 `index.html` 像素级一致 | 设计走查对照 |
| **流畅度** | 滚动与页面切换稳定 60 FPS，9 宫格键盘点击响应延迟 < 50ms | 性能 Profile 检测 |

---

## 8. 技术风险评估与降级预案

| 风险项 | 概率 | 影响 | 降级方案 |
| :--- | :--- | :--- | :--- |
| Kuikly Compose 部分组件在鸿蒙端渲染异常或 API 缺失 | 高 | 高 | 鸿蒙端推迟至独立 M6 专项适配，优先保 Android + iOS |
| Canvas 长图导出在小程序端不兼容 | 中 | 中 | 小程序端降级为纯文本战报复制分享 |
| SQLite 跨端 API 差异导致数据层适配工作量超预期 | 中 | 高 | 评估 SQLDelight 替代方案，统一 SQL 方言 |
| 9 宫格自定义键盘在异形屏/折叠屏下布局错位 | 低 | 中 | 使用百分比布局 + 安全区域 Insets 适配 |
| 知识库 CDN 资源跨域/证书问题 | 低 | 低 | 降级为纯本地预置底包，云端热更新延后 |
| 练中草稿 SQLite 写入在极端低存储设备上失败 | 低 | 高 | 降级为 KV 存储关键字段 (动作ID+重量+次数)，丢失次要元数据可接受 |

---

*文档完结 · 本开发计划已就绪，可作为后续 Kuikly 编码落地的标准执行依据。*
