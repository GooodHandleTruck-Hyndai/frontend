package com.gooodhandletruck.sidong.ui.nearby

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.ui.SAMPLE_COMPLEX_ID
import com.gooodhandletruck.sidong.ui.WindowLayout
import com.gooodhandletruck.sidong.ui.component.PlaceholderAction
import com.gooodhandletruck.sidong.ui.component.PlaceholderScreen
import com.gooodhandletruck.sidong.ui.component.StatusBox
import com.gooodhandletruck.sidong.ui.component.rememberLocationPermissionRequest
import com.gooodhandletruck.sidong.ui.navigation.Screen
import com.gooodhandletruck.sidong.ui.theme.AppDimens

/**
 * S-03 주변 단지. 정차 중 홈. 카드나 약도 말풍선을 누르면 S-04가 열린다.
 * 위치 권한이 없으면 상태 상자의 [위치 권한 허용]으로 권한 창을 다시 띄운다.
 * TODO: "허용하지 않고 다시 묻지 않음"을 고른 뒤에는 창이 뜨지 않아 버튼이 아무 일도 하지 않는다.
 *  그때 보여 줄 문구는 화면 설계서 '정할 것' 11번에서 정한다.
 */
@Composable
fun NearbyScreen(
    window: WindowLayout,
    locationGranted: Boolean,
    onLocationPermissionResult: () -> Unit,
    onOpenComplex: (String) -> Unit
) {
    val requestPermission = rememberLocationPermissionRequest { onLocationPermissionResult() }
    PlaceholderScreen(
        id = Screen.Nearby.id,
        title = stringResource(R.string.screen_nearby),
        description = stringResource(R.string.placeholder_nearby),
        window = window,
        actions = listOf(
            PlaceholderAction(stringResource(R.string.action_open_detail)) { onOpenComplex(SAMPLE_COMPLEX_ID) }
        )
    ) {
        if (!locationGranted) {
            StatusBox(
                message = stringResource(R.string.status_location_denied),
                actionLabel = stringResource(R.string.action_allow_location),
                onAction = requestPermission,
                modifier = Modifier.widthIn(max = AppDimens.ListPaneWidth)
            )
        }
    }
}
