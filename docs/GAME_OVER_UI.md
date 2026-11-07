# 게임 오버 픽셀 아트 UI — STEP 10-8

## 구조와 실제 데이터

`GameState.GAME_OVER`를 그대로 사용한다. 마지막 목숨 소진 조건과 사망 처리 시점은 변경하지 않았다.
`Main.applyDeath()`는 목숨·사망 카운터를 갱신하고, 목숨이 0이면 현재 학기를 재시작하지 않는다.
따라서 마지막 시도에서 수집한 점수·아이템 수와 사망 횟수가 그대로 남는다.

`Main.renderStateUi()`가 `GameOverRenderer`에 아래 값을 직접 전달한다.

| 표시 | 원본 |
| --- | --- |
| YEAR / SEMESTER | 현재 StageDefinition.getYear/getSemester |
| SCORE | stageScore: 마지막 시도의 아이템 점수, 생존 보너스 제외 |
| ITEMS | collectedItemCount / gradeItems.size |
| SEMESTER DEATHS | semesterDeathCount: 마지막 사망까지 포함 |

이 화면을 위해 StageResult를 새로 만들거나 미완료 학기 결과를 확정하지 않는다.
사망 횟수는 고정값 5가 아니며, 방학 효과의 추가 목숨으로 더 많이 사망하면 그 실제 값을 표시한다.
전체 누적 사망 횟수와 이전 학기 기록은 기존 규칙대로 메뉴 선택 전까지 보존된다.

## 디자인

- 전체 창에 alpha 0.60 어두운 오버레이. 기존 FitViewport 바깥 여백도 함께 처리한다.
- 중앙 크림 패널 `#F5E8CB`, 테두리 `#594738`, 글씨 `#493B32`.
- GAME OVER 제목에 파스텔 분홍 `#E991A5`, 선택 행에 파스텔 노랑 `#EBCB6E` 강조.
- 기존 일시정지 메뉴와 동일한 폭 360, 테두리 3, 그림자 4, 제목 폰트 배율 2, 메뉴 배율 1.3, 행 간격 46.
- 결과 정보를 포함하므로 높이는 392로 확장한다. 가용 UI 크기를 상한으로 계산하고 중앙에 배치한다.
- 기존 UI FitViewport를 통해 800×600 / 1280×720 / 1920×1080에서 같은 비율로 확대한다.
- 기존 HUD는 수정하지 않고 뒤에서 어두워진 상태로 유지한다. 중앙 패널과 겹치지 않는다.
- GAME_OVER 월드의 기존 hurt 스프라이트 표시를 유지한다.

## 메뉴와 입력

| 메뉴 | 동작 |
| --- | --- |
| RESTART (기본 선택) | 기존 startNewGame()으로 전체 기록을 초기화하고 Stage 1 PLAYING 시작 |
| TITLE | 같은 전체 초기화를 실행한 후 TITLE_SCREEN에서 대기 |

어떤 학기에서 실패하든 RESTART는 실패 학기로 돌아가지 않는다.
두 메뉴 모두 StageResult·점수·생존 보너스·학기별/전체 사망 횟수·방학 기록을 초기화하고
Stage 1의 5목숨·0점·0수집량과 초기 월드 상태를 복원한다.

- UP/DOWN의 `isKeyJustPressed`로 두 항목을 순환한다. 키 유지로 연속 이동하지 않는다.
- 방향키와 Enter가 동시에 입력되면 이동만 처리한다. UP/DOWN 동시 입력은 서로 상쇄된다.
- Enter로 선택을 실행한다. 기본 선택의 Enter는 기존 즉시 새 게임 동작을 유지한다.
- TITLE에는 기존 키 해제 대기를 적용하여 누른 Enter가 타이틀을 바로 통과하지 않게 한다.
- GAME_OVER 입력 분기에서 반환하므로 물리·적·함정·발판·아이템·월드 시간은 진행하지 않는다.
- ESC로 게임을 재개할 수 없다. 기존 GAME_OVER 규칙을 유지한다.

