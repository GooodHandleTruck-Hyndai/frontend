package com.gooodhandletruck.zippick.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gooodhandletruck.zippick.R

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

// Figma 텍스트 스타일이 쓰는 굵기 네 가지(Regular 400, Medium 500, Bold 700, ExtraBold 800)
val AstaSans = FontFamily(
    astaSans(FontWeight.Normal),
    astaSans(FontWeight.Medium),
    astaSans(FontWeight.Bold),
    astaSans(FontWeight.ExtraBold)
)

private fun textStyle(size: Int, lineHeight: Int, weight: FontWeight) = TextStyle(
    fontFamily = AstaSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp
)

/**
 * Figma 디자인 시스템(✦ Foundation)의 텍스트 스타일 14개. 이름도 Figma에 맞췄다(Body2/Bold → Body2Bold).
 * 크기/줄높이 sp, 자간 0. 20sp보다 작은 글자는 쓰지 않는다.
 */
object AppType {
    // Headline은 아직 쓰지 않는다(CLAUDE.md 화면 규격의 제목은 40 이하)
    val Headline1 = textStyle(60, 80, FontWeight.ExtraBold)
    val Headline2 = textStyle(48, 64, FontWeight.Bold)

    val Title1 = textStyle(40, 52, FontWeight.Bold) // 주행 중 단지 이름, 상세 단지 이름·대표 가격
    val Title2 = textStyle(36, 44, FontWeight.Bold) // 화면 제목, 카드 가격, 상세 칸 제목, 번호 배지
    val Body1 = textStyle(30, 38, FontWeight.Normal) // 설정 항목 이름, 주행 중 대기 문구, 공시가격
    val Body2 = textStyle(28, 36, FontWeight.Normal) // 본문, 설명, 거래 표 날짜·층
    val Body2Bold = textStyle(28, 36, FontWeight.Bold) // 카드 단지 이름, 거래 표 금액
    val Body3 = textStyle(26, 34, FontWeight.Normal) // 주소, 보조 설명
    val Label1Medium = textStyle(24, 28, FontWeight.Medium) // 칩, 고른 탭 이름, 방향·거리, 상태 알약
    val Label1 = textStyle(24, 28, FontWeight.Normal) // 고르지 않은 탭 이름
    val Label2 = textStyle(20, 24, FontWeight.Normal) // 기준일, 캡션, 약도 말풍선

    // 버튼 글자: Button/Large·Medium·Small. Small 버튼(64)은 76보다 작아 쓰지 않는다
    val ButtonL = textStyle(32, 40, FontWeight.Bold)
    val ButtonM = textStyle(30, 38, FontWeight.Bold)
    val ButtonS = textStyle(26, 34, FontWeight.Bold)
}

/**
 * Material 컴포넌트가 기본으로 쓰는 글자도 위 스타일 안에서 고른다.
 * Material 기본값은 11~16sp가 많아서 그대로 두면 20sp보다 작아진다.
 */
val AppTypography = Typography(
    displayLarge = AppType.Title1,
    displayMedium = AppType.Title1,
    displaySmall = AppType.Title1,
    headlineLarge = AppType.Title2,
    headlineMedium = AppType.Title2,
    headlineSmall = AppType.Title2,
    titleLarge = AppType.Body1,
    titleMedium = AppType.Body1,
    titleSmall = AppType.Label1Medium,
    bodyLarge = AppType.Body2,
    bodyMedium = AppType.Body3,
    bodySmall = AppType.Label2,
    labelLarge = AppType.Label1Medium,
    labelMedium = AppType.Label2,
    labelSmall = AppType.Label2
)
