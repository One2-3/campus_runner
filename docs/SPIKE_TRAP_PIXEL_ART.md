# 가시 및 시간제 함정 픽셀 아트

## 현재 규칙: 실제 가시 삼각형과 충돌 일치 (2026-11-07)

이 절이 현재 동작이다. 아래 높이 축소 당시의 허공 피격 기록은 수정 전 이력이다.
**최대 25.2단위 높았던 허공 피격 영역을 제거했다.** PNG·가시 높이·받침대·간격은 바꾸지 않았다.

### 변경 파일

- `core/src/main/java/io/jbnu/test/SpikeLayout.java`: 그래픽에 의존하지 않는 불변 가시 배치 및 삼각형-Rectangle 겹침 계산.
- `core/src/main/java/io/jbnu/test/TimedTrap.java`: 가시 배치를 한 번 생성하고 ACTIVE일 때 실제 가시로 피해 검사.
- `core/src/main/java/io/jbnu/test/TrapSprites.java`: 기존 수식 대신 동일한 가시 배치 객체로 렌더링. 결과 이미지는 동일.
- `core/src/test/java/io/jbnu/test/SpikeCollisionVerification.java`: 경계·독립 다각형 판정·전체 학기 검증 추가.
- `core/src/test/java/io/jbnu/test/FreshmanLevelVerification.java`: 전용 검증을 Gradle build 회귀 체인에 연결.
- `core/src/test/java/io/jbnu/test/SophomoreSemester2Verification.java`: 실제 꼭짓점을 가로지르는 착지 검사와 늦은 점프 접촉 경로.
- `core/src/test/java/io/jbnu/test/JuniorSemester2Verification.java`: 짧아진 실제 가시에 닿는 늦은 점프 경로.
- `core/src/test/java/io/jbnu/test/SeniorSemester1Verification.java`: 동일한 실제 접촉 경로 보정.
- `docs/SPIKE_TRAP_PIXEL_ART.md`: 현재 판정과 검증 기록.

Main의 피격/사망 처리, StageFactory, Player, 원본 PNG는 이번 수정에서 변경하지 않았다.

### 변경 전후 피해 영역

이전 판정은 authored Rectangle 내부에서 바닥 Y부터 원래 높이까지 네 삼각형을 사용했다.
받침대 아래 부분도 위험했고, 높이를 줄인 그림 위로도 판정이 남아 있었다.
현재는 받침대 윗면부터 실제 표시 꼭짓점까지의 네 삼각형만 위험하다.
원래 Rectangle은 맵 정보·광범위 검사에 그대로 남으며 전체가 피해 영역이라는 의미는 아니다.

```text
count = 4
baseHeight = min(16, originalHeight * 0.5)
cellWidth = originalWidth / count
inset = cellWidth * baseHeight / (2 * originalHeight)
spikeWidth = cellWidth - 2 * inset
spikeLeftX(i) = trap.x + i * cellWidth + inset
spikeCenterX(i) = spikeLeftX(i) + spikeWidth / 2
spikeBottomY = trap.y + baseHeight
spikeHeight = (originalHeight - baseHeight) * 0.55
spikeTopY = spikeBottomY + spikeHeight
```

| 원래 높이 | 피해 시작 Y (상대) | 피해 끝 Y (상대) | 돌출/피해 높이 | 이전 허공 위험 구간 |
| --- | --- | --- | --- | --- |
| 24 | 12 | 18.6 | 6.6 | 5.4 제거 |
| 28 | 14 | 21.7 | 7.7 | 6.3 제거 |
| 48 | 16 | 33.6 | 17.6 | 14.4 제거 |
| 72 | 16 | 46.8 | 30.8 | 25.2 제거 |

예를 들어 `(2010,60,144,72)`는 Y=76~106.8의 삼각형만 검사한다.
Y=106.8 위의 허공과 받침대 Y=60~76은 피해 영역에서 제외된다.
경고등·꽃·흙·금속·반투명 받침대 속 가시 재질은 장식이며 피해 영역을 만들지 않는다.

