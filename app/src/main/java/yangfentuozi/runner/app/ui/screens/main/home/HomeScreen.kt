package yangfentuozi.runner.app.ui.screens.main.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import yangfentuozi.runner.app.Service
import yangfentuozi.runner.app.ui.screens.main.home.components.GrantShizukuPermCard
import yangfentuozi.runner.app.ui.screens.main.home.components.ServiceStatusCard
import yangfentuozi.runner.app.ui.screens.main.home.components.StartWithShizukuCard
import yangfentuozi.runner.app.ui.theme.AppSpacing
import yangfentuozi.runner.app.ui.viewmodels.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val refreshTrigger by viewModel.refreshTrigger.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    // 监听生命周期事件
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    viewModel.triggerRefresh()
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(
            top = AppSpacing.topBarContentSpacing,
            bottom = AppSpacing.screenBottomPadding,
            start = AppSpacing.screenHorizontalPadding,
            end = AppSpacing.screenHorizontalPadding
        ),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.cardSpacing)
    ) {
        item(key = "service_status_$refreshTrigger") {
            ServiceStatusCard()
        }
        if (!Service.pingService()) {
            if (!Service.shizukuPermission) {
                item(key = "grant_perm_$refreshTrigger") {
                    GrantShizukuPermCard(viewModel)
                }
            } else {
                item(key = "shizuku_status_$refreshTrigger") {
                    StartWithShizukuCard(viewModel = viewModel)
                }
            }
        }
    }
}

