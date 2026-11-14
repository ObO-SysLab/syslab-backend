# syslab-backend

DiveOn([diveon.net](https://diveon.net))의 API 서버다. 회원, 문제 출제와 풀이, 제출과 채점 요청, 대회, 그룹, 실습 환경을 맡는다.
Spring Boot 3.5와 Java 21로 만들었고, 운영에서는 API EC2의 Docker 컨테이너로 돈다.

## 패키지

`src/main/java/net/diveon/backend/domain/` 아래에 기능별로 나뉜다.

| 패키지 | 내용 | 주요 경로 |
| --- | --- | --- |
| `user` | 회원가입, 로그인(JWT, 구글), 이메일 인증, 프로필, 랭킹 | `/api/auth`, `/api/users`, `/api/profile` |
| `problem` | 객관식·코딩형·실습형 문제 출제·수정·조회, OBO 문서 검증, 댓글, 실습 이미지 다시 빌드 | `/api/problems`, `/api/challenges` |
| `grade` | 제출과 채점. 객관식·실습형은 서버에서 바로 채점하고, 코딩형은 SQS로 채점기에 넘긴다 | `/api/submissions` |
| `contest` | 대회 생성·참가, 문제, 공지, 질문, 제출 현황 | `/api/contests` |
| `group` | 그룹, 멤버, 게시글 | `/api/groups` |
| `vm` | 실습 컨테이너 켜기·끄기, 웹 터미널 접속 티켓 | `/api/vm` |
| `store` | 포인트와 상점 | `/api/store` |
| `notification` | 알림 | `/api/notifications` |
| `activity`, `ad` | 최근 활동, 광고 자리 | `/api/activity`, `/api/ad` |

`global/`에는 공통 응답 형식, 예외 처리, 보안 설정(JWT 필터), S3 클라이언트가 있다.

## 연결된 것

```text
CloudFront ─ /api/* ─ API EC2 Nginx ─ Spring Boot (:8080)
                                       ├─ PostgreSQL 17 (RDS)
                                       ├─ Redis (같은 EC2): 대회 순위, 이메일 인증 코드, 갱신 토큰, 터미널 티켓
                                       ├─ S3: 제출 코드, 테스트케이스, 실습 Dockerfile
                                       ├─ SQS → 코딩 채점 Lambda (syslab-infra/grader)
                                       ├─ CodeBuild·ECR: 실습 이미지 빌드 (syslab-infra/vm/codebuild)
                                       └─ SSH → VM EC2: 실습 컨테이너 (syslab-infra/vm/lifecycle)
```

`obo-runtime/`은 OBO 시나리오 엔진의 복사본이다. 문제를 저장할 때 `node obo-runtime/checker.mjs`로 OBO 문서를 검사한다. 원본은 syslab-frontend의 `components/obo/runtime/`이고, 그쪽의 `scripts/export-obo-runtime.mjs`로 맞춘다.

## 로컬 실행

필요한 것: JDK 21, PostgreSQL 17, Redis, Node.js

`application.yml`은 `local` 프로필에 `secret`을 더해 읽는다. 아래 두 파일은 git에 올리지 않으므로 직접 만든다.

- `src/main/resources/application-local.yml`: 로컬 DB·Redis 주소
- `src/main/resources/application-secret.yml`: DB 계정, JWT 키, 메일, 구글 OAuth 같은 비밀 값

`local` 프로필에서는 S3, SQS, ECR, Docker, 구글 로그인이 모의 구현으로 바뀐다. AWS 자격 증명 없이 돌릴 수 있다.

```sh
./gradlew bootRun
./gradlew test
./gradlew bootJar verifyRelease   # JAR에 비밀 설정 파일이 들어가지 않았는지 검사
```

## 배포

[`.github/workflows/deploy.yml`](.github/workflows/deploy.yml)이 맡는다.

- PR, `fix/**` push: 테스트, JAR 빌드, Docker 이미지 빌드까지 한다. 배포는 하지 않는다.
- `main` push: 커밋 해시를 태그로 ECR에 이미지를 올린다. 그다음 API EC2 보안 그룹에 GitHub 실행기 IP를 잠깐 열어 SSH로 접속하고, `ops/deploy-backend.sh`로 컨테이너를 바꾼 뒤 그 규칙을 지운다.

운영 DB 스키마는 Hibernate가 바꾸지 않는다(`ddl-auto: validate`). 스키마를 바꿀 때는 `ops/`에 날짜별 SQL과 절차 문서를 두고, 배포 전에 따로 적용한다.
운영 비밀 설정은 서버의 `/run/diveon/`에서 읽고, 이미지에는 넣지 않는다.

## 작업 규칙

- `develop`에서 `feat/…`, `fix/…` 브랜치를 만들어 `develop`으로 PR을 보낸다. `develop`을 `main`에 병합하면 운영에 배포된다.
- 커밋 메시지: `feat: 로그인 API 구현`
- PR 제목: `[feat] 로그인 API 구현 (#21)`, 본문은 [PR 템플릿](.github/PULL_REQUEST_TEMPLATE.md)을 따른다.
