# 움직이는 발판 픽셀 아트

## 적용 범위 및 변경 파일

- `core/src/main/java/io/jbnu/test/Main.java`: 공유 렌더러 생성·SpriteBatch 호출·해제.
- `core/src/main/java/io/jbnu/test/MovingPlatform.java`: 단색 도형 렌더링 제거, 불변 경로를 읽는 `isHorizontal()` 추가.
- `core/src/main/java/io/jbnu/test/MovingPlatformRenderer.java`: 이미지 선택·비율 유지·윗면 정렬.
- `docs/MOVING_PLATFORM_PIXEL_ART.md`: 적용 방식과 검증 기록.

이동 발판의 update/reset/cancelMove 및 Player의 탑승·착지·측면 충돌·압착 방지 코드는 변경하지 않았다.
Stage 1~8 데이터, 기존 PNG, 고정 발판 등 다른 오브젝트 그래픽과 게임 규칙도 변경하지 않았다.

## 이미지 선택

`assets/pixel/platforms/`의 기존 투명 PNG를 사용한다.

| 조건 | 이미지 | 원본 크기 | 나무 윗면 기준 행 (위에서부터) |
| --- | --- | --- | --- |
| 수평, 충돌 너비 150 미만 | moving_wood_short.png | 423×172 | 8 |
| 수평, 충돌 너비 150 이상 | moving_wood_long.png | 768×173 | 8 |
| 수직 | moving_wood_hanging.png | 492×294 | 160 |

현재 이동 방향이나 프레임 이동량 대신 시작·끝 Y가 같은지 확인한다.
따라서 초기 정지 프레임, 일시정지, 끝점 반전에서도 그림이 바뀌지 않는다.
체인과 아래 금속 장식에는 충돌 영역을 만들지 않는다.

## 크기와 정렬

PNG 전체를 충돌 사각형에 가로·세로로 각각 늘리지 않는다.
긴 수평 발판은 기존의 한 장 균일 축소를 유지한다.
짧은 수평 발판은 이전 배율의 75%, 수직 발판은 70%로 각 조각을 균일 축소한다.
줄어드는 가로 길이는 장식이 없는 가운데 나무 부분만 반복해 채운다.
따라서 원본 픽셀과 체인·끝 장식의 비율을 유지하면서 전체 너비는 충돌 박스와 일치한다.
원본의 나무 윗면 기준 행이 충돌 박스의 `y + height`와 일치하도록 Y를 계산한다.

```text
heightFactor = short ? 0.75 : vertical ? 0.70 : 1.0
scale = collisionWidth / textureWidth * heightFactor
drawWidth = collisionWidth
drawHeight = textureHeight * scale
drawX = collisionX
drawY = collisionY + collisionHeight - (textureHeight - surfaceRow) * scale
```

변경 전후 크기 (전체 그림 경계, 월드 단위):

| 발판 | 변경 전 | 변경 후 | 높이 변화 |
| --- | --- | --- | --- |
| 짧은 수평 너비 140 | 140×56.9 | 140×42.7 | -25% |
| 짧은 수평 너비 120 | 120×48.8 | 120×36.6 | -25% |
| 긴 수평 너비 160 | 160×36.0 | 160×36.0 | 유지 |
| 수직 너비 160 | 160×95.6 | 160×66.9 | -30% |
| 수직 너비 140 | 140×83.7 | 140×58.6 | -30% |
| 수직 너비 120 | 120×71.7 | 120×50.2 | -30% |

짧은 PNG의 좌우 160픽셀, 수직 PNG의 좌우 150픽셀을 끝 조각으로 사용한다.
장식이 없는 중앙 영역(각각 103/192픽셀 너비)을 같은 배율로 반복하고 마지막 조각은 UV로 잘라낸다.
조각을 가로·세로로 다르게 확대/축소하지 않으며, 새 PNG를 만들거나 원본을 수정하지 않는다.
체인도 나무와 함께 균일 축소되어 너비 160 수직 발판의 윗면 위 장식은 약 52에서 36.4단위로 줄었다.

이번 크기 조정에서 추가로 변경한 파일은 `MovingPlatformRenderer.java`와 이 문서 두 개다.
`MovingPlatform.java`와 `Main.java`는 앞선 PNG 적용 상태를 그대로 유지했다.

그림의 전체 높이는 기존 충돌 높이 20과 다르다. 체인·둥근 모서리·아래 장식이 사각형 밖으로 보이지만,
탑승 윗면과 기존 충돌 사각형은 유지한다. 체인 위로 걸어 올라가거나 아래 장식 위에 착지할 수 없다.
플레이어의 32×44 충돌 박스와 스프라이트 기준점도 그대로다.

## 렌더링 및 리소스

- Main.create()에서 텍스처 세 장을 한 번 로드한다. 모든 학기·발판·재시작이 공유한다.
- 축소/확대 필터 모두 `TextureFilter.Nearest`를 사용한다.
- 로드 오류에는 실패한 파일 경로를 포함하고, 먼저 로드된 텍스처를 해제한다.
- 고정 발판 → 이동 발판 → 깃발·플레이어 → 가시·적 → 학점 아이템·피격 플레이어 → HUD 순서로 그린다.
- 이동 발판/체인은 플레이어·위험 요소·아이템을 덮지 않는다.
- 기존 월드 카메라/피격용 투영 행렬을 그대로 사용하며 별도 좌표 반올림이나 이동 보정은 하지 않는다.
- SpriteBatch와 ShapeRenderer의 begin/end는 분리한다.
- Main.dispose()에서 렌더러의 세 텍스처를 해제한다.

