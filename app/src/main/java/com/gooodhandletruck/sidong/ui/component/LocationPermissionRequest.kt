package com.gooodhandletruck.sidong.ui.component

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.gooodhandletruck.sidong.data.LocationPermission

/**
 * 위치 권한 창을 띄우는 함수를 돌려준다. 창이 닫히면 [onResult]에 허용 여부가 온다.
 *
 * 권한에 그룹이 없어서 창 문구는 "집픽에서 알 수 없는 작업 실행하도록 허용하시겠습니까?"로 나온다. 앱에서 바꿀 수 없다.
 * 두 번째 창부터는 "허용하지 않고 다시 묻지 않음"이 나오고, 이걸 고르면 시스템이 창을 더 띄우지 않는다.
 * 이 에뮬레이터의 설정 앱('앱 > 집픽 > 권한')에는 이 권한이 보이지 않아서 설정 화면으로 보내지 않는다(공식 샘플과 같다).
 */
@Composable
fun rememberLocationPermissionRequest(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), onResult)
    return { launcher.launch(LocationPermission.NAME) }
}
