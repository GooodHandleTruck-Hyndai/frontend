package com.gooodhandletruck.sidong.ui.format

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val CLOCK = DateTimeFormatter.ofPattern("HH:mm")

/** 들은 시각 표기(S-06): 서버가 준 +09:00 시각의 "14:32" */
fun formatClock(time: OffsetDateTime): String = CLOCK.format(time)
