"""집픽 앱 화면을 피그마로 가져가기 위한 정적 HTML 목업을 만든다.

앱 코드의 값(AppColors·AppType·AppDimens, res/drawable 아이콘, assets/mock 데이터, strings.xml 문구)을
그대로 옮겨 화면마다 앱 창 크기(840×1184, 넓은 창 1700×1184)의 프레임으로 그린다.
JavaScript 없이 HTML/CSS만 쓴다(html.to.design이 그대로 레이어로 바꾸게).

사용: python3 gen.py <frontend 폴더> <출력 폴더>
"""
import json
import math
import sys
import xml.etree.ElementTree as ET
from html import escape
from pathlib import Path

FRONT = Path(sys.argv[1])
OUT = Path(sys.argv[2])
RES = FRONT / "app/src/main/res"
MOCK = FRONT / "app/src/main/assets/mock"

# ---------- 규격 (ui/theme) ----------
ACCENT = "#157A52"
TP = "#131417"
TS = "rgba(19,20,23,0.84)"
TT = "rgba(19,20,23,0.64)"
BG = "#F7F8FA"
SURFACE = "#FFFFFF"
OUTLINE = "#E0E1E6"
BTN2 = "#EDEEF2"
ACCENT_SOFT = "rgba(21,122,82,0.10)"
ACCENT_LINE = "rgba(21,122,82,0.40)"
OFFICIAL = "#B45309"
OFFICIAL_SOFT = "rgba(180,83,9,0.10)"

NARROW = (840, 1184)
WIDE = (1700, 1184)
RAIL = 136
PAD_N = 32
PAD_W = 40

# ---------- 아이콘 (res/drawable 벡터 → SVG) ----------
A = "{http://schemas.android.com/apk/res/android}"


def load_icons():
    icons = {}
    for f in sorted((RES / "drawable").glob("ic_*.xml")):
        root = ET.parse(f).getroot()
        paths = []
        for p in root.iter("path"):
            paths.append({
                "d": p.get(A + "pathData"),
                "fill": (p.get(A + "fillColor") or "#00000000") != "#00000000",
                "sw": p.get(A + "strokeWidth"),
            })
        icons[f.stem[3:]] = paths
    return icons


ICONS = load_icons()


def icon(name, size, color, rotate=0):
    parts = []
    for p in ICONS[name]:
        attrs = [f'd="{p["d"]}"', f'fill="{color if p["fill"] else "none"}"']
        if p["sw"]:
            attrs.append(f'stroke="{color}" stroke-width="{p["sw"]}" stroke-linecap="round" stroke-linejoin="round"')
        parts.append(f"<path {' '.join(attrs)}/>")
    style = f' style="transform:rotate({rotate}deg)"' if rotate else ""
    return (f'<svg class="ic" width="{size}" height="{size}" viewBox="0 0 24 24"{style} '
            f'xmlns="http://www.w3.org/2000/svg">{"".join(parts)}</svg>')


# ---------- 포맷 (ui/format) ----------
def grouped(n):
    return f"{n:,}"


def fmt_price(m):
    eok, man = divmod(m, 10000)
    out = []
    if eok:
        out.append(f"{grouped(eok)}억")
    if man:
        out.append(f"{grouped(man)}만")
    return " ".join(out) or "0만"


def fmt_price_short(m):
    if m < 10000:
        return f"{grouped(m)}만"
    tenths = round(m / 1000)
    whole, tenth = divmod(tenths, 10)
    return f"{grouped(whole)}억" if tenth == 0 else f"{grouped(whole)}.{tenth}억"


def fmt_distance(m):
    r = round(m / 10) * 10
    if r < 1000:
        return f"{r}m"
    t = round(m / 100)
    w, f = divmod(t, 10)
    return f"{w}km" if f == 0 else f"{w}.{f}km"


def fmt_area(a):
    return f"{int(a)}㎡"


def month_day(iso):
    _, mo, d = iso.split("-")
    return f"{int(mo)}월 {int(d)}일"


# ---------- 위치 (data/Geo.kt) ----------
CENTER = (37.4979, 127.0276)  # 시연 지역 가운데(강남역 둘레), 차는 북쪽을 본다
HEADING = 0.0
DIRS = ["앞", "오른쪽 앞", "오른쪽", "오른쪽 뒤", "뒤", "왼쪽 뒤", "왼쪽", "왼쪽 앞"]


