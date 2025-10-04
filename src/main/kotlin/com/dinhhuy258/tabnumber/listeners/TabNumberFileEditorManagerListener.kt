package com.dinhhuy258.tabnumber.listeners

import com.dinhhuy258.tabnumber.settings.TabNumberSettingState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.tabs.TabInfo
import com.intellij.ui.tabs.TabsListener

class TabNumberFileEditorManagerListener(private val project: Project) : FileEditorManagerListener {
    private val tabNumberSettingState: TabNumberSettingState = TabNumberSettingState.getInstance()
    private val fileEditorManagerEx: FileEditorManagerEx = FileEditorManagerEx.getInstanceEx(project)
    private val windowListeners: MutableMap<EditorWindow, TabsListener> = mutableMapOf()
    private val log: Logger = Logger.getInstance("TabNumber")

    override fun fileOpened(
        source: FileEditorManager,
        file: VirtualFile,
    ) {
        refreshTabNumber()
    }

    override fun fileClosed(
        source: FileEditorManager,
        file: VirtualFile,
    ) {
        refreshTabNumber()
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        super.selectionChanged(event)
        refreshTabNumber()
    }

    fun refreshTabNumber() {
        // 确保在 EDT 线程中执行
        if (!ApplicationManager.getApplication().isDispatchThread) {
            ApplicationManager.getApplication().invokeLater {
                refreshTabNumber()
            }
            return
        }

        try {
            // 遍历所有编辑器窗口
            val windows = fileEditorManagerEx.windows
            for (window in windows) {
                refreshWindowTabNumbers(window)
            }
        } catch (e: Exception) {
            log.error("Error refreshing tab numbers", e)
        }
    }

    private fun refreshWindowTabNumbers(window: EditorWindow) {
        try {
            val tabbedPane = window.tabbedPane ?: return
            val tabs = tabbedPane.tabs

            // 为窗口添加监听器（如果尚未添加）
            if (!windowListeners.containsKey(window)) {
                val listener =
                    object : TabsListener {
                        override fun tabsMoved() {
                            refreshWindowTabNumbers(window)
                        }

                        override fun selectionChanged(
                            oldSelection: TabInfo?,
                            newSelection: TabInfo?,
                        ) {
                            super.selectionChanged(oldSelection, newSelection)
                            refreshWindowTabNumbers(window)
                        }

                        override fun tabRemoved(tabToRemove: TabInfo) {
                            super.tabRemoved(tabToRemove)
                            refreshWindowTabNumbers(window)
                        }
                    }
                tabs.addListener(listener)
                windowListeners[window] = listener
            }

            // 更新所有标签的编号
            val files = window.files
            for (index in files.indices) {
                val tabInfo = tabs.getTabAt(index)
                if (tabInfo != null) {
                    tabInfo.text = (index + 1).toString() +
                        tabNumberSettingState.tabNumberSeparator +
                        files[index].presentableName
                }
            }
        } catch (e: Exception) {
            log.error("Error refreshing tab numbers for window", e)
        }
    }
}
