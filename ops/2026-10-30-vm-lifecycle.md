# AUDIT-03/04 VM 수명주기 — 로컬 구현 / 운영 적용 전

## 변경과 계약

- 기존에는 사용자·문제별 첫 RUNNING 행으로 종료 대상을 찾고, Docker 삭제 실패를 경고만 남긴 채 DB를 STOPPED로 바꿀 수 있었다. DB 트랜잭션 안에서 원격 생성을 수행하여 외부 생성과 DB 롤백이 분리될 수 있었고, 만료는 주로 화면 표시였다.
- 이제 `CREATING → RUNNING → STOPPING → STOPPED`. 사용자 행에 짧은 DB 잠금을 걸고 생성/정리 의도를 별도 REQUIRES_NEW 트랜잭션으로 먼저 확정한다. SSH 동안 DB 잠금을 잡지 않는다. 활성 세션은 사용자당 하나이며 CREATING/STOPPING이 있으면 새 생성은 409다.
- 생성/조회 응답에는 `sessionId`, `expiresAtEpochMs`를 추가한다. 종료 DELETE `/api/vm/stop`, 리셋 POST `/api/vm/reset` 요청은 `{probId, sessionId}` 필수다. 로그인 사용자·문제·세션을 함께 조회한다. 이미 종료한 세션의 종료는 멱등이며, 종료한 세션의 리셋은 409다. 오래된 리셋이 새 세션을 종료하지 않는다.
- 2시간은 최초 예약 시점부터이며 재사용해도 연장하지 않는다. DB의 기존 LocalDateTime 해석은 유지한다. **운영 JVM/DB 기록의 시간대 일치 확인이 필수**이며 이 변경에서 기존 타임스탬프를 임의 변환하지 않는다.
- 기존 접근 정책과 터미널 인증을 유지한다. CREATING/STOPPING은 RUNNING 조회에 포함되지 않아 터미널 재검증에서 거부된다. 기존 node-pty 파일은 변경하지 않는다.
- 원격 결과가 불명확하면 503, DB 의도는 남겨 재시도한다. 삭제가 실제 MISSING으로 확인되기 전 STOPPED로 확정하지 않는다. 기존 중복·잘못된 컨테이너 식별자는 자동 수정하지 않는다.

## 원격 도우미

인프라 저장소 `vm/lifecycle/diveon_vm_helper.py`를 승인 후 VM 호스트 `/usr/local/libexec/diveon-vm-helper.py`에 설치한다. Python 표준 라이브러리와 Docker CLI가 필요하다. SSH 계정 소유의 `/var/lib/diveon-vm` 디렉터리는 0700으로 준비하며 재시작/배포 간 유지한다. helper 파일은 제출 컨테이너에서 수정할 수 없어야 한다. 상태 디렉터리나 Docker 소켓을 제출 컨테이너에 마운트하지 않는다.

- 고정 SSH 명령에 JSON stdin만 전달한다. 이미지/식별자를 셸 명령에 보간하지 않는다.
- `vm.ssh-known-hosts`는 신뢰 경로에서 확인한 VM 호스트 키가 들어 있는 파일 경로다. 호스트 키 확인을 끄거나 단순 ssh-keyscan 결과를 검증 없이 신뢰하지 않는다. 미설정/키 불일치는 실패한다.
- 새 컨테이너는 `diveon-vm-s{sessionId}`와 사용자·문제·세션·만료 라벨을 대조한다. Docker 인자는 네트워크 none, 메모리 256m, CPU 0.3, sleep infinity를 유지하며 init을 추가한다.
- 원격 manifest를 생성 전에 기록하고, 삭제 전 영속 종료 기록을 남긴다. 종료한 ID의 늦은 생성 요청을 차단한다. Docker 생성 응답이 늦게 확정되는 경우도 manifest 순회가 다시 정리한다.
- 기존 runtime_version=0 컨테이너는 DB의 전체 64자리 ID와 기존 `/syslab-vm-{userId}-{probId}` 이름을 모두 확인한다. 짧은 ID/이름 불일치/알 수 없는 runtime_version은 수동 대조 전까지 실패한다.
- manifest/종료 기록을 임의 삭제하지 않는다. 이름/라벨/manifest가 맞지 않는 컨테이너나 기록 없는 컨테이너는 자동으로 채택하거나 삭제하지 않는다.

