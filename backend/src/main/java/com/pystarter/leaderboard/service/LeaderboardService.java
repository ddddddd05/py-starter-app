package com.pystarter.leaderboard.service;

import com.pystarter.leaderboard.dto.RankItemDto;
import com.pystarter.leaderboard.dto.SubmitScoreRequest;
import com.pystarter.leaderboard.entity.LeaderboardEntry;
import com.pystarter.leaderboard.repository.LeaderboardRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LeaderboardService {

    private final LeaderboardRepository repository;
    private final String signSecret;

    public LeaderboardService(
            LeaderboardRepository repository,
            @Value("${pystarter.leaderboard.sign-secret}") String signSecret) {
        this.repository = repository;
        this.signSecret = signSecret;
    }

    /**
     * 提交或更新成绩：
     * 1. 校验请求签名，防止抓包篡改 stars/xp；
     * 2. 使用 @Transactional 保证先查后改的原子性，且只保留更高的有效成绩。
     */
    @Transactional
    public LeaderboardEntry submitScore(SubmitScoreRequest req) {
        String expected = sha256(req.playerId() + ":" + req.stars() + ":" + req.xp() + ":" + signSecret);
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                req.signature().trim().toLowerCase().getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("签名校验失败，拒绝异常成绩提交");
        }

        LeaderboardEntry entry = repository.findByPlayerId(req.playerId())
                .orElseGet(LeaderboardEntry::new);
        entry.setPlayerId(req.playerId());
        entry.setNickname(req.nickname().trim());
        // 仅允许更新为更高或相等的进度，防止回退覆盖
        entry.setStars(Math.max(entry.getStars(), req.stars()));
        entry.setXp(Math.max(entry.getXp(), req.xp()));
        entry.setLevel(Math.max(entry.getLevel(), req.level()));
        entry.setUpdatedAt(System.currentTimeMillis());
        return repository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<RankItemDto> top50() {
        AtomicInteger rank = new AtomicInteger(1);
        return repository.findTop50ByOrderByStarsDescXpDescUpdatedAtAsc()
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
