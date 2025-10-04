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
        // 返回 null，让 TabNumberFileEditorManagerListener.refreshWindowTabNumbers
        // 来处理所有标签编号的更新
        // 这样可以确保每个窗口的编号是独立且正确的
        // TabNumberFileEditorManagerListener 会在文件打开、窗口创建等事件时立即刷新编号
        return null
    }
}
