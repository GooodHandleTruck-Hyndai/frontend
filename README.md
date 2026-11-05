# 시동 (Sidong): 플레오스 커넥트 차량 앱

> GooodHandleTruck · 주행 중 운전자를 돕는 차량용 서비스

Kotlin과 Jetpack Compose로 만든 플레오스 커넥트(Android Automotive) 앱이에요.
지금은 S-01 시작하기, S-03 주변 단지(실제 위치 + 목업 단지), S-04 단지 상세, S-05 저장한 단지(목업)가 실제 화면이고, 나머지 5개는 **제목과 이동 버튼만 있는 빈 화면**으로 이어져 있어요.
화면 구성과 이동 규칙은 [화면 설계서](https://claude.ai/artifact/UgMDDBgkDfK7rZVFKAaoQt)에 있어요.

- 협업 규칙(브랜치, 커밋, PR): [CONTRIBUTING.md](CONTRIBUTING.md)
- 패키지: `com.gooodhandletruck.sidong`

---

## 개발 환경 세팅

### 1. Android Studio 설치
https://developer.android.com/studio 에서 최신 버전을 설치하세요.
첫 실행 마법사에서는 **Standard** 를 고르면 돼요.

> VS Code 단축키가 익숙하면 Settings → Keymap 에서 **VS Code** 를 고르세요.

### 2. 플레오스 에뮬레이터 설치
1. **Tools → SDK Manager** → **SDK Update Sites** 탭에서 **+** 를 눌러요.
   - Name: `Pleos Connect System Image`
   - URL
     - Windows / Linux: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`
     - Mac (Apple Silicon): `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`
2. **SDK Platforms** 탭 → 오른쪽 아래 **Show Package Details** 를 체크 → **Android 14.0 (API 34) › Pleos Connect** 를 체크하고 Apply 를 눌러요.
3. Android Studio 를 재시작해요.

### 3. 가상 차량(AVD) 만들기
1. **Shift 두 번** → `Device Manager` 를 검색해서 열어요.
2. **+ → Create Virtual Device** → **Automotive** → **Pleos Connect v2.1.0** 을 골라요.
3. 시스템 이미지는 **API 34 Pleos Connect**, Default boot 는 **Cold**, RAM 은 **4 GB** 로 설정해요.

### 4. 프로젝트 받고 실행하기
```bash
git clone https://github.com/GooodHandleTruck/<저장소이름>.git
```
1. Android Studio → **Open** → 클론한 폴더를 선택하고, Gradle Sync 가 끝날 때까지 기다려요.
2. 기기 목록에서 플레오스 AVD 를 고르고 ▶ **Run** 을 눌러요.
3. 첫 화면(S-01 시작하기)에서 [위치 권한 허용하고 시작하기]를 누르면 위치 권한 창이 떠요. 허용하든 거부하든 S-02 알림 조건을 거쳐 S-03 주변 단지로 가고, 그다음부터는 S-03에서 시작해요.
   - 첫 실행부터 다시 보려면 `adb shell pm clear --user 10 com.gooodhandletruck.sidong` 을 실행해요. 플레오스 에뮬레이터에서는 앱이 사용자 10으로 돌아요.
   - S-03은 차 위치(플레오스 FusedLocation)를 받아 `app/src/main/assets/mock/complexes_nearby.json` 의 가상 단지를 보여 줘요. 가상 단지는 강남역 둘레에 있으니 `adb emu geo fix 127.0276 37.4979` 로 에뮬레이터 위치를 옮겨요(경도가 먼저). 50m 넘게 옮기면 목록을 다시 불러와요.
   - 에뮬레이터는 차 방향(heading)을 늘 0(북쪽)으로 줘요. 그래서 방향 칩은 차가 북쪽을 본다고 보고 나와요.
   - S-03 카드를 누르면 S-04 단지 상세가 열려요. 1:2 창에서는 목록 옆 칸에 열리고([닫기]), 1:1:1 창에서는 창 전체를 덮어요([뒤로]). 상세는 `app/src/main/assets/mock/complex_details.json` 을 읽어요.
   - S-04에서 [저장]한 단지는 저장 탭(S-05)에 최근에 저장한 것부터 모여요. 서버를 붙이기 전까지는 이 기기에 기억해서 앱을 다시 켜도 남고, `pm clear` 로 앱 데이터를 지우면 사라져요.
   - 1:1:1 창은 앱 창 오른쪽 끝의 ⋮ 손잡이를 왼쪽으로 끌면 나와요(앱 창 841×1210dp). 손잡이를 화면 끝까지 끌어 되돌리면 시스템 화면이 앱 아이콘 가림막에 멈출 수 있어요. 그때는 오른쪽 끝 손잡이를 왼쪽으로 조금(100px쯤)만 끌었다 놓으면 1:2 창으로 돌아와요.
   - 에뮬레이터를 오래 켜 두면 플레오스 위치 서비스가 멈춰서 "지금 위치를 확인하지 못했어요"가 뜨거나 옛 위치(미국)가 나올 수 있어요. 그때는 `adb shell am force-stop --user 10 ai.pleos.playground.service.location.fused` 로 위치 서비스를 다시 띄우고, 앱도 다시 실행해요.
4. 에뮬레이터의 **⋯ (Extended controls) → Car data** 에서 기어를 D로 바꾸면 주행 화면(S-08)이 덮고, P로 돌리면 보던 화면으로 돌아와요.
5. 단위 테스트(가격 표기, 거리·방향 계산, 다시 불러오는 기준, 화면 쌓기 규칙, 목업 JSON)는 `./gradlew test` 로 돌려요.

## 📁 폴더 구조

```
app/src/main/java/com/gooodhandletruck/sidong/
├── MainActivity.kt              앱 진입점 (Activity 하나)
├── SidongApp.kt                 Application. 저장소를 담은 AppContainer를 들고 있음
├── ui/
│   ├── AppRoot.kt               창 크기 판단, 주행·정차에 따라 화면 그리기, 1:2 창의 목록+상세 두 칸
│   ├── AppViewModel.kt          화면 이동·주행 상태
│   ├── navigation/              Screen(화면 9개), Navigator(이동 규칙)
│   ├── component/               왼쪽 탭, 버튼, 칩, 단지 카드·사진 자리, 방향·거리 칩, 상태 상자, 위치 권한 요청, 빈 화면 틀
│   ├── format/                  가격·거리·면적·방향 표기 (가격 문자열은 여기서만 만듦)
│   ├── onboarding/ nearby/ detail/ saved/ history/ settings/ driving/   화면별 파일 (nearby/·detail/에 ViewModel)
│   └── theme/                   색·글자·간격 (화면 설계서 공통 규격)
└── data/
    ├── model/                   서버 응답과 같은 모양의 데이터 클래스 (kotlinx.serialization)
    ├── ComplexRepository.kt     단지 데이터 창구. 지금은 FakeRepository, 나중에 RemoteRepository
    ├── FakeRepository.kt        assets/mock/ 목업 JSON을 읽어 서버처럼 답함
    ├── AppContainer.kt          저장소를 한 번만 만들어 나눠 줌 (DI 프레임워크 없이)
    ├── AppJson.kt               JSON 읽기 설정
    ├── Geo.kt                   두 좌표 사이 거리·방위각
    ├── CarLocation.kt           차 위치·향한 쪽, 목록을 다시 불러올 만큼 움직였는지
    ├── LocationRepository.kt    차 위치 받기 (플레오스 FusedLocation, 화면이 구독할 때만)
    ├── DrivingStateRepository.kt 주행 중인지 (AAOS UX 제한)
    ├── OnboardingStore.kt       첫 실행을 끝냈는지
    ├── LocationPermission.kt    위치 권한(FUSED_LOCATION)이 있는지
    ├── CarDataRepository.kt     속도/기어 구독 (표준 AAOS API, 지금은 안 씀)
    └── ApiClient.kt             서버 API
app/src/main/assets/mock/        가상 단지 목업 JSON (Claude Docs 'API 명세 초안 검토'의 P0 형식)
app/src/test/                    단위 테스트
```

## 📚 참고 문서
- [플레오스 개발 시작하기](https://document.pleos.ai/en/docs/pleos-only/getting-started)
- [앱 디자인 가이드 (주행 안전)](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/intro)
- [Vehicle SDK 튜토리얼](https://document.pleos.ai/en/docs/pleos-only/tutorial/vehicle-sdk)
