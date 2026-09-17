package com.woo.kuiklycomposetemplate.features.onboarding

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
import com.tencent.kuikly.compose.foundation.text.BasicTextField
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.ui.zIndex
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.pager.Pager
import com.woo.kuiklycomposetemplate.core.components.GymNumPadDrawer
import com.woo.kuiklycomposetemplate.core.components.NumPadInputMode
import com.woo.kuiklycomposetemplate.core.theme.ReshapeTheme
import com.woo.kuiklycomposetemplate.core.theme.ReshapeThemeId
import com.woo.kuiklycomposetemplate.core.theme.ThemeStateHolder
import com.woo.kuiklycomposetemplate.core.utils.platformImportFile
import com.woo.kuiklycomposetemplate.data.local.BackupValidationResult
import com.woo.kuiklycomposetemplate.data.local.DataBackupEngine
import com.woo.kuiklycomposetemplate.data.local.PlanRepository
import com.woo.kuiklycomposetemplate.data.local.TrainingCycleStore
import com.woo.kuiklycomposetemplate.data.local.UserPreferencesStore
import com.woo.kuiklycomposetemplate.domain.algorithm.DotsScoreCalculator
import com.woo.kuiklycomposetemplate.domain.model.Gender
import com.woo.kuiklycomposetemplate.domain.model.UserProfile

/**
 * 首次启动新手引导与力量基准标定页 (OnboardingPager)
 * 具备性别、体重、经验、三大项 1RM 设定及已有备份一键导入恢复能力
 * 全面集成沉浸式 56dp 9 宫格大按键抽屉
 */
@Page("onboarding", supportInLocal = true)
class OnboardingPager : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ReshapeTheme {
                OnboardingScreen()
            }
        }
    }
}

