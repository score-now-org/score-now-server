package com.scorenow.scorenow_api.domain.match.controller;

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
import org.springframework.web.bind.annotation.RestController;

import com.scorenow.scorenow_api.domain.match.dto.MatchSearchCondition;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchCreateRequest;
import com.scorenow.scorenow_api.domain.match.dto.request.MatchUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchListResponse;
import com.scorenow.scorenow_api.domain.match.service.AdminMatchService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/matches")
@RequiredArgsConstructor
public class AdminMatchController {

    private final AdminMatchService adminMatchService;

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
     * TODO: 응답값 기준 homeName, awayName 이 빈 문자열로 나감
     *          => 캐싱된 값이 반환 되어서 QueryDSL 로 조회한 값이 사용되지 않음 (QueryDSL 의 쿼리는 나가긴 함)
     *          => 기존에 작성하신 분의 의도를 몰라서 일단 급한건 아니니 수정은 안하고 반영
     * 경기 수동 등록
     */
    @PostMapping
    public ApiResponse<MatchListResponse> createMatch(@Valid @RequestBody MatchCreateRequest request) {
        MatchListResponse result = adminMatchService.createMatch(request);
        return ApiResponse.success(result);
    }

    /**
     * 경기 수정
     */
    @PatchMapping("/{matchId}")
    public ApiResponse<Void> updateMatch(
            @PathVariable Long matchId,
            @Valid @RequestBody MatchUpdateRequest request) {
        adminMatchService.updateMatch(matchId, request);
        return ApiResponse.success(null);
    }

}
