# Project Rules for Runner Android App

This document provides a comprehensive overview of the Runner Android application's architecture, coding conventions, and key logic. It is intended for AI programming assistants to understand the project's rules and context.

## 1. Project Overview & Technology Stack

**Runner** is an Android application designed to execute shell commands with elevated privileges, leveraging the **Shizuku** framework. It allows users to manage, execute, and create shortcuts for commands in a persistent and configurable environment.

### 1.1. Core Features
- **Command Execution**: Execute shell commands via a backend service running with Shizuku's privileges.
- **Privilege Management**: Utilizes Shizuku for running processes with higher permissions.
- **Persistent Command List**: Users can save, edit, and reorder a list of commands.
- **Environment Variables**: Support for custom environment variables that are passed to the execution shell.
- **Desktop Shortcuts**: Create home screen shortcuts for one-click command execution.
- **Terminal Extension**: Supports installing a `usr` file system from a zip file to extend the command-line environment.
- **Backup & Restore**: Provides functionality to back up and restore the command list and environment settings.

### 1.2. Technology Stack
- **Language**: Primarily **Kotlin**, with native components written in **C** (JNI).
- **Core Framework**: **Shizuku** (`dev.rikka.shizuku:api`) is essential for acquiring permissions to run the backend service.
- **Architecture**:
    - **Client-Server IPC**: Uses `AIDL` for communication between the main app process (client) and the Shizuku service process (server).
    - **Repository Pattern**: A `DataRepository` singleton acts as the single source of truth for all application data.
    - **Native Interface**: **JNI** is used for low-level operations like reading `/proc` filesystem and sending signals to processes.
- **UI**:
    - **Jetpack Navigation**: Single-Activity architecture with multiple Fragments.
    - **RikkaX UI Libraries**: Heavily uses `dev.rikka.rikkax` components for UI elements and theming, providing a consistent Material Design look and feel.
    - **Material You**: Implements dynamic theming using `dev.rikka.tools.materialthemebuilder`.
- **Database**: Native **SQLite** managed by `SQLiteOpenHelper`. No ORM like Room is used.
- **Build System**: **Gradle** with Kotlin DSL (`.gradle.kts`). Native code is built using **CMake**.

## 2. Code Structure & Module Description

The project is organized into a standard Android app structure with clear separation of concerns.

- **`yangfentuozi.runner.app`**: The client-side application (UI layer).
    - **`.ui`**: Contains all `Activity` and `Fragment` classes, responsible for user interaction.
        - `MainActivity.kt`: The single entry point of the UI.
        - `fragment/home`: Displays the status of Shizuku and the backend service.
        - `fragment/runner`: The core UI for managing and executing commands.
    - **`.data`**: Handles data persistence and management.
        - `DataRepository.kt`: Singleton repository for all data operations.
        - `database/DataDbHelper.kt`: SQLite database schema and migration.
        - `database/CommandDao.kt` & `EnvironmentDao.kt`: DAOs for database table operations.
    - **`.app.Runner.kt`**: A crucial singleton object that manages the connection to Shizuku and the backend `IService`. It acts as the bridge between the UI and the service.

- **`yangfentuozi.runner.server`**: The backend service logic.
    - `ServerMain.kt`: The implementation of the `IService.aidl` interface. It runs in a separate process managed by Shizuku. This is where commands are actually executed.
    - `util/ProcessUtils.kt`: A Kotlin wrapper for the native `processutils` library.

- **`yangfentuozi.runner.shared`**: Shared data models.
    - `data/CommandInfo.kt`: The `Parcelable` data class for a command. Used by both the app and the server.
    - `data/EnvInfo.kt`: The data class for an environment variable.

- **`src/main/aidl`**: Contains `AIDL` interface definitions for IPC.
    - `server/IService.aidl`: Defines all methods the backend service exposes to the client (e.g., `exec`, `getProcesses`, `installTermExt`).

