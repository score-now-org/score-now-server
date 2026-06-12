package com.scorenow.scorenow_api.domain.stadium.controller;

import com.scorenow.scorenow_api.domain.stadium.dto.request.AdminStadiumCreateRequest;
import com.scorenow.scorenow_api.domain.stadium.dto.request.StadiumSearchCondition;
import com.scorenow.scorenow_api.domain.stadium.dto.response.AdminStadiumResponse;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Stadium API", description = "경기장 관련 API")
public interface StadiumApiDocs {

    @Operation(summary = "경기장 검색", description = "경기장 ID 또는 이름으로 경기장을 검색합니다. 조건이 없으면 전체 경기장을 조회합니다.")
    ApiResponse<List<AdminStadiumResponse>> searchStadiums(StadiumSearchCondition condition);

    @Operation(summary = "경기장 단건 수동 생성", description = "수동으로 새로운 경기장 정보를 등록합니다. (추후 경기장 등록 관리자 페이지에서 사용)")
    ApiResponse<AdminStadiumResponse> createStadium(
            @Parameter(description = "생성할 경기장 정보", required = true) AdminStadiumCreateRequest request);
}
