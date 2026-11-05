package com.gooodhandletruck.sidong.ui.theme

import androidx.compose.ui.unit.dp

/** 화면 설계서 '공통 규격 > 레이아웃, 터치와 간격'의 크기. 단위는 dp다. */
object AppDimens {
    // 창 크기: 이 너비 이상이면 넓은 창(1:2, 1700dp)으로 본다. 좁은 창(1:1:1)은 840dp다.
    val WideWindowMinWidth = 1200.dp

    // 누르는 것
    val TouchMin = 76.dp // 누를 수 있는 것은 모두 76×76 이상
    val ButtonHeight = 96.dp // 정차 중 주 버튼·보조 버튼
    val DrivingButtonHeight = 160.dp // 주행 중 버튼

    // 간격: 8의 배수만 쓴다
    val SpaceXs = 8.dp
    val SpaceS = 16.dp
    val SpaceM = 24.dp
    val SpaceL = 32.dp
    val SpaceXl = 40.dp

    // 모서리
    val CardRadius = 28.dp
    val ButtonRadius = 24.dp // 버튼, 칩

    // 단지 카드
    val CardPhotoSize = 132.dp // 카드 왼쪽 사진
    val ThumbnailRadius = 16.dp // 카드 안 사진 모서리. 설계서에 따로 없어서 카드(28)보다 작게 둔다
    val SelectedBorder = 2.dp // 1:2 창에서 상세를 연 카드의 강조색 테두리

    // S-04 단지 상세
    val DetailPhotoHeight = 320.dp // 좁은 창(1:1:1)의 사진 높이. 넓은 창(1:2)은 16:9

    // 틀
    val RailWidth = 136.dp // 왼쪽 탭
    val RailIconSize = 40.dp
    val ListPaneWidth = 640.dp // 넓은 창의 목록 칸
    val ContentPaddingWide = 40.dp // 넓은 창 본문 좌우 여백
    val ContentPaddingNarrow = 32.dp // 좁은 창 본문 좌우 여백
    val ContentPaddingVertical = 32.dp // 본문 위아래 여백
}
