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
        // 在多窗口场景下，EditorTabTitleProvider 无法区分是哪个窗口在请求标题
        // 返回 null，完全由 TabNumberFileEditorManagerListener.refreshTabNumber()
        // 通过 TabInfo.setText() 来设置每个窗口的正确编号
        //
        // 这样可以确保：
        // 1. 启动时所有标签显示正确编号（通过 StartupActivity 的 refreshTabNumber）
        // 2. 多窗口/分屏场景下每个窗口独立编号
        // 3. 同一文件在不同窗口显示不同编号
        return null
    }
}
