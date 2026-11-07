# 픽셀 아트 HUD — STEP 10-6

## STEP 10-6-1: 크기 및 정렬 개선 (현재 적용값)

`PixelHudRenderer.HUD_SCALE = 0.85f`로 패널·폰트·아이콘·테두리·내부 여백을 함께 축소한다.
`Layout`이 전달받은 UI 영역에서 중앙 X와 상단 여백을 계산하며, 공간이 더 좁으면
가용 너비/높이에 맞춰 배율을 추가로 낮춘다. 기존 Main의 800×600 FitViewport가
실제 창 크기에 맞춰 같은 비율로 확대한다. Main 및 게임 업데이트 코드는 이번 조정에서 수정하지 않았다.

800×600 기준 크기(논리 좌표, 그림자 제외):

| 항목 | 이전 | 현재 |
| --- | --- | --- |
| 전체 주 패널 폭 | 772 | 656.2 (85%) |
| 학년·학기 패널 | 약 300.8×50 | 255×42.5 |
| 점수 패널 | 약 192.7×50 | 163.2×42.5 |
| 아이템 패널 | 약 262.5×50 | 224.4×42.5 |
| 목숨 패널(한 줄) | 약 262.5×28 | 224.4×20.4 |
| 화면 왼쪽/오른쪽 여백 | 14 | 약 71.9 |
| 상단 여백 | 14 | 18 |
| 패널 가로/세로 간격 | 8 / 6 | 6.8 / 6.8 |
| A+ 이미지 전체 크기 | 38×38 | 32.3×32.3 |
| 하트 크기 / 하트 배치 간격 | 14×12 / 18 | 11.9×10.2 / 15.3 |

폰트·배치 보정:

- 부제와 주요 학기 정보의 세로 중심을 분리하여 배치한다.
- SCORE/ITEMS 라벨은 같은 중심 높이, 숫자와 아이콘은 아래쪽 공통 줄에 배치한다.
- 기존에 상대적으로 컸던 점수/수집량 숫자는 기본 폰트 배율 1.5→1.3으로 균형을 맞춘 후 전체 0.85 배율을 적용한다.
- GlyphLayout의 실제 글자 높이로 세로 중심을 계산한다. `12 / 12`와 가장 긴 학기 문구의 실제 글자 폭도 GUI 검증에서 확인했다.
- A+는 PNG 투명 여백을 감안해 위치를 보정한다. 원본 파일은 수정하지 않는다.
- 목숨 문구는 `LIVES 5/5` 형식으로 간결하게 표시한다. 하트 시작점은 이전 108→80(축소 전 단위)으로 당긴다.
- 하트 위치는 남은 목숨이 아닌 최대 목숨에 따라 결정하므로 감소할 때 다른 하트가 움직이지 않는다.
- 최대 목숨이 두 자릿수면 텍스트 영역을 추가 확보한다. 하트는 가용 너비를 계산해 줄바꿈한다.
- 렌더링용 transform으로 균일 축소하며, 종료 후 기존 transform·셰이더·색상·블렌딩을 모두 복원한다.

현재 검증 결과:

- 최종 `./gradlew.bat build` 종료 코드 0. 기존 8학기·피격·목숨·방학·성적표·GPA·엔딩의 30·60·120fps 회귀 검증 통과.
- 레이아웃 테스트를 새 규격에 맞춰 수정: 화면 너비별 중앙 배치·균일 배율·가로 간격·최대 5/6/12 하트의 영역 경계 통과.
- 800×600 / 1280×720 / 1920×1080 창 크기를 실제 변경하며 8학기 GUI 캡처 24장 생성. 기존 HUD 24장과 전후 비교.
- 세 해상도의 대표 캡처와 800×600의 `12 / 12` 표시를 직접 확인: 겹침·잘림 없음.
- 실제 오른쪽 이동 입력으로 SCORE 5, ITEMS 1 / 4 갱신 확인. 공통 사망 처리 호출 후 4목숨 표시와 아이템 초기화 확인.
- 실제 학기 완료 Enter 입력 후 학년·학기 및 분모 갱신 확인. 카메라 이동 시 패널 내부 픽셀 동일.
- 이전/이후 24쌍에서 HUD 아래 월드 영역 픽셀 차이 0. 최대 6목숨 GUI 확인, 소유 텍스처 해제 확인.
- 일시정지/게임 오버 중앙 UI·입력, 색상 테마, 배경·물리·충돌·점수 계산은 수정하지 않았다.
  공통 HUD의 새 크기는 그 화면의 뒤에도 동일하게 표시된다.

