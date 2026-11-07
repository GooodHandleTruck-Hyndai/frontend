package com.gooodhandletruck.sidong.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gooodhandletruck.sidong.R

/**
 * Asta Sans 가변 글꼴(굵기 300~800) 파일 하나에서 굵기 축(wght)만 바꿔 쓴다.
 * 굵기 축을 넘기는 Font(variationSettings)는 Compose 1.7에서 실험 API라 opt-in이 필요하다.
 */
@OptIn(ExperimentalTextApi::class)
private fun astaSans(weight: FontWeight) = Font(
    resId = R.font.asta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val AstaSans = FontFamily(
    astaSans(FontWeight.Normal),
    astaSans(FontWeight.Medium),
    astaSans(FontWeight.SemiBold),
    astaSans(FontWeight.Bold),
    astaSans(FontWeight.ExtraBold)
)

private fun textStyle(size: Int, lineHeight: Int, weight: FontWeight) = TextStyle(
    fontFamily = AstaSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp
)

/** 화면 설계서 '공통 규격 > 글자'의 8단계. 20sp보다 작은 글자는 쓰지 않는다. */
object AppType {
    val TitleL = textStyle(40, 52, FontWeight.Bold) // 주행 중 단지 이름, 상세 가격
    val TitleM = textStyle(36, 44, FontWeight.Bold) // 상세 단지 이름, 시작하기 제목
    val TitleS = textStyle(32, 40, FontWeight.Bold) // 화면 제목, 카드 가격
    val BodyL = textStyle(30, 38, FontWeight.SemiBold) // 카드 단지 이름
    val BodyM = textStyle(28, 36, FontWeight.Normal) // 본문, 거래 내역
    val BodyS = textStyle(26, 34, FontWeight.Normal) // 주소, 설명
    val LabelL = textStyle(24, 28, FontWeight.SemiBold) // 칩, 탭 이름, 방향·거리
    val LabelS = textStyle(20, 24, FontWeight.Medium) // 기준일, 캡션, 약도 말풍선

    // 시안(데모 화면)처럼 이름과 가격을 더 굵게(800) 쓰는 글자. 크기·줄높이는 위 8단계에서 고른다
    val DisplayL = textStyle(40, 52, FontWeight.ExtraBold) // 주행 중 단지 이름, 상세 단지 이름·대표 가격
    val DisplayM = textStyle(36, 44, FontWeight.ExtraBold) // 화면 제목, 카드 가격
    val DisplayS = textStyle(32, 40, FontWeight.ExtraBold) // 거래 표 가격, 번호 배지

    // 높이 96 이상 버튼의 글자
    val ButtonL = textStyle(30, 38, FontWeight.Bold)

    // labelL을 굵게(700): 76짜리 작은 버튼, 고른 탭 이름, 방향·거리 줄
    val LabelBold = textStyle(24, 28, FontWeight.Bold)
}

/**
 * Material 컴포넌트가 기본으로 쓰는 글자도 위 8단계 안에서 고른다.
 * Material 기본값은 11~16sp가 많아서 그대로 두면 20sp보다 작아진다.
 */
val AppTypography = Typography(
    displayLarge = AppType.TitleL,
    displayMedium = AppType.TitleL,
    displaySmall = AppType.TitleL,
    headlineLarge = AppType.TitleM,
    headlineMedium = AppType.TitleS,
    headlineSmall = AppType.TitleS,
    titleLarge = AppType.BodyL,
    titleMedium = AppType.BodyL,
    titleSmall = AppType.LabelL,
    bodyLarge = AppType.BodyM,
    bodyMedium = AppType.BodyS,
    bodySmall = AppType.LabelS,
    labelLarge = AppType.LabelL,
    labelMedium = AppType.LabelS,
    labelSmall = AppType.LabelS
)
