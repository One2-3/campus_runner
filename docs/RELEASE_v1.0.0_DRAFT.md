# Campus Runner v1.0.0 — Release 설명 초안

권장 태그: `v1.0.0` (생성하지 않음)

권장 Release 제목: `Campus Runner v1.0.0`

**게시 전 확인:** 이 문서는 초안이다. 자체 소스에 별도 오픈소스 사용권을 부여하지 않는 정책은 확정했다. OFL 폰트 고지는 포함됐으나 네이티브 LGPL 대응 소스·이미지 생성 기록 및 깨끗한 PC 확인은 미해결로 구분한다. 현재 ZIP은 내부 후보본이며 [최종 점검표](PUBLIC_RELEASE_REVIEW.md)를 기준으로 게시 여부를 판단한다.

---

## 학점을 모아 졸업까지 달려라!

Campus Runner는 1학년 1학기부터 4학년 2학기까지 달리는 캠퍼스 픽셀 아트 플랫폼 게임입니다.

- 서로 다른 8학기 맵과 계절별 캠퍼스 배경
- A+ 학점 별 61개와 최대 465점
- 과제 몬스터·가시·움직이는 발판
- 카페 아르바이트, 자격증 공부, 서버 연결 미니게임
- 학기별 성적표·최종 GPA와 5가지 졸업 엔딩
- 타이틀, 일시정지, 게임 오버 및 결과 UI

### Windows x64 실행

첨부 `CampusRunner-Windows-v1.0.0.zip` 전체를 압축 해제하고 `CampusRunner/CampusRunner.exe`를 실행하세요. Java·Gradle·IDE 설치는 필요 없습니다. EXE와 app/runtime/licenses 폴더를 함께 유지해야 합니다.

이동은 좌우 방향키, 점프는 Space, 일시정지는 Esc입니다. 메뉴는 위/아래와 Enter를 사용하며 미니게임별 조작은 시작 안내에서 확인할 수 있습니다.

### 검증과 알려진 한계

Gradle build와 기존 30·60·120fps 회귀 검증을 수행했습니다. 번들 런타임과 별도 압축 해제 경로에서 실행을 검증한 상세 결과는 저장소의 Windows 배포 문서를 확인하세요. Java가 전혀 설치되지 않은 깨끗한 PC에서의 검증은 별도로 필요합니다.

SHA-256은 함께 제공하는 `.zip.sha256` 파일로 확인하세요. 자동 업데이트나 새 네트워크 동작은 추가하지 않았습니다.

---

첨부 예정 파일 (아직 업로드하지 않음):

- `dist/license-reviewed/CampusRunner-Windows-v1.0.0.zip`
- `dist/license-reviewed/CampusRunner-Windows-v1.0.0.zip.sha256`
