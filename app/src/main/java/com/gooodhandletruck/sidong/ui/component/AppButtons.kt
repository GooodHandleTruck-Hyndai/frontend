package com.gooodhandletruck.sidong.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.gooodhandletruck.sidong.ui.theme.AppColors
import com.gooodhandletruck.sidong.ui.theme.AppDimens
import com.gooodhandletruck.sidong.ui.theme.AppType

/** 주 버튼: 강조색 바탕 + 흰 글자. 한 화면에 하나만 둔다. [enabled]가 false면 회색으로 바뀌고 누를 수 없다 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = AppDimens.ButtonHeight,
    enabled: Boolean = true
) {
    AppButton(
        text = text,
        onClick = onClick,
        containerColor = AppColors.Accent,
        contentColor = AppColors.OnAccent,
        modifier = modifier,
        height = height,
        enabled = enabled
    )
}

/**
 * 보조 버튼: #EDEEF2 바탕 + 검정 글자.
 * [horizontalPadding]을 줄이면 카드 안 [길 안내]처럼 76×76에 가까운 작은 버튼이 된다.
 * [enabled]가 false면 누를 수 없고 글자가 흐려진다(S-09 "저장했어요").
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = AppDimens.ButtonHeight,
    horizontalPadding: Dp = AppDimens.SpaceL,
    enabled: Boolean = true
) {
    AppButton(
        text = text,
        onClick = onClick,
        containerColor = AppColors.ButtonSecondary,
        contentColor = AppColors.TextPrimary,
        modifier = modifier,
        height = height,
        horizontalPadding = horizontalPadding,
        enabled = enabled
    )
}

@Composable
private fun AppButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier,
    height: Dp,
    horizontalPadding: Dp = AppDimens.SpaceL,
    enabled: Boolean = true
) {
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
        Text(text = text, style = AppType.LabelL)
    }
}
