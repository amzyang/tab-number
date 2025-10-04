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
            val currentWindow = fileEditorManagerEx.currentWindow ?: return null

            // 优先使用 currentWindow 提供初始编号
            // 这样新打开的文件立即显示编号（即使在多窗口场景下可能不准确）
            // refreshTabNumber() 会随后修正所有窗口的编号
            val files = currentWindow.fileList
            val index = files.indexOf(file)
            if (index >= 0) {
                return TabTitleUtils.generateTabTitle(index, file)
            }
        } catch (e: Exception) {
            log.warn("Error getting editor tab title for file: ${file.name}", e)
        }

        return null
    }
}
