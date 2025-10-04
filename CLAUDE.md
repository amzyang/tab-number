# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

Tab Number 是一个 IntelliJ IDEA 插件,为编辑器标签页添加数字前缀,方便快速识别和切换。

## 技术栈

- **语言**: Kotlin 2.0.21
- **构建工具**: Gradle 8.10.2 with IntelliJ Platform Gradle Plugin 2.9.0
- **目标平台**: IntelliJ IDEA 2024.3+ (build 243+)
- **代码规范**: ktlint 12.1.0
- **JVM 要求**: Java 21 (通过 Gradle Toolchain 自动下载)

## 核心架构

### 插件入口流程

1. **启动阶段** (`TabNumberStartupActivity`)
   - 实现 `StartupActivity` 接口,在项目启动时执行
   - 订阅 `FileEditorManagerListener.FILE_EDITOR_MANAGER` 消息总线
   - 注册 `TabNumberFileEditorManagerListener` 监听器

2. **标签标题提供** (`TabNumberEditorTabTitleProvider`)
   - 实现 `EditorTabTitleProvider` 接口
   - IntelliJ 调用 `getEditorTabTitle()` 获取自定义标签标题
   - 计算文件在当前编辑器窗口中的索引位置
   - 返回格式: `{索引+1}{分隔符}{文件名}`

3. **事件监听与刷新** (`TabNumberFileEditorManagerListener`)
   - 监听文件打开、关闭、选择变化事件
   - 监听标签页移动、移除事件(通过 `TabsListener`)
   - 调用 `refreshTabNumber()` 遍历所有编辑器窗口并动态更新标签
   - 为每个窗口维护独立的 `TabsListener`，存储在 `windowListeners` Map 中
   - **线程安全**: 确保所有 UI 操作在 EDT 线程中执行

### 设置持久化

- `TabNumberSettingState`: 使用 IntelliJ 的 `PersistentStateComponent` 持久化设置
- 配置存储于 `TabNumberPlugin.xml`
- `TabNumberSettingConfigurable` + `TabNumberSettingComponent`: 提供设置 UI(Editor 设置下)

## 常用命令

### 构建与运行

```bash
# 构建插件（使用任意 Java 17+ 版本，Gradle 会自动下载 Java 21）
./gradlew build

# 运行插件(启动带插件的 IDE 实例)
./gradlew runIde

# 代码格式检查
./gradlew ktlintCheck

# 自动格式化代码
./gradlew ktlintFormat

# 构建可分发的插件 zip
./gradlew buildPlugin

# 清理构建
./gradlew clean
```

**重要**:
- Gradle 会通过 Foojay Toolchain Resolver 自动下载 Java 21
- 无需手动安装 Java 21，只需确保本地有 Java 17+ 即可运行 Gradle
- 使用 `env JAVA_HOME=/path/to/java17 ./gradlew` 指定 Gradle 运行的 Java 版本

### 版本更新

插件版本现在由 `build.gradle` 统一管理:
- `build.gradle` 中的 `version` 字段
- `intellijPlatform.pluginConfiguration.version` 自动使用项目版本
- `plugin.xml` 中的 `<version>` 标签已移除（由构建系统自动注入）

## 开发注意事项

### IntelliJ API 使用模式

1. **获取 EditorWindow**
   - 使用 `FileEditorManagerEx.getInstanceEx(project).currentWindow`
   - 必须处理 null 情况(窗口可能未初始化)

2. **标签页操作**
   - 通过 `EditorWindow.tabbedPane?.tabs` 获取 `JBTabs`
   - 使用 `getTabAt(index)` 获取 `TabInfo` 对象
   - 使用 `TabInfo.setText()` 方法更新标签显示（2024.3+ API）

3. **文件列表获取（API 变更）**
   - ❌ 旧 API: `EditorWindow.files` (已废弃)
   - ✅ 新 API: `EditorWindow.fileList` (2024.3+)

4. **延迟初始化**
   - `editorWindow` 和 `openedJBTabs` 使用 `lateinit` + `isInitialized` 检查
   - 仅在第一次调用 `refreshTabNumber()` 时初始化

### 多编辑器窗口支持

