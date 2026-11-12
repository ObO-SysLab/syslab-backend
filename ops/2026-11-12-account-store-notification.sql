-- 2026-11-12 계정 탈퇴 · 알림 · 상점 (묶음 B)
-- 백엔드 배포 전에 적용한다(운영은 ddl-auto=validate라 테이블이 없으면 서버가 뜨지 않는다).
-- 더하기만 하므로 지금 돌고 있는 백엔드와도 호환된다. 여러 번 실행해도 결과가 같다.
-- 적용 전 RDS 스냅샷을 만든다. 절차는 2026-11-12-account-store-notification.md.
BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '60s';

-- 6. 계정 탈퇴: 탈퇴 시각(NULL이면 활성 계정)
ALTER TABLE domain_user ADD COLUMN IF NOT EXISTS deleted_at timestamp NULL;

-- 8. 상점
CREATE TABLE IF NOT EXISTS store_item (
    id          bigserial    PRIMARY KEY,
    category    varchar(20)  NOT NULL CHECK (category IN ('avatar', 'title', 'theme')),
    name        varchar(50)  NOT NULL,
    description varchar(200) NOT NULL,
    price       integer      NOT NULL CHECK (price >= 0),
    rarity      varchar(20)  NOT NULL,
    asset_key   varchar(50)  NOT NULL UNIQUE,
    sort_order  integer      NOT NULL DEFAULT 0,
    is_active   boolean      NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS user_item (
    id           bigserial PRIMARY KEY,
    user_id      bigint    NOT NULL REFERENCES domain_user(id) ON DELETE CASCADE,
    item_id      bigint    NOT NULL REFERENCES store_item(id),
    equipped     boolean   NOT NULL DEFAULT false,
    purchased_at timestamp NOT NULL,
    CONSTRAINT uk_user_item UNIQUE (user_id, item_id)
);

-- 포인트 장부. 잔액 = amount 합. 하루 획득 한도(100P)는 earned_on(서울 날짜)별 SOLVE·DAILY 합에 적용한다.
--   SOLVE    +난이도×10 (공개 문제 첫 정답, ref=문제 번호)
--   DAILY    +50 (그날의 오늘의 다이브 포인트 정답, ref=YYYYMMDD)
--   PURCHASE -가격 (ref=상품 번호)
CREATE TABLE IF NOT EXISTS point_ledger (
    id         bigserial   PRIMARY KEY,
    user_id    bigint      NOT NULL REFERENCES domain_user(id) ON DELETE CASCADE,
    amount     integer     NOT NULL,
    reason     varchar(20) NOT NULL CHECK (reason IN ('SOLVE', 'DAILY', 'PURCHASE')),
    ref_id     bigint      NOT NULL,
    earned_on  date        NOT NULL,
    created_at timestamp   NOT NULL,
    CONSTRAINT uk_point_ledger UNIQUE (user_id, reason, ref_id)
);
CREATE INDEX IF NOT EXISTS idx_point_ledger_user_day ON point_ledger (user_id, earned_on);

-- 날짜별 오늘의 다이브 포인트(서울 날짜). 지금까지는 Redis에만 있어 기록이 남지 않았다.
CREATE TABLE IF NOT EXISTS daily_problem (
    day     date   PRIMARY KEY,
    prob_id bigint NOT NULL REFERENCES problem_summary(id) ON DELETE CASCADE
);

-- 7. 알림
CREATE TABLE IF NOT EXISTS notification (
    id         bigserial    PRIMARY KEY,
    user_id    bigint       NOT NULL REFERENCES domain_user(id) ON DELETE CASCADE,
    type       varchar(30)  NOT NULL,
    title      varchar(100) NOT NULL,
    body       varchar(300) NOT NULL,
    link       varchar(300),
    ref_id     bigint       NOT NULL,
    is_read    boolean      NOT NULL DEFAULT false,
    created_at timestamp    NOT NULL,
    CONSTRAINT uk_notification UNIQUE (user_id, type, ref_id)
);
CREATE INDEX IF NOT EXISTS idx_notification_user_unread ON notification (user_id, is_read, created_at DESC);

-- 행이 없으면 기본값(풀이 성공 켬, 댓글 켬, 대회 시작 끔)으로 본다.
CREATE TABLE IF NOT EXISTS notification_setting (
    user_id       bigint  PRIMARY KEY REFERENCES domain_user(id) ON DELETE CASCADE,
    solve_correct boolean NOT NULL DEFAULT true,
    comment_reply boolean NOT NULL DEFAULT true,
    contest_start boolean NOT NULL DEFAULT false
);

-- 정답 알림 처리 위치. 시작값을 현재 최댓값으로 두어 과거 정답이 한꺼번에 알림으로 가지 않게 한다.
CREATE TABLE IF NOT EXISTS notification_cursor (
    name    varchar(50) PRIMARY KEY,
    last_id bigint      NOT NULL
);
INSERT INTO notification_cursor (name, last_id)
SELECT 'solve_result', COALESCE(MAX(id), 0) FROM solve_result
ON CONFLICT (name) DO NOTHING;

-- 상품 8개. 가격은 하루 한도 100P 기준 며칠 모아야 하는지로 정했다(최고가 5,000P = 50일).
INSERT INTO store_item (category, name, description, price, rarity, asset_key, sort_order) VALUES
    ('avatar', '기본 요원',     '기본 아바타 테두리',             0,    'Common',    'frame-default',       10),
    ('avatar', '네온 해커',     '분홍 네온 아바타 테두리',         1200, 'Rare',      'frame-neon',          20),
    ('avatar', '어셈블리 장인', '호박색 아바타 테두리',            1500, 'Rare',      'frame-asm',           30),
    ('avatar', '사이버 수사대', '청록빛 이중 아바타 테두리',       2500, 'Epic',      'frame-cyber',         40),
    ('title',  'Binary Hunter', '닉네임 옆에 붙는 칭호',           300,  'Common',    'title-binary-hunter', 50),
    ('title',  'Root Access',   '모든 권한을 가진 자의 칭호',      5000, 'Legendary', 'title-root-access',   60),
    ('theme',  '오션 드라이브', '푸른 물결 프로필 카드 배경',      800,  'Rare',      'theme-ocean-drive',   70),
    ('theme',  '다크 커널',     '터미널 격자 프로필 카드 배경',    3500, 'Legendary', 'theme-dark-kernel',   80)
ON CONFLICT (asset_key) DO NOTHING;

COMMIT;

-- 확인용(읽기 전용)
SELECT count(*) AS store_items FROM store_item;
SELECT name, last_id FROM notification_cursor;
SELECT column_name, data_type FROM information_schema.columns
WHERE table_name = 'domain_user' AND column_name = 'deleted_at';
