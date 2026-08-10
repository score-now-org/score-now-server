package com.scorenow.scorenow_api.domain.league.controller;

import java.util.List;

import com.scorenow.scorenow_api.domain.league.dto.request.AdminLeagueCreateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.AdminLeagueUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueApiLeagueIdUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSearchCondition;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSyncEnabledUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueResponse;
import com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueSearchOptionsResponse;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

@Tag(name = "Admin League API", description = "관리자 리그 관리 API")
public interface AdminLeagueApiDocs {

    @Operation(summary = "리그 관리 옵션 조회", description = "리그 등록/수정 화면에서 사용할 종목, 팀 표시 순서, 외부 데이터 원천 옵션을 조회합니다.")
    ApiResponse<AdminLeagueSearchOptionsResponse> getSearchOptions();

    @Operation(summary = "리그 검색", description = "리그 ID 또는 keyword로 리그 목록을 검색합니다. 조건이 없으면 전체 리그를 조회합니다.")
    ApiResponse<List<AdminLeagueResponse>> getLeagues(
            @ParameterObject LeagueSearchCondition condition);

    @Operation(summary = "리그 단건 조회", description = "리그 ID로 리그 상세 정보를 조회합니다.")
    ApiResponse<AdminLeagueResponse> getLeagues(
            @Parameter(description = "조회할 리그 ID", required = true, example = "1") Long leagueId);

    @Operation(summary = "리그 생성", description = "관리자 페이지에서 리그를 등록합니다. 외부 API 연동 리그인 경우 외부 API 매핑 정보도 함께 등록합니다.")
    ApiResponse<AdminLeagueResponse> createLeague(
            @Parameter(description = "생성할 리그 정보", required = true) AdminLeagueCreateRequest request);

    @Operation(summary = "리그 수정", description = "리그 ID로 리그 기본 정보를 부분 수정합니다. 요청에 포함된 필드만 변경됩니다.")
    ApiResponse<Void> updateLeague(
            @Parameter(description = "수정할 리그 ID", required = true, example = "1") Long leagueId,
            @Parameter(description = "수정할 리그 정보", required = true) AdminLeagueUpdateRequest request);

    @Operation(summary = "외부 API 리그 ID 수정", description = "외부 API와 연동된 리그의 apiLeagueId를 수정합니다.")
    ApiResponse<Void> updateApiLeagueId(
            @Parameter(description = "수정할 리그 ID", required = true, example = "1") Long leagueId,
            @Parameter(description = "수정할 외부 API 리그 ID 정보", required = true) LeagueApiLeagueIdUpdateRequest request);

    @Operation(summary = "외부 API 동기화 여부 수정", description = "외부 API와 연동된 리그의 경기 동기화 활성화 여부를 수정합니다.")
    ApiResponse<Void> updateSyncEnabled(
            @Parameter(description = "수정할 리그 ID", required = true, example = "1") Long leagueId,
            @Parameter(description = "수정할 외부 API 동기화 여부", required = true) LeagueSyncEnabledUpdateRequest request);

    @Operation(summary = "리그 삭제", description = "리그 ID로 리그를 삭제합니다.")
    ApiResponse<Void> deleteLeague(
            @Parameter(description = "삭제할 리그 ID", required = true, example = "1") Long leagueId);
}