### 개별 삼각형 판정과 좌표 공유

각 삼각형은 위로 갈수록 폭이 줄어든다. 플레이어 Rectangle과 겹치는 가장 낮은 Y에서의
삼각형 가로 구간을 계산하고 플레이어의 가로 구간과 겹치는지 검사한다.
그 높이에서 겹치지 않으면 더 위의 좁은 단면도 겹칠 수 없으므로,
축과 평행한 Rectangle에 대해서는 정확한 삼각형 교차 판정이다.
실제 접촉은 양의 면적으로 겹쳤을 때 발생하며, 경계에 점/선만 닿는 경우는 기존 Rectangle 규칙처럼 무피해다.

`TimedTrap`이 `SpikeLayout`을 한 번 생성해 보관한다. `isDangerousTo()`와 `TrapSprites`가
같은 객체의 시작점·폭·높이·중심을 읽는다. 높이 비율 상수도 SpikeLayout 한 곳에 있다.
게임 규칙이 Texture/SpriteBatch/렌더러에 의존하지 않으며, 매 프레임 가시 배열을 생성하지 않는다.
reset은 동일 배치를 재사용하고 copyInitial은 같은 규칙으로 새 배치를 만든다.

SAFE/WARNING은 항상 무피해, 시간제 ACTIVE와 고정 ACTIVE만 삼각형 검사를 수행한다.
레거시 일반 TimedTrap의 Rectangle 판정은 유지했다. 실제 8학기 가시 27개는 모두 새 삼각형 판정을 사용한다.
PNG의 안티앨리어싱 알파 픽셀마다 판정하는 방식은 아니며, 표시 외곽 삼각형을 기준으로 한다.

### 자동 검증 결과

`.\gradlew.bat build` 성공. 30/60/120fps의 기존 8학기 경로·목숨·피격·방학·성적표 검증 통과.

전용 `SpikeCollisionVerification`에서 다음을 확인했다.

- 실제 tip/경사면을 조금 침범하면 피격, 바깥/위/사이 틈/받침대는 무피해.
- 실제 32×44 Player가 위쪽 허공과 꼭짓점 사이 빈 공간에서 무피해.
- SAFE/WARNING 무피해와 ACTIVE 피해, 고정 가시, 기존 주기/복사/reset/좌표 유지.
- 5개 크기의 무작위 Rectangle 10,000개를 독립적인 다각형 SAT 판정과 비교해 모두 일치.
- 30/60/120fps 각각 27개 실제 가시의 새 꼭짓점보다 1단위 위가 무피해임을 확인.
- 같은 27개에서 꼭짓점 바로 아래 접촉은 모두 피격. 이전 25.2단위 허공 피해 사례도 해소.

이전 일부 음성 테스트는 높이가 큰 가시에 맞춰 모든 ACTIVE 점프가 실패할 것으로 가정했다.
현재 보이는 가시를 넘는 점프가 정상 통과할 수 있으므로, 걷기 접촉 검사는 유지하고
점프 실패 시나리오는 실제로 가시에 닿는 늦은 점프로 바꿨다.
착지 검사는 임의의 발 높이 대신 전 프레임에서 실제 꼭짓점 위에 있었고 다음 프레임에
몸이 꼭짓점을 가로질러 겹치는지 확인한다. 이동·점프 입력만 사용하며 맵/물리를 바꾸거나 검사를 무조건 통과시키지 않았다.

### 실제 GUI 검증

800×600 같은 위치의 고정/SAFE/WARNING/ACTIVE 캡처 네 장을 이전과 비교했다.
**네 장 모두 각 480,000픽셀이 완전히 동일**하여 충돌 수정으로 그림이 바뀌지 않았음을 확인했다.
세 해상도에서 실제 창을 실행했고 800×600에서는 파란 삼각형 윤곽이 실제 가시와 맞는 것도 확인했다.
임시 윤곽은 Git 제외 GUI 도구에서만 그리며 일반 Main에는 디버그 표시를 추가하지 않았다.

