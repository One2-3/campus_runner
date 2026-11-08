# Campus Runner — 최종 성적표

## STEP 10-10 픽셀 UI (2026-11-08)

`FinalReportRenderer`와 `EndingRenderer`로 렌더링을 분리했다.
두 화면은 `PixelResultCanvas`의 공유 Texture·영어/한글 폰트를 사용한다.
점수/등급 모델인 FinalReport, StageResult, GraduationEnding과 Main의 입력/상태 전환은 수정하지 않았다.

- 크림 `#F5E8CB`, 테두리 `#594738`, 글씨 `#493B32`, 파스텔 노랑 강조.
- 중앙 704×536 논리 패널. 기존 800×600 FitViewport로 세 해상도에 동일 비율 적용.
- FINAL REPORT 제목, 8학기별 아이템/보너스/최종 점수, 전체 아이템 점수/305,
  생존 보너스/160, 최종 점수/465, GPA/4.50, SCORE RATE, 전체 수집량, TOTAL DEATHS, 기존 엔딩 등급 표시.
- 표 행 간격 24, 수치 오른쪽 정렬, 교차 행 배경, 최종 점수 강조 행.
- 전체 수집량은 **저장된 StageResult.getCollectedItemCount()의 합**이다.
  최대 수집량은 기존 학기별 아이템 최대 점수와 GradeItem.DEFAULT_SCORE(5)에서 얻는 61개다.
  UI용 개수만 합산하며 점수 모델이나 저장 기록을 수정하지 않는다.
- 마지막 학기의 기존 배경을 그대로 렌더링한 뒤 alpha 0.60 오버레이를 적용한다.
  배경 PNG·보정 셰이더·카메라 값은 수정하지 않으며 월드 오브젝트는 업데이트하지 않는다.
- 기존 한글 폰트 리소스의 공백 xadvance가 0인 문제는 새 UI 폰트 인스턴스에서만 10으로 보정했다.
  원본 fnt/PNG와 다른 화면의 폰트는 변경하지 않았다.
- 기존 폰트를 늘려 쓰는 대신 별도 폰트를 공유하므로 HUD·방학·기타 메뉴 폰트 상태에 영향을 주지 않는다.
- SpriteBatch projection/transform·셰이더·색상·블렌딩 상태 복원, Nearest 유지.
  Main.dispose()에서 공유 1×1 Texture·영어/한글 폰트를 한 번만 해제한다.

현재 흐름은 Stage 8 깃발 → 결과 저장 → STAGE_CLEAR → Enter → GAME_COMPLETE 성적표
→ Space → GRADUATION_ENDING → Enter → Stage 1 새 게임이다. STEP 10-9에서 승인된 확인 단계를 유지했다.

### 검증 결과

- 최종 `.\gradlew.bat build` 종료 코드 0.
- 기존 8학기 실제 입력 경로·학기 클리어 확인·목숨·방학 미니게임·성적표·GPA·5개 엔딩의 30/60/120fps 회귀 검증 통과.
- 신규 FinalPixelUiVerification: 실제 저장 수집량 합산, 부분 수집, 최대 61개,
  305+160=465, GPA 4.50 표시, 모든 정수 점수 0~465의 기존 등급 기준 검증 통과.
- 기존 최종 성적표 검증의 8행 합산·부분 수집·사망 보너스·최대점·동결·전체 재시작 검증 유지.
- 실제 LWJGL GUI에서 5개 등급 × 3해상도 × 성적표/엔딩의 기존 30장·신규 30장 캡처.
- 800×600 성적표와 엔딩, 1280×720 부분 수집 성적표, 다른 해상도의 등급별 대표 화면을 직접 확인했다.
  필수 항목·8개 행·한글 메시지에 겹침/잘림 없음.
- GUI에서 Stage 8 STAGE_CLEAR의 Enter → 성적표 → Space → 엔딩 → Enter 전체 초기화 확인.
  Space+Enter 동시 입력과 누른 키 유지로 즉시 재시작하지 않음, 월드/함정 시간 정지 확인.
- 배경 보정 셰이더가 설정된 호출에서도 UI 종료 후 셰이더·행렬·색상·블렌딩 복원 확인.
- 공유 Texture/font atlas의 Nearest와 종료 후 GL handle 0 확인.

GUI는 저장 결과 fixture(465/400/345/250/100점)를 사용했다.
실제 이동·점프로 8학기를 완료하는 검증은 기존 회귀 체인이 담당한다.
남은 GUI 문제는 확인 범위에서 발견되지 않았다.

### 캡처와 변경 파일

- [성적표 전후 비교](../build/final-pixel-gui/report-before-after-800x600.png)
- [800×600 만점 성적표](../build/final-pixel-gui/after-report-highest_honors-800x600.png)
- [1280×720 부분 수집 성적표](../build/final-pixel-gui/after-report-honors-1280x720.png)
- [1920×1080 만점 성적표](../build/final-pixel-gui/after-report-highest_honors-1920x1080.png)
- [5개 엔딩 모음](../build/final-pixel-gui/five-endings.png)

