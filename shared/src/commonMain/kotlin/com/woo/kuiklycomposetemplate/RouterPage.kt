package com.woo.kuiklycomposetemplate

import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.core.annotations.Page
import com.woo.kuiklycomposetemplate.base.BasePager
import com.woo.kuiklycomposetemplate.core.theme.ReshapeTheme
import com.woo.kuiklycomposetemplate.features.home.HomeScreen

/**
 * 默认主路由容器，默认直接渲染 Reshape 今日训练首页
 */
@Page("router", supportInLocal = true)
internal class ComposeRoutePager : BasePager() {

    override fun willInit() {
        super.willInit()
        setContent {
            ReshapeTheme {
                HomeScreen()
            }
        }
    }
}