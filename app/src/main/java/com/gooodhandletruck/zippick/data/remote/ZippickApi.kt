package com.gooodhandletruck.zippick.data.remote

import com.gooodhandletruck.zippick.data.remote.dto.AlertListResponse
import com.gooodhandletruck.zippick.data.remote.dto.AlertReadResponse
import com.gooodhandletruck.zippick.data.remote.dto.BuildingDetailResponse
import com.gooodhandletruck.zippick.data.remote.dto.DeviceRegisterRequest
import com.gooodhandletruck.zippick.data.remote.dto.DeviceResponse
import com.gooodhandletruck.zippick.data.remote.dto.FavoriteCreateRequest
import com.gooodhandletruck.zippick.data.remote.dto.FavoriteResponse
import com.gooodhandletruck.zippick.data.remote.dto.InterestConditionRequest
import com.gooodhandletruck.zippick.data.remote.dto.InterestConditionResponse
import com.gooodhandletruck.zippick.data.remote.dto.NearbyBuildingsResponse
import com.gooodhandletruck.zippick.data.remote.dto.PriceTrendResponse
import com.gooodhandletruck.zippick.data.remote.dto.TransactionListResponse
import java.time.OffsetDateTime
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 집픽 백엔드 API 목록(axios의 API 함수 모음과 비슷하다). 경로·쿼리·응답 모양은 백엔드 저장소 main의 컨트롤러와 DTO를 따른다.
 * Swagger: 서버 주소 뒤에 /swagger-ui/index.html.
 *
 * 직접 부르지 말고 [ApiClient.call]·[ApiClient.callAsDevice]로 감싸서 부른다. 실패 응답을 [ApiException]으로 바꾸고 기기 등록을 맡는다.
 * 헤더는 [ApiClient]가 붙인다. 모든 요청에 ngrok 헤더를, 기기를 등록한 뒤에는 X-Anonymous-Id도 붙인다.
 *
 * 앱이 쓰지 않아 넣지 않은 API:
 * - GET /health: 서버가 켜졌는지 사람이 볼 때 쓴다(`{"status": "ok"}`, 공통 응답으로 감싸지 않는다)
 * - GET /api/v1/regions: 위치의 시군구 코드. 서버가 국토부 자료를 찾을 때 쓰는 값이다
 * - POST /api/v1/buildings/coordinates: 국토부 자료 칸으로 좌표 찾기. 주변 건물과 건물 상세가 좌표를 이미 준다
 */
interface ZippickApi {

    /** 기기 등록(201). 헤더 없이 보내고, 받은 익명 ID를 기억해 둔다. [ApiClient]만 부른다 */
    @POST("api/v1/devices")
    suspend fun registerDevice(@Body request: DeviceRegisterRequest): ApiResponse<DeviceResponse>

    /** 내 기기 확인. 헤더가 제대로 붙는지 볼 때 쓴다. 헤더 필요 */
    @GET("api/v1/devices/me")
    suspend fun myDevice(): ApiResponse<DeviceResponse>

    /**
     * 주변 건물, 가까운 순(S-03). 헤더는 필요 없다. 반경 3000m에서 응답에 3초쯤 걸리니 불러오는 중 화면을 보여 준다.
     * 앱의 limit(카드 10개)은 서버에 없어서 앱이 자른다.
     *
     * @param radius 반경(m). 100~3000, null이면 1000. 범위 밖이면 C001
     */
    @GET("api/v1/buildings/nearby")
    suspend fun nearbyBuildings(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("radius") radius: Int? = null
    ): ApiResponse<NearbyBuildingsResponse>

    /** 건물 상세와 최근 거래 3건(S-04). 헤더는 없어도 되지만, 보내야 saved가 맞게 온다. 없는 건물은 B001 */
    @GET("api/v1/buildings/{buildingId}")
    suspend fun buildingDetail(@Path("buildingId") buildingId: Long): ApiResponse<BuildingDetailResponse>

    /** 실거래 내역(S-04 더보기). [page]는 0부터, [size]는 1~50. hasNext가 true일 때만 page + 1을 부른다 */
    @GET("api/v1/buildings/{buildingId}/transactions")
    suspend fun transactions(
        @Path("buildingId") buildingId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = TRANSACTIONS_PAGE_SIZE
    ): ApiResponse<TransactionListResponse>

    /** 가격 추이. 이번 달 포함 최근 12개월이 오래된 달부터 늘 12개 온다. 없는 건물은 B001 */
    @GET("api/v1/buildings/{buildingId}/price-trend")
    suspend fun priceTrend(@Path("buildingId") buildingId: Long): ApiResponse<PriceTrendResponse>

    /** 저장하기(S-04·S-09, 201). 없는 건물은 B001, 이미 저장했으면 F001(409). 헤더 필요 */
    @POST("api/v1/favorites")
    suspend fun saveFavorite(@Body request: FavoriteCreateRequest): ApiResponse<FavoriteResponse>

    /** 저장 목록(S-05). 최근에 저장한 것부터 오고, 없으면 빈 목록이다. 헤더 필요 */
    @GET("api/v1/favorites")
    suspend fun favorites(): ApiResponse<List<FavoriteResponse>>

    /** 저장 취소(S-04). 성공해도 data가 없다. 저장하지 않은 건물이면 F002(404). 헤더 필요 */
    @DELETE("api/v1/favorites/{buildingId}")
    suspend fun deleteFavorite(@Path("buildingId") buildingId: Long): ApiResponse<Unit>

    /** 관심 조건 조회. 조건을 정한 적이 없으면 data가 없고, 그때는 서버가 알림을 만들지 않는다. 헤더 필요 */
    @GET("api/v1/interest-conditions")
    suspend fun interestCondition(): ApiResponse<InterestConditionResponse>

    /** 관심 조건 저장. 전체 값을 바꾸고 저장한 조건을 돌려준다. 최소가 최대보다 크거나 형식이 틀리면 C001. 헤더 필요 */
    @PUT("api/v1/interest-conditions")
    suspend fun saveInterestCondition(@Body request: InterestConditionRequest): ApiResponse<InterestConditionResponse>

    /**
     * 관심 건물 알림 목록, 최신순 최대 100개. 헤더 필요.
     *
     * @param since 이 시각 이후 알림만. null이면 최근 7일. 주소에 넣을 때 +는 %2B로 바뀐다
     */
    @GET("api/v1/alerts")
    suspend fun alerts(@Query("since") since: OffsetDateTime? = null): ApiResponse<AlertListResponse>

    /** 알림 읽음. 이미 읽은 알림이면 처음 읽은 시각을 그대로 준다. 내 알림이 아니거나 없으면 N001(404). 헤더 필요 */
    @PATCH("api/v1/alerts/{alertId}/read")
    suspend fun markAlertRead(@Path("alertId") alertId: Long): ApiResponse<AlertReadResponse>

    companion object {
        /** 실거래 내역 한 쪽의 기본 크기(서버 기본값과 같다) */
        const val TRANSACTIONS_PAGE_SIZE = 20
    }
}
