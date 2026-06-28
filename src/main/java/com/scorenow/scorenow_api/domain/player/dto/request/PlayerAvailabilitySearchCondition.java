package com.scorenow.scorenow_api.domain.player.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "관리자 선수 가용 상태 검색 조건 (관리자 스쿼드/결장자 페이지용)")
public class PlayerAvailabilitySearchCondition {

    @Schema(description = "선수 ID", example = "1")
    private Long playerId;

    @Schema(description = "선수명 검색어", example = "손흥민")
    private String playerName;

    @Schema(description = "팀 ID", example = "1")
    private Long teamId;

    @Schema(description = "팀명 검색어", example = "LAFC")
    private String teamName;
}
