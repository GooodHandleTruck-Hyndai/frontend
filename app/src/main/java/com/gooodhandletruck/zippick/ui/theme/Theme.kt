package com.gooodhandletruck.zippick.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Material 컴포넌트가 쓰는 색 이름에 디자인 시스템의 색을 연결한다.
 * 카드·탭 바탕이 Material 기본 보라빛으로 물들지 않게 surface 계열은 모두 카드 색으로 둔다.
 */
private fun colorScheme(palette: AppPalette): ColorScheme {
    val textSecondary = palette.textPrimary.copy(alpha = 0.84f)
    val base = if (palette.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = palette.accent,
        onPrimary = palette.onAccent,
        primaryContainer = palette.accent,
        onPrimaryContainer = palette.onAccent,
        secondaryContainer = palette.buttonSecondary,
        onSecondaryContainer = palette.textPrimary,
        background = palette.background,
        onBackground = palette.textPrimary,
        surface = palette.surface,
        onSurface = palette.textPrimary,
        surfaceVariant = palette.buttonSecondary,
        onSurfaceVariant = textSecondary,
        surfaceTint = palette.surface,
        surfaceBright = palette.surface,
        surfaceDim = palette.background,
        surfaceContainerLowest = palette.surface,
        surfaceContainerLow = palette.surface,
        surfaceContainer = palette.surface,
        surfaceContainerHigh = palette.surface,
        surfaceContainerHighest = palette.surface,
        outline = palette.outline,
        outlineVariant = palette.outline
    )
}

/** 버튼·칩은 small·medium(모서리 12), 카드는 large·extraLarge(모서리 24)를 쓴다. */
private val AppShapes = Shapes(
    small = RoundedCornerShape(AppDimens.ChipRadius),
    medium = RoundedCornerShape(AppDimens.ChipRadius),
    large = RoundedCornerShape(AppDimens.CardRadius),
    extraLarge = RoundedCornerShape(AppDimens.CardRadius)
)

/**
 * 앱 전체 테마. GUI를 모두 다크로 만들기로 해서 차량 설정과 상관없이 다크가 기본이다.
 * [darkTheme]을 false로 주면 Figma의 Light 모드 값으로 그린다.
 */
@Composable
fun AppTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme(palette),
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
