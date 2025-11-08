package yangfentuozi.runner.app.ui.components

/**
 * 设置卡片容器
 * 提供统一的卡片样式
 */

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import yangfentuozi.runner.app.ui.theme.AppShape

/**
 * 设置卡片
 * @param title 卡片标题
 * @param modifier 修饰符
 * @param content 卡片内容
 */
@Composable
fun BeautifulCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppShape.shapes.cardLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        enabled = onClick != null,
        onClick = {
            onClick?.invoke()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {

            // 内容
            content()
        }
    }
}

