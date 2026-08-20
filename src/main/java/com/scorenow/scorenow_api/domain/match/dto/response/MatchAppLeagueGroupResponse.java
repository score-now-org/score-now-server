package com.scorenow.scorenow_api.domain.match.dto.response;

import com.scorenow.scorenow_api.domain.league.entity.League;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "앱 리그별 경기 목록 응답")
public class MatchAppLeagueGroupResponse {
    @Schema(description = "리그 ID", example = "2")
    private Long leagueId;

    @Schema(description = "리그 이름", example = "프리미어리그")
    private String leagueName;

    @Schema(description = "리그에 속한 경기 목록")
    private List<MatchAppItemResponse> matches;

    public static MatchAppLeagueGroupResponse from(League league, List<MatchAppItemResponse> matches) {
        return MatchAppLeagueGroupResponse.builder()
                .leagueId(league.getId())
                .leagueName(league.resolveLeagueName())
                .matches(matches)
                .build();
    }

}
