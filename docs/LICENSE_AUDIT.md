# Campus Runner — 폰트 및 공개 배포 라이선스 점검 (STEP 10-16)

> 최신 판단은 [STEP 10-17/18 최종 공개 점검표](PUBLIC_RELEASE_REVIEW.md)를 기준으로 한다. 사용자 정책은 자체 소스에 별도 오픈소스 사용권을 부여하지 않는 것으로 확정됐다. 아래 패키지 해시는 STEP 10-16 당시 이력이다. 최초 목록의 IDE bin/main 복사본 41개를 제외하고 ICO/ICNS 2개를 추가해 원본 리소스 목록을 53개로 정정했다.

확인일: 2026-11-08. 이번 작업은 맑은 고딕 기반 엔딩 폰트를 교체하고, 실제 배포 파일의 근거와 미확인 사항을 구분하는 작업이다. 프로젝트 전체 라이선스를 임의로 결정하지 않았으며 Release를 게시하지 않았다.

## 1. 해결한 폰트 문제

기존 GenerateEndingFont.ps1은 Windows Fonts/malgun.ttf에서 글자별 아틀라스를 만들었다. [Microsoft 공식 FAQ](https://learn.microsoft.com/en-us/typography/fonts/font-faq)의 일반 허용 범위와 맞지 않아 재사용하지 않았다.

이미 보유한 NanumGothic-Bold.ttf를 사용하여 **Campus Ending Korean**으로 재생성했다. 원문 OFL을 기존 폰트에 붙인 것이 아니라 폰트 픽셀을 새 원본에서 다시 생성했다. 새 폰트나 TTF를 다운로드하지 않았고, Windows 시스템 폰트 의존성을 제거했다.

| 항목 | 교체 전 | 교체 후 |
| --- | --- | --- |
| 원본 | Windows 맑은 고딕 | 기존 tools/fonts/NanumGothic-Bold.ttf |
| 파생물 이름 | Ending Korean Bitmap | Campus Ending Korean |
| 권리 근거 | Windows 글자별 비트맵 변환 문제 | NHN, SIL OFL 1.1 |
| 파일 | ending-ko.fnt/png | 같은 로딩 경로, 새 내용 + ending-ko-OFL.txt |
| 메트릭 | 24px / lineHeight 36 / base 28 | 24px Bold / lineHeight 36 / base 28 |
| 글리프/아틀라스 | 181 / 512×432 | 181 / 512×432 |
| 문구·판정·배치 코드 | 기존 5종 | 변경 없음 |

원본 SHA-256: `f96298f9fb18e364d2370f4c3ce948ac67a2b61af992d7234bc15c42b033c674`.

새 ending-ko.png SHA-256: `16abc0e59c4d569b4783485c719ada2acd8098afd85816e634c2dd500326f694`.

```powershell
& .\tools\GenerateEndingFont.ps1
```

공통 생성기는 원본 해시를 검사하고 OFL을 함께 복사한다. 다른 폰트를 조용히 대체하지 않는다. 엔딩 렌더러의 문구·색상·좌표·입력은 그대로 두었다. 한글 획이 Bold로 바뀌었으나 세 해상도에서 문구가 패널 안에 들어옴을 확인했다.

## 2. 전체 폰트 현황

| 폰트 | 원본·생성 경로 | 사용 화면 | 배포 파일/권리 |
| --- | --- | --- | --- |
| Campus Ending Korean | 기존 NanumGothic-Bold → GenerateEndingFont → 공통 생성기 | 최종 성적표·5종 엔딩 한글 | ending-ko.fnt/png/OFL, SIL OFL 1.1 |
| Campus Title Korean | 같은 원본 → 공통 생성기 title-ko 옵션 | 타이틀·게임 방법 | title-ko.fnt/png/OFL, SIL OFL 1.1 |
| Campus Vacation Korean | 같은 원본 → 공통 생성기 기본 옵션 | 미니게임 3종 시작·플레이·결과 | vacation-ko.fnt/png/OFL, SIL OFL 1.1 |
| LSans (Liberation Sans 파생물) | LibGDX 1.13.1 JAR의 lsans-15.fnt/png | HUD·정지·게임 오버·학기 클리어·영문 성적표/엔딩 | 라이브러리 안에 포함, OFL 원문 추가 동봉 |

LibGDX 기본 폰트를 Arial로 추측하지 않았다. 해당 버전 BitmapFont 소스와 실제 JAR, [상류 PR #6727](https://github.com/libgdx/libgdx/pull/6727)을 확인했다. LSans는 OFL Liberation Sans의 이름을 바꾼 파생물이다. 원 저작권을 포함한 Liberation-OFL-1.1.txt를 추가했다. 내장 원본 TTF의 세부 버전까지 확인했다는 뜻은 아니다.

나눔고딕의 [원문 OFL](https://github.com/google/fonts/blob/main/ofl/nanumgothic/OFL.txt)은 게임과 함께 배포/임베딩을 허용하며, 파생 폰트 이름·고지·OFL 유지와 폰트 단독 판매 금지 조건이 있다. 게임 전체를 OFL로 바꾸지 않는다. TTF는 저장소 생성 도구용이며 ZIP에는 포함하지 않는다. 상세 출처는 [FONT_PROVENANCE.md](../packaging/FONT_PROVENANCE.md).

## 3. 이미지·리소스 확인표

사용자는 게임 PNG·런처 아이콘에 대해 **“전부 AI 생성물”, “gpt로 파일 올린날”**이라고 설명했다. GPT 생성 출처와 생성일에 대한 이 진술을 기록했다. 정확한 이미지 모델 이름과 달력 날짜는 알려지지 않았으므로 파일 수정/커밋 시각을 생성 시각으로 바꾸어 기록하지 않았다.

개별 경로·SHA-256·실제 배포 포함 여부는 [ASSET_INVENTORY.csv](../packaging/ASSET_INVENTORY.csv)의 53개 원본 항목에 있다. IDE bin/main 복사본은 원본이나 아이콘으로 중복 집계하지 않는다.

| 항목 | 개수 | 출처 | 라이선스·재배포 판단 |
| --- | ---: | --- | --- |
| 계절 배경 | 8 PNG | 사용자 확인 GPT 생성 | 아래 OpenAI 약관 근거, 원생성 기록은 추가 확인 |
| 플레이어 | 4 PNG | 동일 | 동일 |
| 과제 몬스터 | 1 PNG | 동일 | 동일 |
| A+ 아이템 | 1 PNG | 동일 | 동일 |
| 고정/이동 발판 | 7 PNG | 동일 | 동일 |
| 가시/함정 | 9 PNG | 동일 | 동일 |
| legacy block/flag/player/libgdx | 4 PNG | 동일한 사용자 진술로 기록 | 실제 원본이 템플릿/제3자 자료인 경우 그 고지가 별도로 적용됨 |
| 런처 아이콘 | PNG 5개·ICO/ICNS 각 1개 | 동일한 사용자 진술로 기록 | 리소스 PNG 4개는 JAR, 빌드 아이콘 3개는 ZIP 미포함 |
| 폰트 아틀라스·정의·OFL | 9개 | 위 표의 원본과 도구 | SIL OFL 1.1, AI 이미지와 분리 |
| 셰이더 2개·생성 리소스 목록 | 3개 | 저장소 코드/빌드 | 자체 코드 정책 미정 |

OpenAI Docs의 이미지 문서를 먼저 확인했으나 기술 가이드만으로 권리 조건이 정해지지는 않으므로 [OpenAI 일반 이용약관](https://openai.com/policies/terms-of-use/)의 Content 조항을 직접 확인했다. 확인한 페이지의 효력일은 2026-01-01이다. 법이 허용하는 범위에서 OpenAI와 사용자 사이의 출력 권리를 사용자에게 귀속시키는 조항은 게임에 활용할 계약상 근거가 된다. 출력의 독점성·법률상 저작권 성립·입력 자료 및 제3자 권리까지 자동 보증하지는 않는다. 생성 당시 계정/지역/서비스 유형에 적용된 약관은 실제 기록과 대조해야 한다. 이를 CC0/MIT 같은 별도 이미지 라이선스로 선언하지 않았다.

README 스크린샷 5개는 실제 게임 화면의 캡처로 별도 원본 게임 에셋은 아니다. 한글 성적표/졸업 캡처 2장은 새 OFL 폰트를 사용한 화면으로 갱신했다. 스크린샷 안의 게임 이미지 출처도 위 기록을 따른다.

## 4. 라이브러리·네이티브·런타임 확인표

Gradle이 실제 해결한 runtimeClasspath는 55개 아티팩트다. 플랫폼별 classifier를 포함한 좌표·버전·파일·해시는 [DEPENDENCY_INVENTORY.json](../packaging/DEPENDENCY_INVENTORY.json)에 기록했다. 별도 Gradle 캐시에 우연히 있는 다른 버전을 배포 라이브러리로 세지 않았다.

| 구성요소 | 확인한 권리·원문 | 재배포/고지 상태 |
| --- | --- | --- |
| gdx / backend-lwjgl3 / platform 1.13.1 | Apache 2.0, 공식 LICENSE·POM | 원문 동봉. 기본 폰트·네이티브 하위 구성요소는 별도 취급 |
| gdx-jnigen-loader 2.5.2 | Apache 2.0, 실제 POM | Apache 원문 동봉 |
| LWJGL core/GLFW/jemalloc/OpenAL/OpenGL/STB 3.3.3 Java 바인딩 | BSD 3-Clause, 공식 버전 LICENSE | 원문·원 저작권 동봉 |
| libffi 네이티브 | MIT 계열 원문, LWJGL 해당 버전 고지 | 전문 동봉 |
| GLFW DLL | zlib/libpng, 해당 버전 고지 | 전문 동봉 |
| jemalloc DLL | BSD 계열, 해당 버전 고지 | 전문 동봉 |
| OpenGL 관련 Khronos 코드 | Khronos 허용 고지 | 전문 동봉 |
| STB 코드 | MIT/public-domain 이중 조건 | 원문 동봉. 게임 이미지를 public domain으로 선언한 것은 아님 |
| OpenAL Soft DLL | Windows x64/x86에서 1.23.1 문자열 확인, LGPL·파일별 조건 | 고지 + 공식 1.23.1 소스 동봉. 실제 바이너리의 패치/소스 대응 최종 확인 필요 |
| JLayer-gdx 1.0.1-gdx | POM LGPL 2.1, 실제 소스 헤더 Library GPL v2-or-later | 이전 고지 누락 보완. 버전 일치 소스 JAR/POM·저작권·LGPL 전문 동봉 |
| JOrbis/Jogg 0.0.17 | 실제 소스 헤더 Library GPL v2-or-later | 이전 고지 누락 보완. 버전 일치 소스 JAR/POM·저작권·LGPL 전문 동봉 |
| StartupHelper.java | damios Apache 2.0 | 원래 고지 유지 |
| game:core:1.0.0 | 프로젝트 코드 | 소유자가 정책 결정, 새 라이선스 부여 안 함 |
| Oracle JDK 17.0.12 jlink 이미지 | 로컬 NFTC, Oracle 공식 FAQ/해당 릴리스 정보 | 현 빌드의 무상 재배포 조건과 원문 유지. 다른 JDK/버전·유료 배포로 일반화 안 함 |

Windows JAR에는 14개 DLL 엔트리가 있다. libGDX x86/x64와 LWJGL의 두 아키텍처 라이브러리가 포함된다. 기존 fat JAR의 Linux/macOS 네이티브도 남아 있으며 실행하지 않는 파일의 라이선스를 무시하지 않았다. 런타임 DLL과 FreeType·HarfBuzz·JPEG·PNG 등 하위 고지는 `runtime/legal/`의 원문을 그대로 유지했다. gdx-liftoff/Gradle/Construo 등의 개발 도구는 실행용 클래스패스와 구분했다.

[LGPL_REPLACEMENT.md](../packaging/LGPL_REPLACEMENT.md)에 JAR classpath 우선 교체와 OpenAL 공유 DLL 교체 경로를 설명했다. 관련 LGPL 구성요소에 대해 수정 분석·교체 권리를 금지하는 조건을 새로 추가하지 않았다. 실제 수정 소스를 재빌드·교체한 테스트와 모든 플랫폼 바이너리의 정확한 대응 소스 검증까지 완료한 것으로 선언하지 않는다.

## 5. 코드 정책과 ZIP에 넣은 고지

[CODE_LICENSE_POLICY.md](CODE_LICENSE_POLICY.md)를 작성했다. 프로젝트 전체 MIT 등의 라이선스는 소유자 승인 없이 선택하지 않았다. 서드파티 고지와 자체 코드 정책을 구분한다.

새 ZIP의 `licenses/`에는 다음 자료가 포함된다.

- THIRD_PARTY_NOTICES.md, FONT_PROVENANCE.md, ASSET_INVENTORY.csv, DEPENDENCY_INVENTORY.json, LGPL_REPLACEMENT.md.
- ending-ko/title-ko/vacation-ko의 OFL 전문, Liberation-OFL-1.1.txt.
- Apache·LWJGL BSD·libffi·GLFW·jemalloc·Khronos·STB 원문.
- LGPL 2.0/2.1, OpenAL 고지, JLayer/JOrbis 저작권 헤더.
- sources/의 JLayer·JOrbis 동일 버전 소스 JAR/POM 및 OpenAL Soft 공식 1.23.1 태그 소스 ZIP.
- JAVA-RUNTIME-LICENSE.txt 및 app-image의 runtime/legal/ 전체.

라이선스 이행용 서드파티 소스 자료는 개발/테스트 게임 코드와 구분하여 동봉했다. 실행용 라이브러리나 게임 규칙을 추가하지 않았다.

## 6. 구/신 후보본과 검증

| 구분 | 경로 | ZIP SHA-256 |
| --- | --- | --- |
| 교체 전 후보본 (보존) | dist/CampusRunner-Windows-v1.0.0.zip | 3585f10bee256921e01b3576fb55e317879e8e9bf5402780bf25fe4984ca17be |
| 폰트 교체 후 중간 검증본 | dist/font-license-update/ | 최종 게시 대상 아님 |
| **최신 고지 반영 후보본** | **dist/license-reviewed/CampusRunner-Windows-v1.0.0.zip** | **56faf35f8e76bf7deefb178580b7e1ef99e295fa1f6002e97eeff8371bd8ea0b** |

새 EXE: `dist/license-reviewed/CampusRunner/CampusRunner.exe`.

새 ZIP: 58,704,060바이트, 166엔트리. 구 ZIP의 해시는 작업 전후 동일했다. 기존 후보본을 삭제·덮어쓰지 않았다. 새 ZIP을 최종 경로에 생성하기 전 동일 소스로 전체 빌드·회귀 검증을 완료했으며, 최종 패키징에서는 코드 변경 없는 고지 갱신에 대해 SkipBuild 옵션을 사용했다.

| 검증 | 결과 |
| --- | --- |
| Gradle build / 기존 Stage 1~8·목숨·미니게임·GPA·엔딩 회귀 | 통과 |
| 30·60·120fps 기존 검증 | 통과 |
| 새 폰트 이름·OFL 고지·181 글리프·모든 엔딩 문구 | 통과, 기존 조건 완화 없음 |
| 새 ZIP 압축 해제 후 번들 런타임/패키지 JAR 실행 | 통과, 개발 assets 없는 cwd·Java/Gradle 없는 PATH |
| 성적표·엔딩 5종 × 800×600/1280×720/1920×1080 | 통과, 30개 캡처 |
| 한글 누락·패널 겹침·잘림 | 검사한 화면에서 발견되지 않음 |
| 성적표 Space → 엔딩 → Enter 새 게임·월드 정지·공유 리소스 해제 | 통과 |
| 최종 EXE 시작·수집·정지·타이틀 복귀·메뉴 종료 | 통과, 창 메시지 입력 및 종료 코드 0 |

대표 캡처: [800×600 최우수](../build/license-audit/isolated-work/build/final-pixel-gui/after-ending-highest_honors-800x600.png), [1280×720 최우수](../build/license-audit/isolated-work/build/final-pixel-gui/after-ending-highest_honors-1280x720.png), [1920×1080 졸업 유예](../build/license-audit/isolated-work/build/final-pixel-gui/after-ending-deferred-1920x1080.png).

로그: build/license-audit/build.log, package.log, final-package.log, final-font-gui.log, FinalPixelGuiSmoke.log, final-exe-smoke.log. 테스트 도구·로그·캡처는 Git 제외 build 폴더에 있고 ZIP에는 포함하지 않는다. GUI는 자동 입력/장면 픽스처이며 사람이 물리 키보드로 8학기를 완주한 검증은 아니다. 깨끗한 외부 PC와 장시간 프로파일링은 수행하지 않았다.

## 7. 남은 확인 사항

- 생성 서비스는 사용자 설명 GPT로 기록했다. 정확한 모델·생성 달력 날짜와 입력/생성 기록은 미제공이므로 해당 기록과 실제 적용 약관 대조가 남는다. 현재 OpenAI 출력 권리 조항을 제3자 권리 보증으로 설명하지 않았다.
- 자체 소스에 별도 오픈소스 사용권을 부여하지 않는 정책은 사용자 결정으로 확정했다. 공동 기여분 및 서드파티 권리는 별도 확인 대상으로 유지한다.
- OpenAL의 정확한 LWJGL-CI 커밋/패치 및 포함된 타 플랫폼 네이티브의 대응 소스, 수정 라이브러리 실제 재빌드·교체 검증은 추가 확인이 필요하다.

**맑은 고딕 폰트 문제 해결 및 고지 보완과, 공개 배포에 필요한 모든 권리 확인 완료는 별개의 상태다.** 확인이 필요한 항목은 그대로 남겼다. Git commit/push/merge/tag 및 Release 게시를 실행하지 않았다.

## 8. 이번 단계의 변경 파일

- 폰트: assets/fonts/ending-ko.fnt, ending-ko.png, 신규 ending-ko-OFL.txt.
- 생성·검증: tools/GenerateEndingFont.ps1, GenerateVacationFont.ps1, tools/fonts/README.md, core/src/test/java/io/jbnu/test/GraduationEndingVerification.java.
- 패키징: tools/PackageWindows.ps1 보완, 신규 tools/ExportRuntimeLicenses.gradle, packaging/의 고지·출처·목록·LGPL 안내, licenses/ 전문과 sources/ 자료.
- 정책·보고: 신규 docs/LICENSE_AUDIT.md, CODE_LICENSE_POLICY.md, 기존 GRADUATION_ENDINGS.md·WINDOWS_DISTRIBUTION.md·RELEASE_v1.0.0_DRAFT.md 갱신.
- README와 성적표/졸업 스크린샷 2개 갱신. 나머지 게임 이미지·플레이 로직·엔딩 문구·판정·레이아웃 코드는 변경하지 않았다.

작업 시작 시 이미 남아 있던 STEP 10-15의 .gitignore·런처 창 제목 등 변경은 보존했다. 이 단계에서 새로 고친 게임 실행 코드로 혼동하지 않는다.
