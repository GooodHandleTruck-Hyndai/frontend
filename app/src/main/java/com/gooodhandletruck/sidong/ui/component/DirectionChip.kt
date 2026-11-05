package com.gooodhandletruck.sidong.ui.component

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.data.CarLocation
import com.gooodhandletruck.sidong.data.bearingDegrees
import com.gooodhandletruck.sidong.data.distanceMeters
import com.gooodhandletruck.sidong.ui.format.RelativeDirection
import com.gooodhandletruck.sidong.ui.format.RelativePlace
import com.gooodhandletruck.sidong.ui.format.formatDistance

/** 방향·거리 칩: "오른쪽 · 340m" (카드, S-04). 차가 향한 쪽 기준 8방향이고, 1km부터는 "1.2km"다. */
@Composable
fun DirectionChip(place: RelativePlace, modifier: Modifier = Modifier) {
    val directionName = stringResource(place.direction.label)
    TagChip(
        text = stringResource(R.string.direction_chip, directionName, formatDistance(place.distanceMeters)),
        modifier = modifier
    )
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
