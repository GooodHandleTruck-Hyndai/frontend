package com.gooodhandletruck.zippick.data

import com.gooodhandletruck.zippick.data.remote.AlertListDto
import com.gooodhandletruck.zippick.data.remote.NewTradeAlert
import com.gooodhandletruck.zippick.data.remote.ServerClient
import com.gooodhandletruck.zippick.data.remote.decode
import com.gooodhandletruck.zippick.data.remote.toAlert

/**
 * 관심 건물 새 거래 알림(S-10). 서버 배치가 관심 조건·저장한 건물로 알림을 만들고, 앱은 열 때 가져간다.
 * 정차 중에만 부른다(주행 중 서버 호출 규칙).
 */
class AlertInboxRepository(private val server: ServerClient) {

    /** 최근 7일 알림, 최신순 (B 18번) */
    suspend fun alerts(): List<NewTradeAlert> = server.get(PATH).decode<AlertListDto>().alerts.map { it.toAlert() }

    /** 읽음 (B 19번). 이미 읽은 알림에 다시 보내도 성공이다 */
    suspend fun markRead(alertId: Long) {
        server.patch("$PATH/$alertId/read")
    }

    private companion object {
        const val PATH = "api/v1/alerts"
    }
}
