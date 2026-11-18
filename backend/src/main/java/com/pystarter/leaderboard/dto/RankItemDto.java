package com.pystarter.leaderboard.dto;

/** 返回给客户端的排行榜条目 DTO，避免直接暴露数据库实体字段。 */
public record RankItemDto(
        int rank,
        String nickname,
        int stars,
        int xp,
        int level,
        long updatedAt
) {}
