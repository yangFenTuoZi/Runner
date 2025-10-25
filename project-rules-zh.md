# Runner 安卓应用项目规则

本文档旨在为AI编程助手提供对 Runner 安卓应用的架构、编码规范和关键逻辑的全面概述，以便更好地理解项目规则和上下文。

## 1. 项目概述与技术栈

**Runner** 是一款安卓应用，其核心功能是利用 **Shizuku** 框架来执行具有提升权限的 Shell 命令。它允许用户在一个持久化且可配置的环境中管理、执行命令，并为命令创建快捷方式。

### 1.1. 核心功能
- **命令执行**: 通过一个在 Shizuku 权限下运行的后端服务来执行 Shell 命令。
- **权限管理**: 利用 Shizuku 来运行具有更高权限的进程。
- **持久化命令列表**: 用户可以保存、编辑和重新排序一个命令列表。
- **环境变量**: 支持自定义环境变量，这些变量会被传递给执行命令的 Shell 环境。
- **桌面快捷方式**: 为命令创建桌面快捷方式，实现一键执行。
- **终端扩展**: 支持从 zip 文件安装一个 `usr` 文件系统，以扩展命令行环境。
- **备份与恢复**: 提供备份和恢复命令列表及环境设置的功能。

### 1.2. 技术栈
- **语言**: 主要为 **Kotlin**，包含用 **C** 编写的原生组件 (JNI)。
- **核心框架**: **Shizuku** (`dev.rikka.shizuku:api`) 是获取权限以运行后端服务的关键。
- **架构**:
    - **客户端-服务端 IPC**: 使用 `AIDL` 在主应用进程（客户端）和 Shizuku 服务进程（服务端）之间进行通信。
    - **仓库模式 (Repository Pattern)**: 一个 `DataRepository` 单例作为所有应用数据的唯一真实来源 (Single Source of Truth)。
    - **原生接口 (JNI)**: 用于底层操作，如读取 `/proc` 文件系统和向进程发送信号。
- **UI**: **Jetpack Compose** + **MVVM** 架构。使用 Material 3 设计，`StateFlow` 响应式更新，支持动态主题和暗黑模式
- **数据库**: 原生 **SQLite**，由 `SQLiteOpenHelper` 管理。未使用 Room 等 ORM 框架。
- **构建系统**: **Gradle** 与 Kotlin DSL (`.gradle.kts`)。原生代码使用 **CMake** 构建。

## 2. 代码结构与模块说明

项目遵循标准的安卓应用结构，并做到了清晰的关注点分离。

### app 模块（客户端和服务端的主体部分）

- **`yangfentuozi.runner.app`**: 客户端应用（UI 层）。
    - **`.ui`**: Compose UI 实现，MVVM 架构。
        - `activity/`: Activity 容器（`MainActivity`、`ExecShortcutActivity` 等）。
        - `screens/`: Compose 屏幕（`HomeScreen`、`RunnerScreen`、`TerminalScreen`、`ProcScreen`、`SettingsScreen` 等）。
        - `components/`: 可复用 UI 组件（`BeautifulCard`、`ExecDialog` 等）。
        - `viewmodels/`: ViewModel 层，管理 UI 状态（对应各 Screen 的 ViewModel）。
        - `theme/`: 主题系统（`Theme.kt`、`ThemeManager.kt`）。
    - **`.data`**: 处理数据持久化和管理。
        - `DataRepository.kt`: 用于所有数据操作的单例仓库。
        - `database/DataDbHelper.kt`: SQLite 数据库的表结构定义和迁移。
        - `database/CommandDao.kt` & `EnvironmentDao.kt`: 用于操作数据库表的 DAO。
    - **`.app.Runner.kt`**: 一个至关重要的单例对象，管理与 Shizuku 和后端 `IService` 的连接。它扮演着 UI 和服务之间的桥梁角色。

