package com.gooodhandletruck.sidong.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.data.CarLocation
import com.gooodhandletruck.sidong.data.model.ComplexSummary
import com.gooodhandletruck.sidong.data.model.RepresentativePrice
import com.gooodhandletruck.sidong.ui.format.RelativePlace
import com.gooodhandletruck.sidong.ui.format.formatArea
import com.gooodhandletruck.sidong.ui.format.formatPrice
import com.gooodhandletruck.sidong.ui.theme.AppColors
import com.gooodhandletruck.sidong.ui.theme.AppDimens
import com.gooodhandletruck.sidong.ui.theme.AppType

private const val DEFAULT_PERIOD_DAYS = 90
private const val DAYS_PER_MONTH = 30

/** 단지 카드 한 장에 그릴 값. 방향과 거리는 지금 위치와 차의 방향으로 미리 계산해 둔다. */
data class ComplexCardModel(
    val complex: ComplexSummary,
    val place: RelativePlace
)

/** [car]에서 본 카드 값 */
fun ComplexSummary.toCardModel(car: CarLocation) = ComplexCardModel(complex = this, place = car.placeOf(lat, lon))

/**
 * 단지 카드 (S-03·S-05·S-06 공통): 사진 132×132, 이름(bodyL), 방향·거리 칩, 가격(titleS), 면적·기간·기준일(labelS).
 * 최근 거래가 없으면 가격 자리에 "최근 3개월 거래가 없어요"를 쓴다 (화면 설계서 S-03 '상태별 문구').
 * 1:2 창에서 오른쪽 칸에 상세를 연 카드는 [selected]로 강조색 테두리를 두른다.
 */
@Composable
fun ComplexCard(
    model: ComplexCardModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
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
            .padding(AppDimens.SpaceM),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceM)
    ) {
        ComplexPhoto(
            name = model.complex.name,
            modifier = Modifier
                .size(AppDimens.CardPhotoSize)
                .clip(RoundedCornerShape(AppDimens.ThumbnailRadius))
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceXs)
        ) {
            Text(
                text = model.complex.name,
                style = AppType.BodyL,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            DirectionChip(place = model.place)
            PriceLines(price = model.complex.price)
        }
    }
}

/** 가격(titleS)과 그 아래 "84㎡ · 최근 3개월 · 10월 31일 기준"(labelS) */
@Composable
private fun PriceLines(price: RepresentativePrice?) {
    val months = (price?.periodDays ?: DEFAULT_PERIOD_DAYS) / DAYS_PER_MONTH
    val average = price?.avgPriceManwon
    if (average != null) {
        Text(text = formatPrice(average), style = AppType.TitleS, color = AppColors.TextPrimary)
    } else {
        Text(
            text = stringResource(R.string.card_no_recent_trades, months),
            style = AppType.BodyM,
            color = AppColors.TextSecondary
        )
    }
    if (price != null) {
        val area = formatArea(price.areaM2)
        val asOf = stringResource(R.string.date_month_day, price.asOf.monthValue, price.asOf.dayOfMonth)
        val meta = if (average != null) {
            stringResource(R.string.card_meta, area, months, asOf)
        } else {
            stringResource(R.string.card_meta_no_trade, area, asOf)
        }
        Text(text = meta, style = AppType.LabelS, color = AppColors.TextTertiary)
    }
}
