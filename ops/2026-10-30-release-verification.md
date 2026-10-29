# AUDIT-08 백엔드 검증·배포 계약

- 이전: build -x test, application-secret.yml을 CI에서 작성해 JAR/이미지 포함, latest 자동 교체 및 이미지 일괄 정리.
- 변경: test/bootJar/verifyRelease 성공 후 커밋·체크섬이 붙은 JAR/ops 아티팩트만 생성. AWS 인증/push/SSH 자동 배포 없음. 비밀·local 설정·키를 bootJar에서 제외하고 verifyRelease로 검사한다.
- Dockerfile은 단일 diveon-backend.jar만 COPY. JVM -D 옵션 순서를 고쳤다. prod 스키마는 validate만 사용한다.
- CodingGradeJobStoreTest는 DIVEON_GRADING_PG_TEST=approved와 PG_BIN 필수. Windows/Unix 실행 파일 지원. VmSessionPostgresTest는 기존 DIVEON_VM_PG_TEST=approved 유지. 실제 앱 context 테스트는 DIVEON_CONTEXT_TEST=approved로 분리한다.
- 기본 전체 test는 외부 DB/서버를 띄우지 않는다. 제외된 통합 테스트는 성공으로 계산하지 않는다.
- 실행안: 정확한 커밋/JAR checksum 기록 → 운영 스키마 대조·백업·승인 SQL → 외부 secret 파일 준비 → 승인 후 이미지 교체. 비밀 파일을 /run/diveon/application-secret.yml에 읽기 전용 마운트하고 SPRING_CONFIG_ADDITIONAL_LOCATION=file:/run/diveon/ 사용.
- 복구안: 신규 접수 중단, 직전 호환 이미지/config로 복귀. 기존 데이터/스키마/복구용 이미지는 보존한다. 격리 없는 채점 경로는 재개하지 않는다.
- 전체 운영 적용 순서와 세 저장소 릴리스 묶음 계약: syslab-infra/ops/RELEASE.md.
- 검증 상태: 로컬 전체 Java 209개 통과, 별도 승인 DB/context 15개 skip, bootJar/verifyRelease 통과. 실제 GitHub 실행·Docker 빌드·DB/서버/배포는 이 작업에서 실행하지 않는다.