기존 피격 GUI 도구도 다시 실행했다. 일반 1초 hurt/월드 정지/0.25초 흔들림과
마지막 목숨 즉시 GAME_OVER·중복 차감 방지가 세 해상도에서 통과했다 (45개 캡처).

- [시간제 ACTIVE 충돌 윤곽 800×600](../build/spike-collision-gui/active-overlay-800x600.png)
- [고정 가시 충돌 윤곽 800×600](../build/spike-collision-gui/fixed-overlay-800x600.png)
- [그림 변경 전](../build/spike-collision-gui/before-active-800x600.png)
- [그림 변경 후](../build/spike-collision-gui/after-active-800x600.png)

전체 8학기를 물리 키보드로 수동 플레이한 것은 아니다. 실제 경로는 기존 입력 기반 자동 검증으로 확인했다.
Git commit·push·merge는 실행하지 않았다.

## 이전 변경 기록: 가시 돌출 높이 55% (2026-11-07, 당시 허공 판정은 위 수정으로 해소)

이번 추가 변경 파일은 `TrapSprites.java`와 이 문서 두 개다.
Main, TimedTrap, StageFactory, 원본 PNG와 게임 규칙은 이전 상태를 유지했다.

가시의 높이는 받침대 위로 보이는 부분을 기준으로 한다.
고정/시간제 가시 모두 `SPIKE_HEIGHT_SCALE = 0.55`를 적용한다.

```text
baseHeight = min(16, trap.height * 0.5)
oldExposedHeight = trap.height - baseHeight
newExposedHeight = oldExposedHeight * 0.55
spikeBottomY = trap.y + baseHeight
visualTipY = spikeBottomY + newExposedHeight
collisionTipY = trap.y + trap.height  // 변경하지 않음
```

### 변경 전후 높이와 충돌 차이

단위는 월드 좌표이며, 같은 치수의 고정/시간제 가시에 동일한 값을 사용한다.

| 함정 높이 | 받침대 높이 (유지) | 돌출 높이 전 → 후 | 받침대 포함 전체 높이 전 → 후 | 충돌 꼭짓점과 그림 차이 |
| --- | --- | --- | --- | --- |
| 24 | 12 | 12 → 6.6 | 24 → 18.6 | 5.4 |
| 28 | 14 | 14 → 7.7 | 28 → 21.7 | 6.3 |
| 48 | 16 | 32 → 17.6 | 48 → 33.6 | 14.4 |
| 72 | 16 | 56 → 30.8 | 72 → 46.8 | 25.2 |

각 가시의 가로 중심은 `trap.x + (index + 0.5) * trap.width / 4`로 유지한다.
받침대 윗면에서의 기존 삼각형 너비도 유지하여 4개와 간격을 바꾸지 않았다.
공통 받침대 윗면에 붙인 가시 영역만 낮추며 재질 UV는 새 높이에 맞춰 같은 가로·세로 배율로 샘플링한다.
원본 전체를 세로로 압축하지 않는다. 클리핑 때문에 원본에서 보이는 재질 범위는 달라질 수 있다.

고정 가시의 반투명 받침대에는 기존 가시 색이 비쳐 보인다.
이 색을 유지하기 위해 기존 가시의 받침대 아래 부분만 UV를 보간해 잘라 먼저 그리고,
그 위로 나오는 가시는 새 높이로 따로 그린다. 받침대 자체의 텍스처·크롭·크기·좌표는 바꾸지 않았다.
SAFE/WARNING 및 빨간 위험등도 기존 크기·위치를 유지한다.

### 확인된 시각/충돌 불일치

