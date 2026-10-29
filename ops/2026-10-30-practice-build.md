# AUDIT-07 실습 이미지 빌드 결과 확정

## 기존 문제와 확정한 동작

기존 소비자는 SUCCEEDED/FAILED만 저장하고 TIMED_OUT 등 다른 상태도 메시지를 삭제했다. 운영 buildspec의 post_build 자체 통지는 중지·시간 초과 때 누락될 수 있고, 문제 번호만으로 mutable prob-N 태그를 READY로 등록했다. 저장소 buildspec은 prob-1 고정으로 운영과도 달랐다.

사용자는 기존 S3→EventBridge→CodeBuild 자동 시작 유지, 요청별 ID, 모든 종료 상태, 30초 조회 복구, digest 고정을 선택했다. 결과 대기 상한은 제안한 2시간/30분 대신 **요청 후 10분**으로 확정했다. 실습 VM의 2시간 수명과 별개다.

- 생성 시 UUID·요청 시각·마감 시각을 DB에 기록하고 `prob-N/builds/UUID/source.zip`에 업로드한다. 그룹/대회 연결 검증을 업로드 전에 수행한다.
- S3 이벤트의 기존 S3_KEY 전달 계약을 유지한다. 신뢰하는 프로젝트 buildspec에 삽입한 Python이 정확한 키를 다운로드하며, 사용자 ZIP의 buildspec/스크립트로 오케스트레이션을 대체하지 않는다.
- CodeBuild ID의 UUID로 `build-UUID` 태그를 만든다. 실패하면 push를 실행하지 않으며 post_build의 자체 성공 통지를 제거한다. EventBridge의 프로젝트별 모든 Build State Change를 SQS에 원문 전달한다.
- 소비자는 이벤트의 상태·문제 번호를 신뢰하지 않는다. BatchGetBuilds의 프로젝트·ID/ARN·S3 소스 버킷·S3_KEY를 확인한다. API 조회 실패/알 수 없는 상태/레거시 형식/DB 오류는 ack하지 않는다. 기존 redrive/DLQ로 보존한다.
- 짧은 DB 행 잠금 안에서 현재 요청 ID와 최초로 연결한 빌드 ID를 비교한다. AWS 호출 중에는 잠금을 보유하지 않는다. 성공은 먼저 ID를 연결하고 ECR의 해당 빌드 태그 digest를 확인한 다음 READY로 확정한다.
- FAILED/FAULT/STOPPED/TIMED_OUT은 FAILED. 마감 시각 이상이면 DEADLINE_EXCEEDED. 확정 상태와 다른 요청/빌드의 늦은 결과는 덮어쓰지 않는다. 요청 시작에 저장한 마감은 폴링으로 연장하지 않는다.
- 누락 결과는 알려진 ID 재조회, 미연결 요청은 프로젝트 빌드 목록의 S3_KEY 대조로 복구한다. 기본 DB 배치 50개, 발견 조회 최대 10페이지(페이지당 AWS 최대 100개)이며 커서를 순환한다. 조회 범위를 넘는 요청은 이벤트 또는 다음 조회로 복구하며 마감 정책은 그대로 적용한다.
- AWS 조회와 독립된 30초 만료 예약 작업이 있다. 전용 3개 스케줄러 스레드로 소비/조회/만료를 분리하고 다른 예약 작업은 기존 단일 스레드로 유지한다. 10분은 결과 채택의 절대 마감이며 DB 실패 확정은 다음 정상 만료 처리 때 이루어진다. API/DB 중단·잠금 지연 중 10분 정각 반영을 보장하지 않는다.
- S3 삭제는 해당 요청의 정확한 키만 삭제한다. ECR 삭제는 공유 digest 자체가 아닌 해당 빌드 태그만 제거하며 기존 행의 prob-N/source.zip 및 prob-N 태그 삭제도 보존한다. VM 전달은 digest를 그대로 유지한다.

## 설정과 호환성

`practice.build.enabled=false`, `recovery-enabled=false`, `deadline-minutes=10`, `poll-ms=30000`, `batch-size=50`, `discovery-pages=10`, `project=syslab-practice-build`가 기본이다. 준비가 안 된 상태의 실습 생성은 503으로 차단한다. 기존 READY 이미지 조회/실습은 이 플래그와 별개다. 기존 소비자와 새 소비자를 동시에 활성화하면 안 된다.