- **`src/main/cpp`**: Native C code.
    - `starter.c`: A small executable that sets up the user/group environment and launches `bash`. It is the direct entry point for command execution.
    - `processutils.c`: JNI implementation for fetching process information and sending signals.

## 3. Coding Conventions & Best Practices

- **Repository Pattern**: All data access MUST go through `DataRepository.kt`. UI components (Fragments, Adapters) should not interact with DAOs or the database directly.
- **IPC via AIDL**: All interactions with the backend service MUST be done through the `IService` interface, accessed via the `Runner.service` object.
- **State Management**: The `Runner.kt` singleton is the central point for managing the connection state with Shizuku and the backend service. UI components should register listeners with `Runner.kt` to react to state changes (e.g., service connected/disconnected).
- **Data Synchronization**: When environment variables are modified in the database, `DataRepository` MUST call `syncToService()` to push the updated data to the backend service. This ensures the execution environment is always up-to-date.
- **UI Components**: Use `RikkaX` and `Material` components to maintain UI consistency. Use `BaseDialogBuilder` for creating dialogs.
- **Threading**: Database and service calls are blocking operations. They should be performed on background threads. `DataRepository` uses a single-thread executor for service synchronization.

## 4. Key Business Logic & Data Flow

### 4.1. Command Execution Flow
1.  **User Action**: User clicks the "run" button for a command in `RunnerFragment`.
2.  **Dialog**: An `ExecDialogFragment` is shown, which gets the `IService` instance from `Runner.service`.
3.  **IPC Call**: The dialog calls `service.exec(command, ...)`, passing the command string and a callback (`IExecResultCallback`).
4.  **Service-Side**: `ServerMain.kt` receives the call. It constructs a `ProcessBuilder` to run the native `starter` executable.
5.  **Native Execution**: The `starter` executable is run. It first sets the UID/GID if specified, then uses `execvp` to replace itself with `/.../bash`. The actual command script is piped to `bash`'s `stdin`.
6.  **Output Streaming**: `ServerMain` reads the `stdout` of the `bash` process line-by-line.
7.  **Callback**: Each line of output is sent back to the client (the `ExecDialogFragment`) via the `IExecResultCallback.onOutput()` method, which updates the dialog's UI in real-time.
8.  **Exit Code**: When the process terminates, the exit code is sent back via `IExecResultCallback.onExit()`.

### 4.2. Database Operations
- The `CommandDao` contains complex logic for maintaining the order of commands using a `position` column. When adding, deleting, or moving items, it uses SQL transactions to update the `position` of all affected rows to ensure data integrity.

### 4.3. Shizuku Service Lifecycle
1.  **Initialization**: The app calls `Runner.init()` on startup.
2.  **Permission & Binding**: `HomeFragment` checks Shizuku status. If permission is granted and Shizuku is running, `Runner.tryBindService()` is called.
3.  **Service Start**: Shizuku starts the `ServerMain` class in a dedicated, privileged process.
4.  **Connection**: The `onServiceConnected` callback in `Runner.kt` is triggered, and the `service` (an `IService` proxy) becomes available for use.
5.  **Disconnection**: If the service process dies, `onServiceDisconnected` and `onBinderDead` listeners are triggered, `Runner.service` is set to `null`, and the UI is updated accordingly.

## 5. Development Constraints & Notes

- **Shizuku is Mandatory**: The application is fundamentally dependent on Shizuku. All core functionalities will fail if Shizuku is not installed, running, or if permission is not granted.
- **Native Dependencies**: The project requires the NDK to build. The `starter` and `processutils` native components are critical for execution and process management.
- **Data Persistence**: The database schema is simple but managed manually. Migrations in `DataDbHelper.onUpgrade` are destructive (drop and recreate tables). Be cautious when changing the schema.
- **UI and Service Decoupling**: Maintain the strict separation between the UI (`app` package) and the service (`server` package). They only communicate through `AIDL` and `Parcelable` data objects.