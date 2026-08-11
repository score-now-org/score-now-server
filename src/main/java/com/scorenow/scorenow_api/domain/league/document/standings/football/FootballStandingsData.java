package com.scorenow.scorenow_api.domain.league.document.standings.football;

import com.scorenow.scorenow_api.domain.league.document.standings.StandingsData;
import com.scorenow.scorenow_api.external.betsapi.dto.BetsStandingsResponse;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.TypeAlias;

import java.util.List;

@Getter
@Builder
@TypeAlias("football-standings")
public class FootballStandingsData implements StandingsData {

    private List<StandingsGroup> groups;

    @Getter
    @Builder
    public static class StandingsGroup {
        private String groupKey;
        private String externalName;
        private String externalGroupName;
        private Integer currentRound;
        private Integer maxRounds;
        private List<Standing> standings;
    }

    @Getter
    @Builder
    public static class Standing {
        private String externalTeamId;
        private String externalTeamName;

        private Integer rank;       // 순위
        private Integer rankOrder;  // 동순위 고려한 정렬용 순위

        private Integer played;
        private Integer win;
        private Integer draw;
        private Integer loss;

        private Integer goalsFor;       // 득점
        private Integer goalsAgainst;   // 실점
        private Integer goalDifference; // 득실차
        private Integer points;         // 승점

        private ExternalPromotion externalPromotion;    //  외부 API 응답 승격,진출,강등 정보
    }

    @Getter
    @Builder
    public static class ExternalPromotion {
        private String name;
        private String shortName;
    }

}
