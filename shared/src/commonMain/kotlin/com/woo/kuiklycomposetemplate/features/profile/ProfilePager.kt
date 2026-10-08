package com.woo.kuiklycomposetemplate.features.profile

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
import com.tencent.kuikly.compose.foundation.layout.fillMaxHeight
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
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.pager.Pager
import com.woo.kuiklycomposetemplate.core.components.GymNumPadDrawer
import com.woo.kuiklycomposetemplate.core.components.NumPadInputMode
import com.woo.kuiklycomposetemplate.core.components.ReshapeBottomBar
import com.woo.kuiklycomposetemplate.core.components.ReshapeNavTab
import com.woo.kuiklycomposetemplate.core.theme.ReshapeTheme
import com.woo.kuiklycomposetemplate.core.theme.ReshapeThemeId
import com.woo.kuiklycomposetemplate.core.theme.ThemeStateHolder
import com.woo.kuiklycomposetemplate.core.utils.copyToClipboard
import com.woo.kuiklycomposetemplate.core.utils.platformExportFile
import com.woo.kuiklycomposetemplate.core.utils.platformImportFile
import com.woo.kuiklycomposetemplate.data.local.BackupValidationResult
import com.woo.kuiklycomposetemplate.data.local.DataBackupEngine
import com.woo.kuiklycomposetemplate.data.local.UserPreferencesStore
import com.woo.kuiklycomposetemplate.domain.algorithm.DotsScoreCalculator
import com.woo.kuiklycomposetemplate.domain.model.Gender

/**
 * 个人档案与设置 (优化编辑入口、支持性别、杠铃杆重自定义与完整指标落盘)
 * 全面集成沉浸式 56dp 9 宫格大按键抽屉
 */
@Page("profile", supportInLocal = true)
class ProfilePager : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ReshapeTheme {
                ProfileScreen()
            }
        }
    }
}

