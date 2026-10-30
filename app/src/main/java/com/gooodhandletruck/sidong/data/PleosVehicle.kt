package com.gooodhandletruck.sidong.data

/*
 * 플레오스 Vehicle SDK 사용 예시 (주석 상태)
 *
 * Gradle 동기화가 끝나면 아래 주석을 풀고, 빨간 줄이 뜨는 `Vehicle` 에 커서를 두고
 * Alt+Enter (Mac: Option+Enter) → Import 하면 패키지 경로가 자동으로 잡혀요.
 *
 * 사용 흐름 (공식 문서 기준):
 *   1) Vehicle(context) 로 인스턴스 생성
 *   2) initialize() 로 차량 서비스 연결
 *   3) getDoor(), getDisplay() 같은 getXxx() 로 도메인 객체를 받아서 Get/Set/Callback 호출
 *   4) 다 쓰면 release()
 *
 * 도메인마다 AndroidManifest 에 pleos.car.permission.* 권한이 필요해요.
 *   예) Door → CAR_DOORS, HVAC → CAR_CLIMATE, EvBattery → CAR_ENERGY, CarInfo → CAR_INFO
 *
 * 문서: https://document.pleos.ai/en/api-reference/connect-sdk-pleos/Vehicle/obtain-domain-object
 */

// class PleosVehicle(context: android.content.Context) {
//
//     private val vehicle = Vehicle(context).apply { initialize() }
//
//     fun readDriverDoor(onResult: (Int?) -> Unit) {
//         vehicle.getDoor().getCurrentDoorPosition(
//             area = DoorArea(DoorValue.ROW_1_LEFT),
//             onComplete = { pos -> onResult(pos) },
//             // 실패 콜백 등 나머지 파라미터는 문서의 get-property 페이지 참고
//         )
//     }
//
//     fun release() = vehicle.release()
// }
