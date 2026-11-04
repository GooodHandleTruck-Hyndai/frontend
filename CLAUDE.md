# 저 아파트 얼마야? — Pleos Connect 차량용 앱

지나가는 아파트 단지의 최근 실거래가를 주행 중에는 음성으로, 정차 중에는 화면으로 알려 주는 Pleos Connect(현대차그룹 차량 OS) 앱이다.
공모전 일정: 1차 개발 완료 11/12, 숏폼 시연 영상 11/14.

## 참고 문서 (필요할 때 열어 읽는다)

- `docs/기획.md`: 기획 정리. 타깃, 기능과 우선순위(P0~P2), 주 사용 시나리오(정차·주행)
- 화면 설계서(Claude Docs): https://claude.ai/artifact/UgMDDBgkDfK7rZVFKAaoQt. 화면 9개, 공통 규격(색·글자·간격), 주행·정차 전환 규칙, 개발 뼈대
- `docs/api-spec.md`: API 명세(아직 저장소에 없다). 엔드포인트, 요청·응답 JSON. 데이터 클래스와 목업은 이 형식을 그대로 따른다

화면 ID: S-01 시작하기 · S-02 알림 조건 · S-03 주변 단지 · S-04 단지 상세 · S-05 저장한 단지 · S-06 오늘 들은 단지 · S-07 설정 · S-08 주행 중 대기 · S-09 주행 중 단지 알림. 코드에서는 `ui/navigation/Screen.kt`의 `Screen.*`

## 기술 스택

- Kotlin, Jetpack Compose, minSdk 28, compileSdk·targetSdk 34 (에뮬레이터 이미지 Pleos Connect v2.1 = Android 14)
- MVVM: ViewModel + StateFlow. DI 프레임워크 없이 수동 주입
- 데이터: `ComplexRepository` 인터페이스 → `FakeRepository`(assets JSON) 먼저, 나중에 `RemoteRepository`(Retrofit + kotlinx.serialization)
- Google Play 서비스에 기대는 라이브러리(구글 지도, FCM, Google Fused Location)는 쓰지 않는다. Pleos 문서에 지원 안내가 없다
- `AndroidManifest.xml`에 `<uses-feature android:name="android.hardware.type.automotive" android:required="true"/>`

## Pleos SDK

- Maven 저장소: `https://nexus-playground.pleos.ai/repository/maven-releases/` (인증 없음)
- `ai.pleos.playground:NaviHelper:3.0.5`
  - `requestRoute(RouteInfo(latitude, longitude, ..., poiName, ..., address))`로 길 안내 요청
  - `onDestinationArrived(DestinationArrivedInfo)`: 이름·주소·거리(m)·시간(초). 좌표는 없다
  - `onCurrentLocationInfo(CurrentLocationInfo)`: latitude, longitude, heading, address
  - 이벤트는 기본 내비 앱 구현에 따라 안 올 수 있다. 안 오면 FusedLocation 근접 알림 + 정차로 대신한다
- `ai.pleos.playground:FusedLocation:1.1.3`
  - `isSupported()` 먼저 확인 → `initialize()` → `registerFusedLocationCallback(FusedLocationListener)`
  - `LocationData`: latitude, longitude, altitude, heading, confidence, timestampMillis
  - `addProximityAlert(ProximityAlertRequest(lat, lon, radiusMeters, setOf(ProximityTransition.ENTER)), ProximityAlertListener)`
- `ai.pleos.playground:TextToSpeech:3.3.1`
  - `TextToSpeech(context, Mode.ON_DEVICE)` → `initialize()` → `setLocale(Locale.KOREA)` → `speak(text)`
  - ON_DEVICE 모드는 앱 등록(clientId/secret)이 필요 없다