def distance(a, b):
    la1, lo1, la2, lo2 = map(math.radians, (a[0], a[1], b[0], b[1]))
    h = math.sin((la2 - la1) / 2) ** 2 + math.cos(la1) * math.cos(la2) * math.sin((lo2 - lo1) / 2) ** 2
    return 2 * 6371008.8 * math.asin(math.sqrt(h))


def bearing(a, b):
    p1, p2 = math.radians(a[0]), math.radians(b[0])
    dl = math.radians(b[1] - a[1])
    y = math.sin(dl) * math.cos(p2)
    x = math.cos(p1) * math.sin(p2) - math.sin(p1) * math.cos(p2) * math.cos(dl)
    return (math.degrees(math.atan2(y, x)) + 360) % 360


def place(c):
    pos = (c["lat"], c["lon"])
    rel = (bearing(CENTER, pos) - HEADING) % 360
    idx = int((rel + 22.5) / 45) % 8
    return {"dir": DIRS[idx], "deg": idx * 45, "dist": distance(CENTER, pos), "rel": rel}


# ---------- 데이터 (assets/mock) ----------
NEARBY = json.loads((MOCK / "complexes_nearby.json").read_text(encoding="utf-8"))["items"]
DETAILS = {d["complexId"]: d for d in json.loads((MOCK / "complex_details.json").read_text(encoding="utf-8"))}
BY_ID = {c["complexId"]: c for c in NEARBY}
for c in NEARBY:
    c["place"] = place(c)


def nearby_within(radius, limit=10):
    items = sorted((c for c in NEARBY if c["place"]["dist"] <= radius), key=lambda c: c["place"]["dist"])
    return items[:limit]


# ---------- 부품 (ui/component) ----------
def t(cls, text, color=TP, extra=""):
    return f'<div class="{cls}" style="color:{color};{extra}">{escape(text)}</div>'


def button(text, kind="primary", height=96, pad=32, ic=None, ic_tint=None, grow=False, disabled=False):
    large = height >= 96
    if disabled:
        bg, fg = BTN2, TT
    elif kind == "primary":
        bg, fg = ACCENT, "#FFFFFF"
    else:
        bg, fg = BTN2, TP
    icon_html = icon(ic, 32 if large else 28, ic_tint if (ic_tint and not disabled) else fg) if ic else ""
    cls = "buttonL" if large else "labelBold"
    flex = "flex:1 1 0;" if grow else ""
    return (f'<div class="btn" style="{flex}height:{height}px;padding:0 {pad}px;background:{bg};color:{fg}">'
            f'{icon_html}<span class="{cls}">{escape(text)}</span></div>')


def small_button(text, ic):
    return button(text, "secondary", height=76, pad=16, ic=ic)


def chip(text, selected=False, disabled=False):
    bg = ACCENT if selected and not disabled else BTN2
    fg = TT if disabled else ("#FFFFFF" if selected else TP)
    return f'<div class="chip labelL" style="background:{bg};color:{fg}">{escape(text)}</div>'


def number_badge(n, filled=False):
    bg, fg = (ACCENT, "#FFFFFF") if filled else (ACCENT_SOFT, ACCENT)
    return f'<div class="badge displayS" style="background:{bg};color:{fg}">{n}</div>'


def icon_badge(ic, size=56, filled=True):
    bg, fg = (ACCENT, "#FFFFFF") if filled else (ACCENT_SOFT, ACCENT)
    return (f'<div class="iconbadge" style="width:{size}px;height:{size}px;background:{bg}">'
            f'{icon(ic, round(size * 0.6), fg)}</div>')


def direction(p):
    return (f'<div class="row g8 center">{icon("direction", 28, ACCENT, p["deg"])}'
            f'<span class="labelBold" style="color:{ACCENT}">{escape(p["dir"])} · {fmt_distance(p["dist"])}</span></div>')


def saved_mark():
    return (f'<div class="savedmark">{icon("heart_filled", 20, ACCENT)}'
            f'<span class="labelS" style="color:{ACCENT}">저장함</span></div>')


