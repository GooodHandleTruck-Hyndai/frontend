package com.gooodhandletruck.zippick.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gooodhandletruck.zippick.BuildConfig
import com.gooodhandletruck.zippick.R
import com.gooodhandletruck.zippick.data.AlertSettings
import com.gooodhandletruck.zippick.ui.WindowLayout
import com.gooodhandletruck.zippick.ui.component.SelectChip
import com.gooodhandletruck.zippick.ui.format.formatDistance
import com.gooodhandletruck.zippick.ui.onboarding.conditionsSummary
import com.gooodhandletruck.zippick.ui.theme.AppColors
import com.gooodhandletruck.zippick.ui.theme.AppDimens
import com.gooodhandletruck.zippick.ui.theme.AppType

// 설정 칸이 넓은 창에서 너무 길어지지 않게 한다
private val CONTENT_MAX_WIDTH = 800.dp

/** S-07이 고를 수 있는 주변 범위(화면 설계서 S-03 범위 칩과 같다) */
private val RADIUS_CHOICES = listOf(500, 1000, 3000)

/**
 * S-07을 ViewModel에 잇는다.
 *
 * @param onEditAlert S-02(설정에서 연 모양)를 연다. 조건 없이 알림을 켤 때도 부른다
 */
@Composable
fun SettingsRoute(window: WindowLayout, onEditAlert: () -> Unit) {
    val viewModel: SettingsViewModel = viewModel()
    val alertSettings by viewModel.alertSettings.collectAsStateWithLifecycle()
    val defaultRadius by viewModel.defaultRadiusMeters.collectAsStateWithLifecycle()
    SettingsScreen(
        window = window,
        alertSettings = alertSettings,
        defaultRadiusMeters = defaultRadius,
        onAlertsChange = { enabled -> if (!viewModel.setAlertsEnabled(enabled)) onEditAlert() },
        onEditAlert = onEditAlert,
        onSelectRadius = viewModel::setDefaultRadius
    )
}

/**
 * S-07 설정. 위에서부터 주행 중 음성 알림(스위치), 알림 조건(지금 조건 요약, 누르면 S-02),
 * 주변 범위 기본값(500m·1km·3km), 앱 정보(버전, 가상 데이터 안내)다. 바꾸면 바로 저장한다.
 */
@Composable
fun SettingsScreen(
    window: WindowLayout,
    alertSettings: AlertSettings,
    defaultRadiusMeters: Int,
    onAlertsChange: (Boolean) -> Unit,
    onEditAlert: () -> Unit,
    onSelectRadius: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = CONTENT_MAX_WIDTH + window.contentPadding * 2)
            .verticalScroll(rememberScrollState())
            .padding(
                start = window.contentPadding,
                end = window.contentPadding,
                top = AppDimens.ContentPaddingTop,
                bottom = AppDimens.ContentPaddingVertical
            ),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceM)
    ) {
        Text(text = stringResource(R.string.screen_settings), style = AppType.Title2, color = AppColors.TextPrimary)

        // 줄 전체를 눌러도 스위치가 바뀐다(누를 곳이 76 이상)
        SettingsCard(
            modifier = Modifier.toggleable(
                value = alertSettings.isOn,
                role = Role.Switch,
                onValueChange = onAlertsChange
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.settings_alerts),
                    style = AppType.Body1,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = alertSettings.isOn,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(checkedTrackColor = AppColors.Accent)
                )
            }
        }

        SettingsCard(modifier = Modifier.clickable(role = Role.Button, onClick = onEditAlert)) {
            Text(
                text = stringResource(R.string.settings_conditions),
                style = AppType.Body1,
                color = AppColors.TextPrimary
            )
            Text(
                text = alertSettings.conditions?.let { conditionsSummary(it) }
                    ?: stringResource(R.string.settings_conditions_none),
                style = AppType.Body3,
                color = AppColors.TextSecondary
            )
        }

        SettingsCard {
            Text(
                text = stringResource(R.string.settings_default_radius),
                style = AppType.Body1,
                color = AppColors.TextPrimary
            )
            Row(
                modifier = Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceS)
            ) {
                RADIUS_CHOICES.forEach { meters ->
                    SelectChip(
                        text = formatDistance(meters.toDouble()),
                        selected = meters == defaultRadiusMeters,
                        onClick = { onSelectRadius(meters) }
                    )
                }
            }
        }

        SettingsCard {
            Text(
                text = stringResource(R.string.settings_app_info),
                style = AppType.Body1,
                color = AppColors.TextPrimary
            )
            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = AppType.Body3,
                color = AppColors.TextSecondary
            )
            Text(
                text = stringResource(R.string.settings_demo_data),
                style = AppType.Body3,
                color = AppColors.TextSecondary
            )
        }
    }
}

/** 설정 한 칸: 카드 모양(카드 바탕, 테두리, 모서리 24) 안에 줄을 쌓는다 */
@Composable
private fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(AppDimens.CardRadius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppDimens.ButtonHeight)
            .clip(shape)
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Outline, shape)
            .then(modifier)
            .padding(AppDimens.SpaceM),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceS, Alignment.CenterVertically),
        content = content
    )
}
