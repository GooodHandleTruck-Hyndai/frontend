package com.gooodhandletruck.sidong.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** 차량 화면은 다크 테마가 기본이에요. */
@Composable
fun DriveTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(), content = content)
}

/** ViewModel 과 연결되는 화면 (React의 container 컴포넌트) */
@Composable
fun DriveScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DriveContent(state = state, onRetryBackend = viewModel::checkBackend)
}

/** 순수 UI (React의 presentational 컴포넌트). props 만 받아서 그려요. */
@Composable
fun DriveContent(state: DriveUiState, onRetryBackend: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = state.speedKmh?.let { "%.0f".format(it) } ?: "--",
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(12.dp))
                Text("km/h", fontSize = 32.sp, modifier = Modifier.padding(bottom = 24.dp))
            }

            Text("기어  ${state.gear}", fontSize = 40.sp)

            state.carError?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 22.sp)
            }

            Spacer(Modifier.height(48.dp))
            Text("서버: ${state.backendStatus}", fontSize = 24.sp)
            Spacer(Modifier.height(16.dp))
            // 차량 화면은 손가락이 크게 닿아도 되도록 버튼을 크게 만들어요
            Button(onClick = onRetryBackend, modifier = Modifier.height(72.dp)) {
                Text("서버 다시 확인", fontSize = 24.sp)
            }
        }
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun DriveContentPreview() {
    DriveTheme {
        DriveContent(
            state = DriveUiState(speedKmh = 62f, gear = "D", backendStatus = "연결됨 (ok)"),
            onRetryBackend = {}
        )
    }
}
