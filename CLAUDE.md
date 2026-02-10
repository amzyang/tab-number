# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

Tab Number 是一个 IntelliJ IDEA 插件，为编辑器标签页添加数字前缀（如 `1. Main.kt`），方便快速识别和切换。

## 技术栈

- **语言**: Kotlin 2.0.21
- **构建工具**: Gradle 8.10.2 + IntelliJ Platform Gradle Plugin 2.9.0
- **目标平台**: IntelliJ IDEA 2024.3+ (build 243+)
- **代码规范**: ktlint 12.1.0
- **JVM**: Java 21（Gradle 通过 Foojay Toolchain Resolver 自动下载，本地有 Java 17+ 即可）

## 常用命令

```bash
./gradlew build              # 构建插件
./gradlew runIde             # 启动带插件的 IDE 实例（可在原 IDE 中断点调试）
./gradlew ktlintCheck        # 代码格式检查
./gradlew ktlintFormat       # 自动格式化代码
./gradlew buildPlugin        # 构建可分发的插件 zip
./gradlew clean              # 清理构建
```

版本号由 `build.gradle` 中的 `version` 字段统一管理，`plugin.xml` 不再包含 `<version>` 标签。

## 核心架构

插件通过两条路径更新标签编号，互为补充：

### 路径一：EditorTabTitleProvider（被动）

`TabNumberEditorTabTitleProvider` 实现 `EditorTabTitleProvider` 接口，IntelliJ 在需要标签标题时调用 `getEditorTabTitle()`。查找策略：先在 `currentWindow` 中找文件索引，找不到则遍历所有窗口，选文件数最少的窗口（Split 后的新窗口通常文件较少）。

### 路径二：事件监听器（主动）

`TabNumberStartupActivity`（`postStartupActivity`）在项目启动时：
1. 创建 `TabNumberFileEditorManagerListener` 并注册到消息总线
2. 同时订阅 `Before` 事件（文件打开前预刷新）
3. 通过 `invokeLater` 主动刷新已打开标签

`TabNumberFileEditorManagerListener` 监听文件打开/关闭/选择变化事件，在 `refreshTabNumber()` 中遍历 `fileEditorManagerEx.windows` 更新所有窗口。每个窗口独立维护一个 `TabsListener`（存储在 `windowListeners: ConcurrentHashMap`），处理标签移动、移除、选择变化等事件。

### 标签标题生成

`TabTitleUtils.generateTabTitle(index, file)` → `"{index+1}{separator}{fileName}"`，分隔符可在设置中自定义。

### 设置持久化

- `TabNumberSettingState`：`PersistentStateComponent`，存储于 `TabNumberPlugin.xml`
- `TabNumberSettingConfigurable` + `TabNumberSettingComponent`：设置 UI（Editor 设置下）
- 目前唯一可配置项：标签编号分隔符（默认 `". "`）

## 关键设计约束

### EDT 线程要求

所有 UI 操作必须在 EDT 中执行。`refreshTabNumber()` 内部已做检查，非 EDT 调用会通过 `invokeLater` 自动切换。新增刷新调用时注意这一点。

### 监听器管理

- 使用 `windowListeners.containsKey(window)` 防止对同一窗口重复注册 `TabsListener`
- `cleanupDisposedWindows()` 在每次刷新时清理已销毁窗口的引用
- Listener 注册为项目的 `Disposable` 子对象，项目关闭时自动清理

### 防抖机制

`fileOpened` 中使用 50ms 防抖间隔 + `invokeLater` 延迟刷新，应对 Split Right 等异步窗口创建操作，减少闪烁。

### IntelliJ 2024.3+ API

- 文件列表：`EditorWindow.fileList`（非已废弃的 `.files`）
- 标签文本：`TabInfo.setText(value)`（非直接赋值 `.text = value`）
- `since-build` 设为 `243`，不设 `untilBuild` 上限

## 调试技巧

- Logger 标签为 `"TabNumber"`，使用 `Logger.getInstance("TabNumber")`
- `runIde` 启动的 IDE 可在原 IDE 中设置断点调试
- 在 `refreshWindowTabNumbers()` 中设断点观察窗口和标签更新流程
- 用 `ApplicationManager.getApplication().isDispatchThread` 验证是否在 EDT 线程
