package com.dinhhuy258.tabnumber.settings

import com.dinhhuy258.tabnumber.utils.TabTitleUtils
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import javax.swing.JComponent

class TabNumberSettingConfigurable : Configurable {
    private var tabNumberSettingComponent: TabNumberSettingComponent? = null

    override fun getDisplayName(): String {
        return "Tab Number"
    }

    override fun createComponent(): JComponent? {
        tabNumberSettingComponent = TabNumberSettingComponent()
        return tabNumberSettingComponent?.getPanel()
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return tabNumberSettingComponent?.getPreferredFocusedComponent()
    }

    override fun isModified(): Boolean {
        val component = tabNumberSettingComponent ?: return false
        return component.getTabNumberSeparator() != TabNumberSettingState.getInstance().tabNumberSeparator
    }

    override fun apply() {
        val component = tabNumberSettingComponent ?: return
        val settings = TabNumberSettingState.getInstance()
        settings.tabNumberSeparator = component.getTabNumberSeparator()

        // 设置变更后刷新所有打开项目的标签编号
        refreshAllProjectTabs()
    }

    override fun reset() {
        val settings = TabNumberSettingState.getInstance()
        tabNumberSettingComponent?.setTabNumberSeparator(settings.tabNumberSeparator)
    }

    private fun refreshAllProjectTabs() {
        for (project in ProjectManager.getInstance().openProjects) {
            if (project.isDisposed) continue
            val fem = FileEditorManagerEx.getInstanceEx(project)
            for (window in fem.windows) {
                val tabbedPane = window.tabbedPane ?: continue
                val tabs = tabbedPane.tabs
                val files = window.fileList
                for (index in files.indices) {
                    tabs.getTabAt(index)?.setText(TabTitleUtils.generateTabTitle(index, files[index]))
                }
            }
        }
    }
}