Docker 문서: [create 후 start](https://docs.docker.com/reference/cli/docker/container/create/), [inspect 형식](https://docs.docker.com/reference/cli/docker/container/inspect/), [정확한 대상 rm --force](https://docs.docker.com/reference/cli/docker/container/rm/).

## 자동 정리

`vm.recovery.enabled`는 기본 비활성화다. 승인된 운영 검증 후에만 true로 설정한다. prod 프로필에서만 동작하며 `vm.recovery.poll-ms` 기본 30000이다.

DB 활성 세션을 ID 순서로 최대 20개씩 순환한다. STOPPING, 2분 이상 CREATING, 만료/권한 회수/실제 중지 RUNNING을 정리한다. 개별 실패가 뒤의 세션을 막지 않도록 커서를 이동한다. 원격 manifest도 별도 순환해 DB STOPPED 이후 늦게 생긴 자원을 찾는다. API 프로세스·DB·SSH·Docker 장애 시 정리가 지연되며, 배치와 요청 제한 때문에 **30초는 주기이지 삭제 완료 보장 시간이 아니다**. 무기한 중단에 대비한 독립 감시/운영 알림 검증은 별도 잔여 사항이다.

## 로컬 임시 PostgreSQL 검증 실행안 — 승인 필요

현재 검증: Java 선택 회귀 109개 통과(신규 39 + 기존 70), 원격 helper 모의 18개, 프론트 55개 및 TypeScript 검사 통과. 아래 실제 PostgreSQL 테스트 6개는 승인 대기 상태로 건너뛰었다. Linux flock·실제 SSH/Docker·브라우저 통합은 아직 실행하지 않았다.

대상은 `VmSessionPostgresTest`다. 기본 환경에서는 건너뛰며 `DIVEON_VM_PG_TEST=approved`가 있어야 실행한다. 전체 애플리케이션 컨텍스트/설정 파일을 로드하지 않고 User·VmSession 엔티티와 실제 JPA 저장소·트랜잭션 프록시만 사용한다.

1. 설치된 PostgreSQL 17의 initdb/pg_ctl을 사용해 OS 임시 디렉터리 `diveon-vm-pg-*`에 새 DB를 만든다. 기존 클러스터를 재사용하지 않는다.
2. 127.0.0.1의 동적 포트에서만 기동한다. 로컬 일회성 trust 인증이며 외부 인터페이스를 열지 않는다. 운영 자격증명·AWS·SSH·Docker는 사용하지 않는다.
3. 임시 스키마에서 준비한 SQL 마이그레이션과 사용자 잠금, 동시 예약, 활성 세션 유일성, 외부 롤백과 독립된 의도 확정, 늦은 활성화 차단을 검사한다.
4. 성공/실패 모두 해당 데이터 디렉터리의 pg_ctl fast stop을 실행한다. 로그와 테스트 디렉터리는 삭제하지 않고 남긴다. 기동 중 실패해도 같은 정리 절차를 수행한다.

승인 후 백엔드 디렉터리에서만 실행:

```powershell
$env:DIVEON_VM_PG_TEST='approved'
$env:PG_BIN='C:/Program Files/PostgreSQL/17/bin'
$initFile=Join-Path $env:TEMP 'syslab-backend-regression/isolated-build.gradle'
try {
  & 'C:/Program Files/Java/jdk-21/bin/java.exe' '-Dgradle.user.home=C:/Users/ChoHyunsu/.gradle' -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain test --tests '*VmSessionPostgresTest' --no-daemon --console=plain --init-script $initFile
} finally {
  Remove-Item Env:DIVEON_VM_PG_TEST -ErrorAction SilentlyContinue
  Remove-Item Env:PG_BIN -ErrorAction SilentlyContinue
}
```

복구: 테스트가 강제 종료되어 DB가 남으면 출력된 **해당 임시 디렉터리**의 절대 경로가 OS TEMP 아래인지 확인하고 `pg_ctl -D <그 디렉터리/data> -m fast -w stop`만 실행한다. 다른 PostgreSQL 프로세스를 일괄 종료하거나 폴더를 재귀 삭제하지 않는다. 이 승인 범위에 실제 Docker/SSH/서비스 서버 기동은 포함하지 않는다.

## 운영 적용 실행안 — 별도 승인 필요 / 미실행

1. VM 생성·종료·리셋 경로를 유지보수 상태로 닫고 API 스케줄러를 끈다. 실제 DB 스키마/시간대·모든 활성 행과 VM Docker 컨테이너를 읽기 전용 대조한다. 중복/고아/짧은 ID는 목록을 만들어 개별 정리 승인을 받는다.
2. DB 복구 가능한 백업과 현재 API/프론트/helper 설정 버전을 기록한다. `2026-10-30-vm-lifecycle.sql`은 활성 중복/알 수 없는 상태가 있으면 중단하도록 준비했다. 기존 동명 인덱스의 정의·유효성도 확인한다. 운영 DB에는 아직 실행하지 않았다.
3. VM helper·영속 상태 디렉터리·검증된 SSH known_hosts를 설치하고 실제 Linux flock/권한, Docker 이미지 pull, create/start/inspect/remove, 지연/장애 후 재시도를 시험용 세션으로 검증한다. API·프론트 계약을 함께 배포한다. 처음에는 자동 정리를 끈다.
4. 생성/재사용/종료/리셋, 만료·권한 회수, 오래된 탭, 동시 생성, SSH 단절·DB 실패 후 복구, 기존 세션의 정확한 이름/ID 대응을 확인한 뒤 자동 정리를 활성화하고 backlog·삭제 실패 로그를 관찰한다. 운영 VM/DB/AWS 기동이나 변경은 이 문서만으로 승인된 것이 아니다.

복구: VM 경로를 닫고 자동 정리를 끈 뒤 해당 버전의 활성 세션·manifest를 보존한다. 새 형식 세션을 구버전 코드가 관리하게 두지 않는다. 필요한 세션을 새 helper로 정확히 정리하고 DB STOPPED를 확인한 후 API·프론트를 함께 이전 버전으로 되돌리는 별도 실행안을 승인받는다. 추가 컬럼·인덱스·종료 기록은 즉시 제거하지 않는다. 새 세션이 남은 채 단순 바이너리 롤백으로 서비스를 재개하지 않는다.

AUDIT-01 실행 격리의 운영 검증은 여전히 최우선 미해결이다. 이 변경은 전체 배포 승인을 의미하지 않는다.