def price_lines(price):
    months = price["periodDays"] // 30
    avg = price.get("avgPriceManwon")
    out = []
    if avg is not None:
        out.append(t("displayM", fmt_price(avg)))
        meta = f'{fmt_area(price["areaM2"])} · 최근 {months}개월 · {month_day(price["asOf"])} 기준'
    else:
        out.append(t("bodyM", f"최근 {months}개월 거래가 없어요", TS))
        meta = f'{fmt_area(price["areaM2"])} · {month_day(price["asOf"])} 기준'
    out.append(t("labelS", meta, TT))
    return "".join(out)


def complex_card(c, leading="", trailing=None, show_place=True, saved_mark_on=False, selected=False):
    border = f"3px solid {ACCENT}" if selected else f"1px solid {OUTLINE}"
    sub = []
    if show_place:
        sub.append(direction(c["place"]))
    if saved_mark_on:
        sub.append(saved_mark())
    sub_html = f'<div class="row g16 center">{"".join(sub)}</div>' if sub else ""
    trail = trailing if trailing is not None else icon("chevron_right", 32, TT)
    return (f'<div class="card row g24 center" style="border:{border};padding:24px 32px">{leading}'
            f'<div class="col g8 grow">{t("titleS ellipsis", c["name"])}{sub_html}{price_lines(c["price"])}</div>'
            f'{trail}</div>')


def status_box(message, detail=None, action=None):
    d = t("bodyS", detail, TS) if detail else ""
    a = button(action) if action else ""
    return (f'<div class="card col g24" style="padding:32px"><div class="col g8">{t("bodyM", message)}{d}</div>'
            f'{a}</div>')


def sort_chip(label):
    return chip(f"{label} ▾")


# ---------- 틀 (ui/AppRoot.kt) ----------
TABS = [("nearby", "주변", "pin"), ("saved", "저장", "heart"), ("history", "기록", "clock"), ("settings", "설정", "settings")]


def header(start_pad, end_pad):
    logo = f'<div class="logo">{icon("app_logo", 34, "#FFFFFF")}</div>'
    return (f'<div class="row g16 center" style="height:96px;padding:0 {end_pad}px 0 {start_pad}px;flex:none">'
            f'{logo}{t("titleS grow", "집픽")}'
            f'<div class="pill labelL" style="color:{TS}">정차 중</div></div>')


def rail(selected):
    items = []
    for key, label, ic in TABS:
        on = key == selected
        color = ACCENT if on else TS
        bg = ACCENT_SOFT if on else "transparent"
        items.append(f'<div class="railitem" style="background:{bg}">{icon(ic, 40, color)}'
                     f'<span class="{"labelBold" if on else "labelL"}" style="color:{color}">{label}</span></div>')
    return f'<div class="rail">{"".join(items)}</div>'


def frame(fid, title, note, size, body):
    w, h = size
    return (f'<section class="frame-wrap"><div class="frame-title"><b>{escape(title)}</b> '
            f'<span>{escape(note)} · {w}×{h}</span></div>'
            f'<div class="frame" id="{fid}" style="width:{w}px;height:{h}px">{body}</div></section>')


def parked(fid, title, note, tab, content, size=NARROW, right_pane=None):
    pad = PAD_W if size == WIDE else PAD_N
    if tab:
        start = (RAIL - 56) // 2
        body_row = rail(tab)
    else:
        start = pad
        body_row = ""
    list_w = 640 + pad * 2
    if right_pane is not None:
        main = (f'<div class="col" style="width:{list_w}px;flex:none;padding:16px {pad}px 0">{content}</div>'
                f'<div class="col grow" style="padding:16px {pad}px 32px 0">{right_pane}</div>')
    else:
        main = f'<div class="col grow" style="padding:16px {pad}px 0;max-width:{list_w if tab else 9999}px">{content}</div>'
    body = (f'<div class="col" style="height:100%">{header(start, pad)}'
            f'<div class="row grow" style="min-height:0">{body_row}{main}</div></div>')
    return frame(fid, title, note, size, body)


