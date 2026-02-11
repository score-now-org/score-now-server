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

    @PatchMapping("/{eventId}/detail")
    public ApiResponse<String> updateMatchDetail(
            @PathVariable String eventId,
            @RequestBody MatchDetailUpdateRequest request) {

        String updatedId = matchDetailService.updateMatchDetailManual(eventId, request);

        return ApiResponse.success(updatedId);
    }
}