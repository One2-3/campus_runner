# 미니게임 한글 폰트 원본

타이틀에도 같은 OFL 원본을 사용한다. 별도 `Campus Title Korean` 부분집합과 라이선스를 `assets/fonts/title-ko.*`, `title-ko-OFL.txt`에 생성한다. 기존 방학·엔딩 아틀라스는 수정하지 않는다. 재생성 명령은 `docs/TITLE_SCREEN.md`를 참고한다.

- 원본: Google Fonts의 NanumGothic-Bold.ttf (https://github.com/google/fonts/tree/main/ofl/nanumgothic)
- 저작권: Copyright (c) 2010, NHN Corporation.
- 라이선스: SIL Open Font License 1.1, [OFL.txt](OFL.txt).
- 원본 SHA-256: `f96298f9fb18e364d2370f4c3ce948ac67a2b61af992d7234bc15c42b033c674`.
- 수정하지 않은 원본 TTF는 재생성 도구용이며 assets 안에 넣지 않는다.
- 변환한 부분집합 이름은 **Campus Vacation Korean**으로 원본의 Reserved Font Name을 사용하지 않는다.
- 생성 파일 `assets/fonts/vacation-ko.fnt`, `vacation-ko.png`와 함께 저작권/라이선스 전문 `vacation-ko-OFL.txt`를 배포한다.
- 재생성: 저장소 루트에서 `powershell -ExecutionPolicy Bypass -File tools/GenerateVacationFont.ps1`.

기존 `ending-ko` 리소스와 그 생성 도구는 이 작업에서 수정하지 않는다.