def driving(fid, title, note, content, buttons):
    body = (f'<div class="col" style="height:100%;padding:32px">'
            f'<div class="col grow center-all">{content}</div>'
            f'<div class="row g24">{buttons}</div></div>')
    return frame(fid, title, note, NARROW, body)


# ---------- 약도 (ui/nearby/NearbyMap.kt) ----------
def nearby_map(cards, radius, w, h):
    half = min(w, h) / 2 - 24
    per_m = half / (radius * 1.15)
    cx, cy = w / 2, h / 2
    rings = [m for m in (500, 1000, 3000) if m <= radius]
    svg = [f'<svg class="abs" width="{w}" height="{h}" viewBox="0 0 {w} {h}" xmlns="http://www.w3.org/2000/svg">']
    for m in rings:
        svg.append(f'<circle cx="{cx}" cy="{cy}" r="{m * per_m:.1f}" fill="none" stroke="{OUTLINE}" stroke-width="1"/>')
    s = 14
    svg.append(f'<path d="M{cx} {cy - s} L{cx + s * 0.8} {cy + s} L{cx} {cy + s * 0.5} L{cx - s * 0.8} {cy + s} Z" fill="{TP}"/>')
    svg.append("</svg>")
    parts = ["".join(svg)]
    for m in rings:
        r = m * per_m
        parts.append(f'<div class="abs labelS" style="left:{cx - r + 8:.0f}px;top:{cy:.0f}px;color:{TT}">{fmt_distance(m)}</div>')
    for i, c in enumerate(cards, 1):
        rel = math.radians(c["place"]["rel"])
        d = c["place"]["dist"] * per_m
        x, y = cx + math.sin(rel) * d, cy - math.cos(rel) * d
        if abs(x - cx) > w / 2 or abs(y - cy) > h / 2:
            continue
        avg = c["price"].get("avgPriceManwon")
        label = fmt_price_short(avg) if avg is not None else "-"
        lc = TP if avg is not None else TT
        parts.append(f'<div class="abs dot" style="left:{x - 4:.0f}px;top:{y - 4:.0f}px"></div>')
        parts.append(f'<div class="abs pin" style="left:{x:.0f}px;top:{y - 32:.0f}px">'
                     f'<div class="pinnum labelBold">{i}</div><span class="labelS" style="color:{lc}">{label}</span></div>')
    parts.append(f'<div class="abs labelS" style="left:24px;top:24px;color:{TT}">위 = 차가 향한 쪽</div>')
    parts.append(f'<div class="abs labelS" style="left:24px;bottom:24px;color:{TT}">'
                 f'번호 = 목록 순서 · 가격 = 대표 평형의 최근 3개월 평균</div>')
    return (f'<div class="map" style="width:{w}px;height:{h}px;flex:none">{"".join(parts)}</div>')


# ---------- 화면 ----------
def s01():
    lines = "".join(
        f'<div class="row g24 center">{icon_badge(ic, filled=False)}{t("bodyM grow", text, TS)}</div>'
        for ic, text in (("speaker", "주행 중에는 소리로 알려 드려요"), ("app_logo", "정차하면 주변 단지 가격을 화면으로 보여 드려요")))
    content = (f'<div class="col grow" style="justify-content:center;padding-bottom:32px">'
               f'{t("displayM", "지나가는 아파트, 얼마인지 알려 드려요")}<div style="height:32px"></div>'
               f'<div class="col g24">{lines}</div><div style="height:40px"></div>'
               f'<div class="row">{button("위치 권한 허용하고 시작하기", ic="pin")}</div></div>')
    return parked("s01", "S-01 시작하기", "첫 실행 1단계 · 탭 없음", None, content)


