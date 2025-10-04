package com.dinhhuy258.tabnumber.listeners

import com.dinhhuy258.tabnumber.utils.TabTitleUtils
import com.intellij.openapi.Disposable
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
import java.util.concurrent.ConcurrentHashMap

class TabNumberFileEditorManagerListener(private val project: Project) :
    FileEditorManagerListener,
    Disposable {
    private val fileEditorManagerEx: FileEditorManagerEx = FileEditorManagerEx.getInstanceEx(project)
    private val windowListeners: MutableMap<EditorWindow, TabsListener> = ConcurrentHashMap()
    private val log: Logger = Logger.getInstance("TabNumber")

    // 防抖：记录最后一次刷新请求的时间
    @Volatile
    private var lastRefreshTime: Long = 0
    private val debounceMillis: Long = 50 // 50ms 防抖间隔

    override fun fileOpened(
        source: FileEditorManager,
        file: VirtualFile,
    ) {
        // 立即刷新一次
        refreshTabNumber()

        // 延迟再次刷新，确保 Split Right 等异步窗口创建操作完成
        // 使用较短的延迟以减少用户感知的闪烁
        ApplicationManager.getApplication().invokeLater(
            {
                val currentTime = System.currentTimeMillis()
                // 防抖：避免在短时间内重复刷新
                if (currentTime - lastRefreshTime > debounceMillis) {
                    refreshTabNumber()
                    lastRefreshTime = currentTime
                }
            },
            { project.isDisposed },
        )
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
            // 更新刷新时间戳
            lastRefreshTime = System.currentTimeMillis()

            // 清理已失效的窗口监听器
            cleanupDisposedWindows()

            // 遍历所有编辑器窗口
            val windows = fileEditorManagerEx.windows

            // 检测新窗口并立即为其设置监听器
            // 这对于 Split Right 等操作很重要，因为它们可能不触发 fileOpened
            for (window in windows) {
                if (!windowListeners.containsKey(window) && !window.isDisposed) {
                    // 发现新窗口，立即为其添加监听器和刷新标签
                    log.info("Detected new editor window, adding listener")
                    refreshWindowTabNumbers(window)
                }
            }

            // 刷新所有窗口的标签
            for (window in windows) {
                refreshWindowTabNumbers(window)
            }
        } catch (e: Exception) {
            log.error("Error refreshing tab numbers", e)
        }
    }

    private fun cleanupDisposedWindows() {
        val disposedWindows = windowListeners.keys.filter { it.isDisposed }
        disposedWindows.forEach { window ->
            windowListeners.remove(window)
        }
    }

    override fun dispose() {
        // 清理所有窗口监听器引用
        // 注意：IntelliJ Platform 2024.3+ 的 JBTabs 监听器通过弱引用管理，无需手动移除
        windowListeners.clear()
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
                            // 标签移动后立即刷新
                            refreshWindowTabNumbers(window)
                        }

                        override fun selectionChanged(
                            oldSelection: TabInfo?,
                            newSelection: TabInfo?,
                        ) {
                            super.selectionChanged(oldSelection, newSelection)
                            // 选择变化可能伴随窗口切换，刷新所有窗口
                            ApplicationManager.getApplication().invokeLater(
                                {
                                    refreshTabNumber()
                                },
                                { project.isDisposed },
                            )
                        }

                        override fun tabRemoved(tabToRemove: TabInfo) {
                            super.tabRemoved(tabToRemove)
                            // 标签移除后刷新该窗口
                            refreshWindowTabNumbers(window)
                        }

                        override fun beforeSelectionChanged(
                            oldSelection: TabInfo?,
                            newSelection: TabInfo?,
                        ) {
                            super.beforeSelectionChanged(oldSelection, newSelection)
                            // 选择前预刷新，确保标签显示正确
                            refreshWindowTabNumbers(window)
                        }
                    }
                tabs.addListener(listener)
                windowListeners[window] = listener
            }

            // 更新所有标签的编号
            val files = window.fileList
            for (index in files.indices) {
                tabs.getTabAt(index)?.setText(TabTitleUtils.generateTabTitle(index, files[index]))
            }
        } catch (e: Exception) {
            log.error("Error refreshing tab numbers for window", e)
        }
    }
}
