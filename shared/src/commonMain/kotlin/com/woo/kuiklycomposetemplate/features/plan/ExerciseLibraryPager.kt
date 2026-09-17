package com.woo.kuiklycomposetemplate.features.plan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.tencent.kuikly.compose.foundation.layout.fillMaxHeight
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TextField
import com.tencent.kuikly.compose.material3.TextFieldDefaults
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.pager.Pager
import com.woo.kuiklycomposetemplate.core.theme.ReshapeTheme
import com.woo.kuiklycomposetemplate.data.local.ExerciseLibraryRepository
import com.woo.kuiklycomposetemplate.data.local.PlanRepository
import com.woo.kuiklycomposetemplate.domain.model.DayType
import com.woo.kuiklycomposetemplate.domain.model.Exercise
import com.woo.kuiklycomposetemplate.domain.model.JointType
import com.woo.kuiklycomposetemplate.domain.model.LoadType
import com.woo.kuiklycomposetemplate.domain.model.MuscleTaxonomy
import com.woo.kuiklycomposetemplate.domain.model.PresetExercises

/**
 * 全量动作库管理与挑选页 (支持检索、分类、主辅肌群选择、预制/自定义动作个性化编辑与无痛变式图谱)
 */
@Page("exercise_library", supportInLocal = true)
class ExerciseLibraryPager : ComposeContainer() {

    private var initialDayType: DayType? = null
    private var initialAutoCreate: Boolean = false
    private var initialEditExerciseId: String? = null

    override fun willInit() {
        super.willInit()
        try {
            val params = pageData.params
            val dayStr = params.optString("dayCode", "")
            if (dayStr.isNotBlank()) {
                initialDayType = when (dayStr.uppercase()) {
                    "PUSH", "D1" -> DayType.PUSH
                    "PULL", "D2" -> DayType.PULL
                    "LEGS", "D3" -> DayType.LEGS
                    else -> null
                }
            }
            initialAutoCreate = params.optBoolean("autoCreate", false)
            val editId = params.optString("exerciseId", "")
            if (editId.isNotBlank() && editId != "new") {
                initialEditExerciseId = editId
            }
        } catch (_: Exception) {}

        setContent {
            ExerciseLibraryScreen(
                targetDayType = initialDayType,
                autoOpenCreateDialog = initialAutoCreate,
                autoOpenEditExerciseId = initialEditExerciseId
            )
        }
    }
}

