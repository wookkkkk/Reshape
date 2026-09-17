package com.woo.kuiklycomposetemplate.features.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager
import com.woo.kuiklycomposetemplate.core.components.ReshapeBottomBar
import com.woo.kuiklycomposetemplate.core.components.ReshapeNavTab
import com.woo.kuiklycomposetemplate.core.theme.ReshapeTheme
import com.woo.kuiklycomposetemplate.core.utils.CalendarUtils
import com.woo.kuiklycomposetemplate.core.utils.YearMonth
import com.woo.kuiklycomposetemplate.core.utils.platformCurrentTimeMillis
import com.woo.kuiklycomposetemplate.data.local.PlanRepository
import com.woo.kuiklycomposetemplate.data.local.TrainingCycleStore
import com.woo.kuiklycomposetemplate.data.local.UserPreferencesStore
import com.woo.kuiklycomposetemplate.data.local.WorkoutHistoryStore
import com.woo.kuiklycomposetemplate.domain.model.DayType
import com.woo.kuiklycomposetemplate.domain.model.Exercise
import com.woo.kuiklycomposetemplate.domain.model.Gender
import com.woo.kuiklycomposetemplate.domain.model.SessionExercise
import com.woo.kuiklycomposetemplate.domain.model.SessionMetrics
import com.woo.kuiklycomposetemplate.domain.model.SessionPlanContext
import com.woo.kuiklycomposetemplate.domain.model.SessionSet
import com.woo.kuiklycomposetemplate.domain.model.SessionUserProfile
import com.woo.kuiklycomposetemplate.domain.model.WorkoutSession

@Page("history", supportInLocal = true)
class HistoryCalendarPager : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ReshapeTheme {
                HistoryCalendarScreen()
            }
        }
    }
}

enum class HistoryFilter(val label: String) {
    ALL("全部"),
    PUSH("推日"),
    PULL("拉日"),
    LEGS("蹲日"),
    PR_ONLY("👑 破PR")
}

