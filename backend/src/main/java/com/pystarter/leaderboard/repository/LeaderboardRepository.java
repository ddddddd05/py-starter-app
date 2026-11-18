package com.pystarter.leaderboard.repository;

import com.pystarter.leaderboard.entity.LeaderboardEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaderboardRepository extends JpaRepository<LeaderboardEntry, Long> {

    Optional<LeaderboardEntry> findByPlayerId(String playerId);

    /** 查询前 50 名：按星数降序、经验降序、先达成者靠前。 */
    List<LeaderboardEntry> findTop50ByOrderByStarsDescXpDescUpdatedAtAsc();
}
