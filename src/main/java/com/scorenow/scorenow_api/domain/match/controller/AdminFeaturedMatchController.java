package com.scorenow.scorenow_api.domain.match.controller;

import com.scorenow.scorenow_api.domain.match.dto.request.FeaturedMatchCreateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.FeaturedMatchOrderUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.FeaturedMatchCandidateResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.FeaturedMatchesResponse;
import com.scorenow.scorenow_api.domain.match.service.AdminFeaturedMatchService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/featured-matches")
@RequiredArgsConstructor
public class AdminFeaturedMatchController implements AdminFeaturedMatchApiDocs {

    private final AdminFeaturedMatchService adminFeaturedMatchService;

    /**
     * 상단고정/핫매치 등록 경기 조회
     */
    @GetMapping
    public ApiResponse<FeaturedMatchesResponse> searchFeaturedMatches(
            @RequestParam @DateTimeFormat(pattern = "yyyyMMdd") LocalDate date) {

        FeaturedMatchesResponse result = adminFeaturedMatchService.searchFeaturedMatches(date);
        return ApiResponse.success(result);
    }

    /**
     * 상단고정/핫매치 등록 대상 경기 조회
     */
    @GetMapping("/candidates")
    public ApiResponse<List<FeaturedMatchCandidateResponse>> searchFeaturedMatchCandidates(
            @RequestParam @DateTimeFormat(pattern = "yyyyMMdd") LocalDate date) {

        List<FeaturedMatchCandidateResponse> result = adminFeaturedMatchService.searchFeaturedMatchCandidates(date);
        return ApiResponse.success(result);
    }

    /**
     * 상단고정/핫매치 등록
     */
    @PostMapping
    public ApiResponse<FeaturedMatchesResponse> createFeaturedMatch(
            @Valid @RequestBody FeaturedMatchCreateRequest request) {

        FeaturedMatchesResponse result = adminFeaturedMatchService.createFeaturedMatch(
                request.getMatchId(),
                request.getType());
        return ApiResponse.success(result);
    }

    /**
     * 상단고정/핫매치 순서 변경
     */
    @PatchMapping("/order")
    public ApiResponse<FeaturedMatchesResponse> updateFeaturedMatchesOrder(
            @Valid @RequestBody FeaturedMatchOrderUpdateRequest request) {

        FeaturedMatchesResponse result = adminFeaturedMatchService.updateFeaturedMatchesOrder(
                request.getDisplayDate(),
                request.getType(),
                request.getFeaturedMatchIds());
        return ApiResponse.success(result);
    }

    /**
     * 상단고정/핫매치 해제
     */
    @DeleteMapping("/{featuredMatchId}")
    public ApiResponse<FeaturedMatchesResponse> deleteFeaturedMatch(@PathVariable Long featuredMatchId) {
        FeaturedMatchesResponse result = adminFeaturedMatchService.deleteFeaturedMatch(featuredMatchId);
        return ApiResponse.success(result);
    }
}
