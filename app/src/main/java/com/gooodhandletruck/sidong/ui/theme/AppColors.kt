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
}
