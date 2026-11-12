# 2026-11-12 계정 탈퇴 · 알림 · 상점 (묶음 B) 운영 적용

운영 DB는 `ddl-auto: validate`라서 새 테이블과 칼럼이 없으면 새 백엔드가 뜨지 않는다. 그래서 **SQL을 먼저 적용하고, 그다음 백엔드를 배포한다.** SQL은 더하기만 하므로 지금 돌고 있는 백엔드와도 호환된다.

## 바뀌는 것

- `domain_user.deleted_at`(NULL이면 활성 계정)
- 상점: `store_item`(시드 8개, 0~5,000P), `user_item`, `point_ledger`(지급 날짜 `earned_on`), `daily_problem`(날짜별 오늘의 다이브 포인트)
- 포인트 정책: 공개 문제 첫 정답 난이도×10P, 그날의 오늘의 문제 정답 +50P, 하루(서울 날짜) 한도 100P. 최고가 5,000P = 한도로 50일. 지난 풀이도 푼 날짜 기준 한도로 소급 지급(오늘의 문제 보너스는 기록이 생긴 뒤부터).
- 알림: `notification`, `notification_setting`, `notification_cursor`(시작값은 현재 `max(solve_result.id)`)

## 순서

1. 백업: API 서버에서 `pg_dump -Fc`로 운영 DB를 덤프한다(11/05에 고정한 PG17 이미지, 읽기 전용 세션).
2. `2026-11-12-account-store-notification.sql`을 적용한다. 한 트랜잭션이고, 잠금은 5초, 실행은 60초에서 중단된다.
3. 확인 쿼리 결과를 본다: 상품 8개, 새 테이블 7개, 처리 위치 행 1개, `deleted_at` 칼럼, 사용자 수 변화 없음.

1~3은 `obo-harness/experiments/production-deployment/store-20261112-apply.sh` 하나로 한다(`run-ssh.py`, 고정 호스트 키).
4. 백엔드를 배포한다(main push).
5. 확인한다.
   - `GET /api/store/items`: 200, 8개
   - `GET /api/problems/today`: 200, `daily_problem`에 오늘 행이 생김
   - `GET /api/notifications/unread-count`: 200(로그인 필요)
   - 정답 제출 20초 안에 알림이 오는지

## 되돌리기

백엔드를 이전 커밋으로 되돌리면 새 테이블은 쓰이지 않는다(이전 코드는 이 테이블을 모른다). 테이블을 지울 필요는 없다. 탈퇴 처리된 계정은 개인정보를 지웠으므로 스냅샷으로만 되돌릴 수 있다.

## 검증

임시 PostgreSQL 17에서 운영 순서를 재현했다(`AccountStoreNotificationPostgresTest`, `OBO_PG_TEST=local`, `PG_BIN`).

1. 엔티티 스키마에서 `deleted_at`을 빼 둔다.
2. SQL을 두 번 실행한다.
3. 새 엔티티를 `validate`로 띄운다.
4. 정산, 구매·착용, 정답·댓글·대회 알림, 탈퇴, 탐사 로그 쿼리를 실행한다.

하루 한도(140P → 100P)와 오늘의 문제 보너스를 포함해 9개 모두 통과다.
