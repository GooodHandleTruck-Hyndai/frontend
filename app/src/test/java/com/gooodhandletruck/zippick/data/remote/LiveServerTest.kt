package com.gooodhandletruck.zippick.data.remote

import com.gooodhandletruck.zippick.BuildConfig
import com.gooodhandletruck.zippick.data.remote.dto.FavoriteCreateRequest
import com.gooodhandletruck.zippick.data.remote.dto.InterestConditionRequest
import com.gooodhandletruck.zippick.data.remote.dto.InterestConditionResponse
import java.io.File
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * 실제 서버에 붙여 보는 테스트. 평소 `./gradlew test`에서는 건너뛴다. 서버가 켜져 있을 때 이렇게 돌린다:
 * `ZIPPICK_LIVE_API=1 ./gradlew :app:testDebugUnitTest --tests '*LiveServerTest*' --rerun`
 * 값이 1이면 BuildConfig.API_BASE_URL(개발 서버)이고, 주소를 넣으면 그 서버다(내 PC 백엔드는 http://localhost:8080/).
 * 테스트마다 보낸 요청, 받은 응답, 결과가 app/build/reports/live-server/의 로그 파일에 순서대로 남는다([LiveLog]).
 * 서버에 테스트 기기를 두 개 등록하고, 건물 21을 저장했다가 지우고, 알림을 끈 관심 조건을 저장한다.
 */
class LiveServerTest {

    @get:Rule
    val logEachTest: TestWatcher = LiveLog.Watcher()

    @Before
    fun requireLiveServer() = assumeTrue("ZIPPICK_LIVE_API가 없어서 건너뛰어요", BASE_URL != null)

    @Test
    @Title("기기 등록, X-Anonymous-Id 헤더로 내 기기 확인")
    fun registersDeviceAndSendsHeader() = runBlocking<Unit> {
        val id = client.ensureDevice()
        val me = client.callAsDevice { myDevice() }.requireData()

        LiveLog.note("기기 ID $id, 등록 시각 ${me.createdAt}")
        assertEquals(id, me.anonymousId)
    }

    @Test
    @Title("주변 건물: 도화동 반경 1km, 가까운 순")
    fun nearbyAroundDohwaDong() = runBlocking<Unit> {
        val (nearby, ms) = timed { client.call { nearbyBuildings(DOHWA_LAT, DOHWA_LON, radius = 1000) }.requireData() }

        LiveLog.note("주변 1km: ${nearby.buildings.size}곳, ${ms}ms, 자료 기준일 ${nearby.dataBaseDate}")
        nearby.buildings.take(PRINT_COUNT).forEach { LiveLog.note("  $it") }
        assertTrue(nearby.buildings.isNotEmpty())
        assertEquals(nearby.buildings.sortedBy { it.distanceM }, nearby.buildings) // 가까운 순
        assertTrue(nearby.buildings.all { it.distanceM <= 1000 })
    }

    @Test
    @Title("주변 건물: 가장 넓은 반경 3km, 걸리는 시간")
    fun nearbyWidestRadius() = runBlocking<Unit> {
        val (nearby, ms) = timed { client.call { nearbyBuildings(DOHWA_LAT, DOHWA_LON, radius = 3000) }.requireData() }

        // 서버 안내: 반경 3000m에서 3초쯤 걸린다. 앱의 읽기 시간 제한은 20초다
        val withoutDeal = nearby.buildings.count { it.latestDeal == null }
        LiveLog.note("주변 3km: ${nearby.buildings.size}곳, ${ms}ms, 최근 거래 없는 곳 ${withoutDeal}곳")
        assertTrue(nearby.buildings.all { it.distanceM <= 3000 })
    }

    @Test
    @Title("건물 21: 상세, 실거래 내역 첫 쪽, 가격 추이")
    fun buildingDetailTransactionsAndTrend() = runBlocking<Unit> {
        val detail = client.call { buildingDetail(21) }.requireData()
        val page = client.call { transactions(21, page = 0, size = 5) }.requireData()
        val trend = client.call { priceTrend(21) }.requireData()

        LiveLog.note("상세: $detail")
        LiveLog.note("내역 0쪽: ${page.transactions.size}건, hasNext ${page.hasNext}")
        LiveLog.note("추이: ${trend.months.joinToString { "${it.yearMonth}=${it.avgPrice}(${it.count})" }}")
        assertEquals(21L, detail.buildingId)
        assertTrue(detail.recentTransactions.size <= 3)
        // 상세의 최근 거래와 내역 첫 쪽은 같은 최신순이다
        assertEquals(detail.recentTransactions, page.transactions.take(detail.recentTransactions.size))
        assertEquals(12, trend.months.size)
    }

    @Test
    @Title("없는 건물 999999는 B001(404)")
    fun unknownBuildingIsB001() = runBlocking<Unit> {
        val error = expectThrows<ApiException> { client.call { buildingDetail(999999) } }

        assertEquals(404, error.httpStatus)
        assertEquals(ApiErrorCode.BUILDING_NOT_FOUND, error.code)
    }

    @Test
    @Title("저장 → 목록 → 다시 저장(F001) → 취소 → 다시 취소(F002)")
    fun saveListAndDeleteFavorite() = runBlocking<Unit> {
        val saved = client.callAsDevice { saveFavorite(FavoriteCreateRequest(21)) }.requireData()
        val list = client.callAsDevice { favorites() }.requireData()
        val again = expectThrows<ApiException> { client.callAsDevice { saveFavorite(FavoriteCreateRequest(21)) } }
        val savedInDetail = client.call { buildingDetail(21) }.requireData().saved
        client.callAsDevice { deleteFavorite(21) }
        val deleteAgain = expectThrows<ApiException> { client.callAsDevice { deleteFavorite(21) } }

        LiveLog.note("저장: $saved")
        LiveLog.note("저장한 뒤 건물 상세의 saved = $savedInDetail")
        assertEquals(listOf(21L), list.map { it.buildingId })
        assertEquals(ApiErrorCode.FAVORITE_ALREADY_EXISTS, again.code)
        assertEquals(ApiErrorCode.FAVORITE_NOT_FOUND, deleteAgain.code)
        assertTrue(client.callAsDevice { favorites() }.requireData().isEmpty())
    }

    @Test
    @Title("관심 조건: 처음엔 없음 → 저장 → 조회, 최소 가격 > 최대 가격은 C001")
    fun interestConditionRoundTrip() = runBlocking<Unit> {
        // 알림을 끈 채로 저장해서 서버 배치가 테스트 기기에 알림을 만들지 않게 한다
        val request = InterestConditionRequest(
            regionCodes = listOf(MAPO_GU),
            priceMin = null,
            priceMax = 150_000,
            types = listOf("APT"),
            areaMinM2 = 59,
            areaMaxM2 = 85,
            alertEnabled = false
        )

        val before = client.callAsDevice { interestCondition() }.data
        val saved = client.callAsDevice { saveInterestCondition(request) }.requireData()
        val after = client.callAsDevice { interestCondition() }.requireData()
        val invalid = expectThrows<ApiException> {
            client.callAsDevice { saveInterestCondition(request.copy(priceMin = 200_000)) }
        }

        LiveLog.note("저장 전: $before")
        LiveLog.note("저장 뒤: $after")
        assertNull(before) // 새 기기라 조건이 없다
        assertEquals(saved, after)
        assertEquals(InterestConditionResponse(listOf(MAPO_GU), null, 150_000, listOf("APT"), 59, 85, false), after)
        assertEquals(ApiErrorCode.INVALID_INPUT, invalid.code)
    }

    @Test
    @Title("관심 건물 알림: 목록(최근 7일·since), 없는 알림 읽음은 N001")
    fun alertsListAndUnknownRead() = runBlocking<Unit> {
        val recent = client.callAsDevice { alerts() }.requireData().alerts
        val sinceYesterday = client.callAsDevice { alerts(OffsetDateTime.now(SEOUL).minusDays(1)) }.requireData()
        val unknown = expectThrows<ApiException> { client.callAsDevice { markAlertRead(UNKNOWN_ALERT_ID) } }

        LiveLog.note("알림: 최근 7일 ${recent.size}개, 어제부터 ${sinceYesterday.alerts.size}개")
        assertTrue(recent.isEmpty()) // 새 기기라 알림이 없다
        assertEquals(ApiErrorCode.ALERT_NOT_FOUND, unknown.code)
    }

    @Test
    @Title("서버가 모르는 기기 ID(D002)면 다시 등록하고 한 번 더 보내기")
    fun forgottenDeviceRegistersAgain() = runBlocking<Unit> {
        val unknownId = UUID.randomUUID().toString() // 서버가 모르는 ID
        val deviceIds = MemoryDeviceIds(anonymousId = unknownId)

        val favorites = liveClient(deviceIds).callAsDevice { favorites() }.requireData()

        LiveLog.note("모르는 ID $unknownId → 새 ID ${deviceIds.anonymousId}")
        assertTrue(favorites.isEmpty())
        assertNotEquals(unknownId, deviceIds.anonymousId)
    }

    private inline fun <T> timed(block: () -> T): Pair<T, Long> {
        val started = System.nanoTime()
        val result = block()
        return result to (System.nanoTime() - started) / NANOS_PER_MILLI
    }

    companion object {
        /** 로그 끝에 통과·실패 수를 적는다 */
        @JvmStatic
        @AfterClass
        fun writeSummary() = LiveLog.summary()

        // 서버 안내의 테스트 위치: 마포구 도화동
        private const val DOHWA_LAT = 37.5404
        private const val DOHWA_LON = 126.9455
        private const val MAPO_GU = "11440"
        private const val UNKNOWN_ALERT_ID = 999_999_999L
        private val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
        private const val PRINT_COUNT = 5
        private const val NANOS_PER_MILLI = 1_000_000

        /** 테스트들이 기기 하나를 나눠 쓴다(서버에 등록을 덜 남긴다) */
        private val client by lazy { liveClient(MemoryDeviceIds()) }

        /** 앱과 같은 창구에 로그 파일로 쓰는 interceptor를 더한다 */
        private fun liveClient(deviceIds: DeviceIdStorage): ApiClient {
            val logToFile = HttpLoggingInterceptor(LiveLog::http).apply { level = HttpLoggingInterceptor.Level.BODY }
            val okHttp = createOkHttpClient(deviceIds).newBuilder().addInterceptor(logToFile).build()
            return ApiClient(createZippickApi(BASE_URL!!, okHttp), deviceIds)
        }
    }
}

/** 붙일 서버 주소. null이면 라이브 테스트를 건너뛴다 */
private val BASE_URL: String? = System.getenv("ZIPPICK_LIVE_API")?.let { value ->
    if (value == "1") BuildConfig.API_BASE_URL else "${value.trim().trimEnd('/')}/"
}

/** 로그에 쓰는 테스트 제목 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
private annotation class Title(val value: String)

/**
 * 라이브 테스트 로그. 테스트마다 보낸 요청(주소·헤더·본문), 받은 응답(상태·걸린 시간·본문), 결과를 순서대로 파일 하나에 쓴다.
 * 파일은 app/build/reports/live-server/live-server-날짜-시각.log다. JSON 본문은 줄을 나눠 쓰고, 긴 본문은 앞부분만 남긴다.
 * 라이브 테스트를 건너뛸 때(ZIPPICK_LIVE_API가 없을 때)는 파일을 만들지 않는다.
 */
private object LiveLog {
    private const val MAX_BODY_LINES = 80
    private val startedAt = LocalDateTime.now()
    private val prettyJson = Json { prettyPrint = true }

    // 단위 테스트는 app 모듈 폴더에서 돈다
    private val file = File("build/reports/live-server/live-server-${startedAt.format(FILE_TIME)}.log")
    private val passed = mutableListOf<String>()
    private val failed = mutableListOf<String>()
    private var opened = false

    @Synchronized
    fun write(text: String) {
        if (BASE_URL == null) return
        if (!opened) {
            file.parentFile?.mkdirs()
            file.writeText("집픽 라이브 서버 테스트\n서버: $BASE_URL\n시작: ${startedAt.format(LOG_TIME)}\n")
            opened = true
            println("라이브 테스트 로그: ${file.absolutePath}")
        }
        file.appendText("$text\n")
    }

    /** 테스트 안에서 남기는 메모 */
    fun note(text: String) = write("메모: $text")

    /** OkHttp가 넘기는 로그 한 줄. JSON 본문은 줄을 나눠 쓰고, HTML 본문(ngrok 안내·오류 페이지)은 크기만 쓴다 */
    fun http(message: String) = write(
        when {
            message.startsWith("{") || message.startsWith("[") -> pretty(message)
            message.startsWith("<!DOCTYPE", ignoreCase = true) -> "(HTML 본문 ${message.toByteArray().size}바이트는 생략)"
            else -> message
        }
    )

    @Synchronized
    fun record(name: String, error: Throwable?) {
        if (error == null) passed += name else failed += "$name: $error"
    }

    @Synchronized
    fun summary() {
        if (!opened) return
        write("\n==================== 요약 ====================")
        write("통과 ${passed.size}개, 실패 ${failed.size}개")
        failed.forEach { write("실패 $it") }
    }

    private fun pretty(body: String): String {
        val text = try {
            prettyJson.encodeToString(JsonElement.serializer(), prettyJson.parseToJsonElement(body))
        } catch (e: IllegalArgumentException) { // JSON이 아니면 그대로 쓴다(SerializationException 포함)
            return body
        }
        val lines = text.lines()
        if (lines.size <= MAX_BODY_LINES) return text
        val cut = "… (${lines.size}줄 중 앞 ${MAX_BODY_LINES}줄만 남겼어요. 본문 ${body.toByteArray().size}바이트)"
        return (lines.take(MAX_BODY_LINES) + cut).joinToString("\n")
    }

    /** 테스트마다 제목, 결과, 걸린 시간을 쓴다 */
    class Watcher : TestWatcher() {
        private var startedNanos = 0L

        override fun starting(description: Description) {
            startedNanos = System.nanoTime()
            val title = description.getAnnotation(Title::class.java)?.value?.let { " · $it" }.orEmpty()
            write("\n==================== ${description.methodName}$title ====================")
        }

        override fun succeeded(description: Description) = finish(description, error = null)

        override fun failed(e: Throwable, description: Description) = finish(description, e)

        private fun finish(description: Description, error: Throwable?) {
            if (BASE_URL == null) return
            val seconds = "%.1f".format((System.nanoTime() - startedNanos) / NANOS_PER_SECOND)
            record(description.methodName, error)
            write(if (error == null) "결과: 통과 (${seconds}초)" else "결과: 실패 (${seconds}초) $error")
        }
    }
}

private val FILE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
private val LOG_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private const val NANOS_PER_SECOND = 1e9
