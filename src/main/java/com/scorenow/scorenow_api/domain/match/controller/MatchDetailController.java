package com.scorenow.scorenow_api.domain.match.controller;

import com.scorenow.scorenow_api.domain.match.dto.request.MatchDetailUpdateRequest;
import com.scorenow.scorenow_api.domain.match.service.MatchDetailService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/matches")
public class MatchDetailController {

    private final MatchDetailService matchDetailService;

    /**
     * TODO: MatchDetailDocument 에 점수 정보가 안보임
     */
    @PatchMapping("/{matchId}/detail")
    public ApiResponse<Long> updateMatchDetail(
            @PathVariable Long matchId,
            @RequestBody MatchDetailUpdateRequest request) {

        Long updatedId = matchDetailService.updateMatchDetailManual(matchId, request);

        return ApiResponse.success(updatedId);
    }
}