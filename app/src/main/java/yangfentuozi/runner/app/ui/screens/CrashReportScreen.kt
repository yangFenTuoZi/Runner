package yangfentuozi.runner.app.ui.screens

import android.os.Build
import android.os.Bundle
import android.os.SystemProperties
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import yangfentuozi.runner.app.App
import yangfentuozi.runner.app.ui.theme.RunnerTheme
import yangfentuozi.runner.app.ui.theme.monoFontFamily
import java.io.FileOutputStream
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashReportScreen(
    crashFile: String?,
    crashInfo: String?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as App

    // 构建完整的崩溃信息文本
    val fullCrashInfo = remember(crashInfo) {
        buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append("VERSION.RELEASE: ")
            }
            append(Build.VERSION.RELEASE)
            append("\n")

            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append("VERSION.SDK_INT: ")
            }
            append(Build.VERSION.SDK_INT.toString())
            append("\n")

            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append("BUILD_TYPE: ")
            }
            append(Build.TYPE)
            append("\n")

            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append("CPU_ABI: ")
            }
            append(SystemProperties.get("ro.product.cpu.abi"))
            append("\n")

            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append("CPU_SUPPORTED_ABIS: ")
            }
            append(Build.SUPPORTED_ABIS.contentToString())
            append("\n\n")
            append(crashInfo ?: "")
        }
    }

    // 保存崩溃信息到文件
    LaunchedEffect(crashFile, fullCrashInfo) {
        if (crashFile != null) {
            try {
                FileOutputStream(crashFile).use { out ->
                    out.write(fullCrashInfo.text.toByteArray())
                }
            } catch (_: IOException) {
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            app.finishApp()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crash Report") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // 崩溃文件路径
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                SelectionContainer {
                    Row {
                        Text(
                            text = "Crash File: ",
                            fontFamily = monoFontFamily,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = crashFile ?: "",
                            fontFamily = monoFontFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 崩溃详细信息
            SelectionContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = fullCrashInfo,
                        fontFamily = monoFontFamily,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// 创建一个独立的 Activity 用于崩溃报告
class CrashReportActivityCompose : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crashFile = intent.getStringExtra("crash_file")
        val crashInfo = intent.getStringExtra("crash_info")

        setContent {
            RunnerTheme {
                CrashReportScreen(
                    crashFile = crashFile,
                    crashInfo = crashInfo,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

