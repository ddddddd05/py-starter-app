package com.pystarter.leaderboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pystarter.leaderboard.dto.RankItemDto;
import com.pystarter.leaderboard.dto.SubmitScoreRequest;
import com.pystarter.leaderboard.entity.LeaderboardEntry;
import com.pystarter.leaderboard.mapper.LeaderboardMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LeaderboardService {

    private final LeaderboardMapper mapper;
    private final String signSecret;

    public LeaderboardService(
            LeaderboardMapper mapper,
            @Value("${pystarter.leaderboard.sign-secret}") String signSecret) {
        this.mapper = mapper;
        this.signSecret = signSecret;
    }

    /**
     * 提交或更新成绩：
     * 1. @CacheEvict 在更新数据库后自动淘汰排行榜缓存，保证读写一致性；
     * 2. @Transactional 保证先查后改的原子性；
     * 3. 仅在星数或经验真正提升时刷新 updatedAt，避免重复上传刷新时间戳导致同分排名下降。
     */
    @Transactional
    @CacheEvict(value = "leaderboard", allEntries = true)
    public LeaderboardEntry submitScore(SubmitScoreRequest req) {
        String expected = sha256(req.playerId() + ":" + req.stars() + ":" + req.xp() + ":" + signSecret);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                req.signature().trim().toLowerCase().getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("签名校验失败，拒绝异常成绩提交");
        }

        LeaderboardEntry entry = mapper.selectOne(
                new LambdaQueryWrapper<LeaderboardEntry>()
                        .eq(LeaderboardEntry::getPlayerId, req.playerId())
        );

        boolean isNew = (entry == null);
        if (isNew) {
            entry = new LeaderboardEntry();
            entry.setPlayerId(req.playerId());
            entry.setStars(req.stars());
            entry.setXp(req.xp());
            entry.setLevel(req.level());
            entry.setUpdatedAt(System.currentTimeMillis());
        } else {
            int newStars = Math.max(entry.getStars(), req.stars());
            int newXp = Math.max(entry.getXp(), req.xp());
            boolean improved = newStars > entry.getStars() || newXp > entry.getXp();
            entry.setStars(newStars);
            entry.setXp(newXp);
            entry.setLevel(Math.max(entry.getLevel(), req.level()));
            if (improved) {
                entry.setUpdatedAt(System.currentTimeMillis());
            }
        }
        entry.setNickname(req.nickname().trim());

        if (isNew) {
            mapper.insert(entry);
        } else {
            mapper.updateById(entry);
        }
        return entry;
    }

    /** 查询 Top 50：使用 Spring Cache + Caffeine 缓存热点榜单，MyBatis-Plus LambdaQueryWrapper 分页查询。 */
    @Cacheable(value = "leaderboard", key = "'top50'")
    public List<RankItemDto> top50() {
        Page<LeaderboardEntry> page = mapper.selectPage(
                new Page<>(1, 50, false),
                new LambdaQueryWrapper<LeaderboardEntry>()
                        .orderByDesc(LeaderboardEntry::getStars)
                        .orderByDesc(LeaderboardEntry::getXp)
                        .orderByAsc(LeaderboardEntry::getUpdatedAt)
        );
        AtomicInteger rank = new AtomicInteger(1);
        return page.getRecords()
                .stream()
                .map(e -> new RankItemDto(
                        rank.getAndIncrement(),
                        e.getNickname(),
                        e.getStars(),
                        e.getXp(),
                        e.getLevel(),
                        e.getUpdatedAt()))
                .toList();
    }

    public static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
