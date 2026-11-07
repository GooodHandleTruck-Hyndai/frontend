package com.gooodhandletruck.sidong.ui.driving

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.ui.WindowLayout
import com.gooodhandletruck.sidong.ui.component.PrimaryButton
import com.gooodhandletruck.sidong.ui.component.SecondaryButton
import com.gooodhandletruck.sidong.ui.theme.AppColors
import com.gooodhandletruck.sidong.ui.theme.AppDimens
import com.gooodhandletruck.sidong.ui.theme.AppType

/**
 * S-09 주행 중 단지 알림. 단지 이름 한 줄(displayL, 길면 말줄임표)과 큰 버튼 두 개 [다시 듣기] [저장](높이 160, 반씩).
 * 이름은 시안(D-01)처럼 옅은 강조색 테두리(3)의 흰 카드에 소리 아이콘과 함께 둔다. 글자는 이름 한 줄뿐이다.
 * 가격은 화면에 쓰지 않고 소리로만 말한다. 2분이 지나면 S-08로 돌아간다(Navigator).
 * [저장]을 누르면 "저장했어요"로 바뀌고 더 누를 수 없다. 취소는 정차 중에 한다.
 */
@Composable
fun DrivingAlertScreen(window: WindowLayout, name: String, saved: Boolean, onReplay: () -> Unit, onSave: () -> Unit) {
    DrivingLayout(
        window = window,
        buttons = {
            SecondaryButton(
                text = stringResource(R.string.action_replay),
                onClick = onReplay,
                modifier = Modifier.weight(1f),
                height = AppDimens.DrivingButtonHeight,
                icon = R.drawable.ic_speaker
            )
            if (saved) {
                SecondaryButton(
                    text = stringResource(R.string.action_saved),
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    height = AppDimens.DrivingButtonHeight,
                    enabled = false,
                    icon = R.drawable.ic_heart_filled
                )
            } else {
                PrimaryButton(
                    text = stringResource(R.string.action_save),
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    height = AppDimens.DrivingButtonHeight,
                    icon = R.drawable.ic_heart
                )
            }
        }
    ) {
        NameCard(name = name)
    }
}

/** 단지 이름 카드: 흰 바탕, 옅은 강조색 테두리, 강조색 소리 아이콘과 이름 한 줄 */
@Composable
private fun NameCard(name: String) {
    val shape = RoundedCornerShape(AppDimens.CardRadius)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, shape)
            .border(AppDimens.HighlightBorder, AppColors.AccentLine, shape)
            .padding(horizontal = AppDimens.SpaceL, vertical = AppDimens.SpaceXl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceS, Alignment.CenterHorizontally)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_speaker),
            contentDescription = null,
            tint = AppColors.Accent,
            modifier = Modifier.size(AppDimens.RailIconSize)
        )
        Text(
            text = name,
            style = AppType.DisplayL,
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
