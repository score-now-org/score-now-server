package com.scorenow.scorenow_api.domain.stadium.controller;

import com.scorenow.scorenow_api.domain.stadium.dto.request.StadiumCreateRequest;
import com.scorenow.scorenow_api.domain.stadium.dto.response.StadiumResponse;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Stadium API", description = "경기장 관련 API")
public interface StadiumApiDocs {

    @Operation(summary = "전체 경기장 조회", description = "저장되어 있는 모든 경기장을 조회합니다.")
    ApiResponse<List<StadiumResponse>> getAllStadiums();

    @Operation(summary = "경기장 이름 검색", description = "경기장 이름으로 특정 경기장 목록을 검색합니다.")
    ApiResponse<List<StadiumResponse>> searchStadiumsByName(
            @Parameter(description = "검색할 경기장 이름", required = true, example = "Neo Quimica Arena") String name);

    @Operation(summary = "경기장 외부 API의 ID 로 검색", description = "외부 API의 경기장 ID(External ID)를 이용하여 특정 경기장을 검색합니다.")
    ApiResponse<List<StadiumResponse>> searchStadiumByExternalStadiumId(
            @Parameter(description = "검색할 외부 API 경기장 ID", required = true, example = "686") String id);

    @Operation(summary = "경기장 단건 수동 생성", description = "수동으로 새로운 경기장 정보를 등록합니다. (추후 경기장 등록 관리자 페이지에서 사용)")
    ApiResponse<StadiumResponse> createStadium(
            @Parameter(description = "생성할 경기장 정보", required = true) StadiumCreateRequest request);
}