@Composable
fun OnboardingScreen() {
    val pager = LocalActivity.current.getPager() as? Pager
    val spModule = pager?.acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)

    val initialProfile = remember { UserPreferencesStore.getProfile(spModule) }

    var gender by remember { mutableStateOf(initialProfile.gender) }
    var nickname by remember { mutableStateOf(initialProfile.nickname.ifBlank { "Reshape 老铁" }) }
    var heightCm by remember { mutableStateOf(initialProfile.heightCm) }
    var bodyWeightKg by remember { mutableStateOf(initialProfile.bodyWeightKg) }
    var targetWeightKg by remember { mutableStateOf(initialProfile.targetWeightKg) }
    var bodyFatPercentage by remember { mutableStateOf(initialProfile.bodyFatPercentage) }
    var skeletalMuscleKg by remember { mutableStateOf(initialProfile.skeletalMuscleKg) }
    var expYears by remember { mutableStateOf(initialProfile.trainingExpYears) }
    var barbellWeightKg by remember { mutableStateOf(initialProfile.barbellWeightKg) }

    // 三大项 1RM
    var bench1Rm by remember { mutableStateOf(initialProfile.bench1RmKg) }
    var squat1Rm by remember { mutableStateOf(initialProfile.squat1RmKg) }
    var deadlift1Rm by remember { mutableStateOf(initialProfile.deadlift1RmKg) }

    // 沉浸式 9 宫格数字键盘抽屉状态
    var numPadVisible by remember { mutableStateOf(false) }
    var numPadMode by remember { mutableStateOf(NumPadInputMode.BODY_WEIGHT) }
    var numPadInitial by remember { mutableStateOf("75") }

    // 主题色
    var selectedTheme by remember { mutableStateOf(ThemeStateHolder.currentThemeId) }

    // 导入备份弹窗
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var validationResult by remember { mutableStateOf<BackupValidationResult?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val total1Rm = bench1Rm + squat1Rm + deadlift1Rm
    val dotsScore = DotsScoreCalculator.calculateDots(total1Rm, bodyWeightKg, gender == Gender.MALE)
    val strengthTitle = DotsScoreCalculator.getStrengthTierTitle(dotsScore, gender == Gender.MALE)

    // 一键力量等级预设
    fun applyPresetLevel(level: String) {
        val bw = bodyWeightKg
        when (level) {
            "novice" -> {
                bench1Rm = ((bw * (if (gender == Gender.MALE) 0.8f else 0.5f)) / 2.5f).toInt() * 2.5f
                squat1Rm = ((bw * (if (gender == Gender.MALE) 1.0f else 0.7f)) / 2.5f).toInt() * 2.5f
                deadlift1Rm = ((bw * (if (gender == Gender.MALE) 1.3f else 0.9f)) / 2.5f).toInt() * 2.5f
            }
            "intermediate" -> {
                bench1Rm = ((bw * (if (gender == Gender.MALE) 1.0f else 0.7f)) / 2.5f).toInt() * 2.5f
                squat1Rm = ((bw * (if (gender == Gender.MALE) 1.4f else 1.0f)) / 2.5f).toInt() * 2.5f
                deadlift1Rm = ((bw * (if (gender == Gender.MALE) 1.7f else 1.3f)) / 2.5f).toInt() * 2.5f
            }
            "advanced" -> {
                bench1Rm = ((bw * (if (gender == Gender.MALE) 1.3f else 0.9f)) / 2.5f).toInt() * 2.5f
                squat1Rm = ((bw * (if (gender == Gender.MALE) 1.8f else 1.3f)) / 2.5f).toInt() * 2.5f
                deadlift1Rm = ((bw * (if (gender == Gender.MALE) 2.2f else 1.6f)) / 2.5f).toInt() * 2.5f
            }
        }
    }

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

                // 顶部标题
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⚡ RESHAPE", fontSize = 14.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "跳过使用默认 ›",
                            fontSize = 12.sp,
                            color = ReshapeTheme.colors.textSecondary,
                            modifier = Modifier.clickable {
                                UserPreferencesStore.setOnboardingCompleted(true, spModule)
                                pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("home")
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "标定你的力量起点",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = ReshapeTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "基于运动生理学与力量举科学模型，自动生成专属渐进超负荷计划与起始重量。",
                        fontSize = 13.sp,
                        color = ReshapeTheme.colors.textSecondary,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 顶部快速恢复通道 (已有备份？一键口令导入)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(14.dp))
                        .clickable { showImportDialog = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📦", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "已有旧设备备份？一键粘贴口令恢复", fontSize = 13.sp, color = Color(0xFF92400E), fontWeight = FontWeight.Bold)
                            Text(text = "支持粘贴微信/备忘录档案口令，瞬间复原全部历史打卡与计划", fontSize = 11.sp, color = Color(0xFFB45309))
                        }
                        Text(text = "导入 ➔", fontSize = 12.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. 基本信息与性别
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "1. 基本信息与性别", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ReshapeTheme.colors.textPrimary)
                        Spacer(modifier = Modifier.height(12.dp))

                        // 昵称输入
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(text = "用户昵称", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            BasicTextField(
                                value = nickname,
                                onValueChange = { nickname = it },
                                textStyle = TextStyle(fontSize = 14.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 性别单选
                        Text(text = "生理性别 (影响力量基准推算)", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            val isMale = gender == Gender.MALE
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .background(if (isMale) ReshapeTheme.colors.primary else ReshapeTheme.colors.bgCanvas, RoundedCornerShape(10.dp))
                                    .clickable {
                                        gender = Gender.MALE
                                        if (bodyWeightKg == 55f) bodyWeightKg = 75f
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "👦 男性 ♂",
                                    fontSize = 14.sp,
                                    color = if (isMale) Color.White else ReshapeTheme.colors.textPrimary,
                                    fontWeight = if (isMale) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .background(if (!isMale) ReshapeTheme.colors.primary else ReshapeTheme.colors.bgCanvas, RoundedCornerShape(10.dp))
                                    .clickable {
                                        gender = Gender.FEMALE
                                        if (bodyWeightKg == 75f) bodyWeightKg = 55f
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "👧 女性 ♀",
                                    fontSize = 14.sp,
                                    color = if (!isMale) Color.White else ReshapeTheme.colors.textPrimary,
                                    fontWeight = if (!isMale) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. 身高、体重与体成分指标
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "2. 身体数据与体成分 (点击数值键盘输入)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ReshapeTheme.colors.textPrimary)
                        Spacer(modifier = Modifier.height(12.dp))

                        // 第一行：身高 + 当前体重
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // 身高
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "身高 (cm)", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "−",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ReshapeTheme.colors.textSecondary,
                                        modifier = Modifier.clickable { heightCm = maxOf(100f, heightCm - 1f) }
                                    )
                                    key(heightCm) {
                                        Text(
                                            text = "${heightCm.toInt()} cm ⌨",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ReshapeTheme.colors.primary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    numPadMode = NumPadInputMode.HEIGHT
                                                    numPadInitial = heightCm.toInt().toString()
                                                    numPadVisible = true
                                                }
                                        )
                                    }
                                    Text(
                                        text = "+",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ReshapeTheme.colors.textSecondary,
                                        modifier = Modifier.clickable { heightCm = minOf(230f, heightCm + 1f) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // 当前体重
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "当前体重 (kg)", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "−",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ReshapeTheme.colors.textSecondary,
                                        modifier = Modifier.clickable { bodyWeightKg = maxOf(35f, bodyWeightKg - 0.5f) }
                                    )
                                    key(bodyWeightKg) {
                                        Text(
                                            text = "${if (bodyWeightKg % 1.0f == 0f) bodyWeightKg.toInt().toString() else bodyWeightKg.toString()} kg ⌨",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ReshapeTheme.colors.primary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    numPadMode = NumPadInputMode.BODY_WEIGHT
                                                    numPadInitial = if (bodyWeightKg % 1.0f == 0f) bodyWeightKg.toInt().toString() else bodyWeightKg.toString()
                                                    numPadVisible = true
                                                }
                                        )
                                    }
                                    Text(
                                        text = "+",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ReshapeTheme.colors.textSecondary,
                                        modifier = Modifier.clickable { bodyWeightKg = minOf(180f, bodyWeightKg + 0.5f) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 第二行：目标体重 + 体脂率 + 骨骼肌
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // 目标体重
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "目标体重", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(8.dp))
                                        .clickable {
                                            numPadMode = NumPadInputMode.TARGET_WEIGHT
                                            numPadInitial = if (targetWeightKg % 1.0f == 0f) targetWeightKg.toInt().toString() else targetWeightKg.toString()
                                            numPadVisible = true
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${if (targetWeightKg % 1.0f == 0f) targetWeightKg.toInt().toString() else targetWeightKg.toString()} kg ⌨",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ReshapeTheme.colors.textPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // 体脂率
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "体脂率", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(8.dp))
                                        .clickable {
                                            numPadMode = NumPadInputMode.BODY_FAT
                                            numPadInitial = if (bodyFatPercentage % 1.0f == 0f) bodyFatPercentage.toInt().toString() else bodyFatPercentage.toString()
                                            numPadVisible = true
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${if (bodyFatPercentage % 1.0f == 0f) bodyFatPercentage.toInt().toString() else bodyFatPercentage.toString()}% ⌨",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ReshapeTheme.colors.textPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // 骨骼肌重
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "骨骼肌", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(8.dp))
                                        .clickable {
                                            numPadMode = NumPadInputMode.SKELETAL_MUSCLE
                                            numPadInitial = if (skeletalMuscleKg % 1.0f == 0f) skeletalMuscleKg.toInt().toString() else skeletalMuscleKg.toString()
                                            numPadVisible = true
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${if (skeletalMuscleKg % 1.0f == 0f) skeletalMuscleKg.toInt().toString() else skeletalMuscleKg.toString()} kg ⌨",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ReshapeTheme.colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. 训练经验与力量基准三大项 1RM 标定
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "3. 训练经验与力量基准", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ReshapeTheme.colors.textPrimary)
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(text = "$strengthTitle · DOTS ${dotsScore.toInt()}分", fontSize = 11.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 训练年限与空杆重
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // 训练年限
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "力量训练年限", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "−",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ReshapeTheme.colors.textSecondary,
                                        modifier = Modifier.clickable { expYears = maxOf(0f, expYears - 0.5f) }
                                    )
                                    key(expYears) {
                                        Text(
                                            text = "${if (expYears % 1.0f == 0f) expYears.toInt().toString() else expYears.toString()} 年 ⌨",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ReshapeTheme.colors.primary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    numPadMode = NumPadInputMode.TRAINING_EXP
                                                    numPadInitial = if (expYears % 1.0f == 0f) expYears.toInt().toString() else expYears.toString()
                                                    numPadVisible = true
                                                }
                                        )
                                    }
                                    Text(
                                        text = "+",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ReshapeTheme.colors.textSecondary,
                                        modifier = Modifier.clickable { expYears = minOf(30f, expYears + 0.5f) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // 杠铃空杆重
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "常用杠铃空杆重", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(20f to "20kg", 15f to "15kg", 10f to "10kg").forEach { (w, label) ->
                                        val isSel = barbellWeightKg == w
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(if (isSel) ReshapeTheme.colors.primary else Color.Transparent, RoundedCornerShape(6.dp))
                                                .clickable { barbellWeightKg = w }
                                                .padding(vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                color = if (isSel) Color.White else ReshapeTheme.colors.textPrimary,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 快速水平预设按钮
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(text = "三大项 1RM 预设:", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary, modifier = Modifier.align(Alignment.CenterVertically))
                            Spacer(modifier = Modifier.width(6.dp))
                            listOf("新手入门" to "novice", "进阶力量" to "intermediate", "资深健力" to "advanced").forEach { (title, key) ->
                                Box(
                                    modifier = Modifier
                                        .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(6.dp))
                                        .clickable { applyPresetLevel(key) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = title, fontSize = 11.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3 大项卡片 (点击数值即可呼出 9 宫格大键盘)
                        val lifts = listOf(
                            Triple("卧推 1RM", bench1Rm to NumPadInputMode.BENCH_1RM) { delta: Float -> bench1Rm = maxOf(20f, bench1Rm + delta) },
                            Triple("深蹲 1RM", squat1Rm to NumPadInputMode.SQUAT_1RM) { delta: Float -> squat1Rm = maxOf(20f, squat1Rm + delta) },
                            Triple("硬拉 1RM", deadlift1Rm to NumPadInputMode.DEADLIFT_1RM) { delta: Float -> deadlift1Rm = maxOf(20f, deadlift1Rm + delta) }
                        )

                        Row(modifier = Modifier.fillMaxWidth()) {
                            lifts.forEachIndexed { idx, (liftName, liftPair, onAdjust) ->
                                val (liftVal, mode) = liftPair
                                if (idx > 0) Spacer(modifier = Modifier.width(8.dp))
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(10.dp))
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = liftName, fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    key(liftVal, liftName) {
                                        Text(
                                            text = "${if (liftVal % 1.0f == 0f) liftVal.toInt().toString() else liftVal.toString()}kg ⌨",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ReshapeTheme.colors.primary,
                                            modifier = Modifier.clickable {
                                                numPadMode = mode
                                                numPadInitial = if (liftVal % 1.0f == 0f) liftVal.toInt().toString() else liftVal.toString()
                                                numPadVisible = true
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(28.dp)
                                                .background(Color(0xFFE5E7EB), RoundedCornerShape(6.dp))
                                                .clickable { onAdjust(-2.5f) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "−2.5", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(28.dp)
                                                .background(Color(0xFFE5E7EB), RoundedCornerShape(6.dp))
                                                .clickable { onAdjust(2.5f) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "+2.5", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. 主题偏好
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "4. 运动视觉主题", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ReshapeTheme.colors.textPrimary)
                        Spacer(modifier = Modifier.height(10.dp))

                        val themes = listOf(
                            Triple("经典燃脂橙", ReshapeThemeId.ORANGE, Color(0xFFFF5722)),
                            Triple("深海专注蓝", ReshapeThemeId.BLUE, Color(0xFF2563EB)),
                            Triple("翡翠高能绿", ReshapeThemeId.EMERALD, Color(0xFF10B981)),
                            Triple("黑曜石暗金", ReshapeThemeId.GOLD, Color(0xFFD97706)),
                            Triple("赛博霓虹紫", ReshapeThemeId.PURPLE, Color(0xFF8B5CF6)),
                            Triple("破晓冰川青", ReshapeThemeId.CYAN, Color(0xFF06B6D4))
                        )

                        Row(modifier = Modifier.fillMaxWidth()) {
                            themes.take(3).forEachIndexed { idx, (tName, tId, tColor) ->
                                if (idx > 0) Spacer(modifier = Modifier.width(8.dp))
                                val isSelected = selectedTheme == tId
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .background(if (isSelected) tColor else Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedTheme = tId
                                            UserPreferencesStore.updateTheme(tId, spModule)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tName,
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.White else ReshapeTheme.colors.textSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 底部开启按钮
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(52.dp)
                        .background(ReshapeTheme.colors.primary, RoundedCornerShape(14.dp))
                        .clickable {
                            val profile = initialProfile.copy(
                                userId = initialProfile.userId.ifBlank { "local_user" },
                                nickname = nickname.trim().ifBlank { if (gender == Gender.MALE) "Reshape 老铁" else "Reshape 力量女孩" },
                                gender = gender,
                                heightCm = heightCm,
                                bodyWeightKg = bodyWeightKg,
                                targetWeightKg = targetWeightKg,
                                bodyFatPercentage = bodyFatPercentage,
                                skeletalMuscleKg = skeletalMuscleKg,
                                trainingExpYears = expYears,
                                barbellWeightKg = barbellWeightKg,
                                bench1RmKg = bench1Rm,
                                squat1RmKg = squat1Rm,
                                deadlift1RmKg = deadlift1Rm,
                                preferredThemeCode = selectedTheme.name
                            )
                            UserPreferencesStore.saveProfile(profile, spModule)
                            // 标记完成引导
                            UserPreferencesStore.setOnboardingCompleted(true, spModule)
                            // 联动刷新计划建议负荷
                            PlanRepository.refreshSuggestedWeightsWithProfile(profile, spModule)
                            // 初始化第 1 周期
                            TrainingCycleStore.startNewCycle(profile, spModule)
                            // 跳转首页
                            pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("home")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚡ 开启我的智能力量训练 ➔",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(44.dp))
            }
        }

        // 导入数据抽屉
        if (showImportDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(900f)
                    .background(Color(0x80000000))
                    .clickable { showImportDialog = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .clickable { /* 拦截冒泡 */ }
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📦 导入训练档案 / 恢复数据", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ReshapeTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                            Text(text = "✕", fontSize = 18.sp, color = ReshapeTheme.colors.textSecondary, modifier = Modifier.clickable { showImportDialog = false })
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // 快速选取文件按钮
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(8.dp))
                                .clickable {
                                    platformImportFile(listOf(".reshape", ".json")) { fileText ->
                                        importJsonText = fileText
                                        validationResult = DataBackupEngine.validateBackupJson(fileText)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "📁 选取本地 / 微信接收的 .reshape 备份文件", fontSize = 12.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "或者在下方直接粘贴整段档案口令文本：", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                        Spacer(modifier = Modifier.height(8.dp))

                        BasicTextField(
                            value = importJsonText,
                            onValueChange = {
                                importJsonText = it
                                validationResult = if (it.isNotBlank()) DataBackupEngine.validateBackupJson(it) else null
                            },
                            textStyle = TextStyle(fontSize = 12.sp, color = ReshapeTheme.colors.textPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val res = validationResult
                        if (res != null) {
                            if (res.isValid) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFECFDF5), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(text = "✓ 档案口令识别成功", fontSize = 12.sp, color = Color(0xFF065F46), fontWeight = FontWeight.Bold)
                                        Text(text = "• 档案：${res.nickname} (${if (res.gender == "MALE") "男" else "女"}) · 体重 ${res.bodyWeightKg.toInt()}kg", fontSize = 11.sp, color = Color(0xFF047857))
                                        Text(text = "• 包含：${res.historySessionCount} 场历史训练流水 · 自定义动作：${res.customExerciseCount} 个", fontSize = 11.sp, color = Color(0xFF047857))
                                    }
                                }
                            } else {
                                Text(text = "⚠️ ${res.errorMessage}", fontSize = 12.sp, color = Color(0xFFEF4444))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(if (res?.isValid == true) ReshapeTheme.colors.primary else Color(0xFF9CA3AF), RoundedCornerShape(12.dp))
                                .clickable {
                                    if (res?.isValid == true) {
                                        val success = DataBackupEngine.restoreFromBackupJson(importJsonText, spModule)
                                        if (success) {
                                            showImportDialog = false
                                            pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("home")
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "一键恢复并进入首页 ➔", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 沉浸式 56dp 9 宫格数字键盘抽屉 (全端防拦截、肉体实战盲打)
        // =========================================================================
        GymNumPadDrawer(
            visible = numPadVisible,
            mode = numPadMode,
            initialValue = numPadInitial,
            onDismiss = { numPadVisible = false },
            onConfirm = { newVal ->
                when (numPadMode) {
                    NumPadInputMode.HEIGHT -> heightCm = newVal.coerceIn(80f, 250f)
                    NumPadInputMode.BODY_WEIGHT -> bodyWeightKg = newVal.coerceIn(30f, 250f)
                    NumPadInputMode.TARGET_WEIGHT -> targetWeightKg = newVal.coerceIn(30f, 250f)
                    NumPadInputMode.BODY_FAT -> bodyFatPercentage = newVal.coerceIn(3f, 60f)
                    NumPadInputMode.SKELETAL_MUSCLE -> skeletalMuscleKg = newVal.coerceIn(10f, 100f)
                    NumPadInputMode.TRAINING_EXP -> expYears = newVal.coerceIn(0f, 50f)
                    NumPadInputMode.WEIGHT -> barbellWeightKg = newVal.coerceIn(0f, 50f)
                    NumPadInputMode.BENCH_1RM -> bench1Rm = newVal.coerceAtLeast(0f)
                    NumPadInputMode.SQUAT_1RM -> squat1Rm = newVal.coerceAtLeast(0f)
                    NumPadInputMode.DEADLIFT_1RM -> deadlift1Rm = newVal.coerceAtLeast(0f)
                    else -> {}
                }
                numPadVisible = false
            }
        )
    }
}
