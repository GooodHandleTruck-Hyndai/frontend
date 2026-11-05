package com.gooodhandletruck.sidong.data

import kotlinx.serialization.json.Json

/** 목업과 서버 응답을 읽는 JSON 설정. 서버가 필드를 더해도 앱이 깨지지 않게 모르는 필드는 건너뛴다. */
val AppJson = Json { ignoreUnknownKeys = true }
