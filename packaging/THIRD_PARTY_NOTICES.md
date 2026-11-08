# Campus Runner — 배포 구성요소와 확인 사항 (STEP 10-16)

사용자 결정에 따라 **Campus Runner 자체 소스 코드에는 별도 오픈소스 라이선스를 부여하지 않는다.** 자체 코드의 재사용·수정·재배포 권한을 별도로 부여하지 않았으며 GitHub 공개와 이용허락은 다르다. 외부 구성요소의 기존 라이선스·고지·LGPL 교체 권리와 GitHub 약관상 권리는 유지한다. 상세 정책은 동봉 SOURCE_CODE_POLICY.md를 참조한다. **엔딩 폰트는 OFL 원본에서 재생성했다.** 일부 네이티브 대응 소스와 이미지 생성 기록의 독립 확인은 남아 있어 패키지는 내부 검증용 후보본이다.

## 폰트

- Campus Ending Korean / Campus Title Korean / Campus Vacation Korean: 기존 NanumGothic-Bold.ttf 기반, Copyright (c) 2010 NHN Corporation, **SIL OFL 1.1**. 이름을 변경한 파생물이며 각각 ending-ko-OFL.txt, title-ko-OFL.txt, vacation-ko-OFL.txt를 동봉한다. [Google Fonts 원본](https://github.com/google/fonts/tree/main/ofl/nanumgothic). 상세 생성 경로·해시는 FONT_PROVENANCE.md에 있다.
- LibGDX 기본 영문 비트맵은 **LSans (Liberation Sans 파생물), SIL OFL 1.1**이다. 현재 JAR에는 lsans-15.fnt/png가 있고 Arial은 없다. [상류 확인 #6727](https://github.com/libgdx/libgdx/pull/6727), 원 저작권/OFL은 Liberation-OFL-1.1.txt를 동봉한다. 이 폰트 이미지는 수정하지 않았다.

## Java 라이브러리와 네이티브

STEP 10-17/18 재검토: 정확한 파일명·해시·배포 버전과 바이너리 자체 버전 근거는 NATIVE_INVENTORY.json에 별도로 기록했다. app JAR의 52개 네이티브(Windows DLL 14개 포함)와 번들 런타임 DLL 65개, 총 117개 파일이다. LWJGL 3.3.3이라는 배포 좌표를 모든 하위 라이브러리의 버전으로 쓰지 않는다.

Windows glfw.dll은 파일 리소스·문자열이 3.4.0이지만 정확한 커밋은 미확인이다. OpenAL.dll은 1.23.1이다. jemalloc.dll의 x64 je_mallctl(version)은 `0.0.0-0-g000000missing_version_try_git_fetch_tags`를 반환하므로 실제 상류 버전을 확정할 수 없다. core/OpenGL/STB DLL의 정확한 하위 소스 리비전도 미확인이다. 고지의 존재·해시는 대응 소스 또는 권리 확보의 증거로 대체하지 않는다.

실제 resolved runtime 55개 아티팩트의 좌표·파일·SHA-256은 DEPENDENCY_INVENTORY.json을 참조한다. 테스트/빌드 도구를 실행 라이브러리로 혼동하지 않는다.

| 구성요소 | 라이선스·고지 | 출처 |
| --- | --- | --- |
| LibGDX 1.13.1, gdx-backend-lwjgl3, gdx-platform | Apache 2.0; libgdx-Apache-2.0.txt | [LibGDX 1.13.1](https://github.com/libgdx/libgdx/tree/1.13.1) |
| gdx-jnigen-loader 2.5.2 | Apache 2.0; 동일 Apache 전문, Maven POM 확인 | [gdx-jnigen](https://github.com/libgdx/gdx-jnigen) |
| LWJGL 및 OpenGL/STB/GLFW/jemalloc/OpenAL Java 바인딩 3.3.3 | BSD 3-Clause; LWJGL-BSD-3-Clause.txt | [LWJGL 3.3.3](https://github.com/LWJGL/lwjgl3/tree/3.3.3) |
| libffi (LWJGL core 네이티브) | MIT 계열의 원문; libffi_license.txt | 위 버전 modules/lwjgl/core |
| GLFW DLL | zlib/libpng; glfw_license.txt | 위 버전 modules/lwjgl/glfw |
| jemalloc DLL | BSD 계열 원문; jemalloc_license.txt | 위 버전 modules/lwjgl/jemalloc |
| OpenGL 바인딩 관련 Khronos 코드 | Khronos 허용 고지; khronos_license.txt | 위 버전 modules/lwjgl/opengl |
| STB | MIT 또는 public-domain 이중 선택; stb-LICENSE.txt에 원문 동봉 | [STB](https://github.com/nothings/stb) |
| OpenAL Soft 1.23.1 DLL | LGPL 계열 및 파일별 조건; OpenAL-Soft-1.23.1-COPYING.txt, openal_soft_license.txt, 동봉 소스 내부 고지 | [OpenAL Soft 1.23.1](https://github.com/kcat/openal-soft/tree/1.23.1) |
| JLayer-gdx 1.0.1-gdx | POM은 LGPL 2.1, 소스 헤더는 Library GPL v2-or-later. LGPL 2.0/2.1 전문·jlayer-ATTRIBUTION.txt 동봉 | [JLayer-gdx](https://github.com/libgdx/jlayer-gdx) |
| JOrbis/Jogg 0.0.17 | 소스 헤더 Library GPL v2-or-later. LGPL-2.0.txt·jorbis-ATTRIBUTION.txt 동봉 | [JCraft](https://www.jcraft.com/jorbis/) |
| StartupHelper.java | damios, Apache 2.0. 기존 파일 고지 유지 | [gdx-liftoff](https://github.com/libgdx/gdx-liftoff) 기반 런처 |

오디오 라이브러리가 현재 장면에서 사용되지 않더라도 바이너리에 포함된 이상 고지에서 제외하지 않았다. **LGPL의 원문·소스 자료와 교체 방법은 LGPL_REPLACEMENT.md 및 sources/에 있다.** Library source JAR/POM은 사용 버전의 기존 Gradle 캐시에서, OpenAL 소스는 공식 1.23.1 태그에서 확보했다. 소스가 있다는 것만으로 모든 실제 바이너리의 대응 소스 검증이 완료된 것은 아니다.

## Java 런타임

현재 후보본은 Oracle JDK **17.0.12**의 jlink 이미지다. 로컬 `legal/java.base/LICENSE` 및 [Oracle FAQ](https://www.oracle.com/java/technologies/javase/jdk-faqs.html), [17.0.12 릴리스 정보](https://www.oracle.com/java/technologies/javase/17-0-12-relnotes.html)를 대조했다. NFTC가 적용되는 이 빌드의 무상 재배포 조건과 고지를 유지한다. JAVA-RUNTIME-LICENSE.txt, runtime/legal/ 및 runtime/release를 확인한다. 이 판단을 Oracle의 더 최신 17 업데이트나 유료 배포에 일반화하지 않는다. 런타임별 제3자 DLL/폰트/암호화 구성요소의 고지는 runtime/legal/에 남아 있다.

## 확인 필요

1. 사용자 확인(2026-11-08): 게임 PNG와 런처 아이콘은 모두 **GPT 생성물**, 생성 시점은 **파일 업로드 당일**이다. 이를 ASSET_INVENTORY.csv에 기록했다. 정확한 모델명·달력 날짜는 미제공이며 파일 수정 시간으로 추정하지 않는다. [OpenAI 일반 이용약관](https://openai.com/policies/terms-of-use/) (확인일 2026-11-08, 표시 효력일 2026-01-01)의 출력 권리 조항은 법이 허용하는 범위에서 OpenAI와 사용자 사이의 출력 권리를 사용자에게 귀속시킨다. 게임 배포 활용의 계약상 근거로 확인했으나, 생성 당시 적용 약관·입력 자료 권리·제3자 권리까지 자동 증명하는 것은 아니다. 이를 CC0/MIT 또는 저작권 보호 보장으로 바꾸어 선언하지 않는다. 폰트 아틀라스는 GPT 이미지가 아니라 위 폰트 원본에서 도구로 생성한 별도 항목이다.
2. 자체 소스 코드에 별도 오픈소스 라이선스를 부여하지 않는 정책은 사용자 결정으로 확정했다. 이 정책을 외부 라이브러리에 적용하거나 그 고지를 생략하지 않는다.
3. OpenAL의 정확한 LWJGL-CI 소스 커밋/패치/빌드 조건, 다른 OS용 포함 네이티브의 대응 소스, 수정 라이브러리 실제 재빌드·교체 검증은 추가 확인이 필요하다. LGPL_REPLACEMENT.md의 한계를 확인한다.

맑은 고딕 기반 구 후보본과 달리 **새 ending-ko는 직접 검증한 OFL 원본에서 새로 생성**했으며 고지도 함께 포함한다. 폰트 문제 해결을 이미지·코드·모든 의존성 권한 확인 완료로 확대 해석하지 않는다. 확인 필요 항목이 해소되기 전에는 공개 Release 준비 완료로 표시하지 않는다.
