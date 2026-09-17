package com.woo.kuiklycomposetemplate.core.utils

actual fun platformCurrentTimeMillis(): Long = System.currentTimeMillis()

actual fun copyToClipboard(text: String) {
    // Android native clipboard handled via platform service when context is present
}
