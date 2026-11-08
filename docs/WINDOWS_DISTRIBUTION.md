# Campus Runner — Windows 배포 후보본 (STEP 10-15)

> 아래는 교체 전 후보본의 역사적 검증 기록이다. STEP 10-16에서 ending-ko를 OFL 원본으로 교체했으며, 새 후보본·해시·남은 권한은 [최신 라이선스 점검표](LICENSE_AUDIT.md)를 기준으로 한다. 기존 dist 파일은 덮어쓰지 않았다.

검증일: 2026-11-08. EXE·런타임 포함 ZIP과 README를 준비했다. **기술 실행 검증을 통과한 내부 후보본이며, 폰트·이미지 권리 확인이 끝난 공개 배포본은 아니다.** GitHub 업로드·Release 게시·tag·commit·push·merge는 실행하지 않았다.

## 프로젝트와 도구

- Java 8 소스/타깃, LibGDX 1.13.1, LWJGL 3.3.3, Gradle Wrapper 8.14.3.
- 사용한 Windows x64 JDK: JAVA_HOME으로 지정한 Oracle 17.0.12. 기존 설치의 jlink와 jpackage를 사용했으며 새 JDK나 패키징 도구를 설치하지 않았다.
- 데스크톱 main class: `io.jbnu.test.lwjgl3.Lwjgl3Launcher`.
- 기존 `:lwjgl3:jar`는 runtimeClasspath를 합치는 fat JAR이며 Manifest의 Main-Class도 위 진입점이다.
- `sourceSets.main.resources`에 assets가 포함된다. LibGDX internal 파일 로딩은 classpath 리소스로 이미지·폰트·셰이더를 읽을 수 있다.
- 기존 Construo 설정도 있으나 외부 JDK 다운로드 대신 설치된 JDK와 독립 PowerShell 스크립트를 사용했다. 기존 Gradle 태스크는 변경하지 않았다.
- 창 제목만 Project2에서 Campus Runner로 정정했다. 기본 크기·물리·맵·게임 규칙은 변경하지 않았다.

## 패키징 방식

```powershell
& .\tools\PackageWindows.ps1 -JdkHome $env:JAVA_HOME
```

1. 도구와 Windows x64 아키텍처를 검사한다. 없으면 설치하지 않고 실패한다.
2. `.\gradlew.bat build lwjgl3:jar`로 빌드·기존 회귀 검증을 실행한다.
3. 실행 JAR 하나만 새로운 staging 입력 폴더에 `CampusRunner.jar`로 복사한다.
4. jdeps에서 확인한 `java.base,java.desktop,java.management,jdk.unsupported`를 jlink로 연결한다. 전이 모듈을 포함한 실제 runtime/release에는 java.datatransfer·java.xml·java.prefs도 들어 있다. 법적 고지는 유지하고 디버그 심볼·헤더·매뉴얼은 제외한다.
5. jpackage `--type app-image --name CampusRunner --app-version 1.0.0`으로 네이티브 EXE와 런타임을 묶는다. 콘솔 창 옵션은 추가하지 않는다.
6. 실행 안내·라이선스 전문/고지를 넣고 app-image 폴더만 ZIP으로 만든다. SHA-256 파일도 생성한다.

기존 app-image나 ZIP이 있으면 삭제·덮어쓰지 않고 중단한다. 다시 생성하려면 `-OutputDirectory dist/rebuild`처럼 새 위치를 지정한다. `-SkipBuild`는 이미 해당 소스로 빌드를 마친 개발자만 사용하는 선택 옵션이다. staging은 Git 제외 `build/windows-package/<고유ID>/`를 사용한다.

## 산출물

```text
dist/CampusRunner/CampusRunner.exe
dist/CampusRunner/app/CampusRunner.jar
dist/CampusRunner/runtime/
dist/CampusRunner/licenses/
dist/CampusRunner/START-HERE.txt
dist/CampusRunner-Windows-v1.0.0.zip
dist/CampusRunner-Windows-v1.0.0.zip.sha256
```

- EXE: 474,624바이트, PE Machine `0x8664` (Windows x64).
- app-image 전체: 101,159,150바이트.
- ZIP: 57,558,242바이트 (약 54.9MiB), 150개 엔트리.
- SHA-256: `3585f10bee256921e01b3576fb55e317879e8e9bf5402780bf25fe4984ca17be`.
- ZIP 최상위 폴더는 CampusRunner 하나이며 그 안에서 EXE를 찾을 수 있다.

