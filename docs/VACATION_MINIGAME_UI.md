# 방학 미니게임 개별 픽셀 테마 — STEP 10-11

## ESC 일시정지와 카페 클리핑 (2026-11-08)

`feature/vacation-minigame-ui` 브랜치에서 작업했다. 기존 한국어·테마·미니게임 규칙은 유지했다.

### 정지 상태와 재개

- 기존 GameState.PAUSED와 PauseMenuRenderer를 그대로 재사용한다. 메뉴 디자인은 바꾸지 않았다.
- Main.pauseReturnState가 일반 스테이지 PLAYING과 VACATION_EVENT를 구분한다.
- 미니게임 INTRO/PLAYING/RESULT 모두 ESC 새 입력으로 PAUSED에 들어간다.
- PAUSED에서는 원래 미니게임 화면 위에 기존 메뉴를 그린다. 스테이지 화면으로 바뀌지 않는다.
- PAUSED 입력 분기는 즉시 반환하므로 미니게임 타이머·트레이·낙하물·암기/퍼즐 진행·입력 판정이 정지한다.
- ESC/RESUME은 같은 vacationGame 객체와 원래 Phase로 돌아간다. 재개 프레임에는 모델을 진행하지 않고 다음 활성 프레임부터 이어 간다.
- isKeyJustPressed를 사용하므로 누른 ESC가 정지/재개를 반복하지 않는다.
- 일반 스테이지의 기존 일시정지/재개/재시작 규칙은 유지한다.

### RESTART / QUIT

| 메뉴 | 미니게임에서 동작 |
| --- | --- |
| RESUME / ESC | 같은 객체·진행·남은 시간으로 VACATION_EVENT 재개 |
| RESTART | 같은 미니게임을 새 INTRO로 시작. 해당 미니게임 점수/입력/퍼즐/타이머만 새로 생성 |
| QUIT | 기존 전체 새 게임 초기화 후 TITLE_SCREEN. 애플리케이션 종료 없음 |

RESTART는 기존 VacationMinigame.create(pendingType)를 사용한다.
VacationManager.beginAfterStage/complete를 다시 호출하지 않으므로 이미 저장된 StageResult,
학기 점수·사망/목숨 기록·방학 대기 상태를 건드리지 않는다. 결과 화면도 Enter 확정 전이므로 재시도가 가능하다.
버프/디버프는 재시작할 때 적용하지 않으며 새 게임을 실제 완료하고 결과 Enter로 확인할 때 한 번만 확정된다.
미니게임 정지 중 기존 completion callback도 상태 검사로 거부된다.
RESTART/QUIT 뒤에는 기존 키 해제 대기로 누른 Enter의 중복 실행을 방지한다.

### 카페 낙하물 클리핑

큰 플레이 패널은 UI 좌표 (28,60), 크기 744×416이다.
2단위 테두리와 아래쪽 카운터 장식을 제외한 **(30,65)~(770,474)**,
너비 740·높이 409를 커피/쓰레기의 렌더링 영역으로 정했다. 기존 트레이 전체는 이 영역 안에 있다.

1. 패널·카운터·트레이를 그린 후 ShapeRenderer.flush()한다.
2. 현재 UI viewport의 calculateScissors()로 UI 영역을 화면 좌표로 변환한다.
3. ScissorStack.pushScissors() 후 커피·쓰레기 도형만 그린다.
4. finally에서 flush() 후 popScissors()한다. 기존 부모 scissor가 있으면 그 상태로 돌아간다.
5. 안내 카드·HUD·글씨는 클리핑 밖에서 기존대로 그린다.

아이템 전체가 아니라 각 픽셀을 자르므로 위/아래/왼쪽/오른쪽으로 일부 걸친 그림도 영역 밖으로 나오지 않는다.
아이템이 영역 아래에 있어도 모델에서 삭제하지 않는다. 기존 y+height<0 제거, 쟁반 충돌,
커피 점수/쓰레기 감점·낙하 속도·생성 간격·제한 시간은 변경하지 않았다.

### 검증 결과

