package com.gooodhandletruck.sidong.ui.component

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.gooodhandletruck.sidong.ui.theme.AppColors
import com.gooodhandletruck.sidong.ui.theme.AppDimens
import com.gooodhandletruck.sidong.ui.theme.AppType

/**
 * 주 버튼: 강조색 바탕 + 흰 글자. 한 화면에 하나만 둔다. [enabled]가 false면 회색으로 바뀌고 누를 수 없다.
 * [icon]을 주면 글자 왼쪽에 아이콘을 그린다(시안의 [♡ 저장]).
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
 * 보조 버튼: #EDEEF2 바탕 + 검정 글자.
 * [horizontalPadding]을 줄이면 카드 안 [길 안내]처럼 76×76에 가까운 작은 버튼이 된다.
 * [enabled]가 false면 누를 수 없고 글자가 흐려진다(S-09 "저장했어요").
 * [iconTint]를 주면 아이콘만 그 색으로 칠한다(저장한 뒤 강조색 하트).
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = AppDimens.ButtonHeight,
    horizontalPadding: Dp = AppDimens.SpaceL,
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
        horizontalPadding = horizontalPadding,
        enabled = enabled,
        icon = icon,
        iconTint = iconTint
    )
}

/** 높이 96 이상이면 큰 글자(ButtonL)와 아이콘 32, 76짜리 작은 버튼이면 ButtonS와 아이콘 28을 쓴다 */
@Composable
private fun AppButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier,
    height: Dp,
    horizontalPadding: Dp = AppDimens.SpaceL,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    iconTint: Color? = null
) {
    val large = height >= AppDimens.ButtonHeight
    Button(
        onClick = onClick,
        modifier = modifier
            .height(height)
            .widthIn(min = AppDimens.TouchMin),
        enabled = enabled,
        shape = RoundedCornerShape(AppDimens.ButtonRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = AppColors.ButtonSecondary,
            disabledContentColor = AppColors.TextTertiary
        ),
        contentPadding = PaddingValues(horizontal = horizontalPadding)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceXs)
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    // 누를 수 없을 때는 글자처럼 흐리게 둔다
                    tint = iconTint?.takeIf { enabled } ?: LocalContentColor.current,
                    modifier = Modifier.size(if (large) AppDimens.IconL else AppDimens.IconS)
                )
            }
            // 버튼이 좁아 글자가 넘치면 말줄임 대신 두 줄로 쓴다(높이 96에 두 줄이 들어간다)
            Text(
                text = text,
                style = if (large) AppType.ButtonL else AppType.ButtonS,
                textAlign = TextAlign.Center
            )
        }
    }
}
