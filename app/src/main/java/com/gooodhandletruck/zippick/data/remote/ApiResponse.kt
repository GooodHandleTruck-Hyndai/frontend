package com.gooodhandletruck.zippick.data.remote

import com.gooodhandletruck.zippick.data.AppJson
import java.io.IOException
import kotlinx.serialization.Serializable
import retrofit2.HttpException

/**
 * 서버의 공통 응답 형식. 성공하면 `{ "success": true, "code": "OK", "message": "요청 성공", "data": { ... } }`가 온다.
 * 저장 취소처럼 성공해도 data가 없는 API가 있어서 [data]는 비어 있을 수 있다. data가 꼭 있어야 하면 [requireData]로 꺼낸다.
 * 실패는 HTTP 4xx·5xx와 `{ "success": false, "code": "B001", "message": "..." }`로 오고,
 * [ApiClient]가 [ApiException]으로 바꿔 던진다. 그래서 [ApiClient]를 거쳐 받은 응답은 늘 성공이다.
 */
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val code: String,
    val message: String,
    val data: T? = null
)

/** 성공 응답의 data. 와야 할 data가 없으면 서버가 약속한 형식을 어긴 것이라 IllegalStateException을 던진다 */
fun <T : Any> ApiResponse<T>.requireData(): T = checkNotNull(data) { "서버 응답($code)에 data가 없어요" }

/**
 * 서버가 실패로 답했다(HTTP 4xx·5xx). [code]를 [ApiErrorCode]와 비교해 나눈다.
 * 우리 서버 형식이 아닌 응답이면 [code]가 null이다. 예: 개발 서버가 꺼져 있으면 ngrok이 404와 안내 페이지를 준다.
 * 통신 자체가 안 되면(인터넷 끊김, 시간 초과) 이 예외 대신 IOException이 난다.
 *
 * @property httpStatus HTTP 상태 코드
 */
class ApiException(
    val httpStatus: Int,
    val code: String?,
    message: String
) : Exception(message)

/** 서버 에러 코드. 백엔드 global/error/ErrorCode.java와 같다 */
object ApiErrorCode {
    /** 400. 입력값 오류(숫자가 아닌 ID, page·size·radius 범위 밖, 필수 값 빠짐) */
    const val INVALID_INPUT = "C001"

    /** 404. 없는 경로 */
    const val NOT_FOUND = "C002"

    /** 405. 지원하지 않는 HTTP 메서드 */
    const val METHOD_NOT_ALLOWED = "C003"

    /** 500. 서버 내부 오류 */
    const val INTERNAL_ERROR = "C999"

    /** 401. X-Anonymous-Id 헤더가 없다 */
    const val MISSING_ANONYMOUS_ID = "D001"

    /** 404. 서버가 모르는 기기 ID다. [ApiClient.callAsDevice]가 다시 등록하고 한 번 더 보낸다 */
    const val DEVICE_NOT_FOUND = "D002"

    /** 404. 그 위치의 지역 정보가 없다(바다 등) */
    const val REGION_NOT_FOUND = "R001"

    /** 404. 없는 건물 */
    const val BUILDING_NOT_FOUND = "B001"

    /** 502. 외부 서비스(카카오·국토부·행안부) 호출 실패 */
    const val EXTERNAL_API_ERROR = "E001"

    /** 504. 외부 서비스 응답이 늦다 */
    const val EXTERNAL_API_TIMEOUT = "E002"

    /** 409. 이미 저장한 건물 */
    const val FAVORITE_ALREADY_EXISTS = "F001"

    /** 404. 저장하지 않은 건물 */
    const val FAVORITE_NOT_FOUND = "F002"

    /** 404. 내 알림이 아니거나 없는 알림 */
    const val ALERT_NOT_FOUND = "N001"
}

/** 실패 응답 본문을 읽어 [ApiException]으로 바꾼다. 우리 형식이 아니면 code 없이, ngrok 오류 코드가 있으면 메시지에 남긴다 */
internal fun HttpException.toApiException(): ApiException {
    val status = code()
    val body = try {
        response()?.errorBody()?.string()
    } catch (e: IOException) {
        null
    }
    val failure = body?.let {
        try {
            AppJson.decodeFromString<ApiFailure>(it)
        } catch (e: IllegalArgumentException) { // JSON이 아니거나 code가 없을 때(SerializationException 포함)
            null
        }
    }
    if (failure != null) return ApiException(status, failure.code, failure.message)

    // ERR_NGROK_3200이면 개발 서버가 꺼져 있다
    val ngrokError = response()?.headers()?.get(HEADER_NGROK_ERROR_CODE)
    val detail = if (ngrokError != null) "HTTP $status, $ngrokError" else "HTTP $status"
    return ApiException(status, null, "서버가 알 수 없는 응답을 줬어요 ($detail)")
}

/** 실패 응답 본문 `{ success: false, code, message }`. data는 오지 않는다 */
@Serializable
private data class ApiFailure(val code: String, val message: String)

private const val HEADER_NGROK_ERROR_CODE = "ngrok-error-code"