ZIP 무결성 검사와 내용물 검사를 통과했다. .git/.gradle/build/docs/테스트·소스·스크린샷·로그·검증 스크립트는 ZIP에 없다. 라이선스와 실행 안내는 실행 배포 고지로 포함한다. JAR 내부 검증 클래스나 Java 소스도 없다. Windows DLL 14개 엔트리와 실행 라이브러리가 포함되며 기존 fat JAR의 다른 플랫폼용 리소스를 임의로 재구성하지 않았다.

현재 assets의 45개 파일은 JAR 안에 바이트 단위로 동일하게 들어 있음을 확인했다. 8개 배경, 픽셀 오브젝트, 모든 비트맵 폰트, 두 배경 셰이더가 포함된다. 원본 PNG는 수정하지 않았다. app/CampusRunner.jar와 원래 Project2-1.0.0.jar도 바이트 단위로 동일하다.

## 실제 배포 실행 검증

ZIP을 `build/release-smoke/별도 실행 경로/CampusRunner/`에 압축 해제했다. 원래 dist 폴더가 아닌 이 복사본을 실행했다.

자식 프로세스의 PATH를 Windows System32만 남기고 JAVA_HOME·JDK_HOME·JRE_HOME·CLASSPATH·Java 옵션 환경변수를 제거했다. 작업 디렉터리는 별도의 `isolated-work`로 지정했으며 프로젝트 assets 폴더가 없다.

| 항목 | 결과 |
| --- | --- |
| 압축 해제한 CampusRunner.exe 시작 | 통과 |
| 타이틀·한글·배경 표시 | 통과 |
| Enter로 Stage 1 시작 | 통과 |
| 방향키 이동과 A+ 수집, SCORE 5 / ITEMS 1/4 | 통과 |
| Esc 일시정지·QUIT 타이틀 복귀 | 통과 |
| 타이틀 게임 종료, EXE 프로세스 종료 코드 0 | 통과 |
| 패키지 JAR+번들 Java로 미니게임 3종 | 시작/플레이/성공/실패/효과 및 세 해상도 통과 |
| 패키지 JAR+번들 Java로 성적표·엔딩 5종 | 세 해상도, 저장 결과/GPA·입력·재시작 통과 |
| GUI 검증의 텍스처·폰트 해제 및 렌더링 상태 복원 | 통과 |
| README 개발 명령 `:lwjgl3:run` | 실제 Campus Runner 창 실행·정상 종료, Gradle 종료 코드 0 |

EXE 입력은 해당 창에 Windows 키 메시지를 보내 검사했다. 미니게임/최종 결과는 패키지의 runtime/bin/java.exe와 app/CampusRunner.jar로 기존 GUI 검증 도구를 실행했다. 이 도구의 테스트 픽스처를 통해 결과 장면을 준비했으며 EXE에서 사람이 8학기를 완주한 검증으로 주장하지 않는다. 검증 클래스는 배포 ZIP에 포함하지 않았다. 이 테스트들의 실행 cwd에도 assets 폴더가 없어 classpath 리소스를 실제로 사용했다.

처음 EXE 검증 도구는 jpackage 부모 프로세스만 보아 창을 찾지 못했다. 실제 게임은 자식 프로세스에 있었으며 창 탐지·표시와 방향키 확장 키 플래그를 테스트 도구에서 수정한 후 통과했다. 패키지나 게임 로직 오류로 처리하지 않았다.

검증 로그/캡처 (Git 제외):

- `build/windows-package.log`: 전체 build와 패키징 결과.
- `build/release-smoke/exe-smoke.log`: 실제 EXE 시작·수집·메뉴 종료.
- [EXE 타이틀](../build/release-smoke/exe-title.png), [Stage 1](../build/release-smoke/exe-stage1.png), [학점 수집](../build/release-smoke/exe-collected.png), [일시정지](../build/release-smoke/exe-paused.png), [타이틀 복귀](../build/release-smoke/exe-title-return.png).
- `build/release-smoke/VacationThemeGuiSmoke.log`: 45개 미니게임 캡처와 실제 결과/효과 검사.
- `build/release-smoke/FinalPixelGuiSmoke.log`: 30개 성적표/엔딩 캡처와 입력·초기화·리소스 검사.
- 해당 GUI 캡처는 `build/release-smoke/isolated-work/build/vacation-korean-gui/`, `final-pixel-gui/`에 있다.
- `build/release-smoke/gradle-run.log`: 개발 실행 명령 종료 코드 0.

