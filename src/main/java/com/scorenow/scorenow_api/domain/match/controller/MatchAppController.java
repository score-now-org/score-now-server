package com.scorenow.scorenow_api.domain.match.controller;

import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppItemResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchAppResponse;
import com.scorenow.scorenow_api.domain.match.service.MatchAppQueryService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/app/matches")
@RequiredArgsConstructor
public class MatchAppController implements MatchAppApiDocs {

    private final MatchAppQueryService matchAppQueryService;

    @GetMapping
    public ApiResponse<MatchAppResponse> getMatches(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyyMMdd") LocalDate date,
            @RequestParam(required = false) Long sportId,
            @RequestParam(required = false) Long leagueId) {

        return ApiResponse.success(matchAppQueryService.getMatchesWithFeatured(date, sportId, leagueId));
    }

    @GetMapping("/{matchId}")
    public ApiResponse<MatchAppItemResponse> getMatch(@PathVariable Long matchId) {
        return ApiResponse.success(matchAppQueryService.getMatch(matchId));
    }
}
