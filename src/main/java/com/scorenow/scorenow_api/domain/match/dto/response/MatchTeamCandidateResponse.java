package com.scorenow.scorenow_api.domain.match.dto.response;

import com.scorenow.scorenow_api.domain.team.entity.Team;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "경기 등록용 팀 후보 응답")
public class MatchTeamCandidateResponse {

    @Schema(description = "팀 ID", example = "10")
    private Long id;

    @Schema(description = "종목 ID", example = "1")
    private Long sportId;

    @Schema(description = "팀명", example = "토트넘")
    private String name;

    @Schema(description = "축약 팀명", example = "TOT")
    private String shortName;

    @Schema(description = "팀 이미지 URL", example = "https://assets.b365api.com/images/team/m/10.png")
    private String imageUrl;

    public static MatchTeamCandidateResponse from(Team team) {
        return MatchTeamCandidateResponse.builder()
                .id(team.getId())
                .sportId(team.getSport() != null ? team.getSport().getId() : null)
                .name(team.resolveTeamName())
                .shortName(team.getSName())
                .imageUrl(team.getImageUrl())
                .build();
    }

}
