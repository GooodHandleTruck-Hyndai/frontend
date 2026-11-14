package com.gooodhandletruck.zippick.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gooodhandletruck.zippick.ui.theme.AppColors
import com.gooodhandletruck.zippick.ui.theme.AppDimens
import com.gooodhandletruck.zippick.ui.theme.AppType

/**
 * 상태 상자. 한 줄 안내와 버튼 하나로 빈 목록·권한·오류를 알린다 (S-03, S-05, S-06).
 * [detail]이 있으면 안내 아래에 설명을 한 줄 더 쓴다(S-05 빈 목록).
 * 누를 것이 없으면(예: 3km에도 단지가 없을 때) [actionLabel]을 비워 버튼을 뺀다.
 */
@Composable
fun StatusBox(
    message: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    val shape = RoundedCornerShape(AppDimens.CardRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.Surface, shape)
            .border(1.dp, AppColors.Outline, shape)
            .padding(AppDimens.SpaceL),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceM)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceXs)) {
            Text(text = message, style = AppType.Body2, color = AppColors.TextPrimary)
            if (detail != null) {
                Text(text = detail, style = AppType.Body3, color = AppColors.TextSecondary)
            }
        }
        if (actionLabel != null) {
            PrimaryButton(text = actionLabel, onClick = onAction)
        }
    }
}
