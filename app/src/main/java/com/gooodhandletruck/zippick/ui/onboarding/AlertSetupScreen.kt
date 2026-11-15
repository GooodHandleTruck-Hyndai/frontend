package com.gooodhandletruck.zippick.ui.onboarding

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gooodhandletruck.zippick.R
import com.gooodhandletruck.zippick.data.AlertConditions
import com.gooodhandletruck.zippick.data.AreaRange
import com.gooodhandletruck.zippick.data.ComplexType
import com.gooodhandletruck.zippick.data.PriceRange
import com.gooodhandletruck.zippick.ui.WindowLayout
import com.gooodhandletruck.zippick.ui.component.PrimaryButton
import com.gooodhandletruck.zippick.ui.component.SecondaryButton
import com.gooodhandletruck.zippick.ui.component.ToggleChip
import com.gooodhandletruck.zippick.ui.navigation.Screen
import com.gooodhandletruck.zippick.ui.theme.AppColors
import com.gooodhandletruck.zippick.ui.theme.AppDimens
import com.gooodhandletruck.zippick.ui.theme.AppType

// 마지막 칩 줄과 버튼 사이(Figma 시안 플로우_14). TODO: Dev Mode 값과 다르면 여기만 고친다
private val BUTTON_GAP = 48.dp

/**
 * S-02를 ViewModel에 잇는다. 열 때마다 저장한 조건에서 시작한다.
 *
 * @param onFinishOnboarding 첫 실행에서 두 버튼 모두 저장(또는 끄기) 뒤 부른다. 첫 실행을 끝내고 S-03으로 간다
 * @param onSaved 설정에서 [저장]을 누른 뒤 부른다. 연 화면(S-07·S-06)으로 돌아간다
 */
@Composable
fun AlertSetupRoute(
    screen: Screen.AlertSetup,
    window: WindowLayout,
    onFinishOnboarding: () -> Unit,
    onSaved: () -> Unit,
    onBack: (() -> Unit)?
) {
    val viewModel: AlertSetupViewModel = viewModel()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.startEditing() }
    AlertSetupScreen(
        isOnboarding = screen.isOnboarding,
        window = window,
        draft = draft,
        onToggleType = viewModel::toggleType,
        onTogglePrice = viewModel::togglePrice,
        onToggleArea = viewModel::toggleArea,
        onAccept = {
            viewModel.accept()
            if (screen.isOnboarding) onFinishOnboarding() else onSaved()
        },
        onLater = {
            viewModel.later()
            onFinishOnboarding()
        },
        onBack = onBack
    )
}

/**
 * S-02 알림 조건(Figma 시안 플로우_14). 제목·설명 아래에 유형·가격대·전용면적 알약 칩이 있고, 항목마다 여러 개를 고를 수 있다.
 * 첫 실행에서는 둥근 판 가운데에 단계 배지 "2 / 2"를 두고, 아래에 [이 조건으로 알림 받기](주 버튼)·[나중에 할게요]를 세로로 쌓는다.
 * 설정에서 열면 왼쪽 탭 옆에 [뒤로]와 [저장] 하나를 둔다.
 * 항목 하나를 모두 끄면 맞는 단지가 없어서 [이 조건으로 알림 받기]·[저장]을 누를 수 없다.
 */
@Composable
fun AlertSetupScreen(
    isOnboarding: Boolean,
    window: WindowLayout,
    draft: AlertConditions,
    onToggleType: (ComplexType) -> Unit,
    onTogglePrice: (PriceRange) -> Unit,
    onToggleArea: (AreaRange) -> Unit,
    onAccept: () -> Unit,
    onLater: () -> Unit,
    onBack: (() -> Unit)?
) {
    val form: @Composable ColumnScope.() -> Unit = {
        AlertSetupForm(
            isOnboarding = isOnboarding,
            draft = draft,
            onToggleType = onToggleType,
            onTogglePrice = onTogglePrice,
            onToggleArea = onToggleArea,
            onAccept = onAccept,
            onLater = onLater
        )
    }
    if (isOnboarding) {
        OnboardingContent(window = window, content = form)
    } else {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = ONBOARDING_CONTENT_WIDTH + window.contentPadding * 2)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = window.contentPadding,
                    end = window.contentPadding,
                    top = AppDimens.ContentPaddingTop,
                    bottom = AppDimens.ContentPaddingVertical
                )
        ) {
            if (onBack != null) {
                SecondaryButton(
                    text = stringResource(R.string.action_back),
                    onClick = onBack,
                    height = AppDimens.ButtonHeightM,
                    icon = R.drawable.ic_chevron_left
                )
                Spacer(Modifier.height(AppDimens.SpaceL))
            }
            form()
        }
    }
}

/** 단계 배지(첫 실행) → 제목 → 설명 → 칩 세 묶음 → 버튼. 간격은 시안 캡처에서 잰 값이다 */
@Composable
private fun ColumnScope.AlertSetupForm(
    isOnboarding: Boolean,
    draft: AlertConditions,
    onToggleType: (ComplexType) -> Unit,
    onTogglePrice: (PriceRange) -> Unit,
    onToggleArea: (AreaRange) -> Unit,
    onAccept: () -> Unit,
    onLater: () -> Unit
) {
    if (isOnboarding) {
        StepBadge(step = 2, total = 2)
        Spacer(Modifier.height(AppDimens.SpaceL))
    }
    Text(text = stringResource(R.string.alert_title), style = AppType.Title1, color = AppColors.TextPrimary)
    Spacer(Modifier.height(AppDimens.SpaceM))
    Text(text = stringResource(R.string.alert_description), style = AppType.Body2, color = AppColors.TextSecondary)
    Spacer(Modifier.height(AppDimens.SpaceL))
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceL)) {
        ChipSection(title = stringResource(R.string.alert_section_type)) {
            ComplexType.entries.forEach { type ->
                ToggleChip(
                    text = typeLabel(type),
                    checked = type in draft.types,
                    enabled = type.available,
                    onCheckedChange = { onToggleType(type) }
                )
            }
        }
        ChipSection(title = stringResource(R.string.alert_section_price)) {
            PriceRange.entries.forEach { range ->
                ToggleChip(
                    text = priceRangeLabel(range),
                    checked = range in draft.prices,
                    onCheckedChange = { onTogglePrice(range) }
                )
            }
        }
        ChipSection(title = stringResource(R.string.alert_section_area)) {
            AreaRange.entries.forEach { range ->
                ToggleChip(
                    text = areaRangeLabel(range),
                    checked = range in draft.areas,
                    onCheckedChange = { onToggleArea(range) }
                )
            }
        }
    }
    Spacer(Modifier.height(BUTTON_GAP))
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceS)) {
        val accept = stringResource(if (isOnboarding) R.string.action_alert_accept else R.string.action_save)
        PrimaryButton(
            text = accept,
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth(),
            height = AppDimens.ButtonHeightM,
            enabled = draft.isComplete
        )
        if (isOnboarding) {
            // 시안에서 [나중에 할게요]는 창 배경색 바탕이다(판보다 밝다)
            SecondaryButton(
                text = stringResource(R.string.action_alert_later),
                onClick = onLater,
                modifier = Modifier.fillMaxWidth(),
                height = AppDimens.ButtonHeightM,
                containerColor = AppColors.Background
            )
        }
    }
}

/** 항목 이름(label1)과 알약 칩 한 줄. 창이 좁으면 칩 줄만 옆으로 민다 */
@Composable
private fun ChipSection(title: String, chips: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceM)) {
        Text(text = title, style = AppType.Label1, color = AppColors.TextSecondary)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceS)
        ) {
            chips()
        }
    }
}
