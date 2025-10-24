package yangfentuozi.runner.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import yangfentuozi.runner.R
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.app.ui.theme.RunnerTheme
import yangfentuozi.runner.app.ui.theme.monoFontFamily
import yangfentuozi.runner.server.ServerMain
import yangfentuozi.runner.server.callback.IExitCallback
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallTermExtScreen(
    uri: Uri?,
    externalCacheDir: File?,
    onNavigateBack: () -> Unit,
    onShowToast: (String) -> Unit,
    onShowToastRes: (Int) -> Unit
) {
    val context = LocalContext.current
    var output by remember { mutableStateOf("") }
    var isInstalling by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val isStopped = remember { AtomicBoolean(false) }
    val termExtCacheDir = remember { File(externalCacheDir, "termExtCache") }

    // 滚动到底部
    LaunchedEffect(output) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    // 处理安装逻辑
    LaunchedEffect(uri) {
        if (uri == null) {
            output += "! Invalid intent or file\n"
            hasError = true
            return@LaunchedEffect
        }

        // 复制文件到缓存
        val cacheFile = try {
            val input = context.contentResolver.openInputStream(uri)
            if (input == null) {
                output += "! Failed to open file\n"
                hasError = true
                return@LaunchedEffect
            }

            // 清理并创建缓存目录
            termExtCacheDir.deleteRecursively()
            termExtCacheDir.mkdirs()

            val file = File(termExtCacheDir, "termux_ext.zip")
            input.use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream, bufferSize = ServerMain.PAGE_SIZE)
                }
            }
            output += "- File copied to cache: ${file.absolutePath}\n"
            file
        } catch (e: FileNotFoundException) {
            output += "! File not found:\n${e.message}\n"
            hasError = true
            return@LaunchedEffect
        } catch (e: IOException) {
            output += "! Failed to copy file:\n${e.message}\n"
            hasError = true
            return@LaunchedEffect
        }

        // 检查服务
        if (!Runner.pingServer()) {
            output += "! Service not running\n"
            hasError = true
            onShowToastRes(R.string.service_not_running)
            return@LaunchedEffect
        }

        val service = Runner.service
        if (service == null) {
            output += "! Service not available\n"
            hasError = true
            onShowToastRes(R.string.service_not_running)
            return@LaunchedEffect
        }

        // 开始安装
        isInstalling = true
        output += "- Starting installation...\n"

        // 在后台线程中执行安装
        Thread {
            var pipe: Array<ParcelFileDescriptor>? = null
            try {
                pipe = ParcelFileDescriptor.createPipe()
                val readPipe = pipe[0]
                val writePipe = pipe[1]

                val exitCallback = object : IExitCallback.Stub() {
                    override fun onExit(exitValue: Int) {
                        if (isStopped.get()) return

                        // 延迟确保所有输出都已读取
                        Thread.sleep(200)

                        val message = if (exitValue == 0) {
                            "- Installation successful\n"
                        } else {
                            "! Installation failed (exit code: $exitValue)\n"
                        }
                        output += message
                        output += "- Cleanup temp: ${termExtCacheDir.absolutePath}\n"

                        isInstalling = false

                        // 清理缓存
                        try {
                            termExtCacheDir.deleteRecursively()
                        } catch (_: Exception) {
                        }
                    }

                    override fun errorMessage(message: String?) {
                        if (message != null) {
                            output += "$message\n"
                        }
                    }
                }

                // 启动安装
                service.installTermExt(cacheFile.absolutePath, exitCallback, writePipe)

                // 读取输出
                ParcelFileDescriptor.AutoCloseInputStream(readPipe).bufferedReader().use { reader ->
                    while (!isStopped.get()) {
                        val line = reader.readLine() ?: break
                        output += "$line\n"
                    }
                }
            } catch (e: IOException) {
                if (!isStopped.get()) {
                    output += "! Pipe read error: ${e.message}\n"
                    hasError = true
                    isInstalling = false
                }
            } finally {
                // 关闭管道
                pipe?.let { pipes ->
                    try {
                        pipes[0].close()
                    } catch (_: IOException) {
                    }
                    try {
                        pipes[1].close()
                    } catch (_: IOException) {
                    }
                }
            }
        }.start()
    }

    DisposableEffect(Unit) {
        onDispose {
            isStopped.set(true)
            // 清理缓存目录（在后台线程中执行）
            Thread {
                try {
                    termExtCacheDir.deleteRecursively()
                } catch (_: Exception) {
                }
            }.start()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Install Termux Extension") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        enabled = !isInstalling
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = output,
                        fontFamily = monoFontFamily,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 显示加载指示器
            if (isInstalling) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Installing...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

// 创建一个独立的 Activity 用于安装 Termux 扩展
class InstallTermExtActivityCompose : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        }

        setContent {
            RunnerTheme {
                InstallTermExtScreen(
                    uri = uri,
                    externalCacheDir = externalCacheDir,
                    onNavigateBack = { finish() },
                    onShowToast = { message ->
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    },
                    onShowToastRes = { resId ->
                        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

