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
- **UI**:
    - **Jetpack Navigation**: 采用单 Activity 和多 Fragment 的架构。
    - **RikkaX UI 库**: 大量使用 `dev.rikka.rikkax` 系列组件进行 UI 元素构建和主题化，提供了一致的 Material Design 外观。
    - **Material You**: 使用 `dev.rikka.tools.materialthemebuilder` 实现动态主题。
- **数据库**: 原生 **SQLite**，由 `SQLiteOpenHelper` 管理。未使用 Room 等 ORM 框架。
- **构建系统**: **Gradle** 与 Kotlin DSL (`.gradle.kts`)。原生代码使用 **CMake** 构建。

## 2. 代码结构与模块说明

项目遵循标准的安卓应用结构，并做到了清晰的关注点分离。

- **`yangfentuozi.runner.app`**: 客户端应用（UI 层）。
    - **`.ui`**: 包含所有 `Activity` 和 `Fragment` 类，负责用户交互。
        - `MainActivity.kt`: UI 的单一入口点。
        - `fragment/home`: 显示 Shizuku 和后端服务的状态。
        - `fragment/runner`: 用于管理和执行命令的核心 UI。
    - **`.data`**: 处理数据持久化和管理。
        - `DataRepository.kt`: 用于所有数据操作的单例仓库。
        - `database/DataDbHelper.kt`: SQLite 数据库的表结构定义和迁移。
        - `database/CommandDao.kt` & `EnvironmentDao.kt`: 用于操作数据库表的 DAO。
    - **`.app.Runner.kt`**: 一个至关重要的单例对象，管理与 Shizuku 和后端 `IService` 的连接。它扮演着 UI 和服务之间的桥梁角色。

- **`yangfentuozi.runner.server`**: 后端服务逻辑。
    - `ServerMain.kt`: `IService.aidl` 接口的实现。它运行在由 Shizuku 管理的一个独立进程中，命令的实际执行在这里发生。
    - `util/ProcessUtils.kt`: 原生 `processutils` 库的 Kotlin 封装。

- **`yangfentuozi.runner.shared`**: 共享的数据模型。
    - `data/CommandInfo.kt`: 命令的 `Parcelable` 数据类。由客户端和服务端共同使用。
    - `data/EnvInfo.kt`: 环境变量的数据类。

- **`src/main/aidl`**: 包含用于 IPC 的 `AIDL` 接口定义。
    - `server/IService.aidl`: 定义了后端服务暴露给客户端的所有方法（例如 `exec`, `getProcesses`, `installTermExt`）。

- **`src/main/cpp`**: 原生 C 代码。
    - `starter.c`: 一个小型的可执行文件，用于设置用户/组环境并启动 `bash`。它是命令执行的直接入口点。
    - `processutils.c`: 用于获取进程信息和发送信号的 JNI 实现。

## 3. 编码规范与最佳实践

- **仓库模式**: 所有数据访问**必须**通过 `DataRepository.kt` 进行。UI 组件（Fragment、Adapter）不应直接与 DAO 或数据库交互。
- **通过 AIDL 进行 IPC**: 所有与后端服务的交互**必须**通过 `IService` 接口完成，该接口通过 `Runner.service` 对象访问。
- **状态管理**: `Runner.kt` 单例是管理与 Shizuku 和后端服务连接状态的中心点。UI 组件应向 `Runner.kt` 注册监听器以响应状态变化（如服务连接/断开）。
- **数据同步**: 当环境变量在数据库中被修改时，`DataRepository` **必须**调用 `syncToService()` 方法，将更新后的数据推送到后端服务。这确保了执行环境总是最新的。
- **UI 组件**: 使用 `RikkaX` 和 `Material` 组件以保持 UI 的一致性。使用 `BaseDialogBuilder` 创建对话框。
- **线程处理**: 数据库和服务调用是阻塞操作，应在后台线程上执行。`DataRepository` 使用一个单线程执行器来进行服务同步。

## 4. 关键业务逻辑与数据流

### 4.1. 命令执行流程
1.  **用户操作**: 用户在 `RunnerFragment` 中点击一个命令的“运行”按钮。
2.  **对话框**: 显示一个 `ExecDialogFragment` 对话框，它从 `Runner.service` 获取 `IService` 实例。
3.  **IPC 调用**: 对话框调用 `service.exec(command, ...)`，传入命令字符串和一个回调 (`IExecResultCallback`)。
4.  **服务端**: `ServerMain.kt` 接收到调用。它构造一个 `ProcessBuilder` 来运行原生的 `starter` 可执行文件。
5.  **原生执行**: `starter` 可执行文件被运行。它首先设置 UID/GID（如果已指定），然后使用 `execvp` 将自身替换为 `/.../bash`。实际的命令脚本通过管道传递给 `bash` 的标准输入 (`stdin`)。
6.  **输出流**: `ServerMain` 逐行读取 `bash` 进程的标准输出 (`stdout`)。
7.  **回调**: 每一行输出都通过 `IExecResultCallback.onOutput()` 方法发送回客户端（`ExecDialogFragment`），从而实时更新对话框的 UI。
8.  **退出码**: 当进程终止时，退出码通过 `IExecResultCallback.onExit()` 发送回来。

### 4.2. 数据库操作
- `CommandDao` 包含使用 `position` 列来维护命令顺序的复杂逻辑。在添加、删除或移动项目时，它使用 SQL 事务来更新所有受影响行的 `position`，以确保数据的完整性。

### 4.3. Shizuku 服务生命周期
1.  **初始化**: 应用在启动时调用 `Runner.init()`。
2.  **权限与绑定**: `HomeFragment` 检查 Shizuku 状态。如果权限已授予且 Shizuku 正在运行，则调用 `Runner.tryBindService()`。
3.  **服务启动**: Shizuku 在一个独立的、具有特权的进程中启动 `ServerMain` 类。
4.  **连接**: `Runner.kt` 中的 `onServiceConnected` 回调被触发，`service`（一个 `IService` 代理）变得可用。
5.  **断开连接**: 如果服务进程死亡，`onServiceDisconnected` 和 `onBinderDead` 监听器被触发，`Runner.service` 被设为 `null`，并且 UI 会相应地更新。

## 5. 开发约束与注意事项

- **Shizuku 是强制性的**: 本应用从根本上依赖于 Shizuku。如果 Shizuku 未安装、未运行或未授予权限，所有核心功能都将失败。
- **原生依赖**: 项目需要 NDK 来构建。`starter` 和 `processutils` 这两个原生组件对于执行和进程管理至关重要。
- **数据持久化**: 数据库模式虽然简单，但是手动管理的。`DataDbHelper.onUpgrade` 中的迁移是破坏性的（删除并重新创建表）。在更改表结构时要特别小心。
- **UI 与服务解耦**: 保持 UI（`app` 包）和服务（`server` 包）之间的严格分离。它们只能通过 `AIDL` 和 `Parcelable` 数据对象进行通信。