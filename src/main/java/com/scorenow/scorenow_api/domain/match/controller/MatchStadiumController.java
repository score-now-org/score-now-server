package com.scorenow.scorenow_api.domain.match.controller;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.match.dto.request.AssignStadiumRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.AssignTemporaryStadiumRequest;
import com.scorenow.scorenow_api.domain.match.service.MatchStadiumService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
public class MatchStadiumController {

    private final MatchStadiumService matchStadiumService;

    /**
     * 경기에 임시 경기장(일회성) 할당
     */
    @PatchMapping("/{matchId}/temporary-stadium")
    public ApiResponse<Void> assignTemporaryStadium(
            @PathVariable Long matchId,
            @Valid @RequestBody AssignTemporaryStadiumRequest request) {
        matchStadiumService.assignTemporaryStadiumToMatch(matchId, request.getStadiumName(), request.getCity());
        return ApiResponse.success();
    }

    /**
     * 경기에 정식 경기장 할당
     */
    @PatchMapping("/{matchId}/stadium")
    public ApiResponse<Void> assignStadium(
            @PathVariable Long matchId,
            @Valid @RequestBody AssignStadiumRequest request) {
        matchStadiumService.assignStadiumToMatch(matchId, request.getStadiumId());
        return ApiResponse.success();
    }

}
