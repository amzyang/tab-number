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
        if (project.isDisposed) return

        // 确保在 EDT 线程中执行
        if (!ApplicationManager.getApplication().isDispatchThread) {
            ApplicationManager.getApplication().invokeLater(
                { refreshTabNumber() },
                { project.isDisposed },
            )
            return
        }

        try {
            // 更新刷新时间戳
            lastRefreshTime = System.currentTimeMillis()

            // 清理已失效的窗口监听器
            cleanupDisposedWindows()

            // 遍历所有编辑器窗口，刷新标签（refreshWindowTabNumbers 内部会处理新窗口的 listener 注册）
            for (window in fileEditorManagerEx.windows) {
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

    /**
     * 创建 Before 监听器，处理文件打开/关闭之前的事件
     * beforeFileOpened 在标签页创建之前触发，可以预初始化窗口状态
     */
    fun createBeforeListener(): FileEditorManagerListener.Before =
        object : FileEditorManagerListener.Before {
            override fun beforeFileOpened(
                source: FileEditorManager,
                file: VirtualFile,
            ) {
                // 文件打开前预刷新，确保窗口监听器已设置
                log.debug("Before file opened: ${file.name}")

                // 立即刷新，为新标签准备环境
                refreshTabNumber()
            }

            override fun beforeFileClosed(
                source: FileEditorManager,
                file: VirtualFile,
            ) {
                // 文件关闭前不需要特殊处理，fileClosed 会处理
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
                            // 标签移动后立即刷新
                            refreshWindowTabNumbers(window)
                        }

                        override fun selectionChanged(
                            oldSelection: TabInfo?,
                            newSelection: TabInfo?,
                        ) {
                            super.selectionChanged(oldSelection, newSelection)
                            // 选择变化仅需刷新当前窗口
                            refreshWindowTabNumbers(window)
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
