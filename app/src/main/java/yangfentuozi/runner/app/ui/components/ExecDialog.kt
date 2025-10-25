package yangfentuozi.runner.app.ui.components

import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import yangfentuozi.runner.R
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.app.ui.theme.monoFontFamily
import yangfentuozi.runner.server.ServerMain
import yangfentuozi.runner.server.callback.IExitCallback
import yangfentuozi.runner.shared.data.CommandInfo
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun ExecDialog(
    command: CommandInfo,
    onDismiss: () -> Unit
) {
    var output by remember { mutableStateOf("") }
    var isExecuting by remember { mutableStateOf(true) }
    var pid by remember { mutableStateOf<Int?>(null) }
    var exitCode by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(Unit) {
        val thread = Thread {
            try {
                val service = Runner.service
                if (service == null) {
                    output = "Service not available"
                    isExecuting = false
                    return@Thread
                }

                // 创建管道来读取输出
                val pipe = ParcelFileDescriptor.createPipe()
                val readPipe = pipe[0]
                val writePipe = pipe[1]

                val exitCallback = object : IExitCallback.Stub() {
                    override fun onExit(exitValue: Int) {
                        exitCode = exitValue
                        isExecuting = false
                    }

                    override fun errorMessage(message: String?) {
                        output += message + "\n"
                    }
                }

                // 启动命令执行
                service.exec(
                    command.command ?: "",
                    command.name ?: "command",
                    exitCallback,
                    writePipe
                )

                // 读取输出
                val inputStream = ParcelFileDescriptor.AutoCloseInputStream(readPipe)
                
                try {
                    // 第一行：读取 PID
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val firstLine = reader.readLine()
                    firstLine?.let {
                        pid = it.toIntOrNull() ?: -1
                    }

                    // 后续数据：分块读取
                    val buffer = ByteArray(ServerMain.PAGE_SIZE)
                    while (true) {
                        val bytesRead = inputStream.read(buffer)
                        if (bytesRead <= 0) break
                        val chunk = String(buffer, 0, bytesRead, Charsets.UTF_8)
                        output += chunk
                    }
                } catch (_: Exception) {
                } finally {
                    try { inputStream.close() } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                output += "Error: ${e.message}\n"
                isExecuting = false
            }
        }

        thread.start()

        onDispose {
            // 清理资源,注意线程可能无法优雅地停止
            thread.interrupt()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isExecuting) stringResource(R.string.executing) else stringResource(R.string.finish)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                pid?.let {
                    pid
                    Text(
                        text = "PID: $pid",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                exitCode?.let { code ->
                    Text(
                        text = stringResource(
                            R.string.return_info, code, when (code) {
                                0 -> stringResource(R.string.normal)
                                127 -> stringResource(R.string.command_not_found)
                                139 -> stringResource(R.string.segmentation_error)
                                130 -> stringResource(R.string.ctrl_c_exit)
                                else -> stringResource(R.string.other_error)
                            }
                        ),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Text(
                    text = output,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                    fontFamily = monoFontFamily,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        }
    )
}

