package com.scorenow.scorenow_api.domain.team.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scorenow.scorenow_api.domain.common.enums.DataOrigin;
import com.scorenow.scorenow_api.domain.team.entity.Team;
import com.scorenow.scorenow_api.domain.team.entity.TeamType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "관리자 팀 응답")
public class AdminTeamResponse {

    @Schema(description = "팀 ID", example = "1")
    private Long id;

    @Schema(description = "종목 ID", example = "1")
    private Long sportId;

    @Schema(description = "팀 타입", example = "CLUB", allowableValues = {"CLUB", "NATIONAL"})
    private TeamType teamType;

    @Schema(description = "한글 팀명", example = "맨체스터 유나이티드")
    @JsonProperty("kName")
    private String kName;

    @Schema(description = "영문 팀명", example = "Manchester United")
    @JsonProperty("eName")
    private String eName;

    @Schema(description = "축약 팀명", example = "MU")
    @JsonProperty("sName")
    private String sName;

    @Schema(description = "팀 이미지 URL", example = "https://assets.b365api.com/images/team/m/12345.png")
    private String imageUrl;

    @Schema(description = "국가 코드", example = "KR")
    private String cc;

    @Schema(description = "데이터 원천", example = "BETS", allowableValues = {"BETS", "MANUAL"})
    private DataOrigin dataOrigin;

    public static AdminTeamResponse from(Team team) {
        return AdminTeamResponse.builder()
                .id(team.getId())
                .sportId(team.getSport() != null ? team.getSport().getId() : null)
                .teamType(team.getType())
                .kName(team.getKName())
                .eName(team.getEName())
                .sName(team.getSName())
                .imageUrl(team.getImageUrl())
                .cc(team.getCc())
                .dataOrigin(team.getDataOrigin())
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