Java·Gradle이 PATH에 없는 환경은 검증했지만, 개발 JDK가 전혀 설치되지 않은 깨끗한 PC/VM은 사용하지 않았다. 파일 로딩과 번들 런타임 실행 검증을 그와 동일한 것으로 간주하지 않는다.

## README와 Git 관리

기존 README의 병합 충돌 표시를 제거하고 기존 프로젝트 기원·core/lwjgl3 모듈·wrapper 빌드 정보를 한국어로 보존·정리했다. 소개·기능·실제 조작키·실행·개발 빌드·점수·GPA·라이선스와 배포 한계를 추가했다. Release ZIP의 임의 다운로드 링크는 만들지 않았다.

README용 1280×720 캡처 5개를 기존 build 캡처에서 그대로 `docs/screenshots/`로 복사했다.

| 선정 파일 | 원본 |
| --- | --- |
| title.png | title-redesign-gui/after-menu-start-1280x720.png |
| gameplay.png | pixel-hud-compact-gui/after-stage7-1280x720.png |
| server-minigame.png | vacation-korean-gui/after-hackathon-detail-1280x720.png |
| final-report.png | final-pixel-gui/after-report-highest_honors-1280x720.png |
| graduation.png | final-pixel-gui/after-ending-highest_honors-1280x720.png |

원본은 모두 build 아래에 있다. README의 5개 이미지 경로가 존재함을 확인했다. 최종 결과 이미지는 검증용 만점 픽스처 예시라고 표시했다.

기존 .gitignore는 build/.gradle/IDE 임시 파일/dist를 이미 제외한다. 루트에 잘못 놓인 Windows ZIP·해시도 제외하도록 보완했다. docs/screenshots는 무시되지 않아 이 5개만 저장소 변경에 포함된다. 추적 중인 기존 문서나 파일을 삭제하지 않았다.

## 라이선스와 공개 배포 전 남은 사항

상세 원문·출처는 [THIRD_PARTY_NOTICES.md](../packaging/THIRD_PARTY_NOTICES.md)에 있다.

- 기존 설치 Oracle 17.0.12는 로컬 NFTC 전문과 [Oracle 공식 안내](https://www.oracle.com/java/technologies/javase/jdk-faqs.html)를 확인했다. 런타임 법적 고지를 유지했다. 현재 버전의 조건을 다른 JDK/버전 또는 유료 배포에 일반화하지 않는다.
- LibGDX/LWJGL·네이티브 고지와 Nanum 기반 폰트 OFL을 동봉했다. OpenAL Soft의 LGPL을 BSD로 오인하지 않았으며 대응 소스·교체 조건은 공개 전 별도 검토 대상이다.
- **ending-ko는 맑은 고딕의 글자별 비트맵 아틀라스다.** [Microsoft 공식 FAQ](https://learn.microsoft.com/en-us/typography/fonts/font-faq)는 이 변환/게임 임베딩을 일반적으로 허용하지 않는다. 공개 전에 허용된 폰트로 교체하거나 별도 권한을 확보해야 한다. 이번 작업에서 원본 폰트 PNG는 임의 변경하지 않았다.
- 제공된 게임 이미지의 출처·배포 허용 증빙과 게임 자체 코드 라이선스는 확인되지 않았다. 임의의 라이선스를 부여하지 않았다.
- 완전히 깨끗한 Windows x64 PC, 다른 GPU·모니터 DPI, 장시간 실행은 추가 확인이 필요하다. 현재 런타임은 기존 설치 17.0.12로서 최신 보안 패치 버전이라는 주장은 하지 않는다.

## Release 준비

추천 태그 `v1.0.0`, 제목 `Campus Runner v1.0.0`. [한국어 설명 초안](RELEASE_v1.0.0_DRAFT.md)을 작성했다. 위 권리 확인을 해결한 뒤 ZIP·해시를 첨부할 수 있도록 위치만 준비했으며 GitHub Release를 게시하지 않았다.
