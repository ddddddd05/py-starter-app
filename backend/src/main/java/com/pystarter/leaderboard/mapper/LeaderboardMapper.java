package com.pystarter.leaderboard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pystarter.leaderboard.entity.LeaderboardEntry;
import org.apache.ibatis.annotations.Mapper;

/** MyBatis-Plus Mapper：继承 BaseMapper 直接获得单表 CRUD 与 LambdaQueryWrapper 能力。 */
@Mapper
public interface LeaderboardMapper extends BaseMapper<LeaderboardEntry> {
}
