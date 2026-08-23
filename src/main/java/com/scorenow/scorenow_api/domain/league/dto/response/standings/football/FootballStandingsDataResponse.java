package com.scorenow.scorenow_api.domain.league.dto.response.standings.football;

import com.scorenow.scorenow_api.domain.league.document.standings.football.FootballStandingsData;
import com.scorenow.scorenow_api.domain.league.dto.response.standings.StandingsDataResponse;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FootballStandingsDataResponse implements StandingsDataResponse {

    private List<FootballStandingsGroup> groups;

    @Getter
    @Builder
    public static class FootballStandingsGroup {
        private String name;            // 순위 그룹명
        private Integer order;          // 그룹 순서
        private Integer currentRound;   // 현재 라운드
        private Integer maxRounds;      // 총 라운드

        List<Standing> standings;       // 순위 정보
    }

    @Getter
    @Builder
    public static class Standing {
        private Long teamId;
        private String teamName;
        private String teamImageUrl;

        private Integer rank;
        private Integer played;
        private Integer win;
        private Integer draw;
        private Integer loss;
        private Integer goalDifference;
        private Integer points;

        public static Standing from(Team team, FootballStandingsData.Standing standing) {
            return Standing.builder()
                    .teamId(team.getId())
                    .teamName(team.resolveTeamName())
                    .teamImageUrl(team.getImageUrl())
                    .rank(standing.getRank())
                    .played(standing.getPlayed())
                    .win(standing.getWin())
                    .draw(standing.getDraw())
                    .loss(standing.getLoss())
                    .goalDifference(standing.getGoalDifference())
                    .points(standing.getPoints())
                    .build();
        }
    }

}
