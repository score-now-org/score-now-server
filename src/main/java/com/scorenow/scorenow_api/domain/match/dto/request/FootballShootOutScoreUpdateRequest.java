package com.scorenow.scorenow_api.domain.match.dto.request;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballShootOutScore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FootballShootOutScoreUpdateRequest {
    @NotNull
    @Schema(description = "홈팀 승부차기 점수", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
    @PositiveOrZero
    private Integer homeShootOutScore;

    @NotNull
    @Schema(description = "원정팀 승부차기 점수", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    @PositiveOrZero
    private Integer awayShootOutScore;

    public FootballShootOutScore toFootballShootOutScore() {
        return FootballShootOutScore.builder()
                .homeScore(homeShootOutScore)
                .awayScore(awayShootOutScore)
                .build();
    }
}
