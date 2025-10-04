package com.dinhhuy258.tabnumber.providers

import com.intellij.openapi.diagnostic.Logger
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
        // 不在此处设置标题，由 TabNumberFileEditorManagerListener 通过 TabInfo.setText() 统一管理
        // 这样可以确保多窗口场景下每个窗口显示正确的编号
        return null
    }
}
