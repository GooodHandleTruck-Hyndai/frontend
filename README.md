# 시동 (Sidong): 플레오스 커넥트 차량 앱

> GooodHandleTruck · 주행 중 운전자를 돕는 차량용 서비스

Kotlin과 Jetpack Compose로 만든 플레오스 커넥트(Android Automotive) 앱이에요.
지금은 **속도, 기어, 서버 연결 상태**를 보여주는 기본 뼈대 상태예요.

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
3. 속도 권한 팝업이 뜨면 **허용** 을 눌러요.
4. 에뮬레이터의 **⋯ (Extended controls) → Car data** 에서 기어나 속도를 바꾸면 화면 숫자가 바뀌어요.

## 📁 폴더 구조

```
app/src/main/java/com/gooodhandletruck/sidong/
├── MainActivity.kt          앱 진입점, 권한 요청
├── ui/
│   ├── DriveScreen.kt       화면 (Compose)
│   └── MainViewModel.kt     화면 상태 + 로직
└── data/
    ├── CarDataRepository.kt 속도/기어 구독 (표준 AAOS API)
    ├── PleosVehicle.kt      플레오스 Vehicle SDK 예시 (주석 상태)
    └── ApiClient.kt         서버 API
```

## 📚 참고 문서
- [플레오스 개발 시작하기](https://document.pleos.ai/en/docs/pleos-only/getting-started)
- [앱 디자인 가이드 (주행 안전)](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/intro)
- [Vehicle SDK 튜토리얼](https://document.pleos.ai/en/docs/pleos-only/tutorial/vehicle-sdk)
