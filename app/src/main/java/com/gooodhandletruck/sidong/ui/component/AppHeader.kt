package com.gooodhandletruck.sidong.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.ui.theme.AppColors
import com.gooodhandletruck.sidong.ui.theme.AppDimens
import com.gooodhandletruck.sidong.ui.theme.AppType

private const val LOGO_ICON_RATIO = 0.6f

/**
 * 정차 중 화면 맨 위 머리글(높이 96): 앱 로고, 앱 이름(titleS), 오른쪽 끝 상태 알약 "정차 중" (시안 머리글).
 * 왼쪽 탭이 보이면 [startPadding]으로 로고를 탭 가운데에 맞춘다. 주행 중 화면(S-08·S-09)에는 두지 않는다.
 */
@Composable
fun AppHeader(startPadding: Dp, endPadding: Dp, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimens.HeaderHeight)
            .padding(start = startPadding, end = endPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceS)
    ) {
        AppLogo()
        Text(
            text = stringResource(R.string.brand_name),
            style = AppType.TitleS,
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        StatusPill(text = stringResource(R.string.status_parked))
    }
}

/** 앱 로고: 강조색 둥근 네모 안에 흰 건물 그림 */
@Composable
fun AppLogo(modifier: Modifier = Modifier, size: Dp = AppDimens.LogoSize) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(AppDimens.LogoRadius))
            .background(AppColors.Accent),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_app_logo),
            contentDescription = null,
            tint = AppColors.OnAccent,
            modifier = Modifier.size(size * LOGO_ICON_RATIO)
        )
    }
}

/** 지금 상태를 알리는 알약(labelL): "정차 중". 누를 수 없다 */
@Composable
fun StatusPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppType.LabelL,
        color = AppColors.TextSecondary,
        maxLines = 1,
        modifier = modifier
            .clip(CircleShape)
            .background(AppColors.ButtonSecondary)
            .padding(horizontal = AppDimens.SpaceM, vertical = AppDimens.SpaceXs)
    )
}

/**
 * 동그라미 안 아이콘 하나(방금 들은 단지 띠, 빈 상세 칸). [filled]면 강조색 바탕에 흰 아이콘,
 * 아니면 옅은 강조색 바탕에 강조색 아이콘이다.
 */
@Composable
fun IconBadge(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    size: Dp = AppDimens.LogoSize,
    filled: Boolean = true
) {
    val background: Color = if (filled) AppColors.Accent else AppColors.AccentSoft
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (filled) AppColors.OnAccent else AppColors.Accent,
            modifier = Modifier.size(size * LOGO_ICON_RATIO)
        )
    }
}
