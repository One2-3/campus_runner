# 학기 클리어 픽셀 성적표 — STEP 10-9

## 기존 구조와 승인된 전환 변경

기존에는 깃발 도착 즉시 `recordStageResult()`로 결과와 생존 보너스를 확정한 뒤,
1/3/5/7학기는 STAGE_CLEAR, 2/4/6학기는 VACATION_EVENT, 8학기는 GAME_COMPLETE로 바로 전환했다.

사용자가 선택한 **8학기 모두 클리어 화면 → Enter 후 기존 목적지** 방식으로 변경했다.
모든 학기의 깃발에서 결과를 저장하고 STAGE_CLEAR에 머문다.
기존 목적지 선택 부분만 `continueAfterStageClear()`로 옮겼으며, 보너스·방학 효과·미니게임·최종 성적표·엔딩 규칙은 유지한다.

| 완료 학기 | CONTINUE Enter 후 |
| --- | --- |
| 1 | Stage 2 |
| 2 | CAFE 미니게임 INTRO → 기존 결과 확인 후 Stage 3 |
| 3 | Stage 4 |
| 4 | CERTIFICATION 미니게임 INTRO → 기존 결과 확인 후 Stage 5 |
| 5 | Stage 6 |
| 6 | HACKATHON 미니게임 INTRO → 기존 결과 확인 후 Stage 7 |
| 7 | Stage 8 |
| 8 | 기존 GAME_COMPLETE 최종 성적표 → Space 졸업 엔딩 → Enter 새 게임 |

Stage 9는 생성하지 않는다. 방학 미니게임의 시작/진행/결과 확인 입력은 기존대로 유지한다.

## 표시 데이터와 점수

`StageClearRenderer`에는 `completedResults[currentStageNumber - 1]`에 저장된
**동일한 StageResult 객체**와 현재 학기 아이템 총개수 `gradeItems.size`를 전달한다.

- 완료 학년·학기: 저장 결과의 stageNumber에서 학년·학기 표시.
- ITEM SCORE: `getItemScore()`.
- ITEMS: `getCollectedItemCount()` / 현재 스테이지 총개수.
- DEATHS: `getDeathCount()`.
- SURVIVAL BONUS: `getSurvivalBonus()`.
- FINAL SCORE: `getFinalScore()`.

렌더러에서 점수를 재계산하거나 StageResult를 새로 만들지 않는다.
기존 StageResult가 확정하는 `max(0, 20 - deaths * 5)` 보너스와 아이템 점수 합계를 표시한다.
Enter는 결과를 다시 저장하거나 보너스를 더하지 않는다. 최대 아이템 305, 보너스 160, 총점 465/GPA 4.50을 유지한다.

## 디자인과 리소스

- 기존 HUD/PAUSED/GAME_OVER와 같은 크림 `#F5E8CB`, 테두리 `#594738`, 글씨 `#493B32`.
- SEMESTER CLEAR! 제목 아래 파스텔 노랑 강조선, FINAL SCORE 노랑 행, 밝은 노랑 `#F7DE8D` CONTINUE 행.
- 전체 창에 어두운 alpha 0.60 오버레이. 기존 HUD는 수정 없이 뒤에서 어두워진다.
- 기본 UI 논리 크기 360×392, 화면 중앙. 기존 FitViewport가 세 해상도에 균일 배율로 적용된다.
- 성적표 라벨은 왼쪽, 실제 수치는 오른쪽 정렬. NEXT 안내로 방학/다음 학기/최종 성적표 목적지를 보여준다.
- 공통 1×1 Texture와 전용 폰트를 초기화 시 한 번 생성, Nearest 적용, dispose에서 해제.
- 렌더링 후 projection/transform·셰이더·색상·블렌딩 설정을 복원한다.
- 원본 PNG, 기존 HUD/PAUSED/GAME_OVER 렌더러, 월드 그래픽과 물리는 수정하지 않았다.

## 월드 정지와 입력

STAGE_CLEAR에서는 기존 `gameState != PLAYING` 경로가 플레이어 물리·적·발판·함정 시간·아이템 수집을 차단한다.
Enter는 `isKeyJustPressed`만 처리한다. 깃발 도착 프레임의 Enter로 새 화면을 통과하지 않으며,
계속 누른 Enter로 다음 학기/방학 INTRO를 여러 번 통과하지 않는다.
Stage 8의 Enter와 Space가 동시에 눌려도 최종 성적표만 열고 엔딩을 같은 프레임에 열지 않는다.

## 자동 검증 결과 (2026-11-07)