수동 SQL은 `2026-10-30-practice-build.sql`. 새 컬럼·유일성/상태 제약·마감 인덱스를 준비했다. 과거 PENDING은 요청 ID가 없어 안전하게 연결할 수 없으므로 승인된 마이그레이션 시 FAILED/LEGACY_UNTRACKED로 확정한다. 과거 READY/FAILED와 이미지는 유지한다. 과거 결과를 추정해서 새 요청에 붙이지 않는다. 자동 재빌드/재시도 API는 추가하지 않았다.

현재 생성 트랜잭션에서 S3 업로드를 수행하는 구조는 유지했다. 업로드 이후 DB 커밋 실패/삭제와 겹친 빌드는 결과를 채택하지 않고 메시지가 DLQ로 갈 수 있다. 그 빌드와 고아 S3/ECR 산출물의 자동 중지/정리는 이번 구현에 없다. 운영 보존/정리 대상 목록을 승인 후 별도로 확정한다.

## 실제 AWS 읽기 조회 (2026-10-30 KST)

AWS Core STS GetCallerIdentity와 서울 리전 BatchGetProjects가 성공했다. 계정 290373175858의 `syslab-practice-build`는 **실행 제한 60분, 큐 대기 제한 480분**이다. 이 조회로 연결 복구를 확인했으며 설정은 변경하지 않았다. 환경변수/비밀값은 조회 결과로 출력하거나 저장하지 않았다.

플랫폼의 10분 실패 확정은 CodeBuild StopBuild 호출이 아니다. 현재 AWS 제한을 유지하면 뒤늦게 빌드가 계속 실행될 수 있다. 운영 검토안은 큐 대기 5분·실행 5분으로 각각 제한하는 것이다. 이 분배는 아직 운영 승인 전이며, 큰 이미지 호환성·시작 지연과 전체 10분 마감을 함께 검증한 후 확정한다. AWS의 두 제한만으로 S3 이벤트 전달 시간을 포함한 종단 간 10분 강제 종료를 보장하지 않는다.

## 검증

서버/DB/Docker 기동 없이 Java Mockito·상태 전이 테스트와 Python subprocess mock으로 검증한다. 실제 AWS 호출은 위 연결·설정 읽기 두 종류뿐이며 테스트에서는 사용하지 않는다.

검사 범위: 6개 상태, 10분 직전/정각/지연 저장, 중복·이전 요청·다른 빌드, API 장애, 누락 통지 발견/페이지 제한, ECR 미가시성 재시도, malformed/레거시 메시지 미삭제, 기본 비활성화, 요청 선기록, 정확한 S3/ECR 삭제, VM digest 전달, 빌드/push 실패, ZIP 경로 이탈·심볼릭 링크 거절.

최종 검증: **Java 184개 통과(신규 55 + 기존 129), 실패/오류 0**, 기존 opt-in VM PostgreSQL 6개는 미기동으로 건너뛰었다. Python 빌드 모의 10개와 VM helper 19개(기존 18 + digest 전달 1), 총 **29개 통과**. 첫 Java 실행에서 재시도 mock 재설정이 기존 예외를 다시 던지는 테스트 오류를 수정했고 최종 전체 선택 회귀는 통과했다. 실제 JPA 잠금/SQL·CodeBuild 빌드·EventBridge/SQS 전달·ECR pull·브라우저 전체 흐름은 미검증이다. 기존 VM 임시 DB 승인으로 이번 DB를 시작하지 않는다. AUDIT-01 P0 최우선 미해결·Terraform 보류를 유지한다.

## 운영 실행안 — 준비만 함, 실행 전 승인 필수

