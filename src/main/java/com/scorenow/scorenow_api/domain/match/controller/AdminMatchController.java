package com.scorenow.scorenow_api.domain.match.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.match.dto.MatchSearchCondition;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchCreateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.AdminMatchSearchOptionsResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchListResponse;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchTeamCandidateResponse;
import com.scorenow.scorenow_api.domain.match.service.AdminMatchService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/matches")
@RequiredArgsConstructor
public class AdminMatchController implements AdminMatchApiDocs {

    private final AdminMatchService adminMatchService;

    /**
     * 경기 리스트 검색 옵션 조회
     */
    @GetMapping("/search-options")
    public ApiResponse<AdminMatchSearchOptionsResponse> getSearchOptions() {
        AdminMatchSearchOptionsResponse result = adminMatchService.getSearchOptions();
        return ApiResponse.success(result);
    }

    /**
     * 경기 등록용 팀 후보 검색
     */
    @GetMapping("/team-candidates")
    public ApiResponse<List<MatchTeamCandidateResponse>> getTeamCandidates(
            @RequestParam String keyword,
            @RequestParam Long sportId) {

        List<MatchTeamCandidateResponse> result = adminMatchService.getTeamCandidates(keyword, sportId);
        return ApiResponse.success(result);
    }

    /**
     * 경기 리스트 조회
     */
    @GetMapping
    public ApiResponse<Page<MatchListResponse>> getMatches(
            @ModelAttribute MatchSearchCondition condition,
            @PageableDefault(size = 20, sort = "startAt", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<MatchListResponse> result = adminMatchService.getMatches(condition, pageable);
        return ApiResponse.success(result);
    }


    /**
     * 경기 수동 등록
     */
    @PostMapping
    public ApiResponse<Void> createMatch(@Valid @RequestBody MatchCreateRequest request) {
        adminMatchService.createMatch(request);
        return ApiResponse.success();
    }

    /**
     * 경기 수정
     */
    @PatchMapping("/{matchId}")
    public ApiResponse<Void> updateMatch(
            @PathVariable Long matchId,
            @Valid @RequestBody MatchUpdateRequest request) {
        adminMatchService.updateMatch(matchId, request);
        return ApiResponse.success();
    }

}