## 리소스

전용 폰트와 공유 1×1 Texture를 한 번만 생성하고 Nearest 필터를 적용한다.
프레임마다 Texture를 생성하지 않는다. Main.dispose()에서 함께 해제한다.
배경 셰이더와 독립적으로 렌더링하고, projection/transform·셰이더·색상·블렌딩 상태를 복원한다.
원본 PNG, HUD, PauseMenuRenderer, 플레이어 물리와 충돌, 배경 및 월드 그래픽은 변경하지 않았다.

## 자동 및 GUI 검증 (2026-11-07)

- 최종 `./gradlew.bat build`: 종료 코드 0.
- 신규 GameOverVerification: 30/60/120fps × 8학기 × RESTART/TITLE 모두 통과.
- 실제 아이템 수집 판정 및 다섯 번의 낙사 처리로 마지막 시도의 5점·1개 수집·5회 사망 유지 확인.
- 동적으로 표시하는 학년/학기·수집 분모·점수·사망 문자열의 실제 데이터 일치 확인.
- GAME_OVER에서 이동·점프 키를 유지해도 플레이어·적·발판·함정 시간·아이템·카운터가 변하지 않음.
- 화살표 edge 입력·길게 누르기, RESTART의 전체 초기화, TITLE 초기화와 Enter 해제 후 재시작 확인.
- 기존 8학기·목숨·마지막 목숨 즉시 GAME_OVER·피격·일시정지 메뉴·방학 미니게임·성적표·GPA·엔딩의 30/60/120fps 회귀 검증 통과.
- 실제 LWJGL GUI에서 8학기 × 3해상도의 기존 화면 24장 및 신규 화면 24장 캡처.
- 신규 화면의 TITLE 선택 캡처 24장, 짝수 학기에서 타이틀 복귀 캡처 12장 추가.
- 세 해상도 대표 캡처 직접 확인: 결과 수치 정상, 중앙 정렬·선택 강조 정상, 글씨 잘림과 HUD 겹침 없음.
- GUI에서도 기존 공통 사망 처리와 수집 처리로 실제 결과 값을 생성했다. GUI 장면 준비는 진단용 좌표 fixture이며,
  실제 입력 기반 8학기 클리어·충돌 경로의 보장은 기존 회귀 검증이 담당한다.
- GUI에서 메뉴 입력에 따른 RESTART/TITLE, 월드 정지, 배치 상태 복원, Nearest, dispose 후 폰트·Texture GL handle 0 확인.

## 캡처와 진단 코드

모든 자료는 Git 추적 대상이 아닌 `build/game-over-gui/`에 있다.

- [800×600 전후 비교](../build/game-over-gui/before-after-800x600.png) — 왼쪽 기존, 오른쪽 신규.
- [800×600 신규](../build/game-over-gui/after-stage1-800x600.png)
- [1280×720 신규](../build/game-over-gui/after-stage4-1280x720.png)
- [1920×1080 TITLE 선택](../build/game-over-gui/after-title-selected-stage8-1920x1080.png)
- [Stage 8에서 TITLE 복귀](../build/game-over-gui/after-title-from-stage8-800x600.png)
- GUI 진단: `build/game-over-gui/GameOverGuiSmoke.java`.

## 변경 파일

- Main.java: 게임 오버 렌더러·입력 연결, 기존 전체 재시작 재사용, 구형 게임 오버 텍스트 분기 제거.
- GameOverRenderer.java: 결과 표시와 픽셀 패널·메뉴 렌더링.
- GameOverVerification.java: 8학기 결과·정지·메뉴·전체 초기화 검증.
- FreshmanLevelVerification.java: 기존 build 검증 체인 연결.
- docs/GAME_OVER_UI.md: 동작과 검증 기록.

확인 범위에서 남은 GUI 문제는 없다. Git commit/push/merge는 실행하지 않았다.
