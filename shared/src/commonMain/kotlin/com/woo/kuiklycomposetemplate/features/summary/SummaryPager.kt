package com.woo.kuiklycomposetemplate.features.summary

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
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.pager.Pager
import com.woo.kuiklycomposetemplate.core.theme.ReshapeTheme
import com.woo.kuiklycomposetemplate.core.utils.copyToClipboard
import com.woo.kuiklycomposetemplate.data.local.WorkoutHistoryStore
import com.woo.kuiklycomposetemplate.domain.model.WorkoutSession

@Page("summary", supportInLocal = true)
class SummaryPager : ComposeContainer() {

    private var initialSessionId: String? = null

    override fun willInit() {
        super.willInit()
        try {
            val sid = pageData.params.optString("sessionId", "")
            if (sid.isNotBlank()) {
                initialSessionId = sid
            }
        } catch (_: Exception) {}

        setContent {
            ReshapeTheme {
                SummaryScreen(initialSessionId)
            }
        }
    }
}

@Composable
fun SummaryScreen(initialSessionId: String? = null) {
    val pager = LocalActivity.current.getPager() as? Pager
    val spModule = pager?.acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)

    val history = remember { WorkoutHistoryStore.getHistory(spModule) }
    val latestSession: WorkoutSession? = remember {
        if (!initialSessionId.isNullOrBlank()) {
            history.find { it.sessionId == initialSessionId } ?: history.firstOrNull()
        } else {
            history.firstOrNull()
        }
    }

    var isCopied by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ReshapeTheme.colors.bgCanvas)
    ) {
        item {
            Spacer(modifier = Modifier.height(44.dp))

            if (latestSession == null) {
                // 空状态：尚无真实训练记录
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "📋", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无已结算的训练记录",
                        fontSize = 20.sp,
                        color = ReshapeTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "去开启并完成一次训练，战报将在此自动结算归档并分析超量恢复状态。",
                        fontSize = 13.sp,
                        color = ReshapeTheme.colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(ReshapeTheme.colors.primary, RoundedCornerShape(12.dp))
                            .clickable {
                                pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("home")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "前往首页开启训练 ➔", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                val planTitle = latestSession.planContext.dayTitle
                val dayCode = latestSession.planContext.dayCode
                val totalTonnage = latestSession.sessionMetrics.totalTonnageKg
                val durationMin = latestSession.sessionMetrics.durationMinutes
                val workingSets = latestSession.sessionMetrics.workingSetsCount
                val warmupSets = latestSession.sessionMetrics.warmupSetsCount
                val prCount = latestSession.sessionMetrics.prBreakCount

                // 提取所有突破 PR 的动作明细
                val prSetsList = latestSession.exercises.flatMap { ex ->
                    ex.sets.filter { it.isPr }.map { s -> "${ex.name} ${s.weightKg.toInt()}kg" }
                }

                // 庆祝标头
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🏆", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "训练圆满达成！",
                        fontSize = 24.sp,
                        color = ReshapeTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$dayCode $planTitle · 今日强度达标 · 超量恢复正式启动",
                        fontSize = 13.sp,
                        color = ReshapeTheme.colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 3 大核心真实战报指标汇总
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    // 有效正式组
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "有效正式组", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$workingSets 组",
                                fontSize = 20.sp,
                                color = ReshapeTheme.colors.primary,
                                fontWeight = FontWeight.Bold
                            )
                            if (warmupSets > 0) {
                                Text(text = "+$warmupSets 热身", fontSize = 10.sp, color = ReshapeTheme.colors.textFaint)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 总吨位
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "总负荷吨位", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${totalTonnage.toInt()} kg",
                                fontSize = 20.sp,
                                color = ReshapeTheme.colors.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = "累计做功", fontSize = 10.sp, color = ReshapeTheme.colors.textFaint)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 训练耗时
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "训练总耗时", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (durationMin > 0) "$durationMin 分钟" else "< 1 分钟",
                                fontSize = 20.sp,
                                color = ReshapeTheme.colors.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = "秒表实测", fontSize = 10.sp, color = ReshapeTheme.colors.textFaint)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // PR 突破表彰卡 (真实依据打卡数据展示)
                if (prSetsList.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(
                                Color(0xFFFEF3C7),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎖️", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "恭喜达成 ${prSetsList.size} 项历史重量 PR 突破！",
                                    fontSize = 13.sp,
                                    color = Color(0xFF92400E),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "突破动作：${prSetsList.joinToString("、")}",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 真实动作与做组清单明细 (Real Breakdown)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "📋 真实动作与做组明细 (${latestSession.exercises.size} 个动作)",
                                fontSize = 15.sp,
                                color = ReshapeTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "全场实录",
                                fontSize = 11.sp,
                                color = ReshapeTheme.colors.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        latestSession.exercises.forEachIndexed { exIdx, ex ->
                            if (exIdx > 0) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(ReshapeTheme.colors.bgCanvas)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            val exVolume = ex.sets.sumOf { (it.weightKg * it.effectiveReps).toDouble() }.toInt()
                            val workingSetsCount = ex.sets.count { !it.isWarmup }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${exIdx + 1}. ${ex.name}",
                                        fontSize = 14.sp,
                                        color = ReshapeTheme.colors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "$workingSetsCount 组正式 · $exVolume kg",
                                        fontSize = 12.sp,
                                        color = ReshapeTheme.colors.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // 做组表格
                                ex.sets.forEach { setItem ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (setItem.isWarmup) "第 ${setItem.setNo} 组 [热身]" else "第 ${setItem.setNo} 组",
                                            fontSize = 12.sp,
                                            color = if (setItem.isWarmup) ReshapeTheme.colors.textFaint else ReshapeTheme.colors.textSecondary,
                                            modifier = Modifier.width(90.dp)
                                        )

                                        val repsText = if (setItem.leftReps != null && setItem.rightReps != null) {
                                            "L:${setItem.leftReps} / R:${setItem.rightReps} 次"
                                        } else {
                                            "${setItem.effectiveReps} 次"
                                        }

                                        Text(
                                            text = "${setItem.weightKg} kg × $repsText",
                                            fontSize = 12.sp,
                                            color = ReshapeTheme.colors.textPrimary,
                                            fontWeight = if (setItem.isPr) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.weight(1f)
                                        )

                                        if (setItem.isPr) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFFEF3C7), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(text = "🎖️ PR", fontSize = 10.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }

                                        Text(
                                            text = "RPE ${setItem.rpe}",
                                            fontSize = 11.sp,
                                            color = ReshapeTheme.colors.textFaint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 极简纯黑老铁战报卡 (StoryCard 真实数据生成)
                val stimulatedMuscles = latestSession.exercises.map { it.category }.distinct().filter { it.isNotBlank() }
                val storyCardText = buildString {
                    appendLine("🔥 RESHAPE 铁馆硬核打卡")
                    appendLine("━━━━━━━━━━━━━━━━━━━")
                    appendLine("🏋️ 今日分化：$dayCode $planTitle")
                    appendLine("⏱️ 训练时长：${if (durationMin > 0) "$durationMin 分钟" else "< 1 分钟"}")
                    appendLine("💪 累计做功：${totalTonnage.toInt()} kg 总负荷")
                    appendLine("🎯 正式做组：$workingSets 组正式做组 (热身 $warmupSets 组)")
                    if (stimulatedMuscles.isNotEmpty()) {
                        appendLine("🧬 轰炸部位：${stimulatedMuscles.joinToString(" · ")}")
                    }
                    if (prCount > 0) {
                        appendLine("🎖️ 今日极限突破：${prSetsList.joinToString("、")}")
                    }
                    appendLine("━━━━━━━━━━━━━━━━━━━")
                    appendLine("📋 动作全场实录：")
                    latestSession.exercises.forEachIndexed { idx, ex ->
                        val setsDetail = ex.sets.joinToString(" | ") { s ->
                            "${s.weightKg}k×${s.effectiveReps}" + (if (s.isPr) "👑" else "")
                        }
                        appendLine("${idx + 1}. ${ex.name}: $setsDetail")
                    }
                    appendLine("━━━━━━━━━━━━━━━━━━━")
                    append("「把每一滴汗水，焊进肌肉纤维里。」#Reshape力量训练 #健身打卡")
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(Color(0xFF0D0F12), RoundedCornerShape(18.dp))
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⚡ RESHAPE 纯黑金战报", fontSize = 14.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(text = "真实做组归档", fontSize = 12.sp, color = Color(0xFF71717A))
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = "「把每一滴汗水，焊进肌肉纤维里。」", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "• 分化大纲：$dayCode $planTitle\n• 完成动作：${latestSession.exercises.size} 个动作\n• 正式做组：$workingSets 组 · 总吨位 ${totalTonnage.toInt()} kg\n• 轰炸肌群：${if (stimulatedMuscles.isNotEmpty()) stimulatedMuscles.joinToString(" · ") else "全身核心"}",
                            fontSize = 12.sp,
                            color = Color(0xFFA1A1AA),
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        key(isCopied) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .background(if (isCopied) Color(0xFF059669) else Color(0xFF27272A), RoundedCornerShape(10.dp))
                                    .clickable {
                                        copyToClipboard(storyCardText)
                                        isCopied = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCopied) {
                                    Text(
                                        text = "✓ 已复制小红书/朋友圈打卡文案！",
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "📋 复制老铁专业战报 (朋友圈/小红书)",
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 超量恢复与肌群恢复指导建议
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🧬 超量恢复与营养指导", fontSize = 14.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(text = "生理恢复模型", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "本次 $planTitle 已对主刺激肌群形成充分微损伤。预计在 48~72 小时后肌糖原与肌纤维将达到超量恢复峰值。建议练后 2 小时内补充 30~40g 优质蛋白质并保证 7~8 小时深度睡眠。",
                            fontSize = 12.sp,
                            color = ReshapeTheme.colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 返回首页 CTA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(50.dp)
                        .background(ReshapeTheme.colors.primary, RoundedCornerShape(14.dp))
                        .clickable {
                            pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("home")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "返回首页 ➔", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
