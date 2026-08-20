package com.scorenow.scorenow_api.domain.match.controller;

import com.scorenow.scorenow_api.domain.match.dto.request.FeaturedMatchCreateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.FeaturedMatchOrderUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.FeaturedMatchCandidateResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.FeaturedMatchesResponse;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Admin Featured Match API", description = "관리자 상단고정/핫매치 관리 API")
public interface AdminFeaturedMatchApiDocs {

    @Operation(
            summary = "상단고정/핫매치 등록 경기 조회",
            description = "yyyyMMdd 날짜 기준으로 상단고정/핫매치로 설정된 경기를 조회합니다. 핫매치와 상단고정은 각각 displayOrder 오름차순으로 반환됩니다."
    )
    ApiResponse<FeaturedMatchesResponse> searchFeaturedMatches(
            @Parameter(description = "조회 날짜. yyyyMMdd 형식입니다.", required = true, example = "20260723")
            LocalDate date
    );

    @Operation(
            summary = "상단고정/핫매치 등록 대상 경기 조회",
            description = "yyyyMMdd 날짜 기준으로 등록 후보 경기를 조회합니다. 이미 상단고정/핫매치로 등록된 경기는 registerable=false로 반환됩니다."
    )
    ApiResponse<List<FeaturedMatchCandidateResponse>> searchFeaturedMatchCandidates(
            @Parameter(description = "후보 경기 조회 날짜. yyyyMMdd 형식입니다.", required = true, example = "20260723")
            LocalDate date
    );

    @Operation(
            summary = "상단고정/핫매치 등록",
            description = "경기를 핫매치 또는 상단고정으로 등록합니다. 한 경기는 두 타입 중 하나로만 등록할 수 있습니다."
    )
    ApiResponse<Void> createFeaturedMatch(
            @Parameter(description = "등록할 경기와 타입 정보", required = true)
            FeaturedMatchCreateRequest request
    );

    @Operation(
            summary = "상단고정/핫매치 순서 변경",
            description = "특정 날짜와 타입의 상단고정/핫매치 순서를 변경합니다. featuredMatchIds는 변경 후 전체 순서 목록이어야 합니다."
    )
    ApiResponse<FeaturedMatchesResponse> updateFeaturedMatchesOrder(
            @Parameter(description = "순서 변경 정보", required = true)
            FeaturedMatchOrderUpdateRequest request
    );

    @Operation(
            summary = "상단고정/핫매치 해제",
            description = "상단고정/핫매치 설정을 제거합니다. 경기는 삭제되지 않으며, 같은 날짜와 타입의 후순위 항목은 한 칸씩 앞으로 이동합니다."
    )
    ApiResponse<FeaturedMatchesResponse> deleteFeaturedMatch(
            @Parameter(description = "삭제할 상단고정/핫매치 설정 ID", required = true, example = "1")
            Long featuredMatchId
    );
}