@Composable
fun ProfileScreen() {
    val pager = LocalActivity.current.getPager() as? Pager
    val spModule = pager?.acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)

    var profile by remember { mutableStateOf(UserPreferencesStore.getProfile(spModule)) }
    var soundEnabled by remember { mutableStateOf(profile.soundEnabled) }
    var screenAwake by remember { mutableStateOf(profile.screenKeepOn) }

    // 数据备份与迁移
    var showImportSheet by remember { mutableStateOf(false) }
    var backupJsonInput by remember { mutableStateOf("") }
    var backupValidation by remember { mutableStateOf<BackupValidationResult?>(null) }

    // 档案编辑弹窗
    var isEditingProfile by remember { mutableStateOf(false) }
    var formNickname by remember { mutableStateOf(profile.nickname) }
    var formGender by remember { mutableStateOf(profile.gender) }
    var formBarbellWeight by remember { mutableStateOf(profile.barbellWeightKg.toString()) }
    var formExpYears by remember { mutableStateOf(profile.trainingExpYears.toString()) }
    var formHeight by remember { mutableStateOf(profile.heightCm.toInt().toString()) }
    var formBodyWeight by remember { mutableStateOf(profile.bodyWeightKg.toString()) }
    var formTargetWeight by remember { mutableStateOf(profile.targetWeightKg.toString()) }
    var formBodyFat by remember { mutableStateOf(profile.bodyFatPercent.toString()) }
    var formMuscle by remember { mutableStateOf(profile.skeletalMuscleKg.toString()) }

    // 沉浸式 9 宫格数字键盘抽屉状态
    var numPadVisible by remember { mutableStateOf(false) }
    var numPadMode by remember { mutableStateOf(NumPadInputMode.BODY_WEIGHT) }
    var numPadInitial by remember { mutableStateOf("75") }

    // 提示信息
    var saveNotification by remember { mutableStateOf<String?>(null) }

    val total1Rm = profile.bench1RmKg + profile.squat1RmKg + profile.deadlift1RmKg
    val dotsScore = DotsScoreCalculator.calculateDots(total1Rm, profile.bodyWeightKg, profile.gender == Gender.MALE)
    val strengthTitle = DotsScoreCalculator.getStrengthTierTitle(dotsScore, profile.gender == Gender.MALE)

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

                if (saveNotification != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .background(Color(0xFFECFDF5), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = saveNotification ?: "", fontSize = 12.sp, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // 1. 个人基本信息卡片 (统一的唯一编辑资料入口，适配小屏防换行)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(46.dp)
                                    .height(46.dp)
                                    .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(23.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (profile.nickname.isNotBlank()) profile.nickname.take(1).uppercase() else "K",
                                    fontSize = 20.sp,
                                    color = ReshapeTheme.colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = profile.nickname,
                                        fontSize = 16.sp,
                                        color = ReshapeTheme.colors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = strengthTitle,
                                            fontSize = 10.sp,
                                            color = ReshapeTheme.colors.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${profile.gender.label} · 铁龄 ${profile.trainingExpYears}年 · 杆重 ${profile.barbellWeightKg.toInt()}kg",
                                    fontSize = 11.sp,
                                    color = ReshapeTheme.colors.textSecondary
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // 紧凑编辑按钮 (小屏自适应)
                            Box(
                                modifier = Modifier
                                    .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(8.dp))
                                    .clickable {
                                        formNickname = profile.nickname
                                        formGender = profile.gender
                                        formBarbellWeight = profile.barbellWeightKg.toString()
                                        formExpYears = profile.trainingExpYears.toString()
                                        formHeight = profile.heightCm.toInt().toString()
                                        formBodyWeight = profile.bodyWeightKg.toString()
                                        formTargetWeight = profile.targetWeightKg.toString()
                                        formBodyFat = profile.bodyFatPercent.toString()
                                        formMuscle = profile.skeletalMuscleKg.toString()
                                        isEditingProfile = true
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "✏️ 编辑",
                                    fontSize = 11.sp,
                                    color = ReshapeTheme.colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF3F4F6)))
                        Spacer(modifier = Modifier.height(12.dp))

                        // 力量系数 DOTS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "力量系数 (DOTS)：${dotsScore.toInt()} 分 · $strengthTitle",
                                fontSize = 12.sp,
                                color = ReshapeTheme.colors.textSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "标准评级与推算 ›",
                                fontSize = 12.sp,
                                color = ReshapeTheme.colors.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("one_rep_max")
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. 身体档案数据矩阵
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "身体档案指标", fontSize = 15.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(text = "身高 Height", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "${profile.heightCm.toInt()} cm", fontSize = 16.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(text = "当前体重 Weight", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "${profile.bodyWeightKg} kg", fontSize = 16.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(text = "目标体重 Target", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "${profile.targetWeightKg} kg", fontSize = 16.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(text = "体脂率 Body Fat", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "${profile.bodyFatPercent}%", fontSize = 16.sp, color = ReshapeTheme.colors.green, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. 三大项 1RM 极速管理卡片
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🏋️ 三大项 1RM 极限推算基准", fontSize = 15.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(
                                text = "去推算/重测 ›",
                                fontSize = 12.sp,
                                color = ReshapeTheme.colors.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("one_rep_max")
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "深蹲 1RM", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "${profile.squat1RmKg.toInt()} kg", fontSize = 16.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "卧推 1RM", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "${profile.bench1RmKg.toInt()} kg", fontSize = 16.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "硬拉 1RM", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "${profile.deadlift1RmKg.toInt()} kg", fontSize = 16.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. 专业运动主题色切换
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "🎨 专业运动主题配色", fontSize = 15.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

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
                                val isSelected = ThemeStateHolder.currentThemeId == tId
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .background(if (isSelected) tColor else Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                                        .clickable {
                                            UserPreferencesStore.updateTheme(tId, spModule)
                                            saveNotification = "已切换主题为 $tName ✓"
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

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            themes.drop(3).forEachIndexed { idx, (tName, tId, tColor) ->
                                if (idx > 0) Spacer(modifier = Modifier.width(8.dp))
                                val isSelected = ThemeStateHolder.currentThemeId == tId
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .background(if (isSelected) tColor else Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                                        .clickable {
                                            UserPreferencesStore.updateTheme(tId, spModule)
                                            saveNotification = "已切换主题为 $tName ✓"
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

                Spacer(modifier = Modifier.height(14.dp))

                // 5. 系统与体验设置
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "⚙️ 训练体验设置", fontSize = 15.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        key(soundEnabled) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        soundEnabled = !soundEnabled
                                        profile = profile.copy(soundEnabled = soundEnabled)
                                        UserPreferencesStore.saveProfile(profile, spModule)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "倒计时轻音提示 (耳机穿透)", fontSize = 13.sp, color = ReshapeTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                                Text(text = if (soundEnabled) "已开启 ☑" else "已关闭 ☐", fontSize = 13.sp, color = if (soundEnabled) ReshapeTheme.colors.primary else ReshapeTheme.colors.textFaint)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF3F4F6)))
                        Spacer(modifier = Modifier.height(12.dp))

                        key(screenAwake) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        screenAwake = !screenAwake
                                        profile = profile.copy(screenKeepOn = screenAwake)
                                        UserPreferencesStore.saveProfile(profile, spModule)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "训练中屏幕常亮防息屏", fontSize = 13.sp, color = ReshapeTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                                Text(text = if (screenAwake) "已开启 ☑" else "已关闭 ☐", fontSize = 13.sp, color = if (screenAwake) ReshapeTheme.colors.primary else ReshapeTheme.colors.textFaint)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 6. 💾 本地数据备份与换机迁移 (Local-First 离线防丢)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💾 数据备份与换机迁移", fontSize = 15.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFECFDF5), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = "纯离线安全", fontSize = 10.sp, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Reshape 数据 100% 存储于本机。支持微信口令分享、.reshape 档案全量备份与 .csv 流水表格导出（Excel可用）。",
                            fontSize = 11.sp,
                            color = ReshapeTheme.colors.textSecondary,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // 3 大导出形态网格
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // 1. 复制口令
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(8.dp))
                                    .clickable {
                                        val shareableText = DataBackupEngine.exportShareablePasscode(spModule)
                                        copyToClipboard(shareableText)
                                        saveNotification = "✓ 训练档案迁移口令已复制！可在微信/备忘录直接粘贴"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "📦 复制口令", fontSize = 11.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // 2. 导出 .reshape 文件
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .background(Color(0xFFE0E7FF), RoundedCornerShape(8.dp))
                                    .clickable {
                                        val backupJson = DataBackupEngine.exportFullBackupJson(spModule)
                                        platformExportFile("Reshape_全量档案_备份.reshape", backupJson, "application/json")
                                        saveNotification = "✓ 已触发 .reshape 档案文件导出！"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "💾 导出.reshape", fontSize = 11.sp, color = Color(0xFF3730A3), fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // 3. 导出 .csv 表格
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                                    .clickable {
                                        val csvData = DataBackupEngine.exportTrainingHistoryCsv(spModule)
                                        platformExportFile("Reshape_训练打卡流水.csv", csvData, "text/csv")
                                        saveNotification = "✓ 已触发 Excel/CSV 训练流水表格导出！"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "📊 导出.csv", fontSize = 11.sp, color = Color(0xFF166534), fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 导入恢复入口主按钮
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp))
                                .clickable {
                                    showImportSheet = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "📥 导入档案口令 / 上传备份文件", fontSize = 13.sp, color = Color(0xFF92400E), fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 重新新手标定入口
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("onboarding")
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🎯 重新进行新手力量标定与偏好设置", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary, modifier = Modifier.weight(1f))
                            Text(text = "重设 ›", fontSize = 12.sp, color = ReshapeTheme.colors.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(90.dp))
            }
        }

        // =========================================================================
        // 6. 编辑个人档案与身体指标 Modal 弹窗
        // =========================================================================
        if (isEditingProfile) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000))
                    .clickable { isEditingProfile = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.88f)
                        .background(Color.White, RoundedCornerShape(18.dp))
                        .clickable(enabled = false) {}
                        .padding(16.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            // 顶栏
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "修改个人档案与身体指标",
                                    fontSize = 16.sp,
                                    color = ReshapeTheme.colors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "✕",
                                    fontSize = 18.sp,
                                    color = ReshapeTheme.colors.textFaint,
                                    modifier = Modifier
                                        .clickable { isEditingProfile = false }
                                        .padding(6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // ① 昵称
                            Text(text = "昵称 / 姓名", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = formNickname,
                                    onValueChange = { formNickname = it },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(fontSize = 14.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // ② 性别 (清晰男 / 女单选)
                            Text(text = "性别", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .background(if (formGender == Gender.MALE) ReshapeTheme.colors.primary else Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                                        .clickable { formGender = Gender.MALE },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "男 ♂", fontSize = 13.sp, color = if (formGender == Gender.MALE) Color.White else ReshapeTheme.colors.textSecondary, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .background(if (formGender == Gender.FEMALE) ReshapeTheme.colors.primary else Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                                        .clickable { formGender = Gender.FEMALE },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "女 ♀", fontSize = 13.sp, color = if (formGender == Gender.FEMALE) Color.White else ReshapeTheme.colors.textSecondary, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // ③ 常用奥林匹克杠铃杆重 (自由可选 20kg / 15kg / 10kg 或自定义)
                            Text(text = "常用奥林匹克杠铃杆重", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                listOf("20.0" to "20kg (标准杆)", "15.0" to "15kg (轻量杆)", "10.0" to "10kg").forEachIndexed { idx, (wKg, label) ->
                                    if (idx > 0) Spacer(modifier = Modifier.width(6.dp))
                                    val isSelected = formBarbellWeight == wKg
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .background(if (isSelected) ReshapeTheme.colors.primarySoft else Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                                            .clickable { formBarbellWeight = wKg },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = label, fontSize = 11.sp, color = if (isSelected) ReshapeTheme.colors.primary else ReshapeTheme.colors.textSecondary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // ④ 铁龄 (年)
                            Text(text = "训练经验 (铁龄/年)", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = formExpYears,
                                    onValueChange = { formExpYears = it.filter { c -> c.isDigit() || c == '.' } },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(fontSize = 14.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // ⑤ 身高 (cm) 与 当前体重 (kg)
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "身高 (cm)", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                                            .clickable {
                                                numPadMode = NumPadInputMode.HEIGHT
                                                numPadInitial = formHeight
                                                numPadVisible = true
                                            }
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        key(formHeight) {
                                            Text(text = "${formHeight} cm ⌨", fontSize = 14.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "当前体重 (kg)", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                                            .clickable {
                                                numPadMode = NumPadInputMode.BODY_WEIGHT
                                                numPadInitial = formBodyWeight
                                                numPadVisible = true
                                            }
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        key(formBodyWeight) {
                                            Text(text = "${formBodyWeight} kg ⌨", fontSize = 14.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // ⑥ 目标体重 (kg) 与 体脂率 (%)
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "目标体重 (kg)", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                                            .clickable {
                                                numPadMode = NumPadInputMode.TARGET_WEIGHT
                                                numPadInitial = formTargetWeight
                                                numPadVisible = true
                                            }
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        key(formTargetWeight) {
                                            Text(text = "${formTargetWeight} kg ⌨", fontSize = 14.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "体脂率 (%)", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                                            .clickable {
                                                numPadMode = NumPadInputMode.BODY_FAT
                                                numPadInitial = formBodyFat
                                                numPadVisible = true
                                            }
                                            .padding(horizontal = 10.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        key(formBodyFat) {
                                            Text(text = "${formBodyFat} % ⌨", fontSize = 14.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // ⑦ 保存主按钮
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .background(ReshapeTheme.colors.primary, RoundedCornerShape(10.dp))
                                    .clickable {
                                        val updated = profile.copy(
                                            nickname = formNickname.ifBlank { "Reshape 老铁" },
                                            gender = formGender,
                                            barbellWeightKg = formBarbellWeight.toFloatOrNull() ?: 20.0f,
                                            trainingExpYears = formExpYears.toFloatOrNull() ?: 2.0f,
                                            heightCm = formHeight.toFloatOrNull() ?: 175.0f,
                                            bodyWeightKg = formBodyWeight.toFloatOrNull() ?: 75.0f,
                                            targetWeightKg = formTargetWeight.toFloatOrNull() ?: 72.0f,
                                            bodyFatPercentage = formBodyFat.toFloatOrNull() ?: 15.0f,
                                            skeletalMuscleKg = formMuscle.toFloatOrNull() ?: 36.0f
                                        )
                                        profile = updated
                                        UserPreferencesStore.saveProfile(updated, spModule)
                                        isEditingProfile = false
                                        saveNotification = "个人身体档案已成功保存并实时落盘 ✓"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "保存身体档案", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 7. 导入全量 JSON 备份 Modal 抽屉
        // =========================================================================
        if (showImportSheet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000))
                    .clickable { showImportSheet = false },
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
                            Text(text = "✕", fontSize = 18.sp, color = ReshapeTheme.colors.textSecondary, modifier = Modifier.clickable { showImportSheet = false })
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
                                        backupJsonInput = fileText
                                        backupValidation = DataBackupEngine.validateBackupJson(fileText)
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
                            value = backupJsonInput,
                            onValueChange = {
                                backupJsonInput = it
                                backupValidation = if (it.isNotBlank()) DataBackupEngine.validateBackupJson(it) else null
                            },
                            textStyle = TextStyle(fontSize = 12.sp, color = ReshapeTheme.colors.textPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(ReshapeTheme.colors.bgCanvas, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val res = backupValidation
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
                                        Text(text = "• 档案：${res.nickname} · 体重 ${res.bodyWeightKg.toInt()}kg · 三大项总和 ${res.total1RmKg.toInt()}kg", fontSize = 11.sp, color = Color(0xFF047857))
                                        Text(text = "• 包含：${res.historySessionCount} 场历史打卡 · 自定义动作：${res.customExerciseCount} 个", fontSize = 11.sp, color = Color(0xFF047857))
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
                                .height(46.dp)
                                .background(if (res?.isValid == true) ReshapeTheme.colors.primary else Color(0xFF9CA3AF), RoundedCornerShape(12.dp))
                                .clickable {
                                    if (res?.isValid == true) {
                                        val success = DataBackupEngine.restoreFromBackupJson(backupJsonInput, spModule)
                                        if (success) {
                                            profile = UserPreferencesStore.getProfile(spModule)
                                            showImportSheet = false
                                            saveNotification = "✓ 全量训练档案与历史流水已成功恢复！"
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "确认覆盖并恢复全量数据", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        ReshapeBottomBar(
            currentTab = ReshapeNavTab.PROFILE,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

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
                    NumPadInputMode.HEIGHT -> formHeight = newVal.toInt().toString()
                    NumPadInputMode.BODY_WEIGHT -> formBodyWeight = if (newVal % 1.0f == 0f) newVal.toInt().toString() else (kotlin.math.round(newVal * 10f) / 10f).toString()
                    NumPadInputMode.TARGET_WEIGHT -> formTargetWeight = if (newVal % 1.0f == 0f) newVal.toInt().toString() else (kotlin.math.round(newVal * 10f) / 10f).toString()
                    NumPadInputMode.BODY_FAT -> formBodyFat = if (newVal % 1.0f == 0f) newVal.toInt().toString() else (kotlin.math.round(newVal * 10f) / 10f).toString()
                    else -> {}
                }
                numPadVisible = false
            }
        )
    }
}
