package com.woo.kuiklycomposetemplate.core.components

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.pager.Pager
import com.woo.kuiklycomposetemplate.core.theme.ReshapeTheme

enum class ReshapeNavTab(val title: String, val icon: String, val pageName: String) {
    TRAIN("训练", "⚡", "home"),
    KNOWLEDGE("知识", "📚", "knowledge"),
    HISTORY("记录", "📅", "history"),
    PROFILE("我的", "👤", "profile")
}

@Composable
fun ReshapeBottomBar(
    currentTab: ReshapeNavTab,
    modifier: Modifier = Modifier
) {
    val pager = LocalActivity.current.getPager() as? Pager

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ReshapeTheme.colors.card)
    ) {
        // 顶部精细分割线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFEDEEF4))
        )

        // 4 个核心 Tab 栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReshapeNavTab.entries.forEach { tab ->
                val isSelected = tab == currentTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (!isSelected) {
                                pager?.acquireModule<RouterModule>(RouterModule.MODULE_NAME)?.openPage(tab.pageName)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tab.icon,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.title,
                            fontSize = 12.sp,
                            color = if (isSelected) ReshapeTheme.colors.primary else ReshapeTheme.colors.textFaint
                        )
                    }
                }
            }
        }
    }
}
