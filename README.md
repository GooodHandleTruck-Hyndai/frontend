# 집픽 (Zippick)

> 지나가는 아파트 단지의 최근 실거래가를 주행 중에는 음성으로, 정차 중에는 화면으로 알려 주는 Pleos Connect 차량 앱

팀 GooodHandleTruck · 패키지 `com.gooodhandletruck.zippick` · 버전 0.1.0

## 📦 APK

| 항목 | 내용 |
|---|---|
| 파일 위치 | [`apk/GooodHandleTruck_Zippick_release.apk`](apk/GooodHandleTruck_Zippick_release.apk) |
| 빌드 종류 | release (바로 설치할 수 있게 개발용 키로 서명) |
| 연결 서버 | 공모전 심사용 공개 서버 `https://3-37-254-181.sslip.io` (1차 심사 결과 발표일 11/23까지 운영) |

## ✨ 주요 기능

- **주행 중 (S-08·S-09)**: 알림 조건(유형·가격대·면적)에 맞는 단지 반경 300m에 들어가면 "오른쪽, ○○단지. 전용 84제곱미터, 최근 3개월 평균 12억 3,000만 원이에요."처럼 소리로 알려 줘요. [주변 단지 듣기]로 차 앞쪽에서 가장 가까운 단지를 바로 들을 수도 있어요. 화면에는 단지 이름과 큰 버튼 두 개([다시 듣기], [저장])만 보여요.
- **정차 중**: 주변 단지 목록과 약도(S-03), 평형별 가격과 최근 거래(S-04), 저장한 단지(S-05), 오늘 들은 단지(S-06), 저장한 단지의 새 거래 알림(S-10)을 화면으로 보여 줘요.
- **길 안내**: 단지를 기본 내비의 목적지로 보내고, 도착하면 그 단지 상세를 열어요.
- 실거래 데이터는 백엔드가 국토교통부 실거래가 자료를 모아 제공해요.

## ▶️ 실행 방법

### A. APK로 실행하기

1. Pleos Connect 에뮬레이터를 켜요. 아래 '개발 환경'의 에뮬레이터 설치 방법을 보세요.
2. APK를 설치해요.
   ```bash
   adb install -r apk/GooodHandleTruck_Zippick_release.apk
   ```
3. 에뮬레이터 위치를 **서울 마포구 도화동**으로 옮겨요. 경도를 먼저 써요.
   ```bash
   adb emu geo fix 126.9455 37.5404
   ```
   Extended controls(⋯) → Location에서 위도 `37.5404`, 경도 `126.9455`를 넣어도 같아요.
4. 앱 목록에서 **집픽**을 열고 [위치 권한 허용하고 시작하기] → 알림 조건 고르기 → 주변 단지 순으로 진행해요.

### B. 소스로 빌드하기

```bash
git clone https://github.com/GooodHandleTruck-Hyndai/frontend.git
```

1. Android Studio → **Open** → 클론한 폴더를 열고 Gradle Sync가 끝날 때까지 기다려요.
2. release APK: `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`
3. debug 빌드(▶ Run)는 내 PC의 로컬 백엔드(`http://10.0.2.2:8080`)를 봐요. 공개 서버로 확인하려면 release로 빌드해요.
4. 단위 테스트: `./gradlew test` · 코드 스타일 검사: `./gradlew ktlintCheck`

## 🧪 테스트 환경 및 필요한 설정

| 항목 | 내용 |
|---|---|
| 서버 | release APK는 공개 서버에 바로 연결돼요. 따로 띄울 서버는 없어요 |
| 데이터 범위 | 지금은 **서울 마포구** 실거래 데이터만 있어요. 다른 위치에서는 "단지가 없어요"가 나와요 |
| 위치 권한 | 첫 화면에서 허용해요. 거부하면 주변 단지를 볼 수 없어요 |
| 차 방향 | 에뮬레이터는 차 방향(heading)을 늘 북쪽(0도)으로 줘요. 방향 칩은 차가 북쪽을 본다고 보고 나와요 |

**주행·정차 바꾸기**
- Extended controls(⋯) → **Car data**에서 기어를 **D**로 바꾸면 주행 화면(S-08)이 덮고, **P**로 돌리면 보던 화면으로 돌아와요.
- 명령으로 바꿀 수도 있어요. 정차는 `off`예요.
  ```bash
  adb shell cmd hmg_car_service --service drivingstate driving_regulation on
  ```

