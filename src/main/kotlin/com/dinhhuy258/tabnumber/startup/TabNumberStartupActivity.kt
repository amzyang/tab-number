package com.dinhhuy258.tabnumber.startup

import com.dinhhuy258.tabnumber.listeners.TabNumberFileEditorManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.Disposer

class TabNumberStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        val listener = TabNumberFileEditorManagerListener(project)

        // 将 listener 注册为项目的 Disposable 子对象，项目关闭时自动 dispose
        Disposer.register(project, listener)

        val connection = project.messageBus.connect(project)

        // 订阅常规文件编辑器事件（异步）
        connection.subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            listener,
        )

        // 订阅 Before 事件（文件打开/关闭之前）
        // 这些事件在标签创建前触发，可以预初始化状态
        connection.subscribe(
            FileEditorManagerListener.Before.FILE_EDITOR_MANAGER,
            listener.createBeforeListener(),
        )

        // 在 EDT 线程中主动刷新已打开的标签
        // 由于 TabNumberEditorTabTitleProvider 现在返回 null
        // 我们必须确保启动时所有标签都能立即获得编号
        ApplicationManager.getApplication().invokeLater {
            // 第一次刷新
            listener.refreshTabNumber()

            // 快速连续刷新，确保所有标签都获得编号
            ApplicationManager.getApplication().invokeLater(
                {
                    listener.refreshTabNumber()

                    // 第三次刷新
                    ApplicationManager.getApplication().invokeLater(
                        {
                            listener.refreshTabNumber()

                            // 第四次刷新，处理延迟加载的窗口
                            ApplicationManager.getApplication().invokeLater(
                                {
                                    listener.refreshTabNumber()

                                    // 最后一次刷新，确保所有异步操作完成
                                    ApplicationManager.getApplication().invokeLater(
                                        {
                                            listener.refreshTabNumber()
                                        },
                                        { project.isDisposed },
                                    )
                                },
                                { project.isDisposed },
                            )
                        },
                        { project.isDisposed },
                    )
                },
                { project.isDisposed },
            )
        }
    }
}
