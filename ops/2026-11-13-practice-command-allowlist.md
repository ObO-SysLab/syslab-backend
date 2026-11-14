# 2026-11-13 실습 환경 정리 운영 적용

명령어 제한, 제작 실패 원인 표시, 수정 시 다시 빌드, 터미널 크기 맞춤, noexec 강화, 실습 화면 정리를 한 번에 반영하는 절차다. 단계마다 승인을 받고 순서대로 진행한다.

## 바뀌는 것

- **빌드** (infra `vm/codebuild`)
  - 허용 명령어가 있으면 출제자 이미지 위에 제한 층을 얹는다(`lab` 계정, 실행 권한 제거, 라벨 `net.diveon.restriction=v1`).
  - 성공하면 표식 태그 `build-<uuid>-cmd-…`를 함께 push한다.
  - 실패하면 같은 폴더의 `result.json`에 사유 코드를 남긴다. S3 트리거는 `source.zip`으로 끝나는 키에만 반응한다.
- **DB**: `problem_practice.command_allowlist`(jsonb)와 `build_failure_detail`(varchar 80)을 추가한다. 둘 다 NULL 허용이고, 기존 행은 NULL로 남는다.
- **백엔드**
  - 허용 목록을 검사해서 S3 메타데이터로 보낸다.
  - 표식 태그를 확인한 뒤에만 READY로 바꾼다. 없으면 `RESTRICTION_UNVERIFIED`로 실패 처리한다.
  - 실패 원인은 작성자에게만 보여 준다.
  - `POST /api/problems/practice/{id}/rebuild`로 다시 빌드한다.
  - ECR 이미지를 지울 때 표식 태그도 함께 지운다.
  - 실습 생성 요청에서 OS 이미지를 필수로 받지 않는다.
- **VM**
  - ws-server는 크기 프레임(`\0DVRS`)을 받아 PTY 크기를 바꾼다.
  - 도우미는 제한 이미지면 `/tmp`, `/var/tmp`, `~/work`를 noexec tmpfs로 덮고, `no-new-privileges`와 pids 256을 건다.
- **프런트**
  - 출제와 수정 화면에 허용 명령어를 넣었다.
  - 두 실습 화면을 공용 컴포넌트로 합쳤다: 공용 헤더, 초기화, 오답이면 유지, `H:MM:SS` 타이머와 만료 안내, 크기 전송.
  - 실패 원인 문구, 홈 문구, 약관(최종 수정일 2026-11-13, 배포일이 다르면 고친다)을 바꿨다.

## 순서

1. **IAM**
   - CodeBuild 역할에 `s3:PutObject`(`syslab-practice-dockerfile/prob-*/builds/*/result.json`)를 추가한다.
   - 백엔드(API EC2 역할)에 부족한 권한을 추가한다: `s3:GetObject`(result.json·source.zip), `s3:CopyObject`에 필요한 GetObject/PutObject, `ecr:BatchDeleteImage`.
   - 먼저 현재 권한을 조회해서 실제로 빠진 것만 넣는다.
2. **CodeBuild buildspec 교체**
   - infra `vm/codebuild/buildspec.yml` 내용을 `syslab-practice-build` 프로젝트에 넣는다. 바꾸기 전 buildspec은 보관한다.
   - 메타데이터가 없으면 지금과 같은 빌드다.
3. **SQL**: `2026-11-13-practice-command-allowlist.sql`. 열을 추가할 뿐이라 지금 백엔드와 호환되고, 운영이 `ddl-auto: validate`라서 4·5보다 먼저 해야 한다. 파일 끝의 확인 쿼리를 돌린다.
4. **VM** (11/05 방식)
   - 새 버전 폴더에 ws-server를 설치하고, `/usr/local/libexec/diveon-vm-helper.py`를 교체한 뒤 서비스를 전환한다. 이전 폴더와 도우미 파일은 보존한다.
   - 6(프런트)보다 먼저 해야 한다. 옛 ws-server는 크기 프레임을 셸 입력으로 넣어 버린다.
5. **백엔드 배포** (main push)
6. **프런트 배포** (main push)
7. **운영 확인**
   - 기존 문제: 실습, 초기화, 오답이면 유지, 정답이면 종료, vim 전체 화면, 공용 헤더
   - 시험용 제한 문제(예: `ls, cat`): READY, 표식 태그, `lab@` 프롬프트, 허용·거부, `~/work`에 쓴 파일 실행 거부
   - 없는 명령어를 넣은 문제: 작성자에게 "이미지에 … 명령어가 없습니다"가 보이는지
   - 허용 목록만 바꿔 다시 빌드
   - 시험 문제를 삭제한 뒤 ECR에서 태그 두 개가 모두 사라졌는지
   - 시험 문제는 확인 후 지운다.

## 되돌리기

배포의 역순으로 한다: 프런트 → 백엔드 → VM(이전 폴더와 도우미로 서비스 전환) → buildspec.

- buildspec만 먼저 되돌리면 새 제한 문제는 표식 태그가 없어 FAILED가 된다. 제한이 몰래 풀리는 방향이 아니라 막히는 방향이다.
- 도우미만 되돌리면 제한 이미지는 noexec 없이 돈다(이전 수준).
- 열과 IAM 추가분은 남겨 두어도 된다.

## 검증 (로컬)

- infra
  - 단위: `python -B -m unittest discover -s vm/codebuild -p 'test_*.py'`, `vm/lifecycle`
  - ws-server: `node --test auth.test.js server.test.js terminal.test.js`
- infra Linux: `DIVEON_RESTRICTION_LINUX=1 python -B -m unittest discover -s vm/codebuild -p 'test_restriction_linux.py' -v`
  - ubuntu와 debian에서 다음을 확인한다: 허용·거부, 셸 탈출, 동적 로더, 실패 코드, 도우미 옵션이 있을 때와 없을 때의 noexec 대조.
- 백엔드: JDK 21로 `./gradlew test`를 돌린다. 해시 예시 값은 infra 테스트와 같다.
- 프런트: `npx vitest run`, `npx tsc --noEmit`, `npm run build`

## 제한 기능의 한계 (문서로만 둔다)

구조상 막을 수 없는 것들이다. 출제 화면에는 안내하지 않는다.
- bash 내장 명령(`echo *`, `read`)으로 `ls`·`cat`을 흉내 낼 수 있다.
- `gcc`처럼 내부 프로그램(`cc1`·`as`·`ld`)을 부르는 명령어는 그 프로그램까지 허용해야 동작한다.
- busybox 이미지는 제한할 수 없어 빌드가 실패한다.
- `/home/lab`에 출제자가 넣은 실행 파일은 실행된다.

자세한 내용은 infra `vm/codebuild/README.md`에 있다.