- **`yangfentuozi.runner.server`**: 后端服务逻辑。
    - `ServerMain.kt`: `IService.aidl` 接口的实现。它运行在由 Shizuku 管理的一个独立进程中，命令的实际执行在这里发生。
    - `util/ProcessUtils.kt`: 原生 `processutils` 库的 Kotlin 封装。
    - `util/ExecUtils.kt`: 原生 `executils` 库的 Kotlin 封装。

- **`yangfentuozi.runner.shared`**: 共享的数据模型。
    - `data/CommandInfo.kt`: 命令的 `Parcelable` 数据类。由客户端和服务端共同使用。
    - `data/EnvInfo.kt`: 环境变量的数据类。
    - `data/ProcessInfo.kt`: 进程信息的 `Parcelable` 数据类。由服务端生成返回给客户端处理，客户端可处理的操作：根据进程列表分析某进程的进程树信息、请求服务端利用特权杀死指定某(些)进程。
    - `data/TermExtVersion.kt`: 服务端返回给客户端的终端扩展包版本数据信息。

- **`src/main/aidl`**: 包含用于 IPC 的 `AIDL` 接口定义。
    - `server/IService.aidl`: 定义了后端服务暴露给客户端的所有方法（例如 `exec`, `getProcesses`, `installTermExt`）。
    - `callback/IExitCallback.aidl`：定义了一个回调接口，用于在后台耗时操作执行完成时通知客户端退出码，以及返回在执行耗时操作时在极端情况下导致的崩溃信息。

- **`src/main/cpp`**: 原生 C 代码。
    - `executils.c`: 用于执行命令的 JNI 实现。
    - `processutils.c`: 用于获取进程信息和发送信号的 JNI 实现。

### emulatorview 模块（VT100 终端模拟器）

**emulatorview** 是一个独立的终端模拟器库，基于 Android Open Source Project，实现了 VT100/xterm 终端仿真。

- **`jackpal.androidterm.emulatorview`** 包：
    - `EmulatorView`: 自定义 View，负责渲染终端屏幕、处理触摸/键盘输入、滚动和文本选择。
    - `TerminalEmulator`: VT100 终端仿真核心，解析和处理 ANSI 转义序列，维护屏幕缓冲区。
    - `TermSession`: 终端会话抽象，管理输入/输出流、读写线程、字符编码转换（UTF-8）。
    - `TranscriptScreen` / `Screen`: 屏幕缓冲区实现，支持滚动历史记录。
    - `TextRenderer` / `PaintRenderer`: 文本渲染引擎，支持颜色方案和字体样式。
    - `ColorScheme`: 终端颜色方案配置。
    - `TermKeyListener`: 键盘输入监听器。

### term 模块（客户端终端 UI）

**term** 模块基于 emulatorview，提供完整的终端 UI 和与 rish 服务的集成。

- **`yangfentuozi.runner.app.ui.fragment.terminal`** 包：
    - `Term.java`: 终端 Fragment，管理多个终端会话、上下文菜单、软键盘。
    - `RishTermSession.java`: 集成 `RishService` 的终端会话实现。
        - 通过 `Os.pipe()` 创建管道连接客户端和服务端。
        - 调用 `RishService.createHost()` 在服务端创建 PTY 和进程。
        - 双向数据传输：客户端管道 ↔ 服务端 PTY ↔ Shell 进程。
        - 监听进程退出并更新 UI。
    - `TermService.java`: 后台服务，保持终端会话在后台运行，显示通知。
    - `TermViewFlipper`: ViewFlipper 容器，支持多个终端窗口切换。
    - `TermView.java`: EmulatorView 的扩展，添加手势和输入法支持。
    - `util/SessionList.java`: 管理所有终端会话的列表。
    - `util/TermSettings.java`: 终端设置（字体、颜色、快捷键等）。

- **数据流**:
    1. 用户输入 → `EmulatorView` → `TermSession.write()` → 管道 write end
    2. 服务端从管道 read end 读取 → PTY master → Shell 进程
    3. Shell 输出 → PTY master → 服务端写入管道 write end
    4. 客户端从管道 read end 读取 → `TermSession` 读线程 → `TerminalEmulator.append()` → `EmulatorView` 刷新

### rish 模块（服务端的终端实现部分）