def s02():
    def section(title, chips):
        return (f'<div class="col g16">{t("labelL", title, TS)}'
                f'<div class="row g16 nowrap">{"".join(chips)}</div></div>')
    types = [chip("아파트", True), chip("오피스텔 · 준비 중", disabled=True), chip("원룸 · 준비 중", disabled=True)]
    prices = [chip(x, True) for x in ("5억 이하", "5억~10억", "10억~15억", "15억 넘게")]
    areas = [chip(x, True) for x in ("60㎡ 이하", "60~85㎡", "85㎡ 넘게")]
    content = (f'<div class="col g32 grow" style="padding-bottom:32px">'
               f'<div class="col g32 grow">'
               f'<div class="col g16">{t("displayM", "어떤 단지를 알려 드릴까요?")}'
               f'{t("bodyM", "고른 조건에 맞는 단지를 지날 때만 소리로 알려 드려요", TS)}</div>'
               f'{section("유형", types)}{section("가격대", prices)}{section("전용면적", areas)}</div>'
               f'<div class="row g24">{button("이 조건으로 알림 받기", grow=True)}{button("나중에 할게요", "secondary")}</div>'
               f'</div>')
    return parked("s02", "S-02 알림 조건", "첫 실행 2단계 · 탭 없음", None, content)


def nearby_list(radius, with_map, map_size=None):
    cards = nearby_within(radius)
    head = (f'<div class="col g8">{t("displayM", "주변 단지")}'
            f'{t("bodyM", f"내 위치에서 {fmt_distance(radius)} 안 · {len(cards)}곳", TT)}</div>')
    chips = "".join(chip(fmt_distance(m), m == radius) for m in (500, 1000, 3000))
    controls = f'<div class="row g16">{chips}{sort_chip("가까운 순")}</div>'
    items = []
    if with_map:
        items.append(nearby_map(cards, radius, *map_size))
    items += [complex_card(c, number_badge(i)) for i, c in enumerate(cards, 1)]
    return cards, f'<div class="col g24 grow">{head}{controls}<div class="col g16 clip">{"".join(items)}</div></div>'


def s03():
    _, content = nearby_list(1000, True, (640, 480))
    return parked("s03", "S-03 주변 단지", "정차 중 홈 · 좁은 창(1:1:1)", "nearby", content)


def s03_wide():
    cards, content = nearby_list(1000, False)
    pane = nearby_map(cards, 1000, WIDE[0] - RAIL - (640 + PAD_W * 2) - PAD_W, WIDE[1] - 96 - 16 - 32)
    return parked("s03w", "S-03 주변 단지 (넓은 창)", "넓은 창(1:2) · 왼쪽 목록 + 오른쪽 약도", "nearby", content, WIDE, pane)


def detail_card(inner):
    return f'<div class="card col g16" style="padding:32px">{inner}</div>'


def s04():
    d = DETAILS["c-1001"]
    c = BY_ID["c-1001"]
    area = next(a for a in d["areas"] if a["areaM2"] == d["defaultAreaM2"])
    months = d["periodDays"] // 30
    head = (f'<div class="row g24">{small_button("뒤로", "chevron_left")}'
            f'<div class="col g8 grow">{t("displayL ellipsis", d["name"])}'
            f'<div class="row g16 center"><div class="tag labelS" style="color:{TS}">아파트</div>{direction(c["place"])}</div>'
            f'{t("bodyS ellipsis", d["address"], TT)}</div></div>')
    area_chips = "".join(chip(fmt_area(a["areaM2"]), a["areaM2"] == area["areaM2"]) for a in d["areas"])
    official = (f'<div class="row g16 center" style="padding-top:8px">'
                f'<div class="officialchip labelBold">공시가격</div>{t("bodyL", fmt_price(area["officialPriceManwon"]))}</div>')
    price = (f'<div class="row g16 center">{t("labelL", "전용", TS)}{area_chips}</div>'
             f'<div class="col g8" style="padding-top:8px">{t("labelL", f"최근 {months}개월 평균", TS)}'
             f'{t("displayL", fmt_price(area["avgPriceManwon"]))}'
             f'{t("labelS", f"거래 {area["tradeCount"]}건 · {month_day(d["asOf"])} 기준", TT)}{official}</div>')
    rows = [f'<div class="row trade">{t("labelS c1", "계약일", TT)}{t("labelS c2", "층", TT)}'
            f'{t("labelS c3", "거래금액", TT)}</div>']
    for tr in area["recentTrades"]:
        rows.append('<div class="divider"></div>')
        rows.append(f'<div class="row trade" style="padding:8px 0">{t("bodyM c1", month_day(tr["contractDate"]), TS)}'
                    f'{t("bodyM c2", str(tr["floor"]) + "층", TS)}{t("displayS c3", fmt_price(tr["priceManwon"]))}</div>')
    trades = t("titleS", "최근 거래") + "".join(rows)

    def src(ic, text, tint=ACCENT):
        return f'<div class="row g16">{icon(ic, 32, tint)}{t("bodyM grow", text, TS)}</div>'
    sources = (t("titleS", "자료 기준") + src("calendar", f"실거래가는 {month_day(d['asOf'])} 기준이에요")
               + src("clock", "계약 뒤 30일 안에 신고돼서 최근 거래가 빠져 있을 수 있어요")
               + src("tag", "공시가격은 올해 1월 1일 기준이에요", OFFICIAL))
    body = (f'<div class="col g24 grow clip">{detail_card(price)}{detail_card(trades)}{detail_card(sources)}</div>')
    actions = f'<div class="row g24">{button("저장", ic="heart", grow=True)}{button("길 안내", "secondary", ic="route", grow=True)}</div>'
    content = f'<div class="col g24 grow" style="padding-bottom:32px">{head}{body}{actions}</div>'
    return parked("s04", "S-04 단지 상세", "좁은 창에서는 창 전체를 덮는다", "nearby", content)


