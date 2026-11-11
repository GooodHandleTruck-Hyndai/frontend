package com.gooodhandletruck.zippick.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 한 모드(Light·Dark)의 색 묶음. 값은 Figma 디자인 시스템의 'Semantic: Color' 변수에서 가져온다.
 * 각 줄 주석의 이름이 Figma 변수 이름이다.
 */
@Immutable
data class AppPalette(
    val isDark: Boolean,
    val background: Color, // Background/primary
    val surface: Color, // Background/secondary
    val outline: Color, // Border/Neutral/secondary
    val textPrimary: Color, // Text/Neutral/dark/primary
    val buttonSecondary: Color, // Background/tertiary
    val accent: Color, // Button/Brand/enabled
    val onAccent: Color, // Text/Neutral/light/primary
    val accentSoft: Color, // Surface/Brand/default
    val officialPrice: Color // Figma에 시맨틱 토큰이 아직 없다. 아래 값 주석을 본다
)

/** Light 모드. 지금 앱은 쓰지 않지만 Figma 값과 맞춰 둔다. */
val LightPalette = AppPalette(
    isDark = false,
    background = Color(0xFFFFFFFF), // Gray/00
    surface = Color(0xFFF7F8FA), // Gray/50
    outline = Color(0x1A131417), // Alpha/dark/100
    textPrimary = Color(0xFF131417), // Static/dark/100
    buttonSecondary = Color(0xFFEDEEF2), // Gray/100
    accent = Color(0xFF0DC9BF), // Teal/500
    onAccent = Color(0xFFFFFFFF), // Static/light/100
    accentSoft = Color(0xFFE1F5F3), // Teal/100
    officialPrice = Color(0xFFB45309) // TODO: 디자이너에게 공시가격 색 토큰을 받는다. 지금은 바꾸기 전 값이다
)

/** Dark 모드. GUI를 모두 다크로 만들기로 해서 앱은 이 묶음만 쓴다. */
val DarkPalette = AppPalette(
    isDark = true,
    background = Color(0xFF1D1E21), // Gray/800
    surface = Color(0xFF313236), // Gray/700
    outline = Color(0x1AFFFFFF), // Alpha/light/100
    textPrimary = Color(0xFFFFFFFF), // Static/light/100
    buttonSecondary = Color(0xFF44464E), // Gray/600
    accent = Color(0xFF52DBD1), // Teal/400
    onAccent = Color(0xFF131417), // Static/dark/100
    accentSoft = Color(0xFF063E3B), // Teal/900
    officialPrice = Color(0xFFFFA927) // TODO: 디자이너에게 공시가격 색 토큰을 받는다. 지금은 프리미티브 Orange/400이다
)

/** [AppTheme]이 고른 색 묶음을 화면에 내려 준다. */
val LocalAppPalette = staticCompositionLocalOf { DarkPalette }

/**
 * 화면에서 쓰는 색 이름. 값은 [AppTheme]이 고른 모드의 [AppPalette]에서 읽는다.
 * 컴포저블 안에서만 읽을 수 있다. Canvas 그리기 블록에서는 블록 밖에서 먼저 읽어 변수에 담아 쓴다.
 * 강조색은 한 화면에서 주 버튼과 고른 상태에만 쓴다.
 */
object AppColors {
    private val palette: AppPalette
        @Composable @ReadOnlyComposable get() = LocalAppPalette.current

    /** 창 배경 */
    val Background: Color
        @Composable @ReadOnlyComposable get() = palette.background

    /** 카드, 왼쪽 탭 바탕 */
    val Surface: Color
        @Composable @ReadOnlyComposable get() = palette.surface

    /** 카드 테두리, 구분선 */
    val Outline: Color
        @Composable @ReadOnlyComposable get() = palette.outline

    /** 이름, 가격, 본문 */
    val TextPrimary: Color
        @Composable @ReadOnlyComposable get() = palette.textPrimary

    /** 설명 문구. Text/Neutral/dark/secondary(84%) */
    val TextSecondary: Color
        @Composable @ReadOnlyComposable get() = palette.textPrimary.copy(alpha = 0.84f)

    /** 기준일, 캡션. Text/Neutral/dark/tertiary(64%) */
    val TextTertiary: Color
        @Composable @ReadOnlyComposable get() = palette.textPrimary.copy(alpha = 0.64f)

    /** 보조 버튼, 고르지 않은 칩 */
    val ButtonSecondary: Color
        @Composable @ReadOnlyComposable get() = palette.buttonSecondary

    /** 주 버튼, 고른 탭·칩, 방향·거리 */
    val Accent: Color
        @Composable @ReadOnlyComposable get() = palette.accent

    /** 주 버튼 글자 */
    val OnAccent: Color
        @Composable @ReadOnlyComposable get() = palette.onAccent

    /** 공시가격 */
    val OfficialPrice: Color
        @Composable @ReadOnlyComposable get() = palette.officialPrice

    /** 번호 배지, 고른 탭, 저장함 칩 바탕 */
    val AccentSoft: Color
        @Composable @ReadOnlyComposable get() = palette.accentSoft

    /** 눈에 띄게 할 카드의 테두리(방금 들은 단지, 도착) */
    val AccentLine: Color
        @Composable @ReadOnlyComposable get() = palette.accent.copy(alpha = 0.4f)

    /** 공시가격 칩 바탕 */
    val OfficialPriceSoft: Color
        @Composable @ReadOnlyComposable get() = palette.officialPrice.copy(alpha = 0.1f)
}
