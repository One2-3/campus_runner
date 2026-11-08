# 미니게임 한글 폰트 원본

타이틀·방학·엔딩 모두 같은 OFL 원본을 사용한다. 각각 Campus Title Korean, Campus Vacation Korean, Campus Ending Korean으로 이름을 바꾼 별도 부분집합과 OFL을 생성한다. 출력 경로를 분리하여 다른 화면의 아틀라스를 덮어쓰지 않는다.

- 원본: Google Fonts의 NanumGothic-Bold.ttf (https://github.com/google/fonts/tree/main/ofl/nanumgothic)
- 저작권: Copyright (c) 2010, NHN Corporation.
- 라이선스: SIL Open Font License 1.1, [OFL.txt](OFL.txt).
- 원본 SHA-256: `f96298f9fb18e364d2370f4c3ce948ac67a2b61af992d7234bc15c42b033c674`.
- 수정하지 않은 원본 TTF는 재생성 도구용이며 assets 안에 넣지 않는다.
- 변환한 부분집합 이름은 **Campus Vacation Korean**으로 원본의 Reserved Font Name을 사용하지 않는다.
- 생성 파일 `assets/fonts/vacation-ko.fnt`, `vacation-ko.png`와 함께 저작권/라이선스 전문 `vacation-ko-OFL.txt`를 배포한다.
- 재생성: 저장소 루트에서 `powershell -ExecutionPolicy Bypass -File tools/GenerateVacationFont.ps1`.

STEP 10-16에서 ending-ko의 Windows 시스템 폰트 의존성을 제거했다. 재생성은 저장소 루트에서 `& .\tools\GenerateEndingFont.ps1`을 실행한다. 181글리프의 ending-ko.fnt/png와 ending-ko-OFL.txt를 생성한다. 도구는 원본 TTF 해시를 검사하며 새 다운로드를 하지 않는다. 상세 출처·사용 화면은 [FONT_PROVENANCE.md](../../packaging/FONT_PROVENANCE.md)를 참고한다.
