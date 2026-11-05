package com.gooodhandletruck.sidong.ui.driving

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.ui.WindowLayout
import com.gooodhandletruck.sidong.ui.component.PrimaryButton
import com.gooodhandletruck.sidong.ui.component.SecondaryButton
import com.gooodhandletruck.sidong.ui.theme.AppColors
import com.gooodhandletruck.sidong.ui.theme.AppDimens
import com.gooodhandletruck.sidong.ui.theme.AppType

/**
 * S-09 주행 중 단지 알림. 단지 이름 한 줄(titleL, 길면 말줄임표)과 큰 버튼 두 개 [다시 듣기] [저장](높이 160, 반씩).
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
                height = AppDimens.DrivingButtonHeight
            )
            if (saved) {
                SecondaryButton(
                    text = stringResource(R.string.action_saved),
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    height = AppDimens.DrivingButtonHeight,
                    enabled = false
                )
            } else {
                PrimaryButton(
                    text = stringResource(R.string.action_save),
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    height = AppDimens.DrivingButtonHeight
                )
            }
        }
    ) {
        Text(
            text = name,
            style = AppType.TitleL,
            color = AppColors.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
