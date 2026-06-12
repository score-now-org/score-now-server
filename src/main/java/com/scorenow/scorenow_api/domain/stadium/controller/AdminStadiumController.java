package com.scorenow.scorenow_api.domain.stadium.controller;

import java.util.List;

import com.scorenow.scorenow_api.domain.stadium.dto.request.AdminStadiumCreateRequest;
import com.scorenow.scorenow_api.domain.stadium.dto.request.AdminStadiumUpdateRequest;
import com.scorenow.scorenow_api.domain.stadium.dto.request.StadiumSearchCondition;
import com.scorenow.scorenow_api.domain.stadium.service.AdminStadiumService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import com.scorenow.scorenow_api.domain.stadium.dto.response.AdminStadiumResponse;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/stadiums")
@RequiredArgsConstructor
public class AdminStadiumController implements AdminStadiumApiDocs {

    private final AdminStadiumService adminStadiumService;

    /**
     * 경기장 검색 (경기장ID, 경기장명)
     */
    @GetMapping
    public ApiResponse<List<AdminStadiumResponse>> searchStadiums(@ModelAttribute StadiumSearchCondition condition) {
        return ApiResponse.success(adminStadiumService.searchStadiums(condition));
    }

    /**
     * 경기장 수동 등록
     */
    @PostMapping
    public ApiResponse<AdminStadiumResponse> createStadium(@Valid @RequestBody AdminStadiumCreateRequest request) {
        return ApiResponse.success(adminStadiumService.createStadium(request));
    }

    /**
     * 경기장 정보 수정
     */
    @PatchMapping("/{stadiumId}")
    public ApiResponse<Void> updateStadium(@PathVariable Long stadiumId, @Valid @RequestBody AdminStadiumUpdateRequest request) {
        adminStadiumService.updateStadium(stadiumId, request);
        return ApiResponse.success();
    }


}