이번 단계 변경 파일: `PixelHudRenderer.java`, `PixelHudVerification.java`, 이 문서.
아래 STEP 10-6 설명의 원래 치수는 최초 구현 기록이며, 현재 치수는 위 표를 따른다.

비교 자료:

- [800×600 이전/이후](../build/pixel-hud-compact-gui/before-after-800x600.png) — 왼쪽 이전, 오른쪽 현재.
- [8학기 HUD 모음](../build/pixel-hud-compact-gui/all-semesters-hud.png)
- [1280×720 현재](../build/pixel-hud-compact-gui/after-stage4-1280x720.png)
- [1920×1080 현재](../build/pixel-hud-compact-gui/after-stage8-1920x1080.png)
- [12 / 12 전체 수집 표시](../build/pixel-hud-compact-gui/after-full12-800x600.png)
- [목숨 감소 후](../build/pixel-hud-compact-gui/after-four-lives-800x600.png)
- [픽셀 비교 결과](../build/pixel-hud-compact-gui/comparison-results.json)

요청한 해상도에서 남은 HUD 배치 문제는 발견되지 않았다. Git commit/push/merge는 실행하지 않았다.

## 구조와 표시 정보

`Main.renderHud()`에서 기존 UI `FitViewport(800, 600)`를 적용하고
`PixelHudRenderer`에 현재 게임 데이터를 전달한다. 기존 텍스트·원형 하트 HUD 호출은 교체했다.
카메라, 배경 보정, 월드 렌더링과 분리된 UI 좌표를 그대로 사용한다.

| 영역 | 표시 | 데이터 |
| --- | --- | --- |
| 상단 왼쪽 | YEAR n / SEMESTER n | 현재 StageDefinition |
| 상단 중앙 | 별 아이콘, SCORE | 현재 학기 stageScore, 아이템 점수만 표시 |
| 상단 오른쪽 | 기존 A+ PNG, ITEMS n / n | collectedItemCount / gradeItems.size |
| 오른쪽 두 번째 줄 | LIVES n / n, 픽셀 하트 | lives / semesterMaxLives |

기본 최대 목숨을 HUD에 5로 고정하지 않는다. 방학 효과로 6목숨이 되면 하트도 6개를 그린다.
더 많은 하트는 패널 너비에 맞춰 줄바꿈하며 높이를 늘린다.
남은 하트는 분홍색, 잃은 하트는 어두운 갈색이다.
학기별·전체 사망 횟수와 생존 보너스는 계속 플레이 HUD에서 숨긴다.

## 디자인과 레이아웃

- 크림 패널 `#F5E8CB`, 갈색 테두리 `#594738`, 기본 글씨 `#493B32`.
- 파스텔 연두·노랑·분홍 2단위 강조선, 2단위 테두리와 계단형 픽셀 아이콘.
- 논리 화면 기준 좌우·상단 여백 14, 주 패널 높이 50, 목숨 패널 높이 28(기본 한 줄).
- 왼쪽/중앙/오른쪽 열은 가용 너비의 40%/26%/34%로 구분하고 열 사이 간격을 둔다.
- 기존 FitViewport가 해상도에 맞춰 UI 전체를 같은 비율로 확대한다. 16:9에서는 기존 중앙 UI viewport 영역을 유지한다.
- 별·하트는 코드의 픽셀 마스크로 그린다. A+ 아이콘은 Main이 이미 로드한 Texture를 공유한다.
- HUD 전용 기본 BitmapFont를 한 번 생성하고 Nearest로 설정한다. 다른 화면의 기존 영어·한글 폰트는 수정하지 않는다.

일시정지·게임 오버·학기 완료의 중앙 안내 문구와 입력 처리는 수정하지 않았다.
이 화면 아래의 공통 HUD도 같은 새 렌더러를 사용한다. 성적표·졸업 엔딩·시작 화면·방학 미니게임 UI는 변경하지 않았다.

## 렌더링 상태와 리소스

전용 1×1 흰색 Texture와 폰트를 초기화할 때 한 번 생성하고, 사각형·아이콘은 SpriteBatch로 그린다.
기존 A+ Texture를 새로 로드하지 않는다. 모든 HUD Texture/폰트 atlas는 Nearest 필터를 사용한다.
`Main.dispose()`에서 HUD 소유 폰트와 흰색 Texture를 해제한다. A+ Texture의 해제 책임은 기존대로 Main에 있다.