def s05():
    ids = ["c-1006", "c-1001", "c-1009", "c-1011"]
    cards = [complex_card(BY_ID[i], trailing=small_button("길 안내", "route")) for i in ids]
    head = (f'<div class="row g16 center">{t("displayM grow", f"저장한 단지 · {len(ids)}곳")}'
            f'{button("새 거래 알림 2", "secondary", height=76, pad=16)}</div>')
    content = (f'<div class="col g24 grow">{head}<div class="row">{sort_chip("저장한 순")}</div>'
               f'<div class="col g16 clip">{"".join(cards)}</div></div>')
    return parked("s05", "S-05 저장한 단지", "카드 오른쪽 [길 안내] · 제목 옆 [새 거래 알림]", "saved", content)


def s06():
    rows = [("15:12", "c-1005", True), ("14:47", "c-1003", False), ("14:32", "c-1010", False), ("09:05", "c-1006", True)]
    cards = []
    for time, cid, saved in rows:
        lead = f'<div class="bodyS" style="width:88px;flex:none;color:{TT}">{time}</div>'
        cards.append(complex_card(BY_ID[cid], lead, saved_mark_on=saved))
    content = (f'<div class="col g24 grow">{t("displayM", f"오늘 들은 단지 · {len(rows)}곳")}'
               f'<div class="col g24 clip">{"".join(cards)}</div></div>')
    return parked("s06", "S-06 오늘 들은 단지", "왼쪽은 들은 시각, 저장한 단지는 '저장함'", "history", content)


def settings_card(inner):
    return f'<div class="card col g16" style="padding:24px;min-height:96px;justify-content:center">{inner}</div>'


def s07():
    switch = '<div class="switch"><div class="knob"></div></div>'
    radius = "".join(chip(x, x == "1km") for x in ("500m", "1km", "3km"))
    content = (f'<div class="col g24 grow">{t("displayM", "설정")}'
               + settings_card(f'<div class="row center">{t("bodyL grow", "주행 중 음성 알림")}{switch}</div>')
               + settings_card(t("bodyL", "알림 조건") + t("bodyS", "아파트 · 모든 가격대 · 모든 면적", TS))
               + settings_card(t("bodyL", "주변 범위 기본값") + f'<div class="row g16">{radius}</div>')
               + settings_card(t("bodyL", "앱 정보") + t("bodyS", "버전 0.1.0", TS)
                               + t("bodyS", "단지 이름과 가격은 시연용 가상 데이터예요", TS))
               + "</div>")
    return parked("s07", "S-07 설정", "", "settings", content)


def s08():
    content = (f'{icon_badge("speaker", 96, filled=False)}<div style="height:32px"></div>'
               f'{t("bodyL", "단지를 지나면 소리로 알려 드릴게요", TP, "text-align:center")}')
    return driving("s08", "S-08 주행 중 대기", "주행 중 · 머리글·탭 없음", content,
                   button("주변 단지 듣기", height=160, ic="speaker", grow=True))