**rish** 是一个独立的库模块，提供了伪终端（PTY）和进程管理功能，用于在服务端运行交互式 Shell。

- **`rikka.rish`** 包：
    - `IRishService.aidl`: 定义了伪终端服务的 AIDL 接口。
        - `createHost()`: 创建一个新的终端会话（fork 进程并配置 PTY）。
        - `setWindowSize()`: 设置终端窗口大小。
        - `getExitCode()`: 获取进程退出码。
        - `getAllHost()` / `releaseHost()`: 管理所有终端会话。
    - `RishService.java`: `IRishService` 的实现，管理多个 `RishHost` 实例。
    - `RishHost.java`: 单个终端会话的 Java 层封装，负责调用 JNI 创建进程和 PTY。
    - `RishConfig.java`: 配置类，用于初始化 JNI 库和获取服务实例。

- **原生代码（C++）**:
    - `rikka_rish_RishHost.cpp`: 核心 JNI 实现。
        - `start()`: fork 子进程，配置 PTY/管道，执行 Shell（`execvp`/`execvpe`）。
        - 异步 I/O 传输：使用多线程在 stdin/stdout/stderr 和 PTY/管道间传输数据。
        - `waitFor()`: 等待进程退出并返回退出码。
    - `pts.cpp`: 伪终端（PTY）相关工具函数。
        - `open_ptmx()`: 打开主伪终端设备。
        - `transfer_async()`: 异步数据传输线程。
        - TTY 模式管理（raw mode）。

- **与主应用的集成**:
    - `ServerMain` 在初始化时创建 `RishService` 实例。
    - `IService.shellService` 返回 `RishService` 的 Binder，供终端模块使用。
    - `RishBinderHolder`（term 模块）持有 `IRishService` 引用，供终端 UI 调用。

## 3. 编码规范与最佳实践

- **仓库模式**: 所有数据访问**必须**通过 `DataRepository.kt` 进行。UI 层（Screen、ViewModel）不应直接与 DAO 交互。
- **通过 AIDL 进行 IPC**: 所有与后端服务的交互**必须**通过 `IService` 接口完成，该接口通过 `Runner.service` 对象访问。
- **状态管理**: `Runner.kt` 单例管理 Shizuku 和服务连接状态。ViewModel 通过注册监听器响应状态变化。
- **数据同步**: 环境变量修改后，`DataRepository` **必须**调用 `syncToService()` 同步到后端。
- **UI 架构 (MVVM + Compose)**:
    - UI 状态通过 `StateFlow` 从 ViewModel 流向 Composable，用户事件通过回调处理。
    - ViewModel 管理状态和业务逻辑，Screen 仅负责 UI 渲染。
    - 对话框状态由 ViewModel 的 `StateFlow<Boolean>` 管理。
    - 主应用使用 Compose Navigation，5 个主页面通过底部导航栏切换。
    - 所有 Activity 继承 `BaseActivity`，独立功能使用独立 Activity。
- **主题系统**: `ThemeManager` 管理三种模式（LIGHT/DARK/SYSTEM），通过 `StateFlow` 响应式更新，支持 Material You 动态颜色和纯黑主题。
- **线程处理**: 
    - 数据库和服务调用**必须**在 `Dispatchers.IO` 执行。
    - ViewModel 使用 `viewModelScope`，Composable 使用 `rememberCoroutineScope()`。

## 4. 关键业务逻辑与数据流

