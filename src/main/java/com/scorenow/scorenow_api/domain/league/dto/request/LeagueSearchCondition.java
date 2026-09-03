package com.scorenow.scorenow_api.domain.league.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "관리자 리그 검색 조건")
public class LeagueSearchCondition {

    @Schema(description = "리그 ID", example = "1")
    private Long leagueId;

    @Schema(description = "리그명 검색어. 한글명/영문명/숏네임 기준으로 검색합니다.", example = "Premier")
    private String keyword;
}
