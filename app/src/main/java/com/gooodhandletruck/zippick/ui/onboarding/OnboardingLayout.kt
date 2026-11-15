package com.gooodhandletruck.zippick.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gooodhandletruck.zippick.R
import com.gooodhandletruck.zippick.ui.WindowLayout
import com.gooodhandletruck.zippick.ui.theme.AppColors
import com.gooodhandletruck.zippick.ui.theme.AppDimens
import com.gooodhandletruck.zippick.ui.theme.AppType

/*
 * 첫 실행(S-01·S-02) 공통 틀. Figma 시안 플로우_13·14의 onboarding_content.
 * 수치는 시안 캡처에서 버튼 높이를 기준으로 잰 값이다.
 * TODO: 미소님 피그마 Dev Mode 값과 다르면 아래 상수만 고친다
 */

/** 첫 실행 내용 칸의 너비. 판 가운데에 놓는다 */
internal val ONBOARDING_CONTENT_WIDTH = 928.dp

/**
 * 머리글 아래를 둥근 판(좌우·아래 여백 20, 모서리 32)으로 덮고, 그 가운데(가로·세로)에 너비
 * [ONBOARDING_CONTENT_WIDTH]로 [content]를 놓는다. 좁은 창(840)에서는 좌우 여백을 남기고 판 너비에 맞춘다.
 * 내용이 판보다 길면(좁은 창의 S-02) 세로로 민다.
 */
@Composable
fun OnboardingContent(window: WindowLayout, content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = AppDimens.PanelInset, end = AppDimens.PanelInset, bottom = AppDimens.PanelInset)
            .clip(RoundedCornerShape(AppDimens.PanelRadius))
            .background(AppColors.Panel)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // 스크롤 안의 내용 높이를 판 높이 이상으로 두어야 짧은 내용이 세로 가운데에 온다
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = window.contentPadding, vertical = AppDimens.ContentPaddingVertical),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = ONBOARDING_CONTENT_WIDTH)
                    .fillMaxWidth(),
                content = content
            )
        }
    }
}

/** 첫 실행 단계 배지 "1 / 2"(label1Medium). 회색 알약이고 누를 수 없다 */
@Composable
fun StepBadge(step: Int, total: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(AppColors.ButtonSecondary)
            .padding(horizontal = AppDimens.SpaceS, vertical = AppDimens.SpaceXs),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.onboarding_step, step, total),
            style = AppType.Label1Medium,
            color = AppColors.TextSecondary
        )
    }
}
