package com.pystarter.leaderboard.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "leaderboard_entry",
    indexes = {
        @Index(name = "idx_rank_sort", columnList = "stars DESC, xp DESC, updated_at ASC")
    }
)
public class LeaderboardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false, unique = true, length = 64)
    private String playerId;

    @Column(nullable = false, length = 24)
    private String nickname;

    @Column(nullable = false)
    private int stars;

    @Column(nullable = false)
    private int xp;

    @Column(nullable = false)
    private int level;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt;

    public Long getId() { return id; }
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public int getStars() { return stars; }
    public void setStars(int stars) { this.stars = stars; }
    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
