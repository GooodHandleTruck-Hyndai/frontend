package com.gooodhandletruck.zippick.data.remote

/*
 * 백엔드 Swagger 예시(건물 21 한화오벨리스크, 마포구 도화동)로 만든 응답 본문.
 * 칸 이름과 모양은 백엔드 저장소 main의 DTO를 따랐다. 서버 형식이 바뀌면 여기와 dto/를 함께 고친다.
 */
object ApiSamples {

    const val DEVICE_ID = "7f3c2a9e-1b4d-4c8e-9f21-0a6b5d4e3c21"

    const val DEVICE = """
        {"success":true,"code":"OK","message":"요청 성공",
         "data":{"anonymousId":"$DEVICE_ID","createdAt":"2026-11-08T16:40:12.123456+09:00"}}
    """

    const val NEARBY = """
        {"success":true,"code":"OK","message":"요청 성공","data":{
          "dataBaseDate":"2026-11-07",
          "buildings":[
            {"buildingId":21,"name":"한화오벨리스크","type":"APT","address":"서울 마포구 도화동 555",
             "lat":37.54004,"lon":126.94547,"distanceM":180,"imageUrl":null,
             "latestDeal":{"price":59700,"dealDate":"2026-09-30","areaM2":36.49,"areaPyeong":11.0}},
            {"buildingId":9,"name":"대흥동태영아파트","type":"APT","address":"서울 마포구 대흥동 4",
             "lat":37.5478,"lon":126.9426,"distanceM":860,"imageUrl":null,"latestDeal":null}
          ]}}
    """

    const val NEARBY_EMPTY = """
        {"success":true,"code":"OK","message":"요청 성공","data":{"dataBaseDate":null,"buildings":[]}}
    """

    const val DETAIL = """
        {"success":true,"code":"OK","message":"요청 성공","data":{
          "buildingId":21,"name":"한화오벨리스크","type":"APT","jibunAddress":"서울 마포구 도화동 555",
          "roadAddress":null,"builtYear":2004,"lat":37.54004,"lon":126.94547,"imageUrl":null,"saved":false,
          "dataBaseDate":"2026-11-07",
          "recentTransactions":[
            {"dealDate":"2026-09-30","areaM2":36.49,"areaPyeong":11.0,"floor":15,"dong":"101","price":59700},
            {"dealDate":"2026-09-12","areaM2":84.97,"areaPyeong":25.7,"floor":7,"dong":null,"price":83700}
          ],
          "hasMoreTransactions":true}}
    """

    const val TRANSACTIONS = """
        {"success":true,"code":"OK","message":"요청 성공","data":{
          "transactions":[
            {"dealDate":"2026-08-21","areaM2":59.98,"areaPyeong":18.1,"floor":3,"dong":"102","price":71000}
          ],
          "page":1,"size":20,"hasNext":false}}
    """

    const val PRICE_TREND = """
        {"success":true,"code":"OK","message":"요청 성공","data":{"months":[
          {"yearMonth":"2025-12","avgPrice":83700,"count":10},
          {"yearMonth":"2026-01","avgPrice":null,"count":0}
        ]}}
    """

    const val FAVORITE = """
        {"success":true,"code":"OK","message":"요청 성공","data":
          {"buildingId":21,"name":"한화오벨리스크","type":"APT","jibunAddress":"서울 마포구 도화동 555",
           "lat":null,"lon":null,"savedAt":"2026-11-08T16:41:03.5+09:00"}}
    """

    const val FAVORITES = """
        {"success":true,"code":"OK","message":"요청 성공","data":[
          {"buildingId":21,"name":"한화오벨리스크","type":"APT","jibunAddress":"서울 마포구 도화동 555",
           "lat":37.54004,"lon":126.94547,"savedAt":"2026-11-08T16:41:03+09:00"}
        ]}
    """

    const val INTEREST_CONDITION = """
        {"success":true,"code":"OK","message":"요청 성공","data":{
          "regionCodes":["11440"],"priceMin":80000,"priceMax":150000,"types":["APT"],
          "areaMinM2":59,"areaMaxM2":85,"alertEnabled":true}}
    """

    const val ALERTS = """
        {"success":true,"code":"OK","message":"요청 성공","data":{"alerts":[
          {"alertId":7,"buildingId":21,"buildingName":"한화오벨리스크",
           "deal":{"price":59700,"dealDate":"2026-10-12","areaM2":36.49,"areaPyeong":11.0},
           "read":false,"createdAt":"2026-11-09T04:30:01.123456+09:00"}
        ]}}
    """

    const val ALERT_READ = """
        {"success":true,"code":"OK","message":"요청 성공",
         "data":{"alertId":7,"read":true,"readAt":"2026-11-09T10:12:00+09:00"}}
    """

    /** 저장 취소처럼 data가 없는 성공. 관심 조건을 정한 적이 없을 때의 조회도 이렇게 온다 */
    const val OK_NO_DATA = """{"success":true,"code":"OK","message":"요청 성공"}"""

    const val BUILDING_NOT_FOUND = """{"success": false, "code": "B001", "message": "건물을 찾을 수 없어요"}"""

    const val DEVICE_NOT_FOUND = """{"success":false,"code":"D002","message":"등록되지 않은 기기예요"}"""

    const val FAVORITE_ALREADY_EXISTS = """{"success":false,"code":"F001","message":"이미 저장한 건물이에요"}"""

    const val ALERT_NOT_FOUND = """{"success":false,"code":"N001","message":"알림을 찾을 수 없어요"}"""

    /** 개발 서버가 꺼져 있을 때 ngrok이 주는 404 페이지(앞부분) */
    const val NGROK_OFFLINE_HTML = """<!DOCTYPE html><html><head><noscript>
        The endpoint gullible-satisfy-fructose.ngrok-free.dev is offline. (ERR_NGROK_3200)</noscript></head></html>"""
}
