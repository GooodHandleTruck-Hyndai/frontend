package com.gooodhandletruck.zippick.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gooodhandletruck.zippick.ui.theme.AppColors
import com.gooodhandletruck.zippick.ui.theme.AppDimens
import com.gooodhandletruck.zippick.ui.theme.AppType

/**
 * 주 버튼(Figma BoxButton의 Brand): 강조색 바탕. 한 화면에 하나만 둔다. [enabled]가 false면 흐려지고 누를 수 없다.
 * [icon]을 주면 글자 왼쪽에 아이콘을 그린다([♡ 저장]).
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = AppDimens.ButtonHeight,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null
) {
    AppButton(
        text = text,
        onClick = onClick,
        containerColor = AppColors.Accent,
        contentColor = AppColors.OnAccent,
        modifier = modifier,
        height = height,
        enabled = enabled,
        icon = icon
    )
}

/**
 * 보조 버튼: 회색 바탕 + 기본 글자색.
 * [height]를 [AppDimens.ButtonHeightM]으로 주면 머리글·카드 안에 넣는 Medium 버튼이 된다.
 * [enabled]가 false면 누를 수 없고 글자가 흐려진다(S-09 "저장했어요").
 * [iconTint]를 주면 아이콘만 그 색으로 칠한다(저장한 뒤 강조색 하트).
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = AppDimens.ButtonHeight,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    iconTint: Color? = null
) {
    AppButton(
        text = text,
        onClick = onClick,
        containerColor = AppColors.ButtonSecondary,
        contentColor = AppColors.TextPrimary,
        modifier = modifier,
        height = height,
        enabled = enabled,
        icon = icon,
        iconTint = iconTint
    )
}

/** Figma BoxButton의 크기별 규격. 주행 중 버튼(160)처럼 100보다 높은 버튼도 Large 규격을 쓴다 */
private class ButtonSpec(
    val radius: Dp,
    val horizontalPadding: Dp,
    val gap: Dp,
    val minWidth: Dp,
    val textStyle: TextStyle,
    val iconSize: Dp
)

private val LargeSpec = ButtonSpec(
    radius = AppDimens.ButtonRadiusL,
    horizontalPadding = 40.dp,
    gap = 16.dp,
    minWidth = 140.dp,
    textStyle = AppType.ButtonL,
    iconSize = AppDimens.IconL
)

private val MediumSpec = ButtonSpec(
    radius = AppDimens.ButtonRadiusM,
    horizontalPadding = 32.dp,
    gap = 12.dp,
    minWidth = 120.dp,
    textStyle = AppType.ButtonM,
    iconSize = AppDimens.IconS
)

@Composable
private fun AppButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier,
    height: Dp,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    iconTint: Color? = null
) {
    val spec = if (height >= AppDimens.ButtonHeight) LargeSpec else MediumSpec
    Button(
        onClick = onClick,
        modifier = modifier
            .height(height)
            .widthIn(min = spec.minWidth),
        enabled = enabled,
        shape = RoundedCornerShape(spec.radius),
        // 누를 수 없을 때(Figma State=Disabled): 바탕 5%, 테두리 10%, 글자 32%
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = AppColors.TextPrimary.copy(alpha = 0.05f),
            disabledContentColor = AppColors.TextPrimary.copy(alpha = 0.32f)
        ),
        border = if (enabled) null else BorderStroke(1.dp, AppColors.Outline),
        contentPadding = PaddingValues(horizontal = spec.horizontalPadding)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spec.gap)
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    // 누를 수 없을 때는 글자처럼 흐리게 둔다
                    tint = iconTint?.takeIf { enabled } ?: LocalContentColor.current,
                    modifier = Modifier.size(spec.iconSize)
                )
            }
            // 버튼이 좁아 글자가 넘치면 말줄임 대신 두 줄로 쓴다(높이 100에 두 줄이 들어간다)
            Text(text = text, style = spec.textStyle, textAlign = TextAlign.Center)
        }
    }
}