GUI 진단 코드: `build/final-pixel-gui/FinalPixelGuiSmoke.java`. build 자료는 Git 추적 대상이 아니다.
변경 파일은 Main.java, FinalReportRenderer.java, EndingRenderer.java, PixelResultCanvas.java,
FinalPixelUiVerification.java, FreshmanLevelVerification.java 및 이 문서와 GRADUATION_ENDINGS.md다.
기존 HUD·PAUSED·GAME_OVER·STAGE_CLEAR 디자인, 원본 에셋, 게임 규칙은 수정하지 않았다.
Git commit/push/merge는 실행하지 않았다.

## 진입과 표시 구조

Stage 8의 깃발에 도달하면 기존 `recordStageResult()`가 마지막 학기 결과를 저장하고 STAGE_CLEAR를 표시한다.
Enter 확인 후 `GAME_COMPLETE`로 전환한다. 결과 슬롯이 비어 있을 때만 저장하는 기존 중복 방지 규칙을 유지한다.
`Main.render()`는 이 상태에서 월드·플레이 HUD 대신 `renderFinalReport()`를 호출한다.
제목은 FINAL REPORT, 부제는 CAMPUS RUNNER / 8 SEMESTERS다.

`FinalReport`는 렌더링과 분리된 읽기 전용 집계 객체다. 8개 StageResult 참조 배열을 복사하고,
누락되거나 순서가 잘못된 결과를 거부한다. StageResult 자체는 기존 불변 객체를 재사용한다.
`Main.getFinalReport()`는 GAME_COMPLETE 및 GRADUATION_ENDING에서 집계를 제공한다. 별도 캐시를 두지 않아
새 게임 이후 이전 성적표를 표시할 경로가 없다.

기존 BitmapFont·SpriteBatch와 800×600 UI FitViewport를 사용한다.
표에는 학기, 아이템 점수/최대, 생존 보너스/20, 최종 점수/학기 최대를 표시한다.
열 숫자는 오른쪽 정렬하고, 행 간격은 24이며 교차 배경으로 구분한다.
표 아래에는 두 열로 최종 요약을 표시하고, FINAL SCORE를 노랑 행으로 강조한다.
제목·부제·Space 안내는 가운데 정렬한다. 창 크기 변경 시 기존 FitViewport가
전체 800×600 화면을 같은 비율로 축소·확대하고 여백을 두므로 배치 좌표는 유지된다.
공유 캔버스는 HUD 폰트를 사용하지 않으며, 렌더링 후 배치 상태를 복원한다.

## 점수 집계

| 학기 | 아이템 최대 | 생존 보너스 최대 | 학기 최종 최대 |
| --- | ---: | ---: | ---: |
| 1-1 | 20 | 20 | 40 |
| 1-2 | 25 | 20 | 45 |
| 2-1 | 30 | 20 | 50 |
| 2-2 | 35 | 20 | 55 |
| 3-1 | 40 | 20 | 60 |
| 3-2 | 45 | 20 | 65 |
| 4-1 | 50 | 20 | 70 |
| 4-2 | 60 | 20 | 80 |
| 합계 | **305** | **160** | **465** |

```text
semesterSurvivalBonus = max(0, 20 - semesterDeathCount * 5)
totalItemScore = sum(StageResult.getItemScore())
totalSurvivalBonus = sum(StageResult.getSurvivalBonus())
finalScore = totalItemScore + totalSurvivalBonus
scoreRate = finalScore / 465.0 * 100.0
gpa = finalScore / 465.0 * 4.5
```

학기 클리어 때 확정한 보너스를 다시 계산하거나 중복 가산하지 않는다.
현재 학기 런타임 점수를 추가하지 않으며 저장된 8개 결과를 각각 한 번만 합산한다.
획득률과 GPA는 계산 중 반올림하지 않고 표시할 때만 소수점 둘째 자리까지 반올림한다.
`Locale.ROOT`를 사용하여 소수점과 백분율 표시가 운영체제 언어에 따라 달라지지 않게 한다.
**GPA는 실제 대학 성적 산식이 아닌 게임용 환산값이다.** 무사망 전체 수집 시 465점·100.00%·4.50이다.

## 사망 공개와 입력

플레이 HUD는 YEAR·SEMESTER·ITEMS·SCORE·LIVES 및 하트를 유지한다.
학기별 사망·전체 사망·생존 보너스는 플레이 HUD에서 숨긴다.
성적표의 TOTAL DEATHS는 `Main.totalDeathCount`의 실제 누적값을 사용한다.
학기 전환이나 완료 진입 시 초기화하거나 보너스 계산에 재반영하지 않는다.