**보이는 가시에 닿지 않아도 피격될 수 있다.** 충돌 박스를 유지하면서 그래픽만 줄인 결과다.
기존 네 삼각형의 꼭짓점과 그래픽 꼭짓점 사이에 위 표의 위험 구간이 남는다.
가장 높은 함정의 25.2단위 차이는 플레이어 충돌 높이 44의 약 57%로 큰 편이다.

실제 Stage 1~8 정의의 가시 27개를 ACTIVE로 진행시킨 후,
첫 번째 삼각형 가로 중심에 플레이어를 두고 발을 새 그림 꼭짓점보다 1단위 높게 놓았다.
30/60/120fps 모두 27개에서 `isDangerousTo()`가 true였다.
이는 게임 경로 테스트와 별도로 불일치를 측정한 배치 픽스처이며, 경로 통과를 위해 순간 이동한 것은 아니다.

예를 들어 Stage 4의 `(2010,60,144,72)` 함정은 기존 충돌 꼭짓점 Y=132를 유지하지만,
그림 꼭짓점은 Y=106.8이다. 발 Y=107.8의 플레이어도 기존 삼각형과 접촉한다.
충돌 영역·주기·목숨 처리 수정은 하지 않았다. 따라서 이번 변경은 이 불일치를 해결하지 않는다.

### 검증 결과

- `.\gradlew.bat build` 성공. 기존 8학기 및 30/60/120fps 회귀 검증 통과.
- 실제 SpriteBatch 출력 정점을 기록해 고정/시간제 네 치수 모두 돌출 높이 55%,
  바닥 Y, 네 개의 중심/간격을 검사했다.
- 실제 LWJGL 창을 세 해상도로 실행하고, 800×600에서 플레이어와 네 크기의 가시를 비교했다.
- 800×600 변경 전후 비교: SAFE/WARNING은 각각 전체 480,000픽셀이 완전히 동일했다.
- 시간제 ACTIVE 받침대의 네 영역 총 4,800픽셀이 완전히 동일했다.
- 고정 가시 받침대도 좌표·크기는 동일하다. 클리핑 UV 보간의 반올림 때문에 4,800픽셀 중
  4픽셀에서만 RGB 한 채널이 1단계 달랐고, 나머지 픽셀은 완전히 동일했다.
- 모든 함정 텍스처의 공유·Nearest·종료 시 해제 검사도 통과했다.

800×600 비교 캡처 (Git 제외):

| 종류 | 변경 전 | 변경 후 |
| --- | --- | --- |
| 시간제 ACTIVE | [전](../build/trap-height-gui/before-active-800x600.png) | [후](../build/trap-height-gui/after-active-800x600.png) |
| 고정 ACTIVE | [전](../build/trap-height-gui/before-fixed-800x600.png) | [후](../build/trap-height-gui/after-fixed-800x600.png) |

실제 키보드로 8학기 전체를 수동 플레이한 것은 아니다.
이하 내용은 초기 적용과 이전 받침대 정렬 보정 기록이며, 가시 높이는 위 최신 공식을 따른다.

## 변경 범위

- `core/src/main/java/io/jbnu/test/TrapSprites.java`: 공통 PNG 9개, 상태 선택, 받침대·경고등·가시 렌더링.
- `core/src/main/java/io/jbnu/test/Main.java`: 생성·SpriteBatch 렌더링·해제 연결.
- `core/src/main/java/io/jbnu/test/TimedTrap.java`: 기존 ShapeRenderer 렌더링 제거, 읽기 전용 `usesSpikeDesign()` 추가.
- `docs/SPIKE_TRAP_PIXEL_ART.md`: 적용·검증 및 시각적 차이 기록.

기존 `assets/pixel/traps/`의 파일을 그대로 사용했다. PNG를 수정하거나 새 이미지 리소스를 생성하지 않았다.
아직 Git에서 추적되지 않은 사용자 제공 리소스도 삭제하거나 덮어쓰지 않았다.

## PNG 9개와 선택 기준

