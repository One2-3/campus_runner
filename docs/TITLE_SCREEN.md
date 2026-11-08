# Campus Runner — 메인 타이틀 (STEP 10-12)

## 화면 구성

`TitleScreenRenderer`가 메인 메뉴와 게임 방법을 담당한다. 기존 크림색 결과 화면과 구분되는 남색·청록·금색 테마다. CAMPUS RUNNER, “학점을 모아 졸업까지 달려라!”, 기존 A+ 별과 코드로 그린 픽셀 졸업모를 표시한다.

봄 낮 캠퍼스 PNG와 기존 SeasonalBackgroundRenderer를 재사용한다. 고정 가상 중심으로 배경을 그려 월드 카메라에 영향받지 않는다. 기존 비율·색감 보정을 유지하며 남색 56% 오버레이로 가독성을 높인다. 배경은 창 전체를 채우고 UI는 기존 800×600 FitViewport로 중앙 정렬한다. 원본 PNG는 수정하지 않았다.

## 메뉴와 입력

기존 `TITLE_SCREEN`을 유지하며 `titleSelection`, `titleHelpVisible`로 화면을 관리한다. 새 게임 상태를 추가하지 않는다. 최초 타이틀에서 StageDefinition과 Player를 만들지 않고 월드 업데이트 없이 반환한다.

- 위/아래: 게임 시작 / 게임 방법 / 게임 종료 순환. 기본 선택은 게임 시작이다.
- Enter: 선택 실행. 방향키와 Enter가 같은 프레임이면 이동만 처리한다.
- 게임 방법: 실제 코드의 좌우 방향키 이동·Space 점프·Esc 일시정지/재개와 학점·장애물·8학기·방학 미니게임 3종·최종 GPA·다섯 엔딩을 안내한다. Enter는 무시하고 Esc로 원래 선택에 복귀한다. Esc+Enter도 안내만 닫는다.
- 게임 시작: 기존 `startNewGame()`을 재사용해 Stage 1, 목숨·점수·학기 결과·사망 기록·방학 효과와 타이틀 선택 상태를 초기화한다.
- 게임 종료: `Gdx.app.exit()`으로 정상 종료하고 기존 dispose 흐름을 사용한다.

`isKeyJustPressed`를 사용하므로 키를 누르고 있는 동안 메뉴가 반복 이동하지 않는다. GAME_OVER TITLE 및 PAUSED QUIT은 기존 초기화를 거쳐 기본 타이틀로 복귀한다. GAME_OVER RESTART와 졸업 엔딩 Enter는 기존대로 타이틀을 거치지 않고 Stage 1 새 게임을 시작한다. 성적표 Space → 엔딩 전환도 유지한다. 엔딩에 새 타이틀 복귀 입력은 추가하지 않았다.

## 폰트와 리소스

기존 `tools/fonts/NanumGothic-Bold.ttf`로 타이틀 전용 비트맵 부분집합 **Campus Title Korean**을 생성했다. 기존 방학·엔딩 폰트는 변경하지 않았다. SIL OFL 1.1 안내는 `assets/fonts/title-ko-OFL.txt`에 동봉한다. 새 다운로드나 라이브러리는 없다.

재생성 명령 (저장소 루트):

```powershell
& .\tools\GenerateVacationFont.ps1 -OutputStem title-ko -FaceName 'Campus Title Korean' -SourcePaths @('core/src/main/java/io/jbnu/test/TitleScreenRenderer.java')
```

전용 폰트를 초기화 시 한 번 로드하고 Nearest 필터로 렌더링한다. 매 프레임 텍스처를 만들지 않는다. 별·배경·흰색 픽셀은 기존 텍스처를 빌려 쓰며 Main이 해제하고, 전용 폰트는 TitleScreenRenderer가 해제한다. 렌더링 뒤 배치의 셰이더·색상·투영/변환 행렬·블렌딩과 공유 폰트 상태를 복원한다.

## 자동 검증

`TitleMenuVerification`을 기존 FreshmanLevelVerification 실행 체인에 추가했다. 기존 TitleScreenVerification도 유지한다. `.\gradlew.bat build` 성공. 30·60·120fps에서 다음 항목이 통과했다.

- 최초 타이틀·월드 미생성, 메뉴 순환, 누른 키 중복 방지.
- 게임 방법 진입·Esc 복귀와 동시 입력 우선순위.
- 새 게임 초기화 및 PLAYING에서 Enter 중복 초기화 방지.
- 다섯 번째 사망 후 GAME_OVER TITLE 복귀·선택 초기화.
- 종료 메뉴의 정확히 한 번 종료 요청.
- 모든 타이틀 문구 글리프와 폰트 라이선스 파일 존재.
- 기존 8학기 실제 이동/점프 경로, 목숨·피격·방학 3종·점수·성적표·GPA·엔딩 및 UI 회귀 검증.

## 실제 GUI 확인

LWJGL3에서 실제 Main.create/render/resize를 실행하고 키 입력을 주입했다. 800×600, 1280×720, 1920×1080 모두 메뉴 선택·게임 방법·게임 시작·GAME_OVER TITLE 복귀를 검증했다. 캡처에서 글리프 누락·잘림·메뉴 겹침·흰 여백이 없음을 확인했다. 월드 카메라 이동 전후 타이틀 캡처가 동일했다.

실제 종료 메뉴로 창이 종료되고 dispose가 한 번 실행되며 전용 폰트·별·배경 텍스처가 해제됨을 검증했다. 다른 배치 셰이더·색상·행렬·블렌딩과 공유 폰트 설정을 적용한 뒤에도 렌더링 후 모두 복원됐다.

| 해상도 | 타이틀 | 게임 방법 |
| --- | --- | --- |
| 800×600 | [메뉴](../build/title-redesign-gui/after-menu-start-800x600.png) | [안내](../build/title-redesign-gui/after-help-800x600.png) |
| 1280×720 | [메뉴](../build/title-redesign-gui/after-menu-start-1280x720.png) | [안내](../build/title-redesign-gui/after-help-1280x720.png) |
| 1920×1080 | [메뉴](../build/title-redesign-gui/after-menu-start-1920x1080.png) | [안내](../build/title-redesign-gui/after-help-1920x1080.png) |

[800×600 변경 전후: 왼쪽 기존, 오른쪽 신규](../build/title-redesign-gui/comparison-800x600.png)

캡처·GUI 도구와 로그는 Git 제외 `build/title-redesign-gui/`에 있다. 빌드 로그는 `build.log`, GUI 로그는 `after.log`다.

남은 수동 확인: 물리 키보드로 장시간 누르기·빠른 연속 누르기를 확인한다. GUI 검증은 코드로 키 입력을 주입했으며 사람이 키보드를 직접 조작한 것으로 보고하지 않는다. 발견된 미해결 기능·레이아웃 문제는 없다.

게임 맵·물리·다른 UI·점수·GPA·엔딩 규칙은 변경하지 않았다. Git commit, push, merge는 실행하지 않았다.