- 최종 `.\gradlew.bat build`: 종료 코드 0. 기존 8학기·미니게임·효과·점수/GPA·엔딩의 30/60/120fps 회귀 검증 통과.
- 신규 VacationPauseVerification: 3게임 × INTRO/플레이/성공/실패 × 30/60/120fps.
- ESC 길게 유지, 이동/점프 입력 중 2초 정지, 모델/타이머 불변, ESC/RESUME의 같은 객체 보존 확인.
- 재개 후 실제 Main.update()에서 타이머가 다시 줄어듦 확인. 일반 PLAYING으로 잘못 돌아가지 않음.
- RESTART의 새 INTRO/제한 시간 복원, 기존 학기 결과/방학 대기 보존, 정지 중 보상 확정 거부,
  누른 Enter 중복 방지, 다시 실제 완료한 뒤 효과 한 번 확정, QUIT 타이틀 전체 초기화 확인.
- 실제 LWJGL GUI에서 800×600 / 1280×720 / 1920×1080의 세 게임 정지 화면과 메뉴 동작 확인.
- 진단용 커피/쓰레기를 네 경계에 일부 걸치게 배치하고, 완전히 밖에 있는 경우도 포함했다.
  실제 생성 범위 밖의 좌표는 **진단 fixture에만** 사용하며 게임 모델은 수정하지 않았다.
- 아이템 없는 화면과 비교해 바뀐 아이템 픽셀은 800×600에서 1,578개, 1280×720에서 2,278개,
  1920×1080에서 5,117개이며 모두 화면으로 변환된 내부 사각형에만 존재했다.
  네 경계의 일부 그림은 내부에 남고, 테두리/트레이/HUD 영역의 픽셀은 변하지 않았다.
- ScissorStack이 원래 비었을 때 렌더링 후 비어 있고 GL_SCISSOR_TEST가 꺼짐 확인.
  부모 scissor를 push한 상태에서도 원래 부모가 유지됨을 확인했다.
- 종료 시 폰트 Texture handle 0, 남은 scissor 없음 확인. 새 GPU 리소스/외부 라이브러리 추가 없음.

### 캡처와 변경 파일

- [카페 ESC 800×600](../build/vacation-pause-clip-gui/after-paused-cafe-800x600.png)
- [자격증 ESC 1280×720](../build/vacation-pause-clip-gui/after-paused-certification-1280x720.png)
- [서버 ESC 1920×1080](../build/vacation-pause-clip-gui/after-paused-hackathon-1920x1080.png)
- [카페 네 경계 클리핑 전후](../build/vacation-pause-clip-gui/cafe-clip-before-after.png) — 왼쪽 이전, 오른쪽 현재.
- [클리핑 800×600](../build/vacation-pause-clip-gui/after-cafe-all-edges-800x600.png)
- [클리핑 1280×720](../build/vacation-pause-clip-gui/after-cafe-all-edges-1280x720.png)
- [클리핑 1920×1080](../build/vacation-pause-clip-gui/after-cafe-all-edges-1920x1080.png)

진단 코드: build/vacation-pause-clip-gui/VacationPauseClipGuiSmoke.java.
이번 수정 파일은 Main.java, VacationMinigameRenderer.java, VacationPauseVerification.java,
FreshmanLevelVerification.java 및 이 문서다. 기존 미커밋 UI/폰트 작업은 유지했다.
확인 범위에서 남은 문제는 없다. Git commit/push/merge는 실행하지 않았다.

## 한국어 적용 및 자격증 결과 장식 수정 (2026-11-08)

세 미니게임의 제목·부제·설명·목표·조작·통계·진행·성공/실패·결과·효과·다음 학년 안내를 한국어로 바꿨다.
실제 키 이름 Enter/Space는 조작 식별을 위해 유지한다. 코드의 클래스/변수/enum/리소스 이름은 번역하지 않았다.
카페는 ‘카페 아르바이트 / 아르바이트 정산표’, 자격증은 ‘자격증 공부 / 시험 결과표’,
서버는 ‘서버 연결 / 서버 연결 보고서’로 표시한다.
기존 세 팔레트와 패널 좌표/정보 구조는 유지하고 한글 폭에 맞춰 텍스트 배율과 최대 폭만 조정했다.

### 한글 폰트

기존 ending-ko BitmapFont는 엔딩 문구만 포함한 부분집합이라 전체 미니게임 한국어 문자를 표시할 수 없다.
기존 리소스를 수정하지 않고 **나눔고딕 Bold 기반 Campus Vacation Korean** BitmapFont를 미니게임에만 적용했다.