GAME_COMPLETE에서는 기존 `update()`의 상태 검사로 플레이어·적·가시·이동 발판 갱신이 정지한다.
이동 키와 ESC로 게임을 다시 진행하거나 Stage 9로 진입할 수 없다. R은 기존과 같이 동작을 추가하지 않는다.
성적표의 Space는 GRADUATION_ENDING으로 이동하고 Enter는 무시한다. 엔딩 화면에서
Enter는 기존 `startNewGame()`을 호출한다. 8개 결과·점수·보너스·학기 사망 기록·전체 사망을 지우고,
Stage 1·목숨 5·현재 사망 0·현재 아이템 점수 0으로 시작하며 런타임 객체도 초기화한다.
GAME_OVER의 전체 재시작과 같은 경로를 사용한다.

## 자동 검증

`FinalReportVerification`을 기존 `FreshmanLevelVerification` 실행 체인에 추가했다.
`.\gradlew.bat build` 성공 (JDK 17 실행, Java 8 소스 호환 유지).

| 항목 | 30fps | 60fps | 120fps |
| --- | --- | --- | --- |
| 정상 Stage 1~8 전환·Stage 8 저장 후 성적표 데이터 제공 | 통과 | 통과 | 통과 |
| 모든 8개 결과 존재·반복 조회 시 중복 저장/합산 없음 | 통과 | 통과 | 통과 |
| 무사망 전체 수집: 305 + 160 = 465·GPA 4.50 | 통과 | 통과 | 통과 |
| 부분 수집·학기별 사망 보너스·누적 사망 16회 | 통과 | 통과 | 통과 |
| 부분 수집: 265 + 80 = 345·GPA 3.34 | 통과 | 통과 | 통과 |
| 완료 후 240프레임 및 ESC·이동 입력에도 런타임 정지 | 통과 | 통과 | 통과 |
| Space로 엔딩 확인 후 Enter로 기록 전체 삭제·Stage 1 시작 | 통과 | 통과 | 통과 |
| 기존 목숨·누적 사망·전체 최대 465 회귀 검증 | 통과 | 통과 | 통과 |
| 기존 Stage 1~8 실제 입력 경로·충돌·재시작 회귀 검증 | 통과 | 통과 | 통과 |

집계 단위 검증은 원본 결과 배열 변경과 무관한 스냅샷 보존, 결과 누락 거부,
0점·GPA 0·획득률 0의 하한도 확인한다.
부분 수집 시 학기별로 아이템을 하나씩 놓치고, 사망을 1/2/3/4/0/1/2/3회 발생시킨다.
누적 16회 사망과 학기별 보너스 합계 80점을 별도로 대조한다.

성적표 통합 검증은 기존 생명주기 검증과 같이 플레이어를 아이템·깃발·추락 위치에 두는
제어된 테스트 픽스처로 실제 Main.update의 수집·사망·클리어·전환을 실행한다.
점수·사망 카운터·결과 슬롯은 직접 주입하지 않는다. 실제 맵 이동·점프 가능 여부는
기존 8개 스테이지 입력 기반 회귀 검증이 별도로 확인한다.
화면의 실제 글리프 렌더링과 가독성은 GUI 수동 확인 대상으로 남긴다.

## GUI 수동 확인 항목 (미실행)

- Stage 8 깃발 도착 시 플레이 HUD 대신 성적표가 나타나고 마지막 학기 결과가 포함되는지 확인.
- 제목·8개 행·요약·Space 안내가 800×600, 1280×720, 좁거나 큰 창에서 겹치거나 잘리지 않는지 확인.
- 아이템/보너스/학기 최종 점수 열의 오른쪽 정렬과 교차 행 배경 가독성 확인.
- 실제 부분 수집·사망 기록, 강조된 TOTAL DEATHS, GPA 두 자리 및 백분율이 맞는지 확인.
- 성적표에서 이동·ESC 입력으로 월드가 움직이지 않고 Space만 엔딩 화면으로 이동하는지 확인.
- 엔딩의 Enter 후 Stage 1의 목숨·하트·아이템·장애물·HUD가 초기 상태인지 확인.

## 변경 범위

- `Main.java`: GAME_COMPLETE 전용 성적표 렌더링과 데이터 연결.
- `FinalReport.java`: 읽기 전용 집계·GPA·획득률 계산.
- `FinalReportVerification.java`: 집계 및 완료·재시작 통합 검증.
- `FreshmanLevelVerification.java`: 새 검증 호출 추가.
- `docs/FINAL_REPORT.md`: 설계와 검증 기록.
- `docs/LIFE_SYSTEM.md`: 완료 화면 설명을 현재 성적표와 연결.

Stage 1~8 맵, Player 물리, 목숨 규칙, 사망 누적, StageResult 생존 보너스 계산은 변경하지 않았다.
졸업 등급·엔딩 화면은 STEP 7-3에서 추가했으며 [GRADUATION_ENDINGS.md](GRADUATION_ENDINGS.md)를 따른다.
새 라이브러리는 추가하지 않았으며 Git commit·push·merge는 실행하지 않는다.
