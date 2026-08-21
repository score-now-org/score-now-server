package com.scorenow.scorenow_api.domain.league.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.common.enums.TeamDisplayOrder;
import com.scorenow.scorenow_api.domain.league.entity.League;
import com.scorenow.scorenow_api.domain.sport.entity.Sport;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "관리자 리그 응답")
@Getter
@Builder
public class AdminLeagueResponse {

    @Schema(description = "리그 ID", example = "1")
    private Long id;

    @Schema(description = "종목 ID", example = "1")
    private Long sportId;

    @Schema(description = "종목 코드", example = "FOOTBALL")
    private SportCode sportCode;

    @Schema(description = "종목명", example = "축구")
    private String sportName;

    @Schema(description = "한글 리그명", example = "프리미어리그")
    @JsonProperty("kName")
    private String kName;

    @Schema(description = "영문 리그명", example = "Premier League")
    @JsonProperty("eName")
    private String eName;

    @Schema(description = "숏 리그명", example = "EPL")
    @JsonProperty("sName")
    private String sName;

    @Schema(description = "팀 표시 순서", example = "HOME_AWAY", allowableValues = {"HOME_AWAY", "AWAY_HOME"})
    private TeamDisplayOrder teamDisplayOrder;

    @Schema(description = "데이터 원천", example = "BETS", allowableValues = {"BETS", "MANUAL"})
    private DataOrigin dataOrigin;

    @Schema(description = "외부 API 경기 동기화 활성화 여부. 수동 관리 리그이면 null입니다.", example = "true")
    private Boolean syncEnabled;

    public static AdminLeagueResponse from(League league) {
        return from(league, league.getSport() != null ? league.getSport().resolveSportName() : null, null);
    }

    public static AdminLeagueResponse from(League league, String sportName) {
        return from(league, sportName, null);
    }

    public static AdminLeagueResponse from(League league, Boolean syncEnabled) {
        return from(league, league.getSport() != null ? league.getSport().resolveSportName() : null, syncEnabled);
    }

    public static AdminLeagueResponse from(League league, String sportName, Boolean syncEnabled) {
        SportCode sportCode = league.getSport() != null ? league.getSport().getSportCode() : null;
        return from(league, sportName, sportCode, syncEnabled);
    }

    public static AdminLeagueResponse from(League league, Sport sport, Boolean syncEnabled) {
        String sportName = sport != null ? sport.resolveSportName() : null;
        SportCode sportCode = sport != null ? sport.getSportCode() : null;
        return from(league, sportName, sportCode, syncEnabled);
    }

    private static AdminLeagueResponse from(
            League league,
            String sportName,
            SportCode sportCode,
            Boolean syncEnabled) {
        return AdminLeagueResponse.builder()
                .id(league.getId())
                .sportId(league.getSportId())
                .sportCode(sportCode)
                .sportName(sportName)
                .kName(league.getKName())
                .eName(league.getEName())
                .sName(league.getSName())
                .teamDisplayOrder(league.resolveTeamDisplayOrder())
                .dataOrigin(league.getDataOrigin())
                .syncEnabled(syncEnabled)
                .build();
    }

    @JsonProperty("kName")
    public String getKName() {
        return kName;
    }

    @JsonProperty("eName")
    public String getEName() {
        return eName;
    }

    @JsonProperty("sName")
    public String getSName() {
        return sName;
    }
}
