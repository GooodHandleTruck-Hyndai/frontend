package com.gooodhandletruck.zippick.data.remote.dto

import com.gooodhandletruck.zippick.data.model.OffsetDateTimeSerializer
import java.time.OffsetDateTime
import kotlinx.serialization.Serializable

/*
 * 기기 API의 요청·응답 data. 이름과 모양은 백엔드 domain/device/dto와 같다.
 * 로그인 대신 서버가 발급한 익명 ID로 기기를 구분한다.
 */

/** 기기 등록 요청(POST /api/v1/devices). [appVersion]은 20자까지이고 없어도 된다 */
@Serializable
data class DeviceRegisterRequest(val appVersion: String? = null)

/**
 * 기기 정보. 기기 등록과 내 기기 확인의 data다.
 *
 * @property anonymousId 서버가 만든 익명 ID(UUID). 앱에 저장해 두고 X-Anonymous-Id 헤더로 보낸다
 * @property createdAt 등록 시각(+09:00)
 */
@Serializable
data class DeviceResponse(
    val anonymousId: String,
    @Serializable(with = OffsetDateTimeSerializer::class)
    val createdAt: OffsetDateTime? = null
)