def s09():
    name = BY_ID["c-1001"]["name"]
    card = (f'<div class="namecard">{icon("speaker", 40, ACCENT)}{t("displayL ellipsis", name)}</div>')
    buttons = (button("다시 듣기", "secondary", height=160, ic="speaker", grow=True)
               + button("저장", height=160, ic="heart", grow=True))
    return driving("s09", "S-09 주행 중 단지 알림", "단지를 말한 뒤 2분 · 가격은 음성으로만", card, buttons)


def s10():
    alerts = [("c-1006", 157500, "2026-10-30", False), ("c-1001", 123000, "2026-10-24", False),
              ("c-1009", 143000, "2026-10-12", True)]
    cards = []
    for cid, price, date, read in alerts:
        c = BY_ID[cid]
        dot = SURFACE if read else ACCENT
        line = f'{fmt_area(c["price"]["areaM2"])} · {fmt_price(price)} · {month_day(date)} 계약'
        cards.append(f'<div class="card row g16 center" style="padding:24px;min-height:76px">'
                     f'<div class="unread" style="background:{dot}"></div>'
                     f'<div class="col g8 grow">{t("bodyL ellipsis", c["name"])}{t("bodyS", line, TT if read else TS)}</div></div>')
    content = (f'<div class="col g24 grow"><div class="row">{small_button("뒤로", "chevron_left")}</div>'
               f'{t("displayM", "새 거래 알림")}<div class="col g16 clip">{"".join(cards)}</div></div>')
    return parked("s10", "S-10 새 거래 알림", "저장한 단지에서 연다 · 안 읽은 알림은 초록 점", "saved", content)