렌더링 중 기본 SpriteBatch 셰이더와 알파 블렌딩을 명시적으로 사용한다.
종료 시 호출 전 셰이더, packed color, 블렌딩 활성화 여부, RGB/알파 blend function을 복원한다.
배경 보정 셰이더는 수정하지 않았다. HUD는 배경 보정의 영향을 받지 않는다.
프레임마다 폰트나 Texture를 생성하지 않는다.

## 검증 결과 (2026-11-07)

- `./gradlew.bat build`: 종료 코드 0. 기존 verifyFreshman 통합 체인과 신규 PixelHudVerification 통과.
- 기존 Stage 1~8 실제 입력 경로, 피격·목숨, 방학 미니게임 3종, 성적표·GPA·졸업 엔딩의 30·60·120fps 회귀 검증 통과.
- 신규 자동 검증: 8학기 학년·학기/아이템 분모/전체 수집 점수 문자열, 패널 여백·분리, 최대 5·6·12개 하트의 줄바꿈 경계 통과.
- 실제 LWJGL GUI: 8학기 × 800×600 / 1280×720 / 1920×1080의 기존·신규 캡처 48장 생성.
- 대표 GUI 캡처와 8학기 HUD 모음을 직접 확인: 패널·글씨·아이콘 겹침 및 잘림 없음.
- 실제 Stage 1 오른쪽 이동 입력 12프레임으로 첫 아이템 획득 후 SCORE 5 / ITEMS 1 / 4 표시 확인.
- 기존 공통 `applyDeath()` 호출로 4목숨·현재 학기 점수/아이템 초기화 확인. 일반 피격 대기와 충돌 자체는 기존 통합 검증으로 확인했다.
- 실제 STAGE_CLEAR의 Enter 입력으로 Stage 2 전환 후 YEAR 1 / SEMESTER 2, 5목숨, ITEMS 0 / 5 확인.
- 6목숨 렌더링 진단 fixture 확인. 실제 방학 추가 목숨 적용 규칙은 기존 방학 통합 검증이 담당한다.
- 카메라 X를 변경한 진단 장면에서 배경은 바뀌지만 HUD 패널 내부 픽셀은 동일함을 자동 비교했다.
- 24개 전후 장면에서 기존 HUD 아래(논리 Y=450 이하) 모든 픽셀 동일. 월드·배경 그래픽에 영향 없음.
- GUI에서 상속된 배경 셰이더/색상/블렌딩 설정에 관계없이 HUD 렌더링 후 상태를 정확히 복원함을 검증했다.
- HUD 폰트·흰색 Texture의 Nearest와 dispose 후 GL handle 0 확인.

GUI 진단은 `build/pixel-hud-gui/PixelHudGuiSmoke.java`, 이미지 비교는
`build/pixel-hud-gui/compare_captures.py`에 있다. build 아래 자료는 빌드 산출물이며 Git 추적 대상이 아니다.

## 비교 캡처

- [800×600 기존/신규 비교](../build/pixel-hud-gui/before-after-800x600.png) — 왼쪽 기존, 오른쪽 신규.
- [8학기 HUD 모음](../build/pixel-hud-gui/all-semesters-hud.png) — 행마다 1-1/1-2, 2-1/2-2, 3-1/3-2, 4-1/4-2.
- [1280×720](../build/pixel-hud-gui/after-stage4-1280x720.png)
- [1920×1080](../build/pixel-hud-gui/after-stage8-1920x1080.png)
- [아이템 수집 후](../build/pixel-hud-gui/after-collected-800x600.png)
- [목숨 감소 후](../build/pixel-hud-gui/after-four-lives-800x600.png)
- [추가 목숨](../build/pixel-hud-gui/after-extra-life-800x600.png)
- [학기 전환](../build/pixel-hud-gui/after-semester-transition-800x600.png)
- [전후 픽셀 비교 결과](../build/pixel-hud-gui/comparison-results.json)

## 남은 사항

요청한 세 해상도와 기본/추가 목숨 범위에서 발견된 HUD 시각 문제는 없다.
기존 UI viewport의 4:3 배치 정책은 유지했으며, 16:9에서 HUD를 창 전체 너비로 늘리는 변경은 하지 않았다.
새 에셋 다운로드, PNG 수정, 게임 규칙 변경, Git commit/push/merge는 수행하지 않았다.
