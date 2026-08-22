package com.scorenow.scorenow_api.domain.stadium.controller;

import com.scorenow.scorenow_api.domain.stadium.dto.request.AdminStadiumCreateRequest;
import com.scorenow.scorenow_api.domain.stadium.dto.request.AdminStadiumUpdateRequest;
import com.scorenow.scorenow_api.domain.stadium.dto.request.StadiumSearchCondition;
import com.scorenow.scorenow_api.domain.stadium.dto.response.AdminStadiumResponse;
import com.scorenow.scorenow_api.domain.stadium.dto.response.AdminStadiumSearchOptionsResponse;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Tag(name = "Admin Stadium API", description = "관리자 경기장 관리 API")
public interface AdminStadiumApiDocs {

    @Operation(summary = "경기장 검색", description = "경기장 ID 또는 이름으로 경기장을 검색합니다. 조건이 없으면 전체 경기장을 조회합니다.")
    ApiResponse<Page<AdminStadiumResponse>> searchStadiums(
            @ParameterObject StadiumSearchCondition condition,
            @ParameterObject Pageable pageable);

    @Operation(summary = "경기장 관리 옵션 조회", description = "경기장 등록 화면에서 사용하는 종목 목록을 조회합니다.")
    ApiResponse<AdminStadiumSearchOptionsResponse> getSearchOptions();

    @Operation(summary = "경기장 단건 수동 생성", description = "수동으로 새로운 경기장 정보를 등록합니다.")
    ApiResponse<AdminStadiumResponse> createStadium(
            @Parameter(description = "생성할 경기장 정보", required = true) AdminStadiumCreateRequest request);

    @Operation(summary = "경기장 수정", description = "경기장 ID로 경기장명과 도시명을 부분 수정합니다. 요청에 포함된 필드만 변경됩니다.")
    ApiResponse<Void> updateStadium(
            @Parameter(description = "수정할 경기장 ID", required = true, example = "1") Long stadiumId,
            @Parameter(description = "수정할 경기장 정보", required = true) AdminStadiumUpdateRequest request);

    @Operation(summary = "경기장 삭제", description = "수동 등록된 경기장만 비활성화합니다. 외부 연동 경기장은 삭제할 수 없습니다.")
    ApiResponse<Void> deleteStadium(
            @Parameter(description = "삭제할 경기장 ID", required = true, example = "1") Long stadiumId);
}
