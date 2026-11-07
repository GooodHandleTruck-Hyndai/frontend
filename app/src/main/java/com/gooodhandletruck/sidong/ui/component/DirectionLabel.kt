package com.gooodhandletruck.sidong.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.data.CarLocation
import com.gooodhandletruck.sidong.data.bearingDegrees
import com.gooodhandletruck.sidong.data.distanceMeters
import com.gooodhandletruck.sidong.ui.format.RelativeDirection
import com.gooodhandletruck.sidong.ui.format.RelativePlace
import com.gooodhandletruck.sidong.ui.format.formatDistance
import com.gooodhandletruck.sidong.ui.theme.AppColors
import com.gooodhandletruck.sidong.ui.theme.AppDimens
import com.gooodhandletruck.sidong.ui.theme.AppType

/**
 * 방향·거리 줄: 그 방향으로 돌린 강조색 화살표 + "오른쪽 · 340m" (카드, S-04).
 * 차가 향한 쪽 기준 8방향이고, 1km부터는 "1.2km"다. 화살표는 꾸밈이라 읽어 주지 않는다.
 */
@Composable
fun DirectionLabel(place: RelativePlace, modifier: Modifier = Modifier) {
    val directionName = stringResource(place.direction.label)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceXs)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_direction),
            contentDescription = null,
            tint = AppColors.Accent,
            modifier = Modifier
                .size(AppDimens.IconS)
                .rotate(place.direction.arrowDegrees)
        )
        Text(
            text = stringResource(R.string.direction_chip, directionName, formatDistance(place.distanceMeters)),
            style = AppType.LabelBold,
            color = AppColors.Accent
        )
    }
}

/** 이 차에서 ([lat], [lon])에 있는 단지가 어느 쪽으로 얼마나 떨어져 있는지 */
fun CarLocation.placeOf(lat: Double, lon: Double) = RelativePlace(
    direction = RelativeDirection.of(bearingDegrees(this.lat, this.lon, lat, lon), heading),
    distanceMeters = distanceMeters(this.lat, this.lon, lat, lon)
)

/** 방향 이름. 주행 중 음성 안내(P1)도 같은 이름을 쓴다 */
@get:StringRes
val RelativeDirection.label: Int
    get() = when (this) {
        RelativeDirection.FRONT -> R.string.direction_front
        RelativeDirection.FRONT_RIGHT -> R.string.direction_front_right
        RelativeDirection.RIGHT -> R.string.direction_right
        RelativeDirection.BACK_RIGHT -> R.string.direction_back_right
        RelativeDirection.BACK -> R.string.direction_back
        RelativeDirection.BACK_LEFT -> R.string.direction_back_left
        RelativeDirection.LEFT -> R.string.direction_left
        RelativeDirection.FRONT_LEFT -> R.string.direction_front_left
    }

/** 위(차가 향한 쪽)를 가리키는 화살표를 이 방향으로 돌릴 각도. 시계 방향이다 */
private val RelativeDirection.arrowDegrees: Float
    get() = when (this) {
        RelativeDirection.FRONT -> 0f
        RelativeDirection.FRONT_RIGHT -> 45f
        RelativeDirection.RIGHT -> 90f
        RelativeDirection.BACK_RIGHT -> 135f
        RelativeDirection.BACK -> 180f
        RelativeDirection.BACK_LEFT -> 225f
        RelativeDirection.LEFT -> 270f
        RelativeDirection.FRONT_LEFT -> 315f
    }