### 4.1. 命令执行流程
1.  **用户操作**: 用户在 `RunnerScreen` 中点击一个命令的“运行”按钮。
2.  **对话框**: 显示一个 `ExecDialog` 对话框，它从 `Runner.service` 获取 `IService` 实例。
3.  **IPC 调用**: 对话框调用 `service.exec(procName, command, ...)`，传入自定义进程名标记字符串、命令字符串、一个回调 (`IExitCallback`)、输出信息管道（`ParcelFileDescriptor`）。
4.  **服务端**: `ServerMain.kt` 接收到调用。它调用 `ExecUtils` 以通过 JNI 创建进程，传入 bash 路径、自定义进程名标记字符串、输出信息管道的原始 fd 值、环境信息、临时创建的用于告知 bash 的执行的命令字符串的管道 fd。
5.  **原生执行**: `executils` JNI 库文件被运行。它首先 fork 子进程，然后在子进程里设置输入流输出流，然后使用 `execve` 将自身替换为传入的 bash，实际的命令脚本通过管道传递给 `bash` 的标准输入。
6.  **输出流**: `executils` fork 出的 `bash` 进程的标准输出 (`stdout`) 持续重定向回客户端。
7.  **回调**: 客户端（`ExecDialog`）实时读取之前传入的管道信息，并实时更新对话框的 UI。
8.  **退出码**: 当进程终止时，退出码通过 `IExitCallback.onExit()` 发送回来。

### 4.2. 数据库操作
- `CommandDao` 包含使用 `position` 列来维护命令顺序的复杂逻辑。在添加、删除或移动项目时，它使用 SQL 事务来更新所有受影响行的 `position`，以确保数据的完整性。

### 4.3. Shizuku 服务生命周期
1.  **初始化**: 应用在启动时调用 `Runner.init()`。
2.  **权限与绑定**: `HomeScreen` 检查 Shizuku 状态。如果权限已授予且 Shizuku 正在运行，则调用 `Runner.tryBindService()`。
3.  **服务启动**: Shizuku 在一个独立的、具有特权的进程中启动 `ServerMain` 类。
4.  **连接**: `Runner.kt` 中的 `onServiceConnected` 回调被触发，`service`（一个 `IService` 代理）变得可用。
5.  **断开连接**: 如果服务进程死亡，`onServiceDisconnected` 和 `onBinderDead` 监听器被触发，`Runner.service` 被设为 `null`，并且 UI 会相应地更新。

### 4.4. UI 数据流模式
- **数据加载**: ViewModel 在 `viewModelScope` + `Dispatchers.IO` 中调用 Repository → 更新 `StateFlow` → UI 通过 `collectAsState()` 自动重组。
- **用户交互**: 用户操作 → Screen 调用 ViewModel 方法 → ViewModel 更新 StateFlow → UI 响应变化（如显示对话框）。
- **监听器模式**: ViewModel 在 `init` 中向 `Runner` 注册监听器，在 `onCleared()` 中移除。状态变化时触发 `StateFlow` 更新，驱动 UI 刷新。
- **对话框管理**: ViewModel 用 `MutableStateFlow<Boolean>` 控制显示，Screen 根据状态条件渲染对话框。

## 5. 开发约束与注意事项

- **Shizuku 是强制性的**: 本应用从根本上依赖于 Shizuku。如果 Shizuku 未安装、未运行或未授予权限，所有核心功能都将失败。
- **原生依赖**: 项目需要 NDK 来构建。`executils`、 `processutils` 和 `rish` 这几个原生组件对于执行、进程管理、终端至关重要。
- **数据持久化**: 数据库模式虽然简单，但是手动管理的。`DataDbHelper.onUpgrade` 中的迁移是破坏性的（删除并重新创建表）。在更改表结构时要特别小心。
- **UI 与服务解耦**: 保持 UI（`app` 包）和服务（`server` 包）之间的严格分离。它们只能通过 `AIDL` 和 `Parcelable` 数据对象进行通信。
- **UI 开发注意事项**:
    - Composable 应无状态，业务状态在 ViewModel 中管理。
    - LazyColumn/LazyRow 的 items 必须提供稳定的 key。
    - 使用 `LocalContext.current` 获取 Context，`stringResource()` 获取字符串。
    - ViewModel 监听器在 `init` 注册，`onCleared()` 移除。
    - StateFlow 更新线程安全，但复杂对象需创建新实例。
- **代码规范**:
    - 命名：Screen/ViewModel/Dialog 使用对应后缀。
    - StateFlow：私有 `_stateName`，公开 `stateName`。
    - Composable 参数顺序：必需参数 → 可选参数 → Modifier → 回调 → lambda。
    - 保证代码连贯性、可读性。
