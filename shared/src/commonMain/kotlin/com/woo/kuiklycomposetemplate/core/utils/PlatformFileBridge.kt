package com.woo.kuiklycomposetemplate.core.utils

/**
 * 跨端文件导出桥接 (H5 浏览器下载 / 微信小程序 shareFileMessage / 原生系统分享)
 *
 * @param fileName 文件名 (如 "Reshape_训练档案_20260831.reshape")
 * @param content 文件文本内容
 * @param mimeType MIME 类型 (如 "application/json", "text/csv")
 */
expect fun platformExportFile(
    fileName: String,
    content: String,
    mimeType: String = "text/plain"
)

/**
 * 跨端文件导入选择器桥接 (H5 input type=file / 微信小程序 chooseMessageFile / 原生文件管理器)
 *
 * @param acceptExtensions 允许的文件扩展名列表 (如 listOf(".json", ".reshape", ".csv"))
 * @param onContentRead 读取到的文件文本内容回调
 */
expect fun platformImportFile(
    acceptExtensions: List<String>,
    onContentRead: (String) -> Unit
)
