package com.scorenow.scorenow_api.domain.match.controller;

import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppDetailResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppResponse;
import com.scorenow.scorenow_api.domain.odds.dto.response.MatchOddsHistorySliceResponse;
import com.scorenow.scorenow_api.domain.odds.enums.OddsCategory;
import com.scorenow.scorenow_api.domain.odds.enums.OddsMarketType;
import com.scorenow.scorenow_api.domain.sport.model.SportCode;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;

@Tag(name = "Match App API", description = "앱 경기 조회 API")
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
            @Parameter(description = "종목 코드", example = "FOOTBALL")
            SportCode sportCode,
            @Parameter(description = "리그 ID", example = "2")
            Long leagueId
    );

    @Operation(
            summary = "앱 경기 상세 조회",
            description = "경기의 공통 정보와 지원 종목의 상세 정보를 조회합니다."
    )
    ApiResponse<MatchAppDetailResponse> getMatch(
            @Parameter(description = "경기 ID", example = "1001")
            Long matchId
    );

    @Operation(
            summary = "앱 경기 배당률 이력 조회",
            description = "카테고리와 마켓 타입에 해당하는 활성 배당률 이력을 최신순으로 커서 조회합니다."
    )
    ApiResponse<MatchOddsHistorySliceResponse> getMatchOddsHistories(
            @Parameter(description = "경기 ID", example = "1001")
            Long matchId,
            @Parameter(description = "배당률 카테고리", example = "OVERSEAS")
            OddsCategory category,
            @Parameter(description = "배당률 마켓 타입", example = "HANDICAP")
            OddsMarketType marketType,
            @Parameter(description = "이전 응답의 nextCursor. 최초 요청에는 생략합니다.")
            String cursor,
            @Parameter(description = "조회할 이력 개수. 1~50, 기본값 20", example = "20")
            int size
    );
}
