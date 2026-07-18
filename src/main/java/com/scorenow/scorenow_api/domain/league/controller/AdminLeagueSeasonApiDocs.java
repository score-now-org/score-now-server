package com.scorenow.scorenow_api.domain.league.controller;

import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonCreateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonSearchCondition;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueSeasonResponse;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Tag(name = "Admin League Season API", description = "관리자 리그 시즌 관리 API")
public interface AdminLeagueSeasonApiDocs {

    @Operation(summary = "리그 시즌 생성", description = "관리자 페이지에서 리그에 시즌 정보를 등록합니다. 현재 시즌으로 등록하면 기존 현재 시즌은 해제됩니다.")
    ApiResponse<Void> createLeagueSeason(
            @Parameter(description = "생성할 리그 시즌 정보", required = true) LeagueSeasonCreateRequest request);

    @Operation(summary = "리그 시즌 검색", description = "리그 ID, 리그명 조건으로 리그 시즌 목록을 페이징 조회합니다. 조건이 없으면 전체 리그 시즌을 조회합니다.")
    ApiResponse<Page<AdminLeagueSeasonResponse>> searchLeagueSeasons(
            @ParameterObject LeagueSeasonSearchCondition condition,
            @ParameterObject Pageable pageable);

    @Operation(summary = "리그 시즌 수정", description = "리그 시즌 ID 기준으로 시즌 정보를 부분 수정합니다. 요청에 포함된 필드만 변경됩니다.")
    ApiResponse<Void> updateLeagueSeason(
            @Parameter(description = "수정할 리그 시즌 ID", required = true, example = "1") Long leagueSeasonId,
            @Parameter(description = "수정할 리그 시즌 정보", required = true) LeagueSeasonUpdateRequest request);

    @Operation(summary = "리그 시즌 삭제", description = "리그 시즌 ID 기준으로 시즌을 비활성화합니다.")
    ApiResponse<Void> deleteLeagueSeason(
            @Parameter(description = "삭제할 리그 시즌 ID", required = true, example = "1") Long leagueSeasonId);
}
