# Tab Number 插件安装指南

## 插件信息

- **版本**: 1.2.0
- **文件**: `build/distributions/tab-number-1.2.0.zip`
- **文件大小**: 1.6 MB
- **支持版本**: IntelliJ IDEA 2024.3+

## 安装方法

### 方式 1: 通过 IntelliJ IDEA 安装（推荐）

1. **打开 IntelliJ IDEA**

2. **进入插件设置**
   - macOS: `IntelliJ IDEA` → `Settings` → `Plugins`
   - Windows/Linux: `File` → `Settings` → `Plugins`

3. **从磁盘安装**
   - 点击设置图标 ⚙️ → 选择 `Install Plugin from Disk...`
   - 浏览并选择: `build/distributions/tab-number-1.2.0.zip`
   - 点击 `OK`

4. **重启 IDE**
   - 点击 `Restart IDE` 按钮
   - 插件将在重启后生效

### 方式 2: 手动复制（高级）

```bash
# macOS/Linux
mkdir -p ~/Library/Application\ Support/JetBrains/IntelliJIdea2024.3/plugins/
unzip build/distributions/tab-number-1.2.0.zip -d ~/Library/Application\ Support/JetBrains/IntelliJIdea2024.3/plugins/

# Windows
unzip build/distributions/tab-number-1.2.0.zip -d %APPDATA%\JetBrains\IntelliJIdea2024.3\plugins\
```

然后重启 IntelliJ IDEA。

## 验证安装

1. **检查插件是否已安装**
   - `Settings` → `Plugins` → `Installed`
   - 查找 "Tab Number" 插件

2. **测试功能**
   - 打开多个文件
   - 查看编辑器标签页，应显示编号（如 "1. Main.java"）
   - 切换标签，编号应保持正确
   - 拖动标签重新排序，编号应实时更新

3. **测试重启后的表现**
   - 关闭 IntelliJ IDEA
   - 重新打开项目
   - 已打开的标签应立即显示编号（修复 Issue #4）

4. **测试多窗口支持**
   - 分屏编辑器 (右键标签 → `Split Right`)
   - 每个窗口的标签都应有独立的编号

## 插件设置

- **位置**: `Settings` → `Editor` → `Tab Number`
- **可配置项**:
  - **Tab Number Separator**: 编号与文件名之间的分隔符（默认: `. `）
  - 例如: 修改为 ` - ` 将显示为 "1 - Main.java"

## 已修复的问题

### Issue #2: 切换标签后编号消失
- ✅ 已修复：现在切换标签时编号会正确保留和更新

### Issue #4: IDE 重启后无编号
- ✅ 已修复：重启后已打开的标签会立即显示编号

### 额外改进
- ✅ 支持多窗口/分屏场景
- ✅ 增强了线程安全性
- ✅ 改进了错误处理

## 卸载插件

1. `Settings` → `Plugins`
2. 找到 "Tab Number"
3. 点击 `Uninstall`
4. 重启 IDE

## 构建命令参考

```bash
# 使用 Java 17+ 构建（Gradle 会自动下载 Java 21）
# macOS/Linux:
export JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home

# Windows:
# set JAVA_HOME=C:\Program Files\Java\jdk-17

# 清理并构建插件
./gradlew clean buildPlugin

# 生成的文件位置
# build/distributions/tab-number-1.2.0.zip
```

**重要说明**:
- **Java 21 自动下载**: Gradle 通过 Foojay Toolchain Resolver 自动下载 Java 21
- **本地 Java 要求**: 只需确保本地有 Java 17+ 来运行 Gradle
- **无需手动安装 Java 21**: 构建系统会自动处理

## 故障排除

### 插件无法加载
- 确认 IntelliJ IDEA 版本 >= 2024.3
- 检查插件是否在 `Settings` → `Plugins` → `Installed` 中启用
- 旧版 IDEA 用户请使用 v1.1.0（支持 2022.1+）

### 编号不显示
1. 重启 IDE
2. 检查设置: `Settings` → `Editor` → `Tab Number`
3. 尝试打开新文件触发刷新

### 报告问题
- GitHub Issues: https://github.com/dinhhuy258/tab-number/issues
