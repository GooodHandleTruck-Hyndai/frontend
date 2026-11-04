package com.gooodhandletruck.sidong.ui.saved

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.ui.SAMPLE_COMPLEX_ID
import com.gooodhandletruck.sidong.ui.WindowLayout
import com.gooodhandletruck.sidong.ui.component.PlaceholderAction
import com.gooodhandletruck.sidong.ui.component.PlaceholderScreen
import com.gooodhandletruck.sidong.ui.navigation.Screen

/** S-05 저장한 단지. 카드를 누르면 S-04, 빈 목록의 [주변 단지 보기]는 S-03으로 간다. */
@Composable
fun SavedScreen(window: WindowLayout, onOpenComplex: (String) -> Unit, onGoNearby: () -> Unit) {
    PlaceholderScreen(
        id = Screen.Saved.id,
        title = stringResource(R.string.screen_saved),
        description = stringResource(R.string.placeholder_saved),
        window = window,
        actions = listOf(
            PlaceholderAction(stringResource(R.string.action_open_detail)) { onOpenComplex(SAMPLE_COMPLEX_ID) },
            PlaceholderAction(stringResource(R.string.action_go_nearby), onClick = onGoNearby)
        )
    )
}
