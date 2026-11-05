package com.gooodhandletruck.sidong.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.gooodhandletruck.sidong.ui.theme.AppColors
import com.gooodhandletruck.sidong.ui.theme.AppType

/**
 * 단지 사진 자리(카드 132×132, S-04 머리). 크기와 모서리는 [modifier]로 정한다.
 * 사진이 없으면 이름 첫 글자를 넣은 회색 칸이다 (화면 설계서 S-04 '사진').
 */
@Composable
fun ComplexPhoto(name: String, modifier: Modifier = Modifier, letterStyle: TextStyle = AppType.TitleM) {
    // TODO: imageUrl이 오면 사진을 그린다. 그림을 불러올 라이브러리(Coil 등)는 넣기 전에 먼저 묻는다
    Box(modifier = modifier.background(AppColors.ButtonSecondary), contentAlignment = Alignment.Center) {
        Text(text = name.take(1), style = letterStyle, color = AppColors.TextTertiary)
    }
}
