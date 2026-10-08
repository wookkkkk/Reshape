package com.woo.kuiklycomposetemplate.core.utils

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.FileReader
import org.w3c.files.get

actual fun platformExportFile(
    fileName: String,
    content: String,
    mimeType: String
) {
    try {
        // 1. 尝试微信小程序 API 适配
        val isWeChat = js("typeof wx !== 'undefined' && typeof wx.getFileSystemManager === 'function'") as Boolean
        if (isWeChat) {
            val fs = js("wx.getFileSystemManager()")
            val filePath = js("wx.env.USER_DATA_PATH") as String + "/" + fileName
            fs.writeFile(
                js("({ filePath: filePath, data: content, encoding: 'utf8', success: function() { wx.shareFileMessage({ filePath: filePath, fileName: fileName }); } })")
            )
            return
        }

        // 2. 标准 Web / H5 环境 Blob 下载
        val blob = Blob(arrayOf(content), BlobPropertyBag(type = mimeType))
        val url = js("URL.createObjectURL(blob)") as String
        val anchor = document.createElement("a") as HTMLAnchorElement
        anchor.href = url
        anchor.download = fileName
        anchor.style.display = "none"
        document.body?.appendChild(anchor)
        anchor.click()
        document.body?.removeChild(anchor)
        js("URL.revokeObjectURL(url)")
    } catch (_: Throwable) {
        // 兜底降级为复制到剪贴板
        copyToClipboard(content)
    }
}

actual fun platformImportFile(
    acceptExtensions: List<String>,
    onContentRead: (String) -> Unit
) {
    try {
        // 1. 尝试微信小程序聊天文件选取 API
        val isWeChat = js("typeof wx !== 'undefined' && typeof wx.chooseMessageFile === 'function'") as Boolean
        if (isWeChat) {
            val extensionsArray = acceptExtensions.map { it.removePrefix(".") }.toTypedArray()
            js("""
                wx.chooseMessageFile({
                    count: 1,
                    type: 'file',
                    extension: extensionsArray,
                    success: function(res) {
                        if (res.tempFiles && res.tempFiles.length > 0) {
                            var tempPath = res.tempFiles[0].path;
                            var fs = wx.getFileSystemManager();
                            fs.readFile({
                                filePath: tempPath,
                                encoding: 'utf8',
                                success: function(data) {
                                    onContentRead(data.data);
                                }
                            });
                        }
                    }
                })
            """)
            return
        }

        // 2. 标准 Web / H5 环境 <input type="file">
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = acceptExtensions.joinToString(",")
        input.style.display = "none"

        input.onchange = {
            val files = input.files
            if (files != null && files.length > 0) {
                val file = files[0]
                if (file != null) {
                    val reader = FileReader()
                    reader.onload = { event ->
                        val resultText = event.target.asDynamic().result as? String
                        if (!resultText.isNullOrBlank()) {
                            onContentRead(resultText)
                        }
                    }
                    reader.readAsText(file, "UTF-8")
                }
            }
            document.body?.removeChild(input)
        }

        document.body?.appendChild(input)
        input.click()
    } catch (_: Throwable) {}
}
