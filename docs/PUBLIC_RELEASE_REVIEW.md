# Campus Runner — GitHub 공개 전 최종 점검 (STEP 10-17/18)

검토일: 2026-11-08. 브랜치: `feature/windows-release`.

## 판단

**자체 소스 권리 정책과 기술적 배포 준비는 반영했다. 그러나 전체 저장소/실행 ZIP의 공개 배포 권한 검토가 모두 끝났다는 판정은 보류한다.** 가장 중요한 미해결은 OpenAL 등의 LGPL 대응 소스와 실제 바이너리의 정확한 대응 관계다. 일부 이미지의 출처는 독립 생성 기록이 아닌 사용자 진술에 근거한다. 깨끗한 Windows PC 실행도 완료하지 않았다.

소스 공개와 실행 ZIP Release는 구분한다. 자체 코드에 오픈소스 라이선스를 부여하지 않는 선택 자체가 GitHub 공개를 금지하는 것은 아니다. 다만 저장소에 함께 들어가는 폰트·이미지·서드파티 소스의 권리는 별개이며, 저장소 공개가 코드 재사용 허가와 동일하지 않다. 실제 GitHub 가시성 설정이나 Release를 변경하지 않았다.

## 1. 자체 코드 정책 — 사용자 결정 반영 완료

- Campus Runner 자체 소스에 MIT·Apache·GPL 등 별도 오픈소스 라이선스를 부여하지 않는다.
- 자체 소스의 재사용·수정·재배포 권한을 별도로 부여하지 않았다는 점을 README와 [권리 정책](CODE_LICENSE_POLICY.md)에 명시했다.
- 새 저작권자 이름·연도를 추정해 작성하지 않았고 루트 LICENSE를 임의 생성하지 않았다.
- GitHub 약관의 보기·포크 기능상 권리나 법률상 예외를 부인하지 않는다. [GitHub 공식 안내](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/licensing-a-repository).
- 외부 구성요소의 원래 고지와 OFL/LGPL상 권리는 유지한다. 이 결정은 더 이상 ‘정책 미정’ 항목이 아니다.

## 2. 권리 확인 상태 분류

| 분류 | 항목 | 현재 상태 |
| --- | --- | --- |
| 권리/조건 확인 | 나눔고딕 기반 3개 BitmapFont | SIL OFL 1.1, 원본 해시·저작권·이름 변경·라이선스 전문 확인 |
| 권리/조건 확인 | 기본 LSans | LibGDX 상류의 OFL Liberation Sans 파생물 근거와 OFL 전문 동봉 |
| 권리/조건 확인 | LibGDX, LWJGL Java 바인딩 | Apache/BSD 원문 및 실제 배포 좌표 확인. 하위 네이티브 조건은 별도 |
| 권리/조건 확인 | Oracle 17.0.12 jlink 런타임 | 해당 빌드의 NFTC 무상 재배포 조건과 원래 legal 고지 보존. 다른 버전·유료 배포에 일반화 안 함 |
| 추가 고지 보완 | 자체 소스 권리 정책 | 새 ZIP의 licenses/SOURCE_CODE_POLICY.md에 포함 |
| 추가 고지 보완 | 개별 이미지·네이티브 근거 | IMAGE_PROVENANCE.md, NATIVE_INVENTORY.json 추가 |
| 출처 확인 필요 | 게임 이미지·런처 아이콘 | 사용자 GPT 생성 설명과 실제 파일·메타데이터 관찰을 구분. 원 생성 기록 미제공 |
| 배포 충족 불명확 | OpenAL Soft LGPL | 원문·공식 1.23.1 소스는 있으나 실제 LWJGL-CI 바이너리와 정확한 소스/패치 대응 미확인 |
| 추가 검증 필요 | JLayer/JOrbis LGPL | 버전 일치 소스 JAR/POM·고지·교체 안내 포함. 실제 수정본 재빌드/교체와 완전한 재링크 자료 검증은 미완료 |
| 실행 확인 미완료 | 깨끗한 Windows PC | PATH에서 Java/Gradle을 제거한 로컬 검증과 구분 |

