package com.gooodhandletruck.zippick.ui.format

/** 차가 향한 쪽을 기준으로 본 단지 방향. 45도씩 8방향이다 (화면 설계서 '방향·거리 칩'). */
enum class RelativeDirection {
    FRONT,
    FRONT_RIGHT,
    RIGHT,
    BACK_RIGHT,
    BACK,
    BACK_LEFT,
    LEFT,
    FRONT_LEFT;

    companion object {
        private const val FULL_TURN = 360.0
        private const val SECTOR = 45.0

        /** [bearing]은 단지가 있는 방위각, [heading]은 차가 향한 방위각이다. 둘 다 북쪽이 0도이고 시계 방향이다. */
        fun of(bearing: Double, heading: Double): RelativeDirection {
            val relative = ((bearing - heading) % FULL_TURN + FULL_TURN) % FULL_TURN
            val index = ((relative + SECTOR / 2) / SECTOR).toInt() % entries.size
            return entries[index]
        }
    }
}

/** 차에서 본 단지의 방향과 거리. 방향·거리 칩("오른쪽 · 340m")에 쓴다 */
data class RelativePlace(val direction: RelativeDirection, val distanceMeters: Double)