| 길이 | SAFE | WARNING | ACTIVE |
| --- | --- | --- | --- |
| 1tile | safe_1tile.png (300×105) | warning_1tile.png (300×162) | active_1tile.png (381×161) |
| 2tile | safe_2tile.png (399×104) | warning_2tile.png (400×167) | active_2tile.png (477×166) |
| 3tile | safe_3tile.png (497×103) | warning_3tile.png (495×171) | active_3tile.png (572×170) |

월드 너비 64 이하는 1tile, 64 초과 112 이하는 2tile, 112 초과는 3tile을 선택한다.
이는 디자인 선택 기준이며 1tile을 반드시 월드 너비 64로 그린다는 의미가 아니다.

```text
width <= 64  -> 1tile
width <= 112 -> 2tile
otherwise    -> 3tile
```

고정 가시는 기존 `TimedTrap.fixedSpikes()`의 항상 ACTIVE인 상태를 그대로 읽는다.
시간제 함정은 `getState()`가 반환하는 SAFE/WARNING/ACTIVE에 해당하는 PNG를 사용한다.
렌더러에서 시간을 더하거나 상태를 변경하지 않는다.

## 기존 로직 확인 및 보존

현재 고정 가시와 시간제 가시는 모두 TimedTrap이 관리한다.
`spikeDesign`인 함정은 Rectangle 안의 직립 삼각형 네 개로 ACTIVE 접촉을 판정한다.
SAFE/WARNING은 해당 Rectangle과 겹쳐도 무피해다.
이전 테스트 맵용 일반 TimedTrap은 ACTIVE Rectangle 전체가 위험하다.

생성자·update·refreshState·reset·copyInitial·isDangerousTo는 수정하지 않았다.
모든 StageFactory 좌표, 너비·높이, safe/warning/active 시간과 시작 오프셋도 그대로다.
기존 피격·공통 사망 처리·플레이어·적·발판·점수·방학·GPA 로직은 변경하지 않았다.

## 받침대와 이미지 정렬

원본 PNG는 상태별 캔버스와 받침대 폭이 다르다. 전체 그림을 충돌 사각형에 늘리지 않고
받침대, 경고 장식, ACTIVE 가시 재질을 나눠 그린다.

받침대의 월드 영역은 모든 상태에서 동일하다.

```text
baseX = trap.x
baseY = trap.y
baseWidth = trap.width
baseHeight = min(16, trap.height * 0.5)
```

받침대 소스 범위 (2026-11-07 정렬 보정 반영):

- SAFE: 좌우 2px, 위 2px, 아래 3px 여백 제외.
- 시간제 함정은 SAFE/WARNING/ACTIVE 모두 해당 길이의 SAFE PNG와 위 크롭을 공유한다.
- 고정 가시만 기존 ACTIVE 받침대를 유지한다. 좌우 42/32/22px 제외, 시작 Y는 92/94/98px, 아래 3px 제외.

받침대 높이에 맞춰 각 조각을 균일 축소한다. 좌우 끝 80px 조각을 보존하고 가운데만 반복한다.
마지막 조각은 UV로 자른다. 이 과정에서 각 조각의 가로·세로 배율은 같다.
시간제 함정은 받침대의 PNG·크롭·배율·끝 조각·중앙 반복까지 모두 동일하다.
따라서 외곽 크기뿐 아니라 꽃·흙·끝 장식의 실제 표시 크기도 상태에 따라 바뀌지 않는다.

WARNING은 해당 PNG의 노란 패널 내부 재질을 공통 받침대 안의 높이 최대 3단위 띠로 반복해 표시한다.
해당 PNG의 상단 양 끝 96×60px 경고등/빛 조각도 받침대 윗면 위에 추가한다.
배율은 `min(0.28, width / 192)`로 제한한다. 노란색 패널과 경고등만 표시하고 가시는 그리지 않는다.
시간제 ACTIVE는 해당 PNG의 양 끝 72×60px 빨간 위험등 조각을 받침대 윗면 위에 추가한다.
배율은 `min(0.22, width / 144)`이다. 두 종류의 등은 받침대를 확대하지 않는다.
SAFE 역시 받침대만 표시한다. 받침대/경고 장식은 충돌체가 아니다.

