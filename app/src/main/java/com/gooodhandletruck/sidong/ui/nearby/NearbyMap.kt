package com.gooodhandletruck.sidong.ui.nearby

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.data.CarLocation
import com.gooodhandletruck.sidong.data.bearingDegrees
import com.gooodhandletruck.sidong.ui.component.ComplexCardModel
import com.gooodhandletruck.sidong.ui.format.formatDistance
import com.gooodhandletruck.sidong.ui.format.formatPriceShort
import com.gooodhandletruck.sidong.ui.theme.AppColors
import com.gooodhandletruck.sidong.ui.theme.AppDimens
import com.gooodhandletruck.sidong.ui.theme.AppType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// 고른 범위의 바깥 단지도 가장자리에서 잘리지 않게 범위보다 조금 넓게 그린다
private const val EXTENT_FACTOR = 1.15f

// 거리 원: 고른 범위까지의 원만 그린다(500m·1km, 3km를 고르면 3km도)
private val RING_METERS = listOf(500, 1000, 3000)

private val CAR_SIZE = 28.dp
private val DOT_SIZE = 8.dp
private val BUBBLE_HEIGHT = 40.dp
private val BUBBLE_GAP = 4.dp

/**
 * S-03 약도. 지도 타일 없이 앱이 직접 그린다(구글 지도를 쓰지 않는다).
 * 차가 향한 쪽이 위이고, 차를 가운데 두고 거리 원을 그리며 단지마다 가격 말풍선("12.3억")을 띄운다.
 * 말풍선을 누르면 S-04가 열리고, 목록에서 고른 단지([selectedComplexId])는 말풍선이 강조색이다.
 * 최근 3개월 평균이 없는 단지는 말풍선에 "-"를 흐리게 쓴다.
 */
@Composable
fun NearbyMap(
    center: CarLocation,
    cards: List<ComplexCardModel>,
    radius: NearbyRadius,
    selectedComplexId: String?,
    onOpenComplex: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(AppDimens.CardRadius)
    BoxWithConstraints(
        modifier = modifier
            .clip(shape)
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Outline, shape)
    ) {
        // 화면 가운데에서 짧은 쪽 끝까지가 고른 범위의 EXTENT_FACTOR배다
        val halfSpan = min(maxWidth, maxHeight) / 2 - AppDimens.SpaceM
        val dpPerMeter = halfSpan.value / (radius.meters * EXTENT_FACTOR)
        val rings = RING_METERS.filter { it <= radius.meters }

        MapBackground(rings = rings, dpPerMeter = dpPerMeter)
        rings.forEach { meters -> RingLabel(meters = meters, offsetX = -(meters * dpPerMeter).dp) }

        cards.forEach { card ->
            val complex = card.complex
            val relative = Math.toRadians(
                bearingDegrees(center.lat, center.lon, complex.lat, complex.lon) - center.heading
            )
            val distance = (card.place?.distanceMeters ?: 0.0) * dpPerMeter
            val x = (sin(relative) * distance).dp
            val y = (-cos(relative) * distance).dp
            // 약도 밖으로 나가는 단지는 그리지 않는다
            if (abs(x.value) <= maxWidth.value / 2 && abs(y.value) <= maxHeight.value / 2) {
                PriceBubble(
                    card = card,
                    selected = complex.complexId == selectedComplexId,
                    anchorX = maxWidth / 2 + x,
                    anchorY = maxHeight / 2 + y,
                    onClick = { onOpenComplex(complex.complexId) }
                )
            }
        }

        Text(
            text = stringResource(R.string.map_heading_up),
            style = AppType.LabelS,
            color = AppColors.TextTertiary,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(AppDimens.SpaceM)
        )
        Text(
            text = stringResource(R.string.map_legend),
            style = AppType.LabelS,
            color = AppColors.TextTertiary,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(AppDimens.SpaceM)
        )
    }
}

/** 거리 원과 가운데 차 표시(위를 향한 화살표) */
@Composable
private fun MapBackground(rings: List<Int>, dpPerMeter: Float) {
    val density = LocalDensity.current
    Canvas(modifier = Modifier.fillMaxSize()) {
        val middle = Offset(size.width / 2, size.height / 2)
        val stroke = Stroke(width = with(density) { 1.dp.toPx() })
        rings.forEach { meters ->
            drawCircle(
                color = AppColors.Outline,
                radius = with(density) { (meters * dpPerMeter).dp.toPx() },
                center = middle,
                style = stroke
            )
        }
        val half = with(density) { CAR_SIZE.toPx() } / 2
        val arrow = Path().apply {
            moveTo(middle.x, middle.y - half)
            lineTo(middle.x + half * 0.8f, middle.y + half)
            lineTo(middle.x, middle.y + half * 0.5f)
            lineTo(middle.x - half * 0.8f, middle.y + half)
            close()
        }
        drawPath(arrow, color = AppColors.TextPrimary)
    }
}

/** 원 왼쪽 끝에 붙이는 거리 이름 "500m"·"1km" */
@Composable
private fun RingLabel(meters: Int, offsetX: Dp) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = formatDistance(meters.toDouble()),
            style = AppType.LabelS,
            color = AppColors.TextTertiary,
            modifier = Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                layout(placeable.width, placeable.height) {
                    // 원 왼쪽 끝 바로 안쪽, 가운데 줄 아래
                    placeable.place(offsetX.roundToPx() + AppDimens.SpaceXs.roundToPx(), placeable.height / 2)
                }
            }
        )
    }
}

/**
 * 단지 하나의 가격 말풍선. 단지 자리([anchorX], [anchorY])에 점을 찍고 그 바로 위에 말풍선을 둔다.
 * 누를 곳은 말풍선을 가운데 둔 76×76 이상이다.
 */
@Composable
private fun PriceBubble(card: ComplexCardModel, selected: Boolean, anchorX: Dp, anchorY: Dp, onClick: () -> Unit) {
    val average = card.complex.price?.avgPriceManwon
    val label = average?.let(::formatPriceShort) ?: stringResource(R.string.map_no_average)
    val description = stringResource(R.string.map_bubble_description, card.complex.name, label)
    val bubbleShape = RoundedCornerShape(AppDimens.ButtonRadius)

    Box(
        modifier = Modifier
            .placeCenterAt(anchorX, anchorY)
            .size(DOT_SIZE)
            .clip(CircleShape)
            .background(if (selected) AppColors.Accent else AppColors.TextTertiary)
    )
    Box(
        modifier = Modifier
            .placeCenterAt(anchorX, anchorY - DOT_SIZE / 2 - BUBBLE_GAP - BUBBLE_HEIGHT / 2)
            .sizeIn(minWidth = AppDimens.TouchMin, minHeight = AppDimens.TouchMin)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .height(BUBBLE_HEIGHT)
                .clip(bubbleShape)
                .background(if (selected) AppColors.Accent else AppColors.Surface)
                .border(1.dp, if (selected) AppColors.Accent else AppColors.Outline, bubbleShape)
                .padding(horizontal = AppDimens.SpaceS),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = AppType.LabelS,
                color = when {
                    selected -> AppColors.OnAccent
                    average == null -> AppColors.TextTertiary
                    else -> AppColors.TextPrimary
                }
            )
        }
    }
}

/** 이 요소의 가운데를 부모 안의 ([x], [y])에 둔다. 누르는 자리도 함께 옮겨진다 */
private fun Modifier.placeCenterAt(x: Dp, y: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    layout(placeable.width, placeable.height) {
        placeable.place(x.roundToPx() - placeable.width / 2, y.roundToPx() - placeable.height / 2)
    }
}
