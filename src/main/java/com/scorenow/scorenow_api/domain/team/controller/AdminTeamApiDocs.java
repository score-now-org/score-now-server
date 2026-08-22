package com.scorenow.scorenow_api.domain.team.controller;

import com.scorenow.scorenow_api.domain.team.dto.request.AdminTeamCreateRequest;
import com.scorenow.scorenow_api.domain.team.dto.request.AdminTeamUpdateRequest;
import com.scorenow.scorenow_api.domain.team.dto.request.TeamSearchCondition;
import com.scorenow.scorenow_api.domain.team.dto.response.AdminTeamResponse;
import com.scorenow.scorenow_api.domain.team.dto.response.AdminTeamSearchOptionsResponse;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Tag(name = "Admin Team API", description = "관리자 팀 관리 API")
public interface AdminTeamApiDocs {

    @Operation(summary = "팀 생성", description = "관리자 페이지에서 팀을 수동으로 등록합니다.")
    ApiResponse<AdminTeamResponse> createTeam(
            @Parameter(description = "생성할 팀 정보", required = true) AdminTeamCreateRequest request);

    @Operation(summary = "팀 검색", description = "팀 ID와 팀명 통합 검색어로 팀 목록을 페이징 조회합니다. 통합 검색어는 한글명, 영문명, 숏네임에 적용됩니다.")
    ApiResponse<Page<AdminTeamResponse>> searchTeams(
            @ParameterObject TeamSearchCondition condition,
            @ParameterObject Pageable pageable);

    @Operation(summary = "팀 관리 옵션 조회", description = "팀 등록 및 수정 화면에서 사용하는 팀 타입과 종목 목록을 조회합니다. 국가 코드는 포함하지 않습니다.")
    ApiResponse<AdminTeamSearchOptionsResponse> getSearchOptions();

    @Operation(summary = "팀 수정", description = "팀 ID로 팀 정보를 부분 수정합니다. 요청에 포함된 필드만 변경됩니다.")
    ApiResponse<Void> updateTeam(
            @Parameter(description = "수정할 팀 ID", required = true, example = "1") Long teamId,
            @Parameter(description = "수정할 팀 정보", required = true) AdminTeamUpdateRequest request);

    @Operation(summary = "팀 삭제", description = "수동 등록된 팀만 비활성화합니다. 외부 연동 팀은 삭제할 수 없습니다.")
    ApiResponse<Void> deleteTeam(
            @Parameter(description = "삭제할 팀 ID", required = true, example = "1") Long teamId);
}
