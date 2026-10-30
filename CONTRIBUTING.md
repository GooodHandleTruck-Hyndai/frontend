# 시동 협업 가이드

GooodHandleTruck 팀이 같은 방식으로 일하기 위한 규칙이에요. 짧게 유지하고, 바꾸고 싶은 규칙이 있으면 PR로 제안해 주세요.

---

## 작업 흐름 한눈에 보기

```
이슈 만들기 → 브랜치 따기 → 작업 & 커밋 → PR → 리뷰 1명 승인 → Squash 머지 → 이슈 자동 닫힘
```

1. **이슈부터 만들어요.** 작은 작업이라도 이슈 번호가 있어야 브랜치, 커밋, PR이 서로 연결돼요.
2. **main 에서 브랜치를 따요.**
   ```bash
   git switch main && git pull
   git switch -c feature/#12-speed-alert
   ```
3. **작업하고 커밋해요.** 커밋 규칙은 아래를 보세요.
4. **PR 을 올려요.** 템플릿의 `Closes #12` 에 이슈 번호를 꼭 적어요.
5. **리뷰어 1명이 승인하면 Squash and merge** 로 합쳐요. 머지한 브랜치는 삭제해요.

---

## 브랜치 이름

`종류/#이슈번호-짧은-설명` (영어 소문자, 단어는 하이픈으로 연결)

| 종류 | 언제 | 예시 |
|---|---|---|
| `feature` | 새 기능 | `feature/#12-speed-alert` |
| `fix` | 버그 수정 | `fix/#15-crash-on-start` |
| `refactor` | 동작 변화 없는 코드 개선 | `refactor/#20-split-viewmodel` |
| `chore` | 빌드, 설정, 의존성 | `chore/#3-ktlint-setup` |
| `docs` | 문서 | `docs/#5-readme` |

- `main` 에는 직접 push 하지 않아요. 항상 PR로 들어가요.

---

## 커밋 메시지

```
타입: 무엇을 했는지 (한국어 OK)

필요하면 본문에 이유를 적어요.
```

| 타입 | 뜻 |
|---|---|
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 리팩터링 |
| `style` | 포맷, 세미콜론 등 (동작 변화 없음) |
| `docs` | 문서 |
| `test` | 테스트 |
| `chore` | 빌드, 설정, 의존성 |
| `ci` | GitHub Actions |

예시
```
feat: 과속 시 음성 안내 추가
fix: 속도 권한 거부 시 앱이 멈추는 문제 수정
chore: Pleos Vehicle SDK 2.1.0 으로 업데이트
```

> PR은 Squash 머지라서, **PR 제목**이 main 에 남는 최종 커밋 메시지가 돼요. PR 제목도 같은 규칙으로 써 주세요.

---

## 이슈

- 템플릿(기능 요청 / 버그 리포트 / 작업)을 골라서 써요.
- 라벨: 종류(`feat`, `bug`, …)와 영역(`android`, `backend`)을 하나씩 붙여요.
- 담당자(Assignee)를 꼭 지정해요. 담당자가 없는 이슈는 아무도 안 하는 이슈예요.
- 하루 넘게 걸릴 것 같으면 이슈를 더 잘게 쪼개요.

---

## PR 과 리뷰

- PR 하나에는 이슈 하나만 담아요. 가능하면 변경 400줄 이하로 유지해요.
- 화면이 바뀌면 스크린샷을 꼭 붙여요.
- 리뷰 코멘트 앞에 말머리를 붙이면 의도가 명확해져요.
  - `[필수]` 머지 전에 꼭 고쳐야 함
  - `[제안]` 고치면 좋지만 작성자 판단
  - `[질문]` 궁금해서 묻는 것
- 리뷰 요청은 24시간 안에 봐 주기로 해요.

---

## 코드 스타일

- `.editorconfig` 와 ktlint 가 스타일을 정해요. 사람끼리 포맷으로 다투지 않기로 해요.
- 커밋 전에 한 번:
  ```bash
  ./gradlew ktlintFormat   # 자동 정리
  ./gradlew ktlintCheck    # 검사 (CI에서도 이걸 돌려요)
  ```
  Windows 명령 프롬프트에서는 `gradlew.bat ktlintFormat` 이에요.
- 패키지 구조
  ```
  com.gooodhandletruck.sidong
  ├── ui/     화면(Compose)과 ViewModel
  └── data/   차량 데이터, 서버 API, 저장소
  ```

---

## 절대 커밋하면 안 되는 것

- `local.properties` (각자 PC의 SDK 경로)
- 서명 키 (`*.jks`, `*.keystore`)
- API 키, Client Secret 같은 비밀값. 필요하면 `local.properties` 에 넣고 코드에서 읽어요.

`.gitignore` 에 이미 막혀 있지만, `git add .` 하기 전에 `git status` 로 한 번 확인해 주세요.
