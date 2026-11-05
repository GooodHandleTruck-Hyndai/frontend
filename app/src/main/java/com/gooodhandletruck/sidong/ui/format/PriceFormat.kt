package com.gooodhandletruck.sidong.ui.format

import java.util.Locale
import kotlin.math.roundToInt

/*
 * 가격 문자열은 이 파일에서만 만든다 (CLAUDE.md 데이터 규칙).
 */

private const val MANWON_PER_EOK = 10_000
private const val MANWON_PER_TENTH_EOK = 1_000.0
private const val TENTHS_PER_EOK = 10

/**
 * 카드·상세의 가격 표기: 억 + 쉼표 넣은 만 (화면 설계서 S-04 '가격 표시 규칙').
 * 123000 → "12억 3,000만", 120000 → "12억", 9500 → "9,500만"
 */
fun formatPrice(priceManwon: Int): String {
    val eok = priceManwon / MANWON_PER_EOK
    val man = priceManwon % MANWON_PER_EOK
    return buildList {
        if (eok > 0) add("${grouped(eok)}억")
        if (man > 0) add("${grouped(man)}만")
    }.joinToString(" ").ifEmpty { "0만" }
}

/**
 * 약도 말풍선의 가격: 1억부터 억 소수 첫째 자리(반올림), 1억 미만은 만 (화면 설계서 S-04 '가격 표시 규칙').
 * 123456 → "12.3억", 120000 → "12억", 9500 → "9,500만"
 */
fun formatPriceShort(priceManwon: Int): String {
    if (priceManwon < MANWON_PER_EOK) return "${grouped(priceManwon)}만"
    val tenths = (priceManwon / MANWON_PER_TENTH_EOK).roundToInt()
    val whole = tenths / TENTHS_PER_EOK
    val tenth = tenths % TENTHS_PER_EOK
    return if (tenth == 0) "${grouped(whole)}억" else "${grouped(whole)}.${tenth}억"
}

/** 음성의 가격: 카드 형식 + " 원" (화면 설계서 S-04 '가격 표시 규칙'). 123000 → "12억 3,000만 원" */
fun formatPriceVoice(priceManwon: Int): String = "${formatPrice(priceManwon)} 원"

private fun grouped(number: Int): String = String.format(Locale.KOREA, "%,d", number)