해시와 목록은 식별·무결성 자료다. 배포 권한 또는 대응 소스 검증의 대체 자료로 사용하지 않는다.

## 3. 정확한 네이티브 이름·버전과 의무

[NATIVE_INVENTORY.json](../packaging/NATIVE_INVENTORY.json)에 개별 경로·크기·SHA-256·버전 근거를 기록했다. app JAR 네이티브 52개(Windows DLL 14개 포함), 번들 런타임 DLL 65개, 총 117개다. 파일의 버전 리소스와 상류 라이브러리 버전은 구분한다.

| Windows DLL (x64/x86) | 확인한 버전 | 라이선스·고지 | 소스 제공/현재 충족 판단 |
| --- | --- | --- | --- |
| gdx64.dll / gdx.dll | gdx-platform 1.13.1 배포물, 내장 버전 없음 | LibGDX Apache 및 하위 코드 고지 | 정확한 네이티브 커밋 미확인. permissive 고지와 하위 조건 구분 |
| windows/x64 및 x86/org/lwjgl/lwjgl.dll | LWJGL 3.3.3 배포물 | BSD 및 libffi 원문 | 라이브러리 자체의 소스 제공 의무와 고지 유지 구분. 내장 libffi 정확한 버전 미확인 |
| .../glfw/glfw.dll | 파일 리소스/문자열 **3.4.0** | zlib/libpng 전문 | 소스 제공 자체가 일반 필수는 아님. 실제 커밋이 정식 3.4 태그와 같은지는 미확인 |
| .../jemalloc/jemalloc.dll | **정확한 버전 미확인** | BSD 계열 전문 | x64 je_mallctl(version)이 빌드 정보 누락 문자열을 반환. 임의로 5.x 버전으로 추정하지 않음 |
| .../openal/OpenAL.dll | 파일 리소스/문자열 **1.23.1** | LGPL 및 파일별 예외 | 공식 소스/고지 동봉, 실제 바이너리의 대응 소스·빌드 조건은 미해결 |
| .../opengl/lwjgl_opengl.dll | LWJGL 3.3.3 배포물 | BSD·Khronos 고지 | 하위 소스 리비전 미확인. 고지 원문 동봉 |
| .../stb/lwjgl_stb.dll | LWJGL 3.3.3 배포물 | BSD·STB 이중 라이선스 원문 | 정확한 내장 STB 리비전 미확인. 고지 원문 동봉 |

jemalloc API 실제 반환값: `0.0.0-0-g000000missing_version_try_git_fetch_tags`. 이는 ‘버전 0.0.0을 확정’한 것이 아니라 버전 정보를 확인할 수 없다는 근거다.

런타임의 java.dll/jvm.dll 등은 JDK 17.0.12 배포 기준으로 기록했다. FreeType은 runtime/legal/java.desktop/freetype.md에서 2.13.2를 확인했다. Visual C++ DLL 등의 파일 리소스 버전도 manifest에 보관했으며, 이를 원 저작권이나 소스 대응 검증으로 확대하지 않았다. runtime/legal의 별도 라이선스 원문을 보존했다.

기존 fat JAR에는 Windows에서 사용하지 않는 Linux/macOS 네이티브 38개도 포함된다. 배포에 포함된 이상 고지/대응 소스 검토에서 제외하지 않았다. 오디오 코드에 포함된 JLayer 1.0.1-gdx, JOrbis/Jogg 0.0.17도 LGPL 대상이며 OpenAL만 점검한 것으로 끝내지 않았다.

## 4. 구체적인 미해결과 필요한 조치

