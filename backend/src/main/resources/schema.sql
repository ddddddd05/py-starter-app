-- MySQL 建表脚本：在线排行榜表 + 联合排序索引
CREATE DATABASE IF NOT EXISTS pystarter DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pystarter;

CREATE TABLE IF NOT EXISTS leaderboard_entry (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    player_id  VARCHAR(64)  NOT NULL COMMENT '设备/玩家唯一标识',
    nickname   VARCHAR(24)  NOT NULL COMMENT '玩家昵称',
    stars      INT          NOT NULL DEFAULT 0 COMMENT '总星数 (0~36)',
    xp         INT          NOT NULL DEFAULT 0 COMMENT '总经验值',
    level      INT          NOT NULL DEFAULT 1 COMMENT '当前等级',
    updated_at BIGINT       NOT NULL COMMENT '最后更新时间戳 (ms)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_player_id (player_id),
    -- 联合索引直接覆盖 ORDER BY stars DESC, xp DESC, updated_at ASC，避免全表 filesort
    KEY idx_rank_sort (stars DESC, xp DESC, updated_at ASC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PyStarter 全服排行榜';