- 원본은 Google Fonts의 NanumGothic-Bold.ttf, 라이선스는 [SIL OFL 1.1](https://raw.githubusercontent.com/google/fonts/main/ofl/nanumgothic/OFL.txt).
- 원본 사용 방식과 라이선스를 적용 전에 보고했다. Reserved Font Name을 피하도록 변환한 폰트 이름을 별도로 정했다.
- 소스 TTF와 저작권/라이선스는 tools/fonts에 보관하고 게임에는 subset PNG/fnt와 라이선스 전문만 포함한다.
- assets/fonts/vacation-ko.fnt, vacation-ko.png, vacation-ko-OFL.txt를 추가했다.
- 기존 엔딩용 fnt/PNG, 다른 게임 PNG, 기존 UI 폰트는 수정하지 않았다.
- 24px 굵은 글꼴, ASCII와 한국어 등 총 **268글리프**, atlas **512×612**. 공백 advance는 8로 설정했다.
- 일반 굵기 시제품에서 Nearest 축소 시 얇은 획이 약해 보여 같은 가족의 Bold로 보정했다.
- Nearest를 유지하며 패널별 최대 텍스트 폭(통계 212, 카드 54 등)에 맞춰 넘칠 때만 글씨를 더 축소한다.
- 재생성: `tools/GenerateVacationFont.ps1`. 글리프를 UI 소스 문자열에서 추출한다.
- 새 FreeType/외부 Java 라이브러리·실행 중 다운로드는 없다. 폰트 소유권/해제는 기존 미니게임 렌더러의 방식 그대로다.

라이선스와 원본 해시는 [tools/fonts/README.md](../tools/fonts/README.md)에 기록했다.

### 자격증 결과 세로선

`VacationMinigameRenderer.sheet()`의 STUDY 분기에서 `(112,120,2,380)` 노트 여백선은
**INTRO일 때만** 그리도록 제한했다. RESULT의 성공/실패 모두 같은 규칙을 적용한다.
큰 결과 패널 외곽선, 내부 보상 카드 테두리, 가로줄과 정보 배치는 유지했다.
카페/서버 장식이나 자격증 플레이 화면은 이 장식 수정의 영향을 받지 않는다.

### 검증

- 최종 `.\gradlew.bat build` 종료 코드 0. 기존 30/60/120fps 통합 회귀 검증 통과.
- 신규 VacationKoreanVerification: 모든 UI 리터럴 글리프, 공백 폭, atlas/라이선스 포함, 별도 폰트 이름 확인.
- 원본 TTF cmap을 추가 검사하여 실제 UI 리터럴의 문자 207개가 전부 존재함을 확인했다. 미지원 문자의 대체 사각형 사용 없음.
- VacationThemeVerification의 결과 요약 예상값을 한국어로 갱신하고 실제 데이터·테마·대비 검증은 유지했다.
- 보상 표시는 기존 PlayerModifier.fromVacation()의 실제 값에서 읽어 한국어로 표시한다.
  카페 목숨 +1/이동 -5%, 자격증 점프 +10%/-5%, 서버 이동 +10%/-5% 정확성 검증 통과.
- 게임 모델/키 처리/제한 시간/판정/전환은 수정하지 않았다. 실제 성공/실패 입력·다음 학년 효과 적용의 기존 검증 통과.
- 실제 GUI: 세 미니게임 × 시작/플레이/성공/실패/추가 상세 상태 × 3해상도, 전후 각각 45장 캡처.
- 800×600 대표 화면에서 굵은 한국어와 전체 문구를 확인했고, 1280×720/1920×1080에서도 잘림·겹침을 확인했다.
- 자격증 성공/실패 × 3해상도 픽셀 비교:
  이전 내부 세로선 위치의 파랑이 흰색으로 바뀌었으며, 큰 외곽선/보상 카드 테두리/가로줄 픽셀은 동일하다.
  INTRO의 내부 세로선도 그대로 남아 있음을 확인했다.
- GUI에서 테마 배경색, 모델/타이머 불변, 배치/ShapeRenderer 상태 복원, Nearest, 폰트 해제 확인.

### 한국어 캡처

다음 모음의 왼쪽 위는 시작, 오른쪽 위는 플레이, 왼쪽 아래는 성공, 오른쪽 아래는 실패다.

- [카페 한국어 시작·플레이·성공·실패](../build/vacation-korean-gui/cafe-korean-screens.png)
- [자격증 한국어 시작·플레이·성공·실패](../build/vacation-korean-gui/certification-korean-screens.png)
- [서버 한국어 시작·플레이·성공·실패](../build/vacation-korean-gui/hackathon-korean-screens.png)
- [자격증 결과 전후 비교](../build/vacation-korean-gui/study-result-before-after.png) — 왼쪽 이전, 오른쪽 현재.
- [자격증 성공 800×600](../build/vacation-korean-gui/after-certification-success-800x600.png)
- [자격증 실패 800×600](../build/vacation-korean-gui/after-certification-failure-800x600.png)
- [자격증 실패 1280×720](../build/vacation-korean-gui/after-certification-failure-1280x720.png)
- [서버 결과 1920×1080](../build/vacation-korean-gui/after-hackathon-failure-1920x1080.png)

진단 코드: build/vacation-korean-gui/VacationThemeGuiSmoke.java 및 verify_font_and_line.py.
요청한 범위에서 남은 글리프/배치 문제는 발견되지 않았다.

이번 한국어 작업의 변경 파일:

- VacationMinigameRenderer.java, VacationUiTheme.java: 한국어 표시·폰트·폭 보정 및 결과 세로선 조건.
- VacationKoreanVerification.java, VacationThemeVerification.java, FreshmanLevelVerification.java: 한국어 검증 및 연결.
- assets/fonts/vacation-ko.fnt, vacation-ko.png, vacation-ko-OFL.txt: 전용 BitmapFont/라이선스.
- tools/GenerateVacationFont.ps1, tools/fonts/NanumGothic-Bold.ttf, OFL.txt, README.md: 재생성·출처·라이선스.
- docs/VACATION_MINIGAME_UI.md: 현재 동작과 결과 기록.

Main의 입력/전환, 미니게임 모델, 버프/디버프 규칙, 기존 색상값은 한국어 작업에서 수정하지 않았다.
아래 STEP 10-11 원래 영어 명칭과 캡처는 이전 구현 기록이다. Git commit/push/merge는 실행하지 않았다.

## 기존 구조와 작업 범위

세 미니게임 모두 기존 `VacationMinigame.Phase.INTRO / PLAYING / RESULT`를 사용한다.
새 상태·선택 메뉴·판정·효과는 추가하지 않았다.
Stage 2/4/6의 학기 결과 저장 → STAGE_CLEAR → Enter → 기존 방학 INTRO 흐름을 유지한다.
결과 화면의 Enter가 기존 `completeVacationEvent()`를 호출하고 다음 학년 효과를 확정한다.

운영 코드에서 변경한 곳은 렌더러·색상 정의와 Main의 렌더러 생성/해제 연결뿐이다.
CafeMinigame, CertificationMinigame, HackathonMinigame, CircuitBoard, VacationManager,
PlayerModifier, Main.update()와 점수/GPA/엔딩 모델은 수정하지 않았다.

## 세 가지 독립 테마

| 미니게임 | 배경 | 테두리/주요 글씨 | 보조 | 강조 | 특징 |
| --- | --- | --- | --- | --- | --- |
| 카페 | #FFF0DB | #704B38 | #C98A53 | #E9B86C | 메뉴판 점수/시간 카드, 영수증 절취선, SHIFT SETTLEMENT 정산표 |
| 자격증 | #F5F8FF | #355B8C | #82A9D9 | #76B9AD | 시험지 줄과 여백선, 암기/입력 카드, 라운드 진행 표시, EXAM RESULT SHEET |
| 서버 | #18263C | 테두리 #65E0C0 / 글씨 #E6F4F5 | #A4DCE8 | #65E0C0 | 패널 #263E54, 네트워크 그리드, 상태 콘솔, CONNECTION REPORT |

카페 패널은 따뜻한 #FFF8ED, 자격증 패널은 흰색을 사용한다.
다른 게임 UI의 공통 크림색을 세 미니게임에 일괄 적용하지 않는다.
`VacationUiTheme`가 색상·제목·결과표 이름·실패 강조색을 소유한다.
모든 INTRO/PLAYING/RESULT 단계가 해당 미니게임의 같은 테마를 참조한다.

공통 요소는 사각 패널, 2단위 테두리/그림자, 글씨 크기·여백·통계 카드 구조다.
Enter 안내 카드는 양쪽 색상 띠로 강조한다. 스터디의 민트 위에 글씨를 올리면 대비가 낮으므로
글씨는 밝은 패널 위에 유지하고 강조색을 띠에만 적용했다.
암기 입력 완료 카드도 같은 원칙으로 흰색 바탕+민트 완료선을 사용한다.
서버 실패 제목에는 어두운 배경에서 읽기 쉬운 밝은 분홍을 사용한다.

## 플레이 표시와 유지한 규칙

### 카페 아르바이트

- 제목, 실제 SCORE, TIME, 기존 TARGET=100을 메뉴판 카드로 표시한다.
- 플레이 영역·상단 통계·하단 좌우 이동 안내 카드를 분리한다.
- 기존 트레이 위치/너비/높이와 낙하 아이템 좌표·컵/회색 쓰레기 구분을 유지한다.
- 좌우 방향키, 커피 +10, 쓰레기 -10(최소 0), 놓친 커피 점수 유지, 30초 종료 시 100점 이상 성공 규칙을 유지한다.
- 성공: 다음 2학년 두 학기 시작 목숨 +1. 실패: 이동 속도 -5%.

### 자격증 암기

- 실제 ROUND/TIME/MISTAKES, OBSERVE/RECALL 상태, 표시 남은 시간 또는 맞춘 입력 개수를 표시한다.
- 암기 카드와 조작/진행 로그 카드를 분리한다.
- 공개 중에는 원래 방향을 계단형 화살표로 표시한다. 숨겨진 동안에는 `?`, 이미 맞춘 입력은 `OK`와 완료선으로 구분한다.
- 방향키 단일 새 입력, 패턴 2/3/3/4/5, 공개 시간, 오답 재시도, 40초·3오답 규칙은 유지한다.
- 성공: 다음 3학년 두 학기 점프력 +10%. 실패: 점프력 -5%.

### 서버 연결

- 실제 PUZZLE/TIME/MISTAKES와 마지막 검사 결과·도달 노드 수를 표시한다.
- 퍼즐 영역과 상단 정보·하단 조작 카드를 분리한다.
- PC는 아이스 블루, 서버는 앰버, 마지막 검사에서 도달한 일반 노드는 민트,
  미도달 일반 노드는 저채도 청회색이다. 커서는 아이스 블루 테두리로 표시한다.
- 상태는 기존 `getLastCheck()`에서만 읽으며 렌더링 중 연결 검사나 퍼즐 회전을 실행하지 않는다.
- 방향키 이동, Space 90도 회전, Enter 연결 검사, 상호 포트 연결 판정, 3퍼즐·45초·3오검사 규칙을 유지한다.
- 성공: 다음 4학년 두 학기 이동 속도 +10%. 실패: 이동 속도 -5%.

## 시작·결과 화면

INTRO는 기존 목표·조작키·실패 조건과 다음 학년 성공/실패 효과를 표시한다.
RESULT는 모델의 실제 성공/실패와 점수/완료 라운드/퍼즐 개수, 남은 시간,
적용할 효과와 대상 학년을 표시한다. 미니게임 점수가 학기 점수에 더해지지 않는다는 안내를 유지했다.
결과 값과 효과는 모델 getter와 기존 VacationEventType에서 읽는다.
화면을 그린다고 결과를 확정하거나 다음 학기로 이동하지 않는다.

## 렌더링과 리소스

기존 800×600 UI FitViewport를 사용한다. 창 크기 변경 시 동일 비율로 확대하며
화면 전체를 테마 배경색으로 지워 가로 여백도 해당 테마 색을 유지한다.
새 이미지 다운로드/생성이나 원본 PNG 수정은 하지 않았다.
패널·아이콘은 기존 ShapeRenderer의 사각형/단순 도형, 글씨는 전용 BitmapFont로 렌더링한다.
전용 폰트 atlas를 초기화 시 한 번 생성하고 Nearest로 설정한다. 프레임마다 Texture를 생성하지 않는다.
Main.dispose()에서 폰트를 해제하고, 빌린 SpriteBatch/ShapeRenderer는 Main이 기존대로 해제한다.

렌더링 후 SpriteBatch의 projection/transform·셰이더·색상·블렌딩과
ShapeRenderer의 projection/transform·색상을 복원한다. Main의 기존 폰트를 변경하지 않는다.
기존 HUD/PAUSED/GAME_OVER/STAGE_CLEAR/최종 성적표/엔딩 UI는 수정하지 않았다.

## 검증 결과 (2026-11-08)

- 최종 `.\gradlew.bat build` 종료 코드 0.
- 신규 VacationThemeVerification: 세 가지 지정 배경/테두리 색, 서로 다른 배경, 일반 글씨 대비 4.5:1 이상,
  큰 실패 제목 대비 3:1 이상, 30/60/120fps 실제 성공/실패 결과 요약 확인.
- 기존 8학기·목숨·방학 미니게임 3종·학기 전환·효과·성적표·GPA·엔딩의 30/60/120fps 회귀 검증 통과.
- 기존 Enter 새 입력·방학 INTRO/RESULT 입력 분리 및 결과 중복 확정 방지 검증 유지.
- 실제 LWJGL GUI: 세 게임 × INTRO/PLAYING/성공/실패/추가 상세 상태 × 세 해상도 = 기존/신규 각각 45장.
- 세 해상도: 800×600, 1280×720, 1920×1080. 대표 화면 직접 확인 시 겹침·잘림 없음.
- GUI 픽셀 검사로 모든 미니게임/화면의 창 모서리 배경이 각각 #FFF0DB / #F5F8FF / #18263C임을 확인했다.
- 시작/플레이/성공/실패와 암기 RECALL의 입력 완료 표시, 부분 연결된 서버의 도달/미도달 색상 확인.
- GUI 결과는 난수 seed를 고정한 실제 모델에 MinigameTestDriver 입력을 넣어 생성했다. 성공/실패 결과를 직접 대입하지 않았다.
- 실제 Enter로 다음 학년 전환 후 목숨/이동/점프 효과가 원래 PlayerModifier와 일치하고 이전 학기 StageResult가 보존됨을 확인했다.
- 화면 캡처 동안 Main의 기존 키 해제 대기로 시뮬레이션을 정지시켰고,
  렌더러 호출 전후 모델/타이머/트레이/패턴/퍼즐 포트 값 동일 확인.
- 배경 셰이더·임의 tint/blending/matrix가 설정된 호출에서도 렌더링 상태 복원 확인.
- 전용 폰트 Nearest와 dispose 후 GL handle 0 확인. 기존 빌린 리소스는 Main에서 해제됨을 확인했다.

UI 변경 후 규칙 차이는 발견되지 않았다. 서버 상세 캡처는 실제 회전·검사 입력으로
도달한 노드가 2개 이상인 미완성 연결을 준비해 민트/미도달 색상 구분을 보여준다.

## 캡처

아래 자료와 진단 코드는 Git 추적 대상이 아닌 `build/vacation-theme-gui/`에 있다.
각 테마 모음은 왼쪽 위 INTRO, 오른쪽 위 PLAYING, 왼쪽 아래 성공, 오른쪽 아래 실패다.

- [세 게임 기존/신규 비교](../build/vacation-theme-gui/three-themes-before-after.png) — 각 행 왼쪽 기존, 오른쪽 신규.
- [카페 시작·플레이·성공·실패](../build/vacation-theme-gui/cafe-screens.png)
- [스터디 시작·플레이·성공·실패](../build/vacation-theme-gui/certification-screens.png)
- [서버 시작·플레이·성공·실패](../build/vacation-theme-gui/hackathon-screens.png)
- [암기 입력 완료 상태 800×600](../build/vacation-theme-gui/after-certification-detail-800x600.png)
- [서버 부분 연결 상태 800×600](../build/vacation-theme-gui/after-hackathon-detail-800x600.png)
- [스터디 시작 1280×720](../build/vacation-theme-gui/after-certification-intro-1280x720.png)
- [서버 결과 1920×1080](../build/vacation-theme-gui/after-hackathon-failure-1920x1080.png)
- 진단 코드: `build/vacation-theme-gui/VacationThemeGuiSmoke.java`.

## 변경 파일과 남은 사항

- VacationUiTheme.java: 세 독립 팔레트·제목/결과 테마.
- VacationMinigameRenderer.java: 공통 패널/동작 카드와 각 게임의 독립 배치·표시.
- Main.java: 렌더러 생성자 연결 및 전용 폰트 해제 호출만 변경.
- VacationThemeVerification.java, FreshmanLevelVerification.java: 전용 검증 및 build 체인 연결.
- docs/VACATION_MINIGAME_UI.md, docs/VACATION_MINIGAMES.md: 현재 UI와 검증 기록.

요청한 해상도/상태에서 남은 UI 문제는 발견되지 않았다.
Git commit/push/merge는 실행하지 않았다.