实现方案:
- `TabNumberFileEditorManagerListener` 遍历所有 `EditorWindow` 实例
- 使用 `windowListeners: MutableMap<EditorWindow, TabsListener>` 为每个窗口维护独立监听器
- `refreshTabNumber()` 方法调用 `fileEditorManagerEx.windows` 获取所有窗口并逐一更新
- 支持分屏、多窗口等复杂场景，每个窗口独立编号

### plugin.xml 配置要点

- `since-build` 和 `version` 现在由 `build.gradle` 的 `intellijPlatform.pluginConfiguration` 块管理
- `editorTabTitleProvider`: 标签标题的主要扩展点
- `postStartupActivity`: 项目启动后执行的活动
- `applicationConfigurable`: 应用级设置页面
- `applicationService`: 应用级服务(设置状态)

### IntelliJ Platform Gradle Plugin 2.x 配置

项目使用最新的 IntelliJ Platform Gradle Plugin 2.x（替代旧的 1.x）:

1. **插件声明**
   ```groovy
   plugins {
       id 'org.jetbrains.intellij.platform' version '2.9.0'
   }
   ```

2. **仓库配置**
   ```groovy
   repositories {
       mavenCentral()
       intellijPlatform {
           defaultRepositories()
       }
   }
   ```

3. **依赖声明**
   ```groovy
   dependencies {
       intellijPlatform {
           create('IC', '2024.3')  // IntelliJ IDEA Community Edition
           bundledPlugins('com.intellij.java')  // 捆绑插件
           instrumentationTools()  // 代码插桩工具
       }
   }
   ```

4. **插件配置**
   ```groovy
   intellijPlatform {
       pluginConfiguration {
           version = project.version
           ideaVersion {
               sinceBuild = '243'
               untilBuild = provider { null }
           }
       }
   }
   ```

5. **Toolchain 配置**
   - `settings.gradle` 中配置 Foojay Toolchain Resolver
   - `kotlin.jvmToolchain(21)` 指定 JVM 版本
   - Gradle 自动下载所需的 JDK

## 已修复的问题

### Issue #2 & #4 修复 (v1.1.0)
- **问题**: 标签切换后编号消失；IDE 重启后标签无编号
- **根因**:
  - 缓存的 EditorWindow 引用在窗口切换后失效
  - 启动时已存在的标签不触发 `fileOpened` 事件
- **解决方案**:
  - 移除单一窗口缓存，改为遍历所有窗口
  - `TabNumberStartupActivity` 中主动调用初始刷新
  - 支持多窗口/分屏场景

### IntelliJ Platform 2024.3+ 适配 (v1.2.0)
- **API 更新**:
  - `EditorWindow.files` → `EditorWindow.fileList`
  - `TabInfo.text = value` → `TabInfo.setText(value)`
- **构建系统升级**:
  - IntelliJ Platform Gradle Plugin 1.x → 2.9.0
  - Kotlin 1.3 → 2.0.21
  - JVM Target 11 → 21

## 常见陷阱

1. **EDT 线程要求**
   - 所有 UI 操作必须在 EDT (Event Dispatch Thread) 中执行
   - 使用 `ApplicationManager.getApplication().invokeLater {}` 切换到 EDT
   - 非 EDT 线程直接操作 UI 会导致异常或不可预测的行为

2. **监听器重复注册**
   - 使用 `windowListeners.containsKey(window)` 检查避免重复添加 `TabsListener`
   - 重复注册会导致多次回调和性能问题

3. **Null 安全**
   - `currentWindow`, `tabbedPane`, `getTabAt()` 都可能返回 null
   - 必须使用 null 检查或安全调用操作符 `?.`

4. **API 版本兼容**
   - 2024.3+ 使用 `fileList` 和 `setText()`
   - 检查 `since-build` 确保 API 可用性

## 调试技巧

- 使用 `Logger.getInstance("TabNumber")` 记录日志
- 通过 `runIde` 任务启动的 IDE 可以在原 IDE 中断点调试
- 检查 `EditorWindow.fileList` 列表顺序确认标签索引
- 使用 `ApplicationManager.getApplication().isDispatchThread` 验证 EDT 线程
- 在 `refreshWindowTabNumbers()` 中添加断点观察窗口和标签更新流程
