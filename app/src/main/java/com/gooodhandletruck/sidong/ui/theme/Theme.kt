package com.gooodhandletruck.sidong.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Material 컴포넌트가 쓰는 색 이름에 공통 규격의 색을 연결한다.
 * 카드·탭 바탕이 Material 기본 보라빛으로 물들지 않게 surface 계열은 모두 흰색으로 둔다.
 */
private val AppColorScheme = lightColorScheme(
    primary = AppColors.Accent,
    onPrimary = AppColors.OnAccent,
    primaryContainer = AppColors.Accent,
    onPrimaryContainer = AppColors.OnAccent,
    secondaryContainer = AppColors.ButtonSecondary,
    onSecondaryContainer = AppColors.TextPrimary,
    background = AppColors.Background,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.ButtonSecondary,
    onSurfaceVariant = AppColors.TextSecondary,
    surfaceTint = AppColors.Surface,
    surfaceBright = AppColors.Surface,
    surfaceDim = AppColors.Background,
    surfaceContainerLowest = AppColors.Surface,
    surfaceContainerLow = AppColors.Surface,
    surfaceContainer = AppColors.Surface,
    surfaceContainerHigh = AppColors.Surface,
    surfaceContainerHighest = AppColors.Surface,
    outline = AppColors.Outline,
    outlineVariant = AppColors.Outline
)

/** 버튼·칩은 small·medium(모서리 24), 카드는 large·extraLarge(모서리 28)를 쓴다. */
private val AppShapes = Shapes(
    small = RoundedCornerShape(AppDimens.ButtonRadius),
    medium = RoundedCornerShape(AppDimens.ButtonRadius),
    large = RoundedCornerShape(AppDimens.CardRadius),
    extraLarge = RoundedCornerShape(AppDimens.CardRadius)
)

/** 앱 전체 테마. 밝은 테마 하나만 있다. */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