@Composable
fun ExerciseLibraryScreen(
    targetDayType: DayType? = null,
    autoOpenCreateDialog: Boolean = false,
    autoOpenEditExerciseId: String? = null,
    onSelectExercise: ((Exercise) -> Unit)? = null
) {
    val pager = LocalActivity.current.getPager() as? Pager
    val spModule = pager?.acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)

    var searchQuery by remember { mutableStateOf("") }
    var selectedDayFilter by remember { mutableStateOf(targetDayType) }
    var selectedLoadFilter by remember { mutableStateOf<LoadType?>(null) }
    var showDialog by remember { mutableStateOf(autoOpenCreateDialog || autoOpenEditExerciseId != null) }
    var editingExerciseId by remember { mutableStateOf(autoOpenEditExerciseId) }
    var refreshKey by remember { mutableStateOf(0) }

    // 表单状态 (使用不可变 List + mutableStateOf 确保 Compose 响应式重组)
    var formName by remember { mutableStateOf("") }
    var formDay by remember { mutableStateOf(targetDayType ?: DayType.PUSH) }
    var formPrimaryMuscle by remember {
        mutableStateOf(
            when (formDay) {
                DayType.PUSH -> "胸大肌中下束"
                DayType.PULL -> "背阔肌 (宽度)"
                DayType.LEGS -> "股四头肌 (前侧)"
                DayType.REST -> "胸大肌中下束"
            }
        )
    }
    var formSecondaryMuscles by remember { mutableStateOf<List<String>>(emptyList()) }
    var formLoad by remember { mutableStateOf(LoadType.DUMBBELL) }
    var formJoint by remember { mutableStateOf(JointType.SHOULDER) }
    var formCues by remember { mutableStateOf("") }
    var formSets by remember { mutableStateOf("4") }
    var formRepsMin by remember { mutableStateOf("8") }
    var formRepsMax by remember { mutableStateOf("10") }
    var formRestSeconds by remember { mutableStateOf("120") }
    var formInterchangeableIds by remember { mutableStateOf<List<String>>(emptyList()) }

    // 初始化时若携带 editId，自动装载该动作数据
    LaunchedEffect(autoOpenEditExerciseId) {
        if (!autoOpenEditExerciseId.isNullOrBlank()) {
            val ex = ExerciseLibraryRepository.findById(autoOpenEditExerciseId, spModule)
            if (ex != null) {
                formName = ex.name
                formDay = ex.dayType
                formPrimaryMuscle = ex.primaryMuscle
                formSecondaryMuscles = ex.secondaryMuscles.toList()
                formLoad = ex.loadType
                formJoint = ex.primaryJoint
                formCues = ex.defaultHoleCues
                formSets = ex.defaultSets.toString()
                formRepsMin = ex.targetRepsMin.toString()
                formRepsMax = ex.targetRepsMax.toString()
                formRestSeconds = ex.defaultRestSeconds.toString()
                formInterchangeableIds = ex.interchangeableExerciseIds.toList()
                editingExerciseId = ex.id
                showDialog = true
            }
        }
    }

    // 动作列表 (注入 spModule 保证持久化动作加载)
    val allExercises = remember(searchQuery, selectedDayFilter, selectedLoadFilter, showDialog, refreshKey) {
        var list = ExerciseLibraryRepository.searchExercises(searchQuery, spModule)
        if (selectedDayFilter != null) {
            list = list.filter { it.dayType == selectedDayFilter }
        }
        if (selectedLoadFilter != null) {
            list = list.filter { it.loadType == selectedLoadFilter }
        }
        list
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ReshapeTheme.colors.bgCanvas)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(40.dp))

                // 顶部导航栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(ReshapeTheme.colors.card, RoundedCornerShape(10.dp))
                            .clickable {
                                pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.closePage()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = "← 返回", fontSize = 14.sp, color = ReshapeTheme.colors.primary)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "📚 动作库",
                        fontSize = 18.sp,
                        color = ReshapeTheme.colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .background(ReshapeTheme.colors.primary, RoundedCornerShape(10.dp))
                            .clickable {
                                editingExerciseId = null
                                formName = ""
                                formDay = selectedDayFilter ?: DayType.PUSH
                                formPrimaryMuscle = when (formDay) {
                                    DayType.PUSH -> "胸大肌中下束"
                                    DayType.PULL -> "背阔肌 (宽度)"
                                    DayType.LEGS -> "股四头肌 (前侧)"
                                    DayType.REST -> "胸大肌中下束"
                                }
                                formSecondaryMuscles = emptyList()
                                formLoad = LoadType.DUMBBELL
                                formJoint = JointType.SHOULDER
                                formCues = ""
                                formSets = "4"
                                formRepsMin = "8"
                                formRepsMax = "10"
                                formRestSeconds = "120"
                                formInterchangeableIds = emptyList()
                                showDialog = true
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = "+ 新增动作", fontSize = 13.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 搜索框
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("🔍 搜索动作 / 主辅肌群 / 器械...", fontSize = 13.sp, color = ReshapeTheme.colors.textFaint) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 训练日 Tabs 过滤
                Row(modifier = Modifier.fillMaxWidth()) {
                    val days = listOf(null to "全部日", DayType.PUSH to "推 (D1)", DayType.PULL to "拉 (D2)", DayType.LEGS to "腿 (D3)")
                    days.forEachIndexed { index, (day, label) ->
                        if (index > 0) Spacer(modifier = Modifier.width(8.dp))
                        val isSelected = selectedDayFilter == day
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) ReshapeTheme.colors.primary else ReshapeTheme.colors.card,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedDayFilter = day }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                color = if (isSelected) Color.White else ReshapeTheme.colors.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 器械类型过滤
                Row(modifier = Modifier.fillMaxWidth()) {
                    val loads = listOf(null to "全部", LoadType.BARBELL to "杠铃", LoadType.DUMBBELL to "哑铃", LoadType.MACHINE to "器械", LoadType.CABLE to "绳索", LoadType.BODYWEIGHT to "自重")
                    loads.forEachIndexed { index, (load, label) ->
                        if (index > 0) Spacer(modifier = Modifier.width(4.dp))
                        val isSelected = selectedLoadFilter == load
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) ReshapeTheme.colors.primarySoft else ReshapeTheme.colors.card,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedLoadFilter = load }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = if (isSelected) ReshapeTheme.colors.primary else ReshapeTheme.colors.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "共收录 ${allExercises.size} 个动作",
                    fontSize = 12.sp,
                    color = ReshapeTheme.colors.textFaint
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            items(allExercises) { ex ->
                val interchangeables = ExerciseLibraryRepository.getInterchangeableExercises(ex.id, spModule)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // ① 顶部行：动作名称 + 属性徽章 (器械 + 受力关节)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ex.name,
                                fontSize = 16.sp,
                                color = ReshapeTheme.colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )

                            if (ex.isCustom) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFEF3C7), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = "自定义", fontSize = 10.sp, color = Color(0xFFD97706))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = ex.loadType.label, fontSize = 10.sp, color = ReshapeTheme.colors.primary)
                            }

                            if (ex.primaryJoint != JointType.NONE) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFF3F4F6), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${ex.primaryJoint.icon} ${ex.primaryJoint.label}",
                                        fontSize = 10.sp,
                                        color = ReshapeTheme.colors.textSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // ② 肌群行：主发力肌群 + 辅助协同肌群（独立横向标签）
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFEFF6FF), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "🎯 主：${ex.primaryMuscle}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF1D4ED8)
                                )
                            }

                            if (ex.secondaryMuscles.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFF9FAFB), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "🧬 辅：${ex.secondaryMuscles.joinToString(" · ")}",
                                        fontSize = 11.sp,
                                        color = ReshapeTheme.colors.textSecondary
                                    )
                                }
                            }
                        }

                        // ③ 动作可替换图谱关系
                        if (interchangeables.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Text(text = "🔄 可替换：", fontSize = 11.sp, color = ReshapeTheme.colors.primary)
                                    Text(
                                        text = interchangeables.take(3).joinToString(" · ") { it.name },
                                        fontSize = 11.sp,
                                        color = ReshapeTheme.colors.textSecondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // ④ 孔位与要领口诀
                        if (ex.defaultHoleCues.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(text = "💡 ", fontSize = 12.sp)
                                Text(
                                    text = ex.defaultHoleCues,
                                    fontSize = 12.sp,
                                    color = ReshapeTheme.colors.textSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF3F4F6)))
                        Spacer(modifier = Modifier.height(10.dp))

                        // ⑤ 底部操作行：建议参数 + [✏️ 编辑] + [选用此动作 +]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${ex.defaultSets}组 · ${ex.targetRepsMin}~${ex.targetRepsMax}次 · 休息${ex.defaultRestSeconds}s",
                                fontSize = 11.sp,
                                color = ReshapeTheme.colors.textFaint,
                                modifier = Modifier.weight(1f)
                            )

                            // ✏️ 编辑按钮 (预制动作 & 自定义动作均可编辑个性化口诀与完整参数)
                            Box(
                                modifier = Modifier
                                    .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(8.dp))
                                    .clickable {
                                        editingExerciseId = ex.id
                                        formName = ex.name
                                        formDay = ex.dayType
                                        formPrimaryMuscle = ex.primaryMuscle
                                        formSecondaryMuscles = ex.secondaryMuscles.toList()
                                        formLoad = ex.loadType
                                        formJoint = ex.primaryJoint
                                        formCues = ex.defaultHoleCues
                                        formSets = ex.defaultSets.toString()
                                        formRepsMin = ex.targetRepsMin.toString()
                                        formRepsMax = ex.targetRepsMax.toString()
                                        formRestSeconds = ex.defaultRestSeconds.toString()
                                        formInterchangeableIds = ex.interchangeableExerciseIds.toList()
                                        showDialog = true
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = "✏️ 编辑", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .background(ReshapeTheme.colors.primary, RoundedCornerShape(8.dp))
                                    .clickable {
                                        PlanRepository.addExercise(ex.dayType, ex, spModule)
                                        if (onSelectExercise != null) {
                                            onSelectExercise(ex)
                                        } else {
                                            pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("plan_edit")
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = "选用此动作 +", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // 动作编辑/新增弹窗模态框 (纯净 Box 居中对齐，杜绝文本重组位移)
        if (showDialog) {
            val availablePrimaryMuscles = when (formDay) {
                DayType.PUSH -> MuscleTaxonomy.PUSH_PRIMARY_MUSCLES
                DayType.PULL -> MuscleTaxonomy.PULL_PRIMARY_MUSCLES
                DayType.LEGS -> MuscleTaxonomy.LEGS_PRIMARY_MUSCLES
                DayType.REST -> MuscleTaxonomy.PUSH_PRIMARY_MUSCLES
            }
            val sameDayExercises = ExerciseLibraryRepository.getExercisesByDay(formDay, spModule).filter { it.id != editingExerciseId }
            val isEditingPreset = editingExerciseId != null && PresetExercises.ALL_PRESETS.any { it.id == editingExerciseId }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000))
                    .clickable { showDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.85f)
                        .background(Color.White, RoundedCornerShape(18.dp))
                        .clickable(enabled = false) {}
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (editingExerciseId != null) "✏️ 编辑动作配置与口诀" else "➕ 新增自定义动作",
                                    fontSize = 16.sp,
                                    color = ReshapeTheme.colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "✕",
                                    fontSize = 16.sp,
                                    color = ReshapeTheme.colors.textFaint,
                                    modifier = Modifier.clickable { showDialog = false }
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 动作名称
                            Text(text = "动作名称", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp)
                            ) {
                                TextField(
                                    value = formName,
                                    onValueChange = { formName = it },
                                    placeholder = { Text("例如：反手窄握平板卧推", fontSize = 13.sp, color = ReshapeTheme.colors.textFaint) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 归属训练日
                            Text(text = "归属训练日", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                listOf(DayType.PUSH to "推 (D1)", DayType.PULL to "拉 (D2)", DayType.LEGS to "腿 (D3)").forEachIndexed { index, (d, label) ->
                                    if (index > 0) Spacer(modifier = Modifier.width(6.dp))
                                    val isSelected = formDay == d
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp)
                                            .background(
                                                if (isSelected) ReshapeTheme.colors.primary else Color(0xFFF3F4F6),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable {
                                                formDay = d
                                                formPrimaryMuscle = when (d) {
                                                    DayType.PUSH -> "胸大肌中下束"
                                                    DayType.PULL -> "背阔肌 (宽度)"
                                                    DayType.LEGS -> "股四头肌 (前侧)"
                                                    DayType.REST -> "胸大肌中下束"
                                                }
                                                formInterchangeableIds = emptyList()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            color = if (isSelected) Color.White else ReshapeTheme.colors.textSecondary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 1. 主发力肌群 (单选 Chips)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🎯 主发力肌群 (单选)", fontSize = 12.sp, color = ReshapeTheme.colors.textPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = formPrimaryMuscle, fontSize = 11.sp, color = ReshapeTheme.colors.primary)
                            }
                            Spacer(modifier = Modifier.height(5.dp))

                            Column {
                                availablePrimaryMuscles.chunked(3).forEach { rowMuscles ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        rowMuscles.forEachIndexed { idx, m ->
                                            if (idx > 0) Spacer(modifier = Modifier.width(6.dp))
                                            val isSelected = formPrimaryMuscle == m
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(34.dp)
                                                    .background(
                                                        if (isSelected) ReshapeTheme.colors.primary else Color(0xFFF3F4F6),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .clickable { formPrimaryMuscle = m },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = m,
                                                    fontSize = 11.sp,
                                                    color = if (isSelected) Color.White else ReshapeTheme.colors.textSecondary
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(5.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 2. 辅助协同肌群 (多选 Chips - 彻底杜绝文字位移与重影)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🧬 辅助协同肌群 (多选)", fontSize = 12.sp, color = ReshapeTheme.colors.textPrimary)
                                if (formSecondaryMuscles.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "已选 ${formSecondaryMuscles.size} 项", fontSize = 11.sp, color = Color(0xFF1D4ED8))
                                }
                            }
                            Spacer(modifier = Modifier.height(5.dp))

                            Column {
                                MuscleTaxonomy.ALL_SECONDARY_MUSCLES.chunked(3).forEach { secRow ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        secRow.forEachIndexed { idx, sm ->
                                            if (idx > 0) Spacer(modifier = Modifier.width(6.dp))
                                            key(sm) {
                                                val isSelected = formSecondaryMuscles.contains(sm)
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(34.dp)
                                                        .background(
                                                            if (isSelected) Color(0xFFDBEAFE) else Color(0xFFF3F4F6),
                                                            RoundedCornerShape(6.dp)
                                                        )
                                                        .clickable {
                                                            formSecondaryMuscles = if (isSelected) {
                                                                formSecondaryMuscles - sm
                                                            } else {
                                                                formSecondaryMuscles + sm
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = sm,
                                                        fontSize = 11.sp,
                                                        color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF4B5563)
                                                    )
                                                }
                                            }
                                        }
                                        if (secRow.size < 3) {
                                            for (k in 0 until (3 - secRow.size)) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(5.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 3. 可替换动作关联 (多选 Chips - 彻底杜绝文字位移与重影)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🔄 可作为哪些动作的替代 (多选)", fontSize = 12.sp, color = ReshapeTheme.colors.textPrimary)
                                if (formInterchangeableIds.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "已关联 ${formInterchangeableIds.size} 个", fontSize = 11.sp, color = Color(0xFFD97706))
                                }
                            }
                            Spacer(modifier = Modifier.height(5.dp))

                            Column {
                                sameDayExercises.take(6).chunked(2).forEach { pair ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        pair.forEachIndexed { idx, ex ->
                                            if (idx > 0) Spacer(modifier = Modifier.width(6.dp))
                                            key(ex.id) {
                                                val isSelected = formInterchangeableIds.contains(ex.id)
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(36.dp)
                                                        .background(
                                                            if (isSelected) Color(0xFFFEF3C7) else Color(0xFFF3F4F6),
                                                            RoundedCornerShape(6.dp)
                                                        )
                                                        .clickable {
                                                            formInterchangeableIds = if (isSelected) {
                                                                formInterchangeableIds - ex.id
                                                            } else {
                                                                formInterchangeableIds + ex.id
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = ex.name,
                                                        fontSize = 11.sp,
                                                        color = if (isSelected) Color(0xFFB45309) else Color(0xFF4B5563)
                                                    )
                                                }
                                            }
                                        }
                                        if (pair.size < 2) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(modifier = Modifier.weight(1f))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(5.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 4. 负重器械类型 (完整5项：杠铃、哑铃、器械、绳索、自重)
                            Text(text = "负重器械类型", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                val loadOptions = listOf(
                                    LoadType.BARBELL to "杠铃",
                                    LoadType.DUMBBELL to "哑铃",
                                    LoadType.MACHINE to "器械",
                                    LoadType.CABLE to "绳索",
                                    LoadType.BODYWEIGHT to "自重"
                                )
                                loadOptions.forEachIndexed { idx, (l, label) ->
                                    if (idx > 0) Spacer(modifier = Modifier.width(4.dp))
                                    val isSelected = formLoad == l
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp)
                                            .background(
                                                if (isSelected) ReshapeTheme.colors.primary else Color(0xFFF3F4F6),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { formLoad = l },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            color = if (isSelected) Color.White else ReshapeTheme.colors.textSecondary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 5. 动作默认建议参数 (作为基准参考，可在计划编排中单独定制)
                            Column {
                                Text(text = "5. 动作默认建议参数 (可在计划编排中单独微调)", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "💡 作为通用基准参考值，加入具体计划日后可独立调整做组与间歇", fontSize = 10.sp, color = ReshapeTheme.colors.textFaint)
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "建议组数", fontSize = 11.sp, color = ReshapeTheme.colors.textFaint)
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        listOf("3", "4", "5").forEachIndexed { idx, s ->
                                            if (idx > 0) Spacer(modifier = Modifier.width(3.dp))
                                            val isSelected = formSets == s
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(30.dp)
                                                    .background(
                                                        if (isSelected) ReshapeTheme.colors.primary else Color(0xFFF3F4F6),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .clickable { formSets = s },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${s}组",
                                                    fontSize = 11.sp,
                                                    color = if (isSelected) Color.White else ReshapeTheme.colors.textSecondary
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1.3f)) {
                                    Text(text = "组间休息", fontSize = 11.sp, color = ReshapeTheme.colors.textFaint)
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        listOf("60" to "60s", "90" to "90s", "120" to "120s", "180" to "180s").forEachIndexed { idx, (sec, label) ->
                                            if (idx > 0) Spacer(modifier = Modifier.width(3.dp))
                                            val isSelected = formRestSeconds == sec
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(30.dp)
                                                    .background(
                                                        if (isSelected) ReshapeTheme.colors.primary else Color(0xFFF3F4F6),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .clickable { formRestSeconds = sec },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 10.sp,
                                                    color = if (isSelected) Color.White else ReshapeTheme.colors.textSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 目标次数区间
                            Text(text = "目标次数区间", fontSize = 11.sp, color = ReshapeTheme.colors.textFaint)
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                listOf("3~5", "6~8", "8~10", "10~12", "12~15").forEachIndexed { idx, range ->
                                    if (idx > 0) Spacer(modifier = Modifier.width(3.dp))
                                    val parts = range.split("~")
                                    val isSelected = formRepsMin == parts[0] && formRepsMax == parts[1]
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(30.dp)
                                            .background(
                                                if (isSelected) ReshapeTheme.colors.primary else Color(0xFFF3F4F6),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable {
                                                formRepsMin = parts[0]
                                                formRepsMax = parts[1]
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${range}次",
                                            fontSize = 10.sp,
                                            color = if (isSelected) Color.White else ReshapeTheme.colors.textSecondary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 6. 孔位口诀 (支持个性化编辑)
                            Text(text = "💡 个性化孔位口诀 / 动作要领", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp)
                            ) {
                                TextField(
                                    value = formCues,
                                    onValueChange = { formCues = it },
                                    placeholder = { Text("例如：平凳对准 3 孔 · 肩胛骨下沉 · 肘内收 45 度", fontSize = 12.sp, color = ReshapeTheme.colors.textFaint) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 底部操作区
                            Row(modifier = Modifier.fillMaxWidth()) {
                                if (isEditingPreset) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .background(Color(0xFFF3F4F6), RoundedCornerShape(10.dp))
                                            .clickable {
                                                editingExerciseId?.let { ExerciseLibraryRepository.resetExerciseToPreset(it, spModule) }
                                                showDialog = false
                                                refreshKey++
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "恢复官方默认", fontSize = 13.sp, color = ReshapeTheme.colors.textSecondary)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .background(
                                            if (formName.isNotBlank()) ReshapeTheme.colors.primary else Color.Gray,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable(enabled = formName.isNotBlank()) {
                                            val baseExercise = editingExerciseId?.let { id ->
                                                ExerciseLibraryRepository.findById(id, spModule)
                                            }
                                            val targetSummary = if (formSecondaryMuscles.isEmpty()) formPrimaryMuscle else "$formPrimaryMuscle / ${formSecondaryMuscles.joinToString(" / ")}"
                                            val exId = editingExerciseId ?: "custom_${formName.hashCode().coerceAtLeast(0)}_${kotlin.random.Random.nextInt(1000, 9999)}"
                                            val updated = Exercise(
                                                id = exId,
                                                name = formName.trim(),
                                                dayType = formDay,
                                                primaryMuscle = formPrimaryMuscle,
                                                secondaryMuscles = formSecondaryMuscles,
                                                targetMuscle = targetSummary,
                                                loadType = formLoad,
                                                primaryJoint = formJoint,
                                                isCustom = baseExercise?.isCustom ?: true,
                                                isUnilateral = baseExercise?.isUnilateral ?: false,
                                                defaultHoleCues = formCues.trim(),
                                                defaultSets = formSets.toIntOrNull() ?: 4,
                                                targetRepsMin = formRepsMin.toIntOrNull() ?: 8,
                                                targetRepsMax = formRepsMax.toIntOrNull() ?: 10,
                                                targetRepsSequence = baseExercise?.targetRepsSequence ?: "",
                                                targetWeightKg = baseExercise?.targetWeightKg ?: 40.0f,
                                                waveWeightsSequence = baseExercise?.waveWeightsSequence ?: "",
                                                targetRpe = baseExercise?.targetRpe ?: 8.0f,
                                                defaultRestSeconds = formRestSeconds.toIntOrNull() ?: 120,
                                                progressionType = baseExercise?.progressionType ?: if ((baseExercise?.isUnilateral ?: false)) {
                                                    com.woo.kuiklycomposetemplate.domain.model.ProgressionType.UNILATERAL_BALANCE
                                                } else {
                                                    com.woo.kuiklycomposetemplate.domain.model.ProgressionType.DOUBLE_PROGRESSION
                                                },
                                                related1RmKey = baseExercise?.related1RmKey,
                                                ratioOf1Rm = baseExercise?.ratioOf1Rm,
                                                painFreeVariantIds = baseExercise?.painFreeVariantIds ?: emptyList(),
                                                interchangeableExerciseIds = formInterchangeableIds,
                                                enableWarmupLadder = baseExercise?.enableWarmupLadder ?: true
                                            )
                                            ExerciseLibraryRepository.saveOrUpdateExercise(updated, spModule)
                                            // 若来自计划编排且属于新增动作，则同步加入当前计划
                                            if (targetDayType != null || autoOpenCreateDialog) {
                                                PlanRepository.addExercise(formDay, updated, spModule)
                                            }
                                            showDialog = false
                                            refreshKey++
                                            // 如果是外部跳转进来的快捷新建，保存后平滑返回计划编排
                                            if (autoOpenCreateDialog || !autoOpenEditExerciseId.isNullOrBlank()) {
                                                pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("plan_edit")
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (editingExerciseId != null) "保存修改" else "保存并加入计划",
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}