## ACTIVE의 높이와 위험 영역

기존 함정은 24×24, 56×24, 96×48, 144×72 등 여러 비율이다.
특히 144×72에 원본 전체를 너비만 맞추면 그림 위에 보이지 않는 위험 영역이 남는다.
반대로 전체 그림의 높이를 맞추면 이웃 안전 공간까지 그림이 퍼진다.

이를 피하기 위해 원본 ACTIVE 가시 내부의 분홍 재질을 균일 배율로 샘플링하고,
기존 네 삼각형의 모양으로 잘라 SpriteBatch 정점으로 그린다.
삼각형 하나는 사각형의 네 번째 정점을 중복시켜 표현하며 별도 라이브러리는 사용하지 않는다.

```text
cellWidth = trap.width / 4
triangleBaseY = trap.y
triangleTipY = trap.y + trap.height
triangleCenterX = trap.x + (index + 0.5) * cellWidth
```

픽셀 재질의 가로·세로 샘플 배율을 같게 유지하므로 PNG의 전체 그림을 비율에 맞지 않게 압축하지 않는다.
기존 위험 영역에 맞춰 원본 실루엣의 양 옆을 잘라내는 방식이다.
원본의 어두운 테두리 픽셀을 사용해 밝은 배경에서도 삼각형 경계가 보이게 한다.
시간제 함정은 원래 삼각형을 받침대 윗면에서 잘라 그 위쪽 부분만 표시한다.
잘린 위치의 너비는 `cellWidth * (1 - baseHeight / trapHeight)`로 계산해 원래 삼각형의 경사와 꼭짓점을 유지한다.
공통 받침대의 둥근 모서리 아래로 가시가 비쳐 받침대가 커 보이지 않도록 한다.
고정 가시의 기존 전체 삼각형/ACTIVE 받침대 렌더링은 유지한다.
받침대 맨 아래와 삼각형 맨 위가 각각 기존 Y와 Y+height에 대응한다.
기존 테스트용 사각형 함정은 ACTIVE 재질로 Rectangle 전체를 채워 위험 영역을 숨기지 않는다.

이 모든 처리는 렌더링에만 적용한다. 실제 위험 판정은 TimedTrap의 기존 코드가 계속 담당한다.

## 리소스 및 렌더링 순서

- Main.create()에서 TrapSprites 한 개가 PNG 9개를 한 번씩 로드한다.
- 모든 함정과 학기·재시작이 동일한 텍스처를 공유한다.
- TextureFilter.Nearest를 축소/확대에 모두 적용한다.
- 렌더링 중 텍스처나 상태별 그림을 새로 생성하지 않는다. 정점 버퍼도 재사용한다.
- 로드 실패 시 파일 경로를 포함해 오류를 내고 먼저 로드된 텍스처를 해제한다.
- 기존 월드 투영 행렬과 같은 SpriteBatch를 사용한다.
- 기존처럼 발판·일반 플레이어 뒤에 함정을 그리고, 적·아이템·피격 플레이어·HUD는 그 위에 그린다.
- 기존 월드 레이어 순서는 그대로이며 중간 ShapeRenderer 호출만 없애 한 SpriteBatch 구간으로 그린다.
- Main.dispose()에서 PNG 9개를 모두 해제한다.

## 자동 검증 결과

2026-11-07 상태별 정렬 보정 후 `.\gradlew.bat build` 종료 코드 0.
기존 검증 코드는 변경하거나 조건을 완화하지 않았다.

