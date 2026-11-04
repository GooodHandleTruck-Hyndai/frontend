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

/** 주 버튼: 강조색 바탕 + 흰 글자. 한 화면에 하나만 둔다. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = AppDimens.ButtonHeight
) {
    AppButton(text, onClick, AppColors.Accent, AppColors.OnAccent, modifier, height)
}

/** 보조 버튼: #EDEEF2 바탕 + 검정 글자 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = AppDimens.ButtonHeight
) {
    AppButton(text, onClick, AppColors.ButtonSecondary, AppColors.TextPrimary, modifier, height)
}

@Composable
private fun AppButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier,
    height: Dp
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(height)
            .widthIn(min = AppDimens.TouchMin),
        shape = RoundedCornerShape(AppDimens.ButtonRadius),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        contentPadding = PaddingValues(horizontal = AppDimens.SpaceL)
    ) {
        Text(text = text, style = AppType.LabelL)
    }
}
