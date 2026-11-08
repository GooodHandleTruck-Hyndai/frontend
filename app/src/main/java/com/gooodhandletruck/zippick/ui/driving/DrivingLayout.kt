package com.gooodhandletruck.zippick.ui.driving

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.gooodhandletruck.zippick.ui.WindowLayout
import com.gooodhandletruck.zippick.ui.theme.AppDimens

/**
 * 주행 중 화면 틀(S-08·S-09). 왼쪽 탭 없이 가운데에 [content]를 두고, 큰 버튼(높이 160)은 창 아래에 가로로 나눠 둔다.
 * 1:2 창에서도 같은 구성을 가운데 정렬하고 버튼 너비만 늘린다. 가격·목록·지도는 그리지 않는다.
 */
@Composable
fun DrivingLayout(
    window: WindowLayout,
    buttons: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = window.contentPadding, vertical = AppDimens.ContentPaddingVertical),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        content()
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceM),
            content = buttons
        )
    }
}
