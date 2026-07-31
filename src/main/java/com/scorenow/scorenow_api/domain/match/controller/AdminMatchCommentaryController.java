package com.scorenow.scorenow_api.domain.match.controller;

import com.scorenow.scorenow_api.domain.match.dto.request.CommentaryCreateRequest;
import com.scorenow.scorenow_api.domain.match.dto.response.AdminMatchCommentaryResponse;
import com.scorenow.scorenow_api.domain.match.service.MatchCommentaryService;
import com.scorenow.scorenow_api.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/matches/{matchId}/commentaries")
@RequiredArgsConstructor
public class AdminMatchCommentaryController implements AdminMatchCommentaryApiDocs {

    private final MatchCommentaryService commentaryService;

    /**
     * 중계 멘트 등록
     */
    @PostMapping
    public ApiResponse<Void> createCommentary(
            @PathVariable Long matchId,
            @Valid @RequestPart(value = "data") CommentaryCreateRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        commentaryService.saveCommentary(matchId, request.getContent(), request.isHighlighted(), request.isRecordEnabled(), image);
        return ApiResponse.success();
    }

    /**
     * 작성한 중계 멘트 목록 조회
     */
    @GetMapping
    public ApiResponse<List<AdminMatchCommentaryResponse>> getCommentaries(@PathVariable Long matchId) {
        return ApiResponse.success(commentaryService.getCommentariesByMatchId(matchId));
    }
}
