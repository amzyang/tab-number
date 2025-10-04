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

        // 将连接绑定到项目生命周期，项目关闭时自动断开
        project.messageBus.connect(project).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            listener,
        )

        // 在 EDT 线程中主动刷新已打开的标签
        ApplicationManager.getApplication().invokeLater {
            listener.refreshTabNumber()
        }
    }
}
