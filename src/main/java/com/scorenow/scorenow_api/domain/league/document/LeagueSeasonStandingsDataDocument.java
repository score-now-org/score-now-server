package com.scorenow.scorenow_api.domain.league.document;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.global.entity.BaseDocument;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@Document(collection = "league_season_standing_data")
public class LeagueSeasonStandingsDataDocument extends BaseDocument {

    @Id
    private String id;

    /* League + LeagueExternalMapping */
    private Long leagueId;
    private String apiLeagueId;
    private DataOrigin dataOrigin;

    /* LeagueSeason */
    @Indexed(   // TODO: 추후 운영에서 인덱스 생성되는지 확인 필요
            name = "uk_league_season_standing_data_document",
            unique = true
    )
    private Long leagueSeasonId;

    /* 외부 API 응답 */
    private String externalSeasonName;
    private Long externalSeasonStartAt; // Unix Timestamp
    private Long externalSeasonEndAt;   // Unix Timestamp

    /* 순위 정보 */
    private StandingsTable standingsTable;

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class StandingsTable {
        private String name;
        private String groupName;
        private Integer currentRound;
        private Integer maxRounds;
        private List<StandingsRow> rows;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class StandingsRow {
        private Integer position;
        private Integer positionChange;

        private Integer played;
        private Integer win;
        private Integer draw;
        private Integer loss;

        private Integer goalsFor;       // 득점
        private Integer goalsAgainst;   // 실점
        private Integer goalDifference; // 득실차
        private Integer points;         // 승점

        private Promotion promotion;
        private Team team;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Promotion {
        private String name;
        private String shortName;
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Team {
        private String apiTeamId;
        private String name;
        private String imageId;
        private String countryCode;
    }
}
