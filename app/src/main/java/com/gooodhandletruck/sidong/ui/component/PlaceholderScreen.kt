package com.gooodhandletruck.sidong.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.ui.WindowLayout
import com.gooodhandletruck.sidong.ui.theme.AppColors
import com.gooodhandletruck.sidong.ui.theme.AppDimens
import com.gooodhandletruck.sidong.ui.theme.AppType
import kotlin.math.roundToInt

/** 빈 화면에서 다른 화면으로 가는 버튼 하나 */
data class PlaceholderAction(
    val label: String,
    val isPrimary: Boolean = false,
    val onClick: () -> Unit
)

/**
 * 정차 중 빈 화면 틀. 화면 번호·이름·설명·창 크기와 이동 버튼만 그린다.
 * 먼저 만든 부분(상태 상자 등)은 [content]로 넣으면 창 크기 줄과 이동 버튼 사이에 그린다.
 * 실제 화면을 만들면 그 화면 파일에서 이 틀을 걷어 낸다.
 */
@Composable
fun PlaceholderScreen(
    id: String,
    title: String,
    description: String,
    window: WindowLayout,
    actions: List<PlaceholderAction>,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = window.contentPadding, vertical = AppDimens.ContentPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceS)
    ) {
        if (onBack != null) {
            SecondaryButton(text = stringResource(R.string.action_back), onClick = onBack)
            Spacer(Modifier.height(AppDimens.SpaceS))
        }
        Text(text = id, style = AppType.LabelL, color = AppColors.TextTertiary)
        Text(text = title, style = AppType.TitleS, color = AppColors.TextPrimary)
        Text(text = description, style = AppType.BodyS, color = AppColors.TextSecondary)
        Text(
            text = stringResource(
                R.string.window_size,
                window.width.value.roundToInt(),
                window.height.value.roundToInt(),
                stringResource(if (window.isWide) R.string.window_wide else R.string.window_narrow)
            ),
            style = AppType.LabelS,
            color = AppColors.TextTertiary
        )
        content()
        Spacer(Modifier.height(AppDimens.SpaceM))
        actions.forEach { action ->
            if (action.isPrimary) {
                PrimaryButton(text = action.label, onClick = action.onClick)
            } else {
                SecondaryButton(text = action.label, onClick = action.onClick)
            }
        }
    }
}
