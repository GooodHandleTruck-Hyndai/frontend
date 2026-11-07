#!/usr/bin/env bash
# 에뮬레이터 시나리오 테스트: 빌드 → 설치 → 정차 화면 → 위치 이동 → 주행 화면 → 정차 복귀
# 사용: scripts/e2e.sh [--no-build]   (에뮬레이터 Pleos_Connect_v2.1.0이 켜져 있어야 해요)
set -u
cd "$(dirname "$0")/.."

PKG=com.gooodhandletruck.zippick
ACT=$PKG/.MainActivity
USER_ID=10   # 앱은 headless 시스템 사용자 10에서 돌아요
PASS=0; FAIL=0

step() { printf '\n▶ %s\n' "$1"; }
screen_text() {
  sleep 3   # am start 직후 dump는 비어 있을 수 있어서 기다려요
  adb shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1
  adb shell cat /sdcard/u.xml | grep -o 'text="[^"]\+"' | sed 's/^text="//; s/"$//'
}
expect() {  # expect "<화면에 있어야 할 문구>" "<설명>"
  if screen_text | grep -qF -- "$1"; then
    echo "  ✅ $2"; PASS=$((PASS+1))
  else
    echo "  ❌ $2 (\"$1\" 없음)"; FAIL=$((FAIL+1))
    adb exec-out screencap -p > "/tmp/e2e-fail-$PASS-$FAIL.png"
  fi
}
tap_text() {  # 화면에서 문구를 찾아 그 가운데를 눌러요
  sleep 1
  adb shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1
  local xy
  xy=$(adb shell cat /sdcard/u.xml | python3 -I -c '
import re,sys
t=sys.argv[1]
for m in re.finditer(r"<node [^>]*?text=\"([^\"]*)\"[^>]*?bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", sys.stdin.read()):
    if t in m.group(1):
        a=list(map(int,m.groups()[1:])); print((a[0]+a[2])//2,(a[1]+a[3])//2); break
' "$1")
  [ -n "$xy" ] && adb shell input tap $xy || echo "  ⚠️ \"$1\" 버튼을 못 찾았어요"
}
launch() { adb shell am start --user $USER_ID "$@" -n $ACT >/dev/null 2>&1; }

adb get-state >/dev/null 2>&1 || { echo "에뮬레이터가 안 켜져 있어요"; exit 1; }

if [ "${1:-}" != "--no-build" ]; then
  step "빌드·설치"
  ./gradlew assembleDebug -q || { echo "빌드 실패"; exit 1; }
  adb install -r --user $USER_ID app/build/outputs/apk/debug/app-debug.apk | tail -1
fi

step "초기화 (첫 실행 상태 + 위치 권한 허용)"
adb shell pm clear --user $USER_ID $PKG >/dev/null
adb shell pm grant --user $USER_ID $PKG pleos.car.permission.FUSED_LOCATION
adb emu geo fix 127.0311 37.5003 >/dev/null   # 경도가 먼저. mock 단지 c-1001 근처

step "1. 첫 실행 → 온보딩 (S-01 → S-02 → S-03)"
launch -f 0x10008000 --ez debug_driving false
expect "정차 중" "정차 상태로 시작해요"
expect "위치 권한 허용하고 시작하기" "S-01 시작하기 화면이 떠요"
tap_text "위치 권한 허용하고 시작하기"
expect "어떤 단지를 알려 드릴까요?" "S-02 알림 조건 화면이 떠요"
tap_text "이 조건으로 알림 받기"
expect "내 위치에서" "S-03 주변 단지 목록이 떠요"

step "2. 주행 중으로 전환 → 대기 화면 (S-08)"
launch --ez debug_driving true
expect "단지를 지나면 소리로 알려 드릴게요" "S-08 주행 대기 문구가 나와요"
expect "주변 단지 듣기" "큰 버튼이 보여요"
if screen_text | grep -qE '억|만원'; then
  echo "  ❌ 주행 중 화면에 가격이 보이면 안 돼요"; FAIL=$((FAIL+1))
else
  echo "  ✅ 주행 중 화면에 가격이 없어요"; PASS=$((PASS+1))
fi

step "3. 다시 정차 → 목록 복귀"
launch --ez debug_driving false
expect "주변 단지" "S-03으로 돌아와요"

printf '\n결과: 통과 %d · 실패 %d\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ]
