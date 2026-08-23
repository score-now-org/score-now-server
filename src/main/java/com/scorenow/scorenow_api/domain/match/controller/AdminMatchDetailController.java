package com.scorenow.scorenow_api.domain.match.controller;

import com.scorenow.scorenow_api.domain.match.dto.request.FootballMatchDetailUpdateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.MatchDetailResponse;
import com.scorenow.scorenow_api.domain.match.service.MatchDetailService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/matches/{matchId}/detail")
public class AdminMatchDetailController implements AdminMatchDetailApiDocs {

    private final MatchDetailService matchDetailService;

    //TODO: 추후 야구 추가시 엔드포인트를 구분지을지, 아니면 다형성을 활용해서 확장된 응답을 내려줄지 고민 필요
    @GetMapping
    public ApiResponse<MatchDetailResponse> getMatchDetail(@PathVariable Long matchId) {
        return ApiResponse.success(matchDetailService.getMatchDetail(matchId));
    }

    @PatchMapping
    public ApiResponse<Void> updateMatchDetail(
            @PathVariable Long matchId,
            @RequestBody FootballMatchDetailUpdateRequest request) {

        matchDetailService.updateMatchDetailManual(matchId, request);
        return ApiResponse.success();
    }
}
