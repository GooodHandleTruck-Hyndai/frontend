#!/usr/bin/env bash
# 로컬 백엔드(../backend 를 키 없이 띄운 것)에 가상 건물·거래를 넣거나 지운다.
#
# 국토부·카카오 키(.env)가 없으면 로컬 DB에 건물이 하나도 없어서 주변 건물·건물 상세·저장을 확인할 수 없다.
# 건물의 좌표 출처(coord_source)를 JUSO로 넣으면 서버가 카카오를 부르지 않고 DB 좌표를 그대로 쓴다.
#
# 사용법 (로컬 백엔드를 띄운 뒤):
#   scripts/local-backend-dummy.sh seed    # 넣기. 이미 넣었으면 지우고 다시 넣는다
#   scripts/local-backend-dummy.sh clear   # 지우기. 진짜 키로 데이터를 받기 전에 먼저 지운다
# 확인: ZIPPICK_LIVE_API=http://localhost:8080/ ./gradlew :app:testDebugUnitTest --tests '*LiveServerTest*' --rerun
#
# - 건물 7곳은 모두 마포구(11440) 도화동 둘레다. 이름은 '더미 '로 시작하고, 단지 이름과 가격은 가상 데이터다.
#   21번이 LiveServerTest가 쓰는 건물이다. 21·22번은 같은 지번의 아파트·오피스텔이다(개발 서버의 21·282번처럼).
# - 거래 날짜는 오늘에서 며칠 전으로 넣어서 언제 넣어도 '최근 거래'가 된다. 거래 지문(fingerprint)은 'dummy-'로 시작한다.
# - 백엔드 main 561452e의 테이블(buildings·trades)에 맞췄다. 백엔드 스키마가 바뀌면 같이 고친다.
# - MySQL은 3307 포트를 연 컨테이너(../backend/compose.yaml)를 찾아 쓴다. 다르면 MYSQL_CONTAINER=이름 을 붙인다.
set -euo pipefail

usage() {
    echo "사용법: $0 seed|clear" >&2
    exit 2
}

