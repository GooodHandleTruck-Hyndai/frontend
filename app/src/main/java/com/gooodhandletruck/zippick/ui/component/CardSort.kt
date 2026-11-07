package com.gooodhandletruck.zippick.ui.component

import com.gooodhandletruck.zippick.data.model.ComplexSummary

/*
 * 단지 카드 정렬(S-03·S-05). 가격은 카드에 크게 쓰는 값과 같은 기준이다:
 * 대표 평형의 최근 3개월 평균, 없으면 마지막 거래 가격. 거래 기록이 아예 없는 단지는 어느 쪽으로 정렬해도 맨 뒤다.
 */

/** 카드 가격 정렬에 쓰는 값. 거래 기록이 없으면 null */
val ComplexSummary.sortPriceManwon: Int?
    get() = price?.avgPriceManwon ?: price?.lastTrade?.priceManwon

/** 가격 낮은 순. 가격이 없는 단지는 맨 뒤 */
fun List<ComplexCardModel>.sortedByPriceAscending(): List<ComplexCardModel> =
    sortedWith(compareBy(nullsLast()) { it.complex.sortPriceManwon })

/** 가격 높은 순. 가격이 없는 단지는 맨 뒤 */
fun List<ComplexCardModel>.sortedByPriceDescending(): List<ComplexCardModel> =
    sortedWith(compareByDescending<ComplexCardModel, Int?>(nullsFirst()) { it.complex.sortPriceManwon })

/** 가까운 순. 차 위치를 아직 몰라 거리가 없는 카드는 맨 뒤 */
fun List<ComplexCardModel>.sortedByDistance(): List<ComplexCardModel> =
    sortedWith(compareBy(nullsLast()) { it.place?.distanceMeters })
