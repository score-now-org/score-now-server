package com.scorenow.scorenow_api.domain.player.controller;

import com.scorenow.scorenow_api.domain.player.dto.request.AdminPlayerAvailabilityUpdateRequest;
import com.scorenow.scorenow_api.domain.player.dto.request.PlayerAvailabilitySearchCondition;
import com.scorenow.scorenow_api.domain.player.dto.response.AdminPlayerAvailabilityResponse;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Tag(name = "Admin Player Availability API", description = "관리자 스쿼드/결장자 상태 관리 API")
public interface AdminPlayerAvailabilityApiDocs {

    @Operation(
            summary = "선수 가용 상태 검색",
            description = "선수 ID, 선수명, 팀 ID, 팀명 조건으로 선수-팀 상세 목록을 페이징 조회합니다. 조건이 없으면 전체 목록을 조회합니다."
    )
    ApiResponse<Page<AdminPlayerAvailabilityResponse>> searchPlayers(
            @ParameterObject PlayerAvailabilitySearchCondition condition,
            @ParameterObject Pageable pageable
    );

    @Operation(
            summary = "선수 가용 상태 수정",
            description = "선수-팀 상세 ID 기준으로 관리자 스쿼드/결장자 상태를 변경합니다."
    )
    ApiResponse<Void> updatePlayerAvailabilityStatus(
            @Parameter(description = "수정할 선수-팀 상세 ID", required = true, example = "1")
            Long playerTeamDetailId,
            @Parameter(description = "변경할 선수 가용 상태", required = true)
            AdminPlayerAvailabilityUpdateRequest request
    );
}