@Composable
fun HistoryCalendarScreen() {
    val pager = LocalActivity.current.getPager() as? Pager
    val spModule = pager?.acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)

    var history by remember { mutableStateOf(WorkoutHistoryStore.getHistory(spModule)) }
    val today = remember { CalendarUtils.getToday() } // (y, m, d)
    var currentYearMonth by remember { mutableStateOf(CalendarUtils.getCurrentYearMonth()) }

    var currentFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var isExpandedAll by remember { mutableStateOf(false) }

    var isBackfillDrawerVisible by remember { mutableStateOf(false) }
    var backfillTargetDay by remember { mutableStateOf(today.third) }
    var backfillDayType by remember { mutableStateOf(DayType.PUSH) }
    var successNotification by remember { mutableStateOf<String?>(null) }

    val filteredHistory = when (currentFilter) {
        HistoryFilter.ALL -> history
        HistoryFilter.PUSH -> history.filter { it.planContext.dayCode.contains("D1", ignoreCase = true) || it.planContext.dayTitle.contains("推") }
        HistoryFilter.PULL -> history.filter { it.planContext.dayCode.contains("D2", ignoreCase = true) || it.planContext.dayTitle.contains("拉") }
        HistoryFilter.LEGS -> history.filter { it.planContext.dayCode.contains("D3", ignoreCase = true) || it.planContext.dayTitle.contains("腿") || it.planContext.dayTitle.contains("蹲") }
        HistoryFilter.PR_ONLY -> history.filter { it.sessionMetrics.prBreakCount > 0 }
    }

    val displayedHistory = if (isExpandedAll || filteredHistory.size <= 3) filteredHistory else filteredHistory.take(3)

    // 统计当前选中月份的真实战果数据
    val monthSessions = history.filter {
        val (y, m, _) = CalendarUtils.parseTimestamp(it.finishedAt ?: it.startedAt)
        y == currentYearMonth.year && m == currentYearMonth.month
    }

    val attendanceDays = monthSessions.size
    val totalTonnageKg = monthSessions.sumOf { it.sessionMetrics.totalTonnageKg.toDouble() }.toFloat()
    val totalDurationMin = monthSessions.sumOf { it.sessionMetrics.durationMinutes }
    val totalPrCount = monthSessions.sumOf { it.sessionMetrics.prBreakCount }

    val totalTonnageTons = (totalTonnageKg / 1000f * 10).toInt() / 10f
    val totalHours = (totalDurationMin / 60f * 10).toInt() / 10f

    // 收集当月打卡受训过的具体日期
    val realTrainedDays = monthSessions.map {
        val (_, _, d) = CalendarUtils.parseTimestamp(it.finishedAt ?: it.startedAt)
        d
    }.toSet()

    val defaultBenchmarkDays = if (history.isEmpty() && currentYearMonth.year == today.first && currentYearMonth.month == today.second) {
        setOf(1, 3, 5, 8, 10, 12, 15, 17, 19, 22, 24, 26, 28).filter { it <= today.third }.toSet()
    } else emptySet()
    val trainedDays = realTrainedDays + defaultBenchmarkDays

    var selectedDayNum by remember(currentYearMonth) {
        mutableStateOf(
            if (monthSessions.isNotEmpty()) {
                val (_, _, d) = CalendarUtils.parseTimestamp(monthSessions.first().finishedAt ?: monthSessions.first().startedAt)
                d
            } else if (currentYearMonth.year == today.first && currentYearMonth.month == today.second) {
                today.third
            } else {
                1
            }
        )
    }

    val selectedSession: WorkoutSession? = monthSessions.firstOrNull {
        val (_, _, d) = CalendarUtils.parseTimestamp(it.finishedAt ?: it.startedAt)
        d == selectedDayNum
    } ?: if (history.isNotEmpty() && selectedDayNum == today.third && currentYearMonth.year == today.first && currentYearMonth.month == today.second) history.firstOrNull() else null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ReshapeTheme.colors.bgCanvas)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Spacer(modifier = Modifier.height(44.dp))

                // 顶栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "‹",
                        fontSize = 26.sp,
                        color = ReshapeTheme.colors.textSecondary,
                        modifier = Modifier
                            .clickable {
                                pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("home")
                            }
                            .padding(end = 16.dp)
                    )
                    Text(
                        text = "训练打卡与历史档案",
                        fontSize = 16.sp,
                        color = ReshapeTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .background(ReshapeTheme.colors.primary, RoundedCornerShape(8.dp))
                            .clickable {
                                backfillTargetDay = selectedDayNum
                                isBackfillDrawerVisible = true
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "+ 补记训练", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                if (successNotification != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .background(Color(0xFFECFDF5), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = successNotification ?: "", fontSize = 12.sp, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // 月度 4 项核心战果卡 (带月份切换控制)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🔥 ${currentYearMonth.displayTitle}战绩汇总",
                                fontSize = 15.sp,
                                color = ReshapeTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )

                            // 上月 / 今天 / 下月 动态控制器
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                                        .clickable { currentYearMonth = currentYearMonth.previous() }
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "‹ 上月", fontSize = 10.sp, color = ReshapeTheme.colors.textPrimary)
                                }
                                Spacer(modifier = Modifier.width(3.dp))
                                if (currentYearMonth.year != today.first || currentYearMonth.month != today.second) {
                                    Box(
                                        modifier = Modifier
                                            .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(6.dp))
                                            .clickable { currentYearMonth = CalendarUtils.getCurrentYearMonth() }
                                            .padding(horizontal = 5.dp, vertical = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "今", fontSize = 10.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(3.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                                        .clickable { currentYearMonth = currentYearMonth.next() }
                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "下月 ›", fontSize = 10.sp, color = ReshapeTheme.colors.textPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "$attendanceDays 次", fontSize = 18.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.Bold)
                                Text(text = "累计出勤", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = if (totalTonnageTons > 0f) "$totalTonnageTons 吨" else "${totalTonnageKg.toInt()} kg", fontSize = 18.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                                Text(text = "总负荷", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = if (totalHours > 0f) "$totalHours h" else "$totalDurationMin 分", fontSize = 18.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                                Text(text = "训练时长", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "$totalPrCount 次", fontSize = 18.sp, color = ReshapeTheme.colors.prGold, fontWeight = FontWeight.Bold)
                                Text(text = "PR 突破", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 真实动态 7 列公历矩阵
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 星期标头
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("一", "二", "三", "四", "五", "六", "日").forEach { w ->
                                Text(
                                    text = w,
                                    fontSize = 12.sp,
                                    color = ReshapeTheme.colors.textFaint,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 真实年月公历网格
                        val daysGrid = CalendarUtils.generateMonthGrid(currentYearMonth.year, currentYearMonth.month)

                        daysGrid.forEach { week ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                week.forEach { d ->
                                    if (d == null) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    } else {
                                        val isSelected = d == selectedDayNum
                                        val isTrained = trainedDays.contains(d)
                                        val isToday = d == today.third && currentYearMonth.year == today.first && currentYearMonth.month == today.second

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .background(
                                                    when {
                                                        isSelected -> ReshapeTheme.colors.primary
                                                        isTrained -> ReshapeTheme.colors.primarySoft
                                                        isToday -> Color(0xFFF3F4F6)
                                                        else -> Color.Transparent
                                                    },
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable { selectedDayNum = d },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "$d",
                                                    fontSize = 12.sp,
                                                    color = when {
                                                        isSelected -> Color.White
                                                        isTrained -> ReshapeTheme.colors.primary
                                                        isToday -> ReshapeTheme.colors.textPrimary
                                                        else -> ReshapeTheme.colors.textPrimary
                                                    },
                                                    fontWeight = if (isSelected || isTrained || isToday) FontWeight.Bold else FontWeight.Normal
                                                )
                                                if (isTrained && !isSelected) {
                                                    Box(modifier = Modifier.width(4.dp).height(4.dp).background(ReshapeTheme.colors.primary, RoundedCornerShape(2.dp)))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 选中单日明细卡片
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    if (selectedSession != null) {
                        val planTitle = selectedSession.planContext.dayTitle
                        val dayCode = selectedSession.planContext.dayCode
                        val workingSets = selectedSession.sessionMetrics.workingSetsCount
                        val totalTonnage = selectedSession.sessionMetrics.totalTonnageKg
                        val duration = selectedSession.sessionMetrics.durationMinutes
                        val prs = selectedSession.exercises.flatMap { ex ->
                            ex.sets.filter { it.isPr }.map { "${ex.name} ${it.weightKg.toInt()}kg" }
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${currentYearMonth.year} 年 ${currentYearMonth.month} 月 ${selectedDayNum} 日 · $dayCode $planTitle",
                                    fontSize = 14.sp,
                                    color = ReshapeTheme.colors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "完整战报 ➔",
                                    fontSize = 13.sp,
                                    color = ReshapeTheme.colors.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        val params = JSONObject().apply {
                                            put("sessionId", selectedSession.sessionId)
                                        }
                                        pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("summary", params)
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "• 完成动作：${selectedSession.exercises.joinToString("、") { it.name }}\n• 训练容量：${totalTonnage.toInt()} kg 总吨位 · $workingSets 组正式组 · 耗时 $duration 分钟\n• 突破记录：${if (prs.isNotEmpty()) prs.joinToString("、") + " NEW PR 👑" else "稳步保持"}",
                                fontSize = 12.sp,
                                color = ReshapeTheme.colors.textSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${currentYearMonth.year} 年 ${currentYearMonth.month} 月 ${selectedDayNum} 日 · 休息恢复日",
                                    fontSize = 14.sp,
                                    color = ReshapeTheme.colors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "➕ 补录当日",
                                    fontSize = 12.sp,
                                    color = ReshapeTheme.colors.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        backfillTargetDay = selectedDayNum
                                        isBackfillDrawerVisible = true
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• 本日暂无做组记录。若实际已受训，可一键补录当日数据点亮日历。",
                                fontSize = 12.sp,
                                color = ReshapeTheme.colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(10.dp))
                                    .clickable {
                                        backfillTargetDay = selectedDayNum
                                        isBackfillDrawerVisible = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "➕ 补录 ${currentYearMonth.month}月${selectedDayNum}日 训练数据 ➔",
                                    fontSize = 13.sp,
                                    color = ReshapeTheme.colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 历史训练记录档案流水列表 (History Stream with Filters & Collapsible)
                if (history.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "📜 历史训练档案 (${history.size} 场)",
                                fontSize = 15.sp,
                                color = ReshapeTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            if (filteredHistory.size > 3) {
                                Text(
                                    text = if (isExpandedAll) "收起 ▴" else "展开全部 (${filteredHistory.size}) ▾",
                                    fontSize = 12.sp,
                                    color = ReshapeTheme.colors.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { isExpandedAll = !isExpandedAll }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 分化筛选 Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HistoryFilter.values().forEach { filter ->
                                val isSelected = filter == currentFilter
                                val count = when (filter) {
                                    HistoryFilter.ALL -> history.size
                                    HistoryFilter.PUSH -> history.count { it.planContext.dayCode.contains("D1", ignoreCase = true) || it.planContext.dayTitle.contains("推") }
                                    HistoryFilter.PULL -> history.count { it.planContext.dayCode.contains("D2", ignoreCase = true) || it.planContext.dayTitle.contains("拉") }
                                    HistoryFilter.LEGS -> history.count { it.planContext.dayCode.contains("D3", ignoreCase = true) || it.planContext.dayTitle.contains("腿") || it.planContext.dayTitle.contains("蹲") }
                                    HistoryFilter.PR_ONLY -> history.count { it.sessionMetrics.prBreakCount > 0 }
                                }
                                key(filter.name, isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .background(
                                                if (isSelected) ReshapeTheme.colors.primary else ReshapeTheme.colors.card,
                                                RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                currentFilter = filter
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "${filter.label} ($count)",
                                            fontSize = 11.sp,
                                            color = if (isSelected) Color.White else ReshapeTheme.colors.textSecondary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (displayedHistory.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .background(ReshapeTheme.colors.card, RoundedCornerShape(12.dp))
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无该分类训练记录",
                                    fontSize = 13.sp,
                                    color = ReshapeTheme.colors.textSecondary
                                )
                            }
                        } else {
                            displayedHistory.forEach { s ->
                                val (_, sessionMonth, sessionDay) = CalendarUtils.parseTimestamp(s.finishedAt ?: s.startedAt)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(ReshapeTheme.colors.card, RoundedCornerShape(14.dp))
                                        .clickable {
                                            val params = JSONObject().apply {
                                                put("sessionId", s.sessionId)
                                            }
                                            pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("summary", params)
                                        }
                                        .padding(14.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "${s.planContext.dayCode} ${s.planContext.dayTitle}",
                                                        fontSize = 14.sp,
                                                        color = ReshapeTheme.colors.textPrimary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "${sessionMonth}月${sessionDay}日",
                                                        fontSize = 11.sp,
                                                        color = ReshapeTheme.colors.textSecondary
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "${s.exercises.size} 个动作 · ${s.sessionMetrics.workingSetsCount} 组正式做组 · ${s.sessionMetrics.durationMinutes} 分钟",
                                                    fontSize = 12.sp,
                                                    color = ReshapeTheme.colors.textSecondary
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "${s.sessionMetrics.totalTonnageKg.toInt()} kg",
                                                    fontSize = 15.sp,
                                                    color = ReshapeTheme.colors.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (s.sessionMetrics.prBreakCount > 0) {
                                                    Text(
                                                        text = "👑 ${s.sessionMetrics.prBreakCount}项 PR",
                                                        fontSize = 11.sp,
                                                        color = ReshapeTheme.colors.prGold,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                            }

                            if (filteredHistory.size > 3) {
                                Spacer(modifier = Modifier.height(6.dp))
                                key(isExpandedAll, filteredHistory.size) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                            .background(ReshapeTheme.colors.card, RoundedCornerShape(12.dp))
                                            .clickable { isExpandedAll = !isExpandedAll },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isExpandedAll) "收起训练档案 ▴" else "查看当月全部 ${filteredHistory.size} 场训练档案 ▾",
                                            fontSize = 13.sp,
                                            color = ReshapeTheme.colors.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(90.dp))
            }
        }

        ReshapeBottomBar(
            currentTab = ReshapeNavTab.HISTORY,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // =========================================================================
        // 极速补录抽屉 (Quick Backfill Sheet)
        // =========================================================================
        if (isBackfillDrawerVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { isBackfillDrawerVisible = false }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .clickable(enabled = false) {}
                        .padding(20.dp)
                ) {
                    val templateExercises: List<Exercise> = PlanRepository.getExercises(backfillDayType, spModule)
                    val totalWorkingSets = templateExercises.sumOf { it.defaultSets }
                    val estimatedTonnage = templateExercises.sumOf { ex: Exercise ->
                        val w = if (ex.targetWeightKg > 0f) ex.targetWeightKg else 40f
                        val r = if (ex.targetRepsMin > 0) ex.targetRepsMin else 10
                        (ex.defaultSets * w * r).toDouble()
                    }.toFloat()

                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 顶栏标题与关闭
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "📝 极速补录训练",
                                fontSize = 16.sp,
                                color = ReshapeTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "✕",
                                fontSize = 18.sp,
                                color = ReshapeTheme.colors.textSecondary,
                                modifier = Modifier
                                    .clickable { isBackfillDrawerVisible = false }
                                    .padding(start = 12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 1. 目标日期快速微调
                        Text(text = "1. 选择受训日期 (${currentYearMonth.year}年${currentYearMonth.month}月)", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                (backfillTargetDay - 2).coerceAtLeast(1),
                                (backfillTargetDay - 1).coerceAtLeast(1),
                                backfillTargetDay,
                                (backfillTargetDay + 1).coerceAtMost(31),
                                (backfillTargetDay + 2).coerceAtMost(31)
                            ).distinct().forEach { d ->
                                val isSelected = d == backfillTargetDay
                                key(d, isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .background(
                                                if (isSelected) ReshapeTheme.colors.primary else ReshapeTheme.colors.bgCanvas,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { backfillTargetDay = d }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "${currentYearMonth.month}月${d}日",
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else ReshapeTheme.colors.textPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. 选择训练分化
                        Text(text = "2. 选择训练分化", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf(
                                DayType.PUSH to "D1 推 (Push)",
                                DayType.PULL to "D2 拉 (Pull)",
                                DayType.LEGS to "D3 蹲 (Legs)"
                            ).forEach { (dt, label) ->
                                val isSelected = dt == backfillDayType
                                key(dt.name, isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 4.dp)
                                            .height(40.dp)
                                            .background(
                                                if (isSelected) ReshapeTheme.colors.primary else ReshapeTheme.colors.bgCanvas,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { backfillDayType = dt },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else ReshapeTheme.colors.textPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3. 预设动作与容量明细预览
                        Text(text = "3. 预设计划做组预览 (标准模板)", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                for ((idx, ex) in templateExercises.take(5).withIndex()) {
                                    val weight = if (ex.targetWeightKg > 0f) ex.targetWeightKg else 40f
                                    val reps = if (ex.targetRepsMin > 0) ex.targetRepsMin else 10
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "${idx + 1}. ${ex.name}", fontSize = 12.sp, color = ReshapeTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                                        Text(text = "${ex.defaultSets}组 · ${weight.toInt()}kg × ${reps}次", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE5E7EB)))
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "💡 预估总计：${templateExercises.size} 个动作 · $totalWorkingSets 组正式组 · ~${estimatedTonnage.toInt()} kg 总负荷 · ~50 分钟",
                                    fontSize = 11.sp,
                                    color = ReshapeTheme.colors.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 4. 立即确认补录 CTA
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(ReshapeTheme.colors.primary, RoundedCornerShape(12.dp))
                                .clickable {
                                    val profile = UserPreferencesStore.getProfile(spModule)
                                    val targetMillis = CalendarUtils.toTimestampMillis(currentYearMonth.year, currentYearMonth.month, backfillTargetDay)
                                    val durationMin = 50

                                    val sessionExercises = templateExercises.map { ex: Exercise ->
                                        val weight = if (ex.targetWeightKg > 0f) ex.targetWeightKg else 40f
                                        val reps = if (ex.targetRepsMin > 0) ex.targetRepsMin else 10
                                        val sets = (1..ex.defaultSets).map { sNo ->
                                            SessionSet(
                                                setNo = sNo,
                                                isWarmup = false,
                                                weightKg = weight,
                                                leftReps = if (ex.isUnilateral) reps else null,
                                                rightReps = if (ex.isUnilateral) reps else null,
                                                effectiveReps = reps,
                                                rpe = if (ex.targetRpe > 0f) ex.targetRpe else 8.0f,
                                                isPr = false,
                                                restSeconds = ex.defaultRestSeconds,
                                                completedAt = targetMillis
                                            )
                                        }
                                        SessionExercise(
                                            exerciseId = ex.id,
                                            name = ex.name,
                                            category = ex.primaryMuscle,
                                            isUnilateral = ex.isUnilateral,
                                            memo = "补录归档",
                                            sets = sets
                                        )
                                    }

                                    val newSession = WorkoutSession(
                                        sessionId = "backfill_${backfillDayType.name}_${backfillTargetDay}_${platformCurrentTimeMillis()}",
                                        userProfile = SessionUserProfile(
                                            bodyWeightKg = profile.bodyWeightKg,
                                            gender = if (profile.gender == Gender.FEMALE) "female" else "male",
                                            trainingExpYears = profile.trainingExpYears,
                                            bench1RmKg = profile.bench1RmKg,
                                            squat1RmKg = profile.squat1RmKg,
                                            deadlift1RmKg = profile.deadlift1RmKg
                                        ),
                                        planContext = SessionPlanContext(
                                            dayCode = backfillDayType.name,
                                            dayTitle = "${backfillDayType.title}训练 (补录)",
                                            readinessState = "full",
                                            isDeloadWeek = false,
                                            isRehabProtected = false
                                        ),
                                        sessionMetrics = SessionMetrics(
                                            totalTonnageKg = estimatedTonnage,
                                            workingSetsCount = totalWorkingSets,
                                            durationMinutes = durationMin,
                                            prBreakCount = 0,
                                            avgRestSeconds = 90
                                        ),
                                        exercises = sessionExercises,
                                        startedAt = targetMillis - durationMin * 60 * 1000L,
                                        finishedAt = targetMillis,
                                        isDraft = false
                                    )

                                    WorkoutHistoryStore.archiveSession(newSession, spModule)
                                    TrainingCycleStore.advanceCycleSession(spModule)
                                    history = WorkoutHistoryStore.getHistory(spModule)
                                    selectedDayNum = backfillTargetDay
                                    successNotification = "${currentYearMonth.month}月${backfillTargetDay}日 训练已成功补录并点亮日历 ✓"
                                    isBackfillDrawerVisible = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "⚡ 立即补录入库并点亮日历",
                                fontSize = 15.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}
