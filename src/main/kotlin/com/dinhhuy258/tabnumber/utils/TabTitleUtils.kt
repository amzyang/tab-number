package com.dinhhuy258.tabnumber.utils

import com.dinhhuy258.tabnumber.settings.TabNumberSettingState
import com.intellij.openapi.vfs.VirtualFile

object TabTitleUtils {
    /**
     * 生成带编号的标签标题
     *
     * @param index 文件在标签列表中的索引（从0开始）
     * @param file 虚拟文件
     * @return 格式化的标签标题，例如 "1. fileName.txt"
     */
    fun generateTabTitle(
        index: Int,
        file: VirtualFile,
    ): String {
        val settingState = TabNumberSettingState.getInstance()
        return (index + 1).toString() +
            settingState.tabNumberSeparator +
            file.presentableName
    }
}
