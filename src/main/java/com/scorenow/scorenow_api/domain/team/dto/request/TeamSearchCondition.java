package com.scorenow.scorenow_api.domain.team.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "관리자 팀 검색 조건")
public class TeamSearchCondition {

    @Schema(description = "팀 ID", example = "1")
    private Long teamId;

    @Schema(description = "한글 팀명 검색어", example = "맨체스터")
    private String kName;

    @Schema(description = "영문 팀명 검색어", example = "manchester")
    private String eName;
}
