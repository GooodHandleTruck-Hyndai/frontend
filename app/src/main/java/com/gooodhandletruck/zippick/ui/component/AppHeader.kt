package com.gooodhandletruck.zippick.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gooodhandletruck.zippick.R
import com.gooodhandletruck.zippick.ui.theme.AppColors
import com.gooodhandletruck.zippick.ui.theme.AppDimens

private const val LOGO_ICON_RATIO = 0.6f

// 안 읽은 알림 점: 80×80 알림 아이콘에서 종의 오른쪽 위 어깨에 둔다(Figma 시안)
private val ALERT_DOT_SIZE = 12.dp
private val ALERT_DOT_OFFSET_X = 46.dp
private val ALERT_DOT_OFFSET_Y = 19.dp

/**
 * 정차 중 화면 맨 위 머리글. 왼쪽에 로고(심볼 + 집픽), 오른쪽 끝에 알림 아이콘을 둔다(Figma 시안 플로우_13·14).
 * 여백은 시안대로 좌우 32, 위 20이다. 창 크기와 왼쪽 탭이 있는지와 상관없이 같다.
 * 알림 아이콘을 누르면 S-10 새 거래 알림이 열리고, 안 읽은 알림이 있으면 빨간 점을 찍는다.
 * [onOpenAlerts]가 null이면(첫 실행 S-01·S-02) 알림 아이콘을 두지 않는다.
 * 주행 중 화면(S-08·S-09)에는 두지 않는다.
 */
@Composable
fun AppHeader(
    modifier: Modifier = Modifier,
    hasUnreadAlerts: Boolean = false,
    onOpenAlerts: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = AppDimens.HeaderPaddingHorizontal,
                end = AppDimens.HeaderPaddingHorizontal,
                top = AppDimens.HeaderPaddingTop
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = stringResource(R.string.brand_name),
            tint = AppColors.TextPrimary,
            modifier = Modifier
                .size(width = AppDimens.HeaderLogoWidth, height = AppDimens.HeaderLogoHeight)
                .semantics { heading() }
        )
        Spacer(modifier = Modifier.weight(1f))
        if (onOpenAlerts != null) {
            AlertsButton(hasUnread = hasUnreadAlerts, onClick = onOpenAlerts)
        }
    }
}

/** 머리글 오른쪽 알림 아이콘(80×80). 안 읽은 알림이 있으면 종 오른쪽 위에 빨간 점을 찍는다 */
@Composable
private fun AlertsButton(hasUnread: Boolean, onClick: () -> Unit) {
    val description = stringResource(if (hasUnread) R.string.cd_alerts_unread else R.string.cd_alerts)
    Box(
        modifier = Modifier
            .size(AppDimens.HeaderIconButton)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_notice),
            contentDescription = null,
            tint = AppColors.TextSecondary,
            modifier = Modifier.fillMaxSize()
        )
        if (hasUnread) {
            Box(
                modifier = Modifier
                    .offset(x = ALERT_DOT_OFFSET_X, y = ALERT_DOT_OFFSET_Y)
                    .size(ALERT_DOT_SIZE)
                    .clip(CircleShape)
                    .background(AppColors.AlertDot)
            )
        }
    }
}

/** 앱 로고: 디자인 시스템의 집픽 심볼(210×240)을 [size] 정사각형 안에 강조색으로 그린다 */
@Composable
fun AppLogo(modifier: Modifier = Modifier, size: Dp = AppDimens.LogoSize) {
    Icon(
        painter = painterResource(R.drawable.logo_symbol),
        contentDescription = null,
        tint = AppColors.Accent,
        modifier = modifier.size(size)
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