CSS = f"""
@font-face {{ font-family: 'Asta Sans'; src: url('fonts/asta_sans.ttf') format('truetype'); font-weight: 100 900; }}
* {{ box-sizing: border-box; margin: 0; }}
body {{ background: #2B2D31; font-family: 'Asta Sans', 'Pretendard', 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif;
  padding: 64px; display: flex; flex-wrap: wrap; gap: 96px 64px; align-items: flex-start; }}
.intro {{ width: 100%; color: #E8E9ED; font-size: 22px; line-height: 1.6; }}
.intro h1 {{ font-size: 36px; margin-bottom: 8px; }}
.frame-wrap {{ display: flex; flex-direction: column; gap: 16px; }}
.frame-title {{ color: #E8E9ED; font-size: 24px; }}
.frame-title span {{ color: #9A9CA5; font-size: 18px; margin-left: 8px; }}
.frame {{ background: {BG}; overflow: hidden; position: relative; }}
.row {{ display: flex; flex-direction: row; }}
.col {{ display: flex; flex-direction: column; }}
.center {{ align-items: center; }}
.center-all {{ align-items: center; justify-content: center; width: 100%; }}
.grow {{ flex: 1 1 0; min-width: 0; min-height: 0; }}
.clip {{ overflow: hidden; }}
.nowrap {{ flex-wrap: nowrap; }}
.g8 {{ gap: 8px; }} .g16 {{ gap: 16px; }} .g24 {{ gap: 24px; }} .g32 {{ gap: 32px; }}
.abs {{ position: absolute; }}
.ellipsis {{ white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }}
.ic {{ flex: none; display: block; }}
.titleL {{ font-size: 40px; line-height: 52px; font-weight: 700; }}
.titleM {{ font-size: 36px; line-height: 44px; font-weight: 700; }}
.titleS {{ font-size: 32px; line-height: 40px; font-weight: 700; }}
.bodyL {{ font-size: 30px; line-height: 38px; font-weight: 600; }}
.bodyM {{ font-size: 28px; line-height: 36px; font-weight: 400; }}
.bodyS {{ font-size: 26px; line-height: 34px; font-weight: 400; }}
.labelL {{ font-size: 24px; line-height: 28px; font-weight: 600; }}
.labelS {{ font-size: 20px; line-height: 24px; font-weight: 500; }}
.displayL {{ font-size: 40px; line-height: 52px; font-weight: 800; }}
.displayM {{ font-size: 36px; line-height: 44px; font-weight: 800; }}
.displayS {{ font-size: 32px; line-height: 40px; font-weight: 800; }}
.buttonL {{ font-size: 30px; line-height: 38px; font-weight: 700; }}
.labelBold {{ font-size: 24px; line-height: 28px; font-weight: 700; }}
.logo {{ width: 56px; height: 56px; border-radius: 16px; background: {ACCENT}; display: flex; align-items: center; justify-content: center; flex: none; }}
.pill {{ background: {BTN2}; border-radius: 999px; padding: 8px 24px; flex: none; }}
.rail {{ width: {RAIL}px; flex: none; display: flex; flex-direction: column; align-items: center; gap: 16px; padding: 8px 0; }}
.railitem {{ width: 104px; min-height: 76px; border-radius: 24px; padding: 16px 0; display: flex; flex-direction: column; align-items: center; gap: 8px; }}
.btn {{ display: flex; align-items: center; justify-content: center; gap: 8px; border-radius: 24px; min-width: 76px; flex: none; text-align: center; }}
.chip {{ min-height: 76px; min-width: 76px; border-radius: 24px; padding: 0 24px; display: flex; align-items: center; justify-content: center; flex: none; white-space: nowrap; }}
.badge {{ width: 72px; height: 72px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex: none; }}
.iconbadge {{ border-radius: 50%; display: flex; align-items: center; justify-content: center; flex: none; }}
.card {{ background: {SURFACE}; border: 1px solid {OUTLINE}; border-radius: 28px; width: 100%; flex: none; }}
.savedmark {{ display: flex; align-items: center; gap: 8px; background: {ACCENT_SOFT}; border-radius: 999px; padding: 8px 16px; }}
.tag {{ border: 1px solid {OUTLINE}; border-radius: 24px; padding: 8px 16px; }}
.officialchip {{ color: {OFFICIAL}; background: {OFFICIAL_SOFT}; border-radius: 999px; padding: 8px 16px; }}
.trade {{ align-items: center; }}
.trade .c1 {{ flex: 1.5 1 0; }} .trade .c2 {{ flex: 0.6 1 0; }} .trade .c3 {{ flex: 1.5 1 0; text-align: right; }}
.divider {{ height: 2px; background: {BTN2}; }}
.map {{ position: relative; background: {BG}; border: 1px solid {OUTLINE}; border-radius: 28px; overflow: hidden; }}
.dot {{ width: 8px; height: 8px; border-radius: 50%; background: {TT}; }}
.pin {{ height: 48px; transform: translate(-50%, -50%); display: flex; align-items: center; gap: 8px; background: {SURFACE};
  border: 2px solid {ACCENT_LINE}; border-radius: 999px; padding: 0 16px 0 8px; white-space: nowrap; }}
.pinnum {{ width: 32px; height: 32px; border-radius: 50%; background: {ACCENT}; color: #FFFFFF; display: flex; align-items: center; justify-content: center; }}
.switch {{ width: 52px; height: 32px; border-radius: 16px; background: {ACCENT}; position: relative; flex: none; }}
.knob {{ position: absolute; right: 4px; top: 4px; width: 24px; height: 24px; border-radius: 50%; background: #FFFFFF; }}
.namecard {{ width: 100%; display: flex; align-items: center; justify-content: center; gap: 16px; background: {SURFACE};
  border: 3px solid {ACCENT_LINE}; border-radius: 28px; padding: 40px 32px; }}
.unread {{ width: 16px; height: 16px; border-radius: 50%; flex: none; }}
"""


def main():
    frames = [s01(), s02(), s03(), s03_wide(), s04(), s05(), s06(), s07(), s08(), s09(), s10()]
    intro = ('<div class="intro"><h1>집픽 화면 목업</h1>'
             '앱 코드(색·글자·간격·아이콘·문구)를 그대로 옮긴 정적 화면이에요. 단지 이름과 가격은 목업 데이터예요. '
             '단위는 dp = px. 글꼴은 Asta Sans(fonts 폴더)예요.</div>')
    html = (f'<!DOCTYPE html><html lang="ko"><head><meta charset="utf-8"><title>집픽 화면 목업</title>'
            f'<style>{CSS}</style></head><body>{intro}{"".join(frames)}</body></html>')
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "index.html").write_text(html, encoding="utf-8")
    print("frames:", len(frames), "bytes:", len(html))


main()
