CREATE TABLE IF NOT EXISTS leaderboard_entry (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    player_id  VARCHAR(64)  NOT NULL UNIQUE,
    nickname   VARCHAR(24)  NOT NULL,
    stars      INT          NOT NULL DEFAULT 0,
    xp         INT          NOT NULL DEFAULT 0,
    level      INT          NOT NULL DEFAULT 1,
    updated_at BIGINT       NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_rank_sort ON leaderboard_entry (stars DESC, xp DESC, updated_at ASC);
