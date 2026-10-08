package com.woo.kuiklycomposetemplate.core.utils

import kotlin.js.Date
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLTextAreaElement

actual fun platformCurrentTimeMillis(): Long = Date.now().toLong()

actual fun copyToClipboard(text: String) {
    try {
        window.navigator.clipboard.writeText(text)
    } catch (_: Throwable) {
        try {
            val textArea = document.createElement("textarea") as HTMLTextAreaElement
            textArea.value = text
            textArea.style.position = "fixed"
            textArea.style.left = "-9999px"
            document.body?.appendChild(textArea)
            textArea.select()
            document.execCommand("copy")
            document.body?.removeChild(textArea)
        } catch (_: Throwable) {}
    }
}