| 기존 회귀 검증 | 30fps | 60fps | 120fps |
| --- | --- | --- | --- |
| Stage 1~8 실제 입력 경로·수집·학기 전환 | 통과 | 통과 | 통과 |
| 함정 상태/주기·SAFE/WARNING 무피해·ACTIVE/고정 접촉 | 통과 | 통과 | 통과 |
| 일반 피격 1초·처음 0.25초 흔들림·월드 정지 | 통과 | 통과 | 통과 |
| 마지막 목숨 즉시 GAME_OVER·사망당 한 번 차감 | 통과 | 통과 | 통과 |
| 재시작·목숨·점수·방학 미니게임/효과 | 통과 | 통과 | 통과 |
| 성적표·465점·GPA 4.50·졸업 엔딩 | 통과 | 통과 | 통과 |

추가 GUI 도구에서도 30/60/120fps로 실제 TimedTrap.update를 여러 주기 호출해
세 상태를 모두 거치는지, 반복 전환·좌표/크기 보존·상태별 접촉·reset 복원이 정상인지 검사했다.
git diff로 상태 계산·충돌 알고리즘·StageFactory가 변경되지 않은 것도 확인했다.

## 실제 GUI 검증

실제 LWJGL 창에서 다음 두 검증을 실행했다.

1. 길이 24/56/96/144, 높이 24/24/48/72인 네 함정의 고정/SAFE/WARNING/ACTIVE 장면을
   640×480·800×600·1280×720에서 캡처했다. 상태별 12장에 각 길이가 함께 보인다.
2. 기존 피격 GUI 검증을 실행해 적/고정/시간제 접촉의 일반·마지막 목숨을 세 해상도에서 확인했다.
   45장의 피격·재시작·GAME_OVER 캡처를 생성하고 일반 1초, HUD 고정 흔들림, 마지막 목숨의 무지연 전환을 검사했다.

대표 캡처를 직접 열어 PNG 출력, 투명 배경, 바닥 정렬, SAFE/WARNING의 가시 부재,
WARNING 노란색 패널·경고등, ACTIVE 분홍색 가시와 피격 hurt의 겹침 순서를 확인했다.
공통 렌더러/Nearest 텍스처 재사용과 종료 후 9개 GL 핸들이 모두 0이 되는 것도 검사했다.

독립 상태 장면은 GUI 확인용 임시 바닥/함정 픽스처다. 실제 StageFactory를 수정한 것은 아니다.
전체 스테이지 통과는 기존 입력 기반 회귀 검증이 따로 확인한다.
물리 키보드로 8학기를 전체 수동 플레이하지는 않았다.

캡처와 임시 검증 소스는 Git 제외 경로에 있다.

- [SAFE 640×480](../build/trap-gui/safe-640x480.png)
- [WARNING 800×600](../build/trap-gui/warning-800x600.png)
- [ACTIVE 800×600](../build/trap-gui/active-800x600.png)
- [ACTIVE 1280×720](../build/trap-gui/active-1280x720.png)
- [고정 가시 0.9초 피격](../build/hit-gui/fixed-hurt-09s-800x600.png)
- [시간제 가시 피격](../build/hit-gui/timed-settled-640x480.png)
- [마지막 목숨 GAME_OVER](../build/hit-gui/last-fixed-gameover-1280x720.png)

## 남은 시각적 차이 및 수동 확인

- 원본 PNG의 가시 개수와 넓고 둥근 실루엣을 그대로 복사하지 않는다.
  기존 네 삼각형과 높이를 보존하기 위해 재질을 잘라 사용하므로 높은 함정은 더 길고 가늘다.
- 원본의 흰색 하이라이트가 커서 분홍색 중앙이 밝게 보인다. 어두운 테두리로 위험 경계를 보완했다.
- 짧은 함정에서는 꽃·패널의 세부가 작게 보이며 긴 받침대에는 중앙 무늬 반복이 있다.
- 다른 지형의 좁은 대기 공간, 카메라 이동, 창 크기 연속 변경에서 경고등/가시 가독성은
  실제 키보드 플레이로 추가 확인할 수 있다.

