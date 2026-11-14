package com.gooodhandletruck.zippick.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gooodhandletruck.zippick.R
import com.gooodhandletruck.zippick.data.CarLocation
import com.gooodhandletruck.zippick.data.model.ComplexSummary
import com.gooodhandletruck.zippick.data.model.RepresentativePrice
import com.gooodhandletruck.zippick.ui.format.RelativePlace
import com.gooodhandletruck.zippick.ui.format.formatArea
import com.gooodhandletruck.zippick.ui.format.formatPrice
import com.gooodhandletruck.zippick.ui.theme.AppColors
import com.gooodhandletruck.zippick.ui.theme.AppDimens
import com.gooodhandletruck.zippick.ui.theme.AppType

private const val DEFAULT_PERIOD_DAYS = 90
private const val DAYS_PER_MONTH = 30

/**
 * 단지 카드 한 장에 그릴 값. 방향과 거리는 지금 위치와 차의 방향으로 미리 계산해 둔다.
 * 차 위치를 아직 못 받았으면 [place]가 null이고 방향·거리 줄을 숨긴다(S-05·S-06).
 */
data class ComplexCardModel(
    val complex: ComplexSummary,
    val place: RelativePlace?
)

/** [car]에서 본 카드 값 */
fun ComplexSummary.toCardModel(car: CarLocation) = ComplexCardModel(complex = this, place = car.placeOf(lat, lon))

/**
 * 단지 카드 (S-03·S-05·S-06 공통). 시안(데모 화면)처럼 사진 칸 없이 글자와 숫자로 그린다.
 * 왼쪽 [leading]에는 번호 배지(S-03, 약도 번호와 같다)나 들은 시각(S-06)을 둔다.
 * 가운데는 위에서부터 이름(body2Bold), 방향·거리 줄(강조색), 가격(title2), 면적·기간·기준일(label2)이다.
 * 최근 거래가 없으면 가격 자리에 "최근 3개월 거래가 없어요"를 쓴다 (화면 설계서 S-03 '상태별 문구').
 * 1:2 창에서 오른쪽 칸에 상세를 연 카드는 [selected]로 강조색 테두리(3)를 두른다.
 * [showSavedMark]면 저장한 단지에 "저장함" 칩을 붙인다(S-06).
 * 오른쪽 끝에는 [trailing] 버튼(S-05 [길 안내])을 두고, 없으면 상세가 열린다는 › 표시를 둔다.
 */
@Composable
fun ComplexCard(
    model: ComplexCardModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    showSavedMark: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(AppDimens.CardRadius)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.Surface)
            .border(
                width = if (selected) AppDimens.SelectedBorder else 1.dp,
                color = if (selected) AppColors.Accent else AppColors.Outline,
                shape = shape
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AppDimens.SpaceL, vertical = AppDimens.SpaceM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceM)
    ) {
        leading?.invoke()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceXs)
        ) {
            Text(
                text = model.complex.name,
                style = AppType.Body2Bold,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val savedMark = showSavedMark && model.complex.saved
            if (model.place != null || savedMark) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceS)
                ) {
                    model.place?.let { DirectionLabel(place = it) }
                    if (savedMark) SavedMark()
                }
            }
            PriceLines(price = model.complex.price)
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = AppColors.TextTertiary,
                modifier = Modifier.size(AppDimens.IconL)
            )
        }
    }
}

/** 가격(title2)과 그 아래 "84㎡ · 최근 3개월 · 10월 31일 기준"(label2) */
@Composable
private fun PriceLines(price: RepresentativePrice?) {
    val months = (price?.periodDays ?: DEFAULT_PERIOD_DAYS) / DAYS_PER_MONTH
    val average = price?.avgPriceManwon
    val lastTrade = price?.lastTrade
    if (average != null) {
        Text(text = formatPrice(average), style = AppType.Title2, color = AppColors.TextPrimary)
    } else if (lastTrade != null) {
        // 평균이 없으면 마지막 거래를 보여 준다. 아래 줄에 계약 월을 꼭 같이 쓴다
        Text(text = formatPrice(lastTrade.priceManwon), style = AppType.Title2, color = AppColors.TextPrimary)
    } else {
        Text(
            text = stringResource(R.string.card_no_recent_trades, months),
            style = AppType.Body2,
            color = AppColors.TextSecondary
        )
    }
    if (price != null) {
        val area = formatArea(price.areaM2)
        val asOf = stringResource(R.string.date_month_day, price.asOf.monthValue, price.asOf.dayOfMonth)
        val meta = if (average != null) {
            stringResource(R.string.card_meta, area, months, asOf)
        } else if (lastTrade != null) {
            val month = stringResource(
                R.string.date_year_month,
                lastTrade.contractDate.year,
                lastTrade.contractDate.monthValue
            )
            stringResource(R.string.card_meta_last_trade, area, month)
        } else {
            stringResource(R.string.card_meta_no_trade, area, asOf)
        }
        Text(text = meta, style = AppType.Label2, color = AppColors.TextTertiary)
    }
}

/** S-06 카드 왼쪽의 들은 시각 "14:32"(body3, 너비 88) */
@Composable
fun CardTime(time: String, modifier: Modifier = Modifier) {
    Text(
        text = time,
        style = AppType.Body3,
        color = AppColors.TextTertiary,
        modifier = modifier.width(AppDimens.TimeColumnWidth)
    )
}
