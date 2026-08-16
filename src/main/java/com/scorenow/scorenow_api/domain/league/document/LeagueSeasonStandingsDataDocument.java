package com.scorenow.scorenow_api.domain.league.document;

import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.league.document.standings.StandingsData;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.global.entity.BaseDocument;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@Document(collection = "league_season_standing_data")
public class LeagueSeasonStandingsDataDocument extends BaseDocument {

    @Id
    private String id;

    /* 내부 정보 */
    private Long sportId;
    private SportCode sportCode;

    private Long leagueId;

    private String apiLeagueId;
    private DataOrigin dataOrigin;

    @Indexed(name = "uk_league_season_standing_data_document", unique = true)   // TODO: 추후 운영에서 인덱스 생성되는지 확인 필요
    private Long leagueSeasonId;

    private Long leagueSeasonStandingsId;

    private Instant syncedAt;

    /* 정규화된 순위 정보 */
    private StandingsData data;

    /* 외부 API 응답 시즌 정보 */
    private ExternalSeason externalSeason;

    /* 관리자 관리 */
    private List<GroupMapping> groupMappings;

    @Getter
    @Builder
    public static class ExternalSeason {
        private String name;
        private Long startAt;   // Unix Timestamp
        private Long endAt;     // Unix Timestamp
    }

    @Getter
    @Builder
    public static class GroupMapping {
        private String groupKey;
        private String displayName;
        private Integer displayOrder;
        private boolean visibleInMatchList;
    }

}
