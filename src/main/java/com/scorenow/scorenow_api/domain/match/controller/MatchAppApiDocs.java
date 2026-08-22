package com.scorenow.scorenow_api.domain.match.controller;

import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppResponse;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;

@Tag(name = "Match App API", description = "앱 경기 목록 조회 API")
public interface MatchAppApiDocs {

    @Operation(
            summary = "앱 경기 목록 및 주요 경기 조회",
            description = """
                    리그별 경기 목록과 상단고정/핫매치 경기 목록을 함께 조회합니다.
                    featuredMatches에는 동일한 조회 조건으로 조회된 경기 중 상단고정 및 핫매치로 설정된 경기만 구분하여 반환합니다.
                    leagueMatches에는 경기 목록을 리그별로 그룹핑하여 반환합니다.
                    각 경기에는 매핑된 경기장이 있으면 경기장명과 도시명을 반환하고, 없으면 stadium을 null로 반환합니다.
                    """
    )
    ApiResponse<MatchAppResponse> getMatches(
            @Parameter(description = "조회 날짜. yyyyMMdd 형식입니다. 값이 없으면 서버 기준 오늘 날짜로 조회합니다.", example = "20260605")
            LocalDate date,
            @Parameter(description = "종목 ID", example = "1")
            Long sportId,
            @Parameter(description = "리그 ID", example = "2")
            Long leagueId
    );
}
