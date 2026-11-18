package com.pystarter.leaderboard.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 上传成绩请求 DTO：使用 Bean Validation 在 Controller 层拦截非法参数。 */
public record SubmitScoreRequest(
        @NotBlank(message = "playerId 不能为空")
        @Size(max = 64, message = "playerId 长度不能超过 64")
        String playerId,

        @NotBlank(message = "昵称不能为空")
        @Size(max = 24, message = "昵称不能超过 24 个字符")
        String nickname,

        @Min(value = 0, message = "星数不能为负数")
        @Max(value = 36, message = "总星数上限为 36")
        int stars,

        @Min(value = 0, message = "经验值不能为负数")
        @Max(value = 20000, message = "单账号经验值超出合理范围")
        int xp,

        @Min(value = 1, message = "等级至少为 1")
        @Max(value = 200, message = "等级超出合理范围")
        int level,

        @NotBlank(message = "签名不能为空")
        String signature
) {}
