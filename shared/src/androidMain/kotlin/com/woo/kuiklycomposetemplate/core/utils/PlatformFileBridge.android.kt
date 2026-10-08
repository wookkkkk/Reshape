package com.woo.kuiklycomposetemplate.core.utils

actual fun platformExportFile(
    fileName: String,
    content: String,
    mimeType: String
) {
    // 兜底降级为剪贴板复制
    copyToClipboard(content)
}

actual fun platformImportFile(
    acceptExtensions: List<String>,
    onContentRead: (String) -> Unit
) {
    // Android 原生环境可通过 FilePickerModule 实现
}
