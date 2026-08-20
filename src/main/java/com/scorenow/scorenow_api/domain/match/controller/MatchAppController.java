package com.scorenow.scorenow_api.domain.match.controller;

import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppLeagueGroupResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppResponse;
import com.scorenow.scorenow_api.domain.match.service.MatchAppQueryService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MatchAppController implements MatchAppApiDocs {

    private final MatchAppQueryService matchAppQueryService;

    @GetMapping("/v1/app/matches")
    public ApiResponse<List<MatchAppLeagueGroupResponse>> getMatches(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyyMMdd") LocalDate date,
            @RequestParam(required = false) Long sportId,
            @RequestParam(required = false) Long leagueId) {

        return ApiResponse.success(matchAppQueryService.getMatches(date, sportId, leagueId));
    }

    @GetMapping("/v2/app/matches")
    public ApiResponse<MatchAppResponse> getMatchesWithFeatured(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyyMMdd") LocalDate date,
            @RequestParam(required = false) Long sportId,
            @RequestParam(required = false) Long leagueId) {

        return ApiResponse.success(matchAppQueryService.getMatchesWithFeatured(date, sportId, leagueId));
    }
}