**주행 중 자동 알림 확인**
1. 정차 중에 알림 조건(S-02)을 정하고, 설정(S-07)에서 주행 중 음성 알림을 켜요.
2. 단지에서 떨어진 곳에서 주행을 시작해요.
3. `adb emu geo fix`로 단지 가까이 옮기면 소리로 알려 줘요. 같은 단지는 하루에 한 번만 알려 줘요.

**문제가 생기면**
- "지금 위치를 확인하지 못했어요"가 뜨거나 옛 위치가 나오면 Pleos 위치 서비스를 다시 띄우고, 앱도 다시 실행해요.
  ```bash
  adb shell am force-stop --user 10 ai.pleos.playground.service.location.fused
  ```
- 첫 실행부터 다시 보려면 앱 데이터를 지워요. Pleos 에뮬레이터에서는 앱이 사용자 10으로 돌아요.
  ```bash
  adb shell pm clear --user 10 com.gooodhandletruck.zippick
  ```

## 🖥️ 개발 환경

| 항목 | 내용 |
|---|---|
| 에뮬레이터 | **Pleos Connect v2.1.0** (Android 14, API 34 · Pleos Connect 시스템 이미지) |
| 화면 규격 | Connect-S 17". 앱 창은 1:2 분할 **1700×1184dp**(가로), 1:1:1 분할 **840×1184dp**(세로) 두 크기를 모두 지원해요 |
| AVD 설정 | Automotive → Pleos Connect v2.1.0 · RAM 4GB · Default boot Cold |
| 개발 도구 | Android Studio, JDK 17, Kotlin 2.1.0, Android Gradle Plugin 8.7.3 |
| SDK 버전 | minSdk 28 · compileSdk / targetSdk 34 |
| Pleos SDK | NaviHelper 3.0.5 (길 안내·도착) · FusedLocation 1.1.3 (위치·근접 알림) · TextToSpeech 3.3.1 (음성 안내) |
| 주요 라이브러리 | Jetpack Compose, kotlinx.serialization, OkHttp |

**에뮬레이터 설치**
1. Android Studio → **Tools → SDK Manager → SDK Update Sites**에서 **+**를 누르고 아래를 추가해요.
   - Name: `Pleos Connect System Image`
   - Windows / Linux: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`
   - Mac (Apple Silicon): `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`
2. **SDK Platforms** 탭 → **Show Package Details** 체크 → **Android 14.0 (API 34) › Pleos Connect**를 설치해요.
3. **Device Manager → + → Create Virtual Device → Automotive → Pleos Connect v2.1.0**으로 AVD를 만들어요.

## 📁 폴더 구조

```
apk/                             제출용 release APK
app/src/main/java/com/gooodhandletruck/zippick/
├── ui/                          화면(S-01~S-10)과 공통 부품, 색·글자·간격(theme)
│   ├── nearby/ detail/ saved/ history/ alerts/ settings/ onboarding/ driving/
│   ├── navigation/              화면 목록과 이동 규칙
│   └── format/                  가격·거리·면적·방향 표기
└── data/
    ├── remote/                  서버 통신(기기 등록·헤더), 응답 모양, 앱 데이터로 바꾸기
    ├── RemoteRepository.kt      주변 단지·상세·저장 (서버)
    ├── InterestConditionSync.kt 알림 조건을 서버 관심 조건으로 보내기
    ├── AlertInboxRepository.kt  새 거래 알림
    ├── LocationRepository.kt    차 위치와 근접 알림 (Pleos FusedLocation)
    ├── NavigationRepository.kt  길 안내와 도착 (Pleos NaviHelper)
    ├── SpeechRepository.kt      음성 안내 (Pleos TextToSpeech)
    └── FakeRepository.kt        서버 없이 화면을 볼 때 쓰는 목업
app/src/test/                    단위 테스트
```

## 🔗 참고

- 백엔드 저장소: https://github.com/GooodHandleTruck-Hyndai/backend
- 협업 규칙(브랜치, 커밋, PR): [CONTRIBUTING.md](CONTRIBUTING.md)
- [Pleos 개발 시작하기](https://document.pleos.ai/en/docs/pleos-only/getting-started) · [앱 디자인 가이드 (주행 안전)](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/intro)