| 대상 | 미해결 | 필요한 조치 |
| --- | --- | --- |
| app/CampusRunner.jar의 windows/x64/x86/.../OpenAL.dll | 정확한 LWJGL-CI 소스 커밋·패치/빌드 설정 미확인 | 실제 빌드 소스와 제공 자료를 대조하거나, 대응 소스가 검증되는 바이너리를 별도 검증 절차로 재빌드하여 교체. 이번 작업에서 게임 라이브러리를 임의 교체하지 않음 |
| 같은 JAR의 linux/macos libopenal 네이티브 | 플랫폼별 소스 대응 미확인 | 각각 검토하거나 Windows 전용 배포 구성을 별도 변경·회귀 검증하여 불필요한 바이너리를 제외 |
| JLayer/JOrbis fat JAR 포함 클래스 | 고지·동일 버전 소스 존재와 완전한 재링크 조건 충족은 별개 | 수정 라이브러리 재빌드·교체 및 필요한 빌드/재링크 자료 점검. [LGPL 안내](../packaging/LGPL_REPLACEMENT.md)의 미완료 항목 유지 |
| assets/pixel/**, legacy PNG, 런처 아이콘 | 원 생성 대화/프롬프트/정확한 생성 날짜와 모델은 미제공 | 보유 기록이 있으면 파일별로 대조. 사용자 설명을 독립 인증이나 모든 제3자 권리 보증으로 바꾸지 않음 |
| 전체 EXE/ZIP | 완전히 깨끗한 Windows PC에서 미실행 | 외부 PC 또는 깨끗한 VM에서 압축 해제·시작·종료와 그래픽/런타임 의존성 확인 |

공개 Release의 최종 승인은 사용자가 한다. 자체 코드 정책은 이미 결정됐으므로 MIT 등 선택을 다시 요구하지 않는다. 코드·아트의 소유권이나 서드파티 조건을 임의로 대신 승인하지 않는다.

## 5. 이미지 기록 — 진술과 검증의 구분

[ASSET_INVENTORY.csv](../packaging/ASSET_INVENTORY.csv)의 53개 원본과 [IMAGE_PROVENANCE.md](../packaging/IMAGE_PROVENANCE.md)를 기준으로 한다. 이전 감사에서 IDE bin/main의 복사본 41개를 런처 아이콘으로 중복 집계한 오류를 수정했고, 기존 파일은 삭제하지 않았다. 런처 원본 PNG 5개와 ICO/ICNS 2개 중 리소스 PNG 4개만 앱 JAR에 포함된다.

사용자 설명은 ‘전부 AI 생성물’, ‘GPT로 파일 올린 날’이다. 파일 해시·존재·사용 여부는 직접 확인했으나 정확한 모델·생성일·프롬프트를 만들어 적지 않았다. PNG 9개에는 caBX 청크가 있지만 서명 검증을 하지 않아 OpenAI 발급 인증으로 단정하지 않았다. 나머지 파일에서 해당 청크가 없다는 사실도 AI 생성이 아니라는 증거는 아니다.

실제 템플릿·타인 자료가 섞였는지와 입력 자료 권한은 별도 확인 대상이다. OpenAI 출력 귀속 조항은 계약상 근거와 제3자 권리를 구분하여 기록했다. 폰트는 AI 이미지로 분류하지 않았다.

## 6. Git·README·개인정보 점검

- 현재 브랜치 feature/windows-release를 유지했다. 새 브랜치·commit/push/merge/tag를 만들지 않았다.
- git ls-files와 untracked-but-not-ignored 후보를 함께 확인했다. build/, dist/, .gradle/, IDE bin 출력은 추적 대상에서 제외되어 있다. 기존 검증 코드·개발 문서를 삭제하지 않았다.
- .gitignore는 생성된 배포 ZIP/해시와 테스트 캡처를 제외한다. docs/screenshots의 선정 이미지 5개는 의도적으로 포함한다.
- README의 게임 설명·실제 키·개발 명령·실행 ZIP 구조가 기존 검증/패키징 방식과 일치한다. 다운로드는 Releases 페이지 안내만 있으며 미게시 v1.0.0 ZIP의 직접 주소를 만들지 않았다.
- README의 5개 이미지 링크와 상대 문서 링크를 로컬 파일 기준으로 검사했다. GitHub 렌더링/온라인 저장소 권한 설정을 원격에서 변경하거나 검증했다고 주장하지 않는다.
- 개발 PC 절대 경로 예시는 JAVA_HOME/상대 경로로 정리했다. app의 jpackage cfg도 APPDIR 기반이다.
- 후보 텍스트 파일의 비밀키/개인 경로 패턴 검사에서 해당 항목을 찾지 못했다. 이메일 8건은 서드파티 저작권/라이선스 고지로 분류하여 삭제하지 않았다. 패턴 검사로 모든 비밀/개인정보 부재를 보증하지는 않는다.

## 7. 저장소 포함 파일과 Release 첨부 파일

GitHub 저장소의 의도한 변경은 다음과 같다. 아직 git add/commit하지 않았다.

- 게임 소스·검증 코드·Gradle wrapper/config·기존 문서와 assets.
- 자체 코드 정책, LICENSE_AUDIT/PUBLIC_RELEASE_REVIEW, README와 Release 초안.
- OFL 엔딩 폰트와 생성 도구·OFL 원문·원본 TTF(허용 조건/고지 유지).
- packaging의 고지·출처/해시 목록·라이선스 전문과 LGPL 이행 자료용 sources/.
- docs/screenshots의 타이틀·플레이·서버 미니게임·성적표·엔딩 5장.

Release가 승인되면 첨부할 파일은 아래 2개다. EXE 단독 업로드는 하지 않는다.

```text
dist/license-reviewed/CampusRunner-Windows-v1.0.0.zip
dist/license-reviewed/CampusRunner-Windows-v1.0.0.zip.sha256
```

ZIP에는 CampusRunner.exe, app JAR, 번들 runtime, 실행 안내와 licenses를 포함한다. licenses/sources의 서드파티 소스 JAR/POM/ZIP은 라이선스 자료이며 개발용 게임 테스트 코드와 구분한다. .git/.gradle, 게임 테스트 클래스/스크립트, 로그, 캡처, 개발 문서는 포함하지 않는다.

## 8. 재생성·검증 결과

기준 ZIP은 정책/고지 갱신 후 재생성했다. 이전 후보본은 `dist/license-reviewed-step10-16/`에 보존했다. 루트 dist의 STEP 10-15 후보본도 보존했다. 게임 로직·맵·폰트 픽셀은 이번 단계에서 변경하지 않았다.

- 최신 ZIP SHA-256: `d0433391b80305c2c5509ecdf1c39924e2ff6fd4536ee06b0afa25d63a25c802`.
- ZIP 크기: 58,715,457바이트. app-image를 새 경로에서 생성한 뒤 기준 경로로 옮겼으며 삭제/덮어쓰기로 구 후보본을 없애지 않았다.
- .\gradlew.bat build 성공, 기존 30·60·120fps 회귀 검증 통과.
- ZIP CRC/엔트리·고지 일치·새 정책 파일·필수 리소스·cfg 상대 경로·Git 제외 검사 통과.
- 새 ZIP을 별도 작업 경로에 풀고 Java/Gradle 없는 PATH에서 실제 EXE 시작·아이템 수집·정지·타이틀 복귀·종료 코드 0을 확인했다.
- 외부 클린 PC·VM은 사용하지 않았다. 자동 창 메시지 입력을 사람이 물리 키보드로 수행한 검증으로 표시하지 않는다.

로그/세부 관찰은 Git 제외 build/public-release-review에 있다. **Git 커밋 후보 파일 점검 완료와 공개 Release 권리 확인 완료는 다르다.** 현 시점 Release 게시 권고는 보류이며, 위 미해결 항목이 해소되면 그 근거로 재판단한다.