Git commit·push·merge는 실행하지 않았다.

## 2026-11-07 상태별 받침대 공통화

### 원인과 변경 전후

기존 코드도 받침대의 목적 사각형은 같았으나, 각 상태마다 다른 높이의 소스 크롭을 같은 높이로 확대했다.
SAFE의 크롭 높이는 1/2/3tile 순서로 100/99/98px, ACTIVE는 66/69/69px였다.
따라서 ACTIVE에서 배율이 약 1.52/1.43/1.42배 커져 끝 장식·꽃·흙이 확대되어 보였다.
캔버스의 투명 여백과 받침대 비율 차이를 목적 사각형만 같게 만드는 것으로 해결하지 못한 것이다.

보정 후 시간제 함정은 세 상태가 동일한 SAFE 받침대를 사용한다.
WARNING은 노란 띠/등, ACTIVE는 가시/빨간 등만 별도로 추가한다.
SAFE의 기존 크기와 정렬을 기준으로 삼았으며 PNG 파일, 충돌 박스, 함정 주기는 수정하지 않았다.
이번 추가 변경 파일은 `TrapSprites.java`와 이 문서 두 개다. Main/TimedTrap은 이전 구현을 유지했다.

### 상태별 실제 받침대 크기

바닥 기준선은 모두 기존 `trap.y`, 가로 시작점은 모두 기존 `trap.x`이다.
다음은 같은 위치를 사용한 GUI 비교 픽스처의 월드 좌표다.

| 함정 크기 | 받침대 X/Y | SAFE 받침대 | WARNING 받침대 | ACTIVE 받침대 | ACTIVE 꼭짓점 Y |
| --- | --- | --- | --- | --- | --- |
| 24×24 | 140 / 60 | 24×12 | 24×12 | 24×12 | 84 |
| 56×24 | 200 / 60 | 56×12 | 56×12 | 56×12 | 84 |
| 96×48 | 340 / 60 | 96×16 | 96×16 | 96×16 | 108 |
| 144×72 | 510 / 60 | 144×16 | 144×16 | 144×16 | 132 |

시간제 함정의 모든 상태에서 같은 공식과 같은 받침대 이미지를 사용한다.
가시/등이 위로 나타나는 높이 변화는 상태 표현이며 받침대 확대가 아니다.

### 추가 검증

- 기존 30/60/120fps 경로·상태·안전/피격·목숨·성적표 검증 통과.
- 실제 GUI를 세 해상도로 실행하고, 800×600에서 동일 위치의 네 너비를 세 상태로 캡처했다.
- 800×600 픽셀 비교: 네 너비 모두 SAFE/ACTIVE 받침대 영역 전체가 완전히 동일했다.
- WARNING은 의도한 노란 패널 띠를 제외한 받침대 영역이 SAFE와 완전히 동일했다.
- 고정 가시의 변경 전후 800×600 장면은 전체 480,000픽셀이 완전히 동일했다.
- 아홉 텍스처의 공유/Nearest/해제와 함정 주기·좌표 보존 검증도 다시 통과했다.

변경 전후 비교 캡처 (Git 제외):

| 상태 | 변경 전 | 변경 후 |
| --- | --- | --- |
| SAFE | [전](../build/trap-alignment-gui/before-safe-800x600.png) | [후](../build/trap-alignment-gui/after-safe-800x600.png) |
| WARNING | [전](../build/trap-alignment-gui/before-warning-800x600.png) | [후](../build/trap-alignment-gui/after-warning-800x600.png) |
| ACTIVE | [전](../build/trap-alignment-gui/before-active-800x600.png) | [후](../build/trap-alignment-gui/after-active-800x600.png) |

기존 네 삼각형 충돌에 맞춘 가늘고 긴 가시, 작은 받침대의 세부 무늬 축소 같은 시각적 차이는 여전히 있다.
이번 수정으로 충돌 영역이나 PNG 원본을 변경하지 않았다.
