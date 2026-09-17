package com.woo.kuiklycomposetemplate.features.plan

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
import com.woo.kuiklycomposetemplate.data.local.PlanRepository
import com.woo.kuiklycomposetemplate.domain.model.DayType
import com.woo.kuiklycomposetemplate.domain.model.Exercise
import com.woo.kuiklycomposetemplate.domain.model.ProgressionType

@Page("exercise_edit", supportInLocal = true)
class ExerciseEditPager : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ReshapeTheme {
                ExerciseEditScreen()
            }
        }
    }
}

@Composable
fun ExerciseEditScreen() {
    val pager = LocalActivity.current.getPager() as? Pager
    val spModule = pager?.acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)

    var exerciseName by remember { mutableStateOf("杠铃卧推") }
    var targetMuscle by remember { mutableStateOf("胸大肌") }
    var loadType by remember { mutableStateOf("杠铃") }
    var setsCount by remember { mutableStateOf(4) }
    var repsMin by remember { mutableStateOf(8) }
    var repsMax by remember { mutableStateOf(10) }
    var targetRpe by remember { mutableStateOf(8.0f) }
    var restSeconds by remember { mutableStateOf(120) }
    var progressionType by remember { mutableStateOf(ProgressionType.DOUBLE_PROGRESSION) }
    var related1RmKey by remember { mutableStateOf("bench") }
    var ratioOf1Rm by remember { mutableStateOf(0.7f) }
    var holeCues by remember { mutableStateOf("座椅 4 孔 / 靠背 3 孔") }
    var isPermanentSave by remember { mutableStateOf(true) }

    val muscleOptions = listOf("胸大肌", "三角肌中后束", "背阔肌", "股四头肌", "臀大肌/腘绳肌", "手臂核心")
    val loadOptions = listOf("杠铃", "哑铃/只", "固定器械", "绳索", "自重", "自重+配重")
    val restOptions = listOf(60, 90, 120, 180)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ReshapeTheme.colors.bgCanvas)
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
                    text = "✕",
                    fontSize = 20.sp,
                    color = ReshapeTheme.colors.textSecondary,
                    modifier = Modifier
                        .clickable {
                            pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("plan_edit")
                        }
                        .padding(end = 16.dp)
                )
                Text(
                    text = "动作详细配置",
                    fontSize = 16.sp,
                    color = ReshapeTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "保存",
                    fontSize = 15.sp,
                    color = ReshapeTheme.colors.primary,
                    modifier = Modifier.clickable {
                        val currentDay = PlanRepository.getCurrentDay(spModule)
                        val newEx = Exercise(
                            id = "ex_${exerciseName.hashCode()}",
                            name = exerciseName,
                            dayType = currentDay,
                            targetMuscle = targetMuscle,
                            loadType = when (loadType) {
                                "杠铃" -> com.woo.kuiklycomposetemplate.domain.model.LoadType.BARBELL
                                "哑铃/只" -> com.woo.kuiklycomposetemplate.domain.model.LoadType.DUMBBELL
                                "固定器械" -> com.woo.kuiklycomposetemplate.domain.model.LoadType.MACHINE
                                "绳索" -> com.woo.kuiklycomposetemplate.domain.model.LoadType.CABLE
                                else -> com.woo.kuiklycomposetemplate.domain.model.LoadType.BODYWEIGHT
                            },
                            primaryJoint = com.woo.kuiklycomposetemplate.domain.model.JointType.NONE,
                            isCustom = true,
                            defaultSets = setsCount,
                            targetRepsMin = repsMin,
                            targetRepsMax = repsMax,
                            targetRpe = targetRpe,
                            defaultRestSeconds = restSeconds,
                            progressionType = progressionType,
                            related1RmKey = related1RmKey,
                            ratioOf1Rm = ratioOf1Rm,
                            defaultHoleCues = holeCues
                        )
                        if (isPermanentSave) {
                            val list = PlanRepository.getExercises(currentDay, spModule).toMutableList()
                            val idx = list.indexOfFirst { it.name == exerciseName }
                            if (idx >= 0) {
                                list[idx] = newEx
                            } else {
                                list.add(newEx)
                            }
                            PlanRepository.saveCustomPlan(currentDay, list, spModule)
                        }
                        pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("plan_edit")
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. 动作名称与训练部位
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "动作名称", fontSize = 13.sp, color = ReshapeTheme.colors.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    TextField(
                        value = exerciseName,
                        onValueChange = { exerciseName = it },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ReshapeTheme.colors.bgCanvas,
                            unfocusedContainerColor = ReshapeTheme.colors.bgCanvas,
                            focusedTextColor = ReshapeTheme.colors.textPrimary,
                            unfocusedTextColor = ReshapeTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "目标肌群", fontSize = 13.sp, color = ReshapeTheme.colors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        muscleOptions.take(3).forEachIndexed { i, m ->
                            if (i > 0) Spacer(modifier = Modifier.width(6.dp))
                            val isSelected = m == targetMuscle
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) ReshapeTheme.colors.primarySoft else ReshapeTheme.colors.bgCanvas,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { targetMuscle = m }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = m, fontSize = 12.sp, color = if (isSelected) ReshapeTheme.colors.primary else ReshapeTheme.colors.textSecondary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        muscleOptions.drop(3).forEachIndexed { i, m ->
                            if (i > 0) Spacer(modifier = Modifier.width(6.dp))
                            val isSelected = m == targetMuscle
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) ReshapeTheme.colors.primarySoft else ReshapeTheme.colors.bgCanvas,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { targetMuscle = m }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = m, fontSize = 12.sp, color = if (isSelected) ReshapeTheme.colors.primary else ReshapeTheme.colors.textSecondary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. 负重形式与组数次数
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "负重形式", fontSize = 13.sp, color = ReshapeTheme.colors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        loadOptions.take(3).forEachIndexed { i, l ->
                            if (i > 0) Spacer(modifier = Modifier.width(6.dp))
                            val isSelected = l == loadType
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) ReshapeTheme.colors.primarySoft else ReshapeTheme.colors.bgCanvas, RoundedCornerShape(8.dp))
                                    .clickable { loadType = l }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = l, fontSize = 12.sp, color = if (isSelected) ReshapeTheme.colors.primary else ReshapeTheme.colors.textSecondary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        loadOptions.drop(3).forEachIndexed { i, l ->
                            if (i > 0) Spacer(modifier = Modifier.width(6.dp))
                            val isSelected = l == loadType
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) ReshapeTheme.colors.primarySoft else ReshapeTheme.colors.bgCanvas, RoundedCornerShape(8.dp))
                                    .clickable { loadType = l }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = l, fontSize = 12.sp, color = if (isSelected) ReshapeTheme.colors.primary else ReshapeTheme.colors.textSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 组数与次数区间
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "正式组数", fontSize = 13.sp, color = ReshapeTheme.colors.textSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "➖",
                                    fontSize = 16.sp,
                                    color = ReshapeTheme.colors.primary,
                                    modifier = Modifier.clickable { if (setsCount > 1) setsCount-- }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = "$setsCount 组", fontSize = 16.sp, color = ReshapeTheme.colors.textPrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "➕",
                                    fontSize = 16.sp,
                                    color = ReshapeTheme.colors.primary,
                                    modifier = Modifier.clickable { if (setsCount < 8) setsCount++ }
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "目标次数区间", fontSize = 13.sp, color = ReshapeTheme.colors.textSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "$repsMin - $repsMax 次", fontSize = 16.sp, color = ReshapeTheme.colors.textPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 休息时长
                    Text(text = "组间休息时长", fontSize = 13.sp, color = ReshapeTheme.colors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        restOptions.forEachIndexed { i, s ->
                            if (i > 0) Spacer(modifier = Modifier.width(8.dp))
                            val isSelected = s == restSeconds
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) ReshapeTheme.colors.primarySoft else ReshapeTheme.colors.bgCanvas, RoundedCornerShape(8.dp))
                                    .clickable { restSeconds = s }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "${s}s", fontSize = 12.sp, color = if (isSelected) ReshapeTheme.colors.primary else ReshapeTheme.colors.textSecondary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. 关联 1RM 基准与推算
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "智能重量推算依据", fontSize = 14.sp, color = ReshapeTheme.colors.textPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "关联三大项 1RM 后，系统可根据您的极限重量自动推算建议做组负荷",
                        fontSize = 12.sp,
                        color = ReshapeTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val benchmarkOptions = listOf(
                        Triple("bench", "卧推 1RM", 0.70f),
                        Triple("squat", "深蹲 1RM", 0.75f),
                        Triple("deadlift", "硬拉 1RM", 0.80f),
                        Triple("none", "不关联", 1.0f)
                    )

                    Row(modifier = Modifier.fillMaxWidth()) {
                        benchmarkOptions.forEachIndexed { i, (k, label, r) ->
                            if (i > 0) Spacer(modifier = Modifier.width(6.dp))
                            val isSelected = k == related1RmKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) ReshapeTheme.colors.primarySoft else ReshapeTheme.colors.bgCanvas, RoundedCornerShape(8.dp))
                                    .clickable {
                                        related1RmKey = k
                                        ratioOf1Rm = r
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = label.take(4), fontSize = 11.sp, color = if (isSelected) ReshapeTheme.colors.primary else ReshapeTheme.colors.textSecondary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. 器械与孔位备忘
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "器械孔位与执行口诀备忘", fontSize = 13.sp, color = ReshapeTheme.colors.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    TextField(
                        value = holeCues,
                        onValueChange = { holeCues = it },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = ReshapeTheme.colors.bgCanvas,
                            unfocusedContainerColor = ReshapeTheme.colors.bgCanvas,
                            focusedTextColor = ReshapeTheme.colors.textPrimary,
                            unfocusedTextColor = ReshapeTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. 生效范围选择
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                key(isPermanentSave) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "永久写入三分化计划", fontSize = 14.sp, color = ReshapeTheme.colors.textPrimary)
                            Text(text = if (isPermanentSave) "修改将持久保存至后续所有训练日" else "仅在本次训练临时追加，不改变模板", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                        }
                        Box(
                            modifier = Modifier
                                .background(if (isPermanentSave) ReshapeTheme.colors.primary else ReshapeTheme.colors.bgCanvas, RoundedCornerShape(16.dp))
                                .clickable { isPermanentSave = !isPermanentSave }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = if (isPermanentSave) "永久生效" else "仅本次", fontSize = 12.sp, color = if (isPermanentSave) Color.White else ReshapeTheme.colors.textSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
