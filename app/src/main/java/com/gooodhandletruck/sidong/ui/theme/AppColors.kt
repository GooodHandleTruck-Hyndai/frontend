package com.gooodhandletruck.sidong.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 화면 설계서 '공통 규격 > 색'의 토큰. 밝은 테마 하나만 쓴다.
 * 강조색은 한 화면에서 주 버튼과 고른 상태에만 쓴다.
 */
object AppColors {
    val Background = Color(0xFFF7F8FA) // 창 배경
    val Surface = Color(0xFFFFFFFF) // 카드, 왼쪽 탭 바탕
    val Outline = Color(0xFFE0E1E6) // 카드 테두리, 구분선
    val TextPrimary = Color(0xFF131417) // 이름, 가격, 본문
    val TextSecondary = TextPrimary.copy(alpha = 0.84f) // 설명 문구
    val TextTertiary = TextPrimary.copy(alpha = 0.64f) // 기준일, 캡션
    val ButtonSecondary = Color(0xFFEDEEF2) // 보조 버튼, 고르지 않은 칩
    val Accent = Color(0xFF157A52) // 주 버튼, 고른 탭·칩, 방향·거리
    val OnAccent = Color(0xFFFFFFFF) // 주 버튼 글자
    val OfficialPrice = Color(0xFFB45309) // 공시가격

    // 시안(데모 화면)의 옅은 강조색. 흰 카드 위에서 강조색 글자와 4.5:1 이상이다
    val AccentSoft = Accent.copy(alpha = 0.1f) // 번호 배지, 고른 탭, 저장함 칩 바탕
    val AccentLine = Accent.copy(alpha = 0.4f) // 눈에 띄게 할 카드의 테두리(방금 들은 단지, 도착)
    val OfficialPriceSoft = OfficialPrice.copy(alpha = 0.1f) // 공시가격 칩 바탕
}
