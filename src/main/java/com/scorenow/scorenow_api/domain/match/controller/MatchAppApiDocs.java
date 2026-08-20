package com.scorenow.scorenow_api.domain.match.controller;

import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppLeagueGroupResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppResponse;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Match App API", description = "앱 경기 목록 조회 API")
public interface MatchAppApiDocs {

    @Operation(
            summary = "앱 경기 목록 조회",
            description = """
                    앱에서 사용하는 경기 목록을 리그별로 그룹핑하여 조회합니다.
                    날짜, 종목, 리그 조건으로 필터링할 수 있으며 각 리그 하위 경기 정렬 순서는 진행중 > 경기 예정 > 경기 종료입니다.
                    경기 시간/결과 표시값은 statusDisplay.displayText를 우선 사용합니다.
                    statusDisplay.detail은 경기 상태와 종목에 따라 구조가 달라질 수 있습니다.
                    현재 중계 멘트가 있으면 currentCommentary와 isCurrentCommentaryHighlighted를 함께 반환합니다.
                    """
    )
    ApiResponse<List<MatchAppLeagueGroupResponse>> getMatches(
            @Parameter(description = "조회 날짜. yyyyMMdd 형식입니다. 값이 없으면 서버 기준 오늘 날짜로 조회합니다.", example = "20260605")
            LocalDate date,
            @Parameter(description = "종목 ID", example = "1")
            Long sportId,
            @Parameter(description = "리그 ID", example = "2")
            Long leagueId
    );

    @Operation(
            summary = "앱 경기 목록 및 주요 경기 조회 (v2)",
            description = """
                    기존 리그별 경기 목록과 상단고정/핫매치 경기 목록을 함께 조회합니다.
                    featuredMatches에는 동일한 조회 조건으로 조회된 경기 중 상단고정 및 핫매치로 설정된 경기만 구분하여 반환합니다.
                    leagueMatches는 v1과 동일하게 경기 목록을 리그별로 그룹핑하여 반환합니다.
                    """
    )
    ApiResponse<MatchAppResponse> getMatchesWithFeatured(
            @Parameter(description = "조회 날짜. yyyyMMdd 형식입니다. 값이 없으면 서버 기준 오늘 날짜로 조회합니다.", example = "20260605")
            LocalDate date,
            @Parameter(description = "종목 ID", example = "1")
            Long sportId,
            @Parameter(description = "리그 ID", example = "2")
            Long leagueId
    );
}
