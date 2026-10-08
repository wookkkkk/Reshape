package com.woo.kuiklycomposetemplate.core.utils

/**
 * 跨端统一当前时间戳获取工具 (毫秒)
 */
expect fun platformCurrentTimeMillis(): Long

/**
 * 跨端剪贴板复制工具
 */
expect fun copyToClipboard(text: String)
