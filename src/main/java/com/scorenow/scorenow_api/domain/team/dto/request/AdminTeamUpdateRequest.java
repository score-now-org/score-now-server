package com.scorenow.scorenow_api.domain.team.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scorenow.scorenow_api.domain.team.entity.TeamType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "관리자 팀 수정 요청")
public class AdminTeamUpdateRequest {

    @Schema(description = "팀 타입. null이면 변경하지 않습니다.", example = "CLUB", allowableValues = {"CLUB", "NATIONAL"})
    @JsonProperty("teamType")
    private TeamType teamType;

    @Schema(description = "한글 팀명. null이면 변경하지 않습니다.", example = "맨체스터 유나이티드")
    @JsonProperty("kName")
    private String kName;

    @Schema(description = "영문 팀명. null이면 변경하지 않습니다.", example = "Manchester United")
    @JsonProperty("eName")
    private String eName;

    @Schema(description = "축약 팀명. null이면 변경하지 않습니다.", example = "MU")
    @JsonProperty("sName")
    private String sName;

    @Schema(description = "팀 이미지 URL. null이면 변경하지 않습니다.", example = "https://assets.b365api.com/images/team/m/12345.png")
    @JsonProperty("imageUrl")
    private String imageUrl;

    @Schema(description = "국가 코드. null이면 변경하지 않습니다.", example = "KR")
    @JsonProperty("cc")
    private String cc;
}