- 권한: `pleos.car.permission.FUSED_LOCATION`(런타임 요청), `pleos.car.permission.NAVI_ROUTE`, `pleos.car.permission.NAVI_ROUTE_SEARCH`, `pleos.car.permission.NAVI_CUSTOM_MAP`, `pleos.car.permission.TTS_SERVICE`, `android.permission.INTERNET`
- 주행 상태: AAOS `CarUxRestrictionsManager`(`android { useLibrary("android.car") }`). `isRequiresDistractionOptimization()`이 true면 주행 중
- 기어: AAOS `CarPropertyManager`의 `GEAR_SELECTION`(권한 `android.car.permission.CAR_POWERTRAIN`)
- 공식 샘플: github.com/Pleos-Playground/pleos-sample-fusedlocation-app, github.com/Pleos-Playground/pleos-sample-navi-app
- Pleos 문서에 없는 클래스나 메서드는 지어내지 않는다. 확실하지 않으면 TODO 주석을 남기고 나에게 묻는다

## 주행 중 규칙 (법과 Pleos 심사 기준)

- 주행 중 화면: 단지 이름 한 줄과 큰 버튼 두 개(다시 듣기, 저장)만. 가격·목록·지도는 정차 중에만 보여 준다
- 가격은 주행 중에는 음성으로만 말한다. 방향·거리("오른쪽 · 340m")는 heading으로 앱에서 계산한다
- 주행 중에 보이는 Activity에만 `<meta-data android:name="distractionOptimized" android:value="true"/>`
- 영상, 반복 애니메이션, 긴 스크롤, 전체 화면 팝업은 쓰지 않는다. 앱 창 안에서 화면을 바꾼다
- 음성은 오디오 포커스를 `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`으로 받은 뒤 낸다
- 주행 중 서버 호출은 `geofences`, `brief`, 관심 단지 저장, `HEARD` 기록만 한다

## 화면 규격 (Connect-S 17", 단위 dp)

- 앱 창: 세로 840×1184(내비와 1:1:1), 가로 1700×1184(1:2). 두 크기 모두에서 레이아웃이 깨지지 않아야 한다
- 글꼴: Asta Sans(`res/font`). 크기/줄높이 sp: 제목 40/52·36/44·32/40, 본문 30/38·28/36·26/34, 라벨 24/28·20/24. 최소 12sp
- 색: 배경 `#F7F8FA`, 카드 `#FFFFFF`(테두리 `#E0E1E6`, 모서리 28), 글자 `#131417`(보조 84%·64% 투명도), 보조 버튼 `#EDEEF2`, 우리 앱 강조 `#157A52`, 공시가격 `#B45309`
- 문구: 해요체, 처음 쓰는 사람 기준. "평소 타던" 같은 비교 표현은 쓰지 않는다

## 데이터 규칙

- 금액은 `priceManwon`(만원 단위 정수). "12억 3,000만" 같은 표시 문자열은 앱 유틸 함수 하나에서만 만든다
- 좌표는 `lat`, `lon`. 시각은 +09:00 ISO 8601
- 서버 전에는 `app/src/main/assets/mock/*.json`(api-spec 응답 예시 그대로)을 `FakeRepository`가 읽는다
- 단지 이름과 가격은 가상 데이터다

## 빌드·실행·시연

- 빌드: `./gradlew assembleDebug` → 설치: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- 위치 이동: `adb emu geo fix <경도> <위도>` (경도가 먼저)
- 주행/정차: `adb shell cmd hmg_car_service --service drivingstate driving_regulation on` (정차는 `off`)
- 기어 P·D: Virtual Console. `adb forward tcp:8088 tcp:8088` 뒤 브라우저에서 `http://127.0.0.1:8088`
- 화면 녹화: `adb exec-out screenrecord --output-format=h264 - > screen.h264`

## 작업 방식

- 한 번에 한 화면이나 한 기능만 만든다. 끝나면 `./gradlew assembleDebug`가 통과하는지 확인한다
- 새 라이브러리를 넣기 전에 먼저 묻는다
- 끝나면 에뮬레이터에서 내가 확인할 항목을 3줄 이내로 알려 준다
- 설명과 주석은 한국어로 쓴다
