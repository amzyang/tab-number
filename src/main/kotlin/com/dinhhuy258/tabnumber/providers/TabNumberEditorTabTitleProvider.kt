package com.dinhhuy258.tabnumber.providers

import com.dinhhuy258.tabnumber.utils.TabTitleUtils
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider
import com.intellij.openapi.fileEditor.impl.EditorWindow
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
            val currentWindow: EditorWindow = fileEditorManagerEx.currentWindow ?: return null
            val files = currentWindow.fileList

            val index = files.indexOf(file)
            if (index >= 0) {
                return TabTitleUtils.generateTabTitle(index, file)
            }
        } catch (e: Exception) {
            // 降级处理：返回 null 让 IntelliJ 使用默认标题
            log.warn("Error getting editor tab title for file: ${file.name}", e)
            return null
        }

        return null
    }
}