- 최종 `./gradlew.bat build`: 종료 코드 0.
- 신규 StageClearVerification: 30/60/120fps × 8학기 × 사망 0~4회 결과 조합 통과.
- 실제 Main 수집 판정·사망·깃발 처리로 결과 저장, 학기/점수/수집량/사망/보너스/최종 점수 정확성 확인.
- 1초 대기 중 플레이어·적·발판·함정 상태와 경과 시간·아이템·목숨/점수·카메라 동일.
- 화면 대기 및 CONTINUE 이후 같은 StageResult 참조 유지: 중복 저장/보너스 합산 없음.
- 깃발 프레임 Enter, 누른 Enter 유지, Enter+Space 동시 입력을 검증했다.
- 2/4/6학기 미니게임 INTRO와 8학기 최종 성적표가 확인 입력 없이 건너뛰어지지 않음을 검증했다.
- 기존 실제 입력 기반 8학기 경로·충돌·목숨·피격·방학 미니게임·성적표·GPA·엔딩 회귀 검증 통과.
- 기존 전환 검증에는 새 STAGE_CLEAR 상태 확인과 실제 Enter 입력 단계를 추가했다.
  기존 점수·아이템·미니게임 동작 검증은 삭제하지 않았다.

## 실제 GUI 검증

- 800×600 / 1280×720 / 1920×1080 실제 창 크기 변경.
- 기존 화면 24장, 신규 성적표 24장, 방학/최종 결과 목적지 12장 캡처.
- 세 해상도 대표 캡처 직접 확인: 중앙 배치, 성적표 수치·CONTINUE·NEXT 안내 정상, 잘림/겹침 없음.
- 8학기 모두 GUI 입력 Enter로 기존 목적지 전환 및 결과 참조 보존 확인.
- 메뉴 표시 중 월드 정지, 배치 상태 복원, Nearest, 해제 후 폰트·Texture GL handle 0 확인.
- GUI 장면은 진단용 위치 fixture와 실제 기존 수집/사망/깃발 함수로 준비했다.
  전체 맵의 실제 이동·점프 입력 검증은 기존 회귀 검증이 담당한다.

자료는 Git 추적 대상이 아닌 `build/stage-clear-gui/`에 저장했다.
기존 2/4/6/8학기 캡처는 새 중간 화면이 없던 시점의 방학/최종 결과 화면이다.

- [800×600 기존/신규 비교](../build/stage-clear-gui/before-after-800x600.png) — 왼쪽 기존, 오른쪽 신규.
- [8학기 성적표 모음](../build/stage-clear-gui/all-semesters.png)
- [800×600 성적표](../build/stage-clear-gui/after-stage1-800x600.png)
- [1280×720 성적표](../build/stage-clear-gui/after-stage4-1280x720.png)
- [1920×1080 성적표](../build/stage-clear-gui/after-stage8-1920x1080.png)
- [Stage 2 → CAFE](../build/stage-clear-gui/after-destination-stage2-800x600.png)
- [Stage 4 → CERTIFICATION](../build/stage-clear-gui/after-destination-stage4-800x600.png)
- [Stage 6 → HACKATHON](../build/stage-clear-gui/after-destination-stage6-800x600.png)
- [Stage 8 → 최종 성적표](../build/stage-clear-gui/after-destination-stage8-800x600.png)
- GUI 진단 코드: `build/stage-clear-gui/StageClearGuiSmoke.java`.

## 변경 파일

운영 코드:

- Main.java: 렌더러 연결·해제, 모든 학기 STAGE_CLEAR 진입, CONTINUE 목적지 연결.
- StageClearRenderer.java: 저장된 결과 표시와 픽셀 UI.

자동 검증:

- StageClearVerification.java: 신규 전체 학기 성적표·정지·입력 검증.
- FreshmanLevelVerification.java: 검증 체인 연결 및 Stage 8 확인 입력.
- Semester2LevelVerification.java, SophomoreSemester2Verification.java, JuniorSemester2Verification.java: 깃발 후 STAGE_CLEAR 확인.
- SeniorSemester2Verification.java: 기존 실제 입력 경로 종료 후 성적표 확인과 Enter 최종 결과 전환.
- LifeSystemVerification.java, FinalReportVerification.java: Stage 8 성적표 확인 후 기존 점수/최종 결과 검증 유지.
- VacationSystemVerification.java, VacationMinigamesVerification.java: 미니게임 전 성적표 확인 입력 추가.
- VacationVerificationSupport.java: 실제 Enter로 목적지와 결과 보존을 확인하는 공통 도우미.
- docs/STAGE_CLEAR_UI.md: 현재 동작과 결과 기록.

확인 범위에서 남은 GUI 문제는 없다. Git commit/push/merge는 실행하지 않았다.
