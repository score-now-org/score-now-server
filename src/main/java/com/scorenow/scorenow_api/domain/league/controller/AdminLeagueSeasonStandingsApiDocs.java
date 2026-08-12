package com.scorenow.scorenow_api.domain.league.controller;

import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonStandingsCreateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonStandingsGroupMappingsUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonStandingsSearchCondition;
import com.scorenow.scorenow_api.domain.league.dto.request.LeagueSeasonStandingsTypeUpdateRequest;
import com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueSeasonStandingsGroupMappingsResponse;
import com.scorenow.scorenow_api.domain.league.dto.response.AdminLeagueSeasonStandingsResponse;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Admin League Season Standings API", description = "관리자 리그 시즌 순위 관리 API")
public interface AdminLeagueSeasonStandingsApiDocs {

    @Operation(summary = "리그 시즌 순위 관리 등록", description = "리그 시즌에 순위 관리 방식을 등록합니다. 시즌 하나에는 하나의 순위 관리 설정만 등록할 수 있습니다.")
    ApiResponse<Void> createLeagueSeasonStandings(
            @Parameter(description = "등록할 리그 시즌 순위 관리 정보", required = true) LeagueSeasonStandingsCreateRequest request);

    @Operation(summary = "리그 시즌 순위 관리 검색", description = "리그 ID, 리그명 조건으로 리그 시즌 순위 관리 목록을 페이징 조회합니다. 조건이 없으면 전체 목록을 조회합니다.")
    ApiResponse<Page<AdminLeagueSeasonStandingsResponse>> searchLeagueSeasonStandings(
            @ParameterObject LeagueSeasonStandingsSearchCondition condition,
            @ParameterObject Pageable pageable);

    @Operation(
            summary = "리그 시즌 순위 관리 타입 수정",
            description = "리그 시즌 순위 관리 타입을 IMAGE 또는 EXTERNAL_DATA로 변경합니다. 기존 이미지 URL과 외부 순위 문서는 삭제하지 않습니다."
    )
    ApiResponse<Void> updateLeagueSeasonStandingsType(
            @Parameter(description = "수정할 순위 관리 설정의 리그 시즌 ID", required = true, example = "1")
            Long leagueSeasonId,

            @Parameter(description = "변경할 리그 시즌 순위 관리 타입", required = true)
            LeagueSeasonStandingsTypeUpdateRequest request
    );

    @Operation(summary = "리그 시즌 순위 관리 삭제", description = "리그 시즌 ID 기준으로 순위 관리 설정을 삭제합니다. 타입과 무관하게 연결된 외부 순위 문서도 함께 삭제합니다.")
    ApiResponse<Void> deleteLeagueSeasonStandings(
            @Parameter(description = "삭제할 순위 관리 설정의 리그 시즌 ID", required = true, example = "1") Long leagueSeasonId);

    @Operation(summary = "리그 시즌 순위 이미지 업로드", description = "이미지 타입으로 관리되는 리그 시즌 순위 이미지를 업로드합니다.")
    ApiResponse<Void> uploadLeagueSeasonStandingsImage(
            @Parameter(description = "이미지를 업로드할 리그 시즌 ID", required = true, example = "1") Long leagueSeasonId,
            @Parameter(description = "업로드할 순위 이미지", required = true) MultipartFile image);

    @Operation(summary = "리그 시즌 순위 외부 데이터 동기화", description = "외부 데이터 타입으로 관리되는 리그 시즌 순위를 즉시 동기화합니다.")
    ApiResponse<Void> syncLeagueSeasonStandingsData(
            @Parameter(description = "동기화할 리그 시즌 ID", required = true, example = "1") Long leagueSeasonId);

    @Operation(summary = "리그 시즌 순위 그룹 표시 설정 조회", description = "외부 순위 그룹명과 관리자가 설정한 표시명 및 표시 순서를 조회합니다.")
    ApiResponse<AdminLeagueSeasonStandingsGroupMappingsResponse> getGroupMappings(
            @Parameter(description = "조회할 리그 시즌 ID", required = true, example = "1") Long leagueSeasonId);

    @Operation(summary = "리그 시즌 순위 그룹 표시 설정 수정", description = "그룹 key는 유지하고 앱에 노출할 표시명과 표시 순서를 일괄 수정합니다.")
    ApiResponse<Void> updateGroupMappings(
            @Parameter(description = "수정할 리그 시즌 ID", required = true, example = "1") Long leagueSeasonId,
            @Parameter(description = "수정할 그룹 표시 설정", required = true) LeagueSeasonStandingsGroupMappingsUpdateRequest request);
}