1. 운영 API/VM 기동 필요 여부, DB 변경, AWS 변경, 배포 대상·시간을 정해 별도 승인받는다. 기존 API 이미지 digest, 프로젝트 buildspec/환경/제한, 이벤트 규칙·대상·큐 정책·IAM을 읽기 백업한다. DB 백업과 영향받는 PENDING 행 목록을 보관한다. 현재 CI의 독립 자동 배포만으로 전환하지 않는다.
2. 실습 신규 생성과 이전 소비자를 중지하고 이전 빌드/메시지를 목록화한다. 구버전 consumer가 새 표준 이벤트를 문제 번호만으로 처리하지 못하게 한다. 기존 메시지를 무조건 purge하지 않는다. 레거시 통지는 보관/DLQ 대상으로 분리한다.
3. 승인된 DB에 SQL을 트랜잭션으로 적용한다. 잠금/제약 오류면 롤백한다. `ddl-auto=validate` 등 검토한 설정으로 수동 SQL을 대신 자동 변경하지 못하게 한다. 오래된 PENDING을 LEGACY_UNTRACKED로 바꾸는 데이터 변경도 승인 범위에 명시한다.
4. 프로젝트의 **신뢰하는 inline buildspec**에 인프라의 생성된 buildspec.yml 내용을 설정한다(단순히 사용자 ZIP 내부 파일명으로 지정하지 않는다). Python3/AWS CLI/Docker가 있는 기존 환경과 privileged 모드를 확인한다. ACCOUNT_ID/AWS_REGION/ECR_REPOSITORY 및 새 PRACTICE_SOURCE_BUCKET을 설정한다. 기존 S3 source 버킷과 S3_KEY InputTransformer를 유지하고 새 키로 자동 시작되는지 검증한다.
5. EventBridge 결과 규칙에 `result-event-pattern.json`을 적용하고 원문을 SQS에 전달한다. 계정·규칙 ARN으로 제한한 큐 정책, DLQ/redrive, 전달 실패 관측을 확인한다. 프로젝트별 모든 상태 이벤트를 받아도 실제 상태는 BatchGetBuilds로 재검증한다.
6. 백엔드 역할에 해당 프로젝트의 ListBuildsForProject/BatchGetBuilds, 해당 ECR 저장소 DescribeImages 조회만 추가 검토한다. 기존 S3/CodeBuild 역할은 새 요청 경로의 GetObject/PutObject 범위를 확인한다. CodeBuild의 자체 SQS SendMessage는 더 이상 필요하지 않으므로 다른 용도를 확인 후 제거한다. 비밀값을 새로 조회·문서화하지 않는다.
7. 승인된 제한 시간 분배(검토안 큐 5분/실행 5분), ECR 태그 불변 정책과 보존 규칙을 확인한다. build-UUID 태그가 덮어써지지 않게 하고 기존 prob-N 소비자의 영향도 확인한다. digest 고정은 오래된 이미지의 삭제까지 막는 기능이 아니므로 사용 중 이미지 보존이 필요하다.
8. 호환되는 API·VM helper를 배포하고 `enabled=true`, `recovery-enabled=true`를 함께 켠다. 테스트 작성자로 정상 생성/실습, 실패, 강제 중지, 시간 초과, 통지 누락, 중복·지연 이벤트, 10분 마감, ECR 조회 장애를 검증한다. 임의 운영 메시지 수신/재전송은 읽기 조회로 취급하지 않고 승인한 테스트 건에만 수행한다.

## 복구안 — 파괴적 원복 없이 호환 조합 유지

1. 신규 생성과 새 소비/복구를 기능 플래그로 끄고 관련 S3 자동 시작을 필요 시 승인 범위에서 멈춘다. 생성은 503, 기존 READY의 실습은 유지한다. 진행 중 빌드/메시지/요청 ID를 보존한다.
2. 새 형식으로 시작된 빌드가 남은 상태에서 구 소비자만 다시 켜지 않는다. 승인한 현재 빌드만 중지하거나 완료까지 격리하고, 새 형식의 메시지는 보관/DLQ로 유지한다. 전체 큐 purge·전체 이미지 삭제는 하지 않는다.
3. 저장한 API/buildspec/규칙/정책을 호환되는 조합으로 복원한다. 구 API는 새 기능 플래그를 모르므로 복구 중 생성 차단은 프록시의 해당 생성 경로 차단 등 승인한 외부 수단으로 유지한다. DB 컬럼은 additive 상태로 남기며 삭제하지 않는다. 새 READY의 digest를 가변 태그로 바꾸거나, 마감 FAILED를 PENDING으로 자동 되돌리지 않는다. 레거시 PENDING 복원도 별도 데이터 검토/승인 대상이다.
4. 승인한 테스트 요청의 S3 키·빌드 ID·ECR 태그만 정리한다. 같은 digest를 공유하는 다른 문제 이미지를 삭제하지 않는다. 실제 동작 검증이 끝날 때까지 생성 차단을 유지한다.

## 공식 계약 근거

- [CodeBuild 상태](https://docs.aws.amazon.com/codebuild/latest/APIReference/API_Build.html)
- [BatchGetBuilds](https://docs.aws.amazon.com/codebuild/latest/APIReference/API_BatchGetBuilds.html)
- [ListBuildsForProject](https://docs.aws.amazon.com/codebuild/latest/APIReference/API_ListBuildsForProject.html): 페이지 토큰, 100개 이상 프로젝트에서 sortOrder 제한.
- [빌드 상태 이벤트](https://docs.aws.amazon.com/codebuild/latest/userguide/sample-build-notifications.html): 특정 프로젝트의 상태 필터를 생략해 모든 상태 변경 수신.
