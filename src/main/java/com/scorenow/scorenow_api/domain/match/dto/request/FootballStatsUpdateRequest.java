package com.scorenow.scorenow_api.domain.match.dto.request;

import com.scorenow.scorenow_api.domain.match.document.sportdetail.football.FootballStats;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FootballStatsUpdateRequest {

    @Valid
    @NotNull
    private TeamStats homeStats;

    @Valid
    @NotNull
    private TeamStats awayStats;

    @Getter
    @NoArgsConstructor
    @Schema(description = "축구 경기 팀 스탯")
    public static class TeamStats {

        @Schema(description = "경고 수", example = "2")
        @PositiveOrZero
        private Integer yellowCards;

        @Schema(description = "퇴장 수", example = "0")
        @PositiveOrZero
        private Integer redCards;

        @Schema(description = "슈팅 수", example = "12")
        @PositiveOrZero
        private Integer shots;

        @Schema(description = "유효 슈팅 수", example = "5")
        @PositiveOrZero
        private Integer shotsOnTarget;

        @Schema(description = "점유율", example = "55")
        @PositiveOrZero
        @Max(100)
        private Integer possession;

        @Schema(description = "오프사이드 수", example = "1")
        @PositiveOrZero
        private Integer offsides;

        @Schema(description = "파울 수", example = "8")
        @PositiveOrZero
        private Integer fouls;

        @Schema(description = "코너킥 수", example = "6")
        @PositiveOrZero
        private Integer corners;

        @Schema(description = "프리킥 수", example = "10")
        @PositiveOrZero
        private Integer freeKicks;

        public FootballStats toFootballStats() {
            return FootballStats.builder()
                    .yellowCards(yellowCards)
                    .redCards(redCards)
                    .shots(shots)
                    .shotsOnTarget(shotsOnTarget)
                    .possession(possession)
                    .offsides(offsides)
                    .fouls(fouls)
                    .corners(corners)
                    .freeKicks(freeKicks)
                    .build();
        }
    }


}
