package com.scorenow.scorenow_api.domain.odds.controller;

import com.scorenow.scorenow_api.domain.odds.dto.request.ManualMatchOddsRegisterRequest;
import com.scorenow.scorenow_api.domain.odds.dto.request.OddsDisplayModeUpdateRequest;
import com.scorenow.scorenow_api.domain.odds.dto.response.ManualMatchOddsHistoryResponse;
import com.scorenow.scorenow_api.domain.odds.dto.response.OverseasOddsManagementOverviewResponse;
import com.scorenow.scorenow_api.domain.odds.enums.OddsMarketType;
import com.scorenow.scorenow_api.domain.odds.service.AdminMatchOddsService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/matches/{matchId}/odds/overseas")
@RequiredArgsConstructor
public class AdminMatchOddsController {

    private final AdminMatchOddsService adminMatchOddsService;

    /**
     * 해외 배당률 관리 현황 조회
     */
    @GetMapping
    public ApiResponse<OverseasOddsManagementOverviewResponse> getOverseasOddsManagementOverview(
            @PathVariable Long matchId
    ) {
        OverseasOddsManagementOverviewResponse result = adminMatchOddsService
                .getOverseasOddsManagementOverview(matchId);
        return ApiResponse.success(result);
    }

    /**
     * 해외 수동 배당률 등록
     */
    @PostMapping("/manual-histories")
    public ApiResponse<Void> registerOverseasManualOdds(
            @PathVariable Long matchId,
            @Valid @RequestBody ManualMatchOddsRegisterRequest request
    ) {
        adminMatchOddsService.registerOverseasManualOdds(matchId, request);
        return ApiResponse.success();
    }

    /**
     * 해외 수동 배당률 이력 조회
     */
    @GetMapping("/manual-histories")
    public ApiResponse<Page<ManualMatchOddsHistoryResponse>> getOverseasManualOddsHistories(
            @PathVariable Long matchId,
            @RequestParam(required = false) Set<OddsMarketType> marketTypes,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<ManualMatchOddsHistoryResponse> result = adminMatchOddsService
                .getOverseasManualOddsHistories(matchId, marketTypes, pageable);
        return ApiResponse.success(result);
    }

    /**
     * 해외 수동 배당률 이력 무효화
     */
    @DeleteMapping("/manual-histories/{historyId}")
    public ApiResponse<Void> invalidateOverseasManualOddsHistory(
            @PathVariable Long matchId,
            @PathVariable Long historyId
    ) {
        adminMatchOddsService.invalidateOverseasManualOddsHistory(matchId, historyId);
        return ApiResponse.success();
    }

    /**
     * 해외 수동 배당률 전체 무효화
     */
    @DeleteMapping("/manual-histories")
    public ApiResponse<Void> invalidateAllOverseasManualOddsHistories(
            @PathVariable Long matchId
    ) {
        adminMatchOddsService.invalidateAllOverseasManualOddsHistories(matchId);
        return ApiResponse.success();
    }

    /**
     * 해외 배당률 노출 방식 변경
     */
    @PatchMapping("/display-mode")
    public ApiResponse<Void> changeOverseasOddsDisplayMode(
            @PathVariable Long matchId,
            @Valid @RequestBody OddsDisplayModeUpdateRequest request
    ) {
        adminMatchOddsService.changeOverseasOddsDisplayMode(matchId, request.displayMode());
        return ApiResponse.success();
    }
}
