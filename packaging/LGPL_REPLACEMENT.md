# LGPL 구성요소: 고지·소스·교체 정보

이 패키지는 OpenAL Soft, JLayer-gdx, JOrbis/Jogg를 포함한다. 해당 구성요소에는 동봉 원문 LGPL이 적용되며 게임 전체 라이선스와 구분된다. 이 문서는 LGPL에 근거한 수정 분석/교체 권리를 제한하지 않는다. 그 밖의 게임 코드에 새로운 이용허락을 부여하지 않는다.

## 구성요소와 소스

| 구성요소 | 근거 | 동봉 자료 |
| --- | --- | --- |
| JLayer `1.0.1-gdx` | 실제 runtimeClasspath와 Maven POM. POM은 LGPL 2.1, 소스 헤더는 Library GPL v2-or-later 표기 | sources/jlayer-1.0.1-gdx-sources.jar, 같은 버전 POM, jlayer-ATTRIBUTION.txt, LGPL-2.0.txt 및 LGPL-2.1.txt |
| JOrbis/Jogg `0.0.17` | 실제 runtimeClasspath·동일 버전 소스 헤더: Library GPL v2-or-later | sources/jorbis-0.0.17-sources.jar, 같은 버전 POM, jorbis-ATTRIBUTION.txt, LGPL-2.0.txt |
| OpenAL Soft `1.23.1` | 배포 Windows x64/x86 OpenAL.dll의 `1.1 ALSOFT 1.23.1` 문자열. 상류 소스는 LGPL v2 계열 고지 및 파일별 예외 | sources/openal-soft-1.23.1.zip, OpenAL-Soft-1.23.1-COPYING.txt, openal_soft_license.txt |

Java 소스 JAR/POM은 사용한 Maven 배포 아티팩트와 같은 버전의 기존 Gradle 캐시에서 복사했다. OpenAL 소스는 [kcat/openal-soft 1.23.1](https://github.com/kcat/openal-soft/tree/1.23.1)의 공식 태그 아카이브다. 아카이브에는 CMake 빌드 설정과 파일별 고지도 들어 있다. 새 런타임 바이너리를 다운로드하거나 소스 코드를 수정하지 않았다.

원본 경로:

- [JLayer-gdx](https://github.com/libgdx/jlayer-gdx), Maven `com.badlogicgames.jlayer:jlayer:1.0.1-gdx`.
- [JOrbis](https://www.jcraft.com/jorbis/), Maven `org.jcraft:jorbis:0.0.17`.
- [LWJGL 3.3.3 Windows 빌드](https://github.com/LWJGL/lwjgl3/blob/3.3.3/config/windows/build.xml).

## 교체와 재실행

게임 JAR을 난독화·암호화하지 않았다. 수정 라이브러리 JAR을 원본 게임 JAR보다 먼저 classpath에 놓아 실행할 수 있다. 예를 들어 app-image 최상위 폴더에서:

```powershell
.\runtime\bin\java.exe -cp "replacement.jar;app\CampusRunner.jar" io.jbnu.test.lwjgl3.Lwjgl3Launcher
```

JLayer와 JOrbis는 각각 원래의 `javazoom.jl.*`, `com.jcraft.*` API와 리소스를 제공해야 한다. 번들 Java는 실행용이며 javac는 포함하지 않는다. 소스 빌드에는 해당 프로젝트의 POM/Java 호환 설정에 맞는 개발 도구가 필요하다. 소스 JAR은 빌드 완료 결과가 아니라 원본 코드 자료다.

OpenAL은 공유 DLL로 로드된다. LWJGL 3.3.3의 `org.lwjgl.openal.libname` 시스템 속성으로 API/아키텍처가 호환되는 수정 DLL 경로를 지정할 수 있다. JVM 옵션은 실행 시 전달하거나 개발용으로 복사한 app/CampusRunner.cfg에 설정할 수 있다. 원래 배포 ZIP은 보존하고 별도 복사본에서 작업한다.

## 공개 배포 전 남은 확인

NATIVE_INVENTORY.json에 실제 파일 이름·해시·바이너리 버전 근거를 기록했다. GLFW는 3.4.0 표시를 확인했지만 출시 태그와 동일하다는 뜻은 아니다. jemalloc의 버전 질의가 빌드 정보 누락 문자열을 반환한 항목도 미확인으로 남겼다. permissive 라이선스 구성요소의 고지 보존과 LGPL의 대응 소스 의무는 구분한다.

자료를 동봉하고 교체 경로를 문서화했지만, 다음 사항을 확인 완료로 선언하지 않는다.

- OpenAL DLL의 버전 문자열만으로 LWJGL-CI의 정확한 소스 커밋·패치·빌드 플래그를 완전히 증명할 수 없다. 공식 1.23.1 소스와 실제 바이너리의 대응 관계는 추가 확인이 필요하다.
- 다른 OS용 네이티브도 기존 fat JAR에 들어 있으므로 그 바이너리들의 대응 소스/조건도 공개 전 확인해야 한다.
- 제공한 소스로 수정 라이브러리를 실제 재빌드해 교체 실행하는 검증과 모든 LGPL 조건 이행 여부의 최종 확인은 남아 있다.

버전·원문·관련 소스의 존재와 해시는 확인했으나 이 문서만으로 공개 배포 의무가 모두 충족되었다고 단정하지 않는다. 확인되지 않은 항목은 배포 점검표에도 남긴다.
