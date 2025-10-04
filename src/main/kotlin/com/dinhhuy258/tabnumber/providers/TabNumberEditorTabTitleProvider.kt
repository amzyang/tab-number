package com.dinhhuy258.tabnumber.providers

import com.dinhhuy258.tabnumber.utils.TabTitleUtils
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.Nullable

class TabNumberEditorTabTitleProvider : EditorTabTitleProvider {
    private val log: Logger = Logger.getInstance("TabNumber")

    @Nullable
    override fun getEditorTabTitle(
        project: Project,
        file: VirtualFile,
    ): String? {
        try {
            val fileEditorManagerEx = FileEditorManagerEx.getInstanceEx(project)
            val currentWindow = fileEditorManagerEx.currentWindow
            val allWindows = fileEditorManagerEx.windows

            // 策略1: 优先使用 currentWindow（新打开/Split 的文件通常在这里）
            if (currentWindow != null) {
                val files = currentWindow.fileList
                val index = files.indexOf(file)
                if (index >= 0) {
                    return TabTitleUtils.generateTabTitle(index, file)
                }
            }

            // 策略2: 文件不在 currentWindow，遍历所有窗口
            // 选择文件数最少的窗口（Split 后的新窗口通常文件较少）
            var bestIndex = -1
            var minFileCount = Int.MAX_VALUE

            for (window in allWindows) {
                val files = window.fileList
                val index = files.indexOf(file)
                if (index >= 0 && files.size < minFileCount) {
                    bestIndex = index
                    minFileCount = files.size
                }
            }

            if (bestIndex >= 0) {
                return TabTitleUtils.generateTabTitle(bestIndex, file)
            }
        } catch (e: Exception) {
            log.warn("Error getting editor tab title for file: ${file.name}", e)
        }

        // 降级：返回 null 让 IntelliJ 使用默认标题
        return null
    }
}
