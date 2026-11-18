package com.pystarter.leaderboard.controller;

import com.pystarter.leaderboard.dto.RankItemDto;
import com.pystarter.leaderboard.dto.SubmitScoreRequest;
import com.pystarter.leaderboard.entity.LeaderboardEntry;
import com.pystarter.leaderboard.service.LeaderboardService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService service;

    public LeaderboardController(LeaderboardService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, Object> list() {
        List<RankItemDto> items = service.top50();
        return Map.of("count", items.size(), "items", items);
    }

    @PostMapping("/submit")
    public Map<String, Object> submit(@Valid @RequestBody SubmitScoreRequest req) {
        LeaderboardEntry saved = service.submitScore(req);
        return Map.of(
                "ok", true,
                "nickname", saved.getNickname(),
                "stars", saved.getStars(),
                "xp", saved.getXp(),
                "level", saved.getLevel()
        );
    }
}
