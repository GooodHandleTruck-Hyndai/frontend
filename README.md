# 시동 (Sidong): 플레오스 커넥트 차량 앱

> GooodHandleTruck · 주행 중 운전자를 돕는 차량용 서비스

Kotlin과 Jetpack Compose로 만든 플레오스 커넥트(Android Automotive) 앱이에요.
지금은 S-01 시작하기만 실제 화면이고, 나머지 8개(S-02~S-09)는 **제목과 이동 버튼만 있는 빈 화면**으로 이어져 있어요.
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
4. 에뮬레이터의 **⋯ (Extended controls) → Car data** 에서 기어를 D로 바꾸면 주행 화면(S-08)이 덮고, P로 돌리면 보던 화면으로 돌아와요.

## 📁 폴더 구조

```
app/src/main/java/com/gooodhandletruck/sidong/
├── MainActivity.kt              앱 진입점 (Activity 하나)
├── ui/
│   ├── AppRoot.kt               창 크기 판단, 주행·정차에 따라 화면 그리기
│   ├── AppViewModel.kt          화면 이동·주행 상태
│   ├── navigation/              Screen(화면 9개), Navigator(이동 규칙)
│   ├── component/               왼쪽 탭, 버튼, 상태 상자, 위치 권한 요청, 빈 화면 틀
│   ├── onboarding/ nearby/ detail/ saved/ history/ settings/ driving/   화면별 파일
│   └── theme/                   색·글자·간격 (화면 설계서 공통 규격)
└── data/
    ├── DrivingStateRepository.kt 주행 중인지 (AAOS UX 제한)
    ├── OnboardingStore.kt       첫 실행을 끝냈는지
    ├── LocationPermission.kt    위치 권한(FUSED_LOCATION)이 있는지
    ├── CarDataRepository.kt     속도/기어 구독 (표준 AAOS API, 지금은 안 씀)
    └── ApiClient.kt             서버 API
```

## 📚 참고 문서
- [플레오스 개발 시작하기](https://document.pleos.ai/en/docs/pleos-only/getting-started)
- [앱 디자인 가이드 (주행 안전)](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/intro)
- [Vehicle SDK 튜토리얼](https://document.pleos.ai/en/docs/pleos-only/tutorial/vehicle-sdk)
