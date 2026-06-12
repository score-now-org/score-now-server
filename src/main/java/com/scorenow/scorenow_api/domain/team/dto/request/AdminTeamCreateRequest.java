package com.scorenow.scorenow_api.domain.team.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scorenow.scorenow_api.domain.team.entity.TeamType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "관리자 팀 생성 요청")
public class AdminTeamCreateRequest {

    @Schema(description = "종목 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "종목 ID는 필수입니다.")
    private Long sportId;

    @Schema(description = "팀 타입", example = "CLUB", allowableValues = {"CLUB", "NATIONAL"}, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "팀 타입은 필수입니다.")
    @JsonProperty("teamType")
    private TeamType teamType;

    @Schema(description = "한글 팀명", example = "맨체스터 유나이티드", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "한글 팀명은 필수입니다.")
    @JsonProperty("kName")
    private String kName;

    @Schema(description = "영문 팀명", example = "Manchester United")
    @JsonProperty("eName")
    private String eName;

    @Schema(description = "축약 팀명", example = "MU")
    @JsonProperty("sName")
    private String sName;

    @Schema(description = "팀 이미지 URL", example = "https://assets.b365api.com/images/team/m/12345.png")
    @JsonProperty("imageUrl")
    private String imageUrl;

    @Schema(description = "국가 코드", example = "KR")
    @JsonProperty("cc")
    private String cc;
}
