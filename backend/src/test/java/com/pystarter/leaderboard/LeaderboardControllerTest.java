package com.pystarter.leaderboard;

import com.pystarter.leaderboard.service.LeaderboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LeaderboardControllerTest {

    @Autowired
    private MockMvc mvc;

    private static final String SECRET = "PyStarter-2026-Secret";

    @Test
    void submitAndRankOrderWorks() throws Exception {
        String sigA = LeaderboardService.sha256("dev-a:12:350:" + SECRET);
        String sigB = LeaderboardService.sha256("dev-b:18:200:" + SECRET);

        mvc.perform(post("/api/leaderboard/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerId":"dev-a","nickname":"小骑士A","stars":12,"xp":350,"level":4,"signature":"%s"}
                                """.formatted(sigA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        mvc.perform(post("/api/leaderboard/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerId":"dev-b","nickname":"小法师B","stars":18,"xp":200,"level":3,"signature":"%s"}
                                """.formatted(sigB)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].nickname").value("小法师B"))
                .andExpect(jsonPath("$.items[1].nickname").value("小骑士A"));
    }

    @Test
    void rejectsOutOfRangeStars() throws Exception {
        String sig = LeaderboardService.sha256("dev-c:99:100:" + SECRET);
        mvc.perform(post("/api/leaderboard/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerId":"dev-c","nickname":"作弊者","stars":99,"xp":100,"level":2,"signature":"%s"}
                                """.formatted(sig)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void rejectsForgedSignature() throws Exception {
        mvc.perform(post("/api/leaderboard/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerId":"dev-d","nickname":"伪造者","stars":30,"xp":500,"level":6,"signature":"bad-sig"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.ok").value(false));
    }
}
