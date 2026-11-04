package com.gooodhandletruck.sidong.ui.navigation

/**
 * 앱의 화면 9개. id는 화면 설계서의 화면 번호다.
 * 정차 중 화면(S-01~S-07)은 백스택에 쌓이고, 주행 중 화면(S-08·S-09)은 백스택 위를 덮기만 한다.
 */
sealed class Screen(val id: String) {

    /** 정차 중 화면. 백스택에 쌓인다. */
    sealed class Parked(id: String) : Screen(id)

    /** 주행 중 화면. 백스택에 쌓지 않고 정차 화면을 덮는다. */
    sealed class Driving(id: String) : Screen(id)

    /** 시작하기: 첫 실행 1단계, 위치 권한 */
    data object Welcome : Parked("S-01")

    /** 알림 조건: 첫 실행 2단계이거나, 설정·기록에서 다시 연다 */
    data class AlertSetup(val isOnboarding: Boolean) : Parked("S-02")

    /** 주변 단지: 정차 중 홈 */
    data object Nearby : Parked("S-03")

    /** 단지 상세 */
    data class ComplexDetail(val complexId: String) : Parked("S-04")

    /** 저장한 단지 */
    data object Saved : Parked("S-05")

    /** 오늘 들은 단지 */
    data object History : Parked("S-06")

    /** 설정 */
    data object Settings : Parked("S-07")

    /** 주행 중 대기: 말할 단지가 없을 때 */
    data object DrivingIdle : Driving("S-08")

    /** 주행 중 단지 알림: 단지를 말한 뒤 2분 동안 */
    data class DrivingAlert(val complexId: String) : Driving("S-09")
}

/** 왼쪽 탭 4개. 탭을 누르면 그 탭의 첫 화면부터 다시 시작한다. */
enum class Tab(val root: Screen.Parked) {
    NEARBY(Screen.Nearby),
    SAVED(Screen.Saved),
    HISTORY(Screen.History),
    SETTINGS(Screen.Settings)
}
