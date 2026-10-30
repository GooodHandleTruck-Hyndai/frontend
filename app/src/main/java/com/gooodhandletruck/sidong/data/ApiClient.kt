package com.gooodhandletruck.sidong.data

import com.gooodhandletruck.sidong.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

/**
 * 백엔드 API 목록. axios 인스턴스 + API 함수 모음이라고 생각하면 돼요.
 * 엔드포인트를 추가할 때는 여기에 함수만 하나씩 늘리면 됩니다.
 */
interface BackendApi {
    @GET("health")
    suspend fun health(): HealthResponse
}

data class HealthResponse(val status: String)

object ApiClient {
    val api: BackendApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(OkHttpClient.Builder().addInterceptor(logging).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BackendApi::class.java)
    }
}
