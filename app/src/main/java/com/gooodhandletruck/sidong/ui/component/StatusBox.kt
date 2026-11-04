package com.gooodhandletruck.sidong.ui.component

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
import com.gooodhandletruck.sidong.ui.theme.AppColors
import com.gooodhandletruck.sidong.ui.theme.AppDimens
import com.gooodhandletruck.sidong.ui.theme.AppType

/** 상태 상자. 한 줄 안내와 버튼 하나로 빈 목록·권한·오류를 알린다 (S-03, S-05, S-06). */
@Composable
fun StatusBox(message: String, actionLabel: String, onAction: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(AppDimens.CardRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.Surface, shape)
            .border(1.dp, AppColors.Outline, shape)
            .padding(AppDimens.SpaceL),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceM)
    ) {
        Text(text = message, style = AppType.BodyM, color = AppColors.TextPrimary)
        PrimaryButton(text = actionLabel, onClick = onAction)
    }
}
