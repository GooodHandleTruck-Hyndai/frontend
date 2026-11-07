package com.gooodhandletruck.zippick.ui.theme

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
    val BadgeSize = 72.dp // S-03 카드 왼쪽 번호 배지(약도 번호와 같다)
    val TimeColumnWidth = 88.dp // S-06 카드 왼쪽 들은 시각 칸
    val CardPlaceholderHeight = 208.dp // 불러오는 중 회색 카드. 방향·거리 줄이 있는 카드 높이와 같다
    val SelectedBorder = 3.dp // 1:2 창에서 상세를 연 카드의 강조색 테두리

    // S-03 약도: 좁은 창(1:1:1)에서는 목록 위 높이 480. 넓은 창(1:2)은 목록 옆 칸을 채운다
    val NarrowMapHeight = 480.dp

    // 틀
    val HeaderHeight = 96.dp // 정차 중 화면 맨 위 머리글(로고·앱 이름·상태)
    val LogoSize = 56.dp
    val LogoRadius = 16.dp
    val RailWidth = 136.dp // 왼쪽 탭
    val RailIconSize = 40.dp
    val ListPaneWidth = 640.dp // 넓은 창의 목록 칸
    val ContentPaddingWide = 40.dp // 넓은 창 본문 좌우 여백
    val ContentPaddingNarrow = 32.dp // 좁은 창 본문 좌우 여백
    val ContentPaddingTop = 16.dp // 머리글 아래 본문 위 여백
    val ContentPaddingVertical = 32.dp // 본문 아래 여백, 머리글이 없는 주행 화면의 위아래 여백

    // 아이콘
    val IconL = 32.dp // 큰 버튼 안, 상태 줄
    val IconS = 28.dp // 작은 버튼 안, 방향·거리 줄
    val HighlightBorder = 3.dp // 눈에 띄게 할 카드의 테두리(AccentLine)
}