[[ $# -eq 1 ]] || usage
command="$1"
[[ "$command" == seed || "$command" == clear ]] || usage

container="${MYSQL_CONTAINER:-$(docker ps --filter publish=3307 --format '{{.Names}}' | head -1)}"
if [[ -z "$container" ]]; then
    echo "로컬 MySQL 컨테이너를 찾지 못했어요. ../backend 에서 ./gradlew bootRun 으로 서버를 먼저 띄워요" >&2
    exit 1
fi

# 컨테이너 안의 접속 정보(MYSQL_USER 등)로 SQL을 실행한다. 비밀번호 경고 한 줄은 숨긴다
run_sql() {
    docker exec -i "$container" sh -c \
        'mysql --default-character-set=utf8mb4 -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' \
        2> >(grep -v 'Using a password' >&2)
}

CLEAR_SQL="
DELETE FROM alerts WHERE building_id IN (SELECT id FROM buildings WHERE apt_nm LIKE '더미 %');
DELETE FROM favorites WHERE building_id IN (SELECT id FROM buildings WHERE apt_nm LIKE '더미 %');
DELETE FROM trades WHERE fingerprint LIKE 'dummy-%'
    OR building_id IN (SELECT id FROM buildings WHERE apt_nm LIKE '더미 %');
DELETE FROM buildings WHERE apt_nm LIKE '더미 %';
-- 다음 번호를 남은 데이터의 마지막 번호 + 1로 되돌린다(비었으면 1)
ALTER TABLE buildings AUTO_INCREMENT = 1;
ALTER TABLE trades AUTO_INCREMENT = 1;
"

if [[ "$command" == clear ]]; then
    run_sql <<<"$CLEAR_SQL"
    echo "더미 건물·거래를 지웠어요 ($container)"
    exit 0
fi

# 진짜 데이터가 있으면 번호(21번 등)가 겹칠 수 있어서 넣지 않는다
real_count=$(run_sql <<<"SELECT COUNT(*) FROM buildings WHERE apt_nm NOT LIKE '더미 %';" | tail -1)
if [[ "$real_count" != 0 ]]; then
    echo "DB에 진짜 건물 데이터가 ${real_count}곳 있어서 더미를 넣지 않았어요" >&2
    exit 1
fi

run_sql <<SQL
$CLEAR_SQL

INSERT INTO buildings
    (id, building_type, sgg_cd, umd_nm, jibun, apt_nm, build_year, latitude, longitude, coord_source,
     created_at, updated_at)
VALUES
    (21, 'APT',       '11440', '도화동', '901', '더미 도화 1단지',    2004, 37.5401, 126.9452, 'JUSO', NOW(6), NOW(6)),
    (22, 'OFFICETEL', '11440', '도화동', '901', '더미 도화 오피스텔', 2010, 37.5401, 126.9452, 'JUSO', NOW(6), NOW(6)),
    (23, 'APT',       '11440', '마포동', '903', '더미 마포 리버뷰',   2015, 37.5385, 126.9460, 'JUSO', NOW(6), NOW(6)),
    (24, 'APT',       '11440', '공덕동', '904', '더미 공덕 센트럴',   2012, 37.5440, 126.9510, 'JUSO', NOW(6), NOW(6)),
    (25, 'APT',       '11440', '용강동', '905', '더미 용강 하이츠',   1999, 37.5395, 126.9395, 'JUSO', NOW(6), NOW(6)),
    (26, 'APT',       '11440', '대흥동', '906', '더미 대흥 파크',     2008, 37.5500, 126.9420, 'JUSO', NOW(6), NOW(6)),
    (27, 'APT',       '11440', '아현동', '907', '더미 아현 힐스',     2018, 37.5560, 126.9560, 'JUSO', NOW(6), NOW(6));

-- 계약월(deal_ym)은 아래 UPDATE가 계약일로 채운다
INSERT INTO trades
    (building_id, fingerprint, seq, sgg_cd, deal_ym, deal_date, deal_amount, exclu_use_ar, floor, apt_dong,
     cdeal_type, cdeal_day, created_at, updated_at)
VALUES
    (21, 'dummy-21-01', 1, '11440', '', CURDATE() - INTERVAL  13 DAY, 112000,  84.97, 12, '101', NULL, NULL, NOW(6), NOW(6)),
    (21, 'dummy-21-02', 1, '11440', '', CURDATE() - INTERVAL  26 DAY,  85500,  59.98,  7, '102', NULL, NULL, NOW(6), NOW(6)),
    (21, 'dummy-21-03', 1, '11440', '', CURDATE() - INTERVAL  21 DAY,  99000,  84.97,  8, NULL, 'O',
        CURDATE() - INTERVAL 11 DAY, NOW(6), NOW(6)),
    (21, 'dummy-21-04', 1, '11440', '', CURDATE() - INTERVAL  41 DAY, 108000,  84.97,  3, NULL, NULL, NULL, NOW(6), NOW(6)),
    (21, 'dummy-21-05', 1, '11440', '', CURDATE() - INTERVAL  59 DAY,  87000,  59.98, 15, '101', NULL, NULL, NOW(6), NOW(6)),
    (21, 'dummy-21-06', 1, '11440', '', CURDATE() - INTERVAL 100 DAY, 110500,  84.97,  9, '103', NULL, NULL, NOW(6), NOW(6)),
    (21, 'dummy-21-07', 1, '11440', '', CURDATE() - INTERVAL 143 DAY,  83000,  59.98,  4, NULL, NULL, NULL, NOW(6), NOW(6)),
    (21, 'dummy-21-08', 1, '11440', '', CURDATE() - INTERVAL 247 DAY, 105000,  84.97, 11, '102', NULL, NULL, NOW(6), NOW(6)),
    (21, 'dummy-21-09', 1, '11440', '', CURDATE() - INTERVAL 326 DAY,  80000,  59.98,  6, '101', NULL, NULL, NOW(6), NOW(6)),
    (21, 'dummy-21-10', 1, '11440', '', CURDATE() - INTERVAL 517 DAY,  98000,  84.97,  5, NULL, NULL, NULL, NOW(6), NOW(6)),
    (22, 'dummy-22-01', 1, '11440', '', CURDATE() - INTERVAL  36 DAY,  32000,  33.05, 10, NULL, NULL, NULL, NOW(6), NOW(6)),
    (22, 'dummy-22-02', 1, '11440', '', CURDATE() - INTERVAL 119 DAY,  27500,  28.10,  5, NULL, NULL, NULL, NOW(6), NOW(6)),
    (23, 'dummy-23-01', 1, '11440', '', CURDATE() - INTERVAL  39 DAY, 138000,  84.99, 14, '101', NULL, NULL, NOW(6), NOW(6)),
    (23, 'dummy-23-02', 1, '11440', '', CURDATE() - INTERVAL 173 DAY, 101000,  59.94,  8, '102', NULL, NULL, NOW(6), NOW(6)),
    (23, 'dummy-23-03', 1, '11440', '', CURDATE() - INTERVAL 345 DAY, 129000,  84.99,  3, '101', NULL, NULL, NOW(6), NOW(6)),
    (24, 'dummy-24-01', 1, '11440', '', CURDATE() - INTERVAL  53 DAY, 165000, 114.80, 20, '105', NULL, NULL, NOW(6), NOW(6)),
    (24, 'dummy-24-02', 1, '11440', '', CURDATE() - INTERVAL 274 DAY, 132000,  84.60, 10, '103', NULL, NULL, NOW(6), NOW(6)),
    (25, 'dummy-25-01', 1, '11440', '', CURDATE() - INTERVAL 805 DAY,  63000,  59.80,  2, NULL, NULL, NULL, NOW(6), NOW(6)),
    (26, 'dummy-26-01', 1, '11440', '', CURDATE() - INTERVAL  11 DAY, 118000,  84.90,  6, '102', NULL, NULL, NOW(6), NOW(6)),
    (26, 'dummy-26-02', 1, '11440', '', CURDATE() - INTERVAL 221 DAY,  92000,  59.90, 12, '101', NULL, NULL, NOW(6), NOW(6)),
    (27, 'dummy-27-01', 1, '11440', '', CURDATE() - INTERVAL  77 DAY, 152000,  84.98, 17, '104', NULL, NULL, NOW(6), NOW(6)),
    (27, 'dummy-27-02', 1, '11440', '', CURDATE() - INTERVAL 299 DAY, 118500,  59.97,  9, '101', NULL, NULL, NOW(6), NOW(6));

UPDATE trades SET deal_ym = DATE_FORMAT(deal_date, '%Y%m') WHERE fingerprint LIKE 'dummy-%';
SQL

echo "더미 건물 7곳과 거래 22건을 넣었어요 ($container)"
echo "- 21번 '더미 도화 1단지': 거래 10건(해제 1건 포함), 상세 [더보기]와 가격 추이를 볼 수 있어요"
echo "- 25번 '더미 용강 하이츠': 1년 넘은 거래뿐이라 주변 목록에서 최근 거래가 비어 있어요"
echo "- 26·27번은 1km 밖, 3km 안이에요. 테스트 위치: 도화동 lat 37.5404, lon 126.9455"
