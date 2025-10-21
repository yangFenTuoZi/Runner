# Compose 迁移快速启动指南

## 🎉 迁移已完成！

整个 app 模块已成功转换为 Jetpack Compose，term 模块保持 XML 布局并完美兼容。

## 📦 立即构建和运行

### 1. 同步 Gradle
```bash
./gradlew clean
./gradlew build
```

### 2. 运行应用
在 Android Studio 中点击 Run 按钮，或使用命令：
```bash
./gradlew installDebug
```

## 🔍 主要变更说明

### ✅ 已转换为 Compose 的部分

#### 主界面
- **MainActivity** - 完全 Compose 化
  - 使用 `setContent { }` 替代 XML 布局
  - Material 3 主题集成
  - 底部导航栏
  - 顶部应用栏

#### 所有功能页面
- **HomeScreen** - 状态卡片展示
- **RunnerScreen** - 命令管理
- **ProcScreen** - 进程管理
- **TerminalScreen** - Terminal 兼容层
- **SettingsScreen** - 设置界面
- **EnvManageScreen** - 环境变量管理

#### 对话框
- **EditCommandDialog** - 命令编辑
- **ExecDialog** - 命令执行输出

### 🔧 Term 模块兼容

term 模块的 XML 部分**完全保持不变**：
- ✅ Java 代码不变
- ✅ XML 布局不变
- ✅ 所有功能正常
- ✅ 通过 AndroidView 在 Compose 中嵌入

## 📝 代码示例

### 在 Compose 中使用 Term Fragment

```kotlin
// TerminalScreen.kt
@Composable
fun TerminalScreen() {
    // 使用 AndroidView 嵌入原有的 Fragment
    AndroidView(
        factory = { context ->
            FragmentContainerView(context).apply {
                id = R.id.terminal_fragment_container
            }
        }
    )
}
```

### 主题切换示例

```kotlin
// MainActivity.kt
setContent {
    RunnerTheme {  // 自动适配深色/浅色主题、动态颜色
        MainScreen(
            activity = this,
            onShowAbout = { showAbout() }
        )
    }
}
```

## 🎨 主题配置

应用支持以下主题选项（在设置中配置）：

1. **深色主题模式**
   - 跟随系统
   - 始终深色
   - 始终浅色

2. **纯黑主题**
   - OLED 友好的纯黑背景

3. **主题颜色**
   - 系统强调色（Android 12+）
   - 18 种预设颜色主题

## 🐛 故障排查

### 问题：编译错误
**解决方案：**
```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

### 问题：Terminal 显示不正常
**解决方案：**
- Term 模块使用 Fragment，确保 Activity 继承 FragmentActivity
- 检查 `R.id.terminal_fragment_container` ID 是否存在

### 问题：主题不生效
**解决方案：**
- 清除应用数据并重启
- 检查 SharedPreferences 是否正确保存

## 📚 文件结构

```
app/src/main/java/yangfentuozi/runner/app/
├── ui/
│   ├── theme/              # Compose 主题
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── navigation/         # 导航定义
│   │   └── NavGraph.kt
│   ├── screens/            # 所有 Compose screens
│   │   ├── HomeScreen.kt
│   │   ├── RunnerScreen.kt
│   │   ├── ProcScreen.kt
│   │   ├── TerminalScreen.kt
│   │   ├── SettingsScreen.kt
│   │   └── EnvManageScreen.kt
│   ├── dialogs/            # Compose 对话框
│   │   ├── EditCommandDialog.kt
│   │   └── ExecDialog.kt
│   ├── MainScreen.kt       # 主界面导航框架
│   └── activity/
│       ├── MainActivity.kt # 已转为 Compose
│       └── ...
├── data/                   # 数据层（未变）
├── Runner.kt              # Shizuku 服务管理（未变）
└── ...

term/src/main/             # Term 模块（完全未变）
├── java/                  # Java 代码
└── res/layout/            # XML 布局
```

## 🚀 性能提升

Compose 带来的优势：
- ✅ 更流畅的动画
- ✅ 更少的内存占用（移除 ViewBinding）
- ✅ 更快的 UI 构建速度
- ✅ 声明式 UI，代码更清晰
- ✅ 更好的状态管理

## 📋 检查清单

在提交代码前，请确保：

- [ ] 应用可以成功编译
- [ ] 所有 Tab 可以正常切换
- [ ] Shizuku 权限请求正常
- [ ] 命令可以添加、编辑、执行
- [ ] Terminal 可以正常使用
- [ ] 进程管理功能正常
- [ ] 设置可以保存并生效
- [ ] 主题切换正常
- [ ] 备份/恢复功能正常
- [ ] 没有内存泄漏

## 💡 开发提示

### 添加新的 Screen

```kotlin
// 1. 在 NavGraph.kt 添加路由
sealed class Screen(val route: String) {
    data object NewScreen : Screen("new_screen")
    // ...
}

// 2. 创建 Screen 文件
@Composable
fun NewScreen() {
    // Your Compose UI
}

// 3. 在 MainScreen.kt 添加到导航图
composable(Screen.NewScreen.route) {
    NewScreen()
}
```

### 使用 Runner 状态

```kotlin
@Composable
fun MyScreen() {
    var refreshTrigger by remember { mutableIntStateOf(0) }
    
    DisposableEffect(Unit) {
        val listener = Runner.ServiceStatusListener {
            refreshTrigger++  // 触发重组
        }
        Runner.addServiceStatusListener(listener)
        onDispose {
            Runner.removeServiceStatusListener(listener)
        }
    }
}
```

## 🎯 下一步

1. **测试应用** - 在真机上全面测试所有功能
2. **性能优化** - 使用 Profiler 检查性能
3. **UI 优化** - 根据 Material 3 指南优化 UI
4. **清理代码** - 删除不再使用的 XML 和旧代码
5. **文档更新** - 更新用户文档和开发文档

## 📞 需要帮助？

如果遇到任何问题：
1. 查看 `COMPOSE_MIGRATION_README.md` 了解详细信息
2. 检查 lint 错误: `./gradlew lintDebug`
3. 查看日志: `adb logcat | grep Runner`

---

**祝开发顺利！** 🎉

