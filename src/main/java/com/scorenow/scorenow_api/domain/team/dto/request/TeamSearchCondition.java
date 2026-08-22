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

    @Schema(description = "팀명 통합 검색어. 한글명, 영문명, 숏네임을 함께 검색합니다.", example = "맨체스터")
    private String keyword;
}
