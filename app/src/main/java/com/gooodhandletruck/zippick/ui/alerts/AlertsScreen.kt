package com.gooodhandletruck.zippick.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gooodhandletruck.zippick.R
import com.gooodhandletruck.zippick.data.remote.NewTradeAlert
import com.gooodhandletruck.zippick.ui.WindowLayout
import com.gooodhandletruck.zippick.ui.component.LoadingCards
import com.gooodhandletruck.zippick.ui.component.SecondaryButton
import com.gooodhandletruck.zippick.ui.component.StatusBox
import com.gooodhandletruck.zippick.ui.format.formatArea
import com.gooodhandletruck.zippick.ui.format.formatPrice
import com.gooodhandletruck.zippick.ui.theme.AppColors
import com.gooodhandletruck.zippick.ui.theme.AppDimens
import com.gooodhandletruck.zippick.ui.theme.AppType

/** S-10을 ViewModel에 잇는다. 1:2 창에서 상세를 닫을 때도 다시 받아 온다 */
@Composable
fun AlertsRoute(
    window: WindowLayout,
    selectedComplexId: String?,
    onOpenComplex: (String) -> Unit,
    onBack: (() -> Unit)?
) {
    val viewModel: AlertsViewModel = viewModel()
    val content by viewModel.content.collectAsStateWithLifecycle()
    LaunchedEffect(selectedComplexId == null) { viewModel.refresh() }
    AlertsScreen(
        window = window,
        content = content,
        selectedComplexId = selectedComplexId,
        onOpen = { alert ->
            viewModel.markRead(alert)
            onOpenComplex(alert.complexId)
        },
        onRetry = viewModel::retry,
        onBack = onBack
    )
}

/**
 * S-10 새 거래 알림. 저장한 단지·관심 조건에 맞는 단지에 새 실거래가 신고되면 서버가 만든 알림을 최신순으로 보여 준다.
 * 한 줄에 단지 이름, 다음 줄에 "84㎡ · 12억 5,000만 · 10월 12일 계약". 안 읽은 알림은 앞에 강조색 점을 찍는다.
 * 누르면 읽음으로 바꾸고 단지 상세(S-04)를 연다. 정차 중에만 열린다.
 */
@Composable
fun AlertsScreen(
    window: WindowLayout,
    content: AlertsContent,
    selectedComplexId: String?,
    onOpen: (NewTradeAlert) -> Unit,
    onRetry: () -> Unit,
    onBack: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = AppDimens.ListPaneWidth + window.contentPadding * 2)
            .padding(start = window.contentPadding, end = window.contentPadding, top = AppDimens.ContentPaddingTop),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceM)
    ) {
        if (onBack != null) {
            SecondaryButton(
                text = stringResource(R.string.action_back),
                onClick = onBack,
                height = AppDimens.ButtonHeightM,
                icon = R.drawable.ic_chevron_left
            )
        }
        Text(text = stringResource(R.string.screen_alerts), style = AppType.DisplayM, color = AppColors.TextPrimary)
        when (content) {
            AlertsContent.Loading -> LoadingCards(modifier = Modifier.weight(1f))
            AlertsContent.Failed -> StatusBox(
                message = stringResource(R.string.status_load_failed),
                actionLabel = stringResource(R.string.action_retry),
                onAction = onRetry
            )
            is AlertsContent.Loaded -> if (content.alerts.isEmpty()) {
                StatusBox(
                    message = stringResource(R.string.alerts_empty),
                    detail = stringResource(R.string.alerts_empty_detail)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceS),
                    contentPadding = PaddingValues(bottom = AppDimens.ContentPaddingVertical)
                ) {
                    items(content.alerts, key = { it.alertId }) { alert ->
                        AlertCard(
                            alert = alert,
                            selected = alert.complexId == selectedComplexId,
                            onClick = { onOpen(alert) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertCard(alert: NewTradeAlert, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(AppDimens.CardRadius)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppDimens.TouchMin)
            .clip(shape)
            .background(AppColors.Surface)
            .border(
                width = if (selected) AppDimens.SelectedBorder else 1.dp,
                color = if (selected) AppColors.Accent else AppColors.Outline,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(AppDimens.SpaceM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceS)
    ) {
        Box(
            modifier = Modifier
                .size(AppDimens.SpaceS) // 안 읽음 점
                .clip(CircleShape)
                .background(if (alert.read) AppColors.Surface else AppColors.Accent)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceXs)) {
            Text(
                text = alert.name,
                style = AppType.BodyL,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(
                    R.string.alerts_deal,
                    formatArea(alert.areaM2),
                    formatPrice(alert.priceManwon),
                    alert.contractDate.monthValue,
                    alert.contractDate.dayOfMonth
                ),
                style = AppType.BodyS,
                color = if (alert.read) AppColors.TextTertiary else AppColors.TextSecondary
            )
        }
    }
}
