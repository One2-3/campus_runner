# 배포 폰트 출처와 고지

## 직접 생성한 한글 아틀라스

세 아틀라스 모두 기존 `tools/fonts/NanumGothic-Bold.ttf`를 사용한다. 새 폰트를 다운로드하거나 Windows 시스템 폰트를 읽지 않는다.

- 원본 출처: [Google Fonts / Nanum Gothic](https://github.com/google/fonts/tree/main/ofl/nanumgothic).
- 권리자: Copyright (c) 2010, NHN Corporation.
- 원본 SHA-256: `f96298f9fb18e364d2370f4c3ce948ac67a2b61af992d7234bc15c42b033c674`.
- 라이선스: SIL Open Font License 1.1. 원문 `tools/fonts/OFL.txt`, 배포 파일의 `*-OFL.txt`와 동일하다.
- 조건: 폰트 단독 판매 금지, 고지·라이선스 유지, 수정본은 Reserved Font Name을 사용하지 않음, 폰트 파생물 자체는 OFL 유지. 게임 전체에 OFL을 적용하는 것은 아니다.
- TTF는 생성 도구용으로 저장소에만 있고 app JAR/ZIP에는 넣지 않는다.

| 이름 | 파일 | 생성 도구 | 사용 화면 |
| --- | --- | --- | --- |
| Campus Title Korean | title-ko.fnt/png, title-ko-OFL.txt | GenerateVacationFont.ps1의 title-ko 옵션 | 타이틀·게임 방법 |
| Campus Vacation Korean | vacation-ko.fnt/png, vacation-ko-OFL.txt | GenerateVacationFont.ps1 기본 옵션 | 방학 미니게임 3종의 시작·플레이·결과 |
| **Campus Ending Korean** | ending-ko.fnt/png, ending-ko-OFL.txt | GenerateEndingFont.ps1 → 공통 생성기 | 최종 성적표·졸업 엔딩 5종의 한글 |

엔딩 폰트 교체일: 2026-11-08. 기존 맑은 고딕 파생 아틀라스의 재배포 문제를 해결하기 위해 새 OFL 파생물을 생성했다. 예전 아틀라스에 OFL 고지만 덧붙인 것이 아니다. 새 엔딩 아틀라스는 181글리프, 512×432 RGBA, 24px Bold, lineHeight 36/base 28, 32×36 셀이다. 엔딩 문구·판정·레이아웃 좌표·폰트 로딩 경로는 유지한다.

새 ending-ko.png SHA-256: `16abc0e59c4d569b4783485c719ada2acd8098afd85816e634c2dd500326f694`.

생성기는 원본 TTF 해시가 달라지면 실패한다. 재생성 후 OFL을 함께 복사하며, Gradle 검증은 새 폰트 이름·고지·모든 엔딩 글리프를 확인한다.

## LibGDX 기본 영문 폰트

Main, PixelHudRenderer, PauseMenuRenderer, GameOverRenderer, StageClearRenderer 및 PixelResultCanvas가 생성하는 `new BitmapFont()`는 **LibGDX 1.13.1 내장 LSans 15px**를 사용한다. Arial이라고 추정하지 않았다. 실제 JAR의 `com/badlogic/gdx/utils/lsans-15.fnt/png`와 해당 버전 BitmapFont 소스를 대조했다.

[상류 교체 기록 #6727](https://github.com/libgdx/libgdx/pull/6727)은 LSans가 OFL Liberation Sans의 이름을 바꾼 파생물임을 명시한다. 원 저작권과 OFL 전문은 배포 `Liberation-OFL-1.1.txt`에 보완했다. [Liberation Fonts의 OFL 원문](https://github.com/liberationfonts/liberation-fonts/blob/2.1.5/LICENSE)을 사용하며, 내장 비트맵 원본 TTF의 세부 버전까지 식별했다는 뜻은 아니다. LSans 이미지와 메트릭은 변경하지 않았다.

폰트 배포 고지 목록: `ending-ko-OFL.txt`, `title-ko-OFL.txt`, `vacation-ko-OFL.txt`, `Liberation-OFL-1.1.txt`, 이 파일. 원문·저작권은 요약문으로 대체하지 않는다.
