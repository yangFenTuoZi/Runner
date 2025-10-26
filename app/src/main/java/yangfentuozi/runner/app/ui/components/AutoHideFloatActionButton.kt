package yangfentuozi.runner.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp

@Composable
fun BlockWithAutoHideFloatActionButton(
    content: @Composable () -> Unit,
    onClickFAB: () -> Unit,
    contentFAB: @Composable () -> Unit,
) {
    var isFabVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // 手指向上滑动（内容向下滚动），隐藏FAB
                if (available.y < -1) {
                    isFabVisible = false
                }
                // 手指向下滑动（内容向上滚动），显示FAB
                if (available.y > 1) {
                    isFabVisible = true
                }
                return Offset.Zero
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        content.invoke()
        Box(
            // 展开时填充整个父容器以显示遮罩，折叠时恢复原始modifier定位
            modifier = if (isFabVisible) {
                Modifier.fillMaxSize()
            } else {
                Modifier.align(Alignment.BottomEnd)
            },
            contentAlignment = Alignment.BottomEnd
        ) {
            // 使用AnimatedVisibility替代scale动画，隐藏时完全移除避免拦截点击
            AnimatedVisibility(
                visible = isFabVisible,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ),
                exit = scaleOut(
                    animationSpec = tween(durationMillis = 150)
                )
            ) {
                Column(
                    // 展开时添加padding确保FAB在右下角
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    FloatingActionButton(
                        onClick = onClickFAB,
                        modifier = Modifier.size(54.dp),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp
                        ),
                        content = contentFAB
                    )
                }
            }
        }
    }
}