## 자동 검증 결과

`2026-11-06`, `.\gradlew.bat build` 종료 코드 0.
기존 검증 코드는 수정하거나 완화하지 않았다.

| 검증 | 30fps | 60fps | 120fps |
| --- | --- | --- | --- |
| Stage 1~8 실제 입력 경로, 수집·학기 전환 | 통과 | 통과 | 통과 |
| 수평·수직 탑승/하차·왕복·발판 갈아타기 | 통과 | 통과 | 통과 |
| 적·가시·낙하·피격·목숨·재시작 | 통과 | 통과 | 통과 |
| 방학 미니게임 3종·효과 | 통과 | 통과 | 통과 |
| 성적표·465점·GPA 4.50·졸업 엔딩 | 통과 | 통과 | 통과 |
| 추가 측면 충돌·벽 밀림/천장 압착 방지 픽스처 | 통과 | 통과 | 통과 |

추가 충돌 픽스처는 실제 Player.update/MovingPlatform.update를 사용한다.
오른쪽 입력으로 이동 발판 옆면에 접근해 정지하는지, 수평 발판이 플레이어를 벽으로 밀거나
수직 발판이 천장으로 올릴 때 cancelMove가 작동하고 플레이어가 겹치지 않는지 확인했다.
렌더링은 충돌 데이터를 읽기만 하며, git diff로 경로·속도·물리 구현이 그대로인지 확인했다.

## 실제 GUI 확인 결과

실제 LWJGL 창에서 짧은 수평(Stage 2), 긴 수평(Stage 3), 체인 수직(Stage 3) 발판을
640×480·800×600·1280×720으로 실행했다. 각 왕복 끝점 이후를 캡처해 총 18장을 생성했다.
초기 PNG 적용에서는 세 종류의 대표 이미지를 각 해상도에서 열어 확인했다.
이번 크기 조정 후에도 같은 도구로 세 해상도에서 18장을 다시 캡처했고,
800×600의 세 종류 이미지를 변경 전 캡처와 비교해 크기·투명 배경·발 위치·레이어를 확인했다.

GUI 픽스처는 확인할 발판 위에 플레이어를 초기 배치한 뒤 실제 갱신으로 착지하고 왕복시킨다.
전체 경로의 입력 기반 탑승/하차는 기존 8학기 회귀 검증이 별도로 확인한다.
GUI에서는 입력 없이도 발판과 함께 이동하는지, 수직 상승·하강 양쪽에서 발 위치/수평 상대 위치가
유지되는지, 양 끝에서 반전 후 grounded가 유지되는지 확인했다.

발과 나무 윗면이 자연스럽게 닿고, 체인이 플레이어를 가리지 않는 것을 대표 장면에서 확인했다.
짧은 발판과 체인 발판의 높이가 줄고 긴 발판의 두께는 유지되었다.
짧은/수직 발판 중앙 나뭇결에는 반복 무늬가 생기지만 확인한 장면에서 틈이나 비율 찌그러짐은 없었다.
학기 전환/재시작 시 같은 렌더러와 텍스처를 재사용하며, 종료 후 GL 텍스처 핸들 세 개가 모두 0임을 검사했다.

캡처와 GUI 검증 도구는 Git에서 제외된 `build/moving-platform-gui/`에 있다.

- [짧은 수평 800×600](../build/moving-platform-gui/short-horizontal-end0-800x600.png)
- [긴 수평 640×480](../build/moving-platform-gui/long-horizontal-end0-640x480.png)
- [수직 상단 800×600](../build/moving-platform-gui/hanging-vertical-end0-800x600.png)
- [수직 하단 640×480](../build/moving-platform-gui/hanging-vertical-end1-640x480.png)
- [수직 상단 1280×720](../build/moving-platform-gui/hanging-vertical-end0-1280x720.png)

800×600 크기 비교용으로 변경 전후 캡처를 별도 보존했다 (Git 제외).

| 발판 | 변경 전 | 변경 후 |
| --- | --- | --- |
| 짧은 수평 | [전](../build/moving-platform-size-gui/short-before-800x600.png) | [후](../build/moving-platform-size-gui/short-after-800x600.png) |
| 수직 | [전](../build/moving-platform-size-gui/vertical-before-800x600.png) | [후](../build/moving-platform-size-gui/vertical-after-800x600.png) |
| 긴 수평 | [전](../build/moving-platform-size-gui/long-before-800x600.png) | [후](../build/moving-platform-size-gui/long-after-800x600.png) |

물리 키보드로 8학기 전체를 수동 플레이한 것은 아니다. 남은 수동 확인 항목:

- 실제 연속 점프/갈아타기에서 나무 두께와 발끝 접촉감 확인.
- 다른 학기의 좁은 구간에서 장식이 충돌 영역으로 오해되지 않는지 확인.
- 이동 중 창 크기 변경·피격 흔들림에서 픽셀 가장자리와 체인의 가독성 확인.

Git commit·push·merge는 실행하지 않았다.
