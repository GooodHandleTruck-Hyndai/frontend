plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization) // @Serializable 데이터 클래스용
    alias(libs.plugins.ktlint) // ./gradlew ktlintCheck (검사) / ktlintFormat (자동 정리)
}

android {
    namespace = "com.gooodhandletruck.zippick"
    compileSdk = 34 // 에뮬레이터(Pleos Connect v2.1 = Android 14)에 맞춘다

    // 차량 API(android.car.*)를 쓰기 위한 설정. AAOS 앱에는 필수예요.
    useLibrary("android.car")

    defaultConfig {
        applicationId = "com.gooodhandletruck.zippick" // 앱마켓 출시 후에는 절대 변경 금지
        minSdk = 28 // 플레오스 최소 요구사항
        targetSdk = 34 // 플레오스 에뮬레이터 = API 34
        versionCode = 1
        versionName = "0.1.0"

        // 에뮬레이터에서 내 PC(localhost)에 접속하는 주소는 10.0.2.2 예요.
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 공모전 심사용 공개 서버(백엔드가 운영). debug 빌드는 그대로 내 PC 로컬 서버를 본다
            buildConfigField("String", "API_BASE_URL", "\"https://3-37-254-181.sslip.io/\"")
            // 심사·시연용 APK를 바로 설치할 수 있게 개발용 서명 키로 서명한다.
            // TODO: 앱마켓에 낼 때는 팀 정식 서명 키로 바꾼다
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.kotlinx.coroutines.android)
    // JSON 읽기: 지금은 assets/mock 목업, 나중에 서버 응답도 같은 클래스로 읽는다
    implementation(libs.kotlinx.serialization.json)

    // 백엔드 통신
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // 플레오스 Vehicle SDK (도어, 공조, 배터리 등 차량 도메인 API)
    implementation(libs.pleos.vehicle)

    // 플레오스 내비(길 안내·도착), 위치(근접 알림), 음성 안내
    implementation(libs.pleos.navihelper)
    implementation(libs.pleos.fusedlocation)
    implementation(libs.pleos.texttospeech)

    debugImplementation(libs.androidx.ui.tooling)

    // 단위 테스트: ./gradlew test
    testImplementation(libs.junit)
}
