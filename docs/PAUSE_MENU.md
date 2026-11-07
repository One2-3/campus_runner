# 픽셀 아트 일시정지 메뉴 — STEP 10-7

## 기존 구조와 변경 범위

기존 `Main.update()`는 PLAYING에서 ESC를 누르면 PAUSED로 전환하고, PAUSED에서는
월드 업데이트를 실행하지 않았다. 화면에는 PAUSED와 ESC 안내만 표시했다.
`restartStage()`와 전체 초기화 `startNewGame()`은 이미 있었지만 타이틀 복귀 기능은 없었다.

새 GameState 없이 기존 PAUSED를 사용한다. `Main`은 선택 인덱스와 입력·행동을 관리하고,
`PauseMenuRenderer`는 화면만 그린다. 픽셀 HUD, 월드 렌더링, 게임 오버·성적표·졸업 엔딩 UI는 변경하지 않았다.

## 메뉴와 초기화 규칙

| 메뉴/키 | 처리 |
| --- | --- |
| RESUME / ESC | 현재 플레이어·월드 상태를 보존하고 PLAYING으로 복귀 |
| RESTART | 기존 restartStage()로 현재 학기 시작 위치·아이템·점수·적·발판·함정을 복원 |
| QUIT | 기존 startNewGame()으로 모든 기록을 초기화한 뒤 TITLE_SCREEN으로 전환. 애플리케이션 종료 없음 |

RESTART는 목숨·학기별/전체 사망 횟수·이전 학기 결과·현재 방학 효과를 유지한다.
이 메뉴 동작은 사망이 아니므로 목숨이나 사망 횟수를 차감/추가하지 않는다.
현재 학기 아이템 점수·수집 상태만 기존 재시작 함수의 규칙대로 초기화된다.
QUIT는 Stage 1을 초기화하고 5목숨·0점·0사망, 빈 StageResult와 방학 기록으로 타이틀에 머문다.

## 입력과 정지

- ESC로 진입할 때 RESUME을 기본 선택한다.
- UP/DOWN의 `isKeyJustPressed`로 한 칸씩 이동하며 처음/끝에서 순환한다.
- Enter로 선택 실행, ESC는 선택 위치와 관계없이 해제한다.
- UP/DOWN 동시 입력은 이동하지 않으며, 이동과 Enter가 동시에 들어오면 이동만 처리한다.
- PAUSED 분기에서 즉시 반환하므로 플레이어 물리·적·이동 발판·함정 시간·아이템 획득·클리어 판정이 실행되지 않는다.
- RESUME한 입력 프레임에서도 월드 시간을 진행하지 않는다.
- RESTART/QUIT 후 기존 키 해제 대기 로직을 재사용한다. 누른 Enter가 재시작을 반복하거나 타이틀을 곧바로 통과하지 않는다.

## 렌더링

- 화면 전체에 어두운 색 alpha 0.60 오버레이를 적용한다. FitViewport 바깥 여백도 함께 어두워진다.
- 크림 패널 `#F5E8CB`, 테두리 `#594738`, 글씨 `#493B32`, 파스텔 노랑 `#EBCB6E` 선택 강조.
- 기본 UI 논리 크기 360×304, 화면 중앙 배치. 기존 FitViewport가 실제 해상도에 맞춰 동일 비율로 확대한다.
- 패널 크기는 전달된 UI 영역의 가용 너비·높이를 상한으로 계산한다.
- 외곽선·선택 화살표는 SpriteBatch와 공통 1×1 Texture로 표시한다. 별도 이미지 다운로드 없음.
- 전용 BitmapFont와 Texture는 초기화 시 한 번 생성하고 Nearest 필터를 적용한다.
- 종료 시 폰트와 Texture를 해제한다. 배치의 projection/transform, 색상, 셰이더, RGB/alpha 블렌딩 설정을 복원한다.
- 기존 HUD는 그대로 그린 뒤 오버레이로 어둡게 처리한다. 중앙 메뉴와 HUD는 겹치지 않는다.

## 검증 결과 (2026-11-07)

- 최종 `./gradlew.bat build`: 종료 코드 0.
- 신규 PauseMenuVerification: 30/60/120fps 모두 통과.
- 실제 Main 입력 경로로 ESC 진입/해제, RESUME 선택, UP/DOWN 순환, 길게 누르기, 이동+Enter 동시 입력 검증.
- 3초 일시정지 동안 플레이어·적·이동 발판·함정 상태/경과 시간·아이템·점수·목숨·카메라 상태 동일.
- RESTART: 같은 Stage 7, 4목숨, 학기/전체 사망 1 유지, 현재 점수/아이템 0, 초기 위치 복원, 이전 학기 결과 유지.
- 해커톤 성공 효과가 있는 fixture에서 RESTART 후 동일 효과 보존 및 QUIT 후 방학 기록/효과 초기화 확인.
- QUIT: 타이틀 복귀, 모든 학기 결과·점수·사망 기록 초기화, Enter 유지 중 타이틀 정지, 키 해제 후 새 Enter로 시작.
- 기존 8학기·목숨·피격·방학 미니게임·성적표·GPA·엔딩의 30/60/120fps 회귀 검증 통과.
- 실제 LWJGL GUI: 800×600 / 1280×720 / 1920×1080에서 메뉴 선택 3종 및 타이틀 복귀 캡처 총 12장 생성.
- 세 해상도 대표 캡처 직접 확인: 중앙 배치·글씨·선택 영역 정상, HUD와 겹침 없음, 화면 전체 오버레이 정상.
- GUI 입력으로 ESC 복귀, Enter 재시작, QUIT 타이틀 복귀와 월드 정지 확인.
- GUI에서 배치 projection/transform·색상·블렌딩 상태 복원, Nearest 필터, 종료 후 Texture/font GL handle 0 확인.

GUI 진단 코드와 캡처는 Git 추적 대상이 아닌 `build/pause-menu-gui/`에 있다.

## GUI 캡처

- [800×600 RESUME](../build/pause-menu-gui/resume-800x600.png)
- [1280×720 RESTART](../build/pause-menu-gui/restart-1280x720.png)
- [1920×1080 QUIT](../build/pause-menu-gui/quit-1920x1080.png)
- [QUIT 후 타이틀](../build/pause-menu-gui/title-800x600.png)

## 변경 파일

- Main.java: PAUSED 입력과 메뉴 렌더러 연결, 기존 재시작/전체 초기화 재사용.
- PauseMenuRenderer.java: 오버레이·중앙 패널·선택 강조·리소스 관리.
- PauseMenuVerification.java: 상태·입력·초기화 회귀 검증.
- FreshmanLevelVerification.java: 기존 build 검증 체인에 추가.
- docs/PAUSE_MENU.md: 동작과 결과 기록.

발견된 GUI 배치 문제는 없다. PNG·스테이지·HUD·플레이어 물리·점수/목숨 규칙은 수정하지 않았다.
Git commit/push/merge는 실행하지 않았다.
