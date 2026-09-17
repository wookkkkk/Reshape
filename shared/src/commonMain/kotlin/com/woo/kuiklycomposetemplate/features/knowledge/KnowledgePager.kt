package com.woo.kuiklycomposetemplate.features.knowledge

import androidx.compose.runtime.Composable
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
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager
import com.woo.kuiklycomposetemplate.core.components.ReshapeBottomBar
import com.woo.kuiklycomposetemplate.core.components.ReshapeNavTab
import com.woo.kuiklycomposetemplate.core.theme.ReshapeTheme

@Page("knowledge", supportInLocal = true)
class KnowledgePager : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ReshapeTheme {
                KnowledgeScreen()
            }
        }
    }
}

@Composable
fun KnowledgeScreen() {
    val pager = LocalActivity.current.getPager() as? Pager

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "理论体系与进阶指引", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)
                    Text(text = "力量训练知识库", fontSize = 22.sp, color = ReshapeTheme.colors.textPrimary)
                }
                Box(
                    modifier = Modifier
                        .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = "科学力量", fontSize = 11.sp, color = ReshapeTheme.colors.primary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 离线与内容同步状态
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📡 内容同步与离线保障", fontSize = 14.sp, color = ReshapeTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                        Text(text = "manifest v104", fontSize = 10.sp, color = ReshapeTheme.colors.textFaint)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Box(modifier = Modifier.background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(text = "本地预置底包", fontSize = 10.sp, color = ReshapeTheme.colors.primary)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.background(Color(0xFFECFDF5), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(text = "弱网无感降级", fontSize = 10.sp, color = Color(0xFF047857))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "官方计划与训练原则 100% 本地预置，健身房地下室飞行模式也能秒开；支持弱网下无感运行。",
                        fontSize = 12.sp,
                        color = ReshapeTheme.colors.textSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 动作库与原则库金刚双入口
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // 动作库大卡片
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                        .clickable {
                            pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("exercise_library")
                        }
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📚 动作库", fontSize = 15.sp, color = ReshapeTheme.colors.textPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(ReshapeTheme.colors.primarySoft, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(text = "24+动作", fontSize = 9.sp, color = ReshapeTheme.colors.primary)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "查动作/口诀/无痛变式", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 训练原则库大卡片
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                        .clickable {
                            pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("principles")
                        }
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📖 原则库", fontSize = 15.sp, color = ReshapeTheme.colors.textPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFECFDF5), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(text = "5大原则", fontSize = 9.sp, color = Color(0xFF047857))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "RPE8/双重递进/减载", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 模块 1：官方计划大纲与蓝图
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "📋 官方计划大纲与单日蓝图", fontSize = 15.sp, color = ReshapeTheme.colors.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "基于经典三分化黄金模型", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)

                    Spacer(modifier = Modifier.height(12.dp))

                    // 大纲入口
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("plan")
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "三分化总纲与 15 个动作全览", fontSize = 14.sp, color = ReshapeTheme.colors.textPrimary)
                            Text(text = "推/拉/腿三天一轮 · 主项线性递进 · 辅项泵感", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                        }
                        Text(text = "›", fontSize = 18.sp, color = ReshapeTheme.colors.textFaint)
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF3F4F6)))

                    // D1 蓝图
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val data = JSONObject().apply { put("day", "push") }
                                pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("day_blueprint", data)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "D1 · 推日 5 个动作蓝图详解", fontSize = 14.sp, color = ReshapeTheme.colors.textPrimary)
                            Text(text = "卧推波浪递进 + 上斜哑铃 + 双杠臂屈伸", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                        }
                        Text(text = "›", fontSize = 18.sp, color = ReshapeTheme.colors.textFaint)
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF3F4F6)))

                    // D2 蓝图
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val data = JSONObject().apply { put("day", "pull") }
                                pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("day_blueprint", data)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "D2 · 拉日 5 个动作蓝图详解", fontSize = 14.sp, color = ReshapeTheme.colors.textPrimary)
                            Text(text = "高位下拉 + 坐姿划船 + 二头肌峰超级组", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                        }
                        Text(text = "›", fontSize = 18.sp, color = ReshapeTheme.colors.textFaint)
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF3F4F6)))

                    // D3 蓝图
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val data = JSONObject().apply { put("day", "legs") }
                                pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("day_blueprint", data)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "D3 · 腿日 5 个动作蓝图详解", fontSize = 14.sp, color = ReshapeTheme.colors.textPrimary)
                            Text(text = "杠铃深蹲主项 + 保加利亚单腿蹲弱侧平衡", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                        }
                        Text(text = "›", fontSize = 18.sp, color = ReshapeTheme.colors.textFaint)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 模块 2：五大核心训练原则
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⚡ 力量训练五大底层原则", fontSize = 15.sp, color = ReshapeTheme.colors.textPrimary, modifier = Modifier.weight(1f))
                        Text(
                            text = "查看全文 ›",
                            fontSize = 12.sp,
                            color = ReshapeTheme.colors.primary,
                            modifier = Modifier.clickable {
                                pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("principles")
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. RPE 8 严控力竭：保留 2 次余力，兼顾高容量与神经系统恢复\n2. 渐进超负荷法则：重量/次数/组数阶梯递进\n3. 弱侧优先：单侧动作以弱侧为准，消除肌力代偿\n4. 8 次周期与减载周：蓄力突破停滞平台期",
                        fontSize = 12.sp,
                        color = ReshapeTheme.colors.textSecondary,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 模块 3：科学推算与建议重量推算依据 (对齐 knowledge.html)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(ReshapeTheme.colors.card, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "🧮 科学推算与建议重量依据", fontSize = 15.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "三大项 1RM 与动作建议重量底层数学模型", fontSize = 12.sp, color = ReshapeTheme.colors.textSecondary)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage("one_rep_max")
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Brzycki 1RM 极限推算公式与推算依据", fontSize = 14.sp, color = ReshapeTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "用 3~5 次的做组重量无损反推极限 · 建议重量 5 大推算规则", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                        }
                        Text(text = "计算依据 ›", fontSize = 12.sp, color = ReshapeTheme.colors.primary, fontWeight = FontWeight.Bold)
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF3F4F6)))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "单侧动作弱侧保护原则", fontSize = 14.sp, color = ReshapeTheme.colors.textPrimary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "分腿蹲/单臂划船以弱侧为准，防止肌力与体态代偿失衡", fontSize = 11.sp, color = ReshapeTheme.colors.textSecondary)
                        }
                        Text(text = "理论已内嵌", fontSize = 11.sp, color = ReshapeTheme.colors.textFaint)
                    }
                }
            }

            Spacer(modifier = Modifier.height(90.dp))
        }
    }

    ReshapeBottomBar(
        currentTab = ReshapeNavTab.KNOWLEDGE,
        modifier = Modifier.align(Alignment.BottomCenter)
    )
    }
}
