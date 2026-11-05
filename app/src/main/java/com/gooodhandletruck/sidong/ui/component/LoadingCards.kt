package com.gooodhandletruck.sidong.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.gooodhandletruck.sidong.ui.theme.AppColors
import com.gooodhandletruck.sidong.ui.theme.AppDimens
import com.gooodhandletruck.sidong.ui.theme.AppType

private const val LOADING_CARD_COUNT = 3

/**
 * 목록을 불러오는 중: 움직이지 않는 회색 자리 카드 3개 (반복 애니메이션 금지).
 * [message]가 있으면 위에 한 줄 쓴다(S-03 "주변 단지를 찾고 있어요").
 */
@Composable
fun LoadingCards(modifier: Modifier = Modifier, message: String? = null) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceS)) {
        if (message != null) {
            Text(text = message, style = AppType.BodyM, color = AppColors.TextSecondary)
        }
        repeat(LOADING_CARD_COUNT) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.CardPhotoSize + AppDimens.SpaceM * 2)
                    .clip(RoundedCornerShape(AppDimens.CardRadius))
                    .background(AppColors.ButtonSecondary)
            )
        }
    }
}
