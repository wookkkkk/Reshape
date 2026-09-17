package com.woo.kuiklycomposetemplate.core.utils

actual fun platformExportFile(
    fileName: String,
    content: String,
    mimeType: String
) {
    copyToClipboard(content)
}

actual fun platformImportFile(
    acceptExtensions: List<String>,
    onContentRead: (String) -> Unit
) {
    // iOS 原生环境可通过 UIDocumentPickerViewController 实现
}
