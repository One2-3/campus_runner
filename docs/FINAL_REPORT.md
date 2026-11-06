# Campus Runner — 최종 성적표

## 진입과 표시 구조

Stage 8의 깃발에 도달하면 기존 `recordStageResult()`가 마지막 학기 결과를 저장한 뒤
`GAME_COMPLETE`로 전환한다. 결과 슬롯이 비어 있을 때만 저장하는 기존 중복 방지 규칙을 유지한다.
`Main.render()`는 이 상태에서 월드·플레이 HUD 대신 `renderFinalReport()`를 호출한다.
제목은 FINAL ACADEMIC REPORT, 부제는 CAMPUS RUNNER다.

`FinalReport`는 렌더링과 분리된 읽기 전용 집계 객체다. 8개 StageResult 참조 배열을 복사하고,
누락되거나 순서가 잘못된 결과를 거부한다. StageResult 자체는 기존 불변 객체를 재사용한다.
`Main.getFinalReport()`는 GAME_COMPLETE 및 GRADUATION_ENDING에서 집계를 제공한다. 별도 캐시를 두지 않아
새 게임 이후 이전 성적표를 표시할 경로가 없다.

기존 BitmapFont·SpriteBatch·ShapeRenderer와 800×600 UI FitViewport를 사용한다.
표에는 학기, 아이템 점수/최대, 생존 보너스/20, 최종 점수/학기 최대를 표시한다.
열 숫자는 오른쪽 정렬하고, 행 간격은 28이며 교차 배경으로 구분한다.
표 아래에는 두 열로 최종 요약을 표시하고, TOTAL DEATHS는 짙은 빨강으로 강조한다.
제목·부제·Space 안내는 가운데 정렬한다. 창 크기 변경 시 기존 FitViewport가
전체 800×600 화면을 같은 비율로 축소·확대하고 여백을 두므로 배치 좌표는 유지된다.
성적표 렌더링 후 폰트 크기와 색상을 기존 HUD 설정으로 복원한다.

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